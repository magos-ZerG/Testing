package com.z23u184.studymate.data.di

import androidx.room.Room
import com.z23u184.studymate.data.db.migration.MIGRATION_1_2
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.z23u184.studymate.data.datasource.local.AttachmentLocalDataSource
import com.z23u184.studymate.data.datasource.local.FlashcardBestResultLocalDataSource
import com.z23u184.studymate.data.datasource.local.SyncQueueLocalDataSource
import com.z23u184.studymate.data.datasource.local.SyncSessionLocalDataSource
import com.z23u184.studymate.data.datasource.local.TaskLocalDataSource
import com.z23u184.studymate.data.datasource.local.TaskSolutionLocalDataSource
import com.z23u184.studymate.data.datasource.local.TopicLocalDataSource
import com.z23u184.studymate.data.file.AttachmentRemoteFileDownloader
import com.z23u184.studymate.data.db.StudyMateDatabase
import com.z23u184.studymate.data.file.AttachmentFileManager
import com.z23u184.studymate.data.network.AuthHeaderInterceptor
import com.z23u184.studymate.data.network.AuthSessionManager
import com.z23u184.studymate.data.network.NetworkLoggingInterceptor
import com.z23u184.studymate.data.network.TokenRefreshAuthenticator
import com.z23u184.studymate.data.network.api.StudyMateAuthApi
import com.z23u184.studymate.data.network.api.StudyMateAuthorizedAuthApi
import com.z23u184.studymate.data.network.api.StudyMateSyncApi
import com.z23u184.studymate.data.repository.AttachmentRepositoryImpl
import com.z23u184.studymate.data.repository.FlashcardRepositoryImpl
import com.z23u184.studymate.data.repository.AuthRepositoryImpl
import com.z23u184.studymate.data.repository.SessionRepositoryImpl
import com.z23u184.studymate.data.repository.TaskRepositoryImpl
import com.z23u184.studymate.data.repository.TaskSolutionRepositoryImpl
import com.z23u184.studymate.data.repository.TopicRepositoryImpl
import com.z23u184.studymate.data.session.AuthTokenLocalDataSource
import com.z23u184.studymate.data.session.LocalStudyDataCleaner
import com.z23u184.studymate.data.session.SessionLocalDataSource
import com.z23u184.studymate.data.session.SessionPreferencesDataSource
import com.z23u184.studymate.data.sync.StudyDataSyncCoordinator
import com.z23u184.studymate.data.sync.gateway.SyncGatewayImpl
import com.z23u184.studymate.data.sync.merge.SyncConflictResolver
import com.z23u184.studymate.data.sync.queue.QueueMutationMerger
import com.z23u184.studymate.data.sync.queue.SyncQueuePlanner
import com.z23u184.studymate.data.sync.remote.RetrofitSyncRemoteDataSource
import com.z23u184.studymate.data.sync.remote.SyncRemoteDataSource
import com.z23u184.studymate.data.sync.worker.SyncWorkScheduler
import com.z23u184.studymate.data.sync.worker.WorkManagerSyncWorkScheduler
import com.z23u184.studymate.domain.repository.AttachmentRepository
import com.z23u184.studymate.domain.repository.AuthRepository
import com.z23u184.studymate.domain.repository.FlashcardRepository
import com.z23u184.studymate.domain.repository.SessionRepository
import com.z23u184.studymate.domain.repository.TaskRepository
import com.z23u184.studymate.domain.repository.TaskSolutionRepository
import com.z23u184.studymate.domain.repository.TopicRepository
import com.z23u184.studymate.domain.service.SyncGateway
import com.z23u184.studymate.domain.logging.StudyMateLogger
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

private val BASE_URL_QUALIFIER = named("studymate_base_url")
private val AUTH_RETROFIT_QUALIFIER = named("auth_retrofit")
private val MAIN_RETROFIT_QUALIFIER = named("main_retrofit")
private val AUTH_OKHTTP_QUALIFIER = named("auth_okhttp")
private val MAIN_OKHTTP_QUALIFIER = named("main_okhttp")

fun studyMateDataModule(
    baseUrl: String,
) = module {
    single(BASE_URL_QUALIFIER) { normalizeBaseUrl(baseUrl) }

    single {
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            encodeDefaults = true
        }
    }

    single {
        HttpLoggingInterceptor { message ->
            StudyMateLogger.d("OkHttp", message.sanitizeForLog())
        }.apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
    }
    single { NetworkLoggingInterceptor() }
    single { SessionPreferencesDataSource(get()) }
    single { AttachmentFileManager(get()) }
    single<SessionLocalDataSource> { get<SessionPreferencesDataSource>() }
    single<AuthTokenLocalDataSource> { get<SessionPreferencesDataSource>() }
    single { AuthHeaderInterceptor(get()) }

    single(AUTH_OKHTTP_QUALIFIER) {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(get<NetworkLoggingInterceptor>())
            .addInterceptor(get<HttpLoggingInterceptor>())
            .build()
    }

    single(AUTH_RETROFIT_QUALIFIER) {
        Retrofit.Builder()
            .baseUrl(get<String>(BASE_URL_QUALIFIER))
            .client(get<OkHttpClient>(AUTH_OKHTTP_QUALIFIER))
            .addConverterFactory(get<Json>().asConverterFactory("application/json".toMediaType()))
            .build()
    }

    single<StudyMateAuthApi> {
        get<Retrofit>(AUTH_RETROFIT_QUALIFIER).create(StudyMateAuthApi::class.java)
    }

    single {
        TokenRefreshAuthenticator(
            authTokenLocalDataSource = get(),
            authApi = get(),
        )
    }

    single(MAIN_OKHTTP_QUALIFIER) {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(get<NetworkLoggingInterceptor>())
            .addInterceptor(get<HttpLoggingInterceptor>())
            .addInterceptor(get<AuthHeaderInterceptor>())
            .authenticator(get<TokenRefreshAuthenticator>())
            .build()
    }

    single(MAIN_RETROFIT_QUALIFIER) {
        Retrofit.Builder()
            .baseUrl(get<String>(BASE_URL_QUALIFIER))
            .client(get<OkHttpClient>(MAIN_OKHTTP_QUALIFIER))
            .addConverterFactory(get<Json>().asConverterFactory("application/json".toMediaType()))
            .build()
    }

    single<StudyMateSyncApi> {
        get<Retrofit>(MAIN_RETROFIT_QUALIFIER).create(StudyMateSyncApi::class.java)
    }

    single<StudyMateAuthorizedAuthApi> {
        get<Retrofit>(MAIN_RETROFIT_QUALIFIER).create(StudyMateAuthorizedAuthApi::class.java)
    }

    single { AttachmentRemoteFileDownloader(get(), get(), get()) }
    single<SyncRemoteDataSource> { RetrofitSyncRemoteDataSource(get()) }
    single { LocalStudyDataCleaner(get(), get()) }
    single { AuthSessionManager(get(), get(), get(), get(), get(), get()) }

    single {
        Room.databaseBuilder(get(), StudyMateDatabase::class.java, "studymate.db")
            .addMigrations(MIGRATION_1_2)
            .fallbackToDestructiveMigration(false)
            .build()
    }

    single { get<StudyMateDatabase>().topicDao() }
    single { get<StudyMateDatabase>().taskDao() }
    single { get<StudyMateDatabase>().taskSolutionDao() }
    single { get<StudyMateDatabase>().attachmentDao() }
    single { get<StudyMateDatabase>().flashcardBestResultDao() }
    single { get<StudyMateDatabase>().syncQueueDao() }
    single { get<StudyMateDatabase>().syncSessionDao() }

    single { TopicLocalDataSource(get()) }
    single { TaskLocalDataSource(get()) }
    single { TaskSolutionLocalDataSource(get()) }
    single { AttachmentLocalDataSource(get()) }
    single { FlashcardBestResultLocalDataSource(get()) }
    single { SyncQueueLocalDataSource(get()) }
    single { SyncSessionLocalDataSource(get()) }

    single { QueueMutationMerger() }
    single { SyncQueuePlanner(get()) }
    single { SyncConflictResolver() }

    single {
        StudyDataSyncCoordinator(
            database = get(),
            topicLocalDataSource = get(),
            taskLocalDataSource = get(),
            taskSolutionLocalDataSource = get(),
            attachmentLocalDataSource = get(),
            flashcardBestResultLocalDataSource = get(),
            syncQueueLocalDataSource = get(),
            syncSessionLocalDataSource = get(),
            sessionLocalDataSource = get(),
            syncRemoteDataSource = get(),
            syncConflictResolver = get(),
        )
    }

    single<SyncWorkScheduler> { WorkManagerSyncWorkScheduler(get()) }
    single<SyncGateway> { SyncGatewayImpl(get(), get()) }

    single<TopicRepository> { TopicRepositoryImpl(get(), get(), get(), get(), get(), get(), get(), get()) }
    single<TaskRepository> { TaskRepositoryImpl(get(), get(), get(), get(), get(), get()) }
    single<TaskSolutionRepository> { TaskSolutionRepositoryImpl(get(), get(), get(), get()) }
    single<AttachmentRepository> { AttachmentRepositoryImpl(get(), get(), get(), get()) }
    single<FlashcardRepository> { FlashcardRepositoryImpl(get(), get(), get(), get(), get(), get(), get()) }
    single<SessionRepository> { SessionRepositoryImpl(get()) }
    single<AuthRepository> { AuthRepositoryImpl(get()) }
}

private fun normalizeBaseUrl(baseUrl: String): String =
    if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

private fun String.sanitizeForLog(): String =
    replace(Regex("Authorization: Bearer [^\\n]+"), "Authorization: Bearer ***")
        .replace(Regex("accessToken=[^,)}\\s]+"), "accessToken=***")
        .replace(Regex("refreshToken=[^,)}\\s]+"), "refreshToken=***")
