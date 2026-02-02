package com.github.ousmane_hamadou.domain.validation

sealed class ValidationDomainException(message: String) : Exception(message)

class DoubleValidationException(userId: String, postId: String) :
    ValidationDomainException("L'utilisateur $userId a déjà validé la publication $postId.")

class SelfValidationException :
    ValidationDomainException("Un auteur ne peut pas valider sa propre publication.")