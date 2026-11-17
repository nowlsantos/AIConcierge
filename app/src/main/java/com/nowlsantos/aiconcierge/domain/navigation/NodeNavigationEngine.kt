package com.nowlsantos.aiconcierge.domain.navigation

import com.nowlsantos.common.DocumentNode
import com.nowlsantos.common.UiTemplate
import com.nowlsantos.aiconcierge.utils.NavigationAliasRegistry
import java.text.Normalizer

/**
 * INFALLIBLE PRODUCTION-GRADE NAVIGATION ENGINE
 * 100% Metadata-Driven Search Engine for AIConcierge.
 * Integrates NavigationAliasRegistry for instantaneous alias resolution.
 * Prioritizes container/hub nodes over leaf detail nodes when matching broad category terms.
 * Features Stopword Filtering and Confidence Thresholds to prevent weak filler matches.
 * Operates 100% directly on master_tree.json metadata.
 */
class NodeNavigationEngine {

    private val PREFIX_REGEX = Regex("(?i)^(open|show|show me|take me to|navigate to|go to|open up|display|view|see|the|a|an|our|turn on|turn off|switch on|switch " +
            "off|adjust|play|pause|stop|control/return to)\\s+")

    private val GENERIC_STOPWORDS = setOf(
        "information", "info", "and", "the", "for", "about", "with", "from", "show", "open",
        "view", "see", "get", "page", "section", "item", "details", "detail"
    )

    fun findPathToNode(targetName: String, root: DocumentNode, activeParentNode: DocumentNode? = null): List<DocumentNode>? {
        val rawQuery = normalize(targetName)
        val mappedTarget = NavigationAliasRegistry.ALIAS_MAP[rawQuery] 
            ?: NavigationAliasRegistry.ALIAS_MAP[targetName.lowercase().trim()] 
            ?: targetName
        val query = normalize(mappedTarget)
        if (query.isEmpty()) return null

        // Phase 1: Pre-Order Exact Name Match (Container nodes checked first)
        searchExactName(root, emptyList(), query)?.let { return it }

        // Phase 2: Pre-Order Exact Keyword Match (Container nodes checked first)
        searchExactKeyword(root, emptyList(), query)?.let { return it }

        // Phase 3: Post-Order Name Sub-Phrase Containment (Children checked first)
        searchNameContainment(root, emptyList(), query)?.let { return it }

        // Phase 4: Post-Order Fuzzy Keyword Match (Children checked first)
        searchKeywordMatch(root, emptyList(), query)?.let { return it }

        // Phase 5: Level-Weighted Token Overlap Scoring with Minimum Confidence Threshold
        return searchTokenOverlapMatch(root, query)
    }

    private fun normalize(text: String): String {
        // 1. Accent & Diacritic Stripping (e.g. À la carte -> A la carte)
        val unaccented = Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
        // 2. Symbol & Ampersand Normalization (e.g. & -> and, 's -> s)
        val cleaned = unaccented.lowercase()
            .replace("&", " and ")
            .replace("-", " ")
            .replace("_", " ")
            .replace(Regex("[^a-z0-9\\s]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
        // 3. Filler & Prefix Stripping
        val stripped = cleaned.replace(PREFIX_REGEX, "").trim()
        return stripped.ifBlank { cleaned }
    }

    /**
     * Phase 1: Pre-Order Exact Name Match
     * Evaluates container nodes first so category hub nodes take priority over leaf detail nodes.
     */
    private fun searchExactName(current: DocumentNode, path: List<DocumentNode>, q: String): List<DocumentNode>? {
        val newPath = path + current

        val nameNorm = normalize(current.name)
        val nameNormNoThe = nameNorm.removePrefix("the ").trim()
        val qNoThe = q.removePrefix("the ").trim()

        if (nameNorm == q || nameNormNoThe == qNoThe || nameNorm == qNoThe || nameNormNoThe == q) {
            return newPath
        }

        for (child in current.children) {
            searchExactName(child, newPath, q)?.let { return it }
        }

        return null
    }

    /**
     * Phase 2: Pre-Order Exact Keyword Match
     * Evaluates container nodes first.
     */
    private fun searchExactKeyword(current: DocumentNode, path: List<DocumentNode>, q: String): List<DocumentNode>? {
        val newPath = path + current

        val qNoThe = q.removePrefix("the ").trim()

        if (current.keywords.isNotBlank()) {
            val individualKeywords = current.keywords.split(",").map { normalize(it) }
            if (individualKeywords.any { kw -> kw.isNotBlank() && (kw == q || kw == qNoThe || kw.removePrefix("the ").trim() == qNoThe) }) {
                if (current.uiTemplate != UiTemplate.DETAIL_VIEW || current.children.isEmpty()) {
                    return newPath
                }
            }
        }

        for (child in current.children) {
            searchExactKeyword(child, newPath, q)?.let { return it }
        }

        return null
    }

    /**
     * Phase 3: Post-Order Name Sub-Phrase Containment
     * Evaluates children first.
     */
    private fun searchNameContainment(current: DocumentNode, path: List<DocumentNode>, q: String): List<DocumentNode>? {
        if (q.length < 3) return null
        val newPath = path + current

        // Post-Order Traversal: Check children first
        for (child in current.children) {
            searchNameContainment(child, newPath, q)?.let { return it }
        }

        val nameNorm = normalize(current.name).removePrefix("the ").trim()
        val qNoThe = q.removePrefix("the ").trim()

        if (current.level > 1 && nameNorm.isNotBlank() && qNoThe.isNotBlank()) {
            if (nameNorm == qNoThe || (qNoThe.length >= 4 && nameNorm.length >= 4 && (nameNorm.contains(qNoThe) || qNoThe.contains(nameNorm)))) {
                return newPath
            }
        }

        return null
    }

    /**
     * Phase 4: Post-Order Fuzzy Keyword Match
     * Evaluates children first.
     */
    private fun searchKeywordMatch(current: DocumentNode, path: List<DocumentNode>, q: String): List<DocumentNode>? {
        val newPath = path + current

        // Post-Order Traversal: Check children first
        for (child in current.children) {
            searchKeywordMatch(child, newPath, q)?.let { return it }
        }

        val keywords = current.keywords
        if (keywords.isNotEmpty()) {
            val kwList = keywords.split(",").map { normalize(it) }.filter { kw -> kw !in GENERIC_STOPWORDS }
            val qWords = q.split(" ").filter { it.isNotEmpty() && it !in GENERIC_STOPWORDS }.toSet()

            if (qWords.isNotEmpty()) {
                val matches = kwList.any { kw ->
                    if (kw.isEmpty()) false
                    else {
                        val kwWords = kw.split(" ").filter { it.isNotEmpty() }.toSet()
                        kw == q || kwWords.contains(q) || qWords.contains(kw) ||
                        (q.length >= 4 && kw.length >= 4 && (q.contains(kw) || kw.contains(q)))
                    }
                }
                if (matches) return newPath
            }
        }

        return null
    }

    /**
     * Phase 5: Level-Weighted Token Overlap Scoring with Confidence Threshold
     * Requires overlapCount >= 2 or compositeScore >= 12.0 to prevent weak generic matches.
     */
    private fun searchTokenOverlapMatch(root: DocumentNode, q: String): List<DocumentNode>? {
        val qTokens = q.split(" ")
            .map { normalize(it) }
            .filter { it.length >= 3 && it !in GENERIC_STOPWORDS }
            .toSet()
        
        if (qTokens.isEmpty()) return null

        var bestMatchPath: List<DocumentNode>? = null
        var maxScore = 0.0

        fun collectCandidates(current: DocumentNode, path: List<DocumentNode>) {
            val newPath = path + current
            val nameTokens = normalize(current.name).split(" ").filter { it.length >= 3 && it !in GENERIC_STOPWORDS }.toSet()
            val kwTokens = current.keywords.split(",", " ", "-", "_").map { normalize(it) }.filter { it.length >= 3 && it !in GENERIC_STOPWORDS }.toSet()
            val allNodeTokens = nameTokens + kwTokens

            val overlapCount = qTokens.count { qToken ->
                allNodeTokens.any { nToken -> nToken == qToken || (qToken.length >= 4 && nToken.length >= 4 && (nToken.contains(qToken) || qToken.contains(nToken))) }
            }

            // Require minimum 2 overlapping tokens or a strong precision ratio
            if (overlapCount >= 1) {
                val precisionRatio = overlapCount.toDouble() / allNodeTokens.size.coerceAtLeast(1)
                val levelWeight = current.level.toDouble() / 10.0
                val compositeScore = (overlapCount * 10.0) + (precisionRatio * 5.0) + levelWeight

                // Confidence Threshold: Require score >= 12.0 or multiple overlapping tokens to avoid single weak filler matches
                if (compositeScore >= 12.0 && compositeScore > maxScore) {
                    maxScore = compositeScore
                    bestMatchPath = newPath
                }
            }

            for (child in current.children) {
                collectCandidates(child, newPath)
            }
        }

        collectCandidates(root, emptyList())
        return if (maxScore >= 12.0) bestMatchPath else null
    }

    fun isSameViewType(currentName: String?, targetName: String?, root: DocumentNode?): Boolean {
        return !(currentName.isNullOrBlank() || targetName.isNullOrBlank()) && currentName.trim().equals(targetName.trim(), ignoreCase = true)
    }
}
