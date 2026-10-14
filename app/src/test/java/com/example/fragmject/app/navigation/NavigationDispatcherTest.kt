package com.example.fragmject.app.navigation

import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationDispatcherTest {
    private data class TestKey(val id: Int) : NavKey

    @Test
    fun `commands before binding are replayed in order`() {
        val dispatcher = NavigationDispatcher()
        val events = mutableListOf<String>()
        dispatcher.navigate(TestKey(1))
        dispatcher.navigateUp()
        dispatcher.popBackStack(TestKey(2))
        dispatcher.onAuthSuccess()

        dispatcher.bind(
            navigate = { events += "navigate:${(it as TestKey).id}" },
            navigateUp = { events += "up" },
            popBackStack = { events += "pop:${(it as TestKey).id}" },
            onAuthSuccess = { events += "auth" },
        )

        assertEquals(listOf("navigate:1", "up", "pop:2", "auth"), events)
    }

    @Test
    fun `released graph does not receive new commands`() {
        val dispatcher = NavigationDispatcher()
        val oldEvents = mutableListOf<NavKey>()
        val newEvents = mutableListOf<NavKey>()
        val release = dispatcher.bind({ oldEvents += it }, {}, {}, {})
        release()
        dispatcher.navigate(TestKey(1))
        dispatcher.bind({ newEvents += it }, {}, {}, {})

        assertEquals(emptyList<NavKey>(), oldEvents)
        assertEquals(listOf(TestKey(1)), newEvents)
    }

    @Test
    fun `old owner cannot release a newer graph`() {
        val dispatcher = NavigationDispatcher()
        val events = mutableListOf<NavKey>()
        val releaseOld = dispatcher.bind({}, {}, {}, {})
        dispatcher.bind({ events += it }, {}, {}, {})
        releaseOld()
        dispatcher.navigate(TestKey(2))

        assertEquals(listOf(TestKey(2)), events)
    }

    @Test
    fun `reentrant commands during replay are appended to the queue`() {
        val dispatcher = NavigationDispatcher()
        val events = mutableListOf<NavKey>()
        dispatcher.navigate(TestKey(1))
        dispatcher.navigate(TestKey(2))
        dispatcher.bind(
            navigate = {
                events += it
                if (it == TestKey(1)) dispatcher.navigate(TestKey(3))
            },
            navigateUp = {},
            popBackStack = {},
            onAuthSuccess = {},
        )

        assertEquals(listOf(TestKey(1), TestKey(2), TestKey(3)), events)
    }
}
