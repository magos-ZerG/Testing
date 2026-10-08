package com.z23u184.studymate.data.lab

import com.z23u184.studymate.domain.DomainError
import com.z23u184.studymate.domain.DomainResult
import com.z23u184.studymate.domain.model.TaskStatus
import com.z23u184.studymate.domain.model.TopicId
import io.qameta.allure.Description
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import io.qameta.allure.Severity
import io.qameta.allure.SeverityLevel
import io.qameta.allure.Story
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalTime::class)
@RunWith(RandomRobolectricTestRunner::class)
@Config(sdk = [35])
@Epic("ТИОПО1")
@Feature("CreateTaskUseCase")
@Story("Классические")
class CreateTaskUseCaseClassicTest {
    private lateinit var fixture: CreateTaskClassicFixture

    @Before
    fun setUp() {
        val context =
            RuntimeEnvironment.getApplication().applicationContext

        fixture = CreateTaskClassicFixture.create(context)
        fixture.database.openHelper.writableDatabase
    }

    @After
    fun tearDown() {
        fixture.close()
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @Description("Проверяется trim и фактическая запись в БД.")
    fun `creates normalized task and persists it through real repository`() = runTest {
        // Arrange
        fixture.seedTopic()
        val deadline = Instant.parse("2026-10-01T12:00:00Z")
        val input = CreateTaskInputBuilder()
            .withTitle("  Read chapter 1  ")
            .withDescription("  Prepare a short summary  ")
            .withStatus(TaskStatus.IN_PROGRESS)
            .withDeadline(deadline)
            .build()

        // Act
        val result = fixture.useCase(
            input.topicId,
            input.title,
            input.description,
            input.status,
            input.deadlineAt,
        )

        // Assert
        assertTrue(result is DomainResult.Success)
        val created = (result as DomainResult.Success).value
        val persistedViaRepository = fixture.taskRepository.getById(created.id)
        val persistedEntity = fixture.database.taskDao().getById(created.id.value)

        assertEquals("Read chapter 1", created.title)
        assertEquals("Prepare a short summary", created.description)
        assertEquals(TaskStatus.IN_PROGRESS, created.status)
        assertEquals(deadline, created.deadlineAt)
        assertEquals(created, persistedViaRepository)
        assertNotNull(persistedEntity)
        assertEquals("Read chapter 1", persistedEntity?.title)
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @Description("Негативный: отсутствие родительской темы.")
    fun `returns topic not found when real repository cannot find parent topic`() = runTest {
        // Arrange
        val input = CreateTaskInputBuilder()
            .withTopicId(TopicId("missing-topic"))
            .build()

        // Act
        val result = fixture.useCase(
            input.topicId,
            input.title,
            input.description,
            input.status,
            input.deadlineAt,
        )

        // Assert
        assertEquals(DomainResult.Failure(DomainError.TopicNotFound), result)
        assertNull(fixture.database.taskDao().getById("missing-task"))
        assertTrue(fixture.taskRepository.getByTopicId(input.topicId).isEmpty())
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @Description("Граничное значение: название превращается в пустую строку")
    fun `rejects blank title before writing through real repository`() = runTest {
        // Arrange
        fixture.seedTopic()
        val input = CreateTaskInputBuilder()
            .withTitle("   \t  ")
            .build()

        // Act
        val result = fixture.useCase(
            input.topicId,
            input.title,
            input.description,
            input.status,
            input.deadlineAt,
        )

        // Assert
        assertEquals(DomainResult.Failure(DomainError.EmptyTaskTitle), result)
        assertTrue(fixture.taskRepository.getByTopicId(input.topicId).isEmpty())
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @Description("Граничный: 10001 символ в названии")
    fun `rejects description just above maximum without database write`() = runTest {
        // Arrange
        fixture.seedTopic()
        val input = CreateTaskInputBuilder()
            .withDescription("x".repeat(10_001))
            .build()

        // Act
        val result = fixture.useCase(
            input.topicId,
            input.title,
            input.description,
            input.status,
            input.deadlineAt,
        )

        // Assert
        assertEquals(DomainResult.Failure(DomainError.TaskDescriptionTooLong), result)
        assertTrue(fixture.taskRepository.getByTopicId(input.topicId).isEmpty())
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @Description("Граничный: ровно 10000 символов в названии.")
    fun `accepts description exactly at maximum and persists it`() = runTest {
        // Arrange
        fixture.seedTopic()
        val description = "x".repeat(10_000)
        val input = CreateTaskInputBuilder()
            .withDescription(description)
            .build()

        // Act
        val result = fixture.useCase(
            input.topicId,
            input.title,
            input.description,
            input.status,
            input.deadlineAt,
        )

        // Assert
        assertTrue(result is DomainResult.Success)
        val created = (result as DomainResult.Success).value
        val persisted = fixture.taskRepository.getById(created.id)
        assertNotNull(persisted)
        assertEquals(10_000, persisted?.description?.length)
        assertEquals(description, persisted?.description)
    }
}
