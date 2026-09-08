package com.dmb.bestbefore.data.local

import android.content.Context
import com.dmb.bestbefore.data.api.models.UserDto
import com.dmb.bestbefore.data.models.HallwayCard
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SessionManagerRobolectricTest {

    private lateinit var context: Context
    private lateinit var sessionManager: SessionManager

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        sessionManager = SessionManager.getInstance(context)
        sessionManager.clearSession()
    }

    @Test
    fun saveAndRetrieveAuthToken() {
        assertNull(sessionManager.getToken())

        sessionManager.saveAuthToken("token_xyz_123")
        assertEquals("token_xyz_123", sessionManager.getToken())
    }

    @Test
    fun saveAndRetrieveUserDtoWithComplexFields() {
        val user = UserDto(
            id = "user_456",
            name = "Test Person",
            email = "person@bestbefore.app",
            userType = "artist",
            bio = "Memory archivist",
            theme = "#123456",
            accentColor = "#FF5722",
            ignoredRoomIds = listOf("room_1", "room_2"),
            savedRoomIds = listOf("room_3", "room_4"),
            profileImageUrl = "https://images.bestbefore.app/avatar.png"
        )

        sessionManager.saveUser(user)

        val retrieved = sessionManager.getCachedUser()
        assertNotNull(retrieved)
        assertEquals("user_456", retrieved?.id)
        assertEquals("Test Person", retrieved?.name)
        assertEquals("artist", retrieved?.userType)
        assertEquals(listOf("room_1", "room_2"), retrieved?.ignoredRoomIds)
        assertEquals(listOf("room_3", "room_4"), retrieved?.savedRoomIds)
        assertEquals("https://images.bestbefore.app/avatar.png", sessionManager.getProfileImageUrl())
    }

    @Test
    fun saveAndRetrieveHallwayCardsList() {
        val cards = listOf(
            HallwayCard(id = "card_1", title = "Room One", ownerName = "Alice", tags = listOf("vacation", "summer")),
            HallwayCard(id = "card_2", title = "Room Two", ownerName = "Bob", tags = listOf("art", "music"))
        )

        sessionManager.saveHallwayCards(cards)

        val retrievedCards = sessionManager.getHallwayCards()
        assertEquals(2, retrievedCards.size)
        assertEquals("card_1", retrievedCards[0].id)
        assertEquals("Alice", retrievedCards[0].ownerName)
        assertEquals(listOf("vacation", "summer"), retrievedCards[0].tags)
        assertEquals("card_2", retrievedCards[1].id)
    }

    @Test
    fun clearSessionRemovesAllCachedData() {
        sessionManager.saveAuthToken("active_token")
        sessionManager.saveUser(UserDto(id = "u1", name = "U1", email = "u1@test.com"))

        sessionManager.clearSession()

        assertNull(sessionManager.getToken())
        assertNull(sessionManager.getCachedUser())
        assertNull(sessionManager.getProfileImageUrl())
    }

    @Test
    fun saveAndRetrieveManualProfileTags() {
        val tags = listOf("retro", "analog", "vintage")
        sessionManager.saveManualProfileTags(tags)

        val retrieved = sessionManager.getManualProfileTags()
        assertNotNull(retrieved)
        assertEquals(tags, retrieved)
    }
}
