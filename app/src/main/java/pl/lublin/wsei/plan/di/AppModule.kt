package pl.lublin.wsei.plan.di

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import pl.lublin.wsei.plan.data.local.AcademicSnapshotDao
import pl.lublin.wsei.plan.data.local.ScheduleDao
import pl.lublin.wsei.plan.data.local.ScheduleDatabase
import pl.lublin.wsei.plan.data.remote.api.ArpAutoReauthInterceptor
import pl.lublin.wsei.plan.data.remote.api.ArpSessionCookieJar
import pl.lublin.wsei.plan.data.remote.api.ArpTokenRefresher
import pl.lublin.wsei.plan.data.repository.ScheduleRepositoryImpl
import pl.lublin.wsei.plan.domain.repository.ScheduleRepository
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryBindModule {

    @Binds
    @Singleton
    abstract fun bindScheduleRepository(
        impl: ScheduleRepositoryImpl
    ): ScheduleRepository
}

@Module
@InstallIn(SingletonComponent::class)
object AppNetworkAndDatabaseModule {

    @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
        encodeDefaults = true
    }

    /**
     * REST client for the APR API. Redirects are NOT followed: a 302 to the login page is a session
     * expiry signal, handled by [ArpAutoReauthInterceptor] (previously the comment said so, but the
     * interceptor never looked at 3xx and the refresh provider was hard-wired to `false`).
     */
    @Provides
    @Singleton
    fun provideKtorHttpClient(
        cookieJar: ArpSessionCookieJar,
        tokenRefresher: ArpTokenRefresher,
        json: Json
    ): HttpClient = HttpClient(OkHttp) {
        expectSuccess = false // status codes are validated in decodeAbp()
        engine {
            config {
                cookieJar(cookieJar)
                followRedirects(false)
                followSslRedirects(false)
                retryOnConnectionFailure(true)
                connectTimeout(15, TimeUnit.SECONDS)
                readTimeout(30, TimeUnit.SECONDS)
                addInterceptor(ArpAutoReauthInterceptor(cookieJar, tokenRefresher))
            }
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 45_000
        }
        install(ContentNegotiation) {
            json(json)
        }
    }

    @Provides
    @Singleton
    fun provideScheduleDatabase(
        @ApplicationContext context: Context
    ): ScheduleDatabase {
        return Room.databaseBuilder(
            context,
            ScheduleDatabase::class.java,
            "wsei_schedule.db"
        ).fallbackToDestructiveMigration(dropAllTables = true).build()
    }

    @Provides
    fun provideScheduleDao(database: ScheduleDatabase): ScheduleDao = database.scheduleDao()

    @Provides
    fun provideAcademicSnapshotDao(database: ScheduleDatabase): AcademicSnapshotDao =
        database.academicSnapshotDao()
}
