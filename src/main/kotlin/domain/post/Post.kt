package com.github.ousmane_hamadou.domain.post

import kotlinx.datetime.LocalDateTime
import kotlin.uuid.Uuid
import kotlin.uuid.ExperimentalUuidApi

enum class PostStatus {
    PENDING,    // En attente (score faible)
    PUBLISHED,  // Visible par tous (Admin, Délégué ou score élevé)
    SUSPECT,    // Marqué comme potentiellement faux
    ARCHIVED    // Retiré
}

enum class PostCategory {
    INFO,       // Information générale (cours, emploi du temps)
    ALERT,      // Alerte urgente (examen annulé, incident)
    EVENT       // Événements estudiantins
}

@OptIn(ExperimentalUuidApi::class)
data class Post(
    val id: Uuid = Uuid.random(),
    val authorId: Uuid,
    val title: String,
    val content: String,
    val category: PostCategory,
    val status: PostStatus = PostStatus.PENDING,
    val createdAt: LocalDateTime,
    val upVotes: Int = 0,
    val downVotes: Int = 0
)


