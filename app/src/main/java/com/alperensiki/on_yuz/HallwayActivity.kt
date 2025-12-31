package com.alperensiki.on_yuz

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.alperensiki.on_yuz.databinding.ActivityHallwayBinding

class HallwayActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHallwayBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHallwayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupStack()
        setupOrbActions()
    }

    private fun setupOrbActions() {

        binding.orbLayout.btnSearch.setOnClickListener {
        }

        binding.orbLayout.btnChat.setOnClickListener {
        }

        binding.orbLayout.btnAdd.setOnClickListener {
        }

        binding.orbLayout.btnProfile.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)

            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    private fun setupStack() {
        val items = listOf("NYC Trip", "Daily Trip", "21 Days", "Foreign", "Travel", "Nature")
        binding.viewPager.adapter = HallwayAdapter(items)
        binding.viewPager.orientation = ViewPager2.ORIENTATION_VERTICAL
        binding.viewPager.offscreenPageLimit = 5

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding.sideTitle.text = items[position]
            }
        })

        binding.viewPager.setPageTransformer { page, position ->
            page.apply {
                pivotX = 0f
                pivotY = height / 2f
                val absPos = Math.abs(position)

                when {
                    position < 0 -> {
                        val scale = 0.85f + (1 - absPos.coerceAtMost(1f)) * 0.15f
                        scaleX = scale
                        scaleY = scale
                        translationY = -height * position * 0.4f
                        alpha = 1f - (absPos * 0.3f)
                    }
                    position == 0f -> {
                        scaleX = 1f
                        scaleY = 1f
                        translationY = 0f
                        alpha = 1f
                    }
                    position > 0 -> {
                        val scale = 0.85f + (1 - absPos.coerceAtMost(1f)) * 0.15f
                        scaleX = scale
                        scaleY = scale
                        translationY = -height * position * 0.85f
                        alpha = 1f - (absPos * 0.3f)
                    }
                }
                translationZ = -absPos
            }
        }
    }

    private class HallwayAdapter(private val items: List<String>) :
        RecyclerView.Adapter<HallwayAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewById(R.id.cardTitle)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_stack_card, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.title.text = items[position]
        }

        override fun getItemCount(): Int = items.size
    }
}