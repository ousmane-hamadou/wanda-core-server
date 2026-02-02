package com.github.ousmane_hamadou.domain.validation

import com.github.ousmane_hamadou.domain.post.*
import com.github.ousmane_hamadou.domain.user.TrustImpact
import com.github.ousmane_hamadou.domain.user.UserService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ValidationServiceTest {

    private val validationRepository = mockk<ValidationRepository>()
    private val postRepository = mockk<PostRepository>()
    private val userService = mockk<UserService>(relaxed = true) // Nouveau mock

    private lateinit var validationService: ValidationService

    @BeforeTest
    fun setup() {
        validationService = ValidationService(validationRepository, postRepository, userService)
    }

    private fun createDummyPost(id: Uuid, authorId: Uuid) = Post(
        id = id,
        authorId = authorId,
        title = "Titre Test",
        content = "Contenu de test",
        category = PostCategory.INFO,
        status = PostStatus.PENDING,
        createdAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    )

    @Test
    fun `given valid data when validating post then should return success`() = runTest {
        // Given
        val validatorId = Uuid.random()
        val postId = Uuid.random()
        val authorId = Uuid.random()

        val post = createDummyPost(postId, authorId)

        coEvery { postRepository.findById(postId) } returns post
        coEvery { validationRepository.hasUserValidatedPost(validatorId, postId) } returns false
        coEvery { validationRepository.save(any()) } returnsArgument 0

        coEvery { userService.adjustUserTrust(any(), any()) } returns Result.success(mockk())
        coEvery { validationRepository.countByType(any(), any()) } returns 0
        coEvery { postRepository.save(any()) } returnsArgument 0

        // When
        val result = validationService.validatePost(validatorId, postId, ValidationType.CONFIRM)

        // Then
        assertTrue(result.isSuccess)
    }

    @Test
    fun `given non-existent post when validating then should return PostNotFoundException`() = runTest {
        // Given
        val postId = Uuid.random()
        coEvery { postRepository.findById(postId) } returns null

        // When
        val result = validationService.validatePost(Uuid.random(), postId, ValidationType.CONFIRM)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is PostNotFoundException)
    }

    @Test
    fun `given author when validating own post then should return SelfValidationException`() = runTest {
        // Given
        val authorId = Uuid.random()
        val postId = Uuid.random()

        val post = createDummyPost(postId, authorId)

        coEvery { postRepository.findById(postId) } returns post

        // When
        val result = validationService.validatePost(authorId, postId, ValidationType.CONFIRM)

        // Then
        assertTrue(result.isFailure, "Le résultat devrait être un échec")
        assertTrue(
            result.exceptionOrNull() is SelfValidationException,
            "L'exception devrait être SelfValidationException"
        )
    }

    @Test
    fun `given user who already validated when validating again then should return DoubleValidationException`() =
        runTest {
            // Given
            val userId = Uuid.random()
            val postId = Uuid.random()
            val post = Post(
                id = postId,
                authorId = Uuid.random(), // Auteur différent
                title = "Titre",
                content = "Contenu",
                category = PostCategory.INFO,
                status = PostStatus.PUBLISHED,
                createdAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            )

            coEvery { postRepository.findById(postId) } returns post
            coEvery { validationRepository.hasUserValidatedPost(userId, postId) } returns true

            // When
            val result = validationService.validatePost(userId, postId, ValidationType.CONFIRM)

            // Then
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is DoubleValidationException)
        }

    @Test
    fun `given confirm validation when validating then should call userService adjustTrust`() = runTest {
        // Given
        val validatorId = Uuid.random()
        val authorId = Uuid.random()
        val postId = Uuid.random()

        val post = createDummyPost(postId, authorId)

        // Configuration des mocks
        coEvery { postRepository.findById(postId) } returns post
        coEvery { validationRepository.hasUserValidatedPost(validatorId, postId) } returns false
        coEvery { validationRepository.save(any()) } returnsArgument 0
        coEvery { validationRepository.countByType(any(), any()) } returns 1
        coEvery { postRepository.save(any()) } returnsArgument 0
        // On simule une réponse positive du service de trust
        coEvery {
            userService.adjustUserTrust(
                authorId,
                TrustImpact.POSITIVE_VALIDATION
            )
        } returns Result.success(mockk())

        // When
        val result = validationService.validatePost(validatorId, postId, ValidationType.CONFIRM)

        // Then
        assertTrue(result.isSuccess, "La validation devrait réussir")

        // Vérification cruciale : le UserService a-t-il été notifié avec les bons paramètres ?
        coVerify(exactly = 1) {
            userService.adjustUserTrust(authorId, TrustImpact.POSITIVE_VALIDATION)
        }
    }

    @Test
    fun `given pending post when reaching 5th confirmation then status should become PUBLISHED`() = runTest {
        // Given
        val postId = Uuid.random()
        val authorId = Uuid.random()
        val post = Post(
            id = postId, authorId = authorId, title = "Alerte", content = "Test",
            category = PostCategory.ALERT, status = PostStatus.PENDING,
            createdAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        )

        coEvery { postRepository.findById(postId) } returns post
        coEvery { validationRepository.hasUserValidatedPost(any(), postId) } returns false
        coEvery { validationRepository.save(any()) } returnsArgument 0
        coEvery { userService.adjustUserTrust(any(), any()) } returns Result.success(mockk())

        // Simuler qu'on a maintenant 5 confirmations au total
        coEvery { validationRepository.countByType(postId, ValidationType.CONFIRM) } returns 5
        coEvery { validationRepository.countByType(postId, ValidationType.REFUTE) } returns 0

        // On mocke la sauvegarde finale du post mis à jour
        coEvery { postRepository.save(any()) } returnsArgument 0

        // When
        validationService.validatePost(Uuid.random(), postId, ValidationType.CONFIRM)

        // Then
        coVerify {
            postRepository.save(match { it.status == PostStatus.PUBLISHED })
        }
    }
}