package pl.lublin.wsei.plan

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import pl.lublin.wsei.plan.service.LiveClassNotificationService
import pl.lublin.wsei.plan.ui.navigation.WseiAppShell
import pl.lublin.wsei.plan.ui.schedule.ScheduleViewModel
import pl.lublin.wsei.plan.ui.theme.WseiExpressiveTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val scheduleViewModel: ScheduleViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val uiState by scheduleViewModel.uiState.collectAsStateWithLifecycle()

            // Od Androida 13 powiadomienia wymagają zgody użytkownika. Bez niej usługa działa, ale nic nie widać.
            var notificationsGranted by remember { mutableStateOf(hasNotificationPermission()) }
            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted -> notificationsGranted = granted }
            LaunchedEffect(uiState.currentSession?.studId, uiState.isLiveNotificationEnabled) {
                if (uiState.currentSession != null &&
                    uiState.isLiveNotificationEnabled &&
                    !notificationsGranted &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            WseiExpressiveTheme(themeMode = uiState.themeMode) {
                // Powiadomienie działa dla zalogowanego studenta, gdy funkcja jest włączona i mamy zgodę.
                // Zatrzymujemy je dopiero, gdy nie ma już zajęć do śledzenia (także po wylogowaniu).
                LaunchedEffect(
                    uiState.currentSession?.studId,
                    uiState.liveTrackingEventId,
                    uiState.isLiveNotificationEnabled,
                    notificationsGranted
                ) {
                    val eventId = uiState.liveTrackingEventId
                    if (uiState.currentSession != null &&
                        uiState.isLiveNotificationEnabled &&
                        notificationsGranted &&
                        eventId != null
                    ) {
                        LiveClassNotificationService.startForClass(
                            context = this@MainActivity,
                            eventId = eventId
                        )
                    } else {
                        LiveClassNotificationService.stop(this@MainActivity)
                    }
                }

                WseiAppShell(
                    scheduleUiState = uiState,
                    onLoginSubmit = scheduleViewModel::login,
                    onQuickDebugLogin = scheduleViewModel::loginDebug,
                    onLogout = scheduleViewModel::logout,
                    onChangePassword = scheduleViewModel::changePassword,
                    onSelectDay = scheduleViewModel::selectDay,
                    onSelectDate = scheduleViewModel::selectDate,
                    onToggleCalendarExpanded = scheduleViewModel::toggleCalendarExpanded,
                    onPreviousCalendarPeriod = scheduleViewModel::navigatePreviousCalendarPeriod,
                    onNextCalendarPeriod = scheduleViewModel::navigateNextCalendarPeriod,
                    onSelectScheduleFilter = scheduleViewModel::selectScheduleFilter,
                    onToggleHeroExpanded = scheduleViewModel::toggleHeroExpanded,
                    onSyncLiveNotification = { activeClass ->
                        if (uiState.isLiveNotificationEnabled && notificationsGranted) {
                            LiveClassNotificationService.startForClass(
                                context = this@MainActivity,
                                eventId = activeClass.id
                            )
                        }
                    },
                    onRefreshSchedule = scheduleViewModel::refreshSchedule,
                    onInitiateOnlinePayment = scheduleViewModel::initiateOnlinePayment,
                    onMarkAnnouncementRead = scheduleViewModel::markAnnouncementRead,
                    onDownloadAttachment = scheduleViewModel::downloadAttachment,
                    onDismissStatusBanner = scheduleViewModel::dismissStatusBanner,
                    onSelectThemeMode = scheduleViewModel::selectThemeMode,
                    onToggleLiveNotification = scheduleViewModel::setLiveNotificationEnabled,
                    onToggleHideSensitiveData = scheduleViewModel::setHideSensitiveData
                )
            }
        }
    }

    private fun hasNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
}
