package pl.lublin.wsei.plan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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

            WseiExpressiveTheme(themeMode = uiState.themeMode) {
                // Automatyczna synchronizacja usługi Live Class Progress Notification:
                // uruchamia powiadomienie z osią czasu dnia dla zalogowanego studenta (o ile funkcja jest włączona w ustawieniach)
                // i zatrzymuje po wylogowaniu lub wyłączeniu w ustawieniach.
                LaunchedEffect(
                    uiState.currentSession?.studId,
                    uiState.activeClass?.id,
                    uiState.isLiveNotificationEnabled
                ) {
                    if (uiState.currentSession != null && uiState.isLiveNotificationEnabled) {
                        uiState.activeClass?.let { ongoing ->
                            LiveClassNotificationService.startForClass(
                                context = this@MainActivity,
                                eventId = ongoing.id
                            )
                        }
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
                        if (uiState.isLiveNotificationEnabled) {
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
}
