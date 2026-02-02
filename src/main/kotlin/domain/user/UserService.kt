package com.github.ousmane_hamadou.domain.user

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid


@OptIn(ExperimentalUuidApi::class)
class UserService(
    private val userRepository: UserRepository
) {

    suspend fun registerUser(
        matricule: String,
        fullName: String,
        faculty: String,
        level: String
    ): Result<User> {
        val existingUser = userRepository.findByMatricule(matricule)
        if (existingUser != null) {
            return Result.failure(UserAlreadyExistsException(matricule))
        }

        // Par défaut, un nouvel inscrit est un simple STUDENT avec le TrustScore par défaut
        val newUser = User(
            matricule = matricule,
            fullName = fullName,
            faculty = faculty,
            level = level,
            role = Role.STUDENT,
            trustScore = TrustScore.DEFAULT
        )

        return Result.success(userRepository.save(newUser))
    }


    suspend fun promoteToDelegate(adminId: Uuid, targetStudentId: Uuid): Result<User> {
        // Vérifier si l'auteur de l'action est un admin
        val admin = userRepository.findById(adminId)
            ?: return Result.failure(UserNotFoundException(adminId.toString()))

        if (admin.role != Role.ADMIN) {
            return Result.failure(UnauthorizedAdminActionException(targetStudentId.toString()))
        }

        // Vérifier si l'étudiant cible existe
        val student = userRepository.findById(targetStudentId)
            ?: return Result.failure(Exception("Étudiant non trouvé"))

        // Appliquer le changement de rôle
        val promotedUser = student.copy(
            role = Role.DELEGATE,
            trustScore = TrustScore.MAX
        )

        return Result.success(userRepository.save(promotedUser))
    }

    /**
     * Logique d'évolution de la confiance.
     * Appelé par le domaine Validation lorsqu'une action positive/négative est confirmée.
     */
    suspend fun adjustUserTrust(userId: Uuid, impact: TrustImpact): Result<User> {
        val user = userRepository.findById(userId)
            ?: return Result.failure(UserNotFoundException(userId.toString()))

        val points = when (impact) {
            TrustImpact.POSITIVE_VALIDATION -> 5
            TrustImpact.OFFICIAL_POST_PUBLISHED -> 10
            TrustImpact.REPORT_CONFIRMED -> -20
            TrustImpact.FAKE_NEWS_PUBLISHED -> -50
        }

        val updatedUser = user.updateReputation(points)

        // TODO: Règle métier supplémentaire : Promotion automatique au rôle de confiance ?
        //  On pourrait imaginer qu'un étudiant avec 95 de score devient un "Vérificateur"

        return Result.success(userRepository.save(updatedUser))
    }

    suspend fun getUserProfile(userId: Uuid): User? {
        return userRepository.findById(userId)
    }
}