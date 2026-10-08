package com.z23u184.studymate.data.lab

import com.z23u184.studymate.domain.DomainError
import com.z23u184.studymate.domain.DomainResult
import com.z23u184.studymate.domain.model.StudyTask
import com.z23u184.studymate.domain.model.TaskId
import com.z23u184.studymate.domain.repository.TaskRepository
import com.z23u184.studymate.domain.repository.TopicRepository
import com.z23u184.studymate.domain.service.ClockProvider
import com.z23u184.studymate.domain.service.IdGenerator
import com.z23u184.studymate.domain.usecase.CreateTaskUseCase
import com.z23u184.studymate.domain.usecase.SyncAfterMutationUseCase
import com.z23u184.studymate.domain.validation.TaskValidator
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.qameta.allure.Description
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import io.qameta.allure.Severity
import io.qameta.allure.SeverityLevel
import io.qameta.allure.Story
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@RunWith(RandomOrderRunner::class)
@Epic("ТИОПО1")
@Feature("CreateTaskUseCase")
@Story("Лондонские")
class CreateTaskUseCaseLondonTest {
    private lateinit var topicRepository: TopicRepository
    private lateinit var taskRepository: TaskRepository
    private lateinit var taskValidator: TaskValidator
    private lateinit var idGenerator: IdGenerator
    private lateinit var clockProvider: ClockProvider
    private lateinit var syncAfterMutationUseCase: SyncAfterMutationUseCase
    private lateinit var useCase: CreateTaskUseCase

    @Before
    fun setUp() {
        topicRepository = mockk()
        taskRepository = mockk()
        taskValidator = mockk()
        idGenerator = mockk()
        clockProvider = mockk()
        syncAfterMutationUseCase = mockk()

        useCase = CreateTaskUseCase(
            topicRepository = topicRepository,
            taskRepository = taskRepository,
            taskValidator = taskValidator,
            idGenerator = idGenerator,
            clockProvider = clockProvider,
            syncAfterMutationUseCase = syncAfterMutationUseCase,
        )
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @Description("Проверяется протокол взаимодействия: нормализация, валидация, генерация, save и sync вызываются ровно там, где должны.")
    fun `orchestrates collaborators on successful creation`() = runTest {
        // Arrange
        val topic = TopicMother.existing()
        val input = CreateTaskInputBuilder()
            .withTitle("  London title  ")
            .withDescription("  London description  ")
            .build()
        val savedTask = slot<StudyTask>()

        coEvery { topicRepository.getById(input.topicId) } returns topic
        every { taskValidator.validateTitle("London title") } returns DomainResult.Success(Unit)
        every { taskValidator.validateDescription("London description") } returns DomainResult.Success(Unit)
        every { idGenerator.newTaskId() } returns TaskId("mock-task")
        every { clockProvider.now() } returns LAB_FIXED_TIME
        coEvery { taskRepository.save(capture(savedTask)) } just Runs
        coEvery { syncAfterMutationUseCase.invoke() } returns DomainResult.Success(Unit)

        // Act
        val result = useCase(
            input.topicId,
            input.title,
            input.description,
            input.status,
            input.deadlineAt,
        )

        // Assert
        assertTrue(result is DomainResult.Success)
        assertEquals("mock-task", savedTask.captured.id.value)
        assertEquals("London title", savedTask.captured.title)
        assertEquals("London description", savedTask.captured.description)
        coVerify(exactly = 1) { topicRepository.getById(input.topicId) }
        verify(exactly = 1) { taskValidator.validateTitle("London title") }
        verify(exactly = 1) { taskValidator.validateDescription("London description") }
        verify(exactly = 1) { idGenerator.newTaskId() }
        verify(exactly = 1) { clockProvider.now() }
        coVerify(exactly = 1) { taskRepository.save(any()) }
        coVerify(exactly = 1) { syncAfterMutationUseCase.invoke() }
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @Description("Негативный: если TopicRepository вернул null, флоу прерывается.")
    fun `stops immediately when topic is missing`() = runTest {
        // Arrange
        val input = CreateTaskInputBuilder().build()
        coEvery { topicRepository.getById(input.topicId) } returns null

        // Act
        val result = useCase(
            input.topicId,
            input.title,
            input.description,
            input.status,
            input.deadlineAt,
        )

        // Assert
        assertEquals(DomainResult.Failure(DomainError.TopicNotFound), result)
        coVerify(exactly = 1) { topicRepository.getById(input.topicId) }
        verify(exactly = 0) { taskValidator.validateTitle(any()) }
        verify(exactly = 0) { taskValidator.validateDescription(any()) }
        verify(exactly = 0) { idGenerator.newTaskId() }
        verify(exactly = 0) { clockProvider.now() }
        coVerify(exactly = 0) { taskRepository.save(any()) }
        coVerify(exactly = 0) { syncAfterMutationUseCase.invoke() }
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @Description("Негативный: ошибка проверки названия прерывает флоу проверки")
    fun `stops after title validator rejects input`() = runTest {
        // Arrange
        val topic = TopicMother.existing()
        val input = CreateTaskInputBuilder().withTitle("bad title").build()
        coEvery { topicRepository.getById(input.topicId) } returns topic
        every { taskValidator.validateTitle("bad title") } returns DomainResult.Failure(DomainError.TaskTitleTooLong)

        // Act
        val result = useCase(
            input.topicId,
            input.title,
            input.description,
            input.status,
            input.deadlineAt,
        )

        // Assert
        assertEquals(DomainResult.Failure(DomainError.TaskTitleTooLong), result)
        verify(exactly = 1) { taskValidator.validateTitle("bad title") }
        verify(exactly = 0) { taskValidator.validateDescription(any()) }
        verify(exactly = 0) { idGenerator.newTaskId() }
        verify(exactly = 0) { clockProvider.now() }
        coVerify(exactly = 0) { taskRepository.save(any()) }
        coVerify(exactly = 0) { syncAfterMutationUseCase.invoke() }
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @Description("Негативный: после ошибки объект не создаётся.")
    fun `stops after description validator rejects input`() = runTest {
        // Arrange
        val topic = TopicMother.existing()
        val input = CreateTaskInputBuilder().withDescription("bad description").build()
        coEvery { topicRepository.getById(input.topicId) } returns topic
        every { taskValidator.validateTitle(input.title) } returns DomainResult.Success(Unit)
        every { taskValidator.validateDescription("bad description") } returns DomainResult.Failure(DomainError.TaskDescriptionTooLong)

        // Act
        val result = useCase(
            input.topicId,
            input.title,
            input.description,
            input.status,
            input.deadlineAt,
        )

        // Assert
        assertEquals(DomainResult.Failure(DomainError.TaskDescriptionTooLong), result)
        verify(exactly = 1) { taskValidator.validateTitle(input.title) }
        verify(exactly = 1) { taskValidator.validateDescription("bad description") }
        verify(exactly = 0) { idGenerator.newTaskId() }
        verify(exactly = 0) { clockProvider.now() }
        coVerify(exactly = 0) { taskRepository.save(any()) }
        coVerify(exactly = 0) { syncAfterMutationUseCase.invoke() }
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @Description("Негативный: синхронизация не вызывается после сохранения")
    fun `propagates save exception and never starts sync`() {
        // Arrange
        val topic = TopicMother.existing()
        val input = CreateTaskInputBuilder().build()
        val expected = IllegalStateException("mock persistence failure")
        coEvery { topicRepository.getById(input.topicId) } returns topic
        every { taskValidator.validateTitle(input.title) } returns DomainResult.Success(Unit)
        every { taskValidator.validateDescription(input.description) } returns DomainResult.Success(Unit)
        every { idGenerator.newTaskId() } returns TaskId("mock-task")
        every { clockProvider.now() } returns LAB_FIXED_TIME
        coEvery { taskRepository.save(any()) } throws expected

        // Act
        val actual = assertThrows(IllegalStateException::class.java) {
            runBlocking {
                useCase(
                    input.topicId,
                    input.title,
                    input.description,
                    input.status,
                    input.deadlineAt,
                )
            }
        }

        // Assert
        assertEquals(expected.message, actual.message)
        coVerify(exactly = 1) { taskRepository.save(any()) }
        coVerify(exactly = 0) { syncAfterMutationUseCase.invoke() }
    }

    companion object {
        @JvmStatic
        @BeforeClass
        fun warmUpMockK() {
            val mock = mockk<Runnable>()
            every { mock.run() } returns Unit
            mock.run()
            verify { mock.run() }
            clearAllMocks()
        }
    }
}
