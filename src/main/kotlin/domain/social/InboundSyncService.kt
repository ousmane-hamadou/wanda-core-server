package com.github.ousmane_hamadou.domain.social

import com.github.ousmane_hamadou.domain.post.*
import kotlin.uuid.Uuid

// ID système pour les comptes officiels
private val SYSTEM_OFFICIAL_ID = Uuid.parse("00000000-0000-0000-0000-000000000000")

class InboundSyncService(
    private val providers: List<ExternalInformationProvider>,
    private val postRepository: PostRepository
) {
    suspend fun syncAllSources() = runCatching {
        providers.forEach { provider ->
            val externalPosts = provider.fetchLatestPosts()

            externalPosts.forEach { ext ->
                if (!postRepository.existsByExternalId(ext.externalId)) {
                    val post = Post(
                        authorId = SYSTEM_OFFICIAL_ID,
                        title = ext.title ?: "Communiqué ${provider.sourceName}",
                        content = ext.content,
                        category = PostCategory.OFFICIAL,
                        status = PostStatus.PUBLISHED,
                        source = PostSource.EXTERNAL_OFFICIAL,
                        externalId = ext.externalId,
                        originName = provider.sourceName,
                        createdAt = ext.date,
                        visibility = VisibilityScope(establishment = provider.targetEstablishment)
                    )

                    try {
                        postRepository.save(post)
                    } catch (e: Exception) {
                        // Utilisation d'une exception personnalisée pour la persistance lors du sync
                        throw InboundDataPersistenceException(
                            "Impossible de sauvegarder le post externe ${ext.externalId}",
                            e
                        )
                    }
                }
            }
        }
    }.onFailure {
        throw ExternalIntegrationException("Échec de la synchronisation des sources externes", it)
    }
}