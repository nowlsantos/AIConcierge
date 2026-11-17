package com.nowlsantos.aiconcierge.presentation

import android.content.Context
import com.nowlsantos.aiconcierge.data.remote.GeminiTextClient
import com.nowlsantos.aiconcierge.hardware.capture.AudioCaptureEngine
import com.nowlsantos.aiconcierge.hardware.playback.AudioPlaybackEngine
import com.nowlsantos.aiconcierge.repository.ConciergeRepository
import com.nowlsantos.aiconcierge.speech.GeminiLiveManager
import com.nowlsantos.common.UiTemplate
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class TreeSearchTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: ConciergeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        val dummyContext = mockk<Context>(relaxed = true)
        val dummyScope = CoroutineScope(SupervisorJob())
        val dummyCapture = mockk<AudioCaptureEngine>(relaxed = true)
        val dummyPlayback = mockk<AudioPlaybackEngine>(relaxed = true)
        val dummyManager = GeminiLiveManager(dummyScope, dummyCapture, dummyPlayback, dummyContext)
        val dummyTextClient = mockk<GeminiTextClient>(relaxed = true)
        val dummyRepo = ConciergeRepository(dummyTextClient)
        viewModel = ConciergeViewModel(dummyContext, dummyManager, dummyRepo)
    }

    @Test
    fun exact_category_match_for_Media_returns_Media_category_grid_node() {
        val root = TestTreeFixture.rootHome
        val path = viewModel.findPathToNode("Media", root)

        assertNotNull("Path should not be null for 'Media'", path)
        assertEquals(2, path!!.size) // [Home, Media]
        assertEquals("Home", path[0].name)
        assertEquals("Media", path[1].name)
        assertEquals(UiTemplate.CATEGORY_GRID, path[1].uiTemplate)
    }

    @Test
    fun deep_item_match_for_CNN_International_returns_full_ancestry_stack() {
        val root = TestTreeFixture.rootHome
        val path = viewModel.findPathToNode("CNN International", root)

        assertNotNull("Path should not be null for 'CNN International'", path)
        assertEquals(4, path!!.size) // [Home, Media, TV, CNN International]
        assertEquals("Home", path[0].name)
        assertEquals("Media", path[1].name)
        assertEquals("TV", path[2].name)
        assertEquals("CNN International", path[3].name)
        assertEquals(UiTemplate.DETAIL_VIEW, path[3].uiTemplate)
    }

    @Test
    fun keyword_search_for_congee_resolves_to_Cantonese_Breakfast_parent_selection_view() {
        val root = TestTreeFixture.rootHome
        val path = viewModel.findPathToNode("congee", root)

        assertNotNull("Path should not be null for keyword 'congee'", path)
        assertTrue(path!!.any { it.name == "Cantonese Breakfast" })
        val selectionNode = path.find { it.uiTemplate == UiTemplate.SELECTION_VIEW }
        assertNotNull("Path should contain a SELECTION_VIEW node", selectionNode)
        assertEquals("Cantonese Breakfast", selectionNode!!.name)
    }

    @Test
    fun in_room_dining_search_resolves_to_In_Room_Dining_Ordering_node() {
        val root = TestTreeFixture.rootHome
        val path1 = viewModel.findPathToNode("In-room dining", root)
        assertNotNull("Path should not be null for 'In-room dining'", path1)
        assertEquals("In-Room Dining Ordering", path1!!.last().name)

        val path2 = viewModel.findPathToNode("In Room Dining", root)
        assertNotNull("Path should not be null for 'In Room Dining'", path2)
        assertEquals("In-Room Dining Ordering", path2!!.last().name)
    }

    @Test
    fun exact_name_search_for_CNN_International_does_not_return_first_sibling_BBC_World_News() {
        val root = TestTreeFixture.rootHome
        val path = viewModel.findPathToNode("CNN International", root)

        assertNotNull(path)
        assertEquals("CNN International", path!!.last().name)
        assertNotEquals("BBC World News", path.last().name)
    }

    @Test
    fun exact_category_match_for_Spa_and_Wellness_returns_Spa_and_Wellness_node() {
        val root = TestTreeFixture.rootHome
        val path = viewModel.findPathToNode("Spa and Wellness", root)

        assertNotNull(path)
        assertEquals("Spa and Wellness", path!!.last().name)
        assertEquals(UiTemplate.CATEGORY_GRID, path.last().uiTemplate)
    }

    @Test
    fun search_for_non_existent_topic_returns_null() {
        val root = TestTreeFixture.rootHome
        val path = viewModel.findPathToNode("Mars Exploration Shuttle", root)

        assertNull("Path should be null for non-existent topic", path)
    }
}
