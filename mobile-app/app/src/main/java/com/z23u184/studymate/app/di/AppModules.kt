package com.z23u184.studymate.app.di

import com.z23u184.studymate.app.BuildConfig
import com.z23u184.studymate.app.mapper.SolutionUiMapper
import com.z23u184.studymate.app.mapper.TaskUiMapper
import com.z23u184.studymate.app.mapper.TopicUiMapper
import com.z23u184.studymate.app.ui.auth.login.LoginScreenViewModel
import com.z23u184.studymate.app.ui.auth.register.RegisterScreenViewModel
import com.z23u184.studymate.app.ui.flashcards.FlashcardSessionViewModel
import com.z23u184.studymate.app.ui.root.AppStartViewModel
import com.z23u184.studymate.app.ui.task.createtask.CreateTaskScreenViewModel
import com.z23u184.studymate.app.ui.task.taskdetails.TaskDetailsScreenViewModel
import com.z23u184.studymate.app.ui.task.tasklist.TopicTaskListScreenViewModel
import com.z23u184.studymate.app.ui.topic.createtopic.CreateTopicScreenViewModel
import com.z23u184.studymate.app.ui.topic.topiclist.TopicListScreenViewModel
import com.z23u184.studymate.data.di.studyMateDataModule
import com.z23u184.studymate.domain.service.ClockProvider
import com.z23u184.studymate.domain.service.DefaultFlashcardBestResultPolicy
import com.z23u184.studymate.domain.service.DefaultSyncPolicyService
import com.z23u184.studymate.domain.service.FlashcardBestResultPolicy
import com.z23u184.studymate.domain.service.DefaultTaskStatusTransitionPolicy
import com.z23u184.studymate.domain.service.IdGenerator
import com.z23u184.studymate.domain.service.SyncPolicyService
import com.z23u184.studymate.domain.service.TaskStatusTransitionPolicy
import com.z23u184.studymate.domain.usecase.AddAttachmentUseCase
import com.z23u184.studymate.domain.usecase.ChangeTaskStatusUseCase
import com.z23u184.studymate.domain.usecase.CreateTaskUseCase
import com.z23u184.studymate.domain.usecase.CreateTopicUseCase
import com.z23u184.studymate.domain.usecase.DeleteAttachmentUseCase
import com.z23u184.studymate.domain.usecase.DeleteTaskSolutionUseCase
import com.z23u184.studymate.domain.usecase.DeleteTaskUseCase
import com.z23u184.studymate.domain.usecase.DeleteTopicUseCase
import com.z23u184.studymate.domain.usecase.GetAttachmentsByOwnerUseCase
import com.z23u184.studymate.domain.usecase.GetFlashcardBestResultUseCase
import com.z23u184.studymate.domain.usecase.GetFlashcardsByTopicUseCase
import com.z23u184.studymate.domain.usecase.GetAuthorizedUserIdUseCase
import com.z23u184.studymate.domain.usecase.GetTaskByIdUseCase
import com.z23u184.studymate.domain.usecase.GetTaskSolutionUseCase
import com.z23u184.studymate.domain.usecase.GetTasksByTopicUseCase
import com.z23u184.studymate.domain.usecase.GetTopicByIdUseCase
import com.z23u184.studymate.domain.usecase.GetTopicsUseCase
import com.z23u184.studymate.domain.usecase.GetUserModeUseCase
import com.z23u184.studymate.domain.usecase.HandleLoginSyncUseCase
import com.z23u184.studymate.domain.usecase.LoginUseCase
import com.z23u184.studymate.domain.usecase.LogoutUseCase
import com.z23u184.studymate.domain.usecase.PrepareLogoutUseCase
import com.z23u184.studymate.domain.usecase.RegisterUseCase
import com.z23u184.studymate.domain.usecase.SaveFlashcardBestResultUseCase
import com.z23u184.studymate.domain.usecase.SaveTaskSolutionUseCase
import com.z23u184.studymate.domain.usecase.SyncAfterMutationUseCase
import com.z23u184.studymate.domain.usecase.SyncStudyDataUseCase
import com.z23u184.studymate.domain.usecase.UpdateTaskUseCase
import com.z23u184.studymate.domain.validation.AttachmentValidator
import com.z23u184.studymate.domain.validation.TaskValidator
import com.z23u184.studymate.domain.validation.DefaultAttachmentValidator
import com.z23u184.studymate.domain.validation.DefaultTaskValidator
import com.z23u184.studymate.domain.validation.TopicValidator
import com.z23u184.studymate.domain.validation.DefaultTopicValidator
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.time.Clock
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

@OptIn(ExperimentalUuidApi::class)
fun appModules(): List<Module> = listOf(
    studyMateDataModule(baseUrl = BuildConfig.SERVER_BASE_URL),
    appSupportModule,
    useCaseModule,
    viewModelModule,
)

@OptIn(ExperimentalUuidApi::class)
private val appSupportModule = module {
    single<TopicValidator> { DefaultTopicValidator() }
    single<TaskValidator> { DefaultTaskValidator() }
    single<AttachmentValidator> { DefaultAttachmentValidator() }
    single<SyncPolicyService> { DefaultSyncPolicyService() }
    single<FlashcardBestResultPolicy> { DefaultFlashcardBestResultPolicy() }
    single<TaskStatusTransitionPolicy> { DefaultTaskStatusTransitionPolicy() }
    single<ClockProvider> {
        object : ClockProvider {
            override fun now() = Clock.System.now()
        }
    }
    single<IdGenerator> {
        object : IdGenerator {
            override fun newTopicId() = com.z23u184.studymate.domain.model.TopicId(Uuid.random().toString())
            override fun newTaskId() = com.z23u184.studymate.domain.model.TaskId(Uuid.random().toString())
            override fun newTaskSolutionId() = com.z23u184.studymate.domain.model.TaskSolutionId(Uuid.random().toString())
            override fun newAttachmentId() = com.z23u184.studymate.domain.model.AttachmentId(Uuid.random().toString())
        }
    }
    single { TopicUiMapper() }
    single { TaskUiMapper() }
    single { SolutionUiMapper() }
}

private val useCaseModule = module {
    single { LoginUseCase(get()) }
    single { RegisterUseCase(get()) }
    single { LogoutUseCase(get()) }
    single { PrepareLogoutUseCase(get(), get()) }
    single { GetUserModeUseCase(get()) }
    single { GetAuthorizedUserIdUseCase(get()) }
    single { SyncStudyDataUseCase(get()) }
    single { SyncAfterMutationUseCase(get(), get(), get()) }
    single { HandleLoginSyncUseCase(get(), get(), get()) }
    single { GetTopicsUseCase(get()) }
    single { DeleteTopicUseCase(get(), get()) }
    single { GetTopicByIdUseCase(get()) }
    single { CreateTopicUseCase(get(), get(), get(), get(), get()) }
    single { GetTasksByTopicUseCase(get()) }
    single { GetFlashcardsByTopicUseCase(get()) }
    single { GetFlashcardBestResultUseCase(get()) }
    single { SaveFlashcardBestResultUseCase(get(), get()) }
    single { DeleteTaskUseCase(get(), get()) }
    single { CreateTaskUseCase(get(), get(), get(), get(), get(), get()) }
    single { GetTaskByIdUseCase(get()) }
    single { GetTaskSolutionUseCase(get()) }
    single { SaveTaskSolutionUseCase(get(), get(), get(), get(), get()) }
    single { DeleteTaskSolutionUseCase(get(), get(), get(), get()) }
    single { UpdateTaskUseCase(get(), get(), get(), get(), get()) }
    single { ChangeTaskStatusUseCase(get(), get(), get(), get()) }
    single { GetAttachmentsByOwnerUseCase(get()) }
    single { AddAttachmentUseCase(get(), get(), get(), get(), get(), get(), get()) }
    single { DeleteAttachmentUseCase(get(), get()) }
}

private val viewModelModule = module {
    viewModel { AppStartViewModel(get()) }
    viewModel { LoginScreenViewModel(get(), get()) }
    viewModel { RegisterScreenViewModel(get()) }
    viewModel { TopicListScreenViewModel(get(), get(), get(), get(), get(), get(), get()) }
    viewModel { CreateTopicScreenViewModel(get()) }
    viewModel { TopicTaskListScreenViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { FlashcardSessionViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { CreateTaskScreenViewModel(get(), get()) }
    viewModel { TaskDetailsScreenViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
}
