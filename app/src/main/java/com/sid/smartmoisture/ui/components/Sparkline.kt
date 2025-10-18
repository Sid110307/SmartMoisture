package com.sid.smartmoisture.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun Sparkline(values: List<Double>, modifier: Modifier) {
    val bg = MaterialTheme.colorScheme.surface
    val fg = MaterialTheme.colorScheme.primary

    Canvas(
        modifier
            .background(bg)
            .height(80.dp)
            .fillMaxWidth()
    ) {
        if (values.isEmpty()) return@Canvas

        val max = values.max()
        val min = values.min()
        val range = (max - min).takeIf { it != 0.0 } ?: 1.0

        val stepX = size.width / (values.size - 1).coerceAtLeast(1)
        val path = Path()
        values.forEachIndexed { i, v ->
            val x = i * stepX
            val y = size.height - ((v - min) / range * size.height).toFloat()

            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(path, color = fg, style = Stroke(width = 4f))
        drawPath(path, color = fg.copy(alpha = 0.25f), style = Stroke(width = 12f))
    }
}

@Preview
@Composable
fun SparklinePreview() {
    Sparkline(values = listOf(3.0, 5.0, 2.0, 8.0, 6.0, 7.0, 4.0, 9.0, 5.0), Modifier.fillMaxWidth())
}
