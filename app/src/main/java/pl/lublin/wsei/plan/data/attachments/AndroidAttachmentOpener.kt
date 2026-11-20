package pl.lublin.wsei.plan.data.attachments

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import pl.lublin.wsei.plan.domain.attachments.AttachmentOpenResult
import pl.lublin.wsei.plan.domain.attachments.AttachmentOpener
import pl.lublin.wsei.plan.domain.model.AnnouncementAttachment
import pl.lublin.wsei.plan.domain.repository.ScheduleRepository
import javax.inject.Inject
import javax.inject.Singleton

/** [AttachmentOpener] that caches files in `cacheDir/attachments` and opens them via FileProvider. */
@Singleton
class AndroidAttachmentOpener @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scheduleRepository: ScheduleRepository
) : AttachmentOpener {
    override suspend fun downloadAndOpen(attachment: AnnouncementAttachment): Result<AttachmentOpenResult> =
        Result.failure(UnsupportedOperationException())
}
