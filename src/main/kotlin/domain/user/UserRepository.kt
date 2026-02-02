package com.github.ousmane_hamadou.domain.user

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
interface UserRepository {
    suspend fun findById(id: Uuid): User?
    suspend fun findByMatricule(matricule: String): User?
    suspend fun save(user: User): User
    suspend fun delete(id: Uuid)
    suspend fun findAllByFaculty(faculty: String): List<User>
}

/**
 * Définit les types d'impacts sur la réputation dans le système Wanda.
 */
enum class TrustImpact {
    POSITIVE_VALIDATION,      // Un de ses posts a été validé
    OFFICIAL_POST_PUBLISHED,  // Publication d'un contenu utile
    REPORT_CONFIRMED,         // Un de ses contenus a été signalé à juste titre
    FAKE_NEWS_PUBLISHED       // Sanction lourde pour fausse information
}