package com.alperensiki.on_yuz

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.alperensiki.on_yuz.databinding.ActivityProfileBinding
import androidx.recyclerview.widget.LinearLayoutManager


class ProfileActivity : AppCompatActivity() {

    private var currentMemoryContainer: LinearLayout? = null

    private val getPhoto = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val (date, time) = getCurrentDateTime()
            val newItem = MemoryItem(it, date, time)
            currentMemoryContainer?.let { container ->
                addNewMemoryToUI(container, newItem)
            }
        }
    }

    private val getFile = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val (date, time) = getCurrentDateTime()
            val newItem = MemoryItem(it, date, time)
            currentMemoryContainer?.let { container ->
                addNewMemoryToUI(container, newItem)
            }
        }
    }

    private val getAudio = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val (date, time) = getCurrentDateTime()
            val newItem = MemoryItem(it, date, time)
            currentMemoryContainer?.let { container ->
                addNewMemoryToUI(container, newItem)
            }
        }
    }

    private val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
    }

    companion object {
        private var selectedCapsuleDays = 0
        private var selectedCapsuleHours = 0
        private var selectedNotifyDays = 0
        private var selectedNotifyHours = 0
        private var isRoomPublic = true
        private val createdRoomsList = mutableListOf<TimeCapsuleRoom>()
    }

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
        binding.btnTimeCapsule.setOnClickListener {
            binding.createRoomContainer.visibility = View.VISIBLE
            openTimeCapsuleListScreen()
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
            closeOverlay()
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
            selectedCapsuleDays = npDays?.value ?: 0
            selectedCapsuleHours = npHours?.value ?: 0
            openCreateRoomNotification()
        }

        view.findViewById<View>(R.id.tvBackEditRoomName).setOnClickListener { openCreateRoomRoomName() }
        view.findViewById<View>(R.id.btnBackToProfile).setOnClickListener { closeOverlay() }
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
            // VERİ KAYDI: Notification için seçilen değerler
            selectedNotifyDays = npDays?.value ?: 0
            selectedNotifyHours = npHours?.value ?: 0
            openCreateRoomRoomMode()
        }

        view.findViewById<View>(R.id.tvBackEditTimeCapsule).setOnClickListener { openCreateRoomTimeCapsule() }
        view.findViewById<View>(R.id.btnBackToProfile).setOnClickListener { closeOverlay() }
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
            isRoomPublic = true // VERİ KAYDI
        }

        btnPrivate.setOnClickListener {
            btnPrivate.isSelected = true
            btnPublic.isSelected = false
            isRoomPublic = false // VERİ KAYDI
        }

        view.findViewById<View>(R.id.orbLayout).setOnClickListener {
            if (btnPublic.isSelected) {
                openFinalizeRoom(R.layout.layout_create_public_room)
            } else if (btnPrivate.isSelected) {
                openFinalizeRoom(R.layout.layout_create_private_room)
            }
        }

        view.findViewById<View>(R.id.tvBackEditNotification).setOnClickListener { openCreateRoomNotification() }
        view.findViewById<View>(R.id.btnBackToProfile).setOnClickListener { closeOverlay() }
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
        } else {
            finalizeAndSaveRoom()
        }
    }


    view.findViewById<View>(R.id.tvBackRoomName)?.setOnClickListener { openCreateRoomRoomMode() }
    view.findViewById<View>(R.id.btnBackToProfile)?.setOnClickListener { closeOverlay() }
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
            finalizeAndSaveRoom()
        }


        view.findViewById<View>(R.id.tvBackEditNotification).setOnClickListener {
            openFinalizeRoom(R.layout.layout_create_public_room)
        }

        view.findViewById<View>(R.id.btnBackToProfile).setOnClickListener {
            closeOverlay()
        }
    }

    private fun finalizeAndSaveRoom() {
        val newRoom = TimeCapsuleRoom(
            roomName = currentNewRoomName,
            capsuleDays = selectedCapsuleDays,
            capsuleHours = selectedCapsuleHours,
            notificationDays = selectedNotifyDays,
            notificationHours = selectedNotifyHours,
            isPublic = isRoomPublic
        )

        createdRoomsList.add(newRoom)
        closeOverlay()
    }

    private fun openTimeCapsuleListScreen() {
        binding.createRoomContainer.removeAllViews()
        val view = layoutInflater.inflate(R.layout.layout_time_capsule_main, binding.createRoomContainer, false)
        binding.createRoomContainer.addView(view)

        val recyclerView = view.findViewById<RecyclerView>(R.id.rvTimeCapsuleList)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val adapter = TimeCapsuleAdapter(createdRoomsList) { clickedRoom ->
            openRoomDetail(clickedRoom)
        }
        recyclerView.adapter = adapter

        view.findViewById<View>(R.id.navProfile).setOnClickListener { closeOverlay() }
    }

    private fun openRoomDetail(room: TimeCapsuleRoom) {
        binding.createRoomContainer.removeAllViews()
        val view = layoutInflater.inflate(R.layout.layout_room_detail, binding.createRoomContainer, false)
        binding.createRoomContainer.addView(view)

        currentMemoryContainer = view.findViewById<LinearLayout>(R.id.memoryContainer)

        view.findViewById<TextView>(R.id.tvDetailRoomName).text = room.roomName

        val btnPhoto = view.findViewById<View>(R.id.btnPhotoArchive)
        btnPhoto.findViewById<TextView>(R.id.label).text = "Photo archive"
        btnPhoto.findViewById<ImageView>(R.id.icon).setImageResource(R.drawable.ic_launcher_foreground) // Kendi ikonunla değiştir
        btnPhoto.setOnClickListener { getPhoto.launch("image/*") }

        val btnFile = view.findViewById<View>(R.id.btnChooseFile)
        btnFile.findViewById<TextView>(R.id.label).text = "Choose file"
        btnFile.findViewById<ImageView>(R.id.icon).setImageResource(R.drawable.ic_launcher_foreground)
        btnFile.setOnClickListener { getFile.launch("*/*") }

        val btnCamera = view.findViewById<View>(R.id.btnUseCamera)
        btnCamera.findViewById<TextView>(R.id.label).text = "Use camera"
        btnCamera.findViewById<ImageView>(R.id.icon).setImageResource(R.drawable.ic_launcher_foreground)
        btnCamera.setOnClickListener { takePhoto.launch(null) }

        val btnAudio = view.findViewById<View>(R.id.btnUploadAudio)
        btnAudio.findViewById<TextView>(R.id.label).text = "Record Audio"
        btnAudio.findViewById<ImageView>(R.id.icon).setImageResource(R.drawable.ic_launcher_foreground)
        btnAudio.setOnClickListener { getAudio.launch("audio/*") }

        view.findViewById<View>(R.id.navProfile)?.setOnClickListener { closeOverlay() }
    }

    private fun getCurrentDateTime(): Pair<String, String> {
        val sdfDate = java.text.SimpleDateFormat("dd MMMM yyyy", java.util.Locale.getDefault())
        val sdfTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        val now = java.util.Date()
        return Pair(sdfDate.format(now), sdfTime.format(now))
    }

    private fun addNewMemoryToUI(container: LinearLayout, memory: MemoryItem) {
        val inflater = LayoutInflater.from(this)
        val memoryView = inflater.inflate(R.layout.item_memory_preview, container, false)

        val ivPreview = memoryView.findViewById<ImageView>(R.id.ivMemoryPreview)
        val tvDate = memoryView.findViewById<TextView>(R.id.tvMemoryDate)
        val tvTime = memoryView.findViewById<TextView>(R.id.tvMemoryTime)
        val tvCaption = memoryView.findViewById<TextView>(R.id.btnEditCaption)

        ivPreview.setImageURI(memory.uri)
        tvDate.text = memory.date
        tvTime.text = memory.time
        tvCaption.text = memory.caption

        container.addView(memoryView, 0)
    }

    private fun closeOverlay() {
        binding.createRoomContainer.visibility = View.GONE
        binding.createRoomContainer.removeAllViews()
    }

    override fun onBackPressed() {
        if (binding.createRoomContainer.visibility == View.VISIBLE) {
            closeOverlay()
        } else {
            super.onBackPressed()
        }
    }
}