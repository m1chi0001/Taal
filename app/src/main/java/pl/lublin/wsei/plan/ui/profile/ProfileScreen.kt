package pl.lublin.wsei.plan.ui.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pl.lublin.wsei.plan.domain.model.ClassType
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
import pl.lublin.wsei.plan.ui.components.WseiScreenScaffold
import pl.lublin.wsei.plan.ui.components.WseiSectionTitle
import pl.lublin.wsei.plan.ui.components.WseiStat
import pl.lublin.wsei.plan.ui.icons.WseiIcons
import pl.lublin.wsei.plan.ui.schedule.ScheduleUiState
import pl.lublin.wsei.plan.ui.theme.M3XCookie12Shape
import pl.lublin.wsei.plan.ui.theme.WseiHeroShape
import pl.lublin.wsei.plan.ui.theme.WseiSpacing
import java.util.Locale

/** Profil studenta: karta tożsamości, tok studiów, grupy, plan studiów i konto. */
@Composable
fun ProfileScreen(
    uiState: ScheduleUiState,
    onBack: () -> Unit,
    onChangePassword: (String, String) -> Unit,
    onLogout: () -> Unit,
    onRefresh: () -> Unit
) {
    val session = uiState.currentSession
    var isCourseExpanded by rememberSaveable { mutableStateOf(false) }
    var isGroupsExpanded by rememberSaveable { mutableStateOf(false) }
    var isPlanExpanded by rememberSaveable { mutableStateOf(false) }
    var showPasswordForm by rememberSaveable { mutableStateOf(false) }

    fun average(value: Double?): String = uiState.maskSensitive(String.format(Locale.US, "%.2f", value ?: 0.0))
    val displayAlbum = uiState.maskSensitive(session?.albumNumber ?: "—")

    WseiScreenScaffold(
        title = "Profil",
        onBack = onBack,
        onRefresh = onRefresh,
        isRefreshing = uiState.isRefreshing
    ) {
        item(key = "identity") {
            WseiCard(
                shape = WseiHeroShape,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
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
                    Text(
                        text = listOfNotNull(
                            session?.fieldOfStudy,
                            session?.studyMode
                        ).joinToString(" • "),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(WseiSpacing.s)) {
                        WseiPill(
                            text = "Album $displayAlbum",
                            icon = WseiIcons.Badge,
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            emphasized = true
                        )
                        WseiPill(
                            text = session?.statusName ?: "Studiuje",
                            containerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f),
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(WseiSpacing.s)) {
                    val statColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)
                    val statContent = MaterialTheme.colorScheme.onPrimaryContainer
                    WseiStat(average(session?.averageCurrentSemester), "Śr. semestru", Modifier.weight(1f), statColor, statContent)
                    WseiStat(average(session?.averageGrade), "Śr. studiów", Modifier.weight(1f), statColor, statContent)
                    WseiStat("${session?.semesterNumber ?: 1}", "Semestr", Modifier.weight(1f), statColor, statContent)
                }
            }
        }

        item(key = "section_studies") { WseiSectionTitle("Studia") }

        item(key = "course_of_study") {
            ProfileAccordion(
                title = "Tok studiów",
                badge = session?.courseOfStudyCode ?: "—",
                icon = WseiIcons.School,
                expanded = isCourseExpanded,
                onToggle = { isCourseExpanded = !isCourseExpanded }
            ) {
                WseiInset {
                    WseiInfoRow("Kierunek", session?.fieldOfStudy ?: "—")
                    WseiInfoRow("Specjalność", session?.specialization?.ifBlank { "Ogólna" } ?: "Ogólna")
                    WseiInfoRow("Wydział", session?.department ?: "—")
                    WseiInfoRow("Tryb studiów", session?.studyMode ?: "—")
                    WseiInfoRow("Otrzymywany tytuł", session?.obtainedTitle ?: "—")
                    WseiInfoRow("Rok / semestr", "Rok ${session?.academicYear ?: 1} • Semestr ${session?.semesterNumber ?: 1}")
                    WseiInfoRow("Grupa", uiState.primaryStudentGroupLabel)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    WseiInfoRow("Średnia (bieżący sem.)", average(session?.averageCurrentSemester))
                    WseiInfoRow("Średnia (poprzedni sem.)", average(session?.averagePreviousSemester))
                    WseiInfoRow("Średnia ze studiów", average(session?.averageGrade))
                }
            }
        }

        item(key = "student_groups") {
            val groups = uiState.studentGroups.firstOrNull()?.groups.orEmpty()
            ProfileAccordion(
                title = "Grupy studenckie",
                badge = "${groups.size}",
                icon = WseiIcons.Groups,
                expanded = isGroupsExpanded,
                onToggle = { isGroupsExpanded = !isGroupsExpanded }
            ) {
                WseiListGroup(
                    entries = groups.map { group ->
                        WseiListEntry(key = group.groupCode) { shape ->
                            WseiListItem(
                                title = group.groupCode,
                                supporting = group.formName,
                                shape = shape,
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                trailing = { WseiPill("Gr. ${group.groupNumber}", emphasized = true) }
                            )
                        }
                    }
                )
            }
        }

        item(key = "study_plan") {
            val planSubjects = uiState.studyPlans.firstOrNull()?.semesters?.flatMap { it.subjects }.orEmpty()
            val totalPlanEcts = planSubjects.sumOf { it.ects.toDouble() }
            ProfileAccordion(
                title = "Plan studiów",
                badge = "${totalPlanEcts.toInt()} ECTS",
                icon = WseiIcons.AutoAwesome,
                expanded = isPlanExpanded,
                onToggle = { isPlanExpanded = !isPlanExpanded }
            ) {
                WseiListGroup(
                    entries = planSubjects.mapIndexed { index, subject ->
                        WseiListEntry(key = "plan_$index") { shape ->
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

        item(key = "section_account") { WseiSectionTitle("Konto") }

        item(key = "account_actions") {
            Column(verticalArrangement = Arrangement.spacedBy(WseiSpacing.inGroup)) {
                WseiListItem(
                    title = "Zmień hasło",
                    supporting = "Hasło do Wirtualnego Dziekanatu",
                    icon = WseiIcons.Lock,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 6.dp, bottomEnd = 6.dp),
                    selected = showPasswordForm,
                    onClick = { showPasswordForm = !showPasswordForm }
                )
                AnimatedVisibility(
                    visible = showPasswordForm,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    PasswordChangeForm(
                        onSubmit = { old, new ->
                            onChangePassword(old, new)
                            showPasswordForm = false
                        }
                    )
                }
                WseiListItem(
                    title = "Wyloguj się",
                    supporting = "Zakończ sesję na tym urządzeniu",
                    icon = WseiIcons.Logout,
                    iconContainerColor = MaterialTheme.colorScheme.errorContainer,
                    iconContentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 24.dp, bottomEnd = 24.dp),
                    onClick = onLogout
                )
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
    content: @Composable () -> Unit
) {
    WseiExpandableCard(
        expanded = expanded,
        onToggle = onToggle,
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
    var oldPass by rememberSaveable { mutableStateOf("") }
    var newPass by rememberSaveable { mutableStateOf("") }
    var showPasswords by rememberSaveable { mutableStateOf(false) }
    val transformation = if (showPasswords) VisualTransformation.None else PasswordVisualTransformation()

    WseiCard(
        shape = RoundedCornerShape(6.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentPadding = PaddingValues(WseiSpacing.l)
    ) {
        OutlinedTextField(
            value = oldPass,
            onValueChange = { oldPass = it },
            label = { Text("Obecne hasło") },
            visualTransformation = transformation,
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = newPass,
            onValueChange = { newPass = it },
            label = { Text("Nowe hasło") },
            visualTransformation = transformation,
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
                .height(52.dp)
                .padding(top = WseiSpacing.xs)
        ) {
            Text("Zapisz nowe hasło", fontWeight = FontWeight.SemiBold)
        }
    }
}
