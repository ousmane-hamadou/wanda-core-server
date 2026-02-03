package com.github.ousmane_hamadou.domain.post

import com.github.ousmane_hamadou.domain.user.Department
import com.github.ousmane_hamadou.domain.user.UserRole
import com.github.ousmane_hamadou.domain.user.TrustScore
import com.github.ousmane_hamadou.domain.user.User
import com.github.ousmane_hamadou.domain.user.UserService
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class PostServiceTest {

    private val postRepository = mockk<PostRepository>()
    private val userService = mockk<UserService>()
    private val postService = PostService(postRepository, userService)

    @Test
    fun `given delegate author when creating post then visibility should be restricted to their department`() = runTest {
        // Given
        val delegateId = Uuid.random()
        val delegate = User(
            id = delegateId,
            matricule = "D1",
            fullName = "Delegate IT",
            role = UserRole.DELEGATE,
            department = Department.COMPUTER_SCIENCE, // Filière spécifique
            level = "L3"
        )

        coEvery { userService.getUserProfile(delegateId) } returns delegate
        coEvery { postRepository.save(any()) } returnsArgument 0

        // When
        val result = postService.createPost(delegateId, "Urgent", "TP annulé", PostCategory.ALERT)

        // Then
        assertTrue(result.isSuccess)
        val post = result.getOrThrow()
        assertEquals(PostStatus.PUBLISHED, post.status)
        assertEquals(Department.COMPUTER_SCIENCE, post.visibility.department)
        assertNull(post.visibility.establishment) // Uniquement le département
    }

    @Test
    fun `given student author when creating post then visibility should also be restricted to their department`() = runTest {
        // Given
        val studentId = Uuid.random()
        val student = User(
            id = studentId,
            matricule = "S1",
            fullName = "Student",
            role = UserRole.STUDENT,
            department = Department.BIOLOGY,
            level = "L1"
        )

        coEvery { userService.getUserProfile(studentId) } returns student
        coEvery { postRepository.save(any()) } returnsArgument 0

        // When
        val result = postService.createPost(studentId, "Question", "Livre à prêter", PostCategory.INFO)

        // Then
        assertTrue(result.isSuccess)
        val post = result.getOrThrow()
        assertEquals(Department.BIOLOGY, post.visibility.department)
    }

    @Test
    fun `given admin author when creating post then visibility should be global`() = runTest {
        // Given
        val adminId = Uuid.random()
        val admin = User(
            id = adminId,
            matricule = "A1",
            fullName = "Admin",
            role = UserRole.ADMIN,
            department = Department.COMPUTER_SCIENCE, // Même si l'admin a un dept
            level = "N/A"
        )

        coEvery { userService.getUserProfile(adminId) } returns admin
        coEvery { postRepository.save(any()) } returnsArgument 0

        // When
        val result = postService.createPost(adminId, "Global", "Maintenant", PostCategory.ALERT)

        // Then
        assertTrue(result.isSuccess)
        val post = result.getOrThrow()
        assertNull(post.visibility.department)
        assertNull(post.visibility.establishment) // Global (null/null)
    }

    @Test
    fun `given low trust student when creating post then status should be PENDING`() = runTest {
        // Given
        val studentId = Uuid.random()
        val student = User(
            id = studentId,
            matricule = "S2",
            fullName = "Troll",
            role = UserRole.STUDENT,
            department = Department.MATHEMATICS,
            trustScore = TrustScore(10), // Faible
            level = "L2"
        )

        coEvery { userService.getUserProfile(studentId) } returns student
        coEvery { postRepository.save(any()) } returnsArgument 0

        // When
        val result = postService.createPost(studentId, "Titre", "Contenu", PostCategory.INFO)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(PostStatus.PENDING, result.getOrThrow().status)
    }

    @Test
    fun `given unknown author when creating post then should return PostAuthorNotFoundException`() = runTest {
        // Given
        val unknownId = Uuid.random()
        coEvery { userService.getUserProfile(unknownId) } returns null

        // When
        val result = postService.createPost(unknownId, "Titre", "Contenu", PostCategory.INFO)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is PostAuthorNotFoundException)
    }
}