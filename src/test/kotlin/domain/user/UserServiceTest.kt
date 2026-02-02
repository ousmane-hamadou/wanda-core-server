package com.github.ousmane_hamadou.domain.user

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class UserServiceTest {

    private val userRepository = mockk<UserRepository>()
    private val userService = UserService(userRepository)

    @Test
    fun `given admin user when promoting student then should update role and trust score`() = runTest {
        // Given
        val adminId = Uuid.random()
        val studentId = Uuid.random()

        val admin = User(
            id = adminId,
            matricule = "ADM-001",
            fullName = "Admin User",
            role = Role.ADMIN,
            faculty = "FS",
            level = "N/A"
        )
        val student = User(
            id = studentId,
            matricule = "STU-001",
            fullName = "Student User",
            role = Role.STUDENT,
            faculty = "FS",
            level = "L3"
        )

        coEvery { userRepository.findById(adminId) } returns admin
        coEvery { userRepository.findById(studentId) } returns student
        coEvery { userRepository.save(any()) } returnsArgument 0

        // When
        val result = userService.promoteToDelegate(adminId, studentId)

        // Then
        assertTrue(result.isSuccess)
        val updatedUser = result.getOrThrow()
        assertEquals(Role.DELEGATE, updatedUser.role)
        assertEquals(TrustScore.MAX, updatedUser.trustScore)
    }

    @Test
    fun `given non-admin user when promoting student then should throw UnauthorizedAdminActionException`() = runTest {
        // Given
        val fakeAdminId = Uuid.random()
        val studentId = Uuid.random()
        val studentActingAsAdmin = User(
            id = fakeAdminId,
            matricule = "STU-002",
            fullName = "Regular Student",
            role = Role.STUDENT, // Il n'est PAS ADMIN
            faculty = "FS",
            level = "L1"
        )

        coEvery { userRepository.findById(fakeAdminId) } returns studentActingAsAdmin

        // When
        val result = userService.promoteToDelegate(fakeAdminId, studentId)

        // Then
        assertTrue(result.isFailure, "Result should be a failure")

        val exception = result.exceptionOrNull()

        assertTrue(
            exception is UnauthorizedAdminActionException,
            "Expected UnauthorizedAdminActionException but got ${exception?.javaClass?.simpleName}: ${exception?.message}"
        )
    }

    @Test
    fun `given existing matricule when registering then should throw UserAlreadyExistsException`() = runTest {
        // Given
        val matricule = "20A045FS"
        // Simule qu'un utilisateur existe déjà en retournant un mock au lieu de null
        coEvery { userRepository.findByMatricule(matricule) } returns mockk<User>()

        // When
        val result = userService.registerUser(matricule, "Jane Doe", "IUT", "GIM2")

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is UserAlreadyExistsException)
    }

    @Test
    fun `given valid user id when getting profile then should return user`() = runTest {
        // Given
        val userId = Uuid.random()
        val expectedUser = User(
            id = userId,
            matricule = "MAT-1",
            fullName = "Ousmane",
            role = Role.STUDENT,
            faculty = "FS",
            level = "L3"
        )
        coEvery { userRepository.findById(userId) } returns expectedUser

        // When
        val result = userService.getUserProfile(userId)

        // Then
        assertNotNull(result)
        assertEquals(expectedUser.id, result.id)
        assertEquals("Ousmane", result.fullName)
    }

    @Test
    fun `given non-existent user id when getting profile then should return null`() = runTest {
        // Given
        val userId = Uuid.random()
        coEvery { userRepository.findById(userId) } returns null

        // When
        val result = userService.getUserProfile(userId)

        // Then
        assertNull(result)
    }

    @Test
    fun `given positive validation when adjusting trust then should increase score by 5`() = runTest {
        // Given
        val userId = Uuid.random()
        val initialUser = User(
            id = userId,
            matricule = "MAT-1",
            fullName = "X",
            role = Role.STUDENT,
            faculty = "FS",
            level = "L1",
            trustScore = TrustScore(50)
        )

        coEvery { userRepository.findById(userId) } returns initialUser
        coEvery { userRepository.save(any()) } returnsArgument 0

        // When
        val result = userService.adjustUserTrust(userId, TrustImpact.POSITIVE_VALIDATION)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(55, result.getOrThrow().trustScore.value)
    }

    @Test
    fun `given fake news report when adjusting trust then should decrease score by 50`() = runTest {
        // Given
        val userId = Uuid.random()
        val initialUser = User(
            id = userId,
            matricule = "MAT-1",
            fullName = "X",
            role = Role.STUDENT,
            faculty = "FS",
            level = "L1",
            trustScore = TrustScore(60)
        )

        coEvery { userRepository.findById(userId) } returns initialUser
        coEvery { userRepository.save(any()) } returnsArgument 0

        // When
        val result = userService.adjustUserTrust(userId, TrustImpact.FAKE_NEWS_PUBLISHED)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(10, result.getOrThrow().trustScore.value)
    }

    @Test
    fun `given trust adjustment when score goes below zero then should be clamped to 0`() = runTest {
        // Given
        val userId = Uuid.random()
        val initialUser = User(
            id = userId,
            matricule = "MAT-1",
            fullName = "X",
            role = Role.STUDENT,
            faculty = "FS",
            level = "L1",
            trustScore = TrustScore(10)
        )

        coEvery { userRepository.findById(userId) } returns initialUser
        coEvery { userRepository.save(any()) } returnsArgument 0

        // When
        val result = userService.adjustUserTrust(userId, TrustImpact.REPORT_CONFIRMED) // Impact is -20

        // Then
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrThrow().trustScore.value)
    }

    @Test
    fun `given non-existent user when adjusting trust then should return UserNotFoundException`() = runTest {
        // Given
        val userId = Uuid.random()
        coEvery { userRepository.findById(userId) } returns null

        // When
        val result = userService.adjustUserTrust(userId, TrustImpact.POSITIVE_VALIDATION)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is UserNotFoundException)
    }
}