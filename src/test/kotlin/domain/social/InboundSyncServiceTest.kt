package com.github.ousmane_hamadou.domain.social

import com.github.ousmane_hamadou.domain.post.*
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

@OptIn(ExperimentalUuidApi::class)
class InboundSyncServiceTest {

    private val postRepository = mockk<PostRepository>()
    private val provider = mockk<ExternalInformationProvider>()
    private lateinit var syncService: InboundSyncService

    @BeforeTest
    fun setup() {
        // On initialise le service avec une liste de providers (ici un seul pour le test)
        syncService = InboundSyncService(listOf(provider), postRepository)
        every { provider.sourceName } returns "Facebook Univ"
    }

    @Test
    fun `given new external posts when syncing then should save them as official posts`() = runTest {
        // Given
        val externalId = "fb_123"
        val externalPost = ExternalInboundPost(
            externalId = externalId,
            title = "Avis de grève",
            content = "Les cours sont suspendus...",
            date = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
            rawUrl = "https://facebook.com/post/123"
        )

        coEvery { provider.fetchLatestPosts() } returns listOf(externalPost)
        coEvery { postRepository.existsByExternalId(externalId) } returns false
        coEvery { postRepository.save(any()) } returnsArgument 0

        // When
        val result = syncService.syncAllSources()

        // Then
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) {
            postRepository.save(match {
                it.source == PostSource.EXTERNAL_OFFICIAL &&
                        it.externalId == externalId &&
                        it.status == PostStatus.PUBLISHED
            })
        }
    }

    @Test
    fun `given existing external posts when syncing then should not save duplicates`() = runTest {
        // Given
        val externalId = "fb_existing"
        val externalPost = ExternalInboundPost(
            externalId = externalId,
            title = "Déjà présent",
            content = "Contenu...",
            date = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
            rawUrl = null
        )

        coEvery { provider.fetchLatestPosts() } returns listOf(externalPost)
        coEvery { postRepository.existsByExternalId(externalId) } returns true

        // When
        syncService.syncAllSources()

        // Then
        coVerify(exactly = 0) { postRepository.save(any()) }
    }

    @Test
    fun `given provider failure when syncing then should throw ExternalIntegrationException`() = runTest {
        // Given
        coEvery { provider.fetchLatestPosts() } throws RuntimeException("API Down")

        // When & Then
        assertFailsWith<ExternalIntegrationException> {
            syncService.syncAllSources().getOrThrow()
        }
    }
}