package com.github.ousmane_hamadou.domain.post

import com.github.ousmane_hamadou.domain.user.Role
import com.github.ousmane_hamadou.domain.user.TrustScore
import com.github.ousmane_hamadou.domain.user.User
import com.github.ousmane_hamadou.domain.user.UserService
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class PostServiceTest {

    private val postRepository = mockk<PostRepository>()
    private val userService = mockk<UserService>()
    private val postService = PostService(postRepository, userService)

    @Test
    fun `given admin author when creating post then status should be PUBLISHED`() = runTest {
        // Given
        val adminId = Uuid.random()
        val admin =
            User(id = adminId, matricule = "A1", fullName = "Admin", role = Role.ADMIN, faculty = "FS", level = "N/A")

        coEvery { userService.getUserProfile(adminId) } returns admin
        coEvery { postRepository.save(any()) } returnsArgument 0

        // When
        val result = postService.createPost(adminId, "Titre", "Contenu", PostCategory.ALERT)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(PostStatus.PUBLISHED, result.getOrThrow().status)
    }

    @Test
    fun `given low trust student when creating post then status should be PENDING`() = runTest {
        // Given
        val studentId = Uuid.random()
        val student = User(
            id = studentId,
            matricule = "S1",
            fullName = "Student",
            role = Role.STUDENT,
            trustScore = TrustScore(30), // Score faible
            faculty = "FS",
            level = "L1"
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