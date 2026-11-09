@file:OptIn(ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package pl.lublin.wsei.plan.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Wspólny szkielet ekranu: ten sam nagłówek, marginesy, odstępy i gest Pull-to-Refresh.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WseiScreenScaffold(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onRefresh: (() -> Unit)? = null,
    isRefreshing: Boolean = false,
    content: LazyListScope.() -> Unit
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.statusBars
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            WseiScreenHeader(
                title = title,
                icon = icon,
                onBack = onBack
            )
            val list: @Composable () -> Unit = {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = WseiScreenContentPadding,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    content = content
                )
            }
            if (onRefresh != null) {
                WseiRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize()
                ) { list() }
            } else {
                list()
            }
        }
    }
}

val WseiScreenContentPadding = PaddingValues(
    start = 16.dp,
    end = 16.dp,
    top = 4.dp,
    bottom = 112.dp
)
