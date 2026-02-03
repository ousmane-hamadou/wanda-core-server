package com.github.ousmane_hamadou.domain.post

import com.github.ousmane_hamadou.domain.user.UserRole
import com.github.ousmane_hamadou.domain.user.UserService
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class PostService(
    private val postRepository: PostRepository,
    private val userService: UserService
) {
    suspend fun createPost(
        authorId: Uuid,
        title: String,
        content: String,
        category: PostCategory
    ): Result<Post> {
        val author = userService.getUserProfile(authorId)
            ?: return Result.failure(PostAuthorNotFoundException("Auteur introuvable : $authorId"))

        val initialStatus = when {
            author.role == UserRole.ADMIN || author.role == UserRole.DELEGATE -> PostStatus.PUBLISHED
            author.trustScore.isHighReliability() -> PostStatus.PUBLISHED
            else -> PostStatus.PENDING
        }

        // Règle : Étudiants et Délégués publient pour leur Département uniquement.
        // Seuls les ADMINS peuvent publier de manière globale (VisibilityScope vide).
        val visibilityScope = when (author.role) {
            UserRole.ADMIN -> VisibilityScope()
            else -> VisibilityScope(department = author.department)
        }

        val newPost = Post(
            authorId = authorId,
            title = title,
            content = content,
            category = category,
            status = initialStatus,
            source = PostSource.COMMUNITY,
            visibility = visibilityScope,
            createdAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        )

        return try {
            val savedPost = postRepository.save(newPost)
            Result.success(savedPost)
        } catch (e: Exception) {
            Result.failure(PostCreationException("Erreur lors de la sauvegarde du post", e))
        }
    }
}