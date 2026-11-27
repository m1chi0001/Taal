package pl.lublin.wsei.plan.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import pl.lublin.wsei.plan.data.attachments.AndroidAttachmentOpener
import pl.lublin.wsei.plan.data.calendar.AndroidCalendarSyncManager
import pl.lublin.wsei.plan.data.repository.ScheduleRepositoryImpl
import pl.lublin.wsei.plan.data.update.GitHubUpdateRepository
import pl.lublin.wsei.plan.domain.attachments.AttachmentOpener
import pl.lublin.wsei.plan.domain.calendar.CalendarSyncManager
import pl.lublin.wsei.plan.domain.repository.ScheduleRepository
import pl.lublin.wsei.plan.domain.update.AppUpdateRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindScheduleRepository(impl: ScheduleRepositoryImpl): ScheduleRepository

    @Binds
    @Singleton
    abstract fun bindCalendarSyncManager(impl: AndroidCalendarSyncManager): CalendarSyncManager

    @Binds
    @Singleton
    abstract fun bindAttachmentOpener(impl: AndroidAttachmentOpener): AttachmentOpener

    @Binds
    @Singleton
    abstract fun bindAppUpdateRepository(impl: GitHubUpdateRepository): AppUpdateRepository
}
