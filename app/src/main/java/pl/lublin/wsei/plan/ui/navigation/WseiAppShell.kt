package pl.lublin.wsei.plan.ui.navigation

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.window.core.layout.WindowSizeClass
import kotlinx.datetime.LocalDate
import pl.lublin.wsei.plan.R
import pl.lublin.wsei.plan.data.update.UpdateViewModel
import pl.lublin.wsei.plan.domain.model.AnnouncementAttachment
import pl.lublin.wsei.plan.domain.model.AppColorPalette
import pl.lublin.wsei.plan.domain.model.ClassEvent
import pl.lublin.wsei.plan.domain.model.DeanAnnouncement
import pl.lublin.wsei.plan.domain.model.FinancialObligation
import pl.lublin.wsei.plan.domain.model.ScheduleFilterType
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
import pl.lublin.wsei.plan.ui.more.MoreAboutRoute
import pl.lublin.wsei.plan.ui.more.MoreAnnouncementsRoute
import pl.lublin.wsei.plan.ui.more.MoreScreen
import pl.lublin.wsei.plan.ui.more.MoreSettingsRoute
import pl.lublin.wsei.plan.ui.profile.ProfileScreen
import pl.lublin.wsei.plan.ui.schedule.AppThemeMode
import pl.lublin.wsei.plan.ui.schedule.ScheduleScreen
import pl.lublin.wsei.plan.ui.schedule.ScheduleUiState
import pl.lublin.wsei.plan.ui.subjects.SubjectsScreen
import pl.lublin.wsei.plan.ui.theme.WseiMotion
import pl.lublin.wsei.plan.ui.theme.WseiSpacing
import pl.lublin.wsei.plan.ui.update.UpdateChangelogSheet
import pl.lublin.wsei.plan.ui.update.UpdatePromptHost

/**
 * Primary destinations of the app. The first five sit on the floating bottom bar (compact phones)
 * or on the side navigation rail (tablets, foldables, landscape). Profile opens from the student
 * avatar in the top app bar or rail header.
 */
enum class WseiDestination(
    @param:StringRes val labelRes: Int,
    @param:StringRes val shortLabelRes: Int,
    @param:StringRes val contentDescriptionRes: Int
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
    var showChangelog by rememberSaveable { mutableStateOf(false) }
    var isAboutCurrentlyVisible by rememberSaveable { mutableStateOf(false) }
    val isLoggedIn = scheduleUiState.currentSession != null

    // Activity-scoped: the prompt, "O aplikacji" and the changelog share one update state.
    val updateViewModel: UpdateViewModel = hiltViewModel()
    LifecycleResumeEffect(updateViewModel) {
        updateViewModel.autoCheckOnForeground()
        onPauseOrDispose { }
    }

    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    var welcomeSeen by rememberSaveable { mutableStateOf(prefs.getBoolean(PREF_WELCOME_SEEN, false)) }

    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn) {
            isAboutCurrentlyVisible = false
        }
    }

    val phase = when {
        isLoggedIn -> AuthPhase.APP
        scheduleUiState.isSessionRestoring -> AuthPhase.SPLASH
        !welcomeSeen -> AuthPhase.WELCOME
        else -> AuthPhase.LOGIN
    }

    val windowLayout = rememberWseiWindowLayout()
    val isTallWindow = currentWindowAdaptiveInfo().windowSizeClass
        .isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND)

    CompositionLocalProvider(LocalWseiWindowLayout provides windowLayout) {
        AnimatedContent(
            targetState = phase,
            transitionSpec = {
                scaleIn(WseiMotion.navigation(), initialScale = 0.97f) togetherWith
                    ExitTransition.None using null
            },
            modifier = modifier.fillMaxSize(),
            label = "authPhaseTransition"
        ) { currentPhase ->
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onBackground
            ) {
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
                    AuthPhase.APP -> WseiAuthenticatedAppContent(
                        scheduleUiState = scheduleUiState,
                        updateViewModel = updateViewModel,
                        isTallWindow = isTallWindow,
                        onAboutVisibilityChanged = { isAboutCurrentlyVisible = it },
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
                        onRefreshSchedule = onRefreshSchedule,
                        onInitiateOnlinePayment = onInitiateOnlinePayment,
                        onMarkAnnouncementRead = onMarkAnnouncementRead,
                        onDownloadAttachment = onDownloadAttachment,
                        onDismissStatusBanner = onDismissStatusBanner,
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
        }

        // The About page has its own update card, so the modal prompt steps aside there
        // (and while the full changelog sheet is open).
        UpdatePromptHost(
            viewModel = updateViewModel,
            allowed = (phase == AuthPhase.APP || phase == AuthPhase.LOGIN) &&
                !showChangelog &&
                !(phase == AuthPhase.APP && isAboutCurrentlyVisible),
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
 * Authenticated shell: a single outer [Scaffold] owns the floating bottom bar (`bottomBar`) while
 * [NavHost] inside it manages all top-level tabs and detail sub-screens (`Ogłoszenia`,
 * `Ustawienia`, `O aplikacji`, `Profil`). Both [WseiFloatingNavigationBar] and
 * [WseiNavigationRail] read [NavHostController] directly as their single source of truth — never
 * cached selection state or local function references that capture a stale route string.
 */
@Composable
private fun WseiAuthenticatedAppContent(
    scheduleUiState: ScheduleUiState,
    updateViewModel: UpdateViewModel,
    isTallWindow: Boolean,
    onAboutVisibilityChanged: (Boolean) -> Unit,
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
    onToggleHideSensitiveData: (Boolean) -> Unit
) {
    val windowLayout = LocalWseiWindowLayout.current
    val currentSupportsTwoPane by rememberUpdatedState(windowLayout.supportsTwoPane)
    val useRail = windowLayout.useNavigationRail
    val railShowsProfile = useRail && isTallWindow

    val navController = rememberNavController()
    val activeBackStackEntry = navController.rememberActiveBackStackEntry()
    val currentRoute = activeBackStackEntry?.destination?.route ?: WseiRoute.START
    val isTopLevelDestination = isTopLevelRoute(currentRoute)

    // Selected right-hand detail pane when MoreScreen runs in two-pane mode (tablets / foldables).
    var moreTwoPaneSubView by rememberSaveable { mutableStateOf(MoreSubView.HUB) }

    // Keep single-pane sub-routes and two-pane detail selection synchronised across fold/unfold.
    LaunchedEffect(windowLayout.supportsTwoPane, currentRoute) {
        if (windowLayout.supportsTwoPane && currentRoute in WseiRoute.MoreSubRoutes) {
            moreTwoPaneSubView = routeToMoreSubView(currentRoute)
            if (!navController.popBackStack(WseiRoute.MORE, inclusive = false)) {
                val startId = runCatching { navController.graph.findStartDestination().id }.getOrNull() ?: return@LaunchedEffect
                navController.navigate(WseiRoute.MORE) {
                    popUpTo(startId) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        } else if (!windowLayout.supportsTwoPane &&
            currentRoute == WseiRoute.MORE &&
            moreTwoPaneSubView != MoreSubView.HUB
        ) {
            val subRoute = moreSubViewToRoute(moreTwoPaneSubView)
            moreTwoPaneSubView = MoreSubView.HUB
            if (runCatching { navController.graph }.isSuccess) {
                navController.navigate(subRoute) {
                    launchSingleTop = true
                }
            }
        }
    }

    val showingAbout = currentRoute == WseiRoute.ABOUT ||
        (currentRoute == WseiRoute.MORE && windowLayout.supportsTwoPane && moreTwoPaneSubView == MoreSubView.ABOUT)
    LaunchedEffect(showingAbout) {
        onAboutVisibilityChanged(showingAbout)
    }

    val openMoreSubView: (MoreSubView) -> Unit = openMore@{ subView ->
        if (runCatching { navController.graph }.isFailure) return@openMore
        val liveRoute = navController.liveActiveDestination()?.route
        if (currentSupportsTwoPane) {
            moreTwoPaneSubView = subView
            if (liveRoute != WseiRoute.MORE) {
                val startId = runCatching { navController.graph.findStartDestination().id }.getOrNull() ?: return@openMore
                navController.navigate(WseiRoute.MORE) {
                    popUpTo(startId) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        } else if (subView == MoreSubView.HUB) {
            navController.navigateToTopLevelTab(
                destination = WseiDestination.MORE,
                onResetMoreTwoPane = { moreTwoPaneSubView = MoreSubView.HUB }
            )
        } else {
            val targetRoute = moreSubViewToRoute(subView)
            if (liveRoute != targetRoute) {
                // Keep MoreScreen on the back stack (no popUpTo(MORE) { inclusive = true }).
                navController.navigate(targetRoute) {
                    launchSingleTop = true
                }
            }
        }
    }

    val popDetailBackStack: () -> Unit = {
        if (runCatching { navController.graph }.isSuccess) {
            navController.popBackStack()
        }
    }

    val resetMoreTwoPane: () -> Unit = {
        moreTwoPaneSubView = MoreSubView.HUB
    }

    val session = scheduleUiState.currentSession
    val studentFallbackName = stringResource(R.string.common_student)
    val profileShortcut = remember(
        session?.personPictureBase64,
        session?.fullName,
        railShowsProfile,
        studentFallbackName,
        navController
    ) {
        WseiProfileShortcut(
            photoBase64 = session?.personPictureBase64,
            fullName = session?.fullName ?: studentFallbackName,
            onOpen = {
                if (runCatching { navController.graph }.isSuccess &&
                    navController.liveActiveDestination()?.route != WseiRoute.PROFILE
                ) {
                    navController.navigate(WseiRoute.PROFILE) {
                        launchSingleTop = true
                    }
                }
            },
            showInTopBar = !railShowsProfile
        )
    }

    CompositionLocalProvider(LocalWseiProfileShortcut provides profileShortcut) {
        Row(modifier = Modifier.fillMaxSize()) {
            if (useRail) {
                WseiNavigationRail(
                    navController = navController,
                    unreadAnnouncementsCount = scheduleUiState.unreadAnnouncementsCount,
                    expanded = windowLayout.widthClass == WseiWidthClass.EXPANDED,
                    profileShortcut = if (railShowsProfile) profileShortcut else null,
                    onResetMoreTwoPane = resetMoreTwoPane
                )
            }

            Scaffold(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(
                        if (useRail) {
                            Modifier.consumeWindowInsets(
                                WindowInsets.safeDrawing.only(WindowInsetsSides.Start)
                            )
                        } else {
                            Modifier
                        }
                    ),
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onBackground,
                contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
                bottomBar = {
                    WseiShellBottomBar(
                        navController = navController,
                        statusMessage = scheduleUiState.statusBannerMessage,
                        onDismissStatusMessage = onDismissStatusBanner,
                        showFloatingNavBar = !useRail && isTopLevelDestination,
                        useRail = useRail,
                        unreadAnnouncementsCount = scheduleUiState.unreadAnnouncementsCount,
                        onResetMoreTwoPane = resetMoreTwoPane
                    )
                }
            ) { innerPadding ->
                val layoutDirection = LocalLayoutDirection.current
                NavHost(
                    navController = navController,
                    startDestination = WseiRoute.START,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            start = innerPadding.calculateStartPadding(layoutDirection),
                            end = innerPadding.calculateEndPadding(layoutDirection)
                        ),
                    enterTransition = {
                        wseiNavEnterTransition(
                            initialRoute = initialState.destination.route,
                            targetRoute = targetState.destination.route
                        )
                    },
                    exitTransition = {
                        wseiNavExitTransition(
                            initialRoute = initialState.destination.route,
                            targetRoute = targetState.destination.route
                        )
                    },
                    popEnterTransition = { wseiNavPopEnterTransition() },
                    popExitTransition = { wseiNavPopExitTransition() },
                    sizeTransform = { null }
                ) {
                    composable(WseiRoute.START) {
                        WseiDestinationSurface(scope = this, floatingBarVisible = !useRail) {
                            HomeScreen(
                                uiState = scheduleUiState,
                                onOpenSchedule = {
                                    navController.navigateToTopLevelTab(WseiDestination.SCHEDULE)
                                },
                                onOpenSubjects = {
                                    navController.navigateToTopLevelTab(WseiDestination.SUBJECTS)
                                },
                                onOpenFinances = {
                                    navController.navigateToTopLevelTab(WseiDestination.FINANCES)
                                },
                                onOpenAnnouncements = {
                                    openMoreSubView(MoreSubView.ANNOUNCEMENTS)
                                },
                                onRefresh = onRefreshSchedule
                            )
                        }
                    }
                    composable(WseiRoute.SCHEDULE) {
                        WseiDestinationSurface(scope = this, floatingBarVisible = !useRail) {
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
                                onLoadDeviceCalendars = onLoadDeviceCalendars,
                                onSyncToCalendar = onSyncToCalendar,
                                onDismissCalendarSyncStatus = onDismissCalendarSyncStatus
                            )
                        }
                    }
                    composable(WseiRoute.SUBJECTS) {
                        WseiDestinationSurface(scope = this, floatingBarVisible = !useRail) {
                            SubjectsScreen(
                                uiState = scheduleUiState,
                                onRefresh = onRefreshSchedule
                            )
                        }
                    }
                    composable(WseiRoute.FINANCES) {
                        WseiDestinationSurface(scope = this, floatingBarVisible = !useRail) {
                            FinancesScreen(
                                uiState = scheduleUiState,
                                onInitiateOnlinePayment = onInitiateOnlinePayment,
                                onRefresh = onRefreshSchedule
                            )
                        }
                    }
                    composable(WseiRoute.MORE) {
                        WseiDestinationSurface(scope = this, floatingBarVisible = !useRail) {
                            MoreScreen(
                                uiState = scheduleUiState,
                                currentSubView = moreTwoPaneSubView,
                                onNavigateSubView = openMoreSubView,
                                onMarkAnnouncementRead = onMarkAnnouncementRead,
                                onDownloadAttachment = onDownloadAttachment,
                                onSelectThemeMode = onSelectThemeMode,
                                onSelectColorPalette = onSelectColorPalette,
                                onToggleLiveNotification = onToggleLiveNotification,
                                onToggleHideSensitiveData = onToggleHideSensitiveData,
                                onRefresh = onRefreshSchedule,
                                updateViewModel = updateViewModel
                            )
                        }
                    }
                    composable(WseiRoute.ANNOUNCEMENTS) {
                        WseiDestinationSurface(scope = this, floatingBarVisible = false) {
                            MoreAnnouncementsRoute(
                                uiState = scheduleUiState,
                                onBack = popDetailBackStack,
                                onMarkAnnouncementRead = onMarkAnnouncementRead,
                                onDownloadAttachment = onDownloadAttachment,
                                onRefresh = onRefreshSchedule
                            )
                        }
                    }
                    composable(WseiRoute.SETTINGS) {
                        WseiDestinationSurface(scope = this, floatingBarVisible = false) {
                            MoreSettingsRoute(
                                uiState = scheduleUiState,
                                onBack = popDetailBackStack,
                                onSelectThemeMode = onSelectThemeMode,
                                onSelectColorPalette = onSelectColorPalette,
                                onToggleLiveNotification = onToggleLiveNotification,
                                onToggleHideSensitiveData = onToggleHideSensitiveData
                            )
                        }
                    }
                    composable(WseiRoute.ABOUT) {
                        WseiDestinationSurface(scope = this, floatingBarVisible = false) {
                            MoreAboutRoute(
                                onBack = popDetailBackStack,
                                updateViewModel = updateViewModel
                            )
                        }
                    }
                    composable(WseiRoute.PROFILE) {
                        WseiDestinationSurface(scope = this, floatingBarVisible = false) {
                            ProfileScreen(
                                uiState = scheduleUiState,
                                onBack = popDetailBackStack,
                                onChangePassword = onChangePassword,
                                onLogout = onLogout,
                                onRefresh = onRefreshSchedule
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Single shell-level bottom overlay rendered in `Scaffold.bottomBar` outside [NavHost]:
 * - passes [navController] directly to [WseiFloatingNavigationBar] so selection state is never
 *   buffered through intermediate props or stale closures,
 * - slides the floating bar smoothly out of view on detail sub-screens (`Ogłoszenia`, `Ustawienia`,
 *   `O aplikacji`, `Profil`) and back in when returning to a top-level tab.
 */
@Composable
private fun WseiShellBottomBar(
    navController: NavHostController,
    statusMessage: String?,
    onDismissStatusMessage: () -> Unit,
    showFloatingNavBar: Boolean,
    useRail: Boolean,
    unreadAnnouncementsCount: Int,
    onResetMoreTwoPane: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StatusMessage(
            message = statusMessage,
            onDismiss = onDismissStatusMessage,
            modifier = Modifier
                .padding(horizontal = WseiSpacing.m)
                .then(
                    if (!showFloatingNavBar) {
                        Modifier
                            .navigationBarsPadding()
                            .padding(bottom = if (useRail) WseiSpacing.l else WseiSpacing.m)
                    } else {
                        Modifier
                    }
                )
        )
        AnimatedVisibility(
            visible = showFloatingNavBar,
            enter = slideInVertically(WseiMotion.spatial()) { it } + fadeIn(WseiMotion.effects()),
            exit = slideOutVertically(WseiMotion.size()) { it } + fadeOut(WseiMotion.fastEffects())
        ) {
            WseiFloatingNavigationBar(
                navController = navController,
                unreadAnnouncementsCount = unreadAnnouncementsCount,
                onResetMoreTwoPane = onResetMoreTwoPane,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(
                        top = WseiSpacing.s,
                        start = WseiSpacing.m,
                        end = WseiSpacing.m,
                        bottom = if (useRail) WseiSpacing.l else WseiSpacing.m
                    )
            )
        }
    }
}

/**
 * Komunikat stanu (np. „Zsynchronizowano”) jako pływający snackbar nad paskiem nawigacji,
 * zamiast paska przesuwającego całą treść ekranu.
 */
@Composable
private fun StatusMessage(
    message: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lastMessageHolder = remember { arrayOf("") }
    if (message != null) lastMessageHolder[0] = message
    val lastMessage = lastMessageHolder[0]

    AnimatedVisibility(
        visible = message != null,
        modifier = modifier,
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
