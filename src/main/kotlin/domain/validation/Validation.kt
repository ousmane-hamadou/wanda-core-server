package com.github.ousmane_hamadou.domain.validation

import kotlinx.datetime.LocalDateTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

enum class ValidationType {
    CONFIRM,    // L'information est vraie
    REFUTE      // L'information est fausse/Fake News
}

@OptIn(ExperimentalUuidApi::class)
data class Validation(
    val id: Uuid = Uuid.random(),
    val postId: Uuid,
    val validatorId: Uuid,
    val type: ValidationType,
    val createdAt: LocalDateTime
)


