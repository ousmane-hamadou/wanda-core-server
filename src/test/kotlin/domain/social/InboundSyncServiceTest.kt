package com.github.ousmane_hamadou.domain.social

import com.github.ousmane_hamadou.domain.post.*
import com.github.ousmane_hamadou.domain.user.Establishment
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
class InboundSyncServiceTest {

    private val postRepository = mockk<PostRepository>()
    private val provider = mockk<ExternalInformationProvider>()
    private lateinit var syncService: InboundSyncService

    private val SYSTEM_OFFICIAL_ID = Uuid.parse("00000000-0000-0000-0000-000000000000")

    @BeforeTest
    fun setup() {
        syncService = InboundSyncService(listOf(provider), postRepository)
        every { provider.sourceName } returns "Facebook IUT"
        // Par défaut, le provider cible l'IUT dans nos tests
        every { provider.targetEstablishment } returns Establishment.IUT
    }

    @Test
    fun `given new external posts when syncing then should save with correct visibility and system id`() = runTest {
        // Given
        val externalId = "fb_123"
        val externalPost = ExternalInboundPost(
            externalId = externalId,
            title = "Avis de concours",
            content = "Détails du concours...",
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
                it.authorId == SYSTEM_OFFICIAL_ID &&
                        it.category == PostCategory.OFFICIAL &&
                        it.visibility.establishment == Establishment.IUT &&
                        it.externalId == externalId
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
    fun `given database failure when saving then should throw InboundDataPersistenceException`() = runTest {
        // Given
        val externalPost = ExternalInboundPost(
            externalId = "id_fail",
            title = "Titre",
            content = "Contenu",
            date = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
            rawUrl = null
        )

        coEvery { provider.fetchLatestPosts() } returns listOf(externalPost)
        coEvery { postRepository.existsByExternalId(any()) } returns false
        // Simulation d'une erreur de base de données
        coEvery { postRepository.save(any()) } throws RuntimeException("DB Error")

        // When & Then
        val exception = assertFailsWith<ExternalIntegrationException> {
            syncService.syncAllSources().getOrThrow()
        }

        // Vérification que l'erreur de persistance est bien encapsulée
        assertTrue(exception.cause is InboundDataPersistenceException)
    }

    @Test
    fun `given provider failure when syncing then should throw ExternalIntegrationException`() = runTest {
        // Given
        coEvery { provider.fetchLatestPosts() } throws RuntimeException("Network Down")

        // When & Then
        assertFailsWith<ExternalIntegrationException> {
            syncService.syncAllSources().getOrThrow()
        }
    }
}