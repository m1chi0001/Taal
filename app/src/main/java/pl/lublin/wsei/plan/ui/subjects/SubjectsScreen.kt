@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class
)

package pl.lublin.wsei.plan.ui.subjects

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pl.lublin.wsei.plan.R
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
import androidx.compose.ui.res.pluralStringResource

/** Grades of one semester, as returned by the dean's office (newest semester first). */
private class SemesterGrades(
    val key: String,
    val number: Int,
    val label: String,
    val grades: List<GradeItem>
) {
    val totalEcts: Int = grades.sumOf { it.ects }
    val gradedCount: Int = grades.count { parseGrade(it.gradeValue) != null }

    /** Plain mean of the numeric grades, null when nothing is graded yet. */
    val computedAverage: Double? = grades.mapNotNull { parseGrade(it.gradeValue) }
        .takeIf { it.isNotEmpty() }
        ?.average()
}

/**
 * Przedmioty i oceny z podziałem na semestry. Domyślnie pokazany jest bieżący semestr
 * (wcześniej oceny wszystkich semestrów były zlewane w jedną listę, a ECTS sumowane razem).
 * Na szerszych oknach oceny układają się w siatkę (2–3 kolumny) zamiast rozciągniętych kart.
 */
@Composable
fun SubjectsScreen(
    uiState: ScheduleUiState,
    onRefresh: () -> Unit
) {
    val columns = LocalWseiWindowLayout.current.gridColumns
    val semesters = remember(uiState.grades) {
        uiState.grades
            .groupBy { "${it.semesterNumber}|${it.semesterLabel}" }
            .map { (key, items) ->
                SemesterGrades(
                    key = key,
                    number = items.first().semesterNumber,
                    label = items.first().semesterLabel,
                    grades = items
                )
            }
            .sortedByDescending { it.number }
    }
    val sessionSemester = uiState.currentSession?.semesterNumber
    val currentSemester = semesters.firstOrNull { it.number == sessionSemester } ?: semesters.firstOrNull()
    var selectedKey by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = semesters.firstOrNull { it.key == selectedKey } ?: currentSemester
    val grades = selected?.grades.orEmpty()

    WseiScreenFrame(
        title = stringResource(R.string.nav_subjects),
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
                    semester = selected,
                    isCurrent = selected != null && selected === currentSemester
                )
            }

            if (semesters.size > 1) {
                item(
                    key = "semester_picker",
                    contentType = "semester_picker",
                    span = StaggeredGridItemSpan.FullLine
                ) {
                    SemesterPicker(
                        semesters = semesters,
                        selected = selected,
                        current = currentSemester,
                        onSelect = { selectedKey = it.key }
                    )
                }
            }

            if (grades.isEmpty()) {
                item(
                    key = "grades_empty",
                    contentType = "grades_empty",
                    span = StaggeredGridItemSpan.FullLine
                ) {
                    WseiEmptyState(
                        icon = WseiIcons.School,
                        title = stringResource(R.string.subjects_empty_title),
                        message = stringResource(R.string.subjects_empty_message)
                    )
                }
            } else {
                item(
                    key = "grades_title",
                    contentType = "grades_title",
                    span = StaggeredGridItemSpan.FullLine
                ) {
                    WseiSectionTitle(
                        text = stringResource(R.string.subjects_grades_title),
                        trailing = pluralStringResource(R.plurals.subjects_count_short, grades.size, grades.size)
                    )
                }
                items(
                    items = grades,
                    key = { it.id },
                    contentType = { "grade_item_card" }
                ) { grade ->
                    GradeCard(
                        grade = grade,
                        lecturer = uiState.maskLecturer(grade.lecturerName.trim()),
                        maskSensitive = uiState.isHideSensitiveData
                    )
                }
            }
        }
    }
}

/** Horizontally scrolling semester filter chips; the current semester is marked. */
@Composable
private fun SemesterPicker(
    semesters: List<SemesterGrades>,
    selected: SemesterGrades?,
    current: SemesterGrades?,
    onSelect: (SemesterGrades) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(WseiSpacing.s)
    ) {
        semesters.forEach { semester ->
            val isSelected = semester === selected
            val label = semester.label.ifBlank { stringResource(R.string.subjects_semester_number, semester.number) }
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(semester) },
                label = {
                    Text(
                        text = if (semester === current) {
                            stringResource(R.string.subjects_semester_current, label)
                        } else {
                            label
                        },
                        maxLines = 1
                    )
                },
                leadingIcon = if (isSelected) {
                    {
                        Icon(
                            imageVector = WseiIcons.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                } else {
                    null
                },
                shape = CircleShape
            )
        }
    }
}

@Composable
private fun SemesterSummaryCard(
    uiState: ScheduleUiState,
    semester: SemesterGrades?,
    isCurrent: Boolean
) {
    val session = uiState.currentSession
    val colors = MaterialTheme.colorScheme
    val subjectCount = semester?.grades?.size ?: 0
    val gradedCount = semester?.gradedCount ?: 0
    // The dean's office average is only known for the current semester; older semesters show
    // the plain mean of their numeric grades.
    val officialAverage = session?.averageCurrentSemester?.takeIf { isCurrent && it > 0.0 }
    val averageValue = officialAverage ?: semester?.computedAverage
    val average = uiState.maskSensitive(
        averageValue?.let { String.format(Locale.US, "%.2f", it) } ?: "—"
    )
    val averageLabel = stringResource(
        if (officialAverage != null) R.string.subjects_average else R.string.subjects_average_computed
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
            val overline = semester?.label?.takeIf { it.isNotBlank() }
                ?: (semester?.number ?: session?.semesterNumber)?.let {
                    stringResource(R.string.subjects_semester_number, it)
                }
            if (overline != null) {
                Text(
                    text = overline,
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.primary
                )
            }
            val fieldOfStudy = session?.fieldOfStudy?.takeIf { it.isNotBlank() }
            if (fieldOfStudy != null) {
                Text(
                    text = fieldOfStudy,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(WseiSpacing.s)
        ) {
            val statColor = colors.onPrimaryContainer.copy(alpha = 0.1f)
            val statContent = colors.onPrimaryContainer
            WseiStat(average, averageLabel, Modifier.weight(1f), statColor, statContent)
            WseiStat(
                (semester?.totalEcts ?: 0).toString(),
                stringResource(R.string.subjects_ects),
                Modifier.weight(1f),
                statColor,
                statContent
            )
            WseiStat(
                stringResource(R.string.subjects_graded_value, gradedCount, subjectCount),
                stringResource(R.string.subjects_graded),
                Modifier.weight(1f),
                statColor,
                statContent
            )
        }
        if (subjectCount > 0) {
            val gradedDescription = stringResource(R.string.subjects_graded_cd, gradedCount, subjectCount)
            // How far the semester's grading has progressed — counts only, no grade values.
            LinearWavyProgressIndicator(
                progress = { animatedGraded },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .semantics {
                        contentDescription = gradedDescription
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
private fun GradeCard(grade: GradeItem, lecturer: String, maskSensitive: Boolean) {
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
                    WseiPill(stringResource(R.string.subjects_ects_value, grade.ects), emphasized = true)
                    WseiPill(grade.formLabel)
                    if (grade.passingForm.isNotBlank()) WseiPill(grade.passingForm)
                }
                if (lecturer.isNotBlank()) {
                    Text(
                        text = lecturer,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                val partialLabel = if (grade.partialGrades.isNotEmpty()) {
                    stringResource(
                        R.string.subjects_partial_grades,
                        if (maskSensitive) "••••" else grade.partialGrades.joinToString(", ")
                    )
                } else {
                    null
                }
                val awardedLabel = if (grade.dateAwarded.isNotBlank()) {
                    stringResource(R.string.subjects_grade_awarded, grade.dateAwarded)
                } else {
                    null
                }
                val details = listOfNotNull(partialLabel, awardedLabel).joinToString(" • ")
                if (details.isNotEmpty()) {
                    Text(
                        text = details,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
