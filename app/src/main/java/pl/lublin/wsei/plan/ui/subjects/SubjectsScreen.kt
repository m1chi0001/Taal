@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class
)

package pl.lublin.wsei.plan.ui.subjects

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pl.lublin.wsei.plan.domain.model.GradeItem
import pl.lublin.wsei.plan.ui.components.WseiCard
import pl.lublin.wsei.plan.ui.components.WseiEmptyState
import pl.lublin.wsei.plan.ui.components.WseiPill
import pl.lublin.wsei.plan.ui.components.WseiScreenFrame
import pl.lublin.wsei.plan.ui.components.WseiSectionTitle
import pl.lublin.wsei.plan.ui.components.WseiStat
import pl.lublin.wsei.plan.ui.icons.WseiIcons
import pl.lublin.wsei.plan.ui.adaptive.LocalWseiWindowLayout
import pl.lublin.wsei.plan.ui.adaptive.wseiScreenContentPadding
import pl.lublin.wsei.plan.ui.schedule.ScheduleUiState
import pl.lublin.wsei.plan.ui.theme.M3XCookieShape
import pl.lublin.wsei.plan.ui.theme.WseiHeroShape
import pl.lublin.wsei.plan.ui.theme.WseiMotion
import pl.lublin.wsei.plan.ui.theme.WseiSpacing
import java.util.Locale

/**
 * Przedmioty bieżącego semestru: podsumowanie (średnia, ECTS) i lista ocen.
 * Na szerszych oknach oceny układają się w siatkę (2–3 kolumny) zamiast rozciągniętych kart.
 */
@Composable
fun SubjectsScreen(
    uiState: ScheduleUiState,
    onRefresh: () -> Unit
) {
    val columns = LocalWseiWindowLayout.current.gridColumns
    val grades = uiState.grades
    val totalEcts = remember(grades) { grades.sumOf { it.ects } }
    val gradedCount = remember(grades) { grades.count { parseGrade(it.gradeValue) != null } }

    WseiScreenFrame(
        title = "Przedmioty",
        onRefresh = onRefresh,
        isRefreshing = uiState.isRefreshing
    ) { scrollModifier ->
        // With a single column the staggered grid lays out exactly like the previous list.
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(columns),
            modifier = Modifier
                .fillMaxSize()
                .then(scrollModifier),
            contentPadding = wseiScreenContentPadding(),
            verticalItemSpacing = WseiSpacing.betweenCards,
            horizontalArrangement = Arrangement.spacedBy(WseiSpacing.betweenCards)
        ) {
            item(
                key = "semester_summary",
                contentType = "semester_summary",
                span = StaggeredGridItemSpan.FullLine
            ) {
                SemesterSummaryCard(
                    uiState = uiState,
                    totalEcts = totalEcts,
                    gradedCount = gradedCount,
                    subjectCount = grades.size
                )
            }

            if (grades.isEmpty()) {
                item(
                    key = "grades_empty",
                    contentType = "grades_empty",
                    span = StaggeredGridItemSpan.FullLine
                ) {
                    WseiEmptyState(
                        icon = WseiIcons.School,
                        title = "Brak przedmiotów",
                        message = "Oceny pojawią się tu po synchronizacji z Wirtualnym Dziekanatem."
                    )
                }
            } else {
                item(
                    key = "grades_title",
                    contentType = "grades_title",
                    span = StaggeredGridItemSpan.FullLine
                ) {
                    WseiSectionTitle(text = "Oceny", trailing = "${grades.size} przedm.")
                }
                items(
                    items = grades,
                    key = { it.id },
                    contentType = { "grade_item_card" }
                ) { grade ->
                    GradeCard(grade = grade, maskSensitive = uiState.isHideSensitiveData)
                }
            }
        }
    }
}

@Composable
private fun SemesterSummaryCard(
    uiState: ScheduleUiState,
    totalEcts: Int,
    gradedCount: Int,
    subjectCount: Int
) {
    val session = uiState.currentSession
    val colors = MaterialTheme.colorScheme
    val average = uiState.maskSensitive(
        String.format(Locale.US, "%.2f", session?.averageCurrentSemester ?: 0.0)
    )
    val gradedFraction = if (subjectCount > 0) gradedCount.toFloat() / subjectCount else 0f
    val animatedGraded by animateFloatAsState(
        targetValue = gradedFraction,
        animationSpec = WseiMotion.size(),
        label = "gradedProgress"
    )
    WseiCard(
        shape = WseiHeroShape,
        containerColor = colors.primaryContainer,
        contentColor = colors.onPrimaryContainer,
        verticalSpacing = WseiSpacing.l
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(WseiSpacing.xxs)) {
            Text(
                text = "Semestr ${session?.semesterNumber ?: 1}",
                style = MaterialTheme.typography.labelLarge,
                color = colors.primary
            )
            Text(
                text = session?.fieldOfStudy ?: "Kierunek",
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(WseiSpacing.s)
        ) {
            val statColor = colors.onPrimaryContainer.copy(alpha = 0.1f)
            val statContent = colors.onPrimaryContainer
            WseiStat(average, "Średnia", Modifier.weight(1f), statColor, statContent)
            WseiStat(totalEcts.toString(), "ECTS", Modifier.weight(1f), statColor, statContent)
            WseiStat("$gradedCount/$subjectCount", "Ocenione", Modifier.weight(1f), statColor, statContent)
        }
        if (subjectCount > 0) {
            // How far the semester's grading has progressed — counts only, no grade values.
            LinearWavyProgressIndicator(
                progress = { animatedGraded },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .semantics {
                        contentDescription = "Ocenione przedmioty: $gradedCount z $subjectCount"
                    },
                color = colors.primary,
                trackColor = colors.onPrimaryContainer.copy(alpha = 0.16f)
            )
        }
    }
}

/** Liczbowa wartość oceny („4,5”, „5.0”, „zal” → null). */
private fun parseGrade(raw: String): Double? =
    raw.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it in 2.0..5.5 }

/** Kolor plakietki oceny: wysoka — kolor główny, dostateczna — trzeciorzędny, 2 — błąd. */
private fun gradeColors(raw: String, colors: ColorScheme): Pair<Color, Color> {
    val value = parseGrade(raw)
    return when {
        value == null -> colors.surfaceContainerHighest to colors.onSurfaceVariant
        value >= 4.5 -> colors.primary to colors.onPrimary
        value >= 3.5 -> colors.primaryContainer to colors.onPrimaryContainer
        value >= 3.0 -> colors.tertiaryContainer to colors.onTertiaryContainer
        else -> colors.error to colors.onError
    }
}

@Composable
private fun GradeCard(grade: GradeItem, maskSensitive: Boolean) {
    val gradeText = if (maskSensitive) "••" else grade.gradeValue.ifBlank { "—" }
    val (badgeColor, badgeContent) = if (maskSensitive) {
        MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        gradeColors(grade.gradeValue, MaterialTheme.colorScheme)
    }

    WseiCard(verticalSpacing = WseiSpacing.s) {
        Row(horizontalArrangement = Arrangement.spacedBy(WseiSpacing.l)) {
            Surface(
                shape = M3XCookieShape,
                color = badgeColor,
                contentColor = badgeContent,
                modifier = Modifier.size(60.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = gradeText,
                        style = if (gradeText.length > 3) {
                            MaterialTheme.typography.labelLarge
                        } else {
                            MaterialTheme.typography.titleLarge
                        },
                        maxLines = 1
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(WseiSpacing.s)
            ) {
                Text(
                    text = grade.subjectName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    WseiPill("${grade.ects} ECTS", emphasized = true)
                    WseiPill(grade.formLabel)
                    if (grade.passingForm.isNotBlank()) WseiPill(grade.passingForm)
                }
                Text(
                    text = grade.lecturerName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val details = buildString {
                    if (grade.partialGrades.isNotEmpty()) {
                        append("Cząstkowe: ")
                        append(if (maskSensitive) "••••" else grade.partialGrades.joinToString(", "))
                        append(" • ")
                    }
                    append("wpis ")
                    append(grade.dateAwarded.ifBlank { "—" })
                }
                Text(
                    text = details,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
