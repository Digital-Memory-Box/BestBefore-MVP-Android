package com.alperensiki.on_yuz

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.alperensiki.on_yuz.databinding.ActivityOpeningBinding

class OpeningActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOpeningBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityOpeningBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.circleContainer.setOnClickListener {

        }

        binding.subText2.setOnClickListener {

        }
    }
}
