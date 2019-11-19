package ua.makovskyi.layout

import android.view.View
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable

import androidx.annotation.ColorInt

/**
 * @author Denis Makovskyi
 */

internal fun createRectDrawable(
    @ColorInt color: Int,
    cornerRadius: Float
): Drawable = GradientDrawable().apply {
    shape = GradientDrawable.RECTANGLE
    cornerRadii = FloatArray(8) { cornerRadius }
    setColor(color)
}

internal fun setViewBackground(
    view: View,
    drawable: Drawable
) {
    view.background = drawable
}