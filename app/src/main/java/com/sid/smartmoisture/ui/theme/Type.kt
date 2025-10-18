package com.sid.smartmoisture.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.sid.smartmoisture.R

private val font = FontFamily(Font(R.font.inter))
val Typography = Typography(
    bodyLarge = Typography().bodyLarge.copy(fontFamily = font),
    bodyMedium = Typography().bodyMedium.copy(fontFamily = font),
    bodySmall = Typography().bodySmall.copy(fontFamily = font),
    labelLarge = Typography().labelLarge.copy(fontFamily = font),
    labelMedium = Typography().labelMedium.copy(fontFamily = font),
    labelSmall = Typography().labelSmall.copy(fontFamily = font),
    titleLarge = Typography().titleLarge.copy(fontFamily = font),
    titleMedium = Typography().titleMedium.copy(fontFamily = font),
    titleSmall = Typography().titleSmall.copy(fontFamily = font),
    headlineLarge = Typography().headlineLarge.copy(fontFamily = font),
    headlineMedium = Typography().headlineMedium.copy(fontFamily = font),
    headlineSmall = Typography().headlineSmall.copy(fontFamily = font),
    displayLarge = Typography().displayLarge.copy(fontFamily = font),
    displayMedium = Typography().displayMedium.copy(fontFamily = font),
    displaySmall = Typography().displaySmall.copy(fontFamily = font)
)
