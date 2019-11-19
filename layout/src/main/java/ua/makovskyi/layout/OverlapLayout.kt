package ua.makovskyi.layout

import java.util.concurrent.TimeUnit

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout

import androidx.annotation.AttrRes

/**
 * @author Denis Makovskyi
 */

open class OverlapLayout(
    context: Context,
    attributes: AttributeSet?,
    @AttrRes defStyleAttr: Int
) : FrameLayout(context, attributes, defStyleAttr) {

    internal class SavedState : BaseSavedState {

        companion object {
            @JvmField
            val CREATOR = object : Parcelable.Creator<SavedState> {

                override fun createFromParcel(parcel: Parcel): SavedState {
                    return SavedState(parcel)
                }

                override fun newArray(size: Int): Array<SavedState?> {
                    return arrayOfNulls(size)
                }
            }
        }

        var isOverlapped = false
        var overlapHeight = 0
        var remainingTime = 0L

        constructor(source: Parcel) : super(source) {
            isOverlapped = source.readByte().toInt() != 0
            overlapHeight = source.readInt()
            source.readString()?.let {
                remainingTime = it.toLong()
            }
        }

        constructor(superState: Parcelable?) : super(superState)

        override fun writeToParcel(out: Parcel?, flags: Int) {
            super.writeToParcel(out, flags)
            out?.writeByte((if (isOverlapped) 1 else 0).toByte())
            out?.writeInt(overlapHeight)
            out?.writeString(remainingTime.toString())
        }
    }

    enum class OverlapPosition(val value: Int) {
        BACK(0),
        FRONT(1);

        companion object {
            private val map = values().associateBy(OverlapPosition::value)

            fun fromInt(value: Int): OverlapPosition = requireNotNull(map[value]) {
                "Unable to create enum from integer."
            }
        }
    }

    companion object {

        private const val VIEW_TAG_OVERLAP = "view_overlap"
    }

    private var isSupportPadding = true
    private var isEnabledAfterOverlapping = true

    private var bgColor = Color.TRANSPARENT
    private var cornerRadius = 0.0F

    private var overlapColor = Color.TRANSPARENT
    private var overlapGravity = Gravity.BOTTOM
    private var overlapPosition = OverlapPosition.BACK
    private var overlapDuration = TimeUnit.SECONDS.toMillis(5)

    private var isOverlapped = false
    private var isOverlapping = false
    private var overlapHeight = 0
    private var remainingTime = 0L

    private lateinit var overlapView: View

    constructor(context: Context) : this(context, null, 0)

    constructor(context: Context, attributes: AttributeSet?) : this(context, attributes, 0)

    init {
        if (attributes != null) {
            context.obtainStyledAttributes(attributes, R.styleable.OverlapLayout).apply {
                isSupportPadding = getBoolean(R.styleable.OverlapLayout_ol_supportPadding, isSupportPadding)
                isEnabledAfterOverlapping = getBoolean(R.styleable.OverlapLayout_ol_enabledAfterOverlapping, isEnabledAfterOverlapping)
                //-
                bgColor = getColor(R.styleable.OverlapLayout_ol_backgroundColor, bgColor)
                cornerRadius = getDimensionPixelSize(R.styleable.OverlapLayout_ol_cornerRadius, cornerRadius.toInt()).toFloat()
                //-
                overlapColor = getColor(R.styleable.OverlapLayout_ol_overlapColor, overlapColor)
                overlapGravity = when(getInt(R.styleable.OverlapLayout_ol_overlapGravity, overlapGravity)) {
                    0 -> Gravity.TOP
                    1 -> Gravity.BOTTOM
                    else -> throw IllegalArgumentException("Unsupported gravity")
                }
                overlapPosition = OverlapPosition.fromInt(getInt(R.styleable.OverlapLayout_ol_overlapPosition, overlapPosition.value))
                overlapDuration = getInt(R.styleable.OverlapLayout_ol_overlapDuration, overlapDuration.toInt()).toLong()
                recycle()
            }
        }
        applyParameters()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        createOverlap()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        startAnimation()
    }

    override fun onSaveInstanceState(): Parcelable? {
        return SavedState(super.onSaveInstanceState()).also {
            it.isOverlapped = isOverlapped
            it.overlapHeight = overlapHeight
            it.remainingTime = remainingTime
        }
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is SavedState) {
            isOverlapped = state.isOverlapped
            overlapHeight = state.overlapHeight
            remainingTime = state.remainingTime
        }
        super.onRestoreInstanceState(state)
    }

    private fun applyParameters() {
        if (!isSupportPadding) {
            setPadding(0, 0, 0, 0)
        }
        evaluateEnabledState()
        setViewBackground(this, createRectDrawable(bgColor, cornerRadius))
    }

    private fun createOverlap() {
        if(!::overlapView.isInitialized) {
            overlapView = View(context).also {
                it.tag = VIEW_TAG_OVERLAP
                it.layoutParams = LayoutParams(measuredWidth, overlapHeight).apply {
                    gravity = overlapGravity
                }
                setViewBackground(it, createRectDrawable(overlapColor, cornerRadius))
                when(overlapPosition) {
                    OverlapPosition.BACK -> addView(it, 0)
                    OverlapPosition.FRONT -> addView(it)
                }
            }

        } else {
            overlapView.updateWidth(measuredWidth)
        }
    }

    private fun startAnimation() {
        if (!isOverlapped) {
            if (remainingTime > 0L) {
                animateOverlap(remainingTime)
            } else {
                animateOverlap(overlapDuration)
            }

        } else {
            animatedOverlap()
        }
    }

    private fun animateOverlap(duration: Long) {
        if (!isOverlapped && !isOverlapping) {
            ValueAnimator.ofInt(overlapView.measuredHeight, measuredHeight).also {
                it.addUpdateListener { animation ->
                    overlapHeight = animation.animatedValue as Int
                    remainingTime = animation.currentPlayTime
                    overlapView.updateHeight(overlapHeight)
                }
                it.addListener(object : AnimationListener() {
                    override fun onAnimationStart(animation: Animator?, isReverse: Boolean) {
                        super.onAnimationStart(animation, isReverse)
                        isOverlapped = false
                        isOverlapping = true
                    }

                    override fun onAnimationEnd(animation: Animator?) {
                        super.onAnimationEnd(animation)
                        isOverlapped = true
                        isOverlapping = false
                        evaluateEnabledState()
                    }

                    override fun onAnimationCancel(animation: Animator?) {
                        super.onAnimationCancel(animation)
                        isOverlapped = true
                        isOverlapping = false
                        evaluateEnabledState()
                    }
                })
                it.duration = duration
                it.start()
            }
        }
    }

    private fun animatedOverlap() {
        overlapView.updateHeight(measuredHeight)
        evaluateEnabledState()
    }

    private fun evaluateEnabledState() {
        if (isEnabledAfterOverlapping) {
            if (isEnabled) {
                isEnabled = false

            } else if (!isEnabled) {
                isEnabled = true
            }
        }
    }
}