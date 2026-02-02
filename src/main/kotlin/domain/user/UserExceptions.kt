package com.github.ousmane_hamadou.domain.user

sealed class UserDomainException(message: String) : Exception(message)

class UserAlreadyExistsException(matricule: String) :
    UserDomainException("An account with matricule $matricule already exists.")

class UserNotFoundException(userId: String) :
    UserDomainException("User with ID $userId was not found.")

class UnauthorizedAdminActionException(adminId: String) :
    UserDomainException("Action denied: User $adminId does not have administrative privileges.")

class TrustAdjustmentException(userId: String, reason: String) :
    UserDomainException("Impossible d'ajuster le score de l'utilisateur $userId : $reason")