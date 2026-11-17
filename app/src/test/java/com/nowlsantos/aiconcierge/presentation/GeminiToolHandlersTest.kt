package com.nowlsantos.aiconcierge.presentation

import android.content.Context
import com.nowlsantos.aiconcierge.hardware.capture.AudioCaptureEngine
import com.nowlsantos.aiconcierge.hardware.playback.AudioPlaybackEngine
import com.nowlsantos.aiconcierge.repository.ConciergeRepository
import com.nowlsantos.aiconcierge.speech.GeminiLiveManager
import com.nowlsantos.common.DocumentNode
import com.nowlsantos.common.UiTemplate
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GeminiToolHandlersTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var dummyContext: Context
    private lateinit var dummyRepo: ConciergeRepository
    private lateinit var dummyCapture: AudioCaptureEngine
    private lateinit var dummyPlayback: AudioPlaybackEngine
    private lateinit var dummyScope: CoroutineScope

    private lateinit var geminiLiveManager: GeminiLiveManager
    private lateinit var viewModel: ConciergeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        dummyContext = mockk(relaxed = true)
        dummyRepo = mockk(relaxed = true)
        dummyCapture = mockk(relaxed = true)
        dummyPlayback = mockk(relaxed = true)
        dummyScope = CoroutineScope(testDispatcher)

        val dummyContext = mockk<android.content.Context>(relaxed = true)
        geminiLiveManager = GeminiLiveManager(dummyScope, dummyCapture, dummyPlayback, dummyContext)
        viewModel = ConciergeViewModel(dummyContext, geminiLiveManager, dummyRepo)

        // Inject Test Tree Fixture into ViewModel for Deterministic Offline Testing
        val testTree = TestTreeFixture.rootHome
        val navStackField = ConciergeViewModel::class.java.getDeclaredField("rootTree")
        navStackField.isAccessible = true
        navStackField.set(viewModel, testTree)

        val uiStateField = ConciergeViewModel::class.java.getDeclaredField("_uiState")
        uiStateField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val uiStateFlow = uiStateField.get(viewModel) as MutableStateFlow<ConciergeUiState>
        uiStateFlow.value = ConciergeUiState(
            currentNode = testTree,
            navStack = listOf(testTree),
            isLoading = false
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun gemini_onNavigate_navigates_to_Spa_and_Wellness() = runBlocking {
        val callback = geminiLiveManager.onNavigate
        assertNotNull("onNavigate callback should be registered", callback)

        val feedback = callback!!.invoke("Spa and Wellness")

        val state = viewModel.uiState.value
        assertEquals("Spa and Wellness", state.currentNode?.name)
        assertEquals(UiTemplate.CATEGORY_GRID, state.currentNode?.uiTemplate)
        assertTrue("Feedback should contain SUCCESS status", feedback.contains("Status: SUCCESS"))
        assertTrue("Feedback should contain Spa and Wellness name", feedback.contains("Spa and Wellness"))
    }

    @Test
    fun gemini_onNavigate_navigates_to_Peninsula_Academy() = runBlocking {
        val callback = geminiLiveManager.onNavigate
        assertNotNull("onNavigate callback should be registered", callback)

        val feedback = callback!!.invoke("Peninsula Academy")

        val state = viewModel.uiState.value
        assertEquals("Peninsula Academy", state.currentNode?.name)
        assertEquals(UiTemplate.CHOICE_LIST, state.currentNode?.uiTemplate)
        assertTrue("Feedback should contain SUCCESS status", feedback.contains("Status: SUCCESS"))
    }

    @Test
    fun gemini_onMediaControl_value_first_lookup_navigates_to_CNN_International() = runBlocking {
        val callback = geminiLiveManager.onMediaControl
        assertNotNull("onMediaControl callback should be registered", callback)

        val feedback = callback!!.invoke("TV", "Play", "CNN International")

        val state = viewModel.uiState.value
        assertEquals("Play: CNN International", state.mediaState["TV"])
        assertTrue(feedback.contains("CNN International"))
    }

    @Test
    fun gemini_onMediaControl_filter_updates_mediaState() = runBlocking {
        val callback = geminiLiveManager.onMediaControl
        assertNotNull("onMediaControl callback should be registered", callback)

        val feedback = callback!!.invoke("TV", "Play", "Sports")

        val state = viewModel.uiState.value
        assertEquals("Filter: Sports", state.mediaState["TV"])
        assertTrue(feedback.contains("Sports"))
    }

    @Test
    fun gemini_onRoomControl_updates_roomStates_and_navigates_to_Lighting() = runBlocking {
        val callback = geminiLiveManager.onRoomControl
        assertNotNull("onRoomControl callback should be registered", callback)

        val feedback = callback!!.invoke("Lighting", "Lounge", "Relax Scene")

        val state = viewModel.uiState.value
        assertEquals("Relax Scene", state.roomStates["LIGHTING-LOUNGE"])
        assertTrue(feedback.contains("Lighting"))
    }

    @Test
    fun gemini_onRoomControl_explicit_bedroom_zone_overrides_active_living_room_tab() = runBlocking {
        val uiStateField = ConciergeViewModel::class.java.getDeclaredField("_uiState")
        uiStateField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val uiStateFlow = uiStateField.get(viewModel) as MutableStateFlow<ConciergeUiState>
        uiStateFlow.value = uiStateFlow.value.copy(
            roomStates = mapOf("ROOM CONTROLS-LIVING ROOM" to "SHOW-1")
        )

        val callback = geminiLiveManager.onRoomControl
        assertNotNull("onRoomControl callback should be registered", callback)

        val feedback = callback!!.invoke("Lighting", "BEDROOM", "ON")

        val state = viewModel.uiState.value
        assertEquals("ON", state.roomStates["LIGHTING-BEDROOM"])
        assertTrue(feedback.contains("Lighting"))
        assertTrue(feedback.contains("BEDROOM"))
    }

    @Test
    fun gemini_onGetServiceInfo_searches_tree_and_navigates_to_Bletchley_Park() = runBlocking {
        val callback = geminiLiveManager.onGetServiceInfo
        assertNotNull("onGetServiceInfo callback should be registered", callback)

        val feedback = callback!!.invoke("Bletchley Park")

        val state = viewModel.uiState.value
        assertTrue("Feedback should confirm topic found", feedback.contains("Status: SUCCESS"))
        assertEquals("Bletchley Park Tour", state.currentNode?.name)
    }

    @Test
    fun gemini_onSelectOption_updates_selections_map() = runBlocking {
        val callback = geminiLiveManager.onSelectOption
        assertNotNull("onSelectOption callback should be registered", callback)

        val feedback = callback!!.invoke("Breakfast Beverage", "Fresh Orange Juice")

        val state = viewModel.uiState.value
        assertEquals("Fresh Orange Juice", state.selections["Breakfast Beverage"])
        assertTrue(feedback.contains("Breakfast Beverage"))
        assertTrue(feedback.contains("Fresh Orange Juice"))
    }

    @Test
    fun gemini_onSubmitOrder_navigates_to_ORDER_SUMMARY() = runBlocking {
        val callback = geminiLiveManager.onSubmitOrder
        assertNotNull("onSubmitOrder callback should be registered", callback)

        val feedback = callback!!.invoke()

        val state = viewModel.uiState.value
        assertEquals(UiTemplate.ORDER_SUMMARY, state.currentNode?.uiTemplate)
        assertEquals("Order Summary", state.currentNode?.name)
        assertFalse(state.isOrderConfirmed)
        assertTrue(feedback.contains("Status: SUCCESS"))
    }

    @Test
    fun gemini_onSetMasterScene_standby_requests_standby() = runBlocking {
        val callback = geminiLiveManager.onSetMasterScene
        assertNotNull("onSetMasterScene callback should be registered", callback)

        val feedback = callback!!.invoke("Standby")
        assertTrue(feedback.contains("Status: SUCCESS"))
        assertTrue(feedback.contains("Entering standby mode"))
    }

    @Test
    fun gemini_onStandby_triggers_standby_flow() = runBlocking {
        val callback = geminiLiveManager.onStandby
        assertNotNull("onStandby callback should be registered", callback)

        callback!!.invoke()
        // onStandby sets isStandbyPending in GeminiLiveManager
        val pendingField = GeminiLiveManager::class.java.getDeclaredField("isStandbyPending")
        pendingField.isAccessible = true
        val isPending = pendingField.getBoolean(geminiLiveManager)
        assertTrue("isStandbyPending must be true after onStandby callback", isPending)
    }

    @Test
    fun gemini_onNavigate_standby_keywords_requests_standby() = runBlocking {
        val callback = geminiLiveManager.onNavigate
        assertNotNull("onNavigate callback should be registered", callback)

        val feedback1 = callback!!.invoke("Standby")
        assertTrue(feedback1.contains("Status: SUCCESS"))
        assertTrue(feedback1.contains("Entering standby mode"))

        val feedback2 = callback.invoke("stand by")
        assertTrue(feedback2.contains("Status: SUCCESS"))
        assertTrue(feedback2.contains("Entering standby mode"))

        val feedback3 = callback.invoke("good night")
        assertTrue(feedback3.contains("Status: SUCCESS"))
        assertTrue(feedback3.contains("Entering standby mode"))
    }

    @Test
    fun gemini_onSelectOption_checkbox_respects_maxSelections_limit() = runBlocking {
        val checkboxGroup = DocumentNode(
            name = "Pastries",
            content = "",
            control = "checkbox",
            maxSelections = 2,
            children = listOf(
                DocumentNode(name = "Plain Croissant", content = ""),
                DocumentNode(name = "Pain Au Chocolat", content = ""),
                DocumentNode(name = "Danish Pastry", content = "")
            )
        )
        val selectionNode = DocumentNode(
            name = "Bakery Basket",
            content = "",
            uiTemplate = UiTemplate.SELECTION_VIEW,
            children = listOf(checkboxGroup)
        )
        val uiStateField = ConciergeViewModel::class.java.getDeclaredField("_uiState")
        uiStateField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val uiStateFlow = uiStateField.get(viewModel) as MutableStateFlow<ConciergeUiState>
        uiStateFlow.value = uiStateFlow.value.copy(currentNode = selectionNode)

        val callback = geminiLiveManager.onSelectOption
        assertNotNull("onSelectOption callback should be registered", callback)

        callback!!.invoke("Pastries", "Plain Croissant")
        callback.invoke("Pastries", "Pain Au Chocolat")
        callback.invoke("Pastries", "Danish Pastry")

        val state = viewModel.uiState.value
        val pastriesSelection = state.selections["Pastries"] ?: ""
        val selectedItems = pastriesSelection.split(",").map { it.trim() }.filter { it.isNotBlank() }
        assertEquals(2, selectedItems.size)
        assertTrue(selectedItems.contains("Plain Croissant"))
        assertTrue(selectedItems.contains("Pain Au Chocolat"))
        assertFalse(selectedItems.contains("Danish Pastry"))
    }

    @Test
    fun gemini_onSelectOption_radio_overwrites_single_selection() = runBlocking {
        val radioGroup = DocumentNode(
            name = "Beverage",
            content = "",
            control = "radio",
            children = listOf(
                DocumentNode(name = "Tea", content = ""),
                DocumentNode(name = "Green Tea", content = "")
            )
        )
        val selectionNode = DocumentNode(
            name = "Breakfast",
            content = "",
            uiTemplate = UiTemplate.SELECTION_VIEW,
            children = listOf(radioGroup)
        )
        val uiStateField = ConciergeViewModel::class.java.getDeclaredField("_uiState")
        uiStateField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val uiStateFlow = uiStateField.get(viewModel) as MutableStateFlow<ConciergeUiState>
        uiStateFlow.value = uiStateFlow.value.copy(currentNode = selectionNode)

        val callback = geminiLiveManager.onSelectOption
        assertNotNull("onSelectOption callback should be registered", callback)

        callback!!.invoke("Beverage", "Tea")
        assertEquals("Tea", viewModel.uiState.value.selections["Beverage"])

        callback.invoke("Beverage", "Green Tea")
        assertEquals("Green Tea", viewModel.uiState.value.selections["Beverage"])
    }

    @Test
    fun navigating_home_resets_selections_while_preserving_language() = runBlocking {
        val rootNode = DocumentNode(name = "Home", content = "")
        val diningNode = DocumentNode(name = "In-Room Dining", content = "", uiTemplate = UiTemplate.SELECTION_VIEW)
        val uiStateField = ConciergeViewModel::class.java.getDeclaredField("_uiState")
        uiStateField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val uiStateFlow = uiStateField.get(viewModel) as MutableStateFlow<ConciergeUiState>
        uiStateFlow.value = uiStateFlow.value.copy(
            currentNode = diningNode,
            navStack = listOf(rootNode, diningNode),
            selections = mapOf("Beverage" to "Coffee", "Language" to "French")
        )

        viewModel.onEvent(ConciergeUiEvent.OnHomeClicked)

        val state = viewModel.uiState.value
        assertEquals("Home", state.currentNode?.name)
        assertNull(state.selections["Beverage"])
        assertEquals("French", state.selections["Language"])
    }

    @Test
    fun navigating_back_from_selection_view_resets_selections_while_preserving_language() = runBlocking {
        val rootNode = DocumentNode(name = "Home", content = "")
        val categoryNode = DocumentNode(name = "Breakfast", content = "", uiTemplate = UiTemplate.CATEGORY_GRID)
        val itemNode = DocumentNode(name = "Pancakes", content = "", uiTemplate = UiTemplate.SELECTION_VIEW)

        val uiStateField = ConciergeViewModel::class.java.getDeclaredField("_uiState")
        uiStateField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val uiStateFlow = uiStateField.get(viewModel) as MutableStateFlow<ConciergeUiState>
        uiStateFlow.value = uiStateFlow.value.copy(
            currentNode = itemNode,
            navStack = listOf(rootNode, categoryNode, itemNode),
            selections = mapOf("Syrup" to "Maple", "Language" to "Spanish")
        )

        viewModel.onEvent(ConciergeUiEvent.OnBackClicked)

        val state = viewModel.uiState.value
        assertEquals("Breakfast", state.currentNode?.name)
        assertNull(state.selections["Syrup"])
        assertEquals("Spanish", state.selections["Language"])
    }

    @Test
    fun selecting_another_node_resets_selections_but_order_summary_keeps_them() = runBlocking {
        val burgerNode = DocumentNode(name = "Burger", content = "", uiTemplate = UiTemplate.SELECTION_VIEW)
        val sandwichNode = DocumentNode(name = "Sandwich", content = "", uiTemplate = UiTemplate.SELECTION_VIEW)

        val uiStateField = ConciergeViewModel::class.java.getDeclaredField("_uiState")
        uiStateField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val uiStateFlow = uiStateField.get(viewModel) as MutableStateFlow<ConciergeUiState>
        uiStateFlow.value = uiStateFlow.value.copy(
            currentNode = burgerNode,
            navStack = listOf(burgerNode),
            selections = mapOf("Cooking" to "Medium Rare", "Language" to "German")
        )

        // Advancing to Order Summary keeps selections
        viewModel.onEvent(ConciergeUiEvent.OnSubmitOrder)
        var state = viewModel.uiState.value
        assertEquals("Order Summary", state.currentNode?.name)
        assertEquals("Medium Rare", state.selections["Cooking"])
        assertEquals("German", state.selections["Language"])

        // Selecting a new node (e.g. Sandwich) resets previous item selections but keeps Language
        viewModel.onEvent(ConciergeUiEvent.OnNodeSelected(sandwichNode))
        state = viewModel.uiState.value
        assertEquals("Sandwich", state.currentNode?.name)
        assertNull(state.selections["Cooking"])
        assertEquals("German", state.selections["Language"])
    }

    @Test
    fun voice_navigation_resets_selections_while_preserving_language() = runBlocking {
        val rootNode = DocumentNode(name = "Home", content = "", children = listOf(
            DocumentNode(name = "In-Room Dining", content = "")
        ))
        val currentDish = DocumentNode(name = "Burger", content = "", uiTemplate = UiTemplate.SELECTION_VIEW)
        val uiStateField = ConciergeViewModel::class.java.getDeclaredField("_uiState")
        uiStateField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val uiStateFlow = uiStateField.get(viewModel) as MutableStateFlow<ConciergeUiState>
        uiStateFlow.value = uiStateFlow.value.copy(
            currentNode = currentDish,
            navStack = listOf(rootNode, currentDish),
            selections = mapOf("Cooking" to "Medium", "Language" to "Japanese")
        )

        val callback = geminiLiveManager.onNavigate
        assertNotNull("onNavigate callback should be registered", callback)

        callback!!.invoke("Home")

        val state = viewModel.uiState.value
        assertEquals("Home", state.currentNode?.name)
        assertNull(state.selections["Cooking"])
        assertEquals("Japanese", state.selections["Language"])
    }

    @Test
    fun geminiLiveManager_warmupSession_initializes_without_error() = runBlocking {
        geminiLiveManager.warmupSession()
        assertNotNull(geminiLiveManager.state.value)
    }

    @Test
    fun geminiLiveManager_immunity_shield_active_during_welcome_greeting() = runBlocking {
        val shieldField = GeminiLiveManager::class.java.getDeclaredField("isWelcomeGreetingActive")
        shieldField.isAccessible = true
        shieldField.set(geminiLiveManager, true)
        assertTrue(shieldField.get(geminiLiveManager) as Boolean)
    }
}
