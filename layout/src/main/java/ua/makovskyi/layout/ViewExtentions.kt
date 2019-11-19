package ua.makovskyi.layout

import android.view.View

/**
 * @author Denis Makovskyi
 */

internal fun View.updateWidth(width: Int) {
    layoutParams = layoutParams.also {
        it.width = width
    }
    invalidate()
}

internal fun View.updateHeight(height: Int) {
    layoutParams = layoutParams.also {
        it.height = height
    }
    invalidate()
}