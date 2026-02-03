package com.github.ousmane_hamadou.domain.social

import com.github.ousmane_hamadou.domain.post.Post
import com.github.ousmane_hamadou.domain.post.PostCategory
import com.github.ousmane_hamadou.domain.post.PostRepository
import com.github.ousmane_hamadou.domain.post.PostSource
import com.github.ousmane_hamadou.domain.post.PostStatus
import kotlin.uuid.Uuid

class InboundSyncService(
    private val providers: List<ExternalInformationProvider>,
    private val postRepository: PostRepository
) {
    // ID système pour les comptes officiels
    private val SYSTEM_OFFICIAL_ID = Uuid.parse("00000000-0000-0000-0000-000000000000")

    suspend fun syncAllSources() = runCatching {
        providers.forEach { provider ->
            val externalPosts = provider.fetchLatestPosts()

            externalPosts.forEach { ext ->
                if (!postRepository.existsByExternalId(ext.externalId)) {
                    val post = Post(
                        authorId = SYSTEM_OFFICIAL_ID,
                        title = ext.title ?: "Communiqué ${provider.sourceName}",
                        content = ext.content,
                        category = PostCategory.INFO,
                        status = PostStatus.PUBLISHED,
                        source = PostSource.EXTERNAL_OFFICIAL,
                        externalId = ext.externalId,
                        originName = provider.sourceName,
                        createdAt = ext.date
                    )
                    postRepository.save(post)
                }
            }
        }
    }.onFailure {
        throw ExternalIntegrationException("Échec de la synchronisation des sources externes", it)
    }
}