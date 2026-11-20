package pl.lublin.wsei.plan.ui.profile

import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pl.lublin.wsei.plan.R
import pl.lublin.wsei.plan.domain.model.ClassType
import pl.lublin.wsei.plan.domain.model.StudentGroupItem
import pl.lublin.wsei.plan.domain.model.StudentSession
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
import pl.lublin.wsei.plan.ui.components.WseiStat
import pl.lublin.wsei.plan.ui.components.wseiMasked
import pl.lublin.wsei.plan.ui.icons.WseiIcons
import pl.lublin.wsei.plan.ui.schedule.ScheduleUiState
import pl.lublin.wsei.plan.ui.theme.M3XCookie12Shape
import pl.lublin.wsei.plan.ui.theme.WseiCorners
import pl.lublin.wsei.plan.ui.theme.WseiHeroShape
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

/**
 * Profil studenta: karta tożsamości, tok studiów, grupy, plan studiów i konto.
 *
 * Na szerokich oknach karta studenta i konto są w lewej kolumnie, a szczegóły studiów w prawej.
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
            title = "Profil",
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
            title = "Profil",
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

/** Title and supporting line of a student group row. */
private class StudentGroupLabel(val title: String, val supporting: String?)

/**
 * Exact group data from the dean's office: "Laboratorium · gr. 4" with the raw code
 * ("Lab4/1/I/N") below. Without a parsed group number the code itself is shown instead —
 * never a list index or a made-up number.
 */
@Composable
private fun studentGroupLabel(group: StudentGroupItem): StudentGroupLabel {
    val form = group.formName.trim()
    // Parsed from groupCode, so it is right even for stale cached data; "" when unknown.
    val number = group.displayGroupNumber.trim()
    val code = group.groupCode.trim()
    return if (number.isNotEmpty()) {
        val numberLabel = stringResource(R.string.profile_group_number, number)
        StudentGroupLabel(
            title = listOf(form, numberLabel).filter { it.isNotEmpty() }.joinToString(" · "),
            supporting = code.takeIf { it.isNotEmpty() }
        )
    } else {
        StudentGroupLabel(
            title = listOf(form, code).filter { it.isNotEmpty() }.distinct().joinToString(" · ").ifEmpty { "—" },
            supporting = null
        )
    }
}

private fun formatAverage(value: Double?, hidden: Boolean): String =
    wseiMasked(String.format(Locale.US, "%.2f", value ?: 0.0), hidden)

// -----------------------------------------------------------------------------------------
// Sekcje listy
// -----------------------------------------------------------------------------------------

private fun LazyListScope.identityItems(uiState: ScheduleUiState, itemModifier: Modifier) {
    item(key = "identity", contentType = "identity") {
        IdentityCard(uiState = uiState, modifier = itemModifier)
    }
}

private fun LazyListScope.studyItems(
    uiState: ScheduleUiState,
    sections: ProfileSectionsState,
    itemModifier: Modifier
) {
    val session = uiState.currentSession
    val hidden = uiState.isHideSensitiveData

    item(key = "section_studies", contentType = "section") {
        WseiSectionTitle("Studia", modifier = itemModifier)
    }

    item(key = "course_of_study", contentType = "accordion") {
        ProfileAccordion(
            title = "Tok studiów",
            badge = session?.courseOfStudyCode ?: "—",
            icon = WseiIcons.School,
            expanded = sections.isCourseExpanded,
            onToggle = sections.onToggleCourse,
            modifier = itemModifier
        ) {
            WseiInset {
                WseiInfoRow("Kierunek", session?.fieldOfStudy ?: "—")
                WseiInfoRow("Specjalność", session?.specialization?.ifBlank { "Ogólna" } ?: "Ogólna")
                WseiInfoRow("Wydział", session?.department ?: "—")
                WseiInfoRow("Tryb studiów", session?.studyMode ?: "—")
                WseiInfoRow("Otrzymywany tytuł", session?.obtainedTitle ?: "—")
                WseiInfoRow(
                    "Rok / semestr",
                    session?.let { "Rok ${it.academicYear} • Semestr ${it.semesterNumber}" } ?: "—"
                )
                WseiInfoRow("Grupa", uiState.primaryStudentGroupLabel)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                WseiInfoRow("Średnia (bieżący sem.)", formatAverage(session?.averageCurrentSemester, hidden))
                WseiInfoRow("Średnia (poprzedni sem.)", formatAverage(session?.averagePreviousSemester, hidden))
                WseiInfoRow("Średnia ze studiów", formatAverage(session?.averageGrade, hidden))
            }
        }
    }

    item(key = "student_groups", contentType = "accordion") {
        val semesterGroups = uiState.studentGroups.firstOrNull()
        val groups = semesterGroups?.groups.orEmpty()
        ProfileAccordion(
            title = "Grupy studenckie",
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
                        WseiListEntry(key = "group_${group.id}_$index") { shape ->
                            val label = studentGroupLabel(group)
                            WseiListItem(
                                title = label.title,
                                supporting = label.supporting,
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
            title = "Plan studiów",
            badge = "${totalPlanEcts.toInt()} ECTS",
            icon = WseiIcons.AutoAwesome,
            expanded = sections.isPlanExpanded,
            onToggle = sections.onTogglePlan,
            modifier = itemModifier
        ) {
            WseiListGroup(
                entries = planSubjects.mapIndexed { index, subject ->
                    WseiListEntry(key = "plan_${subject.id}_$index") { shape ->
                        WseiListItem(
                            title = "${subject.subjectNumber}. ${subject.subjectName}",
                            supporting = "${subject.form} • ${subject.passingForm} • ${subject.hours} godz.",
                            icon = WseiIcons.forSubject(subject.subjectName, ClassType.fromRawCode(subject.form)),
                            shape = shape,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            trailing = { WseiPill("${subject.ects.toInt()} ECTS") }
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
        WseiSectionTitle("Konto", modifier = itemModifier)
    }
    item(key = "account_actions", contentType = "account") {
        val count = if (canChangePassword) 2 else 1
        Column(
            modifier = itemModifier,
            verticalArrangement = Arrangement.spacedBy(WseiSpacing.inGroup)
        ) {
            if (canChangePassword) {
                WseiListItem(
                    title = "Zmień hasło",
                    supporting = "Hasło do Wirtualnego Dziekanatu",
                    icon = WseiIcons.Lock,
                    shape = wseiGroupItemShape(0, count),
                    selected = sections.showPasswordForm,
                    onClick = sections.onTogglePasswordForm
                )
                AnimatedVisibility(
                    visible = sections.showPasswordForm,
                    enter = expandVertically(WseiMotion.size()) + fadeIn(WseiMotion.effects()),
                    exit = shrinkVertically(WseiMotion.size()) + fadeOut(WseiMotion.effects())
                ) {
                    PasswordChangeForm(onSubmit = onSubmitPassword)
                }
            }
            WseiListItem(
                title = "Wyloguj się",
                supporting = "Zakończ sesję na tym urządzeniu",
                icon = WseiIcons.Logout,
                iconContainerColor = MaterialTheme.colorScheme.errorContainer,
                iconContentColor = MaterialTheme.colorScheme.onErrorContainer,
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
    val hidden = uiState.isHideSensitiveData
    val colors = MaterialTheme.colorScheme
    val displayAlbum = wseiMasked(session?.albumNumber ?: "—", hidden)

    WseiCard(
        modifier = modifier,
        shape = WseiHeroShape,
        containerColor = colors.primaryContainer,
        contentColor = colors.onPrimaryContainer,
        verticalSpacing = WseiSpacing.l
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(WseiSpacing.m)
        ) {
            StudentAvatar(
                base64Photo = session?.personPictureBase64,
                fullName = session?.fullName ?: "Student",
                size = 112.dp,
                shape = M3XCookie12Shape
            )
            Text(
                text = session?.fullName ?: "Student",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
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
            // FlowRow: on narrow panes the pills wrap instead of being clipped.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(WseiSpacing.s, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(WseiSpacing.s)
            ) {
                WseiPill(
                    text = "Album $displayAlbum",
                    icon = WseiIcons.Badge,
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                    emphasized = true
                )
                WseiPill(
                    text = session?.statusName ?: "Studiuje",
                    containerColor = colors.onPrimaryContainer.copy(alpha = 0.1f),
                    contentColor = colors.onPrimaryContainer
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(WseiSpacing.s)) {
            val statColor = colors.onPrimaryContainer.copy(alpha = 0.1f)
            val statContent = colors.onPrimaryContainer
            WseiStat(formatAverage(session?.averageCurrentSemester, hidden), "Śr. semestru", Modifier.weight(1f), statColor, statContent)
            WseiStat(formatAverage(session?.averageGrade, hidden), "Śr. studiów", Modifier.weight(1f), statColor, statContent)
            WseiStat(session?.semesterNumber?.toString() ?: "—", "Semestr", Modifier.weight(1f), statColor, statContent)
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
            label = { Text("Obecne hasło") },
            visualTransformation = transformation,
            keyboardOptions = passwordKeyboard,
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = newPass,
            onValueChange = { newPass = it },
            label = { Text("Nowe hasło") },
            visualTransformation = transformation,
            keyboardOptions = passwordKeyboard,
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            trailingIcon = {
                IconButton(onClick = { showPasswords = !showPasswords }) {
                    Icon(
                        imageVector = if (showPasswords) WseiIcons.VisibilityOff else WseiIcons.Visibility,
                        contentDescription = if (showPasswords) "Ukryj hasła" else "Pokaż hasła"
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
            Text("Zapisz nowe hasło", fontWeight = FontWeight.SemiBold)
        }
    }
}
