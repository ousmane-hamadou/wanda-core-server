package com.github.ousmane_hamadou.domain.social

import com.github.ousmane_hamadou.domain.post.*
import com.github.ousmane_hamadou.domain.user.UserService
import io.mockk.*
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
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
}