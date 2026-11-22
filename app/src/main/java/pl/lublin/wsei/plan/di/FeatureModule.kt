package pl.lublin.wsei.plan.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import pl.lublin.wsei.plan.data.attachments.AndroidAttachmentOpener
import pl.lublin.wsei.plan.data.calendar.AndroidCalendarSyncManager
import pl.lublin.wsei.plan.domain.attachments.AttachmentOpener
import pl.lublin.wsei.plan.domain.calendar.CalendarSyncManager
import pl.lublin.wsei.plan.data.update.GitHubUpdateRepository
import pl.lublin.wsei.plan.domain.update.AppUpdateRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class FeatureModule {
    @Binds
    abstract fun bindCalendarSyncManager(impl: AndroidCalendarSyncManager): CalendarSyncManager

    @Binds
    abstract fun bindAttachmentOpener(impl: AndroidAttachmentOpener): AttachmentOpener

    @Binds
    abstract fun bindAppUpdateRepository(impl: GitHubUpdateRepository): AppUpdateRepository
}
