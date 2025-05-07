package com.example.hesaplayici

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationSet
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var iconContainers: List<CardView>
    private lateinit var appNameText: TextView
    private lateinit var appSlogan: TextView
    private lateinit var containerLayout: ConstraintLayout

    private val animationDuration = 500L
    private val delayBetweenIcons = 200L
    private val transitionDelay = 800L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        containerLayout = findViewById(R.id.splash_container)
        appNameText = findViewById(R.id.app_name_text)
        appSlogan = findViewById(R.id.app_slogan)

        // İkon container'ları referansla
        iconContainers = listOf(
            findViewById(R.id.divide_container),
            findViewById(R.id.minus_container),
            findViewById(R.id.multiply_container),
            findViewById(R.id.plus_container)
        )

        // Debug için tüm container'ları görünür yapalım
        // Bu satırları test ettikten sonra kaldırabilirsiniz
        for (container in iconContainers) {
            container.visibility = View.VISIBLE
        }

        // Normal animasyonlarımızı başlatalım
        animateIconsSequentially()
    }

    private fun animateIconsSequentially() {
        val handler = Handler(Looper.getMainLooper())
        var delay = 0L

        iconContainers.forEachIndexed { index, container ->
            handler.postDelayed({
                container.visibility = View.VISIBLE
                animateIcon(container) {
                    // Son ikon animasyonu bittikten sonra uygulama adını göster
                    if (index == iconContainers.size - 1) {
                        animateAppName()
                    }
                }
            }, delay)
            delay += delayBetweenIcons
        }
    }

    private fun animateIcon(view: View, onEnd: () -> Unit) {
        val fadeIn = AlphaAnimation(0f, 1f).apply {
            duration = animationDuration
        }

        val scale = ScaleAnimation(
            0.2f, 1.0f, 0.2f, 1.0f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = animationDuration
            interpolator = OvershootInterpolator(1.5f)
        }

        val animSet = AnimationSet(false).apply {
            addAnimation(fadeIn)
            addAnimation(scale)
            setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}

                override fun onAnimationEnd(animation: Animation?) {
                    onEnd()
                }

                override fun onAnimationRepeat(animation: Animation?) {}
            })
        }

        view.startAnimation(animSet)
    }

    private fun animateAppName() {
        appNameText.visibility = View.VISIBLE
        appSlogan.visibility = View.VISIBLE

        val fadeIn = AlphaAnimation(0f, 1f).apply {
            duration = animationDuration
        }

        val scale = ScaleAnimation(
            0.8f, 1.0f, 0.8f, 1.0f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = animationDuration
            interpolator = OvershootInterpolator(2f)
        }

        val animSet = AnimationSet(false).apply {
            addAnimation(fadeIn)
            addAnimation(scale)
            setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}
                override fun onAnimationEnd(animation: Animation?) {
                    Handler(Looper.getMainLooper()).postDelayed({ goToMain() }, transitionDelay)
                }
                override fun onAnimationRepeat(animation: Animation?) {}
            })
        }

        appNameText.startAnimation(animSet)
        appSlogan.startAnimation(animSet)
    }

    private fun goToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}