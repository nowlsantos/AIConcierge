package com.nowlsantos.aiconcierge.ui.component

import com.nowlsantos.aiconcierge.presentation.TestTreeFixture
import com.nowlsantos.common.DocumentNode
import org.junit.Assert.*
import org.junit.Test

class PeninsulaDashboardHubTest {

    private fun findOrSyntheticNode(
        nodes: List<DocumentNode>,
        targetName: String,
        displayName: String? = null
    ): DocumentNode? {
        val cleanTarget = targetName.lowercase().trim()
        val match = nodes.firstOrNull { it.name.lowercase().contains(cleanTarget) }
            ?: nodes.flatMap { it.children }.firstOrNull { it.name.lowercase().contains(cleanTarget) }

        if (match != null) {
            return if (displayName != null) match.copy(name = displayName) else match
        }

        return DocumentNode(
            name = displayName ?: targetName,
            level = 2,
            uiTemplate = "DETAIL_VIEW",
            content = "Request $targetName for your residence.",
            iconName = targetName.lowercase()
        )
    }

    @Test
    fun findOrSyntheticNode_resolves_existing_root_nodes() {
        val rootNodes = TestTreeFixture.rootHome.children

        val diningNode = findOrSyntheticNode(rootNodes, "Dining")
        assertNotNull(diningNode)
        assertEquals("Dining", diningNode?.name)

        val mediaNode = findOrSyntheticNode(rootNodes, "Media")
        assertNotNull(mediaNode)
        assertEquals("Media", mediaNode?.name)

        val guestServicesNode = findOrSyntheticNode(rootNodes, "Guest Services")
        assertNotNull(guestServicesNode)
        assertEquals("Guest Services", guestServicesNode?.name)

        val spaNode = findOrSyntheticNode(rootNodes, "Spa and Wellness")
        assertNotNull(spaNode)
        assertEquals("Spa and Wellness", spaNode?.name)
    }

    @Test
    fun findOrSyntheticNode_generates_synthetic_node_when_not_in_tree() {
        val rootNodes = TestTreeFixture.rootHome.children

        val housekeepingNode = findOrSyntheticNode(rootNodes, "Housekeeping")
        assertNotNull(housekeepingNode)
        assertEquals("Housekeeping", housekeepingNode?.name)
        assertEquals("DETAIL_VIEW", housekeepingNode?.uiTemplate)

        val valetNode = findOrSyntheticNode(rootNodes, "Valet Service")
        assertNotNull(valetNode)
        assertEquals("Valet Service", valetNode?.name)

        val makeupRoomNode = findOrSyntheticNode(rootNodes, "Makeup Room")
        assertNotNull(makeupRoomNode)
        assertEquals("Makeup Room", makeupRoomNode?.name)

        val roomConnectNode = findOrSyntheticNode(rootNodes, "Transportation", "Room Connect")
        assertNotNull(roomConnectNode)
        assertEquals("Room Connect", roomConnectNode?.name)
    }
}
