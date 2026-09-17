package com.nukirk.medrx.ui.theme

import androidx.compose.ui.graphics.Color

fun Color.darken(factor: Float = 0.7f): Color {
    return Color(
        red = this.red * factor,
        green = this.green * factor,
        blue = this.blue * factor,
        alpha = this.alpha
    )
}