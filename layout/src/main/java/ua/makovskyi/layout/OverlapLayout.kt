package ua.makovskyi.layout

import java.util.concurrent.TimeUnit

import android.os.Parcel
import android.os.Parcelable
import android.content.Context
import android.view.View
import android.view.Gravity
import android.graphics.Color
import android.graphics.Canvas
import android.animation.Animator
import android.animation.ValueAnimator
import android.widget.FrameLayout
import android.util.AttributeSet

import androidx.annotation.AttrRes

/**
 * @author Denis Makovskyi
 */

open class OverlapLayout(
    context: Context,
    attributes: AttributeSet?,
    @AttrRes defStyleAttr: Int
) : FrameLayout(context, attributes, defStyleAttr) {

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

    open class OverlapListener {

        open fun onOverlapStarted() {}

        open fun onOverlapCompleted() {}

        open fun onOverlapCancelled() {}
    }

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
        var overlapWidth = 0
        var overlapHeight = 0
        var remainingTime = 0L

        constructor(source: Parcel) : super(source) {
            isOverlapped = source.readByte().toInt() != 0
            overlapWidth = source.readInt()
            overlapHeight = source.readInt()
            source.readString()?.let {
                remainingTime = it.toLong()
            }
        }

        constructor(superState: Parcelable?) : super(superState)

        override fun writeToParcel(out: Parcel?, flags: Int) {
            super.writeToParcel(out, flags)
            out?.writeByte((if (isOverlapped) 1 else 0).toByte())
            out?.writeInt(overlapWidth)
            out?.writeInt(overlapHeight)
            out?.writeString(remainingTime.toString())
        }
    }

    companion object {

        private const val VIEW_TAG_OVERLAP = "view_overlap"
    }

    constructor(context: Context) : this(context, null, 0)

    constructor(context: Context, attributes: AttributeSet?) : this(context, attributes, 0)

    var overlapListener: OverlapListener? = null

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
    private var overlapWidth = 0
    private var overlapHeight = 0
    private var remainingTime = 0L

    private lateinit var overlapView: View

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
                    1 -> Gravity.START
                    2 -> Gravity.END
                    3 -> Gravity.BOTTOM
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
            it.overlapWidth = overlapWidth
            it.overlapHeight = overlapHeight
            it.remainingTime = remainingTime
        }
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is SavedState) {
            isOverlapped = state.isOverlapped
            overlapWidth = state.overlapWidth
            overlapHeight = state.overlapHeight
            remainingTime = state.remainingTime
        }
        super.onRestoreInstanceState(state)
    }

    private fun applyParameters() {
        if (!isSupportPadding) {
            setPadding(0, 0, 0, 0)
        }
        evaluateDisabledState()
        setViewBackground(this, createRectDrawable(bgColor, cornerRadius))
    }

    private fun createOverlap() {
        if(!::overlapView.isInitialized) {
            overlapView = View(context).also {
                it.tag = VIEW_TAG_OVERLAP
                it.layoutParams = when(overlapGravity) {
                    Gravity.TOP, Gravity.BOTTOM -> {
                        LayoutParams(measuredWidth, overlapHeight).apply {
                            gravity = overlapGravity
                        }
                    }
                    Gravity.START, Gravity.END -> {
                        LayoutParams(overlapWidth, measuredHeight).apply {
                            gravity = overlapGravity
                        }
                    }
                    else -> throw IllegalArgumentException("Unsupported gravity")
                }
                setViewBackground(it, createRectDrawable(overlapColor, cornerRadius))
                when(overlapPosition) {
                    OverlapPosition.BACK -> addView(it, 0)
                    OverlapPosition.FRONT -> addView(it)
                }
            }

        } else {
            when(overlapGravity) {
                Gravity.TOP, Gravity.BOTTOM -> overlapView.updateWidth(measuredWidth)
                Gravity.START, Gravity.END -> overlapView.updateHeight(measuredHeight)
                else -> throw IllegalArgumentException("Unsupported gravity")
            }
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
            val valueAnimator = when(overlapGravity) {
                Gravity.TOP, Gravity.BOTTOM -> ValueAnimator.ofInt(overlapView.measuredHeight, measuredHeight)
                Gravity.START, Gravity.END -> ValueAnimator.ofInt(overlapView.measuredWidth, measuredWidth)
                else -> throw IllegalArgumentException("Unsupported gravity")
            }
            valueAnimator.addUpdateListener { animation ->
                when(overlapGravity) {
                    Gravity.TOP, Gravity.BOTTOM -> {
                        overlapHeight = animation.animatedValue as Int
                        overlapView.updateHeight(overlapHeight)
                    }
                    Gravity.START, Gravity.END -> {
                        overlapWidth = animation.animatedValue as Int
                        overlapView.updateWidth(overlapWidth)
                    }
                    else -> throw IllegalArgumentException("Unsupported gravity")
                }
                remainingTime = animation.currentPlayTime
            }
            valueAnimator.addListener(object : AnimationListener() {
                override fun onAnimationStart(animation: Animator?, isReverse: Boolean) {
                    super.onAnimationStart(animation, isReverse)
                    isOverlapped = false
                    isOverlapping = true
                    overlapListener?.onOverlapStarted()
                }

                override fun onAnimationEnd(animation: Animator?) {
                    super.onAnimationEnd(animation)
                    isOverlapped = true
                    isOverlapping = false
                    overlapListener?.onOverlapCompleted()
                    evaluateEnabledState()
                }

                override fun onAnimationCancel(animation: Animator?) {
                    super.onAnimationCancel(animation)
                    isOverlapped = true
                    isOverlapping = false
                    overlapListener?.onOverlapCancelled()
                    evaluateEnabledState()
                }
            })
            valueAnimator.duration = duration
            valueAnimator.start()
        }
    }

    private fun animatedOverlap() {
        when(overlapGravity) {
            Gravity.TOP, Gravity.BOTTOM -> overlapView.updateHeight(measuredHeight)
            Gravity.START, Gravity.END -> overlapView.updateWidth(measuredWidth)
            else -> throw IllegalArgumentException("Unsupported gravity")
        }
        evaluateEnabledState()
    }

    private fun evaluateEnabledState() {
        if (isEnabledAfterOverlapping) {
            if (!isEnabled) {
                isEnabled = true
            }
        }
    }

    private fun evaluateDisabledState() {
        if (isEnabledAfterOverlapping) {
            if (isEnabled) {
                isEnabled = false
            }
        }
    }
}