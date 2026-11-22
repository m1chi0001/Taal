package pl.lublin.wsei.plan.ui.update

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Material Symbols used only by the update UI (the app ships no icon library). */
internal object UpdateIcons {
    val Close: ImageVector by lazy {
        symbol(
            "Close",
            "M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12z"
        )
    }

    /** "history" — changelog. */
    val History: ImageVector by lazy {
        symbol(
            "History",
            "M13,3c-4.97,0 -9,4.03 -9,9L1,12l3.89,3.89 0.07,0.14L9,12L6,12c0,-3.87 3.13,-7 7,-7s7,3.13 7,7 " +
                "-3.13,7 -7,7c-1.93,0 -3.68,-0.79 -4.94,-2.06l-1.42,1.42C8.27,19.99 10.51,21 13,21c4.97,0 " +
                "9,-4.03 9,-9s-4.03,-9 -9,-9zM12,8v5l4.28,2.54 0.72,-1.21 -3.5,-2.08L13.5,8L12,8z"
        )
    }

    /** "new_releases" — update available. */
    val NewReleases: ImageVector by lazy {
        symbol(
            "NewReleases",
            "M23,12l-2.44,-2.78 0.34,-3.68 -3.61,-0.82 -1.89,-3.18L12,3 8.6,1.54 6.71,4.72l-3.61,0.81 " +
                "0.34,3.68L1,12l2.44,2.78 -0.34,3.69 3.61,0.82 1.89,3.18L12,21l3.4,1.46 1.89,-3.18 " +
                "3.61,-0.82 -0.34,-3.68L23,12zM13,17h-2v-2h2v2zM13,13h-2L11,7h2v6z"
        )
    }

    private fun symbol(name: String, pathData: String): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).addPath(
            pathData = addPathNodes(pathData),
            fill = SolidColor(Color.Black)
        ).build()
}
