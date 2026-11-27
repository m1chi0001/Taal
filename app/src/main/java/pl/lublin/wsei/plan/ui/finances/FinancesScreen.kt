@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class
)

package pl.lublin.wsei.plan.ui.finances

import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PersistableBundle
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import pl.lublin.wsei.plan.domain.model.FinancialObligation
import pl.lublin.wsei.plan.domain.model.ScholarshipApplication
import pl.lublin.wsei.plan.domain.model.ScholarshipStatusKind
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import pl.lublin.wsei.plan.domain.model.formatPolishBankAccount
import pl.lublin.wsei.plan.ui.adaptive.LocalWseiWindowLayout
import pl.lublin.wsei.plan.ui.adaptive.WseiTwoPane
import pl.lublin.wsei.plan.ui.adaptive.wseiReadableWidth
import pl.lublin.wsei.plan.ui.adaptive.wseiScreenContentPadding
import pl.lublin.wsei.plan.ui.components.WseiCard
import pl.lublin.wsei.plan.ui.components.WseiCardHeader
import pl.lublin.wsei.plan.ui.components.WseiEmptyState
import pl.lublin.wsei.plan.ui.components.WseiExpandableCard
import pl.lublin.wsei.plan.ui.components.WseiIconBadge
import pl.lublin.wsei.plan.ui.components.WseiInfoRow
import pl.lublin.wsei.plan.ui.components.WseiInset
import pl.lublin.wsei.plan.ui.components.WseiPill
import pl.lublin.wsei.plan.ui.components.WseiScreenFrame
import pl.lublin.wsei.plan.ui.components.WseiScreenScaffold
import pl.lublin.wsei.plan.ui.components.WseiSectionTitle
import pl.lublin.wsei.plan.ui.components.wseiMasked
import pl.lublin.wsei.plan.ui.icons.WseiIcons
import pl.lublin.wsei.plan.ui.schedule.ScheduleUiState
import pl.lublin.wsei.plan.ui.theme.WseiHeroShape
import pl.lublin.wsei.plan.ui.theme.WseiMotion
import pl.lublin.wsei.plan.ui.theme.WseiSpacing
import pl.lublin.wsei.plan.ui.theme.mutedContentColor
import pl.lublin.wsei.plan.ui.theme.tonalLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import pl.lublin.wsei.plan.R

/** `ClipDescription.EXTRA_IS_SENSITIVE` (API 33) — keeps the clipboard preview from showing the value. */
private const val EXTRA_CLIP_IS_SENSITIVE = "android.content.extra.IS_SENSITIVE"

/**
 * Finances: balance and account number, obligations (to pay / paid) and scholarships.
 *
 * On wide windows (tablet, unfolded foldable) the summary and scholarships sit in the left pane
 * and the obligations in the right one; on phones everything is a single list.
 */
@Composable
fun FinancesScreen(
    uiState: ScheduleUiState,
    onInitiateOnlinePayment: (FinancialObligation, (String) -> Unit) -> Unit,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    val windowLayout = LocalWseiWindowLayout.current
    var showScholarshipHistory by rememberSaveable { mutableStateOf(false) }
    val hidden = uiState.isHideSensitiveData

    val sorted = remember(uiState.financials) { uiState.financials.sortedForDisplay() }
    val unpaid = remember(sorted) { sorted.filter { !it.isPaid } }
    val paid = remember(sorted) { sorted.filter { it.isPaid } }
    val unpaidSum = remember(unpaid) { unpaid.sumOf { it.outstandingAmountPln } }
    // Only a real, individual account number from the dean's office is shown — never a guess.
    val accountNumber = remember(uiState.financials) {
        uiState.financials
            .firstOrNull { it.bankAccountNumber.isNotBlank() }
            ?.bankAccountNumber
            ?.let(::formatPolishBankAccount)
    }
    val scholarships = uiState.scholarships

    val payOnline: (FinancialObligation) -> Unit = { item ->
        onInitiateOnlinePayment(item) { paymentUrl ->
            runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(paymentUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }
    val openHistory = { showScholarshipHistory = true }

    if (windowLayout.supportsTwoPane) {
        WseiScreenFrame(
            title = stringResource(R.string.nav_finances),
            onRefresh = onRefresh,
            isRefreshing = uiState.isRefreshing
        ) { scrollModifier ->
            val screenPadding = wseiScreenContentPadding()
            val direction = LocalLayoutDirection.current
            val paneListPadding = PaddingValues(
                top = screenPadding.calculateTopPadding(),
                bottom = screenPadding.calculateBottomPadding()
            )
            WseiTwoPane(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = screenPadding.calculateStartPadding(direction),
                        end = screenPadding.calculateEndPadding(direction)
                    ),
                gap = WseiSpacing.xxl,
                first = {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(scrollModifier),
                        contentPadding = paneListPadding,
                        verticalArrangement = Arrangement.spacedBy(WseiSpacing.betweenCards)
                    ) {
                        summaryItems(unpaidSum, unpaid.size, accountNumber, hidden, Modifier)
                        scholarshipItems(scholarships, hidden, Modifier, openHistory)
                    }
                },
                second = {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(scrollModifier),
                        contentPadding = paneListPadding,
                        verticalArrangement = Arrangement.spacedBy(WseiSpacing.betweenCards)
                    ) {
                        obligationItems(unpaid, paid, hidden, Modifier, payOnline)
                    }
                }
            )
        }
    } else {
        WseiScreenScaffold(
            title = stringResource(R.string.nav_finances),
            onRefresh = onRefresh,
            isRefreshing = uiState.isRefreshing,
            contentPadding = wseiScreenContentPadding()
        ) {
            // On medium windows the cards are capped to a readable width instead of stretching.
            val itemModifier = Modifier.wseiReadableWidth()
            summaryItems(unpaidSum, unpaid.size, accountNumber, hidden, itemModifier)
            obligationItems(unpaid, paid, hidden, itemModifier, payOnline)
            scholarshipItems(scholarships, hidden, itemModifier, openHistory)
        }
    }

    if (showScholarshipHistory) {
        ModalBottomSheet(
            onDismissRequest = { showScholarshipHistory = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            // The history can be longer than the sheet — it must scroll instead of being clipped.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = WseiSpacing.xl)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(WseiSpacing.m)
            ) {
                Text(
                    text = stringResource(R.string.finances_scholarship_history_title),
                    style = MaterialTheme.typography.headlineSmall
                )
                scholarships.forEachIndexed { index, scholarship ->
                    key(scholarship.id, index) {
                        ScholarshipCard(
                            scholarship = scholarship,
                            maskSensitive = hidden,
                            onOpenHistory = null,
                            showAllPayouts = true
                        )
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// List sections (shared by the single- and two-pane layouts)
// -----------------------------------------------------------------------------------------

private fun LazyListScope.summaryItems(
    unpaidSum: Double,
    unpaidCount: Int,
    accountNumber: String?,
    hidden: Boolean,
    itemModifier: Modifier
) {
    item(key = "finance_summary_hero", contentType = "hero") {
        BalanceHeroCard(
            unpaidSum = unpaidSum,
            unpaidCount = unpaidCount,
            accountNumber = accountNumber,
            mask = hidden,
            modifier = itemModifier
        )
    }
}

private fun LazyListScope.obligationItems(
    unpaid: List<FinancialObligation>,
    paid: List<FinancialObligation>,
    hidden: Boolean,
    itemModifier: Modifier,
    onPayOnline: (FinancialObligation) -> Unit
) {
    if (unpaid.isEmpty() && paid.isEmpty()) {
        item(key = "finance_empty", contentType = "empty") {
            WseiEmptyState(
                icon = WseiIcons.AccountBalanceWallet,
                title = stringResource(R.string.finances_empty_title),
                message = stringResource(R.string.finances_empty_message),
                modifier = itemModifier
            )
        }
        return
    }
    if (unpaid.isNotEmpty()) {
        item(key = "section_unpaid", contentType = "section") {
            WseiSectionTitle(text = stringResource(R.string.finances_section_unpaid), trailing = "${unpaid.size}", modifier = itemModifier)
        }
        // Only the nearest due obligation carries the prominent payment button; the others get
        // a compact one until they are expanded.
        val nearestId = unpaid.first().id
        items(items = unpaid, key = { "unpaid_${it.id}" }, contentType = { "obligation" }) { item ->
            ObligationCard(
                item = item,
                maskSensitive = hidden,
                onPayOnline = { onPayOnline(item) },
                isNearestDue = item.id == nearestId,
                modifier = itemModifier
            )
        }
    }
    if (paid.isNotEmpty()) {
        item(key = "section_paid", contentType = "section") {
            WseiSectionTitle(text = stringResource(R.string.finances_section_paid), trailing = "${paid.size}", modifier = itemModifier)
        }
        items(items = paid, key = { "paid_${it.id}" }, contentType = { "obligation" }) { item ->
            ObligationCard(
                item = item,
                maskSensitive = hidden,
                onPayOnline = { onPayOnline(item) },
                modifier = itemModifier
            )
        }
    }
}

private fun LazyListScope.scholarshipItems(
    scholarships: List<ScholarshipApplication>,
    hidden: Boolean,
    itemModifier: Modifier,
    onOpenHistory: () -> Unit
) {
    item(key = "section_scholarships", contentType = "section") {
        WseiSectionTitle(text = stringResource(R.string.finances_section_scholarships), modifier = itemModifier)
    }
    val active = scholarships.firstOrNull()
    if (active != null) {
        item(key = "active_scholarship_card", contentType = "scholarship") {
            ScholarshipCard(
                scholarship = active,
                maskSensitive = hidden,
                onOpenHistory = if (scholarships.size > 1) onOpenHistory else null,
                modifier = itemModifier
            )
        }
    } else {
        item(key = "empty_scholarships", contentType = "empty") {
            WseiEmptyState(
                icon = WseiIcons.WorkspacePremium,
                title = stringResource(R.string.finances_scholarships_empty_title),
                message = stringResource(R.string.finances_scholarships_empty_message),
                modifier = itemModifier
            )
        }
    }
}

// -----------------------------------------------------------------------------------------
// Cards
// -----------------------------------------------------------------------------------------

/** Copies [text] and flags the clip as sensitive, so Android 13+ does not preview it on screen. */
private fun copySensitiveText(context: Context, label: String, text: String): Boolean {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return false
    val clip = ClipData.newPlainText(label, text).apply {
        description.extras = PersistableBundle().apply { putBoolean(EXTRA_CLIP_IS_SENSITIVE, true) }
    }
    return runCatching { clipboard.setPrimaryClip(clip) }.isSuccess
}

@Composable
private fun BalanceHeroCard(
    unpaidSum: Double,
    unpaidCount: Int,
    accountNumber: String?,
    mask: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(2_000)
            copied = false
        }
    }
    val colors = MaterialTheme.colorScheme
    val settled = unpaidCount == 0
    val accountClipLabel = stringResource(R.string.finances_account_number_clip_label)
    val heroContainer = if (settled) colors.primaryContainer else colors.tertiaryContainer
    val contentColor = if (settled) colors.onPrimaryContainer else colors.onTertiaryContainer

    WseiCard(
        modifier = modifier,
        shape = WseiHeroShape,
        containerColor = heroContainer,
        contentColor = contentColor,
        verticalSpacing = WseiSpacing.l
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(WseiSpacing.xs)) {
            Text(
                text = if (settled) {
                    stringResource(R.string.finances_balance)
                } else {
                    stringResource(
                        R.string.finances_to_pay_with_count,
                        pluralStringResource(R.plurals.finances_items_count, unpaidCount, unpaidCount)
                    )
                },
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = wseiMasked(formatPln(unpaidSum), mask),
                style = MaterialTheme.typography.displaySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (settled) {
                WseiPill(
                    text = stringResource(R.string.finances_all_settled),
                    icon = WseiIcons.CheckCircle,
                    containerColor = colors.onPrimaryContainer,
                    contentColor = colors.primaryContainer,
                    emphasized = true
                )
            }
        }

        if (accountNumber != null) {
            val insetContainer = tonalLayer(heroContainer, contentColor, 0.09f)
            WseiInset(containerColor = insetContainer) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(WseiSpacing.m)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(WseiSpacing.xxs)
                    ) {
                        Text(
                            text = stringResource(R.string.finances_account_number),
                            style = MaterialTheme.typography.labelMedium,
                            color = mutedContentColor(contentColor, insetContainer)
                        )
                        Text(
                            text = wseiMasked(accountNumber, mask),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    FilledTonalIconButton(
                        onClick = {
                            copied = copySensitiveText(context, accountClipLabel, accountNumber)
                        },
                        shapes = IconButtonDefaults.shapes(),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = contentColor,
                            contentColor = heroContainer
                        )
                    ) {
                        AnimatedContent(
                            targetState = copied,
                            transitionSpec = {
                                (fadeIn(WseiMotion.effects()) + scaleIn(WseiMotion.fastSpatial(), initialScale = 0.6f)) togetherWith
                                    fadeOut(WseiMotion.effects())
                            },
                            label = "copyIcon"
                        ) { isCopied ->
                            Icon(
                                imageVector = if (isCopied) WseiIcons.CheckCircle else WseiIcons.ContentCopy,
                                contentDescription = stringResource(
                                    if (isCopied) R.string.finances_account_copied_cd else R.string.finances_account_copy_cd
                                ),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Obligation card: collapsed it shows the due date, title, amount and a payment action; expanded
 * it adds the settlement details (zero-value rows are left out).
 *
 * @param isNearestDue the nearest unpaid obligation gets a full-width tonal payment button; the
 *   others get a compact text button until expanded.
 */
@Composable
private fun ObligationCard(
    item: FinancialObligation,
    maskSensitive: Boolean,
    onPayOnline: () -> Unit,
    modifier: Modifier = Modifier,
    isNearestDue: Boolean = false
) {
    var expanded by rememberSaveable(item.id) { mutableStateOf(false) }
    val dueThisMonth = remember(item) { item.isDueThisMonth() }
    val colors = MaterialTheme.colorScheme
    val containerColor = if (dueThisMonth) colors.errorContainer else colors.surfaceContainer
    val contentColor = if (dueThisMonth) colors.onErrorContainer else colors.onSurface
    val amount = wseiMasked(
        if (item.isPaid) formatMoneyText(item.amountFormatted) else formatPln(item.outstandingAmountPln),
        maskSensitive
    )
    val canPay = item.payOnlineAvailable && !item.isPaid

    WseiExpandableCard(
        expanded = expanded,
        onToggle = { expanded = !expanded },
        containerColor = containerColor,
        contentColor = contentColor,
        modifier = if (item.isPaid) modifier.alpha(0.85f) else modifier,
        pinned = if (canPay) {
            {
                PayOnlineAction(
                    amount = amount,
                    prominent = isNearestDue || expanded,
                    // On the error-tinted card the button is an opaque tonal overlay of the card's content colour.
                    tonalContainer = if (dueThisMonth) tonalLayer(containerColor, contentColor, 0.14f) else colors.secondaryContainer,
                    tonalContent = if (dueThisMonth) contentColor else colors.onSecondaryContainer,
                    secondaryContainer = if (dueThisMonth) tonalLayer(containerColor, contentColor, 0.08f) else colors.surfaceContainerHigh,
                    secondaryBorder = if (dueThisMonth) tonalLayer(containerColor, contentColor, 0.24f) else colors.outlineVariant,
                    textColor = if (dueThisMonth) contentColor else colors.primary,
                    onClick = onPayOnline
                )
            }
        } else {
            null
        },
        header = {
            WseiIconBadge(
                icon = if (item.isPaid) WseiIcons.CheckCircle else WseiIcons.AccountBalanceWallet,
                containerColor = when {
                    dueThisMonth -> colors.onErrorContainer
                    item.isPaid -> colors.surfaceContainerHighest
                    else -> colors.primaryContainer
                },
                contentColor = when {
                    dueThisMonth -> colors.errorContainer
                    item.isPaid -> colors.onSurfaceVariant
                    else -> colors.onPrimaryContainer
                }
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(WseiSpacing.xxs)
            ) {
                Text(
                    text = when {
                        dueThisMonth -> stringResource(R.string.finances_due_this_month, item.dueDateIso)
                        item.isPaid -> stringResource(R.string.finances_paid_on, item.paymentDateIso ?: item.dueDateIso)
                        else -> stringResource(R.string.finances_due_on, item.dueDateIso)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        dueThisMonth -> contentColor
                        item.isPaid -> colors.onSurfaceVariant
                        else -> colors.primary
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = amount,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1
                )
            }
        },
        body = {
            val insetContainer = if (dueThisMonth) tonalLayer(containerColor, contentColor, 0.08f) else colors.surfaceContainerLow
            WseiInset(containerColor = insetContainer) {
                val m: (String) -> String = { wseiMasked(formatMoneyText(it), maskSensitive) }
                WseiInfoRow(stringResource(R.string.finances_status), item.statusLabel)
                WseiInfoRow(stringResource(R.string.finances_due_date), item.dueDateIso)
                item.paymentDateIso?.let { WseiInfoRow(stringResource(R.string.finances_payment_date), it) }
                HorizontalDivider(color = tonalLayer(insetContainer, contentColor, 0.14f))
                WseiInfoRow(stringResource(R.string.finances_amount_charged), m(item.amountFormatted))
                // Interest, write-offs and discounts are almost always zero: only real values are listed.
                listOf(
                    R.string.finances_interest_charged to item.interestFormatted,
                    R.string.finances_interest_paid to item.accumulatedInterestFormatted,
                    R.string.finances_amortization to item.amortizationFormatted,
                    R.string.finances_discount to item.discountFormatted,
                    R.string.finances_expired to item.expiredFormatted
                ).forEach { (label, value) ->
                    if (value.isNotBlank() && !isZeroMoneyText(value)) {
                        WseiInfoRow(stringResource(label), m(value))
                    }
                }
            }
        }
    )
}

/**
 * Online payment action of an obligation card. [prominent] (nearest due item or an expanded card)
 * is a full-width filled tonal button with the amount; later installments use a calm full-width
 * tonal outlined button with expressive press-shape morphing instead of floating corner text.
 */
@Composable
private fun PayOnlineAction(
    amount: String,
    prominent: Boolean,
    tonalContainer: Color,
    tonalContent: Color,
    secondaryContainer: Color,
    secondaryBorder: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    if (prominent) {
        FilledTonalButton(
            onClick = onClick,
            shapes = ButtonDefaults.shapes(),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = tonalContainer,
                contentColor = tonalContent
            ),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
        ) {
            Icon(
                imageVector = WseiIcons.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(WseiSpacing.s))
            Text(
                text = stringResource(R.string.finances_pay_online, amount),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            shapes = ButtonDefaults.shapes(),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = secondaryContainer,
                contentColor = textColor
            ),
            border = BorderStroke(1.dp, secondaryBorder),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 46.dp)
        ) {
            Icon(
                imageVector = WseiIcons.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(WseiSpacing.s))
            Text(
                text = stringResource(R.string.finances_pay_online_short),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Theme colours (container to content) for a scholarship status badge. */
private data class ScholarshipStatusColors(val container: Color, val content: Color)

@Composable
private fun scholarshipStatusColors(kind: ScholarshipStatusKind): ScholarshipStatusColors {
    val colors = MaterialTheme.colorScheme
    return when (kind) {
        ScholarshipStatusKind.ACCEPTED -> ScholarshipStatusColors(colors.primaryContainer, colors.onPrimaryContainer)
        ScholarshipStatusKind.GRANTED -> ScholarshipStatusColors(colors.tertiaryContainer, colors.onTertiaryContainer)
        ScholarshipStatusKind.REJECTED -> ScholarshipStatusColors(colors.errorContainer, colors.onErrorContainer)
        ScholarshipStatusKind.UNDER_REVIEW,
        ScholarshipStatusKind.SUBMITTED -> ScholarshipStatusColors(colors.secondaryContainer, colors.onSecondaryContainer)
        ScholarshipStatusKind.UNKNOWN -> ScholarshipStatusColors(colors.surfaceVariant, colors.onSurfaceVariant)
    }
}

private fun scholarshipStatusIcon(kind: ScholarshipStatusKind): ImageVector? = when (kind) {
    ScholarshipStatusKind.ACCEPTED, ScholarshipStatusKind.GRANTED -> WseiIcons.CheckCircle
    ScholarshipStatusKind.REJECTED -> WseiIcons.Info
    ScholarshipStatusKind.UNDER_REVIEW, ScholarshipStatusKind.SUBMITTED -> WseiIcons.Schedule
    ScholarshipStatusKind.UNKNOWN -> null
}

/**
 * Scholarship application card: "№ 70" + title, a prominent `Stan` badge, application details,
 * and the decision / amounts only when the backend actually returned them (nothing is invented).
 * The application number, decision number and amounts are masked in privacy mode.
 */
@Composable
private fun ScholarshipCard(
    scholarship: ScholarshipApplication,
    maskSensitive: Boolean,
    onOpenHistory: (() -> Unit)?,
    modifier: Modifier = Modifier,
    showAllPayouts: Boolean = false
) {
    val colors = MaterialTheme.colorScheme
    val kind = scholarship.statusKind
    val statusColors = scholarshipStatusColors(kind)
    val m: (String) -> String = { wseiMasked(it, maskSensitive) }
    val numberLabel = scholarship.displayNumber?.let {
        stringResource(R.string.finances_scholarship_number, m(it.toString()))
    }
    val overline = listOfNotNull(numberLabel, scholarship.semesterLabel.ifBlank { null })
        .takeIf { it.isNotEmpty() }
        ?.let { parts ->
            if (parts.size == 2) stringResource(R.string.finances_scholarship_overline, parts[0], parts[1]) else parts[0]
        }

    WseiCard(modifier = modifier, containerColor = colors.surfaceContainer) {
        WseiCardHeader(
            title = scholarship.title,
            overline = overline,
            icon = WseiIcons.WorkspacePremium,
            iconContainerColor = statusColors.container,
            iconContentColor = statusColors.content,
            titleMaxLines = 3
        )

        scholarship.statusString?.takeIf { it.isNotBlank() }?.let { status ->
            val statusLabel = stringResource(R.string.finances_scholarship_status)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {},
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(WseiSpacing.s)
            ) {
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onSurfaceVariant
                )
                WseiPill(
                    text = status,
                    icon = scholarshipStatusIcon(kind),
                    containerColor = statusColors.container,
                    contentColor = statusColors.content,
                    emphasized = true
                )
            }
        }

        val typeName = scholarship.typeName.trim()
        val showType = typeName.isNotEmpty() && !typeName.equals(scholarship.title.trim(), ignoreCase = true)
        val variant = scholarship.typeVariantName.trim()
        val submitted = scholarship.submissionDateIso.trim()
        if (showType || variant.isNotEmpty() || submitted.isNotEmpty()) {
            WseiInset {
                if (showType) WseiInfoRow(stringResource(R.string.finances_scholarship_type), typeName)
                if (variant.isNotEmpty()) WseiInfoRow(stringResource(R.string.finances_scholarship_variant), variant)
                if (submitted.isNotEmpty()) WseiInfoRow(stringResource(R.string.finances_scholarship_submitted), submitted)
            }
        }

        if (scholarship.hasDecision) {
            WseiInset(containerColor = colors.surfaceContainerHigh) {
                Text(
                    text = stringResource(R.string.finances_scholarship_decision),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { heading() }
                )
                // The decision number is a personal administrative identifier.
                scholarship.decisionNumber?.takeIf { it.isNotBlank() }?.let {
                    WseiInfoRow(stringResource(R.string.finances_scholarship_decision_number), m(it))
                }
                scholarship.decisionDateIso?.takeIf { it.isNotBlank() }?.let {
                    WseiInfoRow(stringResource(R.string.finances_scholarship_decision_date), it)
                }
                scholarship.decisionStatus?.takeIf { it.isNotBlank() }?.let {
                    WseiInfoRow(stringResource(R.string.finances_scholarship_decision_status), it)
                }
            }
        }

        val amount = scholarship.amountFormatted?.takeIf { it.isNotBlank() }
        val payouts = if (showAllPayouts) scholarship.payouts else scholarship.payouts.take(3)
        if (amount != null || payouts.isNotEmpty()) {
            WseiInset {
                amount?.let { WseiInfoRow(stringResource(R.string.finances_scholarship_amount), m(formatMoneyText(it))) }
                if (payouts.isNotEmpty()) {
                    if (amount != null) HorizontalDivider(color = colors.outlineVariant)
                    Text(
                        text = stringResource(R.string.finances_scholarship_payouts),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.onSurfaceVariant
                    )
                    payouts.forEach { payout ->
                        val label = listOf(payout.dateIso, payout.status).filter { it.isNotBlank() }
                        WseiInfoRow(
                            label = when (label.size) {
                                2 -> stringResource(R.string.finances_payout_label, label[0], label[1])
                                1 -> label[0]
                                else -> "—"
                            },
                            value = m(formatMoneyText(payout.amountFormatted))
                        )
                    }
                }
            }
        }

        if (onOpenHistory != null) {
            OutlinedButton(
                onClick = onOpenHistory,
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.finances_scholarship_history), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
