package com.github.ousmane_hamadou.domain.social

import kotlinx.datetime.LocalDateTime

interface ExternalInformationProvider {
    val sourceName: String
    suspend fun fetchLatestPosts(): List<ExternalInboundPost>
}

data class ExternalInboundPost(
    val externalId: String,
    val title: String?,
    val content: String,
    val date: LocalDateTime,
    val rawUrl: String?
)