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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pl.lublin.wsei.plan.R
import pl.lublin.wsei.plan.domain.model.ClassType
import pl.lublin.wsei.plan.domain.model.GradeItem
import pl.lublin.wsei.plan.l10n.rememberLocalizedDateFormatter
import pl.lublin.wsei.plan.ui.components.WseiCard
import pl.lublin.wsei.plan.ui.components.WseiEmptyState
import pl.lublin.wsei.plan.ui.components.WseiEqualHeightCell
import pl.lublin.wsei.plan.ui.components.WseiEqualHeightRow
import pl.lublin.wsei.plan.ui.components.WseiPill
import pl.lublin.wsei.plan.ui.components.WseiScreenFrame
import pl.lublin.wsei.plan.ui.components.WseiSectionTitle
import pl.lublin.wsei.plan.ui.components.WseiStat
import pl.lublin.wsei.plan.ui.icons.WseiIcons
import pl.lublin.wsei.plan.ui.adaptive.LocalWseiWindowLayout
import pl.lublin.wsei.plan.ui.adaptive.wseiScreenContentPadding
import pl.lublin.wsei.plan.ui.schedule.ScheduleUiState
import pl.lublin.wsei.plan.ui.theme.M3XSquircleShape
import pl.lublin.wsei.plan.ui.theme.WseiCardShape
import pl.lublin.wsei.plan.ui.theme.WseiHeroShape
import pl.lublin.wsei.plan.ui.theme.WseiMotion
import pl.lublin.wsei.plan.ui.theme.WseiSpacing
import pl.lublin.wsei.plan.ui.theme.mutedContentColor
import pl.lublin.wsei.plan.ui.theme.tonalLayer
import pl.lublin.wsei.plan.ui.theme.withContrastAgainst
import pl.lublin.wsei.plan.ui.theme.wseiHorizontalGroupItemShape
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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
 * Subjects and grades split by semester; the current semester is shown by default. On wider
 * windows the grade cards form a balanced grid (2–3 columns, equal heights per row) instead of
 * stretched cards.
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
        // Cards are laid out in rows of [columns]; every card in a row takes the height of the
        // tallest one, so the grid stays balanced without staggered gaps.
        val gradeRows = remember(grades, columns) { grades.chunked(columns.coerceAtLeast(1)) }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(scrollModifier),
            contentPadding = wseiScreenContentPadding(),
            verticalArrangement = Arrangement.spacedBy(WseiSpacing.betweenCards)
        ) {
            item(key = "semester_summary", contentType = "semester_summary") {
                SemesterSummaryCard(
                    uiState = uiState,
                    semester = selected,
                    isCurrent = selected != null && selected === currentSemester
                )
            }

            if (semesters.size > 1) {
                item(key = "semester_picker", contentType = "semester_picker") {
                    SemesterPicker(
                        semesters = semesters,
                        selected = selected,
                        current = currentSemester,
                        onSelect = { selectedKey = it.key }
                    )
                }
            }

            if (grades.isEmpty()) {
                item(key = "grades_empty", contentType = "grades_empty") {
                    WseiEmptyState(
                        icon = WseiIcons.School,
                        title = stringResource(R.string.subjects_empty_title),
                        message = stringResource(R.string.subjects_empty_message)
                    )
                }
            } else {
                item(key = "grades_title", contentType = "grades_title") {
                    WseiSectionTitle(
                        text = stringResource(R.string.subjects_grades_title),
                        trailing = pluralStringResource(R.plurals.subjects_count_short, grades.size, grades.size)
                    )
                }
                items(
                    items = gradeRows,
                    key = { row -> "grades_row_${row.first().id}" },
                    contentType = { "grade_row" }
                ) { row ->
                    val colors = MaterialTheme.colorScheme
                    WseiEqualHeightRow(
                        columns = columns,
                        horizontalSpacing = WseiSpacing.betweenCards,
                        cells = row.map { grade ->
                            WseiEqualHeightCell(
                                shape = WseiCardShape,
                                containerColor = colors.surfaceContainer,
                                contentColor = colors.onSurface
                            ) {
                                GradeCardContent(
                                    grade = grade,
                                    lecturer = uiState.maskLecturer(grade.lecturerName.trim()),
                                    maskSensitive = uiState.isHideSensitiveData
                                )
                            }
                        }
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
    val statColor = tonalLayer(colors.primaryContainer, colors.onPrimaryContainer, 0.12f)
    val statContent = colors.onPrimaryContainer.withContrastAgainst(statColor)

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
                    color = mutedContentColor(colors.onPrimaryContainer, colors.primaryContainer)
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
            horizontalArrangement = Arrangement.spacedBy(WseiSpacing.inGroup)
        ) {
            WseiStat(
                value = average,
                label = averageLabel,
                modifier = Modifier.weight(1f),
                containerColor = statColor,
                contentColor = statContent,
                shape = wseiHorizontalGroupItemShape(0, 3)
            )
            WseiStat(
                value = (semester?.totalEcts ?: 0).toString(),
                label = stringResource(R.string.subjects_ects),
                modifier = Modifier.weight(1f),
                containerColor = statColor,
                contentColor = statContent,
                shape = wseiHorizontalGroupItemShape(1, 3)
            )
            WseiStat(
                value = stringResource(R.string.subjects_graded_value, gradedCount, subjectCount),
                label = stringResource(R.string.subjects_graded),
                modifier = Modifier.weight(1f),
                containerColor = statColor,
                contentColor = statContent,
                shape = wseiHorizontalGroupItemShape(2, 3)
            )
        }
        // Grading progress (counts only, no grade values). Hidden until something is graded:
        // an empty track with a lone stop dot reads like a glitch rather than "0 of 7".
        if (subjectCount > 0 && gradedCount > 0) {
            val gradedDescription = stringResource(R.string.subjects_graded_cd, gradedCount, subjectCount)
            LinearWavyProgressIndicator(
                progress = { animatedGraded },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .semantics {
                        contentDescription = gradedDescription
                    },
                color = colors.onPrimaryContainer,
                trackColor = tonalLayer(colors.primaryContainer, colors.onPrimaryContainer, 0.16f),
                stopSize = 0.dp
            )
        }
    }
}

/** Numeric grade value ("4,5", "5.0"; "zal" → null). */
private fun parseGrade(raw: String): Double? =
    raw.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it in 2.0..5.5 }

private fun isPassingWord(raw: String): Boolean =
    raw.trim().lowercase(Locale.ROOT).let { it == "zal" || it == "zal." || it == "zaliczone" }

/** Grade badge container pairs: high — primaryContainer, good — secondaryContainer, 3 — tertiaryContainer, 2 — errorContainer. */
private fun gradeColors(raw: String, colors: ColorScheme): Pair<Color, Color> {
    if (isPassingWord(raw)) return colors.primaryContainer to colors.onPrimaryContainer
    val value = parseGrade(raw)
    return when {
        value == null -> colors.surfaceContainerHighest to colors.onSurfaceVariant
        value >= 4.5 -> colors.primaryContainer to colors.onPrimaryContainer
        value >= 3.5 -> colors.secondaryContainer to colors.onSecondaryContainer
        value >= 3.0 -> colors.tertiaryContainer to colors.onTertiaryContainer
        else -> colors.errorContainer to colors.onErrorContainer
    }
}

/**
 * Readable class form for the raw APR text. Common dean's office abbreviations ("Prakt zaw
 * zajęcia", "Lab", "Ćw") map to a [ClassType]; anything unknown returns null and is shown as is.
 */
internal fun readableClassForm(raw: String): ClassType? {
    val key = raw.trim()
        .lowercase(Locale.ROOT)
        .replace('.', ' ')
        .split(Regex("\\s+"))
        .filter { it.isNotEmpty() }
        .joinToString(" ")
    return when (key) {
        "wykład", "wykl", "wykł", "wyk" -> ClassType.LECTURE
        "laboratorium", "lab", "labor" -> ClassType.LAB
        "ćwiczenia", "ćw", "cw", "ćwicz", "ćwiczenia audytoryjne" -> ClassType.EXERCISE
        "projekt", "proj" -> ClassType.PROJECT
        "seminarium", "sem", "semin" -> ClassType.SEMINAR
        "e-learning", "elearning", "elrn" -> ClassType.ELEARNING
        "egzamin / zaliczenie", "egzamin", "egz" -> ClassType.EXAM
        "prakt zaw zajęcia", "prakt zaw zaj", "praktyczne zajęcia zawodowe", "pzz" -> ClassType.PRACTICE
        else -> null
    }
}

/** Localised label of a mapped class form. */
@Composable
private fun classFormLabel(type: ClassType): String = when (type) {
    ClassType.LECTURE -> stringResource(R.string.class_type_lecture)
    ClassType.LAB -> stringResource(R.string.class_type_lab)
    ClassType.EXERCISE -> stringResource(R.string.class_type_exercise)
    ClassType.PROJECT -> stringResource(R.string.class_type_project)
    ClassType.ELEARNING -> stringResource(R.string.class_type_elearning)
    ClassType.PRACTICE -> stringResource(R.string.class_type_practice)
    ClassType.SEMINAR -> stringResource(R.string.class_type_seminar)
    ClassType.EXAM -> stringResource(R.string.class_type_exam)
}

/** "Wykład + Lab" → "Wykład + Laboratorium": each part is mapped, unknown parts stay verbatim. */
@Composable
private fun readableFormLabel(raw: String): String =
    raw.split('+')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { part -> readableClassForm(part)?.let { classFormLabel(it) } ?: part }
        .joinToString(" + ")

/** ISO "2027-02-26" (optionally with a time part) → localised date; other text is returned unchanged. */
private fun formatAwardedDate(raw: String, formatter: DateTimeFormatter): String {
    val trimmed = raw.trim()
    val date = runCatching { LocalDate.parse(trimmed.take(10)) }.getOrNull() ?: return trimmed
    return formatter.format(date)
}

@Composable
private fun GradeCardContent(
    grade: GradeItem,
    lecturer: String,
    maskSensitive: Boolean
) {
    val hasGrade = !maskSensitive && (parseGrade(grade.gradeValue) != null || isPassingWord(grade.gradeValue))
    val gradeText = if (maskSensitive) "••" else grade.gradeValue.ifBlank { "—" }
    val (badgeColor, badgeContent) = if (maskSensitive) {
        MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        gradeColors(grade.gradeValue, MaterialTheme.colorScheme)
    }
    val badgeShape = if (hasGrade) MaterialShapes.Cookie9Sided.toShape() else M3XSquircleShape
    val dateFormatter = rememberLocalizedDateFormatter("ddMMyyyy")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(WseiSpacing.xl),
        horizontalArrangement = Arrangement.spacedBy(WseiSpacing.l)
    ) {
        Surface(
            shape = badgeShape,
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
                    fontWeight = FontWeight.Bold,
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
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                WseiPill(stringResource(R.string.subjects_ects_value, grade.ects), emphasized = true)
                val formLabel = readableFormLabel(grade.formLabel)
                if (formLabel.isNotEmpty()) WseiPill(formLabel)
                if (grade.passingForm.isNotBlank()) WseiPill(grade.passingForm)
            }
            if (lecturer.isNotBlank()) {
                Text(
                    text = lecturer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
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
                stringResource(R.string.subjects_grade_awarded, formatAwardedDate(grade.dateAwarded, dateFormatter))
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
