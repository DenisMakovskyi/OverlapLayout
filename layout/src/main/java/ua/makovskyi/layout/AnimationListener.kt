package ua.makovskyi.layout

import android.animation.Animator

/**
 * @author Denis Makovskyi
 */

internal open class AnimationListener: Animator.AnimatorListener {

    override fun onAnimationStart(animation: Animator?) {}

    override fun onAnimationEnd(animation: Animator?) {}

    override fun onAnimationCancel(animation: Animator?) {}

    override fun onAnimationRepeat(animation: Animator?) {}
}