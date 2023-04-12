package com.example.drawingapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.View
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable

class StartingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_starting)

        val animationView = findViewById<LottieAnimationView>(R.id.animation_view)
        animationView.setAnimation((R.raw.pencil))
        animationView.repeatCount = LottieDrawable.INFINITE
        animationView.playAnimation()

        Handler().postDelayed({



            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)

            finish()
        }, 5000)


    }
}