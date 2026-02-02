package com.github.ousmane_hamadou.domain.user

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

enum class Role {
    STUDENT,   // Utilisateur standard
    DELEGATE,  // Délégué (Source de confiance)
    ADMIN      // Modérateur (Administration)
}


@OptIn(ExperimentalUuidApi::class)
data class User(
    val id: Uuid = Uuid.random(),
    val matricule: String,
    val fullName: String,
    val faculty: String,
    val level: String,
    val role: Role,
    val trustScore: TrustScore = TrustScore.DEFAULT
) {
    fun canPublishCertified(): Boolean = role == Role.DELEGATE || role == Role.ADMIN

    fun canVote(): Boolean = trustScore.value > 0

    fun updateReputation(points: Int): User {
        val newValue = (trustScore.value + points).coerceIn(0, 100)
        return this.copy(trustScore = TrustScore(newValue))
    }
}