package com.nowlsantos.aiconcierge.ui.component

import android.content.Context
import android.content.res.Resources
import com.nowlsantos.aiconcierge.R
import com.nowlsantos.aiconcierge.utils.resolveDrawableByName
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class IconUtilsTest {

    @Test
    fun resolveDrawableByName_resolves_exact_matching_drawables() {
        val mockContext = mockk<Context>()
        val mockResources = mockk<Resources>()

        every { mockContext.resources } returns mockResources
        every { mockContext.packageName } returns "com.nowlsantos.aiconcierge"

        every { mockResources.getIdentifier("media", "drawable", "com.nowlsantos.aiconcierge") } returns 0
        every { mockResources.getIdentifier("media_icon", "drawable", "com.nowlsantos.aiconcierge") } returns R.drawable.media_icon

        val resId = resolveDrawableByName(mockContext, "media")
        assertEquals(R.drawable.media_icon, resId)
    }

    @Test
    fun resolveDrawableByName_falls_back_to_services_icon_if_not_found() {
        val mockContext = mockk<Context>(relaxed = true)
        val resId = resolveDrawableByName(mockContext, "Unknown Mystery Service")
        assertEquals(R.drawable.services_icon, resId)
    }
}
