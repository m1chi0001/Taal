package pl.lublin.wsei.plan.ui.navigation

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.window.core.layout.WindowSizeClass
import kotlinx.datetime.LocalDate
import pl.lublin.wsei.plan.R
import pl.lublin.wsei.plan.data.update.UpdateViewModel
import pl.lublin.wsei.plan.domain.model.AnnouncementAttachment
import pl.lublin.wsei.plan.domain.model.ClassEvent
import pl.lublin.wsei.plan.domain.model.DeanAnnouncement
import pl.lublin.wsei.plan.domain.model.FinancialObligation
import pl.lublin.wsei.plan.domain.model.ScheduleFilterType
import pl.lublin.wsei.plan.domain.model.AppColorPalette
import pl.lublin.wsei.plan.ui.adaptive.LocalWseiWindowLayout
import pl.lublin.wsei.plan.ui.adaptive.WseiWidthClass
import pl.lublin.wsei.plan.ui.adaptive.rememberWseiWindowLayout
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
import pl.lublin.wsei.plan.ui.update.UpdateChangelogSheet
import pl.lublin.wsei.plan.ui.update.UpdatePromptHost
import pl.lublin.wsei.plan.ui.theme.WseiSpacing

/**
 * Główne miejsca w aplikacji. Pierwszych pięć leży na pływającym pasku nawigacji (telefon)
 * lub na bocznej szynie (tablet, składany, poziom). Profil otwiera się z awatara —
 * w prawym górnym rogu ekranu albo w nagłówku szyny.
 */
enum class WseiDestination(
    @StringRes val labelRes: Int,
    @StringRes val shortLabelRes: Int,
    @StringRes val contentDescriptionRes: Int
) {
    START(R.string.nav_start, R.string.nav_start_short, R.string.nav_start_cd),
    SCHEDULE(R.string.nav_schedule, R.string.nav_schedule_short, R.string.nav_schedule_cd),
    SUBJECTS(R.string.nav_subjects, R.string.nav_subjects_short, R.string.nav_subjects_cd),
    FINANCES(R.string.nav_finances, R.string.nav_finances_short, R.string.nav_finances_cd),
    MORE(R.string.nav_more, R.string.nav_more_short, R.string.nav_more_cd),
    PROFILE(R.string.nav_profile, R.string.nav_profile_short, R.string.nav_profile_cd);

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
    onSelectColorPalette: (AppColorPalette) -> Unit,
    onLoadDeviceCalendars: () -> Unit,
    onSyncToCalendar: (Long) -> Unit,
    onDismissCalendarSyncStatus: () -> Unit,
    onToggleLiveNotification: (Boolean) -> Unit,
    onToggleHideSensitiveData: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentDestination by rememberSaveable { mutableStateOf(WseiDestination.START) }
    var profileReturnDestination by rememberSaveable { mutableStateOf(WseiDestination.START) }
    var moreSubView by rememberSaveable { mutableStateOf(MoreSubView.HUB) }
    var showChangelog by rememberSaveable { mutableStateOf(false) }
    val isLoggedIn = scheduleUiState.currentSession != null

    // Activity-scoped: the prompt, "O aplikacji" and the changelog share one update state.
    val updateViewModel: UpdateViewModel = hiltViewModel()
    LifecycleResumeEffect(updateViewModel) {
        // Checks on cold start and when returning to the app (debounced/throttled with ETag).
        updateViewModel.autoCheckOnForeground()
        onPauseOrDispose { }
    }

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

    fun selectTab(destination: WseiDestination) {
        // Ponowne dotknięcie „Więcej” wraca do huba z podekranu.
        if (destination == currentDestination && destination == WseiDestination.MORE) {
            moreSubView = MoreSubView.HUB
        }
        currentDestination = destination
    }

    // Wstecz z podglądem (Predictive Back): podekran „Więcej” → hub (w MoreScreen),
    // Profil → poprzednia zakładka, każda inna zakładka → Start; na Starcie animacja systemowa.
    val shellBackState = rememberWseiPredictiveBackState()

    // Ekran powitalny pokazujemy raz, przed pierwszym logowaniem.
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    var welcomeSeen by rememberSaveable { mutableStateOf(prefs.getBoolean(PREF_WELCOME_SEEN, false)) }

    // Po wylogowaniu kolejne logowanie zaczyna się od zakładki Start.
    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn) {
            currentDestination = WseiDestination.START
            profileReturnDestination = WseiDestination.START
            moreSubView = MoreSubView.HUB
        }
    }

    val phase = when {
        isLoggedIn -> AuthPhase.APP
        scheduleUiState.isSessionRestoring -> AuthPhase.SPLASH
        !welcomeSeen -> AuthPhase.WELCOME
        else -> AuthPhase.LOGIN
    }

    // Klasa okna liczona raz dla całej aplikacji (także logowania i powitania) — ekrany czytają
    // ją z LocalWseiWindowLayout i aktualizują się po obrocie lub rozłożeniu urządzenia.
    val windowLayout = rememberWseiWindowLayout()
    // Na niskich oknach (telefon w poziomie) pięć zakładek i awatar nie mieszczą się razem
    // na szynie — awatar zostaje wtedy w górnym pasku ekranu.
    val isTallWindow = currentWindowAdaptiveInfo().windowSizeClass
        .isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND)

    CompositionLocalProvider(LocalWseiWindowLayout provides windowLayout) {
        AnimatedContent(
            targetState = phase,
            transitionSpec = {
                // Fade + scale only (both drawn in a graphics layer); no size animation.
                (fadeIn(WseiMotion.effects()) + scaleIn(WseiMotion.spatial(), initialScale = 0.96f)) togetherWith
                    fadeOut(WseiMotion.fastEffects()) using null
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
                    val useRail = windowLayout.useNavigationRail
                    val railShowsProfile = useRail && isTallWindow
                    val session = scheduleUiState.currentSession
                    val studentFallbackName = stringResource(R.string.common_student)
                    val profileShortcut = remember(
                        session?.personPictureBase64,
                        session?.fullName,
                        railShowsProfile,
                        studentFallbackName
                    ) {
                        WseiProfileShortcut(
                            photoBase64 = session?.personPictureBase64,
                            fullName = session?.fullName ?: studentFallbackName,
                            onOpen = { openProfile() },
                            showInTopBar = !railShowsProfile
                        )
                    }
                    // Każda zakładka zachowuje własny stan (przewinięcie listy, zwinięty pasek
                    // tytułu, rozwinięte karty) przy przełączaniu i po zmianie konfiguracji.
                    val tabStateHolder = rememberSaveableStateHolder()

                    CompositionLocalProvider(LocalWseiProfileShortcut provides profileShortcut) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            if (useRail) {
                                WseiNavigationRail(
                                    currentDestination = currentDestination,
                                    unreadAnnouncementsCount = scheduleUiState.unreadAnnouncementsCount,
                                    expanded = windowLayout.widthClass == WseiWidthClass.EXPANDED,
                                    profileShortcut = if (railShowsProfile) profileShortcut else null,
                                    onSelectDestination = { selectTab(it) }
                                )
                            }
                            val shellBackEnabled = currentDestination != WseiDestination.START &&
                                !(currentDestination == WseiDestination.MORE && moreSubView != MoreSubView.HUB)
                            val backTargetDestination = if (currentDestination == WseiDestination.PROFILE) {
                                profileReturnDestination
                            } else {
                                WseiDestination.START
                            }

                            @Composable
                            fun RenderTab(destination: WseiDestination, subView: MoreSubView) {
                                tabStateHolder.SaveableStateProvider(destination.name) {
                                    DestinationContent(
                                        destination = destination,
                                        uiState = scheduleUiState,
                                        moreSubView = subView,
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
                                        onSelectColorPalette = onSelectColorPalette,
                                        onLoadDeviceCalendars = onLoadDeviceCalendars,
                                        onSyncToCalendar = onSyncToCalendar,
                                        onDismissCalendarSyncStatus = onDismissCalendarSyncStatus,
                                        onToggleLiveNotification = onToggleLiveNotification,
                                        onToggleHideSensitiveData = onToggleHideSensitiveData
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    // Szyna obsługuje już wcięcia od strony początkowej (pasek
                                    // systemowy, wycięcie aparatu w poziomie).
                                    .then(
                                        if (useRail) {
                                            Modifier.consumeWindowInsets(
                                                WindowInsets.safeDrawing.only(WindowInsetsSides.Start)
                                            )
                                        } else {
                                            Modifier
                                        }
                                    )
                                    // W poziomie wycięcie aparatu i 3-przyciskowy pasek systemu
                                    // leżą z boku — treść nie może pod nie wchodzić.
                                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                            ) {
                                WseiPredictiveBackContainer(
                                    enabled = shellBackEnabled,
                                    state = shellBackState,
                                    onBack = {
                                        if (currentDestination == WseiDestination.PROFILE) {
                                            currentDestination = profileReturnDestination
                                        } else {
                                            moreSubView = MoreSubView.HUB
                                            currentDestination = WseiDestination.START
                                        }
                                    },
                                    peekContent = {
                                        val peekSubView = if (backTargetDestination == WseiDestination.MORE) {
                                            moreSubView
                                        } else {
                                            MoreSubView.HUB
                                        }
                                        RenderTab(backTargetDestination, peekSubView)
                                    },
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    AnimatedContent(
                                        targetState = currentDestination,
                                        transitionSpec = {
                                            if (shellBackState.consumeCommittedSwipe()) {
                                                ContentTransform(
                                                    targetContentEnter = EnterTransition.None,
                                                    initialContentExit = ExitTransition.None,
                                                    sizeTransform = null
                                                )
                                            } else {
                                                destinationTransform(initialState, targetState)
                                            }
                                        },
                                        modifier = Modifier.fillMaxSize(),
                                        label = "destinationTransition"
                                    ) { destination ->
                                        RenderTab(destination, moreSubView)
                                    }
                                }

                                // Komunikat stanu i (na telefonie) pływający pasek — na dole,
                                // wyśrodkowane nad treścią, ponad paskiem systemowym.
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .navigationBarsPadding()
                                        .padding(
                                            start = WseiSpacing.m,
                                            end = WseiSpacing.m,
                                            bottom = if (useRail) WseiSpacing.l else WseiSpacing.m
                                        ),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    StatusMessage(
                                        message = scheduleUiState.statusBannerMessage,
                                        onDismiss = onDismissStatusBanner
                                    )
                                    val navVisible = !useRail &&
                                        (currentDestination != WseiDestination.PROFILE || shellBackState.isPeeking)
                                    val navSelectedDestination = if (currentDestination == WseiDestination.PROFILE) {
                                        profileReturnDestination
                                    } else {
                                        currentDestination
                                    }
                                    AnimatedVisibility(
                                        visible = navVisible,
                                        enter = slideInVertically(WseiMotion.spatial()) { it * 2 } + fadeIn(WseiMotion.effects()),
                                        exit = slideOutVertically(WseiMotion.size()) { it * 2 } + fadeOut(WseiMotion.fastEffects())
                                    ) {
                                        WseiFloatingNavigationBar(
                                            currentDestination = navSelectedDestination,
                                            unreadAnnouncementsCount = scheduleUiState.unreadAnnouncementsCount,
                                            onSelectDestination = { selectTab(it) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // The About page has its own update card, so the modal prompt steps aside there
        // (and while the full changelog sheet is open).
        UpdatePromptHost(
            viewModel = updateViewModel,
            allowed = (phase == AuthPhase.APP || phase == AuthPhase.LOGIN) &&
                !showChangelog &&
                !(phase == AuthPhase.APP &&
                    currentDestination == WseiDestination.MORE &&
                    moreSubView == MoreSubView.ABOUT),
            onShowChanges = { showChangelog = true }
        )

        if (showChangelog) {
            UpdateChangelogSheet(
                onDismiss = { showChangelog = false },
                viewModel = updateViewModel
            )
        }
    }
}

/**
 * One transition for every destination change, built only from fade, scale and a small spring
 * offset (no size animation, so nothing is measured twice):
 * - Profile opens with a lift-in scale from the top end (where the avatar is) and closes back,
 * - tabs slide a few dp in the direction of travel on the bar.
 * The outgoing screen fades out quickly so the two never visibly overlap.
 */
private fun destinationTransform(initial: WseiDestination, target: WseiDestination): ContentTransform {
    val profileOrigin = TransformOrigin(1f, 0f)
    val transform = when {
        target == WseiDestination.PROFILE ->
            (fadeIn(WseiMotion.effects()) +
                scaleIn(WseiMotion.spatial(), initialScale = 0.92f, transformOrigin = profileOrigin)) togetherWith
                fadeOut(WseiMotion.fastEffects())
        initial == WseiDestination.PROFILE ->
            fadeIn(WseiMotion.effects()) togetherWith
                (fadeOut(WseiMotion.fastEffects()) +
                    scaleOut(WseiMotion.size(), targetScale = 0.94f, transformOrigin = profileOrigin))
        else -> {
            val forward = target.ordinal > initial.ordinal
            (fadeIn(WseiMotion.effects()) +
                slideInHorizontally(WseiMotion.spatial()) { width -> if (forward) width / 24 else -width / 24 }) togetherWith
                fadeOut(WseiMotion.fastEffects())
        }
    }
    // No size animation between tabs: every destination fills the same bounds.
    return ContentTransform(
        targetContentEnter = transform.targetContentEnter,
        initialContentExit = transform.initialContentExit,
        targetContentZIndex = transform.targetContentZIndex,
        sizeTransform = null
    )
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
    onSelectColorPalette: (AppColorPalette) -> Unit,
    onLoadDeviceCalendars: () -> Unit,
    onSyncToCalendar: (Long) -> Unit,
    onDismissCalendarSyncStatus: () -> Unit,
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
            onRefreshSchedule = onRefresh,
            onLoadDeviceCalendars = onLoadDeviceCalendars,
            onSyncToCalendar = onSyncToCalendar,
            onDismissCalendarSyncStatus = onDismissCalendarSyncStatus
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
            onSelectColorPalette = onSelectColorPalette,
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
        enter = slideInVertically(WseiMotion.spatial()) { it } + fadeIn(WseiMotion.effects()),
        exit = slideOutVertically(WseiMotion.size()) { it } + fadeOut(WseiMotion.fastEffects())
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
                        text = stringResource(R.string.common_ok),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.inversePrimary
                    )
                }
            }
        }
    }
}
