package com.devfest.automation.service

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.devfest.automation.di.AppDependencies
import com.devfest.automation.util.FlowPermissionHelper
import com.devfest.runtime.engine.FlowEngineFactory
import com.devfest.runtime.engine.FlowExecutionInput
import com.devfest.runtime.notifications.NotificationChannels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Runs active flows that react to pattern/unlock failure (e.g. take photo).
 * Started by [com.devfest.automation.receiver.PatternFailureReceiver] when the user fails unlock.
 */
class PatternFailureFlowService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != ACTION_RUN_PATTERN_FAILURE_FLOWS) {
            Log.w(TAG, "Ignoring intent with wrong action: ${intent?.action}")
            stopSelf(startId)
            return START_NOT_STICKY
        }
        Log.d(TAG, "onStartCommand: pattern failure detected, preparing to run flows")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannels.ensureChannel(this)
            val notification = NotificationCompat.Builder(this, NotificationChannels.FLOW_CHANNEL_ID)
                .setContentTitle("Security flow")
                .setContentText("Running automation…")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
            startForeground(NOTIFICATION_ID, notification)
        }
        // Launch asynchronously – do NOT use runBlocking here because
        // CameraCaptureHandler needs Dispatchers.Main for CameraX binding.
        // Blocking the main thread would cause a deadlock.
        serviceScope.launch {
            runFlowsThenStop(startId)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        Log.d(TAG, "Service destroyed, scope cancelled")
    }

    private suspend fun runFlowsThenStop(startId: Int) {
        try {
            val repository = AppDependencies.provideFlowRepository(applicationContext)
            val engine = FlowEngineFactory.createDefault(applicationContext)
            val activeGraphs = repository.getActiveFlowGraphs()
            Log.d(TAG, "Found ${activeGraphs.size} active flow(s)")

            val patternFailureFlows = activeGraphs.filter { graph ->
                FlowPermissionHelper.hasPatternFailureTrigger(graph)
            }
            Log.d(TAG, "Found ${patternFailureFlows.size} pattern-failure flow(s) to execute")

            if (patternFailureFlows.isEmpty()) {
                Log.w(TAG, "No active pattern-failure flows found – nothing to run")
            }

            val input = FlowExecutionInput(metadata = mapOf("trigger" to "pattern_failure"))
            for (graph in patternFailureFlows) {
                try {
                    Log.d(TAG, "Executing flow: ${graph.title} (${graph.id}), blocks: ${graph.blocks.map { it.type }}")
                    val result = engine.execute(graph, input)
                    Log.d(TAG, "Flow ${graph.title} completed: ${result.steps.map { "${it.blockId}=${it.status}(${it.message})" }}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error running flow ${graph.id}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "PatternFailureFlowService error", e)
        } finally {
            Log.d(TAG, "Flow execution complete, stopping service")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(Service.STOP_FOREGROUND_REMOVE)
            }
            stopSelf(startId)
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 9001
        const val ACTION_RUN_PATTERN_FAILURE_FLOWS = "com.devfest.automation.RUN_PATTERN_FAILURE_FLOWS"
        private const val TAG = "PatternFailureFlowSvc"
    }
}

