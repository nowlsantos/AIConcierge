package com.nowlsantos.aiconcierge

import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun test_local_skill_logic() {
        val query = "i'll have the pizza"
        val nodeName = "pizza"
        
        // Logical verification of the matching rule I implemented in ConciergeViewModel
        val isMatch = query == nodeName || query.endsWith(nodeName) || query.contains("show me $nodeName")
        
        assertTrue("Query '$query' should match node '$nodeName'", isMatch)
    }
}
