package pl.lublin.wsei.plan.ui.navigation

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import pl.lublin.wsei.plan.ui.theme.WseiMotion

/**
 * Shared state for Material 3 Expressive Predictive Back transitions.
 *
 * Reads of [progress] and [commitAlpha] happen inside [Modifier.graphicsLayer] lambdas so touch
 * frames during an edge swipe never trigger Compose recomposition or relayout.
 */
@Stable
class WseiPredictiveBackState {
    internal val progressAnim = Animatable(0f)
    internal val commitAlphaAnim = Animatable(1f)

    internal var isDragging by mutableStateOf(false)
    internal var swipeEdge by mutableIntStateOf(BackEventCompat.EDGE_LEFT)
    private var justCommittedFromSwipe = false

    val progress: Float get() = progressAnim.value
    val commitAlpha: Float get() = commitAlphaAnim.value

    val isPeeking: Boolean by derivedStateOf {
        isDragging || progressAnim.value > 0.002f
    }

    /**
     * Returns `true` once right after a finger-driven predictive back swipe commits, allowing
     * `AnimatedContent` to hand off immediately without replaying its full slide/scale transition.
     */
    fun consumeCommittedSwipe(): Boolean {
        val committed = justCommittedFromSwipe
        justCommittedFromSwipe = false
        return committed
    }

    internal fun markCommittedSwipe() {
        justCommittedFromSwipe = true
    }
}

@Composable
fun rememberWseiPredictiveBackState(): WseiPredictiveBackState =
    remember { WseiPredictiveBackState() }

/**
 * Material 3 Expressive Predictive Back container:
 * - shrinks the active foreground screen (`1.0 → 0.90`), rounds its corners (`0 → 28.dp`) and
 *   translates it away from the active swipe edge,
 * - reveals [peekContent] underneath with subtle scale (`0.95 → 1.0`) and parallax,
 * - springs back with [WseiMotion.spatial] if the user cancels the gesture, or glides cleanly into
 *   [peekContent] on release.
 */
@Composable
fun WseiPredictiveBackContainer(
    enabled: Boolean,
    state: WseiPredictiveBackState,
    onBack: () -> Unit,
    peekContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    foregroundBackgroundColor: Color = MaterialTheme.colorScheme.background,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    var recoveryJob by remember { mutableStateOf<Job?>(null) }

    PredictiveBackHandler(enabled = enabled) { progressFlow: Flow<BackEventCompat> ->
        recoveryJob?.cancel()
        var maxRawProgress = 0f
        state.isDragging = true
        state.commitAlphaAnim.snapTo(1f)
        try {
            progressFlow.collect { backEvent ->
                state.swipeEdge = backEvent.swipeEdge
                val raw = backEvent.progress.coerceIn(0f, 1f)
                if (raw > maxRawProgress) maxRawProgress = raw
                state.progressAnim.snapTo(predictiveBackEasing(raw))
            }
            state.isDragging = false
            if (maxRawProgress >= MIN_SWIPE_PROGRESS_FOR_GESTURE_COMMIT) {
                coroutineScope {
                    launch { state.progressAnim.animateTo(1f, WseiMotion.fastSpatial()) }
                    launch { state.commitAlphaAnim.animateTo(0f, WseiMotion.fastEffects()) }
                }
                state.markCommittedSwipe()
            }
            state.progressAnim.snapTo(0f)
            state.commitAlphaAnim.snapTo(1f)
            onBack()
        } catch (e: CancellationException) {
            state.isDragging = false
            recoveryJob = scope.launch {
                coroutineScope {
                    launch { state.progressAnim.animateTo(0f, WseiMotion.spatial()) }
                    launch { state.commitAlphaAnim.animateTo(1f, WseiMotion.fastEffects()) }
                }
            }
            throw e
        }
    }

    val peeking = state.isPeeking
    val scrimColor = MaterialTheme.colorScheme.scrim

    Box(modifier = modifier) {
        if (peeking) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val p = state.progress
                        val dir = if (state.swipeEdge == BackEventCompat.EDGE_RIGHT) -1f else 1f
                        val bgScale = 0.95f + 0.05f * p
                        scaleX = bgScale
                        scaleY = bgScale
                        translationX = -dir * 20.dp.toPx() * (1f - p)
                    }
            ) {
                peekContent()
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = 0.24f * (1f - state.progress) * state.commitAlpha
                    }
                    .background(scrimColor)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val p = state.progress
                    if (p > 0.001f) {
                        val dir = if (state.swipeEdge == BackEventCompat.EDGE_RIGHT) -1f else 1f
                        val fgScale = 1f - 0.10f * p
                        scaleX = fgScale
                        scaleY = fgScale
                        translationX = dir * 28.dp.toPx() * p
                        alpha = (1f - 0.08f * p) * state.commitAlpha
                        shape = RoundedCornerShape((28f * p).dp)
                        clip = true
                        shadowElevation = 16.dp.toPx() * p
                    }
                }
                .then(
                    if (peeking) Modifier.background(foregroundBackgroundColor) else Modifier
                )
        ) {
            content()
        }
    }
}

/** Cubic-out curve matching Material 3 predictive back deceleration. */
internal fun predictiveBackEasing(progress: Float): Float {
    val inv = 1f - progress.coerceIn(0f, 1f)
    return 1f - inv * inv * inv
}

private const val MIN_SWIPE_PROGRESS_FOR_GESTURE_COMMIT = 0.05f
