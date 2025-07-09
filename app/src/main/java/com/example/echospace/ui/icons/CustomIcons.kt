package com.example.echospace.ui.icons

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

}
