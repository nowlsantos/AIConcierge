package com.nowlsantos.aiconcierge.domain.sync

import android.content.Context
import com.nowlsantos.aiconcierge.di.networkJson
import com.nowlsantos.aiconcierge.domain.navigation.NodeNavigationEngine
import com.nowlsantos.aiconcierge.repository.ConciergeRepository
import com.nowlsantos.aiconcierge.utils.Logger
import com.nowlsantos.common.DocumentNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface SyncResult {
    data class Success(
        val rootNode: DocumentNode,
        val startNode: DocumentNode,
        val devPath: List<DocumentNode>,
        val responseText: String,
        val isServerConnected: Boolean
    ) : SyncResult

    data class Failure(val errorMessage: String) : SyncResult
}

class BrainConnectionWatchdog(
    private val repository: ConciergeRepository,
    private val navigationEngine: NodeNavigationEngine
) {

    suspend fun checkConnection(): Result<String> {
        return try {
            repository.checkConnection()
        } catch (e: Exception) {
            // Immediately fail-fast if backend is offline to remain in standalone mode
            Result.failure(e)
        }
    }

    suspend fun performInitialSync(
        context: Context,
        devStartTarget: String = "Home"
    ): SyncResult = withContext(Dispatchers.IO) {
        val handshakeResult = repository.initialHandshake()

        if (handshakeResult.isSuccess) {
            val handshake = handshakeResult.getOrNull()
            val root = handshake?.node
            if (root != null) {
                val devPath = navigationEngine.findPathToNode(devStartTarget, root) ?: listOf(root)
                val startNode = devPath.lastOrNull() ?: root
                return@withContext SyncResult.Success(
                    rootNode = root,
                    startNode = startNode,
                    devPath = devPath,
                    responseText = handshake.text,
                    isServerConnected = true
                )
            }
        }

        // Offline fallback to packaged assets/master_tree.json
        Logger.w("Backend server offline. Falling back to local packaged assets/master_tree.json")
        try {
            val jsonText = context.assets.open("master_tree.json").bufferedReader().use { it.readText() }
            val localRoot = networkJson.decodeFromString<DocumentNode>(jsonText)
            val devPath = navigationEngine.findPathToNode(devStartTarget, localRoot) ?: listOf(localRoot)
            val startNode = devPath.last()

            SyncResult.Success(
                rootNode = localRoot,
                startNode = startNode,
                devPath = devPath,
                responseText = localRoot.content,
                isServerConnected = false
            )
        } catch (e: Exception) {
            Logger.e("Local asset fallback failed: ${e.localizedMessage}", e)
            SyncResult.Failure(e.localizedMessage ?: "Unknown sync error")
        }
    }
}
