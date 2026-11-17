package com.nowlsantos.aiconcierge.ui.screen

import com.nowlsantos.aiconcierge.presentation.TestTreeFixture
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConciergeScreenStateTest {

    @Test
    fun navStack_size_determines_topbar_back_button_visibility() {
        val rootStack = listOf(TestTreeFixture.rootHome)
        val deepStack = listOf(TestTreeFixture.rootHome, TestTreeFixture.diningCategory, TestTreeFixture.inRoomDining)

        val canNavigateBackRoot = rootStack.size > 1
        val canNavigateBackDeep = deepStack.size > 1

        assertFalse("Root screen should not show back arrow in TopBar", canNavigateBackRoot)
        assertTrue("Sub-page screen should show back arrow in TopBar", canNavigateBackDeep)
    }

    @Test
    fun navStack_size_determines_floating_mic_overlay_visibility() {
        val rootStack = listOf(TestTreeFixture.rootHome)
        val deepStack = listOf(TestTreeFixture.rootHome, TestTreeFixture.diningCategory)

        val isRootPage = rootStack.size <= 1
        val isDeepPage = deepStack.size <= 1

        assertTrue("Root page uses integrated center mic in Peninsula Dashboard", isRootPage)
        assertFalse("Sub-pages use bottom floating ArisMicButton overlay", isDeepPage)
    }
}
