@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package pl.lublin.wsei.plan.ui.finances

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import pl.lublin.wsei.plan.domain.model.FinancialObligation
import pl.lublin.wsei.plan.domain.model.ScholarshipApplication
import pl.lublin.wsei.plan.domain.model.formatPolishBankAccount
import pl.lublin.wsei.plan.ui.components.WseiCard
import pl.lublin.wsei.plan.ui.components.WseiCardHeader
import pl.lublin.wsei.plan.ui.components.WseiEmptyState
import pl.lublin.wsei.plan.ui.components.WseiExpandableCard
import pl.lublin.wsei.plan.ui.components.WseiIconBadge
import pl.lublin.wsei.plan.ui.components.WseiInfoRow
import pl.lublin.wsei.plan.ui.components.WseiInset
import pl.lublin.wsei.plan.ui.components.WseiPill
import pl.lublin.wsei.plan.ui.components.WseiScreenScaffold
import pl.lublin.wsei.plan.ui.components.WseiSectionTitle
import pl.lublin.wsei.plan.ui.icons.WseiIcons
import pl.lublin.wsei.plan.ui.schedule.ScheduleUiState
import pl.lublin.wsei.plan.ui.theme.WseiHeroShape
import pl.lublin.wsei.plan.ui.theme.WseiSpacing

private const val MASK = "••••••"
private const val FALLBACK_ACCOUNT_NUMBER = "86 1020 3147 2006 0000 0609 3118"

/** Finanse: saldo i numer konta, należności (do zapłaty / zapłacone) oraz stypendia. */
@Composable
fun FinancesScreen(
    uiState: ScheduleUiState,
    onInitiateOnlinePayment: (FinancialObligation, (String) -> Unit) -> Unit,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    var showScholarshipHistory by rememberSaveable { mutableStateOf(false) }

    val sorted = remember(uiState.financials) { uiState.financials.sortedForDisplay() }
    val unpaid = remember(sorted) { sorted.filter { !it.isPaid } }
    val paid = remember(sorted) { sorted.filter { it.isPaid } }
    val unpaidSum = remember(unpaid) { unpaid.sumOf { it.outstandingAmountPln } }
    val accountNumber = remember(uiState.financials) {
        val raw = uiState.financials.firstOrNull()?.bankAccountNumber
            ?.takeIf { it.isNotBlank() }
            ?: FALLBACK_ACCOUNT_NUMBER
        formatPolishBankAccount(raw)
    }
    val activeScholarship = uiState.scholarships.firstOrNull()

    val payOnline: (FinancialObligation) -> Unit = { item ->
        onInitiateOnlinePayment(item) { paymentUrl ->
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(paymentUrl))) }
        }
    }

    WseiScreenScaffold(
        title = "Finanse",
        onRefresh = onRefresh,
        isRefreshing = uiState.isRefreshing
    ) {
        item(key = "finance_summary_hero") {
            BalanceHeroCard(
                unpaidSum = unpaidSum,
                unpaidCount = unpaid.size,
                accountNumber = accountNumber,
                mask = uiState.isHideSensitiveData
            )
        }

        if (sorted.isEmpty()) {
            item(key = "finance_empty") {
                WseiEmptyState(
                    icon = WseiIcons.AccountBalanceWallet,
                    title = "Brak należności",
                    message = "Nie znaleziono żadnych opłat na Twoim koncie."
                )
            }
        }

        if (unpaid.isNotEmpty()) {
            item(key = "section_unpaid") {
                WseiSectionTitle(text = "Do zapłaty", trailing = "${unpaid.size}")
            }
            items(items = unpaid, key = { it.id }, contentType = { "obligation" }) { item ->
                ObligationCard(
                    item = item,
                    maskSensitive = uiState.isHideSensitiveData,
                    onPayOnline = { payOnline(item) }
                )
            }
        }

        if (paid.isNotEmpty()) {
            item(key = "section_paid") {
                WseiSectionTitle(text = "Zapłacone", trailing = "${paid.size}")
            }
            items(items = paid, key = { it.id }, contentType = { "obligation" }) { item ->
                ObligationCard(
                    item = item,
                    maskSensitive = uiState.isHideSensitiveData,
                    onPayOnline = { payOnline(item) }
                )
            }
        }

        item(key = "section_scholarships") {
            WseiSectionTitle(text = "Stypendia")
        }
        if (activeScholarship != null) {
            item(key = "active_scholarship_card") {
                ScholarshipCard(
                    scholarship = activeScholarship,
                    maskSensitive = uiState.isHideSensitiveData,
                    onOpenHistory = if (uiState.scholarships.size > 1) {
                        { showScholarshipHistory = true }
                    } else {
                        null
                    }
                )
            }
        } else {
            item(key = "empty_scholarships") {
                WseiEmptyState(
                    icon = WseiIcons.WorkspacePremium,
                    title = "Brak wniosków",
                    message = "W tym semestrze nie złożono wniosku stypendialnego."
                )
            }
        }
    }

    if (showScholarshipHistory) {
        ModalBottomSheet(
            onDismissRequest = { showScholarshipHistory = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = WseiSpacing.xl)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(WseiSpacing.m)
            ) {
                Text(
                    text = "Historia decyzji stypendialnych",
                    style = MaterialTheme.typography.headlineSmall
                )
                uiState.scholarships.forEach { scholarship ->
                    ScholarshipCard(
                        scholarship = scholarship,
                        maskSensitive = uiState.isHideSensitiveData,
                        onOpenHistory = null,
                        showAllPayouts = true
                    )
                }
            }
        }
    }
}

@Composable
private fun BalanceHeroCard(
    unpaidSum: Double,
    unpaidCount: Int,
    accountNumber: String?,
    mask: Boolean
) {
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(2_000)
            copied = false
        }
    }
    val colors = MaterialTheme.colorScheme
    val settled = unpaidSum == 0.0

    WseiCard(
        shape = WseiHeroShape,
        containerColor = if (settled) colors.primaryContainer else colors.tertiaryContainer,
        contentColor = if (settled) colors.onPrimaryContainer else colors.onTertiaryContainer,
        verticalSpacing = WseiSpacing.l
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(WseiSpacing.xs)) {
            Text(
                text = if (settled) "Saldo" else "Do zapłaty • $unpaidCount ${if (unpaidCount == 1) "pozycja" else "pozycje"}",
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = if (mask) MASK else formatPln(unpaidSum),
                style = MaterialTheme.typography.displaySmall,
                maxLines = 1
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
            val contentColor = if (settled) colors.onPrimaryContainer else colors.onTertiaryContainer
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
                            text = if (mask) MASK else accountNumber,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    FilledTonalIconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(accountNumber))
                            copied = true
                        },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = contentColor,
                            contentColor = if (settled) colors.primaryContainer else colors.tertiaryContainer
                        )
                    ) {
                        AnimatedContent(targetState = copied, label = "copyIcon") { isCopied ->
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
    onPayOnline: () -> Unit
) {
    var expanded by rememberSaveable(item.id) { mutableStateOf(false) }
    val dueThisMonth = item.isDueThisMonth()
    val colors = MaterialTheme.colorScheme
    val containerColor = if (dueThisMonth) colors.errorContainer else colors.surfaceContainer
    val contentColor = if (dueThisMonth) colors.onErrorContainer else colors.onSurface
    val amount = when {
        maskSensitive -> MASK
        item.isPaid -> item.amountFormatted
        else -> formatPln(item.outstandingAmountPln)
    }

    WseiExpandableCard(
        expanded = expanded,
        onToggle = { expanded = !expanded },
        containerColor = containerColor,
        contentColor = contentColor,
        modifier = if (item.isPaid) Modifier.alpha(0.85f) else Modifier,
        pinned = if (item.payOnlineAvailable && !item.isPaid) {
            {
                Button(
                    onClick = onPayOnline,
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(
                        imageVector = WseiIcons.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(WseiSpacing.s))
                    Text(text = "Zapłać online • $amount", style = MaterialTheme.typography.labelLarge)
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
                    }
                )
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = amount,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        body = {
            WseiInset(
                containerColor = if (dueThisMonth) contentColor.copy(alpha = 0.08f) else colors.surfaceContainerLow
            ) {
                val m: (String) -> String = { if (maskSensitive) MASK else it }
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
    showAllPayouts: Boolean = false
) {
    val colors = MaterialTheme.colorScheme
    WseiCard(containerColor = colors.surfaceContainer) {
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
            WseiInfoRow("Decyzja", scholarship.decisionNumber ?: "W toku")
            scholarship.decisionDateIso?.let { WseiInfoRow("Data decyzji", it) }
            val payouts = if (showAllPayouts) scholarship.payouts else scholarship.payouts.take(3)
            if (payouts.isNotEmpty()) {
                HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.5f))
                payouts.forEach { payout ->
                    WseiInfoRow(
                        label = "${payout.dateIso} • ${payout.status}",
                        value = if (maskSensitive) MASK else payout.amountFormatted
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
