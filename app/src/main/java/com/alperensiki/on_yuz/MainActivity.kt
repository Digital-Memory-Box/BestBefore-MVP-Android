package com.alperensiki.on_yuz

import StackTransfomer
import android.os.Bundle
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import com.alperensiki.on_yuz.databinding.ActivityMainBinding
import com.alperensiki.on_yuz.databinding.ScreenArtistsBinding
import com.alperensiki.on_yuz.databinding.ScreenLoginBinding
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var mainBinding: ActivityMainBinding
    private lateinit var artistsBinding: ScreenArtistsBinding
    private lateinit var loginBinding: ScreenLoginBinding

    private enum class Screen { EVERYONE, ARTISTS, LOGIN }
    private var currentScreen = Screen.EVERYONE

    private var startX = 0f
    private var startY = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mainBinding = ActivityMainBinding.inflate(layoutInflater)
        artistsBinding = ScreenArtistsBinding.inflate(layoutInflater)
        loginBinding = ScreenLoginBinding.inflate(layoutInflater)

        showEveryone()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        handleSwipe(ev)
        return super.dispatchTouchEvent(ev)
    }

    private fun handleSwipe(event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.x
                startY = event.y
            }
            MotionEvent.ACTION_UP -> {
                val dx = event.x - startX
                val dy = event.y - startY

                if (abs(dx) > abs(dy) && abs(dx) > 150) {
                    if (dx < 0) {
                        if (currentScreen == Screen.EVERYONE) showArtists()
                    } else {
                        if (currentScreen == Screen.ARTISTS) showEveryone()
                    }
                }
            }
        }
    }

    private fun showEveryone() {
        setContentView(mainBinding.root)
        currentScreen = Screen.EVERYONE

        mainBinding.root.setOnClickListener {
            if (currentScreen == Screen.EVERYONE) showLogin()
        }
    }

    private fun showArtists() {
        setContentView(artistsBinding.root)
        currentScreen = Screen.ARTISTS

    }

    private fun showLogin() {
        setContentView(loginBinding.root)
        currentScreen = Screen.LOGIN

        loginBinding.orbLayout.root.setOnClickListener {
            val intent = android.content.Intent(this, HallwayActivity::class.java)
            startActivity(intent)
        }
    }
}
