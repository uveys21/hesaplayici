package com.example.hesaplayici

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private lateinit var deviceIcon: ImageView
    private lateinit var minusIcon: ImageView
    private lateinit var multiplayIcon: ImageView
    private lateinit var plusIcon: ImageView

    private val animationDuration = 500L
    private val delayBetweenAnimations = 500L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        deviceIcon = findViewById(R.id.device_icon)
        minusIcon = findViewById(R.id.minus_icon)
        multiplayIcon = findViewById(R.id.multiplay_icon)
        plusIcon = findViewById(R.id.plus_icon)

        startAnimationSequence()
    }

    private fun startAnimationSequence() {
        val handler = Handler(Looper.getMainLooper())

        val animationSequence = listOf(
            Pair(deviceIcon, delayBetweenAnimations),
            Pair(minusIcon, delayBetweenAnimations * 2),
            Pair(multiplayIcon, delayBetweenAnimations * 3),
            Pair(plusIcon, delayBetweenAnimations * 4)
        )

        fun animateNext(index: Int) {
            if (index < animationSequence.size) {
                val (view, delay) = animationSequence[index]
                handler.postDelayed({
                    fadeIn(view) {
                        animateNext(index + 1)
                    }
                }, delay)
            } else {
                handler.postDelayed({
                    navigateToMainActivity()
                },delayBetweenAnimations * 2)

            }
        }

        animateNext(0)
    }

    private fun fadeIn(view: View, onComplete: () -> Unit) {
        val fadeIn = AlphaAnimation(0f, 1f)
        fadeIn.duration = animationDuration
        fadeIn.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {
                view.visibility = View.VISIBLE
            }

            override fun onAnimationEnd(animation: Animation?) {
                onComplete()
            }

            override fun onAnimationRepeat(animation: Animation?) {}
        })
        view.startAnimation(fadeIn)
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}