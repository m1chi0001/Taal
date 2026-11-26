@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class
)

package pl.lublin.wsei.plan.ui.more

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.text.format.Formatter
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.lublin.wsei.plan.data.update.UpdatePhase
import pl.lublin.wsei.plan.data.update.UpdateViewModel
import pl.lublin.wsei.plan.domain.model.AnnouncementAttachment
import pl.lublin.wsei.plan.domain.model.DeanAnnouncement
import pl.lublin.wsei.plan.ui.adaptive.LocalWseiWindowLayout
import pl.lublin.wsei.plan.ui.adaptive.WseiTwoPane
import pl.lublin.wsei.plan.ui.adaptive.wseiBottomClearance
import pl.lublin.wsei.plan.ui.adaptive.wseiReadableWidth
import pl.lublin.wsei.plan.ui.adaptive.wseiScreenContentPadding
import pl.lublin.wsei.plan.ui.components.WseiCard
import pl.lublin.wsei.plan.ui.components.WseiChevron
import pl.lublin.wsei.plan.ui.components.WseiEmptyState
import pl.lublin.wsei.plan.ui.components.WseiIconBadge
import pl.lublin.wsei.plan.ui.components.WseiInfoRow
import pl.lublin.wsei.plan.ui.components.WseiInset
import pl.lublin.wsei.plan.ui.components.WseiListEntry
import pl.lublin.wsei.plan.ui.components.WseiListGroup
import pl.lublin.wsei.plan.ui.components.WseiListItem
import pl.lublin.wsei.plan.ui.components.WseiPill
import pl.lublin.wsei.plan.ui.components.WseiScreenFrame
import pl.lublin.wsei.plan.ui.components.WseiSectionTitle
import pl.lublin.wsei.plan.ui.components.WseiSwitch
import pl.lublin.wsei.plan.ui.icons.WseiIcons
import pl.lublin.wsei.plan.ui.navigation.MoreSubView
import pl.lublin.wsei.plan.ui.schedule.AppThemeMode
import pl.lublin.wsei.plan.ui.schedule.ScheduleUiState
import pl.lublin.wsei.plan.ui.theme.M3XCookieShape
import pl.lublin.wsei.plan.ui.theme.WseiHeroShape
import pl.lublin.wsei.plan.ui.theme.WseiMotion
import pl.lublin.wsei.plan.ui.theme.WseiSpacing
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import pl.lublin.wsei.plan.R
import pl.lublin.wsei.plan.ui.theme.WseiInsetShape

@Composable
internal fun AnnouncementsScreen(
    uiState: ScheduleUiState,
    onBack: (() -> Unit)?,
    embedded: Boolean,
    onMarkRead: (DeanAnnouncement) -> Unit,
    onDownloadAttachment: (AnnouncementAttachment) -> Unit,
    onRefresh: () -> Unit
) {
    val openingAttachmentId = uiState.openingAttachmentId
    MorePage(
        title = stringResource(R.string.announcements_title),
        onBack = onBack,
        embedded = embedded,
        onRefresh = onRefresh,
        isRefreshing = uiState.isRefreshing
    ) { itemModifier ->
        if (uiState.announcements.isEmpty()) {
            item(key = "announcements_empty", contentType = "empty") {
                WseiEmptyState(
                    icon = WseiIcons.Campaign,
                    title = stringResource(R.string.announcements_empty_title),
                    message = stringResource(R.string.announcements_empty_message),
                    modifier = itemModifier
                )
            }
        } else {
            items(
                items = uiState.announcements,
                key = { "announcement_${it.id}" },
                contentType = { "announcement_card" }
            ) { announcement ->
                AnnouncementCard(
                    announcement = announcement,
                    openingAttachmentId = openingAttachmentId,
                    onMarkRead = onMarkRead,
                    onDownloadAttachment = onDownloadAttachment,
                    modifier = itemModifier.animateItem(
                        fadeInSpec = WseiMotion.effects(),
                        placementSpec = WseiMotion.spatial(),
                        fadeOutSpec = WseiMotion.fastEffects()
                    )
                )
            }
        }
    }
}

@Composable
private fun AnnouncementCard(
    announcement: DeanAnnouncement,
    openingAttachmentId: Int?,
    onMarkRead: (DeanAnnouncement) -> Unit,
    onDownloadAttachment: (AnnouncementAttachment) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable(announcement.id) { mutableStateOf(false) }
    val unread = !announcement.isRead
    val colors = MaterialTheme.colorScheme
    val attachments = announcement.attachments

    WseiCard(
        onClick = {
            expanded = !expanded
            if (unread) onMarkRead(announcement)
        },
        containerColor = if (unread) colors.surfaceContainerHigh else colors.surfaceContainer,
        // A single size animation for the whole card: the body text and the attachment list
        // change height together, without a second (competing) AnimatedVisibility resize.
        modifier = modifier.animateContentSize(WseiMotion.size())
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WseiSpacing.m)
        ) {
            WseiIconBadge(
                icon = if (announcement.hasAttachment || attachments.isNotEmpty()) WseiIcons.AttachFile else WseiIcons.Campaign,
                shape = if (unread) M3XCookieShape else CircleShape,
                containerColor = if (unread) colors.primary else colors.surfaceContainerHighest,
                contentColor = if (unread) colors.onPrimary else colors.onSurfaceVariant
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(WseiSpacing.xxs)
            ) {
                Text(
                    text = stringResource(
                        R.string.announcements_meta,
                        announcement.authorDepartment,
                        announcement.publishedAtIso
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (unread) colors.primary else colors.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = announcement.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (unread) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = if (expanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (unread) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(colors.primary)
                )
            }
        }

        Text(
            text = if (expanded) announcement.fullText else announcement.bodySnippet,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            maxLines = if (expanded) Int.MAX_VALUE else 3,
            overflow = TextOverflow.Ellipsis
        )

        if (attachments.isNotEmpty()) {
            if (expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(WseiSpacing.xs)) {
                    Text(
                        text = stringResource(R.string.announcements_attachments_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(start = WseiSpacing.xs, bottom = WseiSpacing.xxs)
                    )
                    attachments.forEach { attachment ->
                        AttachmentButton(
                            attachment = attachment,
                            isOpening = openingAttachmentId == attachment.id,
                            // The ViewModel opens one file at a time; the others wait.
                            enabled = openingAttachmentId == null,
                            onClick = { onDownloadAttachment(attachment) }
                        )
                    }
                }
            } else {
                WseiPill(
                    text = pluralStringResource(
                        R.plurals.announcements_attachments_count,
                        attachments.size,
                        attachments.size
                    ),
                    icon = WseiIcons.AttachFile,
                    containerColor = colors.secondaryContainer,
                    contentColor = colors.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun AttachmentButton(
    attachment: AnnouncementAttachment,
    isOpening: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val openDescription = stringResource(R.string.announcements_attachment_open_cd, attachment.fileName)
    val openingDescription = stringResource(R.string.announcements_attachment_opening)
    val extension = remember(attachment.fileName) { attachmentExtension(attachment.fileName) }
    val details = remember(extension, attachment.sizeBytes, context) {
        listOfNotNull(
            extension?.uppercase(),
            attachment.sizeBytes.takeIf { it > 0 }?.let { Formatter.formatShortFileSize(context, it) }
        ).joinToString(" · ")
    }
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        shape = WseiInsetShape,
        contentPadding = PaddingValues(start = WseiSpacing.l, end = WseiSpacing.m, top = WseiSpacing.s, bottom = WseiSpacing.s),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .semantics {
                contentDescription = if (details.isEmpty()) openDescription else "$openDescription, $details"
                if (isOpening) stateDescription = openingDescription
            }
    ) {
        Icon(
            imageVector = attachmentTypeIcon(extension),
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = WseiSpacing.s)
        ) {
            Text(
                text = attachment.fileName,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.MiddleEllipsis
            )
            if (details.isNotEmpty()) {
                Text(
                    text = details,
                    style = MaterialTheme.typography.labelSmall,
                    color = LocalContentColor.current.copy(alpha = 0.72f),
                    maxLines = 1
                )
            }
        }
        Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) {
            AnimatedContent(
                targetState = isOpening,
                transitionSpec = {
                    (fadeIn(WseiMotion.effects()) + scaleIn(WseiMotion.fastSpatial(), initialScale = 0.6f)) togetherWith
                        fadeOut(WseiMotion.fastEffects())
                },
                label = "attachmentOpening"
            ) { opening ->
                if (opening) {
                    LoadingIndicator(
                        modifier = Modifier.size(28.dp),
                        color = colors.primary
                    )
                } else {
                    Icon(
                        imageVector = WseiIcons.Download,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/** Extension of an attachment name (lower-case, without the dot), or null when it has none. */
private fun attachmentExtension(fileName: String): String? =
    fileName.substringAfterLast('.', "").lowercase()
        .takeIf { it.isNotEmpty() && it.length <= 5 && it.all(Char::isLetterOrDigit) && it.any(Char::isLetter) }

/** File-type symbol for the attachment row; the generic paper clip for unknown types. */
private fun attachmentTypeIcon(extension: String?): ImageVector = when (extension) {
    "pdf" -> PdfFileIcon
    "png", "jpg", "jpeg", "gif", "webp", "bmp", "heic", "tif", "tiff", "svg" -> ImageFileIcon
    "xls", "xlsx", "xlsm", "ods", "csv", "tsv" -> SpreadsheetFileIcon
    "ppt", "pptx", "pps", "ppsx", "odp" -> SlidesFileIcon
    "doc", "docx", "docm", "odt", "rtf", "txt", "md" -> DocumentFileIcon
    "zip", "rar", "7z" -> ArchiveFileIcon
    else -> WseiIcons.AttachFile
}

private fun fileTypeSymbol(name: String, pathData: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = addPathNodes(pathData),
        fill = SolidColor(Color.Black)
    ).build()

/** Material Icons „picture_as_pdf”. */
private val PdfFileIcon: ImageVector by lazy {
    fileTypeSymbol(
        "PictureAsPdf",
        "M20,2H8c-1.1,0 -2,0.9 -2,2v12c0,1.1 0.9,2 2,2h12c1.1,0 2,-0.9 2,-2V4c0,-1.1 -0.9,-2 -2,-2z" +
            "M11.5,9.5c0,0.83 -0.67,1.5 -1.5,1.5H9v2H7.5V7H10c0.83,0 1.5,0.67 1.5,1.5v1z" +
            "M16.5,11.5c0,0.83 -0.67,1.5 -1.5,1.5h-2.5V7H15c0.83,0 1.5,0.67 1.5,1.5v3z" +
            "M20.5,8.5H19v1h1.5V11H19v2h-1.5V7h3v1.5z" +
            "M9,9.5h1v-1H9v1z" +
            "M4,6H2v14c0,1.1 0.9,2 2,2h14v-2H4V6z" +
            "M14,11.5h1v-3h-1v3z"
    )
}

/** Material Icons „image”. */
private val ImageFileIcon: ImageVector by lazy {
    fileTypeSymbol(
        "Image",
        "M21,19V5c0,-1.1 -0.9,-2 -2,-2H5c-1.1,0 -2,0.9 -2,2v14c0,1.1 0.9,2 2,2h14c1.1,0 2,-0.9 2,-2z" +
            "M8.5,13.5l2.5,3.01L14.5,12l4.5,6H5l3.5,-4.5z"
    )
}

/** Material Icons „table_chart”. */
private val SpreadsheetFileIcon: ImageVector by lazy {
    fileTypeSymbol(
        "TableChart",
        "M10,10.02h5V21h-5z" +
            "M17,21h3c1.1,0 2,-0.9 2,-2v-9h-5v11z" +
            "M20,3H5c-1.1,0 -2,0.9 -2,2v3h19V5c0,-1.1 -0.9,-2 -2,-2z" +
            "M3,19c0,1.1 0.9,2 2,2h3V10H3v9z"
    )
}

/** Material Icons „slideshow”. */
private val SlidesFileIcon: ImageVector by lazy {
    fileTypeSymbol(
        "Slideshow",
        "M10,8v8l5,-4 -5,-4z" +
            "M19,3H5c-1.1,0 -2,0.9 -2,2v14c0,1.1 0.9,2 2,2h14c1.1,0 2,-0.9 2,-2V5c0,-1.1 -0.9,-2 -2,-2z" +
            "M19,19H5V5h14v14z"
    )
}

/** Material Icons „description”. */
private val DocumentFileIcon: ImageVector by lazy {
    fileTypeSymbol(
        "Description",
        "M14,2H6c-1.1,0 -1.99,0.9 -1.99,2L4,20c0,1.1 0.89,2 1.99,2H18c1.1,0 2,-0.9 2,-2V8l-6,-6z" +
            "M16,18H8v-2h8v2z" +
            "M16,14H8v-2h8v2z" +
            "M13,9V3.5L18.5,9H13z"
    )
}

/** Material Icons „archive”. */
private val ArchiveFileIcon: ImageVector by lazy {
    fileTypeSymbol(
        "Archive",
        "M20.54,5.23l-1.39,-1.68C18.88,3.21 18.47,3 18,3H6c-0.47,0 -0.88,0.21 -1.16,0.55L3.46,5.23" +
            "C3.17,5.57 3,6.02 3,6.5V19c0,1.1 0.9,2 2,2h14c1.1,0 2,-0.9 2,-2V6.5c0,-0.48 -0.17,-0.93 -0.46,-1.27z" +
            "M12,17.5L6.5,12H10v-2h4v2h3.5L12,17.5z" +
            "M5.12,5l0.81,-1h12l0.94,1H5.12z"
    )
}

// -----------------------------------------------------------------------------------------
// Ustawienia
// -----------------------------------------------------------------------------------------
