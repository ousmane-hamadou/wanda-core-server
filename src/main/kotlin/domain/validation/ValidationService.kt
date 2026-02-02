package com.github.ousmane_hamadou.domain.validation

import com.github.ousmane_hamadou.domain.post.PostNotFoundException
import com.github.ousmane_hamadou.domain.post.PostRepository
import com.github.ousmane_hamadou.domain.post.PostStatus
import com.github.ousmane_hamadou.domain.user.TrustImpact
import com.github.ousmane_hamadou.domain.user.UserService
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val PUBLICATION_THRESHOLD = 5
private const val SUSPICION_THRESHOLD = 3

@OptIn(ExperimentalUuidApi::class)
class ValidationService(
    private val validationRepository: ValidationRepository,
    private val postRepository: PostRepository,
    private val userService: UserService,
) {
    suspend fun validatePost(
        validatorId: Uuid,
        postId: Uuid,
        type: ValidationType
    ): Result<Validation> {
        // 1. Vérifier si le post existe
        val post = postRepository.findById(postId)
            ?: return Result.failure(PostNotFoundException(postId.toString()))

        // 2. Règle métier : Pas d'auto-validation
        if (post.authorId == validatorId) {
            return Result.failure(SelfValidationException())
        }

        // 3. Règle métier : Pas de double vote
        val alreadyValidated = validationRepository.hasUserValidatedPost(validatorId, postId)
        if (alreadyValidated) {
            return Result.failure(DoubleValidationException(validatorId.toString(), postId.toString()))
        }

        val validation = Validation(
            postId = postId,
            validatorId = validatorId,
            type = type,
            createdAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        )

        val savedValidation = validationRepository.save(validation)

        // --- IMPACT SUR LE TRUST SCORE ---
        val impact = if (type == ValidationType.CONFIRM)
            TrustImpact.POSITIVE_VALIDATION
        else
            TrustImpact.REPORT_CONFIRMED

        userService.adjustUserTrust(post.authorId, impact)

        updatePostStatusIfNeeded(postId)

        return Result.success(savedValidation)
    }

    private suspend fun updatePostStatusIfNeeded(postId: Uuid) {
        val post = postRepository.findById(postId) ?: return

        // On ne change pas le statut d'un post déjà archivé ou publié par un Admin
        if (post.status == PostStatus.ARCHIVED) return

        val confirms = validationRepository.countByType(postId, ValidationType.CONFIRM)
        val refutes = validationRepository.countByType(postId, ValidationType.REFUTE)

        val newStatus = when {
            refutes >= SUSPICION_THRESHOLD -> PostStatus.SUSPECT
            confirms >= PUBLICATION_THRESHOLD && post.status == PostStatus.PENDING -> PostStatus.PUBLISHED
            else -> post.status
        }

        if (newStatus != post.status) {
            postRepository.save(post.copy(status = newStatus))
        }
    }


}