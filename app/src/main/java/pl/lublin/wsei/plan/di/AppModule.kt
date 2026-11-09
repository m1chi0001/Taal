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
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import pl.lublin.wsei.plan.data.local.ScheduleDao
import pl.lublin.wsei.plan.data.local.ScheduleDatabase
import pl.lublin.wsei.plan.data.remote.api.ArpAutoReauthInterceptor
import pl.lublin.wsei.plan.data.remote.api.ArpSessionCookieJar
import pl.lublin.wsei.plan.data.repository.ScheduleRepositoryImpl
import pl.lublin.wsei.plan.domain.repository.ScheduleRepository
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
    }

    @Provides
    @Singleton
    fun provideKtorHttpClient(
        cookieJar: ArpSessionCookieJar,
        json: Json
    ): HttpClient {
        return HttpClient(OkHttp) {
            engine {
                config {
                    cookieJar(cookieJar)
                    followRedirects(false) // Handled by ArpAutoReauthInterceptor to catch /Konto/Logowanie redirects
                    addInterceptor(
                        ArpAutoReauthInterceptor(
                            cookieJar = cookieJar,
                            tokenRefreshProvider = {
                                // Re-authenticates using encrypted credentials stored in DataStore
                                false
                            }
                        )
                    )
                }
            }
            install(ContentNegotiation) {
                json(json)
            }
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
}
