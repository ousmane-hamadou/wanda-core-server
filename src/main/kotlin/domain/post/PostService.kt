package com.github.ousmane_hamadou.domain.post

import com.github.ousmane_hamadou.domain.user.Role
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
            ?: return Result.failure(PostAuthorNotFoundException(authorId.toString()))

        val initialStatus = when {
            author.role == Role.ADMIN || author.role == Role.DELEGATE -> PostStatus.PUBLISHED
            author.trustScore.isHighReliability() -> PostStatus.PUBLISHED
            else -> PostStatus.PENDING
        }

        val newPost = Post(
            authorId = authorId,
            title = title,
            content = content,
            category = category,
            status = initialStatus,
            createdAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        )

        return Result.success(postRepository.save(newPost))
    }
}