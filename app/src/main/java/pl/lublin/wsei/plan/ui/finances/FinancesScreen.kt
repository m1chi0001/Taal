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
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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

/** `ClipDescription.EXTRA_IS_SENSITIVE` (API 33) — keeps the clipboard preview from showing the value. */
private const val EXTRA_CLIP_IS_SENSITIVE = "android.content.extra.IS_SENSITIVE"

/**
 * Finanse: saldo i numer konta, należności (do zapłaty / zapłacone) oraz stypendia.
 *
 * Na szerokich oknach (tablet, rozłożony foldable) podsumowanie i stypendia trafiają do lewej
 * kolumny, a lista należności do prawej; na telefonie wszystko jest jedną listą.
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
            title = "Finanse",
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
            title = "Finanse",
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
                    text = "Historia decyzji stypendialnych",
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
// Sekcje listy (wspólne dla układu jedno- i dwukolumnowego)
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
                title = "Brak należności",
                message = "Nie znaleziono żadnych opłat na Twoim koncie.",
                modifier = itemModifier
            )
        }
        return
    }
    if (unpaid.isNotEmpty()) {
        item(key = "section_unpaid", contentType = "section") {
            WseiSectionTitle(text = "Do zapłaty", trailing = "${unpaid.size}", modifier = itemModifier)
        }
        items(items = unpaid, key = { "unpaid_${it.id}" }, contentType = { "obligation" }) { item ->
            ObligationCard(
                item = item,
                maskSensitive = hidden,
                onPayOnline = { onPayOnline(item) },
                modifier = itemModifier
            )
        }
    }
    if (paid.isNotEmpty()) {
        item(key = "section_paid", contentType = "section") {
            WseiSectionTitle(text = "Zapłacone", trailing = "${paid.size}", modifier = itemModifier)
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
        WseiSectionTitle(text = "Stypendia", modifier = itemModifier)
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
                title = "Brak wniosków",
                message = "W tym semestrze nie złożono wniosku stypendialnego.",
                modifier = itemModifier
            )
        }
    }
}

// -----------------------------------------------------------------------------------------
// Karty
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
    val contentColor = if (settled) colors.onPrimaryContainer else colors.onTertiaryContainer

    WseiCard(
        modifier = modifier,
        shape = WseiHeroShape,
        containerColor = if (settled) colors.primaryContainer else colors.tertiaryContainer,
        contentColor = contentColor,
        verticalSpacing = WseiSpacing.l
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(WseiSpacing.xs)) {
            Text(
                text = if (settled) "Saldo" else "Do zapłaty • ${polishItemCountLabel(unpaidCount)}",
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
                    text = "Wszystko uregulowane",
                    icon = WseiIcons.CheckCircle,
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                    emphasized = true
                )
            }
        }

        if (accountNumber != null) {
            WseiInset(containerColor = contentColor.copy(alpha = 0.08f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(WseiSpacing.m)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(WseiSpacing.xxs)
                    ) {
                        Text(
                            text = "Indywidualny numer konta",
                            style = MaterialTheme.typography.labelMedium,
                            color = contentColor.copy(alpha = 0.8f)
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
                            copied = copySensitiveText(context, "Numer konta", accountNumber)
                        },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = contentColor,
                            contentColor = if (settled) colors.primaryContainer else colors.tertiaryContainer
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
                                contentDescription = if (isCopied) "Skopiowano numer konta" else "Kopiuj numer konta",
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
 * Należność: zwinięta pokazuje tytuł, termin, kwotę i przycisk płatności;
 * po rozwinięciu — szczegóły rozliczenia.
 */
@Composable
private fun ObligationCard(
    item: FinancialObligation,
    maskSensitive: Boolean,
    onPayOnline: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable(item.id) { mutableStateOf(false) }
    val dueThisMonth = remember(item) { item.isDueThisMonth() }
    val colors = MaterialTheme.colorScheme
    val containerColor = if (dueThisMonth) colors.errorContainer else colors.surfaceContainer
    val contentColor = if (dueThisMonth) colors.onErrorContainer else colors.onSurface
    val amount = wseiMasked(
        if (item.isPaid) item.amountFormatted else formatPln(item.outstandingAmountPln),
        maskSensitive
    )

    WseiExpandableCard(
        expanded = expanded,
        onToggle = { expanded = !expanded },
        containerColor = containerColor,
        contentColor = contentColor,
        modifier = if (item.isPaid) modifier.alpha(0.85f) else modifier,
        pinned = if (item.payOnlineAvailable && !item.isPaid) {
            {
                Button(
                    onClick = onPayOnline,
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                ) {
                    Icon(
                        imageVector = WseiIcons.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(WseiSpacing.s))
                    Text(
                        text = "Zapłać online • $amount",
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else {
            null
        },
        header = {
            WseiIconBadge(
                icon = if (item.isPaid) WseiIcons.CheckCircle else WseiIcons.AccountBalanceWallet,
                containerColor = when {
                    dueThisMonth -> colors.error
                    item.isPaid -> colors.surfaceContainerHighest
                    else -> colors.primaryContainer
                },
                contentColor = when {
                    dueThisMonth -> colors.onError
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
                        dueThisMonth -> "Termin w tym miesiącu • ${item.dueDateIso}"
                        item.isPaid -> "Zapłacone • ${item.paymentDateIso ?: item.dueDateIso}"
                        else -> "Termin ${item.dueDateIso}"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        dueThisMonth -> colors.error
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
            WseiInset(
                containerColor = if (dueThisMonth) contentColor.copy(alpha = 0.08f) else colors.surfaceContainerLow
            ) {
                val m: (String) -> String = { wseiMasked(it, maskSensitive) }
                WseiInfoRow("Status", item.statusLabel)
                WseiInfoRow("Termin płatności", item.dueDateIso)
                item.paymentDateIso?.let { WseiInfoRow("Data wpłaty", it) }
                HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.5f))
                WseiInfoRow("Kwota naliczona", m(item.amountFormatted))
                WseiInfoRow("Odsetki naliczone", m(item.interestFormatted))
                WseiInfoRow("Odsetki zapłacone", m(item.accumulatedInterestFormatted))
                WseiInfoRow("Umorzenie", m(item.amortizationFormatted))
                WseiInfoRow("Bonifikata", m(item.discountFormatted))
                WseiInfoRow("Przedawnienie", m(item.expiredFormatted))
            }
        }
    )
}

@Composable
private fun ScholarshipCard(
    scholarship: ScholarshipApplication,
    maskSensitive: Boolean,
    onOpenHistory: (() -> Unit)?,
    modifier: Modifier = Modifier,
    showAllPayouts: Boolean = false
) {
    val colors = MaterialTheme.colorScheme
    WseiCard(modifier = modifier, containerColor = colors.surfaceContainer) {
        WseiCardHeader(
            title = scholarship.title,
            overline = "${scholarship.semesterLabel} • ${scholarship.statusString}",
            icon = WseiIcons.WorkspacePremium,
            iconContainerColor = if (scholarship.isGranted) colors.primary else colors.secondaryContainer,
            iconContentColor = if (scholarship.isGranted) colors.onPrimary else colors.onSecondaryContainer
        )
        WseiInset {
            if (scholarship.typeVariantName.isNotBlank()) {
                Text(
                    text = scholarship.typeVariantName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            WseiInfoRow("Data wniosku", scholarship.submissionDateIso)
            // The decision number is a personal administrative identifier.
            WseiInfoRow("Decyzja", scholarship.decisionNumber?.let { wseiMasked(it, maskSensitive) } ?: "W toku")
            scholarship.decisionDateIso?.let { WseiInfoRow("Data decyzji", it) }
            val payouts = if (showAllPayouts) scholarship.payouts else scholarship.payouts.take(3)
            if (payouts.isNotEmpty()) {
                HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.5f))
                payouts.forEach { payout ->
                    WseiInfoRow(
                        label = "${payout.dateIso} • ${payout.status}",
                        value = wseiMasked(payout.amountFormatted, maskSensitive)
                    )
                }
            }
        }
        if (onOpenHistory != null) {
            OutlinedButton(
                onClick = onOpenHistory,
                shape = CircleShape,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Historia decyzji", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
