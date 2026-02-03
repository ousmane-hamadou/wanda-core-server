package com.github.ousmane_hamadou.domain.social

import com.github.ousmane_hamadou.domain.post.*
import com.github.ousmane_hamadou.domain.user.TrustImpact
import com.github.ousmane_hamadou.domain.user.User
import com.github.ousmane_hamadou.domain.user.UserService
import io.mockk.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ModerationServiceTest {

    private val reportRepository = mockk<ReportRepository>()
    private val postRepository = mockk<PostRepository>()
    private val userService = mockk<UserService>()
    private lateinit var moderationService: ModerationService

    @BeforeTest
    fun setup() {
        moderationService = ModerationService(reportRepository, postRepository, userService)
    }

    @Test
    fun `should throw DuplicateReportException when user reports same post twice`() = runTest {
        val reporterId = Uuid.random()
        val postId = Uuid.random()

        coEvery { reportRepository.existsByReporterAndPost(reporterId, postId) } returns true

        assertFailsWith<DuplicateReportException> {
            moderationService.reportPost(reporterId, postId, ReportReason.SPAM, null).getOrThrow()
        }
    }

    @Test
    fun `should archive post automatically when report threshold is reached`() = runTest {
        val postId = Uuid.random()
        val reporterId = Uuid.random()

        coEvery { reportRepository.existsByReporterAndPost(any(), any()) } returns false
        coEvery { reportRepository.save(any()) } returns mockk()
        // On simule qu'on vient d'atteindre le 5ème signalement
        coEvery { reportRepository.countReportsForPost(postId) } returns 5
        coEvery { postRepository.updateStatus(postId, PostStatus.ARCHIVED) } just Runs

        val result = moderationService.reportPost(reporterId, postId, ReportReason.HARASSMENT, "Méchant")

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { postRepository.updateStatus(postId, PostStatus.ARCHIVED) }
    }

    @Test
    fun `given valid report when confirmed then should penalize author and delete post`() = runTest {
        // Given
        val adminId = Uuid.random()
        val reportId = Uuid.random()
        val authorId = Uuid.random()
        val postId = Uuid.random()

        val report = Report(
            id = reportId,
            reporterId = Uuid.random(),
            postId = postId,
            reason = ReportReason.FAKE_NEWS,
            createdAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
            details = null
        )

        val post = Post(
            id = postId,
            authorId = authorId,
            title = "Post à supprimer",
            content = "Contenu problématique",
            category = PostCategory.OFFICIAL,
            status = PostStatus.ARCHIVED, // Il était en quarantaine
            source = PostSource.COMMUNITY,
            createdAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
            visibility = VisibilityScope(establishment = null)
        )

        coEvery { reportRepository.findById(reportId) } returns report
        coEvery { postRepository.findById(postId) } returns post
        coEvery { userService.adjustUserTrust(authorId, any()) } returns Result.success(mockk<User>())
        coEvery { postRepository.delete(postId) } just Runs
        coEvery { reportRepository.updateStatus(reportId, ReportStatus.VALIDATED) } just Runs

        // When
        val result = moderationService.confirmReport(adminId, reportId)

        // Then
        assertTrue(result.isSuccess)
        coVerify {
            userService.adjustUserTrust(authorId, TrustImpact.FAKE_NEWS_PUBLISHED)
            postRepository.delete(postId)
            reportRepository.updateStatus(reportId, ReportStatus.VALIDATED)
        }
    }

    @Test
    fun `given valid report when rejected then should restore post status and close report`() = runTest {
        // Given
        val adminId = Uuid.random()
        val reportId = Uuid.random()
        val postId = Uuid.random()

        val report = Report(
            id = reportId,
            reporterId = Uuid.random(),
            postId = postId,
            reason = ReportReason.WRONG_CATEGORY,
            createdAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
            details = null,
        )

        coEvery { reportRepository.findById(reportId) } returns report
        coEvery { postRepository.updateStatus(postId, PostStatus.PUBLISHED) } just Runs
        coEvery { reportRepository.updateStatus(reportId, ReportStatus.REJECTED) } just Runs

        // When
        val result = moderationService.rejectReport(adminId, reportId)

        // Then
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) {
            postRepository.updateStatus(postId, PostStatus.PUBLISHED)
            reportRepository.updateStatus(reportId, ReportStatus.REJECTED)
        }
        // On vérifie qu'aucune sanction n'est appliquée
        coVerify(exactly = 0) { userService.adjustUserTrust(any(), any()) }
    }
}