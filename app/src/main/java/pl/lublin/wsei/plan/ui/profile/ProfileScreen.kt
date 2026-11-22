@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

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
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
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
import pl.lublin.wsei.plan.ui.components.WseiScreenScaffold
import pl.lublin.wsei.plan.ui.components.WseiSectionTitle
import pl.lublin.wsei.plan.ui.icons.WseiIcons
import pl.lublin.wsei.plan.ui.schedule.ScheduleUiState
import pl.lublin.wsei.plan.ui.theme.M3XCookie12Shape
import pl.lublin.wsei.plan.ui.theme.M3XCookieShape
import pl.lublin.wsei.plan.ui.theme.WseiCorners
import pl.lublin.wsei.plan.ui.theme.WseiHeroShape
import pl.lublin.wsei.plan.ui.theme.WseiInsetShape
import pl.lublin.wsei.plan.ui.theme.WseiMotion
import pl.lublin.wsei.plan.ui.theme.WseiSpacing
import pl.lublin.wsei.plan.ui.theme.wseiGroupItemShape
import java.util.Locale

/** Local UI state of the profile screen, shared by the single- and two-pane layouts. */
private class ProfileSectionsState(
    val isCourseExpanded: Boolean,
    val onToggleCourse: () -> Unit,
    val isGroupsExpanded: Boolean,
    val onToggleGroups: () -> Unit,
    val isPlanExpanded: Boolean,
    val onTogglePlan: () -> Unit,
    val showPasswordForm: Boolean,
    val onTogglePasswordForm: () -> Unit
)

/** Avatar size in the hero card (the outer tonal ring included). */
private val HeroAvatarSize = 136.dp

/**
 * Profil studenta: karta tożsamości, tok nauki, szczegóły studiów, grupy, plan studiów i konto.
 *
 * Na szerokich oknach karta studenta, tok nauki i konto są w lewej kolumnie, a szczegóły
 * studiów w prawej.
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
    var isCourseExpanded by rememberSaveable { mutableStateOf(false) }
    var isGroupsExpanded by rememberSaveable { mutableStateOf(false) }
    var isPlanExpanded by rememberSaveable { mutableStateOf(false) }
    var showPasswordForm by rememberSaveable { mutableStateOf(false) }
    val sections = ProfileSectionsState(
        isCourseExpanded = isCourseExpanded,
        onToggleCourse = { isCourseExpanded = !isCourseExpanded },
        isGroupsExpanded = isGroupsExpanded,
        onToggleGroups = { isGroupsExpanded = !isGroupsExpanded },
        isPlanExpanded = isPlanExpanded,
        onTogglePlan = { isPlanExpanded = !isPlanExpanded },
        showPasswordForm = showPasswordForm,
        onTogglePasswordForm = { showPasswordForm = !showPasswordForm }
    )
    val submitPassword: (String, String) -> Unit = { old, new ->
        onChangePassword(old, new)
        showPasswordForm = false
    }

    if (windowLayout.supportsTwoPane) {
        WseiScreenFrame(
            title = stringResource(R.string.nav_profile),
            onBack = onBack,
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
                        identityItems(uiState, Modifier)
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
                        studyItems(uiState, sections, Modifier)
                    }
                }
            )
        }
    } else {
        WseiScreenScaffold(
            title = stringResource(R.string.nav_profile),
            onBack = onBack,
            onRefresh = onRefresh,
            isRefreshing = uiState.isRefreshing,
            contentPadding = wseiScreenContentPadding()
        ) {
            val itemModifier = Modifier.wseiReadableWidth()
            identityItems(uiState, itemModifier)
            studyItems(uiState, sections, itemModifier)
            accountItems(uiState.currentSession, sections, onLogout, submitPassword, itemModifier)
        }
    }
}

// -----------------------------------------------------------------------------------------
// Etykiety
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

private fun formatAverage(uiState: ScheduleUiState, value: Double?): String =
    uiState.maskSensitive(String.format(Locale.US, "%.2f", value ?: 0.0))

/** Facts of "Tok nauki" that are actually known; every field is optional. */
private class StudyTrackFacts(
    val year: String?,
    val term: String?,
    val mode: String?,
    val course: String?
) {
    val isEmpty: Boolean get() = year == null && term == null && mode == null && course == null
}

private fun StudyTrackInfo?.hasAnyFact(): Boolean =
    this != null && (
        yearOfStudy != null ||
            !academicYear.isNullOrBlank() ||
            semesterTerm != null ||
            studyMode != StudyMode.UNKNOWN ||
            !courseCode.isNullOrBlank()
        )

@Composable
private fun studyTrackFacts(track: StudyTrackInfo): StudyTrackFacts {
    val termName = when (track.semesterTerm) {
        SemesterTerm.WINTER -> stringResource(R.string.profile_track_winter)
        SemesterTerm.SUMMER -> stringResource(R.string.profile_track_summer)
        null -> null
    }
    val academicYear = track.academicYear?.trim()?.takeIf { it.isNotEmpty() }
    val term = when {
        termName != null && academicYear != null -> stringResource(R.string.profile_track_term_year, termName, academicYear)
        termName != null -> termName
        academicYear != null -> stringResource(R.string.profile_track_academic_year, academicYear)
        else -> null
    }
    return StudyTrackFacts(
        year = track.yearOfStudy?.let { stringResource(R.string.profile_track_year, it) },
        term = term,
        mode = when (track.studyMode) {
            StudyMode.FULL_TIME -> stringResource(R.string.profile_track_full_time)
            StudyMode.PART_TIME -> stringResource(R.string.profile_track_part_time)
            StudyMode.UNKNOWN -> null
        },
        course = track.courseCode?.trim()?.takeIf { it.isNotEmpty() }
            ?.let { stringResource(R.string.profile_track_course, it) }
    )
}

// -----------------------------------------------------------------------------------------
// Sekcje listy
// -----------------------------------------------------------------------------------------

private fun LazyListScope.identityItems(uiState: ScheduleUiState, itemModifier: Modifier) {
    item(key = "identity", contentType = "identity") {
        IdentityCard(uiState = uiState, modifier = itemModifier)
    }
    val track = uiState.studyTrack
    if (track != null && track.hasAnyFact()) {
        item(key = "study_track", contentType = "study_track") {
            StudyTrackCard(track = track, modifier = itemModifier)
        }
    }
}

private fun LazyListScope.studyItems(
    uiState: ScheduleUiState,
    sections: ProfileSectionsState,
    itemModifier: Modifier
) {
    val session = uiState.currentSession

    item(key = "section_studies", contentType = "section") {
        WseiSectionTitle(stringResource(R.string.profile_section_studies), modifier = itemModifier)
    }

    item(key = "course_of_study", contentType = "accordion") {
        ProfileAccordion(
            title = stringResource(R.string.profile_study_details),
            badge = session?.courseOfStudyCode?.takeIf { it.isNotBlank() } ?: "—",
            icon = WseiIcons.School,
            expanded = sections.isCourseExpanded,
            onToggle = sections.onToggleCourse,
            modifier = itemModifier
        ) {
            WseiInset {
                WseiInfoRow(stringResource(R.string.profile_field_of_study), session?.fieldOfStudy ?: "—")
                WseiInfoRow(
                    stringResource(R.string.profile_specialization),
                    session?.specialization?.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.profile_specialization_general)
                )
                WseiInfoRow(stringResource(R.string.profile_department), session?.department ?: "—")
                WseiInfoRow(stringResource(R.string.profile_study_mode), session?.studyMode ?: "—")
                WseiInfoRow(stringResource(R.string.profile_obtained_title), session?.obtainedTitle ?: "—")
                WseiInfoRow(
                    stringResource(R.string.profile_year_semester),
                    session?.let {
                        stringResource(R.string.profile_year_semester_value, it.academicYear, it.semesterNumber)
                    } ?: "—"
                )
                WseiInfoRow(
                    stringResource(R.string.profile_group),
                    uiState.primaryStudentGroupLabel ?: stringResource(R.string.profile_no_group)
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                WseiInfoRow(stringResource(R.string.profile_average_current), formatAverage(uiState, session?.averageCurrentSemester))
                WseiInfoRow(stringResource(R.string.profile_average_previous), formatAverage(uiState, session?.averagePreviousSemester))
                WseiInfoRow(stringResource(R.string.profile_average_overall), formatAverage(uiState, session?.averageGrade))
            }
        }
    }

    item(key = "student_groups", contentType = "accordion") {
        // Class groups only: study-track entries ("ROK1/ N") belong to "Tok nauki" above.
        val semesters = uiState.classGroupsBySemester
        val semesterGroups = semesters.firstOrNull { it.groups.isNotEmpty() } ?: semesters.firstOrNull()
        val groups = semesterGroups?.groups.orEmpty()
        ProfileAccordion(
            title = stringResource(R.string.profile_student_groups),
            badge = "${groups.size}",
            icon = WseiIcons.Groups,
            expanded = sections.isGroupsExpanded,
            onToggle = sections.onToggleGroups,
            modifier = itemModifier
        ) {
            val semesterLabel = semesterGroups?.semesterLabel?.takeIf { it.isNotBlank() }
            if (semesterLabel != null) {
                Text(
                    text = semesterLabel,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (groups.isEmpty()) {
                Text(
                    text = stringResource(R.string.profile_groups_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
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
        val planSubjects = remember(uiState.studyPlans) {
            uiState.studyPlans.firstOrNull()?.semesters?.flatMap { it.subjects }.orEmpty()
        }
        val totalPlanEcts = remember(planSubjects) { planSubjects.sumOf { it.ects.toDouble() } }
        ProfileAccordion(
            title = stringResource(R.string.profile_study_plan),
            badge = stringResource(R.string.subjects_ects_value, totalPlanEcts.toInt()),
            icon = WseiIcons.AutoAwesome,
            expanded = sections.isPlanExpanded,
            onToggle = sections.onTogglePlan,
            modifier = itemModifier
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
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            trailing = { WseiPill(stringResource(R.string.subjects_ects_value, subject.ects.toInt())) }
                        )
                    }
                }
            )
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
                WseiListItem(
                    title = stringResource(R.string.profile_change_password),
                    supporting = stringResource(R.string.profile_change_password_supporting),
                    icon = WseiIcons.Lock,
                    shape = wseiGroupItemShape(0, count),
                    selected = sections.showPasswordForm,
                    onClick = sections.onTogglePasswordForm,
                    trailing = { ExpandIndicator(expanded = sections.showPasswordForm) }
                )
                AnimatedVisibility(
                    visible = sections.showPasswordForm,
                    enter = expandVertically(WseiMotion.size(), expandFrom = Alignment.Top) + fadeIn(WseiMotion.effects()),
                    exit = shrinkVertically(WseiMotion.size(), shrinkTowards = Alignment.Top) + fadeOut(WseiMotion.fastEffects())
                ) {
                    PasswordChangeForm(onSubmit = onSubmitPassword)
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
// Karty
// -----------------------------------------------------------------------------------------

@Composable
private fun IdentityCard(uiState: ScheduleUiState, modifier: Modifier = Modifier) {
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
                val subtitle = listOfNotNull(session?.fieldOfStudy, session?.studyMode)
                    .filter { it.isNotBlank() }
                    .joinToString(" • ")
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
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
            ProfileStatCard(
                value = formatAverage(uiState, session?.averageCurrentSemester),
                label = stringResource(R.string.profile_stat_semester_average),
                icon = WseiIcons.AutoAwesome,
                modifier = statModifier
            )
            ProfileStatCard(
                value = formatAverage(uiState, session?.averageGrade),
                label = stringResource(R.string.profile_stat_overall_average),
                icon = WseiIcons.WorkspacePremium,
                modifier = statModifier
            )
            ProfileStatCard(
                value = session?.semesterNumber?.toString() ?: "—",
                label = stringResource(R.string.profile_stat_semester),
                icon = WseiIcons.CalendarMonth,
                modifier = statModifier
            )
        }
    }
}

/**
 * Student photo framed by a tonal ring: an outer cookie in `primary`, a thin `primaryContainer`
 * gap and the photo clipped to the same (smaller) cookie. The photo never touches the card, so
 * the white background of ID photos does not bleed into the hero.
 */
@Composable
private fun ProfilePhoto(base64Photo: String?, fullName: String) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(HeroAvatarSize)
            .clip(M3XCookie12Shape)
            .background(colors.primary)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        StudentAvatar(
            base64Photo = base64Photo,
            fullName = fullName,
            size = HeroAvatarSize - 8.dp,
            shape = M3XCookie12Shape,
            ringWidth = 3.dp,
            ringColor = colors.primaryContainer,
            photoZoom = 1.08f
        )
    }
}

/** One of the three hero statistics: small icon, large value, short label. */
@Composable
private fun ProfileStatCard(
    value: String,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = WseiInsetShape,
        color = colors.surfaceContainerLowest,
        contentColor = colors.onSurface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = WseiSpacing.m, vertical = WseiSpacing.m),
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
                )
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

/** "Tok nauki": year of study, semester, mode and course — only the fields that are known. */
@Composable
private fun StudyTrackCard(track: StudyTrackInfo, modifier: Modifier = Modifier) {
    val facts = studyTrackFacts(track)
    if (facts.isEmpty) return
    val colors = MaterialTheme.colorScheme
    // The most specific fact is the headline; the rest become supporting text and chips.
    val headline = facts.year ?: facts.term ?: facts.mode ?: facts.course.orEmpty()
    val secondary = if (facts.year != null) facts.term else null
    val chips = listOfNotNull(
        facts.mode?.takeIf { it != headline }?.let { it to WseiIcons.Schedule },
        facts.course?.takeIf { it != headline }?.let { it to WseiIcons.School }
    )

    WseiCard(
        modifier = modifier,
        containerColor = colors.surfaceContainer,
        verticalSpacing = WseiSpacing.m
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WseiSpacing.l)
        ) {
            WseiIconBadge(
                icon = WseiIcons.School,
                shape = M3XCookieShape,
                containerColor = colors.tertiaryContainer,
                contentColor = colors.onTertiaryContainer,
                size = 52.dp,
                iconSize = 26.dp
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(WseiSpacing.xxs)
            ) {
                Text(
                    text = stringResource(R.string.profile_track_title),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary
                )
                Text(
                    text = headline,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (secondary != null) {
                    Text(
                        text = secondary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )
                }
            }
        }
        if (chips.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(WseiSpacing.s),
                verticalArrangement = Arrangement.spacedBy(WseiSpacing.s)
            ) {
                chips.forEach { (text, icon) ->
                    WseiPill(
                        text = text,
                        icon = icon,
                        containerColor = colors.secondaryContainer,
                        contentColor = colors.onSecondaryContainer,
                        emphasized = true
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileAccordion(
    title: String,
    badge: String,
    icon: ImageVector,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    WseiExpandableCard(
        expanded = expanded,
        onToggle = onToggle,
        modifier = modifier,
        header = {
            WseiIconBadge(icon = icon)
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            WseiPill(
                text = badge,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                emphasized = true
            )
        },
        body = { content() }
    )
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

@Composable
private fun PasswordChangeForm(onSubmit: (String, String) -> Unit) {
    // Passwords are deliberately kept out of the saved-instance-state Bundle (plain `remember`).
    var oldPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var showPasswords by rememberSaveable { mutableStateOf(false) }
    val transformation = if (showPasswords) VisualTransformation.None else PasswordVisualTransformation()
    val passwordKeyboard = KeyboardOptions(keyboardType = KeyboardType.Password)

    WseiCard(
        shape = RoundedCornerShape(WseiCorners.groupInner),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentPadding = PaddingValues(WseiSpacing.l)
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
