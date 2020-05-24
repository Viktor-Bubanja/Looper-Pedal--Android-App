package com.example.looper

import android.animation.ValueAnimator.*
import android.view.animation.LinearInterpolator
import android.widget.ImageButton

fun clickAnimation(element: ImageButton) {
    val duration = 100L
    val valueAnimator = ofFloat(1.0F, 1.2F, 1.0F)
    valueAnimator.addUpdateListener {
        val animationValue = it.animatedValue as Float
        element.scaleX = animationValue
        element.scaleY = animationValue
    }
    valueAnimator.interpolator = LinearInterpolator()
    valueAnimator.duration = duration
    valueAnimator.start()
}