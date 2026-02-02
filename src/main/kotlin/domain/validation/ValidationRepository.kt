package com.github.ousmane_hamadou.domain.validation

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
interface ValidationRepository {
    suspend fun save(validation: Validation): Validation

    /**
     * Vérifie si un utilisateur a déjà apporté une validation (CONFIRM ou REFUTE) à un post donné.
     * Crucial pour empêcher le double vote.
     */
    suspend fun hasUserValidatedPost(userId: Uuid, postId: Uuid): Boolean

    suspend fun findByPostId(postId: Uuid): List<Validation>

    /**
     * Compte le nombre de validations par type pour un post.
     * Utile pour calculer la fiabilité globale d'une information.
     */
    suspend fun countByType(postId: Uuid, type: ValidationType): Int
}