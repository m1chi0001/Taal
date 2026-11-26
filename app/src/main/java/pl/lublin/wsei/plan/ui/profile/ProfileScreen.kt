@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class
)

package pl.lublin.wsei.plan.ui.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.lublin.wsei.plan.R
import pl.lublin.wsei.plan.domain.model.ClassType
import pl.lublin.wsei.plan.domain.model.GradeItem
import pl.lublin.wsei.plan.domain.model.GroupClassKind
import pl.lublin.wsei.plan.domain.model.SemesterTerm
import pl.lublin.wsei.plan.domain.model.StudentGroupItem
import pl.lublin.wsei.plan.domain.model.StudentSession
import pl.lublin.wsei.plan.domain.model.StudyMode
import pl.lublin.wsei.plan.domain.model.StudyTrackInfo
import pl.lublin.wsei.plan.ui.adaptive.LocalWseiWindowLayout
import pl.lublin.wsei.plan.ui.adaptive.WseiTwoPane
import pl.lublin.wsei.plan.ui.adaptive.wseiReadableWidth
import pl.lublin.wsei.plan.ui.adaptive.wseiScreenContentPadding
import pl.lublin.wsei.plan.ui.components.StudentAvatar
import pl.lublin.wsei.plan.ui.components.WseiCard
import pl.lublin.wsei.plan.ui.components.WseiExpandableCard
import pl.lublin.wsei.plan.ui.components.WseiIconBadge
import pl.lublin.wsei.plan.ui.components.WseiInfoRow
import pl.lublin.wsei.plan.ui.components.WseiInset
import pl.lublin.wsei.plan.ui.components.WseiListEntry
import pl.lublin.wsei.plan.ui.components.WseiListGroup
import pl.lublin.wsei.plan.ui.components.WseiListItem
import pl.lublin.wsei.plan.ui.components.WseiPill
import pl.lublin.wsei.plan.ui.components.WseiScreenFrame
import pl.lublin.wsei.plan.ui.components.WseiSectionTitle
import pl.lublin.wsei.plan.ui.icons.WseiIcons
import pl.lublin.wsei.plan.ui.schedule.ScheduleUiState
import pl.lublin.wsei.plan.ui.theme.WseiHeroShape
import pl.lublin.wsei.plan.ui.theme.WseiInsetShape
import pl.lublin.wsei.plan.ui.theme.WseiMotion
import pl.lublin.wsei.plan.ui.theme.WseiSpacing
import pl.lublin.wsei.plan.ui.theme.wseiGroupItemShape
import java.util.Locale

/** Local UI state of the profile screen, shared by the single- and two-pane layouts. */
private class ProfileSectionsState(
    val isStudyExpanded: Boolean,
    val onToggleStudy: () -> Unit,
    val isGroupsExpanded: Boolean,
    val onToggleGroups: () -> Unit,
    val isPlanExpanded: Boolean,
    val onTogglePlan: () -> Unit,
    val showPasswordForm: Boolean,
    val onTogglePasswordForm: () -> Unit
)

/** Hero avatar footprint, the scalloped accent behind the photo included. */
private val HeroAvatarSize = 136.dp

/** Circular photo (tonal ring and gap included) centred on the scalloped accent. */
private val HeroPhotoSize = 118.dp

/**
 * Student profile: identity hero, study track (with expandable details), groups, study plan
 * and account actions.
 *
 * On wide windows the identity hero and the account actions sit in the first pane and all
 * study sections in the second one.
 */
@Composable
fun ProfileScreen(
    uiState: ScheduleUiState,
    onBack: () -> Unit,
    onChangePassword: (String, String) -> Unit,
    onLogout: () -> Unit,
    onRefresh: () -> Unit
) {
    val windowLayout = LocalWseiWindowLayout.current
    var isStudyExpanded by rememberSaveable { mutableStateOf(false) }
    var isGroupsExpanded by rememberSaveable { mutableStateOf(false) }
    var isPlanExpanded by rememberSaveable { mutableStateOf(false) }
    var showPasswordForm by rememberSaveable { mutableStateOf(false) }
    val sections = ProfileSectionsState(
        isStudyExpanded = isStudyExpanded,
        onToggleStudy = { isStudyExpanded = !isStudyExpanded },
        isGroupsExpanded = isGroupsExpanded,
        onToggleGroups = { isGroupsExpanded = !isGroupsExpanded },
        isPlanExpanded = isPlanExpanded,
        onTogglePlan = { isPlanExpanded = !isPlanExpanded },
        showPasswordForm = showPasswordForm,
        onTogglePasswordForm = { showPasswordForm = !showPasswordForm }
    )
    val averages = remember(uiState.currentSession, uiState.grades) {
        profileAverages(uiState.currentSession, uiState.grades)
    }
    val submitPassword: (String, String) -> Unit = { old, new ->
        onChangePassword(old, new)
        showPasswordForm = false
    }

    // On wide layouts Profil is a destination of the navigation rail, so a back button would be
    // misleading; on compact layouts it is opened from the top bar avatar and keeps it.
    val backAction = onBack.takeUnless { windowLayout.useNavigationRail }

    WseiScreenFrame(
        title = stringResource(R.string.nav_profile),
        onBack = backAction,
        onRefresh = onRefresh,
        isRefreshing = uiState.isRefreshing,
        // This is the profile itself: never show the avatar shortcut to it.
        showProfileShortcut = false
    ) { scrollModifier ->
        val screenPadding = wseiScreenContentPadding()
        if (windowLayout.supportsTwoPane) {
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
                        identityItems(uiState, averages, Modifier)
                        accountItems(uiState.currentSession, sections, onLogout, submitPassword, Modifier)
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
                        studyItems(uiState, averages, sections, Modifier)
                    }
                }
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .then(scrollModifier),
                contentPadding = screenPadding,
                verticalArrangement = Arrangement.spacedBy(WseiSpacing.betweenCards)
            ) {
                // Cards are capped to a readable width on medium windows instead of stretching.
                val itemModifier = Modifier.wseiReadableWidth()
                identityItems(uiState, averages, itemModifier)
                studyItems(uiState, averages, sections, itemModifier)
                accountItems(uiState.currentSession, sections, onLogout, submitPassword, itemModifier)
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// Labels and derived values
// -----------------------------------------------------------------------------------------

/** Title and supporting line of a student group row. */
private class StudentGroupLabel(val title: String, val supporting: String?)

/** Human name of the class kind ("Laboratorium"); unknown kinds fall back to the dean's form name. */
@Composable
private fun groupKindLabel(group: StudentGroupItem): String = when (group.groupKind) {
    GroupClassKind.LECTURE -> stringResource(R.string.class_type_lecture)
    GroupClassKind.EXERCISES -> stringResource(R.string.class_type_exercise)
    GroupClassKind.LAB -> stringResource(R.string.class_type_lab)
    GroupClassKind.PROJECT -> stringResource(R.string.class_type_project)
    GroupClassKind.SEMINAR -> stringResource(R.string.class_type_seminar)
    GroupClassKind.ELEARNING -> stringResource(R.string.class_type_elearning)
    GroupClassKind.PRACTICE -> stringResource(R.string.class_type_practice)
    GroupClassKind.STUDY_TRACK, GroupClassKind.OTHER ->
        group.formName.trim().ifEmpty { stringResource(R.string.profile_group) }
}

private fun groupKindIcon(kind: GroupClassKind): ImageVector = when (kind) {
    GroupClassKind.LECTURE -> WseiIcons.School
    GroupClassKind.EXERCISES -> WseiIcons.Calculate
    GroupClassKind.LAB -> WseiIcons.Computer
    GroupClassKind.PROJECT -> WseiIcons.IntegrationInstructions
    GroupClassKind.SEMINAR -> WseiIcons.Psychology
    GroupClassKind.ELEARNING -> WseiIcons.Cloud
    GroupClassKind.PRACTICE -> WseiIcons.BusinessCenter
    GroupClassKind.STUDY_TRACK, GroupClassKind.OTHER -> WseiIcons.Groups
}

/**
 * Exact group data from the dean's office: "Laboratorium · gr. 4" with the raw code
 * ("Lab4/1/I/N") below. Without a parsed group number the code itself is shown instead —
 * never a list index or a made-up number.
 */
@Composable
private fun studentGroupLabel(group: StudentGroupItem): StudentGroupLabel {
    val kind = groupKindLabel(group)
    // Parsed from groupCode, so it is right even for stale cached data; "" when unknown.
    val number = group.displayGroupNumber.trim()
    val code = group.groupCode.trim()
    return if (number.isNotEmpty()) {
        StudentGroupLabel(
            title = listOf(kind, stringResource(R.string.profile_group_number, number))
                .filter { it.isNotEmpty() }
                .joinToString(" · "),
            supporting = code.takeIf { it.isNotEmpty() }
        )
    } else {
        StudentGroupLabel(
            title = listOf(kind, code).filter { it.isNotEmpty() }.distinct().joinToString(" · ").ifEmpty { "—" },
            supporting = null
        )
    }
}

/**
 * Averages shown on the profile. `null` means "nothing graded yet" and is rendered as "—"
 * instead of a misleading "0.00".
 */
@Immutable
private class ProfileAverages(
    val currentSemester: Double?,
    val previousSemester: Double?,
    val overall: Double?,
    /** False in the first semester, where a "previous semester" average does not exist. */
    val hasPreviousSemester: Boolean
)

/** Numeric grade (2.0–5.5); pass/fail entries ("ZAL") and blanks are not graded. */
private fun numericGrade(raw: String): Double? =
    raw.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it in 2.0..5.5 }

/**
 * Dean's office averages, cross-checked with the grade list: an average is shown only when the
 * relevant scope (current semester, previous semester or the whole studies) holds at least one
 * numeric grade. When grades are not loaded at all, a non-zero dean's office value is trusted.
 */
private fun profileAverages(session: StudentSession?, grades: List<GradeItem>): ProfileAverages {
    val gradedSemesters: Set<Int?> = grades
        .filter { numericGrade(it.gradeValue) != null }
        .mapTo(HashSet<Int?>()) { it.semesterNumber }

    fun pick(value: Double?, isGraded: Boolean): Double? =
        value?.takeIf { it > 0.0 && (grades.isEmpty() || isGraded) }

    val sessionSemester = session?.semesterNumber
    // Same rule as the Subjects screen: the session's semester, else the newest listed one.
    val currentSemester = sessionSemester?.takeIf { number -> grades.any { it.semesterNumber == number } }
        ?: grades.firstOrNull()?.semesterNumber
    val previousSemester = sessionSemester?.minus(1)?.takeIf { it >= 1 }
    return ProfileAverages(
        currentSemester = pick(session?.averageCurrentSemester, currentSemester in gradedSemesters),
        previousSemester = pick(
            session?.averagePreviousSemester,
            previousSemester != null && previousSemester in gradedSemesters
        ),
        overall = pick(session?.averageGrade, gradedSemesters.isNotEmpty()),
        hasPreviousSemester = previousSemester != null
    )
}

/** "4.52", "—" when nothing is graded, or the privacy mask when sensitive data is hidden. */
private fun formatAverage(uiState: ScheduleUiState, value: Double?): String =
    uiState.maskSensitive(value?.let { String.format(Locale.US, "%.2f", it) } ?: "—")

/** Localised study mode; the dean's office free-text value is mapped when the track has none. */
@Composable
private fun studyModeLabel(track: StudyTrackInfo?, session: StudentSession?): String? {
    when (track?.studyMode) {
        StudyMode.FULL_TIME -> return stringResource(R.string.profile_track_full_time)
        StudyMode.PART_TIME -> return stringResource(R.string.profile_track_part_time)
        StudyMode.UNKNOWN, null -> Unit
    }
    val raw = session?.studyMode?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val normalized = raw.lowercase(Locale.ROOT)
    return when {
        normalized.startsWith("niestac") -> stringResource(R.string.profile_track_part_time)
        normalized.startsWith("stac") -> stringResource(R.string.profile_track_full_time)
        else -> raw.replaceFirstChar { it.titlecase(Locale.ROOT) }
    }
}

/**
 * Always-visible summary of the study track, e.g. "Rok 1", "Semestr zimowy 2026/2027",
 * "Niestacjonarne" — only the facts that are known. Internal codes are never shown.
 */
@Composable
private fun studySummary(track: StudyTrackInfo?, session: StudentSession?): List<String> {
    val termName = when (track?.semesterTerm) {
        SemesterTerm.WINTER -> stringResource(R.string.profile_track_winter)
        SemesterTerm.SUMMER -> stringResource(R.string.profile_track_summer)
        null -> null
    }
    val academicYear = track?.academicYear?.trim()?.takeIf { it.isNotEmpty() }
    val term = when {
        termName != null && academicYear != null -> stringResource(R.string.profile_track_term_year, termName, academicYear)
        termName != null -> termName
        academicYear != null -> stringResource(R.string.profile_track_academic_year, academicYear)
        else -> null
    }
    val yearOfStudy = track?.yearOfStudy ?: session?.academicYear?.takeIf { it > 0 }
    return listOfNotNull(
        yearOfStudy?.let { stringResource(R.string.profile_track_year, it) },
        term,
        studyModeLabel(track, session)
    )
}

// -----------------------------------------------------------------------------------------
// List sections
// -----------------------------------------------------------------------------------------

private fun LazyListScope.identityItems(
    uiState: ScheduleUiState,
    averages: ProfileAverages,
    itemModifier: Modifier
) {
    item(key = "identity", contentType = "identity") {
        IdentityCard(uiState = uiState, averages = averages, modifier = itemModifier)
    }
}

private fun LazyListScope.studyItems(
    uiState: ScheduleUiState,
    averages: ProfileAverages,
    sections: ProfileSectionsState,
    itemModifier: Modifier
) {
    item(key = "section_studies", contentType = "section") {
        WseiSectionTitle(stringResource(R.string.profile_section_studies), modifier = itemModifier)
    }

    item(key = "study_track", contentType = "accordion") {
        StudyCard(
            uiState = uiState,
            averages = averages,
            expanded = sections.isStudyExpanded,
            onToggle = sections.onToggleStudy,
            modifier = itemModifier
        )
    }

    item(key = "student_groups", contentType = "accordion") {
        // Class groups only: study-track entries ("ROK1/ N") are summarised by the study card.
        val semesters = uiState.classGroupsBySemester
        val semesterGroups = semesters.firstOrNull { it.groups.isNotEmpty() } ?: semesters.firstOrNull()
        val groups = semesterGroups?.groups.orEmpty()
        val title = stringResource(R.string.profile_student_groups)
        if (groups.isEmpty()) {
            ProfileStaticRow(
                title = title,
                supporting = stringResource(R.string.profile_groups_empty),
                icon = WseiIcons.Groups,
                modifier = itemModifier
            )
        } else {
            ProfileAccordion(
                title = title,
                supporting = semesterGroups?.semesterLabel?.trim()?.takeIf { it.isNotEmpty() },
                icon = WseiIcons.Groups,
                expanded = sections.isGroupsExpanded,
                onToggle = sections.onToggleGroups,
                modifier = itemModifier,
                trailing = { CountBadge(groups.size) }
            ) {
                WseiListGroup(
                    entries = groups.mapIndexed { index, group ->
                        WseiListEntry(key = "group_${group.id}_${group.groupCode}_$index") { shape ->
                            val label = studentGroupLabel(group)
                            WseiListItem(
                                title = label.title,
                                supporting = label.supporting,
                                icon = groupKindIcon(group.groupKind),
                                shape = shape,
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        }
                    }
                )
            }
        }
    }

    item(key = "study_plan", contentType = "accordion") {
        val plan = uiState.studyPlans.firstOrNull()
        val planSubjects = remember(plan) { plan?.semesters?.flatMap { it.subjects }.orEmpty() }
        val totalPlanEcts = remember(planSubjects) { planSubjects.sumOf { it.ects.toDouble() }.toInt() }
        val title = stringResource(R.string.profile_study_plan)
        if (planSubjects.isEmpty()) {
            // Nothing to expand: a calm row with the reason instead of an empty accordion and a "—" pill.
            ProfileStaticRow(
                title = title,
                supporting = stringResource(R.string.profile_plan_empty),
                icon = WseiIcons.AutoAwesome,
                modifier = itemModifier
            )
        } else {
            val colors = MaterialTheme.colorScheme
            ProfileAccordion(
                title = title,
                supporting = plan?.planName?.trim()?.takeIf { it.isNotEmpty() },
                icon = WseiIcons.AutoAwesome,
                expanded = sections.isPlanExpanded,
                onToggle = sections.onTogglePlan,
                modifier = itemModifier,
                trailing = {
                    WseiPill(
                        text = stringResource(R.string.subjects_ects_value, totalPlanEcts),
                        containerColor = colors.secondaryContainer,
                        contentColor = colors.onSecondaryContainer,
                        emphasized = true
                    )
                }
            ) {
                WseiListGroup(
                    entries = planSubjects.mapIndexed { index, subject ->
                        WseiListEntry(key = "plan_${subject.id}_$index") { shape ->
                            WseiListItem(
                                title = stringResource(R.string.profile_plan_subject_title, subject.subjectNumber, subject.subjectName),
                                supporting = stringResource(
                                    R.string.profile_plan_subject_supporting,
                                    subject.form,
                                    subject.passingForm,
                                    subject.hours
                                ),
                                icon = WseiIcons.forSubject(subject.subjectName, ClassType.fromRawCode(subject.form)),
                                shape = shape,
                                containerColor = colors.surfaceContainerLow,
                                trailing = { WseiPill(stringResource(R.string.subjects_ects_value, subject.ects.toInt())) }
                            )
                        }
                    }
                )
            }
        }
    }
}

private fun LazyListScope.accountItems(
    session: StudentSession?,
    sections: ProfileSectionsState,
    onLogout: () -> Unit,
    onSubmitPassword: (String, String) -> Unit,
    itemModifier: Modifier
) {
    val canChangePassword = session?.allowPasswordChanges != false
    item(key = "section_account", contentType = "section") {
        WseiSectionTitle(stringResource(R.string.profile_section_account), modifier = itemModifier)
    }
    item(key = "account_actions", contentType = "account") {
        val colors = MaterialTheme.colorScheme
        val count = if (canChangePassword) 2 else 1
        Column(
            modifier = itemModifier,
            verticalArrangement = Arrangement.spacedBy(WseiSpacing.inGroup)
        ) {
            if (canChangePassword) {
                // The row and its form share one segment, so the expanded form reads as part of it.
                Surface(
                    shape = wseiGroupItemShape(0, count),
                    color = colors.surfaceContainer,
                    contentColor = colors.onSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        WseiListItem(
                            title = stringResource(R.string.profile_change_password),
                            supporting = stringResource(R.string.profile_change_password_supporting),
                            icon = WseiIcons.Lock,
                            shape = RectangleShape,
                            containerColor = Color.Transparent,
                            onClick = sections.onTogglePasswordForm,
                            trailing = { ExpandIndicator(expanded = sections.showPasswordForm) }
                        )
                        AnimatedVisibility(
                            visible = sections.showPasswordForm,
                            enter = expandVertically(WseiMotion.size(), expandFrom = Alignment.Top) + fadeIn(WseiMotion.effects()),
                            exit = shrinkVertically(WseiMotion.size(), shrinkTowards = Alignment.Top) + fadeOut(WseiMotion.fastEffects())
                        ) {
                            PasswordChangeForm(
                                onSubmit = onSubmitPassword,
                                modifier = Modifier.padding(
                                    start = WseiSpacing.l,
                                    end = WseiSpacing.l,
                                    bottom = WseiSpacing.l
                                )
                            )
                        }
                    }
                }
            }
            WseiListItem(
                title = stringResource(R.string.profile_logout),
                supporting = stringResource(R.string.profile_logout_supporting),
                icon = WseiIcons.Logout,
                iconContainerColor = colors.errorContainer,
                iconContentColor = colors.onErrorContainer,
                shape = wseiGroupItemShape(count - 1, count),
                onClick = onLogout
            )
        }
    }
}

// -----------------------------------------------------------------------------------------
// Cards
// -----------------------------------------------------------------------------------------

@Composable
private fun IdentityCard(uiState: ScheduleUiState, averages: ProfileAverages, modifier: Modifier = Modifier) {
    val session = uiState.currentSession
    val colors = MaterialTheme.colorScheme
    val fullName = session?.fullName ?: stringResource(R.string.common_student)
    val displayAlbum = uiState.maskSensitive(session?.albumNumber?.takeIf { it.isNotBlank() } ?: "—")

    WseiCard(
        modifier = modifier,
        shape = WseiHeroShape,
        containerColor = colors.primaryContainer,
        contentColor = colors.onPrimaryContainer,
        contentPadding = PaddingValues(start = WseiSpacing.l, end = WseiSpacing.l, top = WseiSpacing.xxl, bottom = WseiSpacing.l),
        verticalSpacing = WseiSpacing.xl
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(WseiSpacing.m)
        ) {
            ProfilePhoto(base64Photo = session?.personPictureBase64, fullName = fullName)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(WseiSpacing.xs)
            ) {
                Text(
                    text = fullName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() }
                )
                // Course name only: year, term and study mode live in the study card.
                val fieldOfStudy = session?.fieldOfStudy?.trim().orEmpty()
                if (fieldOfStudy.isNotEmpty()) {
                    Text(
                        text = fieldOfStudy,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = colors.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
            // FlowRow: on narrow panes the badges wrap instead of being clipped.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(WseiSpacing.s, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(WseiSpacing.s)
            ) {
                WseiPill(
                    text = stringResource(R.string.home_album_number, displayAlbum),
                    icon = WseiIcons.Badge,
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                    emphasized = true
                )
                WseiPill(
                    text = session?.statusName?.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.profile_status_default),
                    icon = WseiIcons.CheckCircle,
                    containerColor = colors.surfaceContainerLowest,
                    contentColor = colors.primary,
                    emphasized = true
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(WseiSpacing.s)
        ) {
            val statModifier = Modifier
                .weight(1f)
                .fillMaxHeight()
            val noGrades = stringResource(R.string.profile_stat_no_grades)
            val isHidden = uiState.isHideSensitiveData
            ProfileStatCard(
                value = formatAverage(uiState, averages.currentSemester),
                label = stringResource(R.string.profile_stat_semester_average),
                icon = WseiIcons.AutoAwesome,
                valueDescription = noGrades.takeIf { averages.currentSemester == null && !isHidden },
                modifier = statModifier
            )
            ProfileStatCard(
                value = formatAverage(uiState, averages.overall),
                label = stringResource(R.string.profile_stat_overall_average),
                icon = WseiIcons.WorkspacePremium,
                valueDescription = noGrades.takeIf { averages.overall == null && !isHidden },
                modifier = statModifier
            )
            ProfileStatCard(
                value = session?.semesterNumber?.takeIf { it > 0 }?.toString() ?: "—",
                label = stringResource(R.string.profile_stat_semester),
                icon = WseiIcons.CalendarMonth,
                modifier = statModifier
            )
        }
    }
}

/**
 * Hero photo: a smooth circular photo inside a tonal ring (`primaryContainer`) and a thin
 * `surface` gap, so the white background of ID photos ends in a clean edge on light and dark
 * themes. The expressive accent is a scalloped Material shape *behind* the ring; it never
 * clips the photo.
 */
@Composable
private fun ProfilePhoto(base64Photo: String?, fullName: String) {
    val colors = MaterialTheme.colorScheme
    val accentShape = MaterialShapes.Cookie12Sided.toShape()
    Box(
        modifier = Modifier
            .size(HeroAvatarSize)
            .background(colors.primary, accentShape),
        contentAlignment = Alignment.Center
    ) {
        StudentAvatar(
            base64Photo = base64Photo,
            fullName = fullName,
            size = HeroPhotoSize,
            shape = CircleShape,
            ringWidth = 4.dp,
            ringColor = colors.primaryContainer,
            ringGap = 2.dp,
            ringGapColor = colors.surface,
            photoZoom = 1.1f
        )
    }
}

/** One of the three hero statistics: small icon, large value, short label. */
@Composable
private fun ProfileStatCard(
    value: String,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    valueDescription: String? = null
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = WseiInsetShape,
        color = colors.surfaceContainerLowest,
        contentColor = colors.onSurface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(WseiSpacing.m),
            verticalArrangement = Arrangement.spacedBy(WseiSpacing.xs)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(18.dp)
            )
            val valueStyle = MaterialTheme.typography.headlineSmall
            // Narrow columns (three stats on a small phone) shrink the value instead of clipping it.
            Text(
                text = value,
                style = valueStyle,
                fontWeight = FontWeight.Bold,
                color = colors.primary,
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = 14.sp,
                    maxFontSize = valueStyle.fontSize,
                    stepSize = 1.sp
                ),
                modifier = if (valueDescription != null) {
                    Modifier.semantics { contentDescription = valueDescription }
                } else {
                    Modifier
                }
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * The single study card: title and track summary ("Rok 1 · Semestr zimowy 2026/2027 ·
 * Niestacjonarne") are always visible; the known details (department, specialization, title,
 * previous semester average) expand below. Unknown values are left out instead of showing "—"
 * placeholders; without any detail the card is a static row. Nothing here repeats the hero.
 */
@Composable
private fun StudyCard(
    uiState: ScheduleUiState,
    averages: ProfileAverages,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val session = uiState.currentSession
    val colors = MaterialTheme.colorScheme
    val title = stringResource(R.string.profile_track_title)
    val summary = studySummary(uiState.studyTrack, session).joinToString(" · ").takeIf { it.isNotEmpty() }
    val details = listOfNotNull(
        session?.department?.trim()?.takeIf { it.isNotEmpty() }
            ?.let { stringResource(R.string.profile_department) to it },
        // Blank when the dean's office has no specialization: the row is omitted, not guessed.
        session?.specialization?.trim()?.takeIf { it.isNotEmpty() }
            ?.let { stringResource(R.string.profile_specialization) to it },
        session?.obtainedTitle?.trim()?.takeIf { it.isNotEmpty() }
            ?.let { stringResource(R.string.profile_obtained_title) to it }
    )
    val showPrevious = averages.hasPreviousSemester

    if (details.isEmpty() && !showPrevious) {
        ProfileStaticRow(title = title, supporting = summary, icon = WseiIcons.School, modifier = modifier)
        return
    }
    ProfileAccordion(
        title = title,
        supporting = summary,
        icon = WseiIcons.School,
        expanded = expanded,
        onToggle = onToggle,
        modifier = modifier
    ) {
        WseiInset {
            details.forEach { (label, value) -> WseiInfoRow(label, value) }
            if (showPrevious) {
                if (details.isNotEmpty()) {
                    HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.5f))
                }
                WseiInfoRow(
                    stringResource(R.string.profile_average_previous),
                    formatAverage(uiState, averages.previousSemester)
                )
            }
        }
    }
}

/**
 * Header shared by every study section (icon badge, title, optional supporting line), so the
 * accordions and the static rows line up exactly.
 */
@Composable
private fun RowScope.SectionHeaderContent(title: String, supporting: String?, icon: ImageVector) {
    WseiIconBadge(icon = icon)
    Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(WseiSpacing.xxs)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (supporting != null) {
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Expandable study section; [trailing] (count badge, ECTS pill) sits just before the chevron. */
@Composable
private fun ProfileAccordion(
    title: String,
    supporting: String?,
    icon: ImageVector,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    WseiExpandableCard(
        expanded = expanded,
        onToggle = onToggle,
        modifier = modifier,
        header = {
            SectionHeaderContent(title = title, supporting = supporting, icon = icon)
            trailing?.invoke()
        },
        body = { content() }
    )
}

/**
 * Non-expandable study section with the same geometry as [ProfileAccordion] (card shape, padding,
 * header), used when there is nothing to reveal — e.g. no study plan yet.
 */
@Composable
private fun ProfileStaticRow(
    title: String,
    supporting: String?,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    WseiCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WseiSpacing.m)
        ) {
            SectionHeaderContent(title = title, supporting = supporting, icon = icon)
        }
    }
}

/** Round count badge ("7") centred on the header row, the same height as the chevron container. */
@Composable
private fun CountBadge(count: Int) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = CircleShape,
        color = colors.secondaryContainer,
        contentColor = colors.onSecondaryContainer
    ) {
        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = 32.dp, minHeight = 32.dp)
                .padding(horizontal = WseiSpacing.s),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

/** Small chevron for list rows that reveal content below them. */
@Composable
private fun ExpandIndicator(expanded: Boolean) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = WseiMotion.spatial(),
        label = "profileRowChevron"
    )
    Icon(
        imageVector = WseiIcons.ExpandMore,
        contentDescription = null,
        modifier = Modifier
            .size(22.dp)
            .graphicsLayer { rotationZ = rotation }
    )
}

/** Password change fields; rendered inside the "Zmień hasło" segment, not as a separate card. */
@Composable
private fun PasswordChangeForm(onSubmit: (String, String) -> Unit, modifier: Modifier = Modifier) {
    // Passwords are deliberately kept out of the saved-instance-state Bundle (plain `remember`).
    var oldPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var showPasswords by rememberSaveable { mutableStateOf(false) }
    val transformation = if (showPasswords) VisualTransformation.None else PasswordVisualTransformation()
    val passwordKeyboard = KeyboardOptions(keyboardType = KeyboardType.Password)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(WseiSpacing.m)
    ) {
        OutlinedTextField(
            value = oldPass,
            onValueChange = { oldPass = it },
            label = { Text(stringResource(R.string.profile_current_password)) },
            visualTransformation = transformation,
            keyboardOptions = passwordKeyboard,
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = newPass,
            onValueChange = { newPass = it },
            label = { Text(stringResource(R.string.profile_new_password)) },
            visualTransformation = transformation,
            keyboardOptions = passwordKeyboard,
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            trailingIcon = {
                IconButton(onClick = { showPasswords = !showPasswords }) {
                    Icon(
                        imageVector = if (showPasswords) WseiIcons.VisibilityOff else WseiIcons.Visibility,
                        contentDescription = stringResource(
                            if (showPasswords) R.string.profile_hide_passwords else R.string.profile_show_passwords
                        )
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                onSubmit(oldPass, newPass)
                oldPass = ""
                newPass = ""
            },
            enabled = oldPass.isNotBlank() && newPass.isNotBlank(),
            shape = CircleShape,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = WseiSpacing.xs)
                .heightIn(min = 52.dp)
        ) {
            Text(stringResource(R.string.profile_save_password), fontWeight = FontWeight.SemiBold)
        }
    }
}
