package com.alperensiki.on_yuz

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.alperensiki.on_yuz.databinding.ActivityProfileBinding

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    private var currentNewRoomName: String = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupNavigation()
        setupOrbActions()
        setupMenuButtons()
    }

    private fun setupNavigation() {
        binding.bottomNavLayout.getChildAt(1).setOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    private fun setupOrbActions() {
        binding.orbLayout.btnAdd.setOnClickListener {
            openCreateRoomRoomName()
        }
    }

    private fun setupMenuButtons() {
        binding.btnCreateRoom.setOnClickListener {
            openCreateRoomRoomName()
        }
    }

    private fun openCreateRoomRoomName() {
        binding.createRoomContainer.visibility = View.VISIBLE
        binding.createRoomContainer.removeAllViews()
        val view = layoutInflater.inflate(R.layout.layout_create_room_roomname, binding.createRoomContainer, false)
        binding.createRoomContainer.addView(view)
        val etRoomName = view.findViewById<EditText>(R.id.etNewRoomName)
        view.findViewById<View>(R.id.orbLayout).setOnClickListener {
            val name = etRoomName.text.toString()
            if (name.isNotEmpty()) {
                currentNewRoomName = name
                openCreateRoomTimeCapsule()
            } else {
                etRoomName.error = "Please enter room name"
            }
        }

        view.findViewById<View>(R.id.btnBackToProfile).setOnClickListener {
            closeCreateRoomOverlay()
        }
    }

    private fun openCreateRoomTimeCapsule() {
        binding.createRoomContainer.removeAllViews()
        val view = layoutInflater.inflate(R.layout.layout_create_room_timecapsule, binding.createRoomContainer, false)
        binding.createRoomContainer.addView(view)
        val npDays = view.findViewById<NumberPicker>(R.id.npDays)
        val npHours = view.findViewById<NumberPicker>(R.id.npHours)
        npDays?.apply { minValue = 0; maxValue = 365; value = 0; wrapSelectorWheel = true }
        npHours?.apply { minValue = 0; maxValue = 23; value = 0; wrapSelectorWheel = true }

        view.findViewById<View>(R.id.orbLayout).setOnClickListener {
            openCreateRoomNotification()
        }

        view.findViewById<View>(R.id.tvBackEditRoomName).setOnClickListener {
            openCreateRoomRoomName()
        }

        view.findViewById<View>(R.id.btnBackToProfile).setOnClickListener {
            closeCreateRoomOverlay()
        }
    }

    private fun openCreateRoomNotification() {
        binding.createRoomContainer.removeAllViews()
        val view = layoutInflater.inflate(R.layout.layout_create_room_notification, binding.createRoomContainer, false)
        binding.createRoomContainer.addView(view)
        val npDays = view.findViewById<NumberPicker>(R.id.npDays)
        val npHours = view.findViewById<NumberPicker>(R.id.npHours)
        npDays?.apply { minValue = 0; maxValue = 365; wrapSelectorWheel = true }
        npHours?.apply { minValue = 0; maxValue = 23; wrapSelectorWheel = true }

        view.findViewById<View>(R.id.orbLayout).setOnClickListener {
            openCreateRoomRoomMode()
        }

        view.findViewById<View>(R.id.tvBackEditTimeCapsule).setOnClickListener {
            openCreateRoomTimeCapsule()
        }

        view.findViewById<View>(R.id.btnBackToProfile).setOnClickListener {
            closeCreateRoomOverlay()
        }
    }

    private fun openCreateRoomRoomMode() {
        binding.createRoomContainer.removeAllViews()
        val view = layoutInflater.inflate(R.layout.layout_create_room_roommode, binding.createRoomContainer, false)
        binding.createRoomContainer.addView(view)

        val btnPublic = view.findViewById<TextView>(R.id.btnPublic)
        val btnPrivate = view.findViewById<TextView>(R.id.btnPrivate)

        btnPublic.setOnClickListener {
            btnPublic.isSelected = true
            btnPrivate.isSelected = false
        }

        btnPrivate.setOnClickListener {
            btnPrivate.isSelected = true
            btnPublic.isSelected = false
        }

        view.findViewById<View>(R.id.orbLayout).setOnClickListener {
            if (btnPublic.isSelected) {
                openFinalizeRoom(R.layout.layout_create_public_room)
            } else if (btnPrivate.isSelected) {
                openFinalizeRoom(R.layout.layout_create_private_room)
            }
        }

        view.findViewById<View>(R.id.tvBackEditNotification).setOnClickListener {
            openCreateRoomNotification()
        }

        view.findViewById<View>(R.id.btnBackToProfile).setOnClickListener {
            closeCreateRoomOverlay()
        }
    }

private fun openFinalizeRoom(layoutResId: Int) {
    binding.createRoomContainer.removeAllViews()
    val view = layoutInflater.inflate(layoutResId, binding.createRoomContainer, false)
    binding.createRoomContainer.addView(view)

    view.findViewById<TextView>(R.id.tvConfirmationText)?.text = "“$currentNewRoomName”\nwill be created."

    val btnCollaboration = view.findViewById<LinearLayout>(R.id.btnCollaboration)
    val tvCollabLabel = view.findViewById<TextView>(R.id.tvCollabLabel)
    val ivCollabIcon = view.findViewById<ImageView>(R.id.ivCollabIcon)

    btnCollaboration?.setOnClickListener {
        it.isSelected = !it.isSelected

        if (it.isSelected) {
            it.setBackgroundResource(R.drawable.bg_mode_button_selector)
            tvCollabLabel?.setTextColor(Color.BLACK)
            ivCollabIcon?.setColorFilter(Color.BLACK)
        } else {
            it.setBackgroundResource(R.drawable.bg_profile_item)
            tvCollabLabel?.setTextColor(Color.WHITE)
            ivCollabIcon?.setColorFilter(Color.WHITE)
        }
    }

    view.findViewById<View>(R.id.orbLayout)?.setOnClickListener {
        if (btnCollaboration?.isSelected == true) {
            openCollaborationSettings()
        }
    }


    view.findViewById<View>(R.id.tvBackRoomName)?.setOnClickListener { openCreateRoomRoomMode() }
    view.findViewById<View>(R.id.btnBackToProfile)?.setOnClickListener { closeCreateRoomOverlay() }
}

    private fun openCollaborationSettings() {
        binding.createRoomContainer.removeAllViews()
        val view = layoutInflater.inflate(R.layout.layout_create_room_collaboration, binding.createRoomContainer, false)
        binding.createRoomContainer.addView(view)

        view.findViewById<TextView>(R.id.tvChooseRoomMode).text = "Collaboration\n“$currentNewRoomName”"

        val btnShare = view.findViewById<TextView>(R.id.btnShareLink)
        val btnInvite = view.findViewById<TextView>(R.id.btnInviteRoomer)

        btnShare.setOnClickListener { it.isSelected = true; btnInvite.isSelected = false }
        btnInvite.setOnClickListener { it.isSelected = true; btnShare.isSelected = false }


        view.findViewById<View>(R.id.orbLayout).setOnClickListener {
        }


        view.findViewById<View>(R.id.tvBackEditNotification).setOnClickListener {
            openFinalizeRoom(R.layout.layout_create_public_room)
        }

        view.findViewById<View>(R.id.btnBackToProfile).setOnClickListener {
            closeCreateRoomOverlay()
        }
    }


    private fun closeCreateRoomOverlay() {
        binding.createRoomContainer.visibility = View.GONE
        binding.createRoomContainer.removeAllViews()
    }

    override fun onBackPressed() {
        if (binding.createRoomContainer.visibility == View.VISIBLE) {
            closeCreateRoomOverlay()
        } else {
            super.onBackPressed()
        }
    }
}