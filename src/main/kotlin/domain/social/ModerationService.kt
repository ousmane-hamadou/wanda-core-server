package com.github.ousmane_hamadou.domain.social


import com.github.ousmane_hamadou.domain.post.PostRepository
import com.github.ousmane_hamadou.domain.post.PostStatus
import com.github.ousmane_hamadou.domain.user.TrustImpact
import com.github.ousmane_hamadou.domain.user.UserService
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ModerationService(
    private val reportRepository: ReportRepository,
    private val postRepository: PostRepository,
    private val userService: UserService
) {
    /**
     * Valide un signalement.
     * Si validé, le post est masqué et l'auteur subit une baisse de score de confiance.
     */
    suspend fun validateReport(adminId: Uuid, reportId: Uuid): Result<Unit> = runCatching {
        val report = reportRepository.findById(reportId)
            ?: throw ReportNotFoundException("Signalement introuvable : $reportId")

        val post = postRepository.findById(report.postId)
            ?: throw PostNotFoundException("Le post signalé n'existe plus.")

        // 1. Marquer le report comme validé
        reportRepository.updateStatus(reportId, ReportStatus.VALIDATED)

        // 2. Sanctionner l'auteur du post (Impact négatif sur le trust score)
        val impact = when (report.reason) {
            ReportReason.FAKE_NEWS -> TrustImpact.FAKE_NEWS_PUBLISHED // -50
            else -> TrustImpact.REPORT_CONFIRMED // -20
        }

        userService.adjustUserTrust(post.authorId, impact).getOrThrow()

        // 3. Masquer ou supprimer le post
        postRepository.delete(post.id)

    }.onFailure {
        throw ModerationActionException("Échec de la validation du signalement", it)
    }

    private val AUTO_QUARANTINE_THRESHOLD = 5

    /**
     * Crée un signalement avec vérification anti-spam et quarantaine auto.
     */
    suspend fun reportPost(
        reporterId: Uuid,
        postId: Uuid,
        reason: ReportReason,
        details: String?
    ): Result<Report> = runCatching {
        // 1. Empêcher les doublons de signalements par le même utilisateur
        if (reportRepository.existsByReporterAndPost(reporterId, postId)) {
            throw DuplicateReportException("Vous avez déjà signalé ce contenu.")
        }

        // 2. Création et sauvegarde du signalement
        val report = Report(
            reporterId = reporterId,
            postId = postId,
            reason = reason,
            details = details,
            createdAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        )
        val savedReport = reportRepository.save(report)

        // 3. Logique de quarantaine automatique
        val reportCount = reportRepository.countReportsForPost(postId)
        if (reportCount >= AUTO_QUARANTINE_THRESHOLD) {
            // On bascule le post en ARCHIVED pour le sortir du flux public (findAllPublished)
            // tout en le gardant en base pour examen par un admin.
            postRepository.updateStatus(postId, PostStatus.ARCHIVED)
        }

        Result.success(savedReport)
    }.getOrElse { throwable ->
        when (throwable) {
            is DuplicateReportException -> throw throwable
            else -> throw ModerationActionException("Impossible d'enregistrer le signalement", throwable)
        }
    }
}