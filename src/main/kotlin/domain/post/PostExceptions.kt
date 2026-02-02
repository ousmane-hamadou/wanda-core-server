package com.github.ousmane_hamadou.domain.post

sealed class PostDomainException(message: String) : Exception(message)

class PostAuthorNotFoundException(authorId: String) :
    PostDomainException("Impossible de créer la publication : l'auteur avec l'ID $authorId est introuvable.")

class PostNotFoundException(postId: String) :
    PostDomainException("La publication avec l'ID $postId n'existe pas.")