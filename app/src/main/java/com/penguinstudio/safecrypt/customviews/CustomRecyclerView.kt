package com.penguinstudio.safecrypt.customviews

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.TransitionManager
import com.penguinstudio.safecrypt.R
import com.penguinstudio.safecrypt.adapters.AlbumMediaAdapter
import kotlin.math.abs
enum class ScaleFactor {
    SCALE_EXPANDING, SCALE_SHRINKING
}
class CustomRecyclerView(context: Context, attrs: AttributeSet ) : RecyclerView(context, attrs) {

    private val mScaleDetector = ScaleGestureDetector(context, PinchListener(object : PinchListener.PinchListenerResult {
        override fun onResult(scale: ScaleFactor) {
            setGridColumnPreferences(scale)
            isClickable = false
        }

        override fun onScaleBegin() {
            isClickable = false
        }
    }))


    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.pointerCount > 1) {
            suppressLayout(true)
        }
        else {
            suppressLayout(false)
        }
        return super.onTouchEvent(e)
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        super.dispatchTouchEvent(ev)
        return mScaleDetector.onTouchEvent(ev)
    }

    fun setGridColumnPreferences(scaling: ScaleFactor) {
        val sharedPref = context.getSharedPreferences(context.getString(R.string.main_shared_pref), Context.MODE_PRIVATE)
        var currentColumns =
            sharedPref.getInt(context.getString(R.string.grid_columns), 3)


        when(scaling) {
            ScaleFactor.SCALE_EXPANDING -> {
                if(currentColumns == 5) return

                with (sharedPref.edit()) {
                    putInt(context.getString(R.string.grid_columns), ++currentColumns)
                    apply()
                }
            }
            ScaleFactor.SCALE_SHRINKING -> {
                if(currentColumns == 3) return
                with (sharedPref.edit()) {
                    putInt(context.getString(R.string.grid_columns), --currentColumns)
                    apply()
                }
            }
        }

        // Prevent redrawing recyclerview if current column is same size as requested
        animateRecyclerLayoutChange(currentColumns)
    }

    private fun animateRecyclerLayoutChange(layoutSpanCount: Int) {
        val lm = layoutManager as GridLayoutManager
        post {
            TransitionManager.beginDelayedTransition(this)
            lm.spanCount = layoutSpanCount
        }
    }



    internal class PinchListener(private val listener: PinchListenerResult) : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        interface PinchListenerResult {
            fun onResult(scale: ScaleFactor)
            fun onScaleBegin()
        }

        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            listener.onScaleBegin()
            return super.onScaleBegin(detector)
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            if (gestureTolerance(detector)) {

                when {
                    detector.scaleFactor < 1F -> {
                        listener.onResult(ScaleFactor.SCALE_EXPANDING)
                    }
                    detector.scaleFactor > 1F -> {
                        listener.onResult(ScaleFactor.SCALE_SHRINKING)
                    }
                    else -> {
                        Log.d("tag","Should not move")
                    }
                }
            }

        }

        private fun gestureTolerance(detector: ScaleGestureDetector): Boolean {
            val slop = 7
            val spanDelta = abs(detector.currentSpan - detector.previousSpan)
            return spanDelta > slop
        }
    }
}