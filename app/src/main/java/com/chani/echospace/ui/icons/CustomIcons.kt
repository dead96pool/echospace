package com.chani.echospace.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object CustomIcons {

    val FormTextbox: ImageVector = ImageVector.Builder(
        name = "FormTextbox",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = SolidColor(Color.Black),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 0.2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            strokeLineMiter = 2f,
            pathFillType = PathFillType.NonZero
        ) {
            // SVG path data converted to Compose commands
            moveTo(17f, 7f)
            horizontalLineTo(22f)
            verticalLineTo(17f)
            horizontalLineTo(17f)
            verticalLineTo(19f)
            arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = false, 18f, 20f)
            horizontalLineTo(20f)
            verticalLineTo(22f)
            horizontalLineTo(17.5f)
            curveTo(16.95f, 22f, 16f, 21.55f, 16f, 21f)
            curveTo(16f, 21.55f, 15.05f, 22f, 14.5f, 22f)
            horizontalLineTo(12f)
            verticalLineTo(20f)
            horizontalLineTo(14f)
            arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = false, 15f, 19f)
            verticalLineTo(5f)
            arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = false, 14f, 4f)
            horizontalLineTo(12f)
            verticalLineTo(2f)
            horizontalLineTo(14.5f)
            curveTo(15.05f, 2f, 16f, 2.45f, 16f, 3f)
            curveTo(16f, 2.45f, 16.95f, 2f, 17.5f, 2f)
            horizontalLineTo(20f)
            verticalLineTo(4f)
            horizontalLineTo(18f)
            arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = false, 17f, 5f)
            verticalLineTo(7f)
            moveTo(2f, 7f)
            horizontalLineTo(13f)
            verticalLineTo(9f)
            horizontalLineTo(4f)
            verticalLineTo(15f)
            horizontalLineTo(13f)
            verticalLineTo(17f)
            horizontalLineTo(2f)
            verticalLineTo(7f)
            moveTo(20f, 15f)
            verticalLineTo(9f)
            horizontalLineTo(17f)
            verticalLineTo(15f)
            horizontalLineTo(20f)
            close()
        }
    }.build()

    val UnknownDocument: ImageVector = ImageVector.Builder(
        name = "UnknownDocument",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 960f,  // From your XML
        viewportHeight = 960f   // From your XML
    ).apply {
        path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1.0f,
            stroke = null,
            strokeAlpha = 1.0f,
            strokeLineWidth = 1.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 1.0f,
            pathFillType = PathFillType.NonZero
        ) {
            // ✅ Your path data converted to Compose commands
            moveTo(200f, 160f)
            lineTo(200f, 360f)
            lineTo(200f, 360f)
            lineTo(200f, 400f)
            quadToRelative(0f, 0f, 0f, 0f)
            quadToRelative(0f, 0f, 0f, 0f)
            quadToRelative(0f, 0f, 0f, 82f)
            quadToRelative(0f, 82f, 0f, 198f)
            quadToRelative(0f, 32f, 0f, 62.5f)
            quadToRelative(0f, 30.5f, 0f, 57.5f)
            lineTo(200f, 800f)
            quadToRelative(0f, 0f, 0f, 0f)
            quadToRelative(0f, 0f, 0f, 0f)
            lineTo(200f, 160f)
            quadToRelative(0f, 0f, 0f, 0f)
            quadToRelative(0f, 0f, 0f, 0f)
            lineTo(200f, 160f)
            lineTo(200f, 360f)
            lineTo(200f, 360f)
            lineTo(200f, 160f)
            close()

            moveTo(280f, 560f)
            lineTo(427f, 560f)
            quadToRelative(11f, -23f, 25.5f, -43f)
            quadToRelative(14.5f, -20f, 32.5f, -37f)
            lineTo(280f, 480f)
            lineTo(280f, 560f)
            close()

            moveTo(280f, 720f)
            lineTo(403f, 720f)
            quadToRelative(-3f, -20f, -3f, -40f)
            quadToRelative(0f, -20f, 3f, -40f)
            lineTo(280f, 640f)
            lineTo(280f, 720f)
            close()

            moveTo(200f, 880f)
            quadToRelative(-33f, 0f, -56.5f, -23.5f)
            quadToRelative(-23.5f, -23.5f, -23.5f, -56.5f)
            lineTo(120f, 160f)
            quadToRelative(0f, -33f, 23.5f, -56.5f)
            quadToRelative(23.5f, -23.5f, 56.5f, -23.5f)
            lineTo(520f, 80f)
            lineTo(760f, 320f)
            lineTo(760f, 412f)
            quadToRelative(-19f, -6f, -39f, -9f)
            quadToRelative(-20f, -3f, -41f, -3f)
            lineTo(680f, 360f)
            lineTo(480f, 360f)
            lineTo(480f, 160f)
            lineTo(200f, 160f)
            quadToRelative(0f, 0f, 0f, 0f)
            quadToRelative(0f, 0f, 0f, 0f)
            lineTo(200f, 800f)
            quadToRelative(0f, 0f, 0f, 0f)
            quadToRelative(0f, 0f, 0f, 0f)
            lineTo(427f, 800f)
            quadToRelative(11f, 23f, 25.5f, 43f)
            quadToRelative(14.5f, 20f, 32.5f, 37f)
            lineTo(200f, 880f)
            close()

            moveTo(680f, 480f)
            quadToRelative(83f, 0f, 141.5f, 58.5f)
            quadToRelative(58.5f, 58.5f, 58.5f, 141.5f)
            quadToRelative(0f, 83f, -58.5f, 141.5f)
            quadToRelative(-58.5f, 58.5f, -141.5f, 58.5f)
            quadToRelative(-83f, 0f, -141.5f, -58.5f)
            quadToRelative(-58.5f, -58.5f, -58.5f, -141.5f)
            quadToRelative(0f, -83f, 58.5f, -141.5f)
            quadToRelative(58.5f, -58.5f, 141.5f, -58.5f)
            close()

            moveTo(680f, 800f)
            quadToRelative(11f, 0f, 18.5f, -7.5f)
            quadToRelative(7.5f, -7.5f, 7.5f, -18.5f)
            quadToRelative(0f, -11f, -7.5f, -18.5f)
            quadToRelative(-7.5f, -7.5f, -18.5f, -7.5f)
            quadToRelative(-11f, 0f, -18.5f, 7.5f)
            quadToRelative(-7.5f, 7.5f, -7.5f, 18.5f)
            quadToRelative(0f, 11f, 7.5f, 18.5f)
            quadToRelative(7.5f, 7.5f, 18.5f, 7.5f)
            close()

            moveTo(662f, 724f)
            lineTo(698f, 724f)
            lineTo(698f, 714f)
            quadToRelative(0f, -11f, 6f, -19.5f)
            quadToRelative(6f, -8.5f, 14f, -16.5f)
            quadToRelative(14f, -12f, 22f, -23f)
            quadToRelative(8f, -11f, 8f, -31f)
            quadToRelative(0f, -29f, -19f, -46.5f)
            quadToRelative(-19f, -17.5f, -49f, -17.5f)
            quadToRelative(-23f, 0f, -41.5f, 13.5f)
            quadToRelative(-18.5f, 13.5f, -26.5f, 36.5f)
            lineTo(644f, 624f)
            quadToRelative(3f, -12f, 12.5f, -21f)
            quadToRelative(9.5f, -9f, 23.5f, -9f)
            quadToRelative(15f, 0f, 23.5f, 7.5f)
            quadToRelative(8.5f, 7.5f, 8.5f, 22.5f)
            quadToRelative(0f, 11f, -6f, 18.5f)
            quadToRelative(-6f, 7.5f, -14f, 15.5f)
            quadToRelative(-6f, 6f, -12.5f, 12f)
            quadToRelative(-6.5f, 6f, -11.5f, 14f)
            quadToRelative(-3f, 6f, -4.5f, 12f)
            quadToRelative(-1.5f, 6f, -1.5f, 14f)
            lineTo(662f, 724f)
            close()
        }
    }.build()
}
