package pl.lublin.wsei.plan.ui.navigation

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import pl.lublin.wsei.plan.domain.model.AnnouncementAttachment
import pl.lublin.wsei.plan.domain.model.ClassEvent
import pl.lublin.wsei.plan.domain.model.DeanAnnouncement
import pl.lublin.wsei.plan.domain.model.FinancialObligation
import pl.lublin.wsei.plan.domain.model.ScheduleFilterType
import pl.lublin.wsei.plan.ui.components.LocalWseiProfileShortcut
import pl.lublin.wsei.plan.ui.components.WseiProfileShortcut
import pl.lublin.wsei.plan.ui.components.WseiSplashScreen
import pl.lublin.wsei.plan.ui.components.WseiWelcomeScreen
import pl.lublin.wsei.plan.ui.finances.FinancesScreen
import pl.lublin.wsei.plan.ui.home.HomeScreen
import pl.lublin.wsei.plan.ui.icons.WseiIcons
import pl.lublin.wsei.plan.ui.login.LoginScreen
import pl.lublin.wsei.plan.ui.more.MoreScreen
import pl.lublin.wsei.plan.ui.profile.ProfileScreen
import pl.lublin.wsei.plan.ui.schedule.AppThemeMode
import pl.lublin.wsei.plan.ui.schedule.ScheduleScreen
import pl.lublin.wsei.plan.ui.schedule.ScheduleUiState
import pl.lublin.wsei.plan.ui.subjects.SubjectsScreen
import pl.lublin.wsei.plan.ui.theme.WseiMotion

/**
 * Główne miejsca w aplikacji. Pierwszych pięć leży na pływającym pasku nawigacji,
 * Profil otwiera się z awatara w prawym górnym rogu.
 */
enum class WseiDestination(
    val label: String,
    val shortLabel: String,
    val contentDescription: String
) {
    START("Start", "Start", "Ekran startowy"),
    SCHEDULE("Plan zajęć", "Plan", "Plan zajęć i kalendarz"),
    SUBJECTS("Przedmioty", "Oceny", "Przedmioty, punkty ECTS i oceny"),
    FINANCES("Finanse", "Finanse", "Opłaty i stypendia"),
    MORE("Więcej", "Więcej", "Ogłoszenia, ustawienia i informacje o aplikacji"),
    PROFILE("Profil", "Profil", "Dane studenta, tok studiów i grupy");

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

enum class MoreSubView { HUB, ANNOUNCEMENTS, SETTINGS, ABOUT }

private enum class AuthPhase { SPLASH, WELCOME, LOGIN, APP }

private const val PREFS_NAME = "taal_prefs"
private const val PREF_WELCOME_SEEN = "welcome_seen"

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
    var profileReturnDestination by rememberSaveable { mutableStateOf(WseiDestination.START) }
    var moreSubView by rememberSaveable { mutableStateOf(MoreSubView.HUB) }
    val isLoggedIn = scheduleUiState.currentSession != null

    fun openProfile() {
        if (currentDestination != WseiDestination.PROFILE) {
            profileReturnDestination = currentDestination
            currentDestination = WseiDestination.PROFILE
        }
    }

    fun openMore(subView: MoreSubView) {
        moreSubView = subView
        currentDestination = WseiDestination.MORE
    }

    // Wstecz: podekran „Więcej” → hub, Profil → poprzednia zakładka, każda inna zakładka → Start.
    BackHandler(
        enabled = isLoggedIn && (
            currentDestination != WseiDestination.START || moreSubView != MoreSubView.HUB
            )
    ) {
        when {
            currentDestination == WseiDestination.MORE && moreSubView != MoreSubView.HUB ->
                moreSubView = MoreSubView.HUB
            currentDestination == WseiDestination.PROFILE ->
                currentDestination = profileReturnDestination
            else -> {
                moreSubView = MoreSubView.HUB
                currentDestination = WseiDestination.START
            }
        }
    }

    // Ekran powitalny pokazujemy raz, przed pierwszym logowaniem.
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    var welcomeSeen by rememberSaveable { mutableStateOf(prefs.getBoolean(PREF_WELCOME_SEEN, false)) }

    // Po wylogowaniu kolejne logowanie zaczyna się od zakładki Start.
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
            (fadeIn(WseiMotion.effects()) + scaleIn(WseiMotion.spatial(), initialScale = 0.96f)) togetherWith
                fadeOut(WseiMotion.effects())
        },
        modifier = modifier.fillMaxSize(),
        label = "authPhaseTransition"
    ) { currentPhase ->
        when (currentPhase) {
            AuthPhase.SPLASH -> WseiSplashScreen()
            AuthPhase.WELCOME -> WseiWelcomeScreen(
                onContinue = {
                    prefs.edit().putBoolean(PREF_WELCOME_SEEN, true).apply()
                    welcomeSeen = true
                }
            )
            AuthPhase.LOGIN -> LoginScreen(
                isAuthenticating = scheduleUiState.isAuthenticating,
                errorMessage = scheduleUiState.authError,
                savedLoginHint = scheduleUiState.savedEncryptedLoginHint,
                onLoginSubmit = onLoginSubmit,
                onQuickDebugLogin = onQuickDebugLogin
            )
            AuthPhase.APP -> {
                val session = scheduleUiState.currentSession
                val profileShortcut = remember(session?.personPictureBase64, session?.fullName) {
                    WseiProfileShortcut(
                        photoBase64 = session?.personPictureBase64,
                        fullName = session?.fullName ?: "Student",
                        onOpen = { openProfile() }
                    )
                }
                CompositionLocalProvider(LocalWseiProfileShortcut provides profileShortcut) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AnimatedContent(
                            targetState = currentDestination,
                            transitionSpec = {
                                (fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                                    slideInVertically(WseiMotion.spatial()) { it / 24 }) togetherWith
                                    fadeOut(spring(stiffness = Spring.StiffnessHigh))
                            },
                            modifier = Modifier.fillMaxSize(),
                            label = "destinationTransition"
                        ) { destination ->
                            DestinationContent(
                                destination = destination,
                                uiState = scheduleUiState,
                                moreSubView = moreSubView,
                                onNavigate = { currentDestination = it },
                                onOpenMore = { openMore(it) },
                                onNavigateMoreSubView = { moreSubView = it },
                                onCloseProfile = { currentDestination = profileReturnDestination },
                                onLogout = onLogout,
                                onChangePassword = onChangePassword,
                                onSelectDay = onSelectDay,
                                onSelectDate = onSelectDate,
                                onToggleCalendarExpanded = onToggleCalendarExpanded,
                                onPreviousCalendarPeriod = onPreviousCalendarPeriod,
                                onNextCalendarPeriod = onNextCalendarPeriod,
                                onSelectScheduleFilter = onSelectScheduleFilter,
                                onToggleHeroExpanded = onToggleHeroExpanded,
                                onSyncLiveNotification = onSyncLiveNotification,
                                onRefresh = onRefreshSchedule,
                                onInitiateOnlinePayment = onInitiateOnlinePayment,
                                onMarkAnnouncementRead = onMarkAnnouncementRead,
                                onDownloadAttachment = onDownloadAttachment,
                                onSelectThemeMode = onSelectThemeMode,
                                onToggleLiveNotification = onToggleLiveNotification,
                                onToggleHideSensitiveData = onToggleHideSensitiveData
                            )
                        }

                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            StatusMessage(
                                message = scheduleUiState.statusBannerMessage,
                                onDismiss = onDismissStatusBanner
                            )
                            AnimatedVisibility(
                                visible = currentDestination != WseiDestination.PROFILE,
                                enter = slideInVertically(WseiMotion.spatial()) { it * 2 } + fadeIn(),
                                exit = slideOutVertically(WseiMotion.size()) { it * 2 } + fadeOut()
                            ) {
                                WseiFloatingNavigationBar(
                                    currentDestination = currentDestination,
                                    unreadAnnouncementsCount = scheduleUiState.unreadAnnouncementsCount,
                                    onSelectDestination = { destination ->
                                        if (destination == currentDestination && destination == WseiDestination.MORE) {
                                            moreSubView = MoreSubView.HUB
                                        }
                                        currentDestination = destination
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DestinationContent(
    destination: WseiDestination,
    uiState: ScheduleUiState,
    moreSubView: MoreSubView,
    onNavigate: (WseiDestination) -> Unit,
    onOpenMore: (MoreSubView) -> Unit,
    onNavigateMoreSubView: (MoreSubView) -> Unit,
    onCloseProfile: () -> Unit,
    onLogout: () -> Unit,
    onChangePassword: (String, String) -> Unit,
    onSelectDay: (Int) -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onToggleCalendarExpanded: () -> Unit,
    onPreviousCalendarPeriod: () -> Unit,
    onNextCalendarPeriod: () -> Unit,
    onSelectScheduleFilter: (ScheduleFilterType) -> Unit,
    onToggleHeroExpanded: () -> Unit,
    onSyncLiveNotification: (ClassEvent) -> Unit,
    onRefresh: () -> Unit,
    onInitiateOnlinePayment: (FinancialObligation, (String) -> Unit) -> Unit,
    onMarkAnnouncementRead: (DeanAnnouncement) -> Unit,
    onDownloadAttachment: (AnnouncementAttachment) -> Unit,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    onToggleLiveNotification: (Boolean) -> Unit,
    onToggleHideSensitiveData: (Boolean) -> Unit
) {
    when (destination) {
        WseiDestination.START -> HomeScreen(
            uiState = uiState,
            onOpenSchedule = { onNavigate(WseiDestination.SCHEDULE) },
            onOpenSubjects = { onNavigate(WseiDestination.SUBJECTS) },
            onOpenFinances = { onNavigate(WseiDestination.FINANCES) },
            onOpenAnnouncements = { onOpenMore(MoreSubView.ANNOUNCEMENTS) },
            onRefresh = onRefresh
        )
        WseiDestination.SCHEDULE -> ScheduleScreen(
            uiState = uiState,
            onSelectDay = onSelectDay,
            onSelectDate = onSelectDate,
            onToggleCalendarExpanded = onToggleCalendarExpanded,
            onPreviousCalendarPeriod = onPreviousCalendarPeriod,
            onNextCalendarPeriod = onNextCalendarPeriod,
            onSelectScheduleFilter = onSelectScheduleFilter,
            onToggleHeroExpanded = onToggleHeroExpanded,
            onSyncLiveNotification = onSyncLiveNotification,
            onRefreshSchedule = onRefresh
        )
        WseiDestination.SUBJECTS -> SubjectsScreen(
            uiState = uiState,
            onRefresh = onRefresh
        )
        WseiDestination.FINANCES -> FinancesScreen(
            uiState = uiState,
            onInitiateOnlinePayment = onInitiateOnlinePayment,
            onRefresh = onRefresh
        )
        WseiDestination.MORE -> MoreScreen(
            uiState = uiState,
            currentSubView = moreSubView,
            onNavigateSubView = onNavigateMoreSubView,
            onMarkAnnouncementRead = onMarkAnnouncementRead,
            onDownloadAttachment = onDownloadAttachment,
            onSelectThemeMode = onSelectThemeMode,
            onToggleLiveNotification = onToggleLiveNotification,
            onToggleHideSensitiveData = onToggleHideSensitiveData,
            onRefresh = onRefresh
        )
        WseiDestination.PROFILE -> ProfileScreen(
            uiState = uiState,
            onBack = onCloseProfile,
            onChangePassword = onChangePassword,
            onLogout = onLogout,
            onRefresh = onRefresh
        )
    }
}

/**
 * Komunikat stanu (np. „Zsynchronizowano”) jako pływający snackbar nad paskiem nawigacji,
 * zamiast paska przesuwającego całą treść ekranu.
 */
@Composable
private fun StatusMessage(message: String?, onDismiss: () -> Unit) {
    // Ostatni komunikat zostaje w pamięci na czas animacji znikania (zwykły uchwyt, nie stan —
    // zapis w trakcie kompozycji nie wywołuje dodatkowej rekompozycji).
    val lastMessageHolder = remember { arrayOf("") }
    if (message != null) lastMessageHolder[0] = message
    val lastMessage = lastMessageHolder[0]

    AnimatedVisibility(
        visible = message != null,
        enter = slideInVertically(WseiMotion.spatial()) { it } + fadeIn(),
        exit = slideOutVertically(WseiMotion.size()) { it } + fadeOut()
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.inverseSurface,
            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            shadowElevation = 6.dp,
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier.padding(start = 20.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = lastMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onDismiss) {
                    Text(
                        text = "OK",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.inversePrimary
                    )
                }
            }
        }
    }
}
