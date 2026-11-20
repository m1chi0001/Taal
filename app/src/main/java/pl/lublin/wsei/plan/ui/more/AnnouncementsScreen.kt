@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class
)

package pl.lublin.wsei.plan.ui.more

import android.content.Intent
import android.net.Uri
import android.os.Build
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
        title = "Ogłoszenia",
        onBack = onBack,
        embedded = embedded,
        onRefresh = onRefresh,
        isRefreshing = uiState.isRefreshing
    ) { itemModifier ->
        if (uiState.announcements.isEmpty()) {
            item(key = "announcements_empty", contentType = "empty") {
                WseiEmptyState(
                    icon = WseiIcons.Campaign,
                    title = "Brak ogłoszeń",
                    message = "Nowe wiadomości z dziekanatu pojawią się tutaj.",
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
                    text = "${announcement.authorDepartment} • ${announcement.publishedAtIso}",
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
    val openDescription = stringResource(R.string.announcements_attachment_open_cd, attachment.fileName)
    val openingDescription = stringResource(R.string.announcements_attachment_opening)
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        shape = WseiInsetShape,
        contentPadding = PaddingValues(start = WseiSpacing.l, end = WseiSpacing.m, top = WseiSpacing.s, bottom = WseiSpacing.s),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .semantics {
                contentDescription = openDescription
                if (isOpening) stateDescription = openingDescription
            }
    ) {
        Icon(
            imageVector = WseiIcons.AttachFile,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = attachment.fileName,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.MiddleEllipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = WseiSpacing.s)
        )
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

// -----------------------------------------------------------------------------------------
// Ustawienia
// -----------------------------------------------------------------------------------------
