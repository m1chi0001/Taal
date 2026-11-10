package pl.lublin.wsei.plan.ui.icons

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import pl.lublin.wsei.plan.domain.model.ClassType

/**
 * Cohesive, 100% uniform Material Symbols Filled 24dp vector icon system for WSEI Plan.
 *
 * Includes:
 * - Floating Pill Navigation Bar icons (`Home`, `CalendarMonth`, `School`, `Person`, `Apps`)
 * - Subject-specific Material Icons (`Code`, `Computer`, `Memory`, `Security`, `Psychology`,
 *   `Cloud`, `Storage`, `Bolt`, `BusinessCenter`, `Gavel`, `Calculate`, `Science`)
 * - [forSubject]: Maps any academic subject & class form to its matching Material Icon
 * - [renderSubjectBadgeBitmap]: Renders the subject's Material Icon onto a high-DPI M3X squircle
 *   [Bitmap] for the right side of Android Live Notifications.
 */
object WseiIcons {

    // =========================================================================
    // 1. Navigation & Core UI Icons (Uniform Filled Style)
    // =========================================================================

    val Home: ImageVector by lazy {
        materialIcon("Home") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(10f, 20f)
                verticalLineToRelative(-6f)
                horizontalLineToRelative(4f)
                verticalLineToRelative(6f)
                horizontalLineToRelative(5f)
                verticalLineToRelative(-8f)
                horizontalLineToRelative(3f)
                lineTo(12f, 3f)
                lineTo(2f, 12f)
                horizontalLineToRelative(3f)
                verticalLineToRelative(8f)
                close()
            }
        }
    }

    val CalendarMonth: ImageVector by lazy {
        materialIcon("CalendarMonth") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(19f, 4f)
                horizontalLineToRelative(-1f)
                verticalLineTo(2f)
                horizontalLineToRelative(-2f)
                verticalLineToRelative(2f)
                horizontalLineTo(8f)
                verticalLineTo(2f)
                horizontalLineTo(6f)
                verticalLineToRelative(2f)
                horizontalLineTo(5f)
                curveTo(3.89f, 4f, 3f, 4.9f, 3f, 6f)
                verticalLineToRelative(14f)
                curveToRelative(0f, 1.1f, 0.89f, 2f, 2f, 2f)
                horizontalLineToRelative(14f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                verticalLineTo(6f)
                curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
                close()
                moveTo(9f, 14f)
                horizontalLineTo(7f)
                verticalLineToRelative(-2f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(2f)
                close()
                moveTo(13f, 14f)
                horizontalLineToRelative(-2f)
                verticalLineToRelative(-2f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(2f)
                close()
                moveTo(17f, 14f)
                horizontalLineToRelative(-2f)
                verticalLineToRelative(-2f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(2f)
                close()
                moveTo(9f, 18f)
                horizontalLineTo(7f)
                verticalLineToRelative(-2f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(2f)
                close()
                moveTo(13f, 18f)
                horizontalLineToRelative(-2f)
                verticalLineToRelative(-2f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(2f)
                close()
                moveTo(17f, 18f)
                horizontalLineToRelative(-2f)
                verticalLineToRelative(-2f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(2f)
                close()
                moveTo(19f, 8f)
                horizontalLineTo(5f)
                verticalLineTo(6f)
                horizontalLineToRelative(14f)
                verticalLineToRelative(2f)
                close()
            }
        }
    }

    val School: ImageVector by lazy {
        materialIcon("School") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(5f, 13.18f)
                verticalLineToRelative(4f)
                lineTo(12f, 21f)
                lineToRelative(7f, -3.82f)
                verticalLineToRelative(-4f)
                lineTo(12f, 17f)
                lineToRelative(-7f, -3.82f)
                close()
                moveTo(12f, 3f)
                lineTo(1f, 9f)
                lineToRelative(11f, 6f)
                lineToRelative(9f, -4.91f)
                verticalLineTo(17f)
                horizontalLineToRelative(2f)
                verticalLineTo(9f)
                lineTo(12f, 3f)
                close()
            }
        }
    }

    val Person: ImageVector by lazy {
        materialIcon("Person") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 12f)
                curveToRelative(2.21f, 0f, 4f, -1.79f, 4f, -4f)
                reflectiveCurveToRelative(-1.79f, -4f, -4f, -4f)
                reflectiveCurveToRelative(-4f, 1.79f, -4f, 4f)
                reflectiveCurveToRelative(1.79f, 4f, 4f, 4f)
                close()
                moveTo(12f, 14f)
                curveToRelative(-2.67f, 0f, -8f, 1.34f, -8f, 4f)
                verticalLineToRelative(2f)
                horizontalLineToRelative(16f)
                verticalLineToRelative(-2f)
                curveToRelative(0f, -2.66f, -5.33f, -4f, -8f, -4f)
                close()
            }
        }
    }

    val Apps: ImageVector by lazy {
        materialIcon("Apps") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(4f, 8f)
                horizontalLineToRelative(4f)
                verticalLineTo(4f)
                horizontalLineTo(4f)
                verticalLineToRelative(4f)
                close()
                moveTo(10f, 20f)
                horizontalLineToRelative(4f)
                verticalLineToRelative(-4f)
                horizontalLineToRelative(-4f)
                verticalLineToRelative(4f)
                close()
                moveTo(4f, 20f)
                horizontalLineToRelative(4f)
                verticalLineToRelative(-4f)
                horizontalLineTo(4f)
                verticalLineToRelative(4f)
                close()
                moveTo(4f, 14f)
                horizontalLineToRelative(4f)
                verticalLineToRelative(-4f)
                horizontalLineTo(4f)
                verticalLineToRelative(4f)
                close()
                moveTo(10f, 14f)
                horizontalLineToRelative(4f)
                verticalLineToRelative(-4f)
                horizontalLineToRelative(-4f)
                verticalLineToRelative(4f)
                close()
                moveTo(16f, 4f)
                verticalLineToRelative(4f)
                horizontalLineToRelative(4f)
                verticalLineTo(4f)
                horizontalLineToRelative(-4f)
                close()
                moveTo(10f, 8f)
                horizontalLineToRelative(4f)
                verticalLineTo(4f)
                horizontalLineToRelative(-4f)
                verticalLineToRelative(4f)
                close()
                moveTo(16f, 14f)
                horizontalLineToRelative(4f)
                verticalLineToRelative(-4f)
                horizontalLineToRelative(-4f)
                verticalLineToRelative(4f)
                close()
                moveTo(16f, 20f)
                horizontalLineToRelative(4f)
                verticalLineToRelative(-4f)
                horizontalLineToRelative(-4f)
                verticalLineToRelative(4f)
                close()
            }
        }
    }

    val AccountBalanceWallet: ImageVector by lazy {
        materialIcon("AccountBalanceWallet") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(21f, 18f)
                verticalLineToRelative(1f)
                curveToRelative(0f, 1.1f, -0.9f, 2f, -2f, 2f)
                horizontalLineTo(5f)
                curveToRelative(-1.11f, 0f, -2f, -0.9f, -2f, -2f)
                verticalLineTo(5f)
                curveToRelative(0f, -1.1f, 0.89f, -2f, 2f, -2f)
                horizontalLineToRelative(14f)
                curveToRelative(1.1f, 0f, 2f, 0.9f, 2f, 2f)
                verticalLineToRelative(1f)
                horizontalLineToRelative(-9f)
                curveToRelative(-1.11f, 0f, -2f, 0.9f, -2f, 2f)
                verticalLineToRelative(8f)
                curveToRelative(0f, 1.1f, 0.89f, 2f, 2f, 2f)
                horizontalLineToRelative(9f)
                close()
                moveTo(12f, 16f)
                horizontalLineToRelative(10f)
                verticalLineTo(8f)
                horizontalLineTo(12f)
                verticalLineToRelative(8f)
                close()
                moveTo(16f, 13.5f)
                curveToRelative(-0.83f, 0f, -1.5f, -0.67f, -1.5f, -1.5f)
                reflectiveCurveToRelative(0.67f, -1.5f, 1.5f, -1.5f)
                reflectiveCurveToRelative(1.5f, 0.67f, 1.5f, 1.5f)
                reflectiveCurveToRelative(-0.67f, 1.5f, -1.5f, 1.5f)
                close()
            }
        }
    }

    val Campaign: ImageVector by lazy {
        materialIcon("Campaign") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(18f, 11f)
                verticalLineToRelative(2f)
                horizontalLineToRelative(4f)
                verticalLineToRelative(-2f)
                horizontalLineToRelative(-4f)
                close()
                moveTo(16f, 17.61f)
                curveToRelative(0.96f, 0.71f, 2.21f, 1.65f, 3.2f, 2.39f)
                curveToRelative(0.4f, -0.53f, 0.8f, -1.07f, 1.2f, -1.6f)
                curveToRelative(-0.99f, -0.74f, -2.24f, -1.68f, -3.2f, -2.4f)
                curveToRelative(-0.4f, 0.54f, -0.8f, 1.08f, -1.2f, 1.61f)
                close()
                moveTo(20.4f, 5.6f)
                curveToRelative(-0.4f, -0.53f, -0.8f, -1.07f, -1.2f, -1.6f)
                curveToRelative(-0.99f, 0.74f, -2.24f, 1.68f, -3.2f, 2.4f)
                curveToRelative(0.4f, 0.53f, 0.8f, 1.07f, 1.2f, 1.6f)
                curveToRelative(0.96f, -0.72f, 2.21f, -1.65f, 3.2f, -2.4f)
                close()
                moveTo(4f, 9f)
                curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
                verticalLineToRelative(2f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                horizontalLineToRelative(1f)
                verticalLineToRelative(4f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(-4f)
                horizontalLineToRelative(1f)
                lineToRelative(5f, 3f)
                verticalLineTo(6f)
                lineTo(8f, 9f)
                horizontalLineTo(4f)
                close()
                moveTo(15.5f, 12f)
                curveToRelative(0f, -1.33f, -0.58f, -2.53f, -1.5f, -3.35f)
                verticalLineToRelative(6.69f)
                curveToRelative(0.92f, -0.81f, 1.5f, -2.01f, 1.5f, -3.34f)
                close()
            }
        }
    }

    val Schedule: ImageVector by lazy {
        materialIcon("Schedule") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 2f)
                curveTo(6.5f, 2f, 2f, 6.5f, 2f, 12f)
                reflectiveCurveToRelative(4.5f, 10f, 10f, 10f)
                reflectiveCurveToRelative(10f, -4.5f, 10f, -10f)
                reflectiveCurveTo(17.5f, 2f, 12f, 2f)
                close()
                moveTo(16.2f, 16.2f)
                lineTo(11f, 13f)
                verticalLineTo(7f)
                horizontalLineToRelative(1.5f)
                verticalLineToRelative(5.2f)
                lineToRelative(4.5f, 2.7f)
                lineToRelative(-0.8f, 1.3f)
                close()
            }
        }
    }

    val LocationOn: ImageVector by lazy {
        materialIcon("LocationOn") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 2f)
                curveTo(8.13f, 2f, 5f, 5.13f, 5f, 9f)
                curveToRelative(0f, 5.25f, 7f, 13f, 7f, 13f)
                reflectiveCurveToRelative(7f, -7.75f, 7f, -13f)
                curveToRelative(0f, -3.87f, -3.13f, -7f, -7f, -7f)
                close()
                moveTo(12f, 11.5f)
                curveToRelative(-1.38f, 0f, -2.5f, -1.12f, -2.5f, -2.5f)
                reflectiveCurveToRelative(1.12f, -2.5f, 2.5f, -2.5f)
                reflectiveCurveToRelative(2.5f, 1.12f, 2.5f, 2.5f)
                reflectiveCurveToRelative(-1.12f, 2.5f, -2.5f, 2.5f)
                close()
            }
        }
    }

    val Refresh: ImageVector by lazy {
        materialIcon("Refresh") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(17.65f, 6.35f)
                curveTo(16.2f, 4.9f, 14.21f, 4f, 12f, 4f)
                curveToRelative(-4.42f, 0f, -7.99f, 3.58f, -7.99f, 8f)
                reflectiveCurveToRelative(3.57f, 8f, 7.99f, 8f)
                curveToRelative(3.73f, 0f, 6.84f, -2.55f, 7.73f, -6f)
                horizontalLineToRelative(-2.08f)
                curveToRelative(-0.82f, 2.33f, -3.04f, 4f, -5.65f, 4f)
                curveToRelative(-3.31f, 0f, -6f, -2.69f, -6f, -6f)
                reflectiveCurveToRelative(2.69f, -6f, 6f, -6f)
                curveToRelative(1.66f, 0f, 3.14f, 0.69f, 4.22f, 1.78f)
                lineTo(13f, 11f)
                horizontalLineToRelative(7f)
                verticalLineTo(4f)
                lineToRelative(-2.35f, 2.35f)
                close()
            }
        }
    }

    val NotificationsActive: ImageVector by lazy {
        materialIcon("NotificationsActive") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 22f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                horizontalLineToRelative(-4f)
                curveToRelative(0f, 1.1f, 0.89f, 2f, 2f, 2f)
                close()
                moveTo(18f, 16f)
                verticalLineToRelative(-5f)
                curveToRelative(0f, -3.07f, -1.64f, -5.64f, -4.5f, -6.32f)
                verticalLineTo(4f)
                curveToRelative(0f, -0.83f, -0.67f, -1.5f, -1.5f, -1.5f)
                reflectiveCurveToRelative(-1.5f, 0.67f, -1.5f, 1.5f)
                verticalLineToRelative(0.68f)
                curveTo(7.63f, 5.36f, 6f, 7.92f, 6f, 11f)
                verticalLineToRelative(5f)
                lineToRelative(-2f, 2f)
                verticalLineToRelative(1f)
                horizontalLineToRelative(16f)
                verticalLineToRelative(-1f)
                lineToRelative(-2f, -2f)
                close()
            }
        }
    }

    val ExpandMore: ImageVector by lazy {
        materialIcon("ExpandMore") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(16.59f, 8.59f)
                lineTo(12f, 13.17f)
                lineTo(7.41f, 8.59f)
                lineTo(6f, 10f)
                lineToRelative(6f, 6f)
                lineToRelative(6f, -6f)
                close()
            }
        }
    }

    val ExpandLess: ImageVector by lazy {
        materialIcon("ExpandLess") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 8f)
                lineToRelative(-6f, 6f)
                lineToRelative(1.41f, 1.41f)
                lineTo(12f, 10.83f)
                lineToRelative(4.59f, 4.58f)
                lineTo(18f, 14f)
                close()
            }
        }
    }

    val ChevronLeft: ImageVector by lazy {
        materialIcon("ChevronLeft") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(15.41f, 7.41f)
                lineTo(14f, 6f)
                lineToRelative(-6f, 6f)
                lineToRelative(6f, 6f)
                lineToRelative(1.41f, -1.41f)
                lineTo(10.83f, 12f)
                close()
            }
        }
    }

    val ChevronRight: ImageVector by lazy {
        materialIcon("ChevronRight") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(10f, 6f)
                lineTo(8.59f, 7.41f)
                lineTo(13.17f, 12f)
                lineToRelative(-4.58f, 4.59f)
                lineTo(10f, 18f)
                lineToRelative(6f, -6f)
                close()
            }
        }
    }

    val Logout: ImageVector by lazy {
        materialIcon("Logout") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(17f, 7f)
                lineToRelative(-1.41f, 1.41f)
                lineTo(18.17f, 11f)
                horizontalLineTo(8f)
                verticalLineToRelative(2f)
                horizontalLineToRelative(10.17f)
                lineToRelative(-2.58f, 2.58f)
                lineTo(17f, 17f)
                lineToRelative(5f, -5f)
                close()
                moveTo(4f, 5f)
                horizontalLineToRelative(8f)
                verticalLineTo(3f)
                horizontalLineTo(4f)
                curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
                verticalLineToRelative(14f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                horizontalLineToRelative(8f)
                verticalLineToRelative(-2f)
                horizontalLineTo(4f)
                verticalLineTo(5f)
                close()
            }
        }
    }

    val Lock: ImageVector by lazy {
        materialIcon("Lock") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(18f, 8f)
                horizontalLineToRelative(-1f)
                verticalLineTo(6f)
                curveToRelative(0f, -2.76f, -2.24f, -5f, -5f, -5f)
                reflectiveCurveTo(7f, 3.24f, 7f, 6f)
                verticalLineToRelative(2f)
                horizontalLineTo(6f)
                curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
                verticalLineToRelative(10f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                horizontalLineToRelative(12f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                verticalLineTo(10f)
                curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
                close()
                moveTo(12f, 17f)
                curveToRelative(-1.1f, 0f, -2f, -0.9f, -2f, -2f)
                reflectiveCurveToRelative(0.9f, -2f, 2f, -2f)
                reflectiveCurveToRelative(2f, 0.9f, 2f, 2f)
                reflectiveCurveToRelative(-0.9f, 2f, -2f, 2f)
                close()
                moveTo(15.1f, 8f)
                horizontalLineTo(8.9f)
                verticalLineTo(6f)
                curveToRelative(0f, -1.71f, 1.39f, -3.1f, 3.1f, -3.1f)
                curveToRelative(1.71f, 0f, 3.1f, 1.39f, 3.1f, 3.1f)
                verticalLineToRelative(2f)
                close()
            }
        }
    }

    val Bolt: ImageVector by lazy {
        materialIcon("Bolt") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(11f, 21f)
                horizontalLineToRelative(-1f)
                lineToRelative(1f, -7f)
                horizontalLineTo(7.5f)
                curveToRelative(-0.58f, 0f, -0.57f, -0.32f, -0.38f, -0.66f)
                lineToRelative(0.1f, -0.16f)
                lineTo(12.92f, 3f)
                horizontalLineToRelative(1f)
                lineToRelative(-1f, 7f)
                horizontalLineToRelative(3.5f)
                curveToRelative(0.49f, 0f, 0.56f, 0.33f, 0.47f, 0.51f)
                lineToRelative(-0.07f, 0.15f)
                lineTo(11f, 21f)
                close()
            }
        }
    }

    val CheckCircle: ImageVector by lazy {
        materialIcon("CheckCircle") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 2f)
                curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
                reflectiveCurveToRelative(4.48f, 10f, 10f, 10f)
                reflectiveCurveToRelative(10f, -4.48f, 10f, -10f)
                reflectiveCurveTo(17.52f, 2f, 12f, 2f)
                close()
                moveTo(10f, 17f)
                lineToRelative(-5f, -5f)
                lineToRelative(1.41f, -1.41f)
                lineTo(10f, 14.17f)
                lineToRelative(7.59f, -7.59f)
                lineTo(19f, 8f)
                lineToRelative(-9f, 9f)
                close()
            }
        }
    }

    val ArrowForward: ImageVector by lazy {
        materialIcon("ArrowForward") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 4f)
                lineToRelative(-1.41f, 1.41f)
                lineTo(16.17f, 11f)
                horizontalLineTo(4f)
                verticalLineToRelative(2f)
                horizontalLineToRelative(12.17f)
                lineToRelative(-5.58f, 5.59f)
                lineTo(12f, 20f)
                lineToRelative(8f, -8f)
                close()
            }
        }
    }

    val Groups: ImageVector by lazy {
        materialIcon("Groups") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(16f, 11f)
                curveToRelative(1.66f, 0f, 2.99f, -1.34f, 2.99f, -3f)
                reflectiveCurveTo(17.66f, 5f, 16f, 5f)
                curveToRelative(-1.66f, 0f, -3f, 1.34f, -3f, 3f)
                reflectiveCurveToRelative(1.34f, 3f, 3f, 3f)
                close()
                moveTo(8f, 11f)
                curveToRelative(1.66f, 0f, 2.99f, -1.34f, 2.99f, -3f)
                reflectiveCurveTo(9.66f, 5f, 8f, 5f)
                curveTo(6.34f, 5f, 5f, 6.34f, 5f, 8f)
                reflectiveCurveToRelative(1.34f, 3f, 3f, 3f)
                close()
                moveTo(8f, 13f)
                curveToRelative(-2.33f, 0f, -7f, 1.17f, -7f, 3.5f)
                verticalLineTo(19f)
                horizontalLineToRelative(14f)
                verticalLineToRelative(-2.5f)
                curveToRelative(0f, -2.33f, -4.67f, -3.5f, -7f, -3.5f)
                close()
                moveTo(16f, 13f)
                curveToRelative(-0.29f, 0f, -0.62f, 0.02f, -0.97f, 0.05f)
                curveToRelative(1.16f, 0.84f, 1.97f, 1.97f, 1.97f, 3.45f)
                verticalLineTo(19f)
                horizontalLineToRelative(6f)
                verticalLineToRelative(-2.5f)
                curveToRelative(0f, -2.33f, -4.67f, -3.5f, -7f, -3.5f)
                close()
            }
        }
    }

    val AttachFile: ImageVector by lazy {
        materialIcon("AttachFile") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(16.5f, 6f)
                verticalLineToRelative(11.5f)
                curveToRelative(0f, 2.21f, -1.79f, 4f, -4f, 4f)
                reflectiveCurveToRelative(-4f, -1.79f, -4f, -4f)
                verticalLineTo(5f)
                curveToRelative(0f, -1.38f, 1.12f, -2.5f, 2.5f, -2.5f)
                reflectiveCurveToRelative(2.5f, 1.12f, 2.5f, 2.5f)
                verticalLineToRelative(10.5f)
                curveToRelative(0f, 0.55f, -0.45f, 1f, -1f, 1f)
                reflectiveCurveToRelative(-1f, -0.45f, -1f, -1f)
                verticalLineTo(6f)
                horizontalLineTo(10f)
                verticalLineToRelative(9.5f)
                curveToRelative(0f, 1.38f, 1.12f, 2.5f, 2.5f, 2.5f)
                reflectiveCurveToRelative(2.5f, -1.12f, 2.5f, -2.5f)
                verticalLineTo(5f)
                curveToRelative(0f, -2.21f, -1.79f, -4f, -4f, -4f)
                reflectiveCurveTo(7f, 2.79f, 7f, 5f)
                verticalLineToRelative(12.5f)
                curveToRelative(0f, 3.04f, 2.46f, 5.5f, 5.5f, 5.5f)
                reflectiveCurveToRelative(5.5f, -2.46f, 5.5f, -5.5f)
                verticalLineTo(6f)
                horizontalLineToRelative(-1.5f)
                close()
            }
        }
    }

    val ContentCopy: ImageVector by lazy {
        materialIcon("ContentCopy") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(16f, 1f)
                horizontalLineTo(4f)
                curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
                verticalLineToRelative(14f)
                horizontalLineToRelative(2f)
                verticalLineTo(3f)
                horizontalLineToRelative(12f)
                verticalLineTo(1f)
                close()
                moveTo(19f, 5f)
                horizontalLineTo(8f)
                curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
                verticalLineToRelative(14f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                horizontalLineToRelative(11f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                verticalLineTo(7f)
                curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
                close()
            }
        }
    }

    val WorkspacePremium: ImageVector by lazy {
        materialIcon("WorkspacePremium") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 2f)
                curveTo(7.58f, 2f, 4f, 5.58f, 4f, 10f)
                curveToRelative(0f, 2.03f, 0.76f, 3.87f, 2f, 5.28f)
                verticalLineTo(23f)
                lineToRelative(6f, -2f)
                lineToRelative(6f, 2f)
                verticalLineToRelative(-7.72f)
                curveToRelative(1.24f, -1.41f, 2f, -3.25f, 2f, -5.28f)
                curveTo(20f, 5.58f, 16.42f, 2f, 12f, 2f)
                close()
                moveTo(14.31f, 13.69f)
                lineTo(12f, 11.93f)
                lineToRelative(-2.31f, 1.76f)
                lineToRelative(0.88f, -2.85f)
                lineTo(8.25f, 9f)
                horizontalLineToRelative(2.84f)
                lineTo(12f, 6.19f)
                lineTo(12.91f, 9f)
                horizontalLineToRelative(2.84f)
                lineToRelative(-2.32f, 1.84f)
                lineToRelative(0.88f, 2.85f)
                close()
            }
        }
    }

    val Badge: ImageVector by lazy {
        materialIcon("Badge") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(20f, 7f)
                horizontalLineToRelative(-5f)
                verticalLineTo(4f)
                curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
                horizontalLineToRelative(-2f)
                curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
                verticalLineToRelative(3f)
                horizontalLineTo(4f)
                curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
                verticalLineToRelative(11f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                horizontalLineToRelative(16f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                verticalLineTo(9f)
                curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
                close()
                moveTo(9f, 12f)
                curveToRelative(0.83f, 0f, 1.5f, 0.67f, 1.5f, 1.5f)
                reflectiveCurveTo(9.83f, 15f, 9f, 15f)
                reflectiveCurveToRelative(-1.5f, -0.67f, -1.5f, -1.5f)
                reflectiveCurveTo(8.17f, 12f, 9f, 12f)
                close()
                moveTo(12f, 18f)
                horizontalLineTo(6f)
                verticalLineToRelative(-0.75f)
                curveToRelative(0f, -1f, 2f, -1.5f, 3f, -1.5f)
                reflectiveCurveToRelative(3f, 0.5f, 3f, 1.5f)
                verticalLineTo(18f)
                close()
                moveTo(13f, 9f)
                horizontalLineToRelative(-2f)
                verticalLineTo(4f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(5f)
                close()
                moveTo(18f, 16.5f)
                horizontalLineToRelative(-4f)
                verticalLineTo(15f)
                horizontalLineToRelative(4f)
                verticalLineToRelative(1.5f)
                close()
                moveTo(18f, 13.5f)
                horizontalLineToRelative(-4f)
                verticalLineTo(12f)
                horizontalLineToRelative(4f)
                verticalLineToRelative(1.5f)
                close()
            }
        }
    }

    val Download: ImageVector by lazy {
        materialIcon("Download") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(19f, 9f)
                horizontalLineToRelative(-4f)
                verticalLineTo(3f)
                horizontalLineTo(9f)
                verticalLineToRelative(6f)
                horizontalLineTo(5f)
                lineToRelative(7f, 7f)
                lineToRelative(7f, -7f)
                close()
                moveTo(5f, 18f)
                verticalLineToRelative(2f)
                horizontalLineToRelative(14f)
                verticalLineToRelative(-2f)
                horizontalLineTo(5f)
                close()
            }
        }
    }

    val Info: ImageVector by lazy {
        materialIcon("Info") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 2f)
                curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
                reflectiveCurveToRelative(4.48f, 10f, 10f, 10f)
                reflectiveCurveToRelative(10f, -4.48f, 10f, -10f)
                reflectiveCurveTo(17.52f, 2f, 12f, 2f)
                close()
                moveTo(13f, 17f)
                horizontalLineToRelative(-2f)
                verticalLineToRelative(-6f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(6f)
                close()
                moveTo(13f, 9f)
                horizontalLineToRelative(-2f)
                verticalLineTo(7f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(2f)
                close()
            }
        }
    }

    val Settings: ImageVector by lazy {
        materialIcon("Settings") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(19.14f, 12.94f)
                curveToRelative(0.04f, -0.3f, 0.06f, -0.61f, 0.06f, -0.94f)
                curveToRelative(0f, -0.32f, -0.02f, -0.64f, -0.07f, -0.94f)
                lineToRelative(2.03f, -1.58f)
                curveToRelative(0.18f, -0.14f, 0.23f, -0.41f, 0.12f, -0.61f)
                lineToRelative(-1.92f, -3.32f)
                curveToRelative(-0.12f, -0.22f, -0.37f, -0.29f, -0.59f, -0.22f)
                lineToRelative(-2.39f, 0.96f)
                curveToRelative(-0.5f, -0.38f, -1.03f, -0.7f, -1.62f, -0.94f)
                lineTo(14.4f, 2.81f)
                curveToRelative(-0.04f, -0.24f, -0.24f, -0.41f, -0.48f, -0.41f)
                horizontalLineToRelative(-3.84f)
                curveToRelative(-0.24f, 0f, -0.43f, 0.17f, -0.47f, 0.41f)
                lineToRelative(-0.36f, 2.54f)
                curveToRelative(-0.59f, 0.24f, -1.13f, 0.56f, -1.62f, 0.94f)
                lineToRelative(-2.39f, -0.96f)
                curveToRelative(-0.22f, -0.08f, -0.47f, 0f, -0.59f, 0.22f)
                lineTo(2.74f, 8.87f)
                curveToRelative(-0.12f, 0.21f, -0.08f, 0.47f, 0.12f, 0.61f)
                lineToRelative(2.03f, 1.58f)
                curveToRelative(-0.05f, 0.3f, -0.09f, 0.63f, -0.09f, 0.94f)
                reflectiveCurveToRelative(0.02f, 0.64f, 0.07f, 0.94f)
                lineToRelative(-2.03f, 1.58f)
                curveToRelative(-0.18f, 0.14f, -0.23f, 0.41f, -0.12f, 0.61f)
                lineToRelative(1.92f, 3.32f)
                curveToRelative(0.12f, 0.22f, 0.37f, 0.29f, 0.59f, 0.22f)
                lineToRelative(2.39f, -0.96f)
                curveToRelative(0.5f, 0.38f, 1.03f, 0.7f, 1.62f, 0.94f)
                lineToRelative(0.36f, 2.54f)
                curveToRelative(0.05f, 0.24f, 0.24f, 0.41f, 0.48f, 0.41f)
                horizontalLineToRelative(3.84f)
                curveToRelative(0.24f, 0f, 0.44f, -0.17f, 0.47f, -0.41f)
                lineToRelative(0.36f, -2.54f)
                curveToRelative(0.59f, -0.24f, 1.13f, -0.56f, 1.62f, -0.94f)
                lineToRelative(2.39f, 0.96f)
                curveToRelative(0.22f, 0.08f, 0.47f, 0f, 0.59f, -0.22f)
                lineToRelative(1.92f, -3.32f)
                curveToRelative(0.12f, -0.22f, 0.07f, -0.47f, -0.12f, -0.61f)
                lineToRelative(-2.01f, -1.58f)
                close()
                moveTo(12f, 15.6f)
                curveToRelative(-1.98f, 0f, -3.6f, -1.62f, -3.6f, -3.6f)
                reflectiveCurveToRelative(1.62f, -3.6f, 3.6f, -3.6f)
                reflectiveCurveToRelative(3.6f, 1.62f, 3.6f, 3.6f)
                reflectiveCurveToRelative(-1.62f, 3.6f, -3.6f, 3.6f)
                close()
            }
        }
    }

    val Palette: ImageVector by lazy {
        // Oficjalna ikona Material „Palette” (poprzednia ścieżka miała błędny kontur i nie domykała się poprawnie).
        materialIcon("Palette") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 2f)
                curveTo(6.49f, 2f, 2f, 6.49f, 2f, 12f)
                reflectiveCurveToRelative(4.49f, 10f, 10f, 10f)
                curveToRelative(1.38f, 0f, 2.5f, -1.12f, 2.5f, -2.5f)
                curveToRelative(0f, -0.61f, -0.23f, -1.2f, -0.64f, -1.67f)
                curveToRelative(-0.08f, -0.1f, -0.13f, -0.21f, -0.13f, -0.33f)
                curveToRelative(0f, -0.28f, 0.22f, -0.5f, 0.5f, -0.5f)
                lineTo(16f, 17f)
                curveToRelative(3.31f, 0f, 6f, -2.69f, 6f, -6f)
                curveTo(22f, 6.04f, 17.51f, 2f, 12f, 2f)
                close()
                moveTo(6.5f, 13f)
                curveTo(5.67f, 13f, 5f, 12.33f, 5f, 11.5f)
                reflectiveCurveTo(5.67f, 10f, 6.5f, 10f)
                reflectiveCurveTo(8f, 10.67f, 8f, 11.5f)
                reflectiveCurveTo(7.33f, 13f, 6.5f, 13f)
                close()
                moveTo(9f, 9f)
                curveTo(8.17f, 9f, 7.5f, 8.33f, 7.5f, 7.5f)
                reflectiveCurveTo(8.17f, 6f, 9f, 6f)
                reflectiveCurveToRelative(1.5f, 0.67f, 1.5f, 1.5f)
                reflectiveCurveTo(9.83f, 9f, 9f, 9f)
                close()
                moveTo(15f, 9f)
                curveToRelative(-0.83f, 0f, -1.5f, -0.67f, -1.5f, -1.5f)
                reflectiveCurveTo(14.17f, 6f, 15f, 6f)
                reflectiveCurveToRelative(1.5f, 0.67f, 1.5f, 1.5f)
                reflectiveCurveTo(15.83f, 9f, 15f, 9f)
                close()
                moveTo(18f, 13f)
                curveToRelative(-0.83f, 0f, -1.5f, -0.67f, -1.5f, -1.5f)
                reflectiveCurveTo(17.17f, 10f, 18f, 10f)
                reflectiveCurveToRelative(1.5f, 0.67f, 1.5f, 1.5f)
                reflectiveCurveTo(18.83f, 13f, 18f, 13f)
                close()
            }
        }
    }

    val Visibility: ImageVector by lazy {
        materialIcon("Visibility") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 4.5f)
                curveTo(7f, 4.5f, 2.73f, 7.61f, 1f, 12f)
                curveToRelative(1.73f, 4.39f, 6f, 7.5f, 11f, 7.5f)
                reflectiveCurveToRelative(9.27f, -3.11f, 11f, -7.5f)
                curveToRelative(-1.73f, -4.39f, -6f, -7.5f, -11f, -7.5f)
                close()
                moveTo(12f, 17f)
                curveToRelative(-2.76f, 0f, -5f, -2.24f, -5f, -5f)
                reflectiveCurveToRelative(2.24f, -5f, 5f, -5f)
                reflectiveCurveToRelative(5f, 2.24f, 5f, 5f)
                reflectiveCurveToRelative(-2.24f, 5f, -5f, 5f)
                close()
                moveTo(12f, 9f)
                curveToRelative(-1.66f, 0f, -3f, 1.34f, -3f, 3f)
                reflectiveCurveToRelative(1.34f, 3f, 3f, 3f)
                reflectiveCurveToRelative(3f, -1.34f, 3f, -3f)
                reflectiveCurveToRelative(-1.34f, -3f, -3f, -3f)
                close()
            }
        }
    }

    val VisibilityOff: ImageVector by lazy {
        materialIcon("VisibilityOff") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 7f)
                curveToRelative(2.76f, 0f, 5f, 2.24f, 5f, 5f)
                curveToRelative(0f, 0.65f, -0.13f, 1.26f, -0.36f, 1.83f)
                lineToRelative(2.92f, 2.92f)
                curveToRelative(1.51f, -1.26f, 2.7f, -2.89f, 3.44f, -4.75f)
                curveToRelative(-1.73f, -4.39f, -6f, -7.5f, -11f, -7.5f)
                curveToRelative(-1.4f, 0f, -2.74f, 0.25f, -3.98f, 0.7f)
                lineToRelative(2.16f, 2.16f)
                curveTo(10.74f, 7.13f, 11.35f, 7f, 12f, 7f)
                close()
                moveTo(2f, 4.27f)
                lineToRelative(2.28f, 2.28f)
                lineToRelative(0.46f, 0.46f)
                curveTo(3.08f, 8.3f, 1.78f, 10.02f, 1f, 12f)
                curveToRelative(1.73f, 4.39f, 6f, 7.5f, 11f, 7.5f)
                curveToRelative(1.55f, 0f, 3.03f, -0.3f, 4.38f, -0.84f)
                lineToRelative(0.42f, 0.42f)
                lineTo(19.73f, 22f)
                lineTo(21f, 20.73f)
                lineTo(3.27f, 3f)
                lineTo(2f, 4.27f)
                close()
                moveTo(7.53f, 9.8f)
                lineToRelative(1.55f, 1.55f)
                curveToRelative(-0.05f, 0.21f, -0.08f, 0.43f, -0.08f, 0.65f)
                curveToRelative(0f, 1.66f, 1.34f, 3f, 3f, 3f)
                curveToRelative(0.22f, 0f, 0.44f, -0.03f, 0.65f, -0.08f)
                lineToRelative(1.55f, 1.55f)
                curveToRelative(-0.67f, 0.33f, -1.41f, 0.53f, -2.2f, 0.53f)
                curveToRelative(-2.76f, 0f, -5f, -2.24f, -5f, -5f)
                curveToRelative(0f, -0.79f, 0.2f, -1.53f, 0.53f, -2.2f)
                close()
            }
        }
    }

    // =========================================================================
    // 2. Subject-Specific Material Icons (Filled 24dp)
    // =========================================================================

    /** Programming, Software Engineering, Algorithms, Mobile Apps */
    val Code: ImageVector by lazy {
        materialIcon("Code") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(9.4f, 16.6f)
                lineTo(4.8f, 12f)
                lineToRelative(4.6f, -4.6f)
                lineTo(8f, 6f)
                lineToRelative(-6f, 6f)
                lineToRelative(6f, 6f)
                lineToRelative(1.4f, -1.4f)
                close()
                moveTo(14.6f, 16.6f)
                lineToRelative(4.6f, -4.6f)
                lineToRelative(-4.6f, -4.6f)
                lineTo(16f, 6f)
                lineToRelative(6f, 6f)
                lineToRelative(-6f, 6f)
                lineToRelative(-1.4f, -1.4f)
                close()
            }
        }
    }

    /** Operating Systems, Linux, Windows, System Administration */
    val Computer: ImageVector by lazy {
        materialIcon("Computer") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(20f, 18f)
                curveToRelative(1.1f, 0f, 1.99f, -0.9f, 1.99f, -2f)
                lineTo(22f, 6f)
                curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
                horizontalLineTo(4f)
                curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
                verticalLineToRelative(10f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                horizontalLineTo(0f)
                verticalLineToRelative(2f)
                horizontalLineToRelative(24f)
                verticalLineToRelative(-2f)
                horizontalLineToRelative(-4f)
                close()
                moveTo(4f, 6f)
                horizontalLineToRelative(16f)
                verticalLineToRelative(10f)
                horizontalLineTo(4f)
                verticalLineTo(6f)
                close()
            }
        }
    }

    /** Computer Architecture, Hardware, Embedded Systems, Microprocessors */
    val Memory: ImageVector by lazy {
        materialIcon("Memory") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(15f, 9f)
                horizontalLineTo(9f)
                verticalLineToRelative(6f)
                horizontalLineToRelative(6f)
                verticalLineTo(9f)
                close()
                moveTo(21f, 11f)
                verticalLineTo(9f)
                horizontalLineToRelative(-2f)
                verticalLineTo(7f)
                curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
                horizontalLineToRelative(-2f)
                verticalLineTo(3f)
                horizontalLineToRelative(-2f)
                verticalLineToRelative(2f)
                horizontalLineToRelative(-2f)
                verticalLineTo(3f)
                horizontalLineTo(9f)
                verticalLineToRelative(2f)
                horizontalLineTo(7f)
                curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
                verticalLineToRelative(2f)
                horizontalLineTo(3f)
                verticalLineToRelative(2f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(2f)
                horizontalLineTo(3f)
                verticalLineToRelative(2f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(2f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(2f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(-2f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(2f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(-2f)
                horizontalLineToRelative(2f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                verticalLineToRelative(-2f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(-2f)
                horizontalLineToRelative(-2f)
                verticalLineToRelative(-2f)
                horizontalLineToRelative(2f)
                close()
                moveTo(17f, 17f)
                horizontalLineTo(7f)
                verticalLineTo(7f)
                horizontalLineToRelative(10f)
                verticalLineToRelative(10f)
                close()
            }
        }
    }

    /** Cybersecurity, Cryptography, Penetration Testing, Security */
    val Security: ImageVector by lazy {
        materialIcon("Security") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 1f)
                lineTo(3f, 5f)
                verticalLineToRelative(6f)
                curveToRelative(0f, 5.55f, 3.84f, 10.74f, 9f, 12f)
                curveToRelative(5.16f, -1.26f, 9f, -6.45f, 9f, -12f)
                verticalLineTo(5f)
                lineToRelative(-9f, -4f)
                close()
                moveTo(12f, 11.99f)
                horizontalLineToRelative(7f)
                curveToRelative(-0.53f, 4.12f, -3.28f, 7.79f, -7f, 8.94f)
                verticalLineTo(12f)
                horizontalLineTo(5f)
                verticalLineTo(6.3f)
                lineToRelative(7f, -3.11f)
                verticalLineToRelative(8.8f)
                close()
            }
        }
    }

    /** Cryptography, Encryption, Secrets, Security Keys */
    val Key: ImageVector by lazy {
        materialIcon("Key") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(21f, 10f)
                horizontalLineToRelative(-8.35f)
                curveTo(11.83f, 7.67f, 9.61f, 6f, 7f, 6f)
                curveToRelative(-3.31f, 0f, -6f, 2.69f, -6f, 6f)
                reflectiveCurveToRelative(2.69f, 6f, 6f, 6f)
                curveToRelative(2.61f, 0f, 4.83f, -1.67f, 5.65f, -4f)
                horizontalLineTo(17f)
                verticalLineToRelative(4f)
                horizontalLineToRelative(4f)
                verticalLineToRelative(-4f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(-4f)
                close()
                moveTo(7f, 15f)
                curveToRelative(-1.66f, 0f, -3f, -1.34f, -3f, -3f)
                reflectiveCurveToRelative(1.34f, -3f, 3f, -3f)
                reflectiveCurveToRelative(3f, 1.34f, 3f, 3f)
                reflectiveCurveToRelative(-1.34f, 3f, -3f, 3f)
                close()
            }
        }
    }

    /** Penetration Testing, Bug Bounty, Malware Analysis, Exploits */
    val BugReport: ImageVector by lazy {
        materialIcon("BugReport") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(20f, 8f)
                horizontalLineToRelative(-2.81f)
                curveToRelative(-0.45f, -0.78f, -1.07f, -1.45f, -1.82f, -1.96f)
                lineTo(17f, 4.41f)
                lineTo(15.59f, 3f)
                lineToRelative(-2.17f, 2.17f)
                curveTo(12.96f, 5.06f, 12.49f, 5f, 12f, 5f)
                curveToRelative(-0.49f, 0f, -0.96f, 0.06f, -1.41f, 0.17f)
                lineTo(8.41f, 3f)
                lineTo(7f, 4.41f)
                lineToRelative(1.62f, 1.63f)
                curveTo(7.88f, 6.55f, 7.26f, 7.22f, 6.81f, 8f)
                horizontalLineTo(4f)
                verticalLineToRelative(2f)
                horizontalLineToRelative(2.09f)
                curveToRelative(-0.05f, 0.33f, -0.09f, 0.66f, -0.09f, 1f)
                verticalLineToRelative(1f)
                horizontalLineTo(4f)
                verticalLineToRelative(2f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(1f)
                curveToRelative(0f, 0.34f, 0.04f, 0.67f, 0.09f, 1f)
                horizontalLineTo(4f)
                verticalLineToRelative(2f)
                horizontalLineToRelative(2.81f)
                curveToRelative(1.04f, 1.79f, 2.97f, 3f, 5.19f, 3f)
                reflectiveCurveToRelative(4.15f, -1.21f, 5.19f, -3f)
                horizontalLineTo(20f)
                verticalLineToRelative(-2f)
                horizontalLineToRelative(-2.09f)
                curveToRelative(0.05f, -0.33f, 0.09f, -0.66f, 0.09f, -1f)
                verticalLineToRelative(-1f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(-2f)
                horizontalLineToRelative(-2f)
                verticalLineToRelative(-1f)
                curveToRelative(0f, -0.34f, -0.04f, -0.67f, -0.09f, -1f)
                horizontalLineTo(20f)
                verticalLineTo(8f)
                close()
                moveTo(14f, 16f)
                horizontalLineToRelative(-4f)
                verticalLineToRelative(-2f)
                horizontalLineToRelative(4f)
                verticalLineToRelative(2f)
                close()
                moveTo(14f, 12f)
                horizontalLineToRelative(-4f)
                verticalLineToRelative(-2f)
                horizontalLineToRelative(4f)
                verticalLineToRelative(2f)
                close()
            }
        }
    }

    /** Reverse Engineering, Low-level, Terminal, Command Line */
    val Terminal: ImageVector by lazy {
        materialIcon("Terminal") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(20f, 4f)
                horizontalLineTo(4f)
                curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
                verticalLineToRelative(12f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                horizontalLineToRelative(16f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                verticalLineTo(6f)
                curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
                close()
                moveTo(20f, 18f)
                horizontalLineTo(4f)
                verticalLineTo(8f)
                horizontalLineToRelative(16f)
                verticalLineToRelative(10f)
                close()
                moveTo(7.5f, 17f)
                lineToRelative(-1.41f, -1.41f)
                lineTo(8.67f, 13f)
                lineToRelative(-2.58f, -2.59f)
                lineTo(7.5f, 9f)
                lineToRelative(4f, 4f)
                lineToRelative(-4f, 4f)
                close()
                moveTo(11.5f, 17f)
                horizontalLineToRelative(6f)
                verticalLineToRelative(-2f)
                horizontalLineToRelative(-6f)
                verticalLineToRelative(2f)
                close()
            }
        }
    }

    /** AI Models, Transformers, LLMs, Generative AI */
    val AutoAwesome: ImageVector by lazy {
        materialIcon("AutoAwesome") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(19f, 9f)
                lineToRelative(1.25f, -2.75f)
                lineTo(23f, 5f)
                lineToRelative(-2.75f, -1.25f)
                lineTo(19f, 1f)
                lineToRelative(-1.25f, 2.75f)
                lineTo(15f, 5f)
                lineToRelative(2.75f, 1.25f)
                close()
                moveTo(19f, 15f)
                lineToRelative(-1.25f, 2.75f)
                lineTo(15f, 19f)
                lineToRelative(2.75f, 1.25f)
                lineTo(19f, 23f)
                lineToRelative(1.25f, -2.75f)
                lineTo(23f, 19f)
                lineToRelative(-2.75f, -1.25f)
                close()
                moveTo(11.5f, 9.5f)
                lineTo(9f, 4f)
                lineTo(6.5f, 9.5f)
                lineTo(1f, 12f)
                lineToRelative(5.5f, 2.5f)
                lineTo(9f, 20f)
                lineToRelative(2.5f, -5.5f)
                lineTo(17f, 12f)
                close()
            }
        }
    }

    /** Local Area Networks, Routing, Protocols, Telecommunications */
    val Lan: ImageVector by lazy {
        materialIcon("Lan") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(13f, 22f)
                horizontalLineToRelative(8f)
                verticalLineToRelative(-7f)
                horizontalLineToRelative(-3f)
                verticalLineToRelative(-4f)
                horizontalLineToRelative(-5f)
                verticalLineTo(9f)
                horizontalLineToRelative(2f)
                verticalLineTo(2f)
                horizontalLineTo(9f)
                verticalLineToRelative(7f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(2f)
                horizontalLineTo(6f)
                verticalLineToRelative(4f)
                horizontalLineTo(3f)
                verticalLineToRelative(7f)
                horizontalLineToRelative(8f)
                verticalLineToRelative(-7f)
                horizontalLineTo(8f)
                verticalLineToRelative(-2f)
                horizontalLineToRelative(8f)
                verticalLineToRelative(2f)
                horizontalLineToRelative(-3f)
                verticalLineToRelative(7f)
                close()
            }
        }
    }

    /** DevSecOps, Continuous Integration, Deployment Instructions */
    val IntegrationInstructions: ImageVector by lazy {
        materialIcon("IntegrationInstructions") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(19f, 3f)
                horizontalLineToRelative(-4.18f)
                curveTo(14.4f, 1.84f, 13.3f, 1f, 12f, 1f)
                reflectiveCurveTo(9.6f, 1.84f, 9.18f, 3f)
                horizontalLineTo(5f)
                curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
                verticalLineToRelative(14f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                horizontalLineToRelative(14f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                verticalLineTo(5f)
                curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
                close()
                moveTo(12f, 2.75f)
                curveToRelative(0.41f, 0f, 0.75f, 0.34f, 0.75f, 0.75f)
                reflectiveCurveToRelative(-0.34f, 0.75f, -0.75f, 0.75f)
                reflectiveCurveToRelative(-0.75f, -0.34f, -0.75f, -0.75f)
                reflectiveCurveToRelative(0.34f, -0.75f, 0.75f, -0.75f)
                close()
                moveTo(11f, 14.17f)
                lineToRelative(-1.41f, 1.42f)
                lineTo(6f, 12f)
                lineToRelative(3.59f, -3.59f)
                lineTo(11f, 9.83f)
                lineTo(8.83f, 12f)
                lineTo(11f, 14.17f)
                close()
                moveTo(13f, 14.17f)
                lineTo(15.17f, 12f)
                lineTo(13f, 9.83f)
                lineToRelative(1.41f, -1.42f)
                lineTo(18f, 12f)
                lineToRelative(-3.59f, 3.59f)
                lineTo(13f, 14.17f)
                close()
            }
        }
    }

    /** AI Act, Data Protection, Compliance, Policy */
    val Policy: ImageVector by lazy {
        materialIcon("Policy") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 1f)
                lineTo(3f, 5f)
                verticalLineToRelative(6f)
                curveToRelative(0f, 5.55f, 3.84f, 10.74f, 9f, 12f)
                curveToRelative(5.16f, -1.26f, 9f, -6.45f, 9f, -12f)
                verticalLineTo(5f)
                lineToRelative(-9f, -4f)
                close()
                moveTo(11f, 7f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(6f)
                horizontalLineToRelative(-2f)
                verticalLineTo(7f)
                close()
                moveTo(12f, 17f)
                curveToRelative(-0.69f, 0f, -1.25f, -0.56f, -1.25f, -1.25f)
                reflectiveCurveToRelative(0.56f, -1.25f, 1.25f, -1.25f)
                reflectiveCurveToRelative(1.25f, 0.56f, 1.25f, 1.25f)
                reflectiveCurveTo(12.69f, 17f, 12f, 17f)
                close()
            }
        }
    }

    /** Ethics, Legal Balance, Regulations, Civil Law */
    val Balance: ImageVector by lazy {
        materialIcon("Balance") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 2f)
                horizontalLineToRelative(-1f)
                verticalLineToRelative(3f)
                horizontalLineTo(4f)
                curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
                verticalLineToRelative(1f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(7f)
                curveToRelative(0f, 2.76f, 2.24f, 5f, 5f, 5f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(2f)
                horizontalLineTo(7f)
                verticalLineToRelative(2f)
                horizontalLineToRelative(10f)
                verticalLineToRelative(-2f)
                horizontalLineToRelative(-4f)
                verticalLineToRelative(-2f)
                horizontalLineToRelative(2f)
                curveToRelative(2.76f, 0f, 5f, -2.24f, 5f, -5f)
                verticalLineTo(8f)
                horizontalLineToRelative(2f)
                verticalLineTo(7f)
                curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
                horizontalLineToRelative(-7f)
                verticalLineTo(2f)
                close()
                moveTo(9f, 15f)
                curveToRelative(-1.66f, 0f, -3f, -1.34f, -3f, -3f)
                verticalLineTo(8f)
                horizontalLineToRelative(6f)
                verticalLineToRelative(4f)
                curveToRelative(0f, 1.66f, -1.34f, 3f, -3f, 3f)
                close()
                moveTo(15f, 15f)
                curveToRelative(-1.66f, 0f, -3f, -1.34f, -3f, -3f)
                verticalLineTo(8f)
                horizontalLineToRelative(6f)
                verticalLineToRelative(4f)
                curveToRelative(0f, 1.66f, -1.34f, 3f, -3f, 3f)
                close()
            }
        }
    }

    /** Artificial Intelligence, Neural Networks, Data Science, Psychology */
    val Psychology: ImageVector by lazy {
        materialIcon("Psychology") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(13f, 3f)
                curveTo(9.23f, 3f, 6.19f, 5.95f, 6f, 9.66f)
                lineToRelative(-1.92f, 2.53f)
                curveTo(3.58f, 12.85f, 4.05f, 14f, 4.88f, 14f)
                horizontalLineTo(6f)
                verticalLineToRelative(3f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                horizontalLineToRelative(1f)
                verticalLineToRelative(3f)
                horizontalLineToRelative(7f)
                verticalLineToRelative(-4.68f)
                curveToRelative(2.36f, -1.12f, 4f, -3.53f, 4f, -6.32f)
                curveTo(20f, 6.13f, 16.87f, 3f, 13f, 3f)
                close()
                moveTo(13f, 12f)
                curveToRelative(-1.1f, 0f, -2f, -0.9f, -2f, -2f)
                reflectiveCurveToRelative(0.9f, -2f, 2f, -2f)
                reflectiveCurveToRelative(2f, 0.9f, 2f, 2f)
                reflectiveCurveToRelative(-0.9f, 2f, -2f, 2f)
                close()
            }
        }
    }

    /** Cloud Computing, Computer Networks, DevSecOps, Distributed Systems */
    val Cloud: ImageVector by lazy {
        materialIcon("Cloud") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(19.35f, 10.04f)
                curveTo(18.67f, 6.59f, 15.64f, 4f, 12f, 4f)
                curveTo(9.11f, 4f, 6.6f, 5.64f, 5.35f, 8.04f)
                curveTo(2.34f, 8.36f, 0f, 10.91f, 0f, 14f)
                curveToRelative(0f, 3.31f, 2.69f, 6f, 6f, 6f)
                horizontalLineToRelative(13f)
                curveToRelative(2.76f, 0f, 5f, -2.24f, 5f, -5f)
                curveToRelative(0f, -2.64f, -2.05f, -4.78f, -4.65f, -4.96f)
                close()
            }
        }
    }

    /** Databases, Big Data, Storage Systems */
    val Storage: ImageVector by lazy {
        materialIcon("Storage") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(2f, 20f)
                horizontalLineToRelative(20f)
                verticalLineToRelative(-4f)
                horizontalLineTo(2f)
                verticalLineToRelative(4f)
                close()
                moveTo(4f, 17f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(2f)
                horizontalLineTo(4f)
                verticalLineToRelative(-2f)
                close()
                moveTo(2f, 4f)
                verticalLineToRelative(4f)
                horizontalLineToRelative(20f)
                verticalLineTo(4f)
                horizontalLineTo(2f)
                close()
                moveTo(6f, 7f)
                horizontalLineTo(4f)
                verticalLineTo(5f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(2f)
                close()
                moveTo(2f, 14f)
                horizontalLineToRelative(20f)
                verticalLineToRelative(-4f)
                horizontalLineTo(2f)
                verticalLineToRelative(4f)
                close()
                moveTo(4f, 11f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(2f)
                horizontalLineTo(4f)
                verticalLineToRelative(-2f)
                close()
            }
        }
    }

    /** Entrepreneurship, Management, Business, Practice */
    val BusinessCenter: ImageVector by lazy {
        materialIcon("BusinessCenter") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(10f, 16f)
                verticalLineToRelative(-1f)
                horizontalLineTo(3.01f)
                lineTo(3f, 19f)
                curveToRelative(0f, 1.11f, 0.89f, 2f, 2f, 2f)
                horizontalLineToRelative(14f)
                curveToRelative(1.11f, 0f, 2f, -0.89f, 2f, -2f)
                verticalLineToRelative(-4f)
                horizontalLineToRelative(-7f)
                verticalLineToRelative(1f)
                horizontalLineToRelative(-4f)
                close()
                moveTo(20f, 7f)
                horizontalLineToRelative(-4.01f)
                verticalLineTo(5f)
                lineToRelative(-2f, -2f)
                horizontalLineToRelative(-4f)
                lineToRelative(-2f, 2f)
                verticalLineToRelative(2f)
                horizontalLineTo(4f)
                curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
                verticalLineToRelative(3f)
                curveToRelative(0f, 1.11f, 0.89f, 2f, 2f, 2f)
                horizontalLineToRelative(6f)
                verticalLineToRelative(-2f)
                horizontalLineToRelative(4f)
                verticalLineToRelative(2f)
                horizontalLineToRelative(6f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                verticalLineTo(9f)
                curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
                close()
                moveTo(14f, 7f)
                horizontalLineToRelative(-4f)
                verticalLineTo(5f)
                horizontalLineToRelative(4f)
                verticalLineToRelative(2f)
                close()
            }
        }
    }

    /** Law, GDPR/RODO, Intellectual Property, Regulations */
    val Gavel: ImageVector by lazy {
        materialIcon("Gavel") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(1f, 21f)
                horizontalLineToRelative(12f)
                verticalLineToRelative(2f)
                horizontalLineTo(1f)
                close()
                moveTo(5.24f, 8.07f)
                lineToRelative(2.83f, -2.83f)
                lineToRelative(14.14f, 14.14f)
                lineToRelative(-2.83f, 2.83f)
                close()
                moveTo(12.32f, 1f)
                lineToRelative(5.66f, 5.66f)
                lineToRelative(-2.83f, 2.83f)
                lineToRelative(-5.66f, -5.66f)
                close()
                moveTo(3.83f, 9.48f)
                lineToRelative(5.66f, 5.66f)
                lineToRelative(-2.83f, 2.83f)
                lineTo(1f, 12.31f)
                close()
            }
        }
    }

    /** Mathematics, Statistics, Quantitative Methods */
    val Calculate: ImageVector by lazy {
        materialIcon("Calculate") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(19f, 3f)
                horizontalLineTo(5f)
                curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
                verticalLineToRelative(14f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                horizontalLineToRelative(14f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                verticalLineTo(5f)
                curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
                close()
                moveTo(13.03f, 7.06f)
                lineTo(14.09f, 6f)
                lineToRelative(1.41f, 1.41f)
                lineTo(16.91f, 6f)
                lineToRelative(1.06f, 1.06f)
                lineToRelative(-1.41f, 1.41f)
                lineToRelative(1.41f, 1.41f)
                lineToRelative(-1.06f, 1.06f)
                lineTo(15.5f, 9.54f)
                lineToRelative(-1.41f, 1.41f)
                lineToRelative(-1.06f, -1.06f)
                lineToRelative(1.41f, -1.41f)
                lineToRelative(-1.41f, -1.42f)
                close()
                moveTo(6.25f, 7.72f)
                horizontalLineToRelative(5f)
                verticalLineToRelative(1.5f)
                horizontalLineToRelative(-5f)
                verticalLineToRelative(-1.5f)
                close()
                moveTo(11.5f, 16f)
                horizontalLineToRelative(-2f)
                verticalLineToRelative(2f)
                horizontalLineTo(8f)
                verticalLineToRelative(-2f)
                horizontalLineTo(6f)
                verticalLineToRelative(-1.5f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(-2f)
                horizontalLineToRelative(1.5f)
                verticalLineToRelative(2f)
                horizontalLineToRelative(2f)
                verticalLineTo(16f)
                close()
                moveTo(18f, 17.25f)
                horizontalLineToRelative(-5f)
                verticalLineToRelative(-1.5f)
                horizontalLineToRelative(5f)
                verticalLineToRelative(1.5f)
                close()
                moveTo(18f, 14.75f)
                horizontalLineToRelative(-5f)
                verticalLineToRelative(-1.5f)
                horizontalLineToRelative(5f)
                verticalLineToRelative(1.5f)
                close()
            }
        }
    }

    /**
     * Minimalist Taal Volcano icon: Caldera cone with inner Vulcan Point peak,
     * subtle plume arc above the crater, and crater-lake horizon line at the base.
     */
    val TaalVolcano: ImageVector by lazy {
        materialIcon("TaalVolcano") {
            path(fill = SolidColor(Color.Black)) {
                // Plume arc above caldera (fitted inside circular container)
                moveTo(12f, 4.8f)
                curveTo(10.6f, 4.8f, 9.6f, 5.6f, 9.6f, 6.7f)
                curveTo(9.6f, 7.3f, 10.1f, 7.7f, 10.6f, 7.7f)
                curveTo(11.1f, 7.7f, 11.5f, 7.3f, 12.0f, 7.3f)
                curveTo(12.5f, 7.3f, 12.9f, 7.7f, 13.4f, 7.7f)
                curveTo(13.9f, 7.7f, 14.4f, 7.3f, 14.4f, 6.7f)
                curveTo(14.4f, 5.6f, 13.4f, 4.8f, 12.0f, 4.8f)
                close()
                // Main volcano caldera cone + inner Vulcan Point island peak
                moveTo(8.8f, 9.2f)
                lineTo(4.8f, 16.5f)
                horizontalLineTo(19.2f)
                lineTo(15.2f, 9.2f)
                lineTo(13.2f, 10.9f)
                lineTo(12.0f, 9.4f)
                lineTo(10.8f, 10.9f)
                close()
                // Crater lake horizon line at base
                moveTo(5.0f, 17.8f)
                horizontalLineTo(19.0f)
                verticalLineTo(19.2f)
                horizontalLineTo(5.0f)
                close()
            }
        }
    }

    /**
     * Resolves the best-matching Material Icon for any academic subject and class form,
     * fulfilling the Material 3 Expressive contextual iconography specification.
     */
    fun forSubject(
        subjectName: String,
        classType: ClassType = ClassType.LECTURE
    ): ImageVector {
        val norm = subjectName.lowercase()
        return when {
            // Cyberbezpieczeństwo / Kryptografia / Pentesty / Inżynieria wsteczna
            norm.contains("kryptograf") || norm.contains("krypto") || norm.contains("szyfr") -> Key
            norm.contains("penetrac") || norm.contains("pentest") || norm.contains("hack") || norm.contains("ctf") -> BugReport
            norm.contains("wsteczn") || norm.contains("złośliw") || norm.contains("terminal") || norm.contains("malware") -> Terminal
            norm.contains("bezpiecz") || norm.contains("cyber") || norm.contains("security") -> Security

            // Sztuczna Inteligencja / Modele Językowe / Psychologia
            norm.contains("model") || norm.contains("llm") || norm.contains("transform") || norm.contains("generat") -> AutoAwesome
            norm.contains("sztuczn") || norm.contains("neuron") || norm.contains("uczenie") || norm.contains("psychol") || norm.contains("ai") -> Psychology
            norm.contains("architektura") || norm.contains("mikroproces") || norm.contains("sprzęt") || norm.contains("układ") -> Memory

            // Chmura / DevOps / Sieci Komputerowe
            norm.contains("devsecops") || norm.contains("ci/cd") || norm.contains("integrac") -> IntegrationInstructions
            norm.contains("sieci") || norm.contains("lan") || norm.contains("komunikac") || norm.contains("routing") -> Lan
            norm.contains("chmur") || norm.contains("cloud") || norm.contains("rozprosz") -> Cloud

            // Przedmioty Prawne / Etyka / AI Act / Regulacje
            norm.contains("ai act") || norm.contains("rodo") || norm.contains("ochron") || norm.contains("polityk") -> Policy
            norm.contains("prawo") || norm.contains("etyk") || norm.contains("własnoś") -> Balance
            norm.contains("przepis") || norm.contains("regulac") -> Gavel

            // Programowanie / Bazy Danych / Matematyka / Biznes
            norm.contains("program") || norm.contains("algorytm") || norm.contains("obiekt") ||
                norm.contains("aplikac") || norm.contains("inżynieria oprogramowania") || norm.contains("kod") -> Code
            norm.contains("systemy operacyjne") || norm.contains("linux") || norm.contains("windows") ||
                norm.contains("komputer") -> Computer
            norm.contains("baz") || norm.contains("danych") || norm.contains("sql") || norm.contains("hurtown") -> Storage
            norm.contains("matemat") || norm.contains("statyst") || norm.contains("logik") || norm.contains("ekonometr") -> Calculate
            norm.contains("elektro") || norm.contains("fizyk") || norm.contains("energ") || norm.contains("automatyk") -> Bolt
            norm.contains("przedsiębior") || norm.contains("zarządz") || norm.contains("praktyk") || norm.contains("biznes") || norm.contains("ekonom") -> BusinessCenter

            classType == ClassType.LAB -> Computer
            classType == ClassType.PRACTICE -> BusinessCenter
            else -> School
        }
    }

    /**
     * Renders a crisp monochrome Material Icon [Bitmap] matching [subjectName] and [classType]
     * on a transparent background (or optional container if `containerColorArgb != 0`),
     * matching the clean right-side icon style of Android 16 `ProgressStyle` (Koleo style).
     */
    fun renderSubjectBadgeBitmap(
        subjectName: String,
        classType: ClassType,
        sizePx: Int = 128,
        containerColorArgb: Int = 0x00000000,
        iconColorArgb: Int = 0xFFFFFFFF.toInt()
    ): Bitmap {
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        if ((containerColorArgb ushr 24) != 0) {
            paint.style = Paint.Style.FILL
            paint.color = containerColorArgb
            val pad = sizePx * 0.04f
            val radius = sizePx * 0.34f
            canvas.drawRoundRect(
                RectF(pad, pad, sizePx - pad, sizePx - pad),
                radius,
                radius,
                paint
            )
        }

        val scaleFraction = if ((containerColorArgb ushr 24) == 0) 0.78f else 0.54f
        val iconScale = (sizePx * scaleFraction) / 24f
        val offset = (sizePx - (24f * iconScale)) / 2f
        canvas.save()
        canvas.translate(offset, offset)
        canvas.scale(iconScale, iconScale)

        paint.color = iconColorArgb
        paint.style = Paint.Style.FILL
        val norm = subjectName.lowercase()
        val path = Path()

        when {
            norm.contains("kryptograf") || norm.contains("bezpiecz") || norm.contains("cyber") ||
                norm.contains("penetrac") || norm.contains("złośliw") || norm.contains("wsteczn") -> {
                path.moveTo(12f, 1.5f)
                path.lineTo(3.5f, 5.2f)
                path.lineTo(3.5f, 11f)
                path.cubicTo(3.5f, 16.2f, 7.1f, 21f, 12f, 22.3f)
                path.cubicTo(16.9f, 21f, 20.5f, 16.2f, 20.5f, 11f)
                path.lineTo(20.5f, 5.2f)
                path.close()
                canvas.drawPath(path, paint)
            }
            norm.contains("program") || norm.contains("algorytm") || norm.contains("aplikac") ||
                norm.contains("ai") || norm.contains("neuron") -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 2.4f
                paint.strokeCap = Paint.Cap.ROUND
                paint.strokeJoin = Paint.Join.ROUND
                path.moveTo(8.5f, 7f)
                path.lineTo(3.5f, 12f)
                path.lineTo(8.5f, 17f)
                path.moveTo(15.5f, 7f)
                path.lineTo(20.5f, 12f)
                path.lineTo(15.5f, 17f)
                path.moveTo(13.5f, 5.5f)
                path.lineTo(10.5f, 18.5f)
                canvas.drawPath(path, paint)
            }
            norm.contains("architektura") || norm.contains("mikroproces") || norm.contains("sprzęt") -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 2.1f
                canvas.drawRoundRect(RectF(6f, 6f, 18f, 18f), 2f, 2f, paint)
                paint.style = Paint.Style.FILL
                canvas.drawRect(9.2f, 9.2f, 14.8f, 14.8f, paint)
                canvas.drawRect(8.5f, 2.5f, 10.5f, 5.5f, paint)
                canvas.drawRect(13.5f, 2.5f, 15.5f, 5.5f, paint)
                canvas.drawRect(8.5f, 18.5f, 10.5f, 21.5f, paint)
                canvas.drawRect(13.5f, 18.5f, 15.5f, 21.5f, paint)
                canvas.drawRect(2.5f, 8.5f, 5.5f, 10.5f, paint)
                canvas.drawRect(2.5f, 13.5f, 5.5f, 15.5f, paint)
                canvas.drawRect(18.5f, 8.5f, 21.5f, 10.5f, paint)
                canvas.drawRect(18.5f, 13.5f, 21.5f, 15.5f, paint)
            }
            norm.contains("systemy operacyjne") || norm.contains("linux") || norm.contains("komputer") ||
                classType == ClassType.LAB -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 2.2f
                canvas.drawRoundRect(RectF(3f, 4.5f, 21f, 16.5f), 2f, 2f, paint)
                paint.style = Paint.Style.FILL
                canvas.drawRect(1.5f, 18f, 22.5f, 20.2f, paint)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1.9f
                path.moveTo(7f, 8.5f)
                path.lineTo(10f, 10.5f)
                path.lineTo(7f, 12.5f)
                path.moveTo(11.5f, 12.5f)
                path.lineTo(15f, 12.5f)
                canvas.drawPath(path, paint)
            }
            norm.contains("elektro") || norm.contains("fizyk") || norm.contains("energ") -> {
                path.moveTo(13.2f, 2.5f)
                path.lineTo(6.2f, 13.2f)
                path.lineTo(11.5f, 13.2f)
                path.lineTo(10.2f, 21.5f)
                path.lineTo(17.8f, 10.8f)
                path.lineTo(12.5f, 10.8f)
                path.close()
                canvas.drawPath(path, paint)
            }
            norm.contains("przedsiębior") || norm.contains("zarządz") || norm.contains("praktyk") ||
                norm.contains("biznes") || norm.contains("prawo") || norm.contains("rodo") -> {
                canvas.drawRoundRect(RectF(3f, 7.5f, 21f, 19.5f), 2.2f, 2.2f, paint)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 2.2f
                canvas.drawRoundRect(RectF(8.5f, 4f, 15.5f, 7.8f), 1.5f, 1.5f, paint)
            }
            else -> {
                path.moveTo(12f, 3f)
                path.lineTo(1.5f, 8.8f)
                path.lineTo(12f, 14.6f)
                path.lineTo(22.5f, 8.8f)
                path.close()
                path.moveTo(5.5f, 12.8f)
                path.lineTo(5.5f, 17.2f)
                path.lineTo(12f, 20.8f)
                path.lineTo(18.5f, 17.2f)
                path.lineTo(18.5f, 12.8f)
                path.lineTo(12f, 16.4f)
                path.close()
                canvas.drawPath(path, paint)
            }
        }

        canvas.restore()
        return bmp
    }

    /**
     * Renders the Koleo-style circular progress tracker thumb (`setProgressTrackerIcon`)
     * for Android 16 `Notification.ProgressStyle`:
     * - Outer white circle border
     * - Inner vibrant blue circle (`#0077FF`)
     * - Crisp white right-chevron (`>`) inside
     */
    fun renderProgressTrackerThumbBitmap(
        sizePx: Int = 72,
        circleColorArgb: Int = 0xFF0077FF.toInt()
    ): Bitmap {
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val center = sizePx / 2f

        // 1. Outer white ring
        paint.style = Paint.Style.FILL
        paint.color = 0xFFFFFFFF.toInt()
        canvas.drawCircle(center, center, sizePx * 0.46f, paint)

        // 2. Inner blue circle
        paint.color = circleColorArgb
        canvas.drawCircle(center, center, sizePx * 0.36f, paint)

        // 3. White chevron `>`
        paint.style = Paint.Style.STROKE
        paint.color = 0xFFFFFFFF.toInt()
        paint.strokeWidth = sizePx * 0.085f
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND

        val path = Path().apply {
            moveTo(center - sizePx * 0.07f, center - sizePx * 0.14f)
            lineTo(center + sizePx * 0.09f, center)
            lineTo(center - sizePx * 0.07f, center + sizePx * 0.14f)
        }
        canvas.drawPath(path, paint)
        return bmp
    }

    private inline fun materialIcon(
        name: String,
        block: ImageVector.Builder.() -> Unit
    ): ImageVector = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply(block).build()
}
