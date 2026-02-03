package com.github.ousmane_hamadou.domain.social

import kotlinx.datetime.LocalDateTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

enum class ReportReason {
    SPAM, FAKE_NEWS, HARASSMENT, INAPPROPRIATE_CONTENT, WRONG_CATEGORY
}

enum class ReportStatus {
    PENDING, VALIDATED, REJECTED
}

@OptIn(ExperimentalUuidApi::class)
data class Report(
    val id: Uuid = Uuid.random(),
    val reporterId: Uuid,
    val postId: Uuid,
    val reason: ReportReason,
    val details: String?,
    val status: ReportStatus = ReportStatus.PENDING,
    val createdAt: LocalDateTime
)