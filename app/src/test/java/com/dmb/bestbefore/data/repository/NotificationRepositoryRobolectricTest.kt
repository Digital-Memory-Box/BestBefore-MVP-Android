package com.dmb.bestbefore.data.repository

import android.content.Context
import com.dmb.bestbefore.data.models.AppNotification
import com.dmb.bestbefore.data.models.NotificationType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NotificationRepositoryRobolectricTest {

    private lateinit var context: Context
    private lateinit var repository: NotificationRepository

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        repository = NotificationRepository(context)
        repository.clearAll()
    }

    @Test
    fun addNotificationUpdatesStateFlowAndPersistsToSharedPreferences() {
        val notif = AppNotification(
            id = "notif_1",
            title = "Room Invite",
            message = "You have been invited to Summer Memories",
            timestamp = System.currentTimeMillis(),
            type = NotificationType.INVITATION,
            relatedRoomId = "room_abc",
            relatedRoomName = "Summer Memories"
        )

        repository.addNotification(notif)

        val currentList = repository.notifications.value
        assertEquals(1, currentList.size)
        assertEquals("notif_1", currentList[0].id)
        assertEquals("Room Invite", currentList[0].title)

        // Verify fresh repository instance restores same persisted state from SharedPreferences
        val freshRepo = NotificationRepository(context)
        assertEquals(1, freshRepo.notifications.value.size)
        assertEquals("notif_1", freshRepo.notifications.value[0].id)
    }

    @Test
    fun removeNotificationRemembersDismissedAndFiltersOnMerge() {
        val notif1 = AppNotification(id = "notif_1", title = "N1", message = "M1", timestamp = 1000L, type = NotificationType.GENERAL)
        val notif2 = AppNotification(id = "notif_2", title = "N2", message = "M2", timestamp = 2000L, type = NotificationType.GENERAL)

        repository.addNotification(notif1)
        repository.addNotification(notif2)
        assertEquals(2, repository.notifications.value.size)

        repository.removeNotification("notif_1")
        assertEquals(1, repository.notifications.value.size)
        assertEquals("notif_2", repository.notifications.value[0].id)

        // Attempting to merge dismissed notif_1 again must ignore it
        repository.mergeNotifications(listOf(notif1, notif2))
        assertEquals(1, repository.notifications.value.size)
        assertEquals("notif_2", repository.notifications.value[0].id)
    }

    @Test
    fun clearAllRemovesAllNotificationsAndMarksThemDismissed() {
        val notif = AppNotification(id = "notif_1", title = "N1", message = "M1", timestamp = 1000L, type = NotificationType.GENERAL)
        repository.addNotification(notif)

        repository.clearAll()
        assertTrue(repository.notifications.value.isEmpty())

        // Ensure newly instantiated repo also starts empty
        val freshRepo = NotificationRepository(context)
        assertTrue(freshRepo.notifications.value.isEmpty())
    }
}
