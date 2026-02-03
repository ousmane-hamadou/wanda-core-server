package com.github.ousmane_hamadou.domain.post

import com.github.ousmane_hamadou.domain.user.Department
import com.github.ousmane_hamadou.domain.user.Establishment
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
    EVENT, // Événements estudiantins
    OFFICIAL,
}

enum class PostSource {
    COMMUNITY, EXTERNAL_OFFICIAL
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
    val downVotes: Int = 0,
    val source: PostSource = PostSource.COMMUNITY,
    val externalId: String? = null,
    val originName: String? = null,
    val visibility: VisibilityScope = VisibilityScope(),
)

/**
 * Définit la portée de visibilité d'un post.
 * Si les deux sont nuls, le post est considéré comme public à toute l'université.
 */
data class VisibilityScope(
    val establishment: Establishment? = null, // Visible par tout l'établissement (ex: IUT)
    val department: Department? = null        // Restreint à une filière (ex: Génie Informatique)
)