package com.github.ousmane_hamadou.domain.social

import com.github.ousmane_hamadou.domain.user.Establishment
import kotlinx.datetime.LocalDateTime

/**
 * Interface pour les fournisseurs de données externes (Facebook, RSS, Scrapers).
 */
interface ExternalInformationProvider {
    /**
     * Nom affiché de la source (ex: "Page Facebook IUT de Ngaoundéré")
     */
    val sourceName: String

    /**
     * Définit à quel établissement ces informations sont destinées.
     * Si null, l'information est considérée comme globale (ex: Rectorat).
     */
    val targetEstablishment: Establishment?

    /**
     * Récupère les derniers messages depuis la source externe.
     * @throws ExternalIntegrationException en cas d'erreur réseau ou de parsing.
     */
    suspend fun fetchLatestPosts(): List<ExternalInboundPost>
}

/**
 * Objet de transfert de données (DTO) pour les messages entrants.
 */
data class ExternalInboundPost(
    val externalId: String,
    val title: String?,
    val content: String,
    val date: LocalDateTime,
    val rawUrl: String?
)