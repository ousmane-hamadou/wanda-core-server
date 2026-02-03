package com.github.ousmane_hamadou.domain.social


import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
interface ReportRepository {

    /**
     * Enregistre un nouveau signalement.
     * @throws ReportPersistenceException si la sauvegarde échoue.
     */
    suspend fun save(report: Report): Report

    /**
     * Récupère un signalement par son identifiant unique.
     */
    suspend fun findById(id: Uuid): Report?

    /**
     * Récupère tous les signalements en attente pour un établissement donné.
     * Utile pour les modérateurs délégués à un établissement spécifique.
     */
    suspend fun findPendingByEstablishment(establishmentId: String): List<Report>

    /**
     * Met à jour le statut d'un signalement (VALIDATED, REJECTED).
     */
    suspend fun updateStatus(id: Uuid, status: ReportStatus)

    /**
     * Compte combien de fois un post spécifique a été signalé.
     * Permet de prioriser les posts qui ont reçu beaucoup de signalements.
     */
    suspend fun countReportsForPost(postId: Uuid): Long

    /**
     * Vérifie si un utilisateur a déjà signalé un post spécifique.
     * Évite le spam de signalements par la même personne.
     */
    suspend fun existsByReporterAndPost(reporterId: Uuid, postId: Uuid): Boolean
}