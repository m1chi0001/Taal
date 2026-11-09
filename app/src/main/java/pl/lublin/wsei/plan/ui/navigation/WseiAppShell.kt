@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
package pl.lublin.wsei.plan.ui.navigation

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.LocalDate
import pl.lublin.wsei.plan.domain.model.AnnouncementAttachment
import pl.lublin.wsei.plan.domain.model.ClassEvent
import pl.lublin.wsei.plan.domain.model.ClassType
import pl.lublin.wsei.plan.domain.model.DeanAnnouncement
import pl.lublin.wsei.plan.domain.model.DeliveryMode
import pl.lublin.wsei.plan.domain.model.FinancialObligation
import pl.lublin.wsei.plan.domain.model.GradeItem
import pl.lublin.wsei.plan.domain.model.ScheduleFilterType
import pl.lublin.wsei.plan.domain.model.ScholarshipApplication
import pl.lublin.wsei.plan.domain.model.formatPolishBankAccount
import pl.lublin.wsei.plan.ui.components.WseiSplashScreen
import pl.lublin.wsei.plan.ui.components.WseiWelcomeScreen
import pl.lublin.wsei.plan.ui.components.WseiScreenScaffold
import pl.lublin.wsei.plan.ui.icons.WseiIcons
import pl.lublin.wsei.plan.ui.login.LoginScreen
import pl.lublin.wsei.plan.ui.schedule.AppThemeMode
import pl.lublin.wsei.plan.ui.schedule.ScheduleScreen
import pl.lublin.wsei.plan.ui.schedule.ScheduleUiState
import pl.lublin.wsei.plan.ui.theme.ActiveHeroCardAsymmetricShape
import pl.lublin.wsei.plan.ui.theme.M3XArchShape
import pl.lublin.wsei.plan.ui.theme.M3XFloatingPillShape
import pl.lublin.wsei.plan.ui.theme.M3XSquircleShape
import pl.lublin.wsei.plan.ui.theme.rememberDecodedStudentPhoto
import java.util.Locale

/**
 * Główne destynacje dolnego paska nawigacyjnego Material 3 Expressive:
 * 1. Start (`START`) - Kafelki: Profil, Skrócony plan zajęć, Najbliższa opłata
 * 2. Plan Zajęć (`SCHEDULE`) - Kalendarz tygodniowy/miesięczny i oś czasu zajęć
 * 3. Przedmioty (`SUBJECTS`) - Wykaz przedmiotów z dedykowaną ikonografią i ocenami
 * 4. Finanse (`FINANCES`) - Zobowiązania czesnego oraz decyzje stypendialne
 * 5. Więcej (`MORE`) - Hub: Ogłoszenia uczelniane, Ustawienia aplikacji, O aplikacji
 * 6. Profil (`PROFILE`) - Dedykowany przycisk squircle z miniaturą zdjęcia studenta na prawym końcu paska
 */
enum class WseiDestination(
    val label: String,
    val contentDescription: String
) {
    START(
        label = "Start",
        contentDescription = "Ekran startowy"
    ),
    SCHEDULE(
        label = "Plan Zajęć",
        contentDescription = "Plan zajęć i kalendarz akademicki"
    ),
    SUBJECTS(
        label = "Przedmioty",
        contentDescription = "Przedmioty, punkty ECTS i oceny"
    ),
    FINANCES(
        label = "Finanse",
        contentDescription = "Finanse, czesne i stypendia"
    ),
    MORE(
        label = "Więcej",
        contentDescription = "Ogłoszenia uczelniane, ustawienia i o aplikacji"
    ),
    PROFILE(
        label = "Profil",
        contentDescription = "Karta tożsamości studenta, dane toku i grupy"
    );

    val iconVector: ImageVector
        get() = when (this) {
            START -> WseiIcons.Home
            SCHEDULE -> WseiIcons.CalendarMonth
            SUBJECTS -> WseiIcons.School
            FINANCES -> WseiIcons.AccountBalanceWallet
            MORE -> WseiIcons.Apps
            PROFILE -> WseiIcons.Person
        }
}

enum class MoreSubView {
    HUB,
    ANNOUNCEMENTS,
    SETTINGS,
    ABOUT
}

private enum class AuthPhase { SPLASH, WELCOME, LOGIN, APP }

@Composable
fun WseiAppShell(
    scheduleUiState: ScheduleUiState,
    onLoginSubmit: (username: String, password: String, rememberCredentials: Boolean) -> Unit,
    onQuickDebugLogin: () -> Unit,
    onLogout: () -> Unit,
    onChangePassword: (currentPassword: String, newPassword: String) -> Unit,
    onSelectDay: (Int) -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onToggleCalendarExpanded: () -> Unit,
    onPreviousCalendarPeriod: () -> Unit,
    onNextCalendarPeriod: () -> Unit,
    onSelectScheduleFilter: (ScheduleFilterType) -> Unit,
    onToggleHeroExpanded: () -> Unit,
    onSyncLiveNotification: (ClassEvent) -> Unit,
    onRefreshSchedule: () -> Unit,
    onInitiateOnlinePayment: (FinancialObligation, (String) -> Unit) -> Unit,
    onMarkAnnouncementRead: (DeanAnnouncement) -> Unit,
    onDownloadAttachment: (AnnouncementAttachment) -> Unit,
    onDismissStatusBanner: () -> Unit,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    onToggleLiveNotification: (Boolean) -> Unit,
    onToggleHideSensitiveData: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentDestination by rememberSaveable { mutableStateOf(WseiDestination.START) }
    var moreSubView by rememberSaveable { mutableStateOf(MoreSubView.HUB) }
    val isLoggedIn = scheduleUiState.currentSession != null
    // Systemowy gest/przycisk „wstecz": najpierw podekran „Więcej" -> hub, potem dowolna zakładka -> Start.
    BackHandler(
        enabled = isLoggedIn && (
            currentDestination != WseiDestination.START ||
                moreSubView != MoreSubView.HUB
            )
    ) {
        if (currentDestination == WseiDestination.MORE && moreSubView != MoreSubView.HUB) {
            moreSubView = MoreSubView.HUB
        } else {
            moreSubView = MoreSubView.HUB
            currentDestination = WseiDestination.START
        }
    }

    // Ekran powitalny pokazujemy raz, przed pierwszym logowaniem. Po wylogowaniu od razu wracamy do logowania.
    val context = LocalContext.current
    val welcomePrefs = remember(context) {
        context.getSharedPreferences("taal_prefs", android.content.Context.MODE_PRIVATE)
    }
    var welcomeSeen by rememberSaveable { mutableStateOf(welcomePrefs.getBoolean("welcome_seen", false)) }

    // Po wylogowaniu następne logowanie zaczyna się od zakładki Start, nie od ekranu, z którego wylogowano.
    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn) {
            currentDestination = WseiDestination.START
            moreSubView = MoreSubView.HUB
        }
    }

    val phase = when {
        isLoggedIn -> AuthPhase.APP
        scheduleUiState.isSessionRestoring -> AuthPhase.SPLASH
        !welcomeSeen -> AuthPhase.WELCOME
        else -> AuthPhase.LOGIN
    }

    AnimatedContent(
        targetState = phase,
        transitionSpec = {
            fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) togetherWith
                fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
        },
        modifier = modifier.fillMaxSize(),
        label = "authToShellTransition"
    ) { currentPhase ->
        if (currentPhase == AuthPhase.SPLASH) {
            WseiSplashScreen()
        } else if (currentPhase == AuthPhase.WELCOME) {
            WseiWelcomeScreen(
                onContinue = {
                    welcomePrefs.edit().putBoolean("welcome_seen", true).apply()
                    welcomeSeen = true
                }
            )
        } else if (currentPhase == AuthPhase.LOGIN) {
            LoginScreen(
                isAuthenticating = scheduleUiState.isAuthenticating,
                errorMessage = scheduleUiState.authError,
                savedLoginHint = scheduleUiState.savedEncryptedLoginHint,
                onLoginSubmit = onLoginSubmit,
                onQuickDebugLogin = onQuickDebugLogin
            )
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    AnimatedVisibility(visible = scheduleUiState.statusBannerMessage != null) {
                        scheduleUiState.statusBannerMessage?.let { msg ->
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onDismissStatusBanner() }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = msg,
                                        style = MaterialTheme.typography.labelLarge,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "OK",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        AnimatedContent(
                            targetState = currentDestination,
                            transitionSpec = {
                                (fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
                                    scaleIn(initialScale = 0.96f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))) togetherWith
                                    fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium))
                            },
                            modifier = Modifier.fillMaxSize(),
                            label = "destinationTransition"
                        ) { destination ->
                        when (destination) {
                            WseiDestination.START -> {
                                StartDashboardScreen(
                                    uiState = scheduleUiState,
                                    onOpenProfile = { currentDestination = WseiDestination.PROFILE },
                                    onOpenSchedule = { currentDestination = WseiDestination.SCHEDULE },
                                    onOpenFinances = { currentDestination = WseiDestination.FINANCES },
                                    onOpenSettings = {
                                        moreSubView = MoreSubView.SETTINGS
                                        currentDestination = WseiDestination.MORE
                                    },
                                    onOpenAbout = {
                                        moreSubView = MoreSubView.ABOUT
                                        currentDestination = WseiDestination.MORE
                                    },
                                    onRefresh = onRefreshSchedule
                                )
                            }
                            WseiDestination.SCHEDULE -> {
                                ScheduleScreen(
                                    uiState = scheduleUiState,
                                    onSelectDay = onSelectDay,
                                    onSelectDate = onSelectDate,
                                    onToggleCalendarExpanded = onToggleCalendarExpanded,
                                    onPreviousCalendarPeriod = onPreviousCalendarPeriod,
                                    onNextCalendarPeriod = onNextCalendarPeriod,
                                    onSelectScheduleFilter = onSelectScheduleFilter,
                                    onToggleHeroExpanded = onToggleHeroExpanded,
                                    onSyncLiveNotification = onSyncLiveNotification,
                                    onRefreshSchedule = onRefreshSchedule,
                                    onLogout = onLogout
                                )
                            }
                            WseiDestination.SUBJECTS -> {
                                GradesAndSubjectsScreen(
                                    uiState = scheduleUiState,
                                    onRefresh = onRefreshSchedule
                                )
                            }
                            WseiDestination.FINANCES -> {
                                FinancesScreen(
                                    uiState = scheduleUiState,
                                    onInitiateOnlinePayment = onInitiateOnlinePayment,
                                    onRefresh = onRefreshSchedule
                                )
                            }
                            WseiDestination.MORE -> {
                                MoreHubScreen(
                                    uiState = scheduleUiState,
                                    currentSubView = moreSubView,
                                    onNavigateSubView = { moreSubView = it },
                                    onMarkAnnouncementRead = onMarkAnnouncementRead,
                                    onDownloadAttachment = onDownloadAttachment,
                                    onSelectThemeMode = onSelectThemeMode,
                                    onToggleLiveNotification = onToggleLiveNotification,
                                    onToggleHideSensitiveData = onToggleHideSensitiveData,
                                    onRefresh = onRefreshSchedule
                                )
                            }
                            WseiDestination.PROFILE -> {
                                StudentProfileParityScreen(
                                    uiState = scheduleUiState,
                                    onChangePassword = onChangePassword,
                                    onLogout = onLogout,
                                    onRefresh = onRefreshSchedule
                                )
                            }
                        }
                        }
                    }
                }

                // Nowy pływający pasek nawigacyjny Material 3 Expressive (5 pozycji + miniatura profilu studenta)
                FloatingIconOnlyNavigationPill(
                    currentDestination = currentDestination,
                    unreadAnnouncementsCount = scheduleUiState.announcements.count { !it.isRead },
                    base64Photo = scheduleUiState.currentSession?.personPictureBase64,
                    fullName = scheduleUiState.currentSession?.fullName ?: "Student",
                    onSelectDestination = { currentDestination = it },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 14.dp, start = 16.dp, end = 16.dp)
                )
            }
        }
    }
}

/**
 * Pływający dolny pasek nawigacyjny Material 3 Expressive:
 * - Po lewej: Pigułka nawigacyjna z 5 głównymi zakładkami (`Start`, `Plan Zajęć`, `Przedmioty`, `Finanse`, `Więcej`)
 * - Po prawej: Oddzielny pływający przycisk Profilu ze zdjęciem studenta
 */
@Composable
private fun FloatingIconOnlyNavigationPill(
    currentDestination: WseiDestination,
    unreadAnnouncementsCount: Int,
    base64Photo: String?,
    fullName: String,
    onSelectDestination: (WseiDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Pigułka nawigacyjna (5 ikon)
        Surface(
            shape = M3XFloatingPillShape,
            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.96f),
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 10.dp,
            shadowElevation = 18.dp,
            modifier = Modifier
                .height(66.dp)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                    shape = M3XFloatingPillShape
                )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val mainDestinations = remember {
                    listOf(
                        WseiDestination.START,
                        WseiDestination.SCHEDULE,
                        WseiDestination.SUBJECTS,
                        WseiDestination.FINANCES,
                        WseiDestination.MORE
                    )
                }

                mainDestinations.forEach { destination ->
                    val selected = destination == currentDestination
                    val itemWidth by animateDpAsState(
                        targetValue = if (selected) 56.dp else 44.dp,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "pillItemWidth"
                    )
                    val indicatorCorner by animateDpAsState(
                        targetValue = if (selected) 24.dp else 16.dp,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "pillIndicatorCorner"
                    )
                    val containerColor by animateColorAsState(
                        targetValue = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0f)
                        },
                        label = "pillContainerColor"
                    )
                    val iconTint by animateColorAsState(
                        targetValue = if (selected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        label = "pillIconTint"
                    )

                    Surface(
                        onClick = { onSelectDestination(destination) },
                        shape = RoundedCornerShape(indicatorCorner),
                        color = containerColor,
                        modifier = Modifier
                            .width(itemWidth)
                            .height(50.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = destination.iconVector,
                                contentDescription = destination.contentDescription,
                                tint = iconTint,
                                modifier = Modifier.size(if (selected) 25.dp else 22.dp)
                            )

                            if (destination == WseiDestination.MORE && unreadAnnouncementsCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 9.dp, end = 9.dp)
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (selected) MaterialTheme.colorScheme.tertiaryContainer
                                            else MaterialTheme.colorScheme.error
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Oddzielny pływający przycisk Profilu po prawej stronie
        val isProfileSelected = currentDestination == WseiDestination.PROFILE
        val profileBorderColor by animateColorAsState(
            targetValue = if (isProfileSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
            label = "profileBorderColor"
        )
        val profileContainerColor by animateColorAsState(
            targetValue = if (isProfileSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.96f)
            },
            label = "profileContainerColor"
        )

        Surface(
            onClick = { onSelectDestination(WseiDestination.PROFILE) },
            shape = RoundedCornerShape(22.dp),
            color = profileContainerColor,
            tonalElevation = 10.dp,
            shadowElevation = 18.dp,
            border = BorderStroke(if (isProfileSelected) 2.5.dp else 1.dp, profileBorderColor),
            modifier = Modifier.size(66.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                StudentAvatarPhoto(
                    base64Photo = base64Photo,
                    fullName = fullName,
                    size = 48.dp,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    }
}

// =========================================================================================
// A. Ekran Startowy (`StartDashboardScreen`) z 3 spójnymi kafelkami M3X i Pull-to-Refresh
// =========================================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StartDashboardScreen(
    uiState: ScheduleUiState,
    onOpenProfile: () -> Unit,
    onOpenSchedule: () -> Unit,
    onOpenFinances: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    onRefresh: () -> Unit
) {
    WseiScreenScaffold(
        title = "Taal",
        icon = WseiIcons.TaalVolcano,
        onRefresh = onRefresh,
        isRefreshing = uiState.isRefreshing
    ) {
        // 1. KAFELEK: PROFIL STUDENTA (zdjęcie, imię i nazwisko, tok studiów, numer albumu)
        item(key = "tile_student_profile") {
            StudentProfileStartTile(
                uiState = uiState,
                onClick = onOpenProfile
            )
        }

        // 2. KAFELEK: SKRÓCONY PLAN ZAJĘĆ (uproszczony pasek pojedynczych bieżących zajęć)
        item(key = "tile_short_schedule") {
            ShortenedScheduleStartTile(
                uiState = uiState,
                onClick = onOpenSchedule
            )
        }

        // 3. KAFELEK: INFORMACJA O NAJBLIŻSZEJ OPŁACIE
        item(key = "tile_next_payment") {
            NextPaymentStartTile(
                uiState = uiState,
                onClick = onOpenFinances
            )
        }
    }
}

@Composable
private fun StudentProfileStartTile(
    uiState: ScheduleUiState,
    onClick: () -> Unit
) {
    val session = uiState.currentSession
    val fullName = session?.fullName ?: "Student WSEI"
    val courseOfStudy = session?.courseOfStudyCode?.ifBlank { session.fieldOfStudy } ?: "Tok studiów"
    val rawAlbum = session?.albumNumber ?: "—"
    val displayAlbum = uiState.maskSensitive(rawAlbum)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        tonalElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            StudentAvatarPhoto(
                base64Photo = session?.personPictureBase64,
                fullName = fullName,
                size = 78.dp,
                shape = RoundedCornerShape(22.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = WseiIcons.Badge,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Album $displayAlbum",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = fullName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Tok studiów: $courseOfStudy • Semestr ${session?.semesterNumber ?: 1}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.88f)
                )
            }

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = WseiIcons.ChevronRight,
                        contentDescription = "Przejdź do profilu",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Kafelek: Skrócony plan zajęć.
 * Zgodnie z wytycznymi: pozostawiono WYŁĄCZNIE pasek postępu bieżących zajęć.
 * Pasek całego dnia z przerwami usunięto z ekranu startowego.
 */
@Composable
private fun ShortenedScheduleStartTile(
    uiState: ScheduleUiState,
    onClick: () -> Unit
) {
    val todayJava = java.time.LocalDate.now()
    val todayIso = todayJava.toString()
    val todayEvents = uiState.allEvents
        .filter { it.dateIso == todayIso }
        .sortedBy { it.timeSlot.startEpochMillis }

    val upcomingDayEvents = if (todayEvents.isNotEmpty()) {
        todayEvents
    } else {
        val nextDateIso = uiState.allEvents
            .map { it.dateIso }
            .filter { it >= todayIso }
            .minOrNull()
            ?: uiState.selectedDate.toString()
        uiState.allEvents
            .filter { it.dateIso == nextDateIso }
            .sortedBy { it.timeSlot.startEpochMillis }
    }

    val displayedDateIso = upcomingDayEvents.firstOrNull()?.dateIso ?: todayIso
    val displayedJavaDate = runCatching { java.time.LocalDate.parse(displayedDateIso) }.getOrDefault(todayJava)
    val isTodaySchedule = displayedDateIso == todayIso
    val activeClass = uiState.activeClass
    val dateFormatter = java.time.format.DateTimeFormatter.ofPattern("EEEE, d MMM", Locale("pl", "PL"))

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = M3XSquircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = WseiIcons.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = if (isTodaySchedule) "Dzisiaj" else "Najbliższy zjazd",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = displayedJavaDate.format(dateFormatter).replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Text(
                        text = "${upcomingDayEvents.size} zaj.",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // Pasek postępu trwających zajęć (wyłącznie pojedyncze bieżące zajęcia)
            if (activeClass != null) {
                val animatedProgress by animateFloatAsState(
                    targetValue = activeClass.progressFraction(uiState.nowEpochMillis),
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "startActiveClassProgress"
                )
                val elapsedMin = activeClass.elapsedMinutes(uiState.nowEpochMillis)
                val totalMin = activeClass.totalDurationMinutes

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = WseiIcons.forSubject(activeClass.subject.name, activeClass.classType),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Trwa: ${activeClass.subject.name}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Text(
                                text = "$elapsedMin z $totalMin min • do ${activeClass.timeSlot.endFormatted}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        LinearWavyProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(14.dp),
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            trackColor = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.2f)
                        )
                    }
                }
            }

            if (upcomingDayEvents.isEmpty()) {
                Text(
                    text = "Brak zaplanowanych zajęć w bieżącym okresie.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                upcomingDayEvents.take(4).forEach { event ->
                    val isCurrentlyRunning = activeClass?.id == event.id
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isCurrentlyRunning) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = M3XSquircleShape,
                                color = if (isCurrentlyRunning) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.secondaryContainer
                                },
                                contentColor = if (isCurrentlyRunning) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = WseiIcons.forSubject(event.subject.name, event.classType),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = event.subject.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val locationAndModeLabel = when (event.deliveryMode) {
                                    DeliveryMode.ELEARNING -> "E-learning"
                                    DeliveryMode.REMOTE -> "Streaming"
                                    DeliveryMode.ON_SITE -> "Sala ${event.room.number} • Stacjonarnie"
                                }
                                Text(
                                    text = "${event.classType.polishLabel} • $locationAndModeLabel",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHighest
                            ) {
                                Text(
                                    text = "${event.timeSlot.startFormatted}–${event.timeSlot.endFormatted}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                if (upcomingDayEvents.size > 4) {
                    Text(
                        text = "+ ${upcomingDayEvents.size - 4} więcej • Otwórz plan",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun NextPaymentStartTile(
    uiState: ScheduleUiState,
    onClick: () -> Unit
) {
    val today = java.time.LocalDate.now()
    val currentMonthSuffix = String.format(Locale.US, ".%02d.%04d", today.monthValue, today.year)

    val unpaidObligations = uiState.financials.filter { !it.isPaid }
    val dueThisMonth = unpaidObligations.firstOrNull {
        it.dueDateIso.trim().endsWith(currentMonthSuffix)
    }
    val nextUnpaid = dueThisMonth ?: unpaidObligations.firstOrNull()
    val isUrgentThisMonth = dueThisMonth != null

    val containerColor = if (isUrgentThisMonth) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = if (isUrgentThisMonth) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        contentColor = contentColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = M3XSquircleShape,
                        color = contentColor.copy(alpha = 0.14f),
                        contentColor = contentColor,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = WseiIcons.AccountBalanceWallet,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = if (isUrgentThisMonth) {
                                "Termin w tym miesiącu"
                            } else if (nextUnpaid != null) {
                                "Najbliższa opłata"
                            } else {
                                "Opłaty uregulowane"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = when {
                                isUrgentThisMonth -> "Opłata do uregulowania w tym miesiącu"
                                nextUnpaid != null -> nextUnpaid.title
                                else -> "Brak zaległych opłat w bieżącym okresie"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Icon(
                    imageVector = WseiIcons.ChevronRight,
                    contentDescription = "Przejdź do finansów",
                    modifier = Modifier.size(20.dp)
                )
            }

            if (nextUnpaid != null) {
                val amountStr = String.format(Locale.US, "%.2f PLN", kotlin.math.abs(nextUnpaid.currentStatePln))
                val displayAmount = uiState.maskSensitive(amountStr)

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = contentColor.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = nextUnpaid.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Termin płatności: ${nextUnpaid.dueDateIso}",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        Text(
                            text = displayAmount,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Text(
                    text = "Wszystkie opłaty są uregulowane.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

/**
 * Komponent wyświetlający zdjęcie studenta pobrane z Wirtualnego Dziekanatu
 */
@Composable
fun StudentAvatarPhoto(
    base64Photo: String?,
    fullName: String,
    size: Dp,
    shape: Shape = RoundedCornerShape(22.dp),
    modifier: Modifier = Modifier
) {
    val decodedBitmap = rememberDecodedStudentPhoto(base64Photo)
    val initials = fullName
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifEmpty { "ST" }

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(MaterialTheme.colorScheme.primary)
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f),
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (decodedBitmap != null) {
            Image(
                bitmap = decodedBitmap,
                contentDescription = "Zdjęcie studenta $fullName",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = initials,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// =========================================================================================
// B. Przedmioty i Oceny (`GradesAndSubjectsScreen`) — wzorowane 1:1 na kartach Ekranu Start
// =========================================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GradesAndSubjectsScreen(
    uiState: ScheduleUiState,
    onRefresh: () -> Unit
) {
    val session = uiState.currentSession
    val totalEcts = remember(uiState.grades) { uiState.grades.sumOf { it.ects } }
    var isSummaryExpanded by rememberSaveable { mutableStateOf(true) }

    WseiScreenScaffold(
        title = "Przedmioty",
        icon = WseiIcons.School,
        onRefresh = onRefresh,
        isRefreshing = uiState.isRefreshing
    ) {
        item(key = "semester_summary_hero") {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .clickable { isSummaryExpanded = !isSummaryExpanded }
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = M3XSquircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = WseiIcons.School,
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = "Semestr ${session?.semesterNumber ?: 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Przedmioty i Oceny",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        val avgFormatted = String.format(Locale.US, "%.2f", session?.averageGrade ?: 0.0)
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Text(
                                text = "Śr. ${uiState.maskSensitive(avgFormatted)}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    AnimatedVisibility(visible = isSummaryExpanded) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.08f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "${session?.fieldOfStudy ?: "Kierunek"} (${session?.courseOfStudyCode ?: "Tok studiów"})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                val semAvgStr = String.format(Locale.US, "%.2f", session?.averageCurrentSemester ?: 0.0)
                                Text(
                                    text = "Punkty: $totalEcts ECTS • Średnia semestru: ${uiState.maskSensitive(semAvgStr)} • Przedmioty: ${uiState.grades.size}",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }
        }

        items(
            items = uiState.grades,
            key = { it.id },
            contentType = { "grade_item_card" }
        ) { grade ->
            GradeItemRow(
                grade = grade,
                maskSensitive = uiState.isHideSensitiveData
            )
        }
    }
}

@Composable
private fun GradeItemRow(
    grade: GradeItem,
    maskSensitive: Boolean
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = M3XSquircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = WseiIcons.forSubject(grade.subjectName, ClassType.fromRawCode(grade.formLabel)),
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "${grade.moduleName} • ${grade.ects} ECTS",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = grade.subjectName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Text(
                        text = if (maskSensitive) "••" else grade.gradeValue,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "${grade.lecturerName} • ${grade.formLabel} (${grade.hours} godz.)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    val partialInfo = if (grade.partialGrades.isNotEmpty()) {
                        val partialStr = if (maskSensitive) "••••" else grade.partialGrades.joinToString(", ")
                        "${grade.passingForm} • Oceny cząstkowe: $partialStr • ${grade.dateAwarded}"
                    } else {
                        "${grade.passingForm} • Data wpisu: ${grade.dateAwarded}"
                    }
                    Text(
                        text = partialInfo,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// =========================================================================================
// C. Profil Studenta (`StudentProfileParityScreen`) - Skonsolidowany widok w stylu Start
// =========================================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudentProfileParityScreen(
    uiState: ScheduleUiState,
    onChangePassword: (String, String) -> Unit,
    onLogout: () -> Unit,
    onRefresh: () -> Unit
) {
    val session = uiState.currentSession
    var isCourseExpanded by rememberSaveable { mutableStateOf(false) }
    var isGroupsExpanded by rememberSaveable { mutableStateOf(false) }
    var isPlanExpanded by rememberSaveable { mutableStateOf(false) }

    var showPasswordForm by rememberSaveable { mutableStateOf(false) }
    var oldPass by rememberSaveable { mutableStateOf("") }
    var newPass by rememberSaveable { mutableStateOf("") }

    val rawAlbum = session?.albumNumber ?: "—"
    val displayAlbum = uiState.maskSensitive(rawAlbum)

    WseiScreenScaffold(
        title = "Profil",
        icon = WseiIcons.Person,
        onRefresh = onRefresh,
        isRefreshing = uiState.isRefreshing
    ) {
        // 1. Karta tożsamości studenta (identyczna z kafelkiem profilu na Ekranie Start)
        item(key = "student_identity_hero") {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    StudentAvatarPhoto(
                        base64Photo = session?.personPictureBase64,
                        fullName = session?.fullName ?: "Student WSEI",
                        size = 78.dp,
                        shape = RoundedCornerShape(22.dp)
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = WseiIcons.Badge,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Album $displayAlbum",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(
                            text = session?.fullName ?: "Student",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Tok studiów: ${session?.courseOfStudyCode ?: "—"} • Semestr ${session?.semesterNumber ?: 1}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.88f)
                        )
                    }
                }
            }
        }

        // 2. Akordeon: Tok studiów
        item(key = "accordion_course_of_study") {
            val gpaStr = uiState.maskSensitive(String.format(Locale.US, "%.2f", session?.averageGrade ?: 0.0))
            ExpandableAccordionCard(
                title = "Tok studiów i wyniki",
                badgeText = "Śr. $gpaStr",
                icon = WseiIcons.School,
                isExpanded = isCourseExpanded,
                onToggle = { isCourseExpanded = !isCourseExpanded }
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BreakdownMetricRow("Status", session?.statusName ?: "Studiuje")
                        BreakdownMetricRow("Numer albumu", displayAlbum)
                        BreakdownMetricRow("Grupa studencka", uiState.primaryStudentGroupLabel)
                        BreakdownMetricRow("Kierunek", session?.fieldOfStudy ?: "—")
                        BreakdownMetricRow("Specjalność", session?.specialization?.ifBlank { "Ogólna" } ?: "Ogólna")
                        BreakdownMetricRow("Wydział", session?.department ?: "—")
                        BreakdownMetricRow("Tok studiów", session?.courseOfStudyCode ?: "—")
                        BreakdownMetricRow("Tryb studiów", session?.studyMode ?: "—")
                        BreakdownMetricRow("Otrzymywany tytuł", session?.obtainedTitle ?: "inż.")
                        BreakdownMetricRow("Rok / Semestr", "Rok ${session?.academicYear ?: 1} • Semestr ${session?.semesterNumber ?: 1}")
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        BreakdownMetricRow("Średnia za obec. sem.", uiState.maskSensitive(String.format(Locale.US, "%.2f", session?.averageCurrentSemester ?: 0.0)))
                        BreakdownMetricRow("Średnia za poprz. sem.", uiState.maskSensitive(String.format(Locale.US, "%.2f", session?.averagePreviousSemester ?: 0.0)))
                        BreakdownMetricRow("Średnia ze studiów", gpaStr)
                    }
                }
            }
        }

        // 3. Akordeon: Grupy studenckie
        item(key = "accordion_student_groups") {
            val groups = uiState.studentGroups.firstOrNull()?.groups.orEmpty()
            ExpandableAccordionCard(
                title = "Grupy studenckie",
                badgeText = "${groups.size} grup",
                icon = WseiIcons.Groups,
                isExpanded = isGroupsExpanded,
                onToggle = { isGroupsExpanded = !isGroupsExpanded }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    groups.forEach { grp ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = grp.groupCode,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = grp.formName,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                                ) {
                                    Text(
                                        text = "Gr. ${grp.groupNumber}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Akordeon: Plany studiów
        item(key = "accordion_study_plans") {
            val plan = uiState.studyPlans.firstOrNull()
            val planSubjects = plan?.semesters?.flatMap { it.subjects }.orEmpty()
            val totalPlanEcts = planSubjects.sumOf { it.ects.toDouble() }
            ExpandableAccordionCard(
                title = "Plany studiów",
                badgeText = "${totalPlanEcts.toInt()} ECTS",
                icon = WseiIcons.AutoAwesome,
                isExpanded = isPlanExpanded,
                onToggle = { isPlanExpanded = !isPlanExpanded }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    planSubjects.forEach { subj ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = M3XSquircleShape,
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = WseiIcons.forSubject(subj.subjectName, ClassType.fromRawCode(subj.form)),
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${subj.subjectNumber}. ${subj.subjectName}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${subj.form} • ${subj.passingForm} • ${subj.hours} godz.",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                                ) {
                                    Text(
                                        text = "${subj.ects.toInt()} ECTS",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Bezpieczna sekcja zabezpieczeń i jedynego wylogowania w aplikacji (wzorowana na Ekranie Start)
        item(key = "security_and_logout_section") {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = M3XSquircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = WseiIcons.Security,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Konto",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showPasswordForm = !showPasswordForm },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = WseiIcons.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Zmień hasło", style = MaterialTheme.typography.labelLarge)
                        }

                        FilledTonalButton(
                            onClick = onLogout,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = WseiIcons.Logout,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Wyloguj się", style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    AnimatedVisibility(visible = showPasswordForm) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = oldPass,
                                    onValueChange = { oldPass = it },
                                    label = { Text("Obecne hasło") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = newPass,
                                    onValueChange = { newPass = it },
                                    label = { Text("Nowe hasło") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    onClick = {
                                        if (oldPass.isNotBlank() && newPass.isNotBlank()) {
                                            onChangePassword(oldPass, newPass)
                                            showPasswordForm = false
                                            oldPass = ""
                                            newPass = ""
                                        }
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Zapisz nowe hasło w WD")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpandableAccordionCard(
    title: String,
    badgeText: String,
    icon: ImageVector,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = M3XSquircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isExpanded) WseiIcons.ExpandLess else WseiIcons.ExpandMore,
                                contentDescription = if (isExpanded) "Zwiń" else "Rozwiń",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                content()
            }
        }
    }
}

// =========================================================================================
// D. Moduł Finanse i Stypendia (`FinancesScreen`) - Scalone i ujednolicone z Ekranem Start
// =========================================================================================

/**
 * Konwertuje datę w formacie polskim `dd.MM.yyyy` lub ISO `yyyy-MM-dd` na klucz liczbowy `YYYYMMDD`,
 * umożliwiając prawidłową segregację chronologiczną:
 * 1. Najbliższe do zapłaty -> 2. Najpóźniej do zapłaty -> 3. Zapłacone.
 */
private fun parseSortableDueDateKey(rawDate: String): Long {
    val clean = rawDate.trim()
    val dotParts = clean.split(".")
    if (dotParts.size == 3) {
        val day = dotParts[0].toLongOrNull() ?: 1L
        val month = dotParts[1].toLongOrNull() ?: 1L
        val year = dotParts[2].toLongOrNull() ?: 2099L
        return year * 10_000L + month * 100L + day
    }
    val dashParts = clean.split("-")
    if (dashParts.size == 3) {
        val year = dashParts[0].toLongOrNull() ?: 2099L
        val month = dashParts[1].toLongOrNull() ?: 1L
        val day = dashParts[2].take(2).toLongOrNull() ?: 1L
        return year * 10_000L + month * 100L + day
    }
    return 9999_12_31L
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FinancesScreen(
    uiState: ScheduleUiState,
    onInitiateOnlinePayment: (FinancialObligation, (String) -> Unit) -> Unit,
    onRefresh: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var copiedAccountNotice by rememberSaveable { mutableStateOf(false) }
    var showScholarshipHistorySheet by rememberSaveable { mutableStateOf(false) }

    val unpaidSum = remember(uiState.financials) {
        uiState.financials.filter { !it.isPaid }.sumOf { kotlin.math.abs(it.currentStatePln) }
    }
    val rawAccountNumber = uiState.financials.firstOrNull()?.bankAccountNumber
        ?.ifBlank { "86 1020 3147 2006 0000 0609 3118" }
        ?: "86 1020 3147 2006 0000 0609 3118"
    val accountNumber = remember(rawAccountNumber) { formatPolishBankAccount(rawAccountNumber) }

    // Segregacja: 1. Najbliższe do zapłaty -> 2. Najpóźniej do zapłaty -> 3. Zapłacone
    val sortedObligations = remember(uiState.financials) {
        val unpaid = uiState.financials
            .filter { !it.isPaid }
            .sortedBy { parseSortableDueDateKey(it.dueDateIso) }
        val paid = uiState.financials
            .filter { it.isPaid }
            .sortedByDescending { parseSortableDueDateKey(it.dueDateIso) }
        unpaid + paid
    }
    val activeScholarship = uiState.scholarships.firstOrNull()

    WseiScreenScaffold(
        title = "Finanse",
        icon = WseiIcons.AccountBalanceWallet,
        onRefresh = onRefresh,
        isRefreshing = uiState.isRefreshing
    ) {
        // Podsumowanie finansowe + Indywidualny rachunek bankowy (styl karty Start)
        item(key = "finance_summary_hero") {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = M3XSquircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = WseiIcons.AccountBalanceWallet,
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Column {
                                val balanceText = if (unpaidSum == 0.0) {
                                    "0.00 PLN (Uregulowano)"
                                } else {
                                    "Do zapłaty: ${String.format(Locale.US, "%.2f", unpaidSum)} PLN"
                                }
                                Text(
                                    text = uiState.maskSensitive(balanceText),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.08f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Numer konta do wpłat:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = uiState.maskSensitive(accountNumber),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.1.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    FilledTonalButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(accountNumber))
                            copiedAccountNotice = true
                        },
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = WseiIcons.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (copiedAccountNotice) "Skopiowano numer konta" else "Kopiuj numer konta",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        // Kafelki płatności (najbliższe do zapłaty > najpóźniej do zapłaty > zapłacone)
        items(
            items = sortedObligations,
            key = { it.id },
            contentType = { "payment_obligation_card" }
        ) { item ->
            FinancialInstallmentExpressiveRow(
                item = item,
                maskSensitive = uiState.isHideSensitiveData,
                onPayOnline = {
                    onInitiateOnlinePayment(item) { paymentUrl ->
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(paymentUrl))
                            )
                        }
                    }
                }
            )
        }

        // Karta Stypendium (wzorowana na Ekranie Start)
        if (activeScholarship != null) {
            item(key = "active_scholarship_card") {
                ActiveScholarshipCard(
                    scholarship = activeScholarship,
                    maskSensitive = uiState.isHideSensitiveData,
                    hasArchive = uiState.scholarships.size > 1,
                    onOpenHistory = { showScholarshipHistorySheet = true }
                )
            }
        } else {
            item(key = "empty_scholarships") {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Brak wniosków stypendialnych w tym semestrze.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(22.dp)
                    )
                }
            }
        }
    }

    // BottomSheet z historią stypendiów
    if (showScholarshipHistorySheet) {
        ModalBottomSheet(
            onDismissRequest = { showScholarshipHistorySheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Historia decyzji stypendialnych",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                uiState.scholarships.forEach { s ->
                    ScholarshipApplicationCard(
                        scholarship = s,
                        maskSensitive = uiState.isHideSensitiveData
                    )
                }
            }
        }
    }
}

/**
 * Kafelek opłaty w stylu Ekranu Start:
 * - Domyślnie ZWINIĘTY (`expanded = false`): wyświetla tytuł/termin, kwotę oraz przycisk do zapłaty.
 * - Kliknięcie w kafelek rozwija szczegółowe informacje rozliczeniowe.
 */
@Composable
private fun FinancialInstallmentExpressiveRow(
    item: FinancialObligation,
    maskSensitive: Boolean,
    onPayOnline: () -> Unit
) {
    // Domyślnie zwinięty; kliknięcie w kafelek rozwija szczegóły
    var expanded by rememberSaveable { mutableStateOf(false) }
    val today = remember { java.time.LocalDate.now() }
    val currentMonthSuffix = remember(today) {
        String.format(Locale.US, ".%02d.%04d", today.monthValue, today.year)
    }
    val isDueThisMonth = !item.isPaid && item.dueDateIso.trim().endsWith(currentMonthSuffix)

    val containerColor = when {
        isDueThisMonth -> MaterialTheme.colorScheme.errorContainer
        item.isPaid -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f)
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val contentColor = when {
        isDueThisMonth -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    val rawAmount = if (item.isPaid) {
        item.amountFormatted
    } else {
        String.format(Locale.US, "%.2f PLN", kotlin.math.abs(item.currentStatePln).takeIf { it > 0.0 } ?: item.amountPln)
    }
    val displayAmount = if (maskSensitive) "••••••" else rawAmount

    Surface(
        onClick = { expanded = !expanded },
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        contentColor = contentColor,
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Nagłówek w stylu Ekranu Start: 42.dp Squircle + Overline + Tytuł + Kwota i strzałka rozwinięcia
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 10.dp)
                ) {
                    Surface(
                        shape = M3XSquircleShape,
                        color = if (isDueThisMonth) {
                            contentColor.copy(alpha = 0.14f)
                        } else if (item.isPaid) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                        contentColor = if (isDueThisMonth) {
                            contentColor
                        } else if (item.isPaid) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = WseiIcons.AccountBalanceWallet,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = when {
                                isDueThisMonth -> "Najbliższa do zapłaty • termin ${item.dueDateIso}"
                                !item.isPaid -> "Do zapłaty • termin ${item.dueDateIso}"
                                else -> "Zapłacone • termin ${item.dueDateIso}"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = when {
                                isDueThisMonth -> contentColor
                                !item.isPaid -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isDueThisMonth) {
                            contentColor.copy(alpha = 0.14f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        }
                    ) {
                        Text(
                            text = displayAmount,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    Icon(
                        imageVector = if (expanded) WseiIcons.ExpandLess else WseiIcons.ExpandMore,
                        contentDescription = if (expanded) "Zwiń" else "Rozwiń",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Przycisk do zapłaty widoczny od razu na kafelku (dla nieopłaconych zobowiązań)
            if (item.payOnlineAvailable && !item.isPaid) {
                Button(
                    onClick = onPayOnline,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = WseiIcons.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Zapłać online ($displayAmount)",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            // Mniej istotne informacje rozwijane po kliknięciu w kafelek
            AnimatedVisibility(visible = expanded) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDueThisMonth) {
                        contentColor.copy(alpha = 0.1f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        BreakdownMetricRow("Status płatności", item.statusLabel)
                        BreakdownMetricRow("Termin płatności", item.dueDateIso)
                        BreakdownMetricRow("Kwota naliczona", if (maskSensitive) "••••••" else item.amountFormatted)
                        BreakdownMetricRow("Odsetki naliczone", if (maskSensitive) "••••••" else item.interestFormatted)
                        BreakdownMetricRow("Odsetki zapłacone", if (maskSensitive) "••••••" else item.accumulatedInterestFormatted)
                        BreakdownMetricRow("Umorzenie", item.amortizationFormatted)
                        BreakdownMetricRow("Bonifikata", item.discountFormatted)
                        BreakdownMetricRow("Przedawnienie", item.expiredFormatted)
                        if (item.paymentDateIso != null) {
                            BreakdownMetricRow("Data wpłaty", item.paymentDateIso)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveScholarshipCard(
    scholarship: ScholarshipApplication,
    maskSensitive: Boolean,
    hasArchive: Boolean,
    onOpenHistory: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = M3XSquircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = WseiIcons.WorkspacePremium,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "${scholarship.semesterLabel} • ${scholarship.statusString}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = scholarship.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (hasArchive) {
                    OutlinedButton(
                        onClick = onOpenHistory,
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("Historia", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (scholarship.typeVariantName.isNotBlank()) {
                        Text(
                            text = scholarship.typeVariantName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = "Data wniosku: ${scholarship.submissionDateIso} • Decyzja: ${scholarship.decisionNumber ?: "W toku"} (${scholarship.decisionDateIso ?: "—"})",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (scholarship.payouts.isNotEmpty()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        scholarship.payouts.take(3).forEach { payout ->
                            BreakdownMetricRow(
                                label = "${payout.dateIso} • ${payout.status}",
                                value = if (maskSensitive) "••••••" else payout.amountFormatted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScholarshipApplicationCard(
    scholarship: ScholarshipApplication,
    maskSensitive: Boolean
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "${scholarship.semesterLabel} • ${scholarship.statusString}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = scholarship.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Decyzja: ${scholarship.decisionNumber ?: "W toku"} (${scholarship.decisionDateIso ?: "—"})",
                style = MaterialTheme.typography.bodyMedium
            )
            scholarship.payouts.forEach { payout ->
                BreakdownMetricRow(
                    label = "${payout.dateIso} • ${payout.status}",
                    value = if (maskSensitive) "••••••" else payout.amountFormatted
                )
            }
        }
    }
}

// =========================================================================================
// E. Hub Funkcjonalny „Więcej” (`MoreHubScreen`) oraz Ekrany: Ustawienia, Ogłoszenia, O aplikacji
// =========================================================================================

@Composable
private fun MoreHubScreen(
    uiState: ScheduleUiState,
    currentSubView: MoreSubView,
    onNavigateSubView: (MoreSubView) -> Unit,
    onMarkAnnouncementRead: (DeanAnnouncement) -> Unit,
    onDownloadAttachment: (AnnouncementAttachment) -> Unit,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    onToggleLiveNotification: (Boolean) -> Unit,
    onToggleHideSensitiveData: (Boolean) -> Unit,
    onRefresh: () -> Unit
) {
    AnimatedContent(
        targetState = currentSubView,
        transitionSpec = {
            (fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
                scaleIn(initialScale = 0.96f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))) togetherWith
                fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium))
        },
        modifier = Modifier.fillMaxSize(),
        label = "moreSubViewTransition"
    ) { subView ->
        when (subView) {
            MoreSubView.HUB -> {
                MoreBentoHubView(
                    uiState = uiState,
                    onOpenAnnouncements = { onNavigateSubView(MoreSubView.ANNOUNCEMENTS) },
                    onOpenSettings = { onNavigateSubView(MoreSubView.SETTINGS) },
                    onOpenAbout = { onNavigateSubView(MoreSubView.ABOUT) }
                )
            }
            MoreSubView.ANNOUNCEMENTS -> {
                AnnouncementsSubScreen(
                    uiState = uiState,
                    onBack = { onNavigateSubView(MoreSubView.HUB) },
                    onMarkAnnouncementRead = onMarkAnnouncementRead,
                    onDownloadAttachment = onDownloadAttachment,
                    onRefresh = onRefresh
                )
            }
            MoreSubView.SETTINGS -> {
                SettingsSubScreen(
                    uiState = uiState,
                    onBack = { onNavigateSubView(MoreSubView.HUB) },
                    onSelectThemeMode = onSelectThemeMode,
                    onToggleLiveNotification = onToggleLiveNotification,
                    onToggleHideSensitiveData = onToggleHideSensitiveData
                )
            }
            MoreSubView.ABOUT -> {
                AboutSubScreen(
                    onBack = { onNavigateSubView(MoreSubView.HUB) }
                )
            }
        }
    }
}

@Composable
private fun MoreBentoHubView(
    uiState: ScheduleUiState,
    onOpenAnnouncements: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit
) {
    WseiScreenScaffold(
        title = "Więcej",
        icon = WseiIcons.Apps
    ) {
        // Karta 1: Ogłoszenia uczelniane
        item(key = "hub_card_announcements") {
            val unread = uiState.announcements.count { !it.isRead }
            BentoHubCard(
                title = "Ogłoszenia uczelniane",
                subtitle = "Wiadomości z dziekanatu i załączniki",
                icon = WseiIcons.Campaign,
                badgeText = if (unread > 0) "$unread nowe" else "${uiState.announcements.size} wiad.",
                badgeIsUrgent = unread > 0,
                onClick = onOpenAnnouncements
            )
        }

        // Karta 2: Ustawienia aplikacji
        item(key = "hub_card_settings") {
            BentoHubCard(
                title = "Ustawienia aplikacji",
                subtitle = "Motyw, powiadomienia i prywatność",
                icon = WseiIcons.Settings,
                badgeText = uiState.themeMode.polishLabel,
                badgeIsUrgent = false,
                onClick = onOpenSettings
            )
        }

        // Karta 3: O aplikacji
        item(key = "hub_card_about") {
            BentoHubCard(
                title = "O aplikacji Taal",
                subtitle = "Wersja 0.2",
                icon = WseiIcons.Info,
                badgeText = "v0.2",
                badgeIsUrgent = false,
                onClick = onOpenAbout
            )
        }
    }
}

@Composable
private fun BentoHubCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    badgeText: String,
    badgeIsUrgent: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = M3XSquircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = if (badgeIsUrgent) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = if (badgeIsUrgent) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = WseiIcons.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// E1. Podstrona: Ogłoszenia uczelniane (wzorowana na kartach Ekranu Start)
// -----------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnnouncementsSubScreen(
    uiState: ScheduleUiState,
    onBack: () -> Unit,
    onMarkAnnouncementRead: (DeanAnnouncement) -> Unit,
    onDownloadAttachment: (AnnouncementAttachment) -> Unit,
    onRefresh: () -> Unit
) {
    WseiScreenScaffold(
        title = "Ogłoszenia",
        icon = WseiIcons.Campaign,
        onBack = onBack,
        onRefresh = onRefresh,
        isRefreshing = uiState.isRefreshing
    ) {
        items(
            items = uiState.announcements,
            key = { it.id },
            contentType = { "announcement_card" }
        ) { announcement ->
            AnnouncementRow(
                announcement = announcement,
                onMarkRead = { onMarkAnnouncementRead(announcement) },
                onDownloadAttachment = onDownloadAttachment
            )
        }
    }
}

@Composable
private fun AnnouncementRow(
    announcement: DeanAnnouncement,
    onMarkRead: () -> Unit,
    onDownloadAttachment: (AnnouncementAttachment) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Surface(
        onClick = {
            expanded = !expanded
            if (!announcement.isRead) onMarkRead()
        },
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = M3XSquircleShape,
                        color = if (!announcement.isRead) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = if (!announcement.isRead) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = WseiIcons.Campaign,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "${announcement.authorDepartment} • ${announcement.publishedAtIso}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = announcement.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = if (!announcement.isRead) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest
                ) {
                    Text(
                        text = if (!announcement.isRead) "Nowe" else "Przeczytano",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (expanded) announcement.fullText else announcement.bodySnippet,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (expanded) Int.MAX_VALUE else 3,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (expanded && announcement.attachments.isNotEmpty()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        announcement.attachments.forEach { att ->
                            FilledTonalButton(
                                onClick = { onDownloadAttachment(att) },
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = WseiIcons.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Pobierz: ${att.fileName}",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// E2. Podstrona: Ustawienia Aplikacji (`SettingsSubScreen`) — karty w stylu Ekranu Start
// -----------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSubScreen(
    uiState: ScheduleUiState,
    onBack: () -> Unit,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    onToggleLiveNotification: (Boolean) -> Unit,
    onToggleHideSensitiveData: (Boolean) -> Unit
) {
    WseiScreenScaffold(
        title = "Ustawienia",
        icon = WseiIcons.Settings,
        onBack = onBack
    ) {
        // 1. Motyw aplikacji
        item(key = "settings_theme_section") {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = M3XSquircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = WseiIcons.Palette,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Motyw aplikacji",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppThemeMode.entries.forEach { mode ->
                            val isSelected = uiState.themeMode == mode
                            Surface(
                                onClick = { onSelectThemeMode(mode) },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerLow
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = mode.polishLabel,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                    )
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { onSelectThemeMode(mode) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Powiadomienia Live (Android 16 ProgressStyle)
        item(key = "settings_live_notifications_section") {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = M3XSquircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = WseiIcons.NotificationsActive,
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = "Zajęcia na ekranie blokady",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Switch(
                            checked = uiState.isLiveNotificationEnabled,
                            onCheckedChange = onToggleLiveNotification
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Pokazuje postęp trwających zajęć na ekranie blokady i w pasku stanu.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                        )
                    }
                }
            }
        }

        // 3. Prywatność i ochrona danych (Ukryj dane wrażliwe)
        item(key = "settings_privacy_section") {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = M3XSquircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (uiState.isHideSensitiveData) WseiIcons.VisibilityOff else WseiIcons.Visibility,
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = "Ukryj dane",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Switch(
                            checked = uiState.isHideSensitiveData,
                            onCheckedChange = onToggleHideSensitiveData
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Ukrywa numer albumu, kwoty i oceny, gdy korzystasz z aplikacji wśród ludzi.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// E3. Podstrona: O aplikacji (`AboutSubScreen`) — karty w stylu Ekranu Start
// -----------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AboutSubScreen(
    onBack: () -> Unit
) {
    WseiScreenScaffold(
        title = "O aplikacji",
        icon = WseiIcons.Info,
        onBack = onBack
    ) {
        item(key = "about_hero") {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = M3XSquircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = WseiIcons.TaalVolcano,
                                    contentDescription = null,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Taal",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "Wersja 0.2",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                    Text(
                        text = "Aplikacja dla studentów Uniwersytetu WSEI w Lublinie. Pokazuje plan zajęć, oceny i opłaty z Wirtualnego Dziekanatu.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        item(key = "about_credits") {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BreakdownMetricRow("Dane", "Wirtualny Dziekanat (APR System)")
                    BreakdownMetricRow("Czcionka", "Google Sans Flex (OFL)")
                    BreakdownMetricRow("Licencja", "MIT")
                }
            }
        }
    }
}

@Composable
private fun BreakdownMetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
