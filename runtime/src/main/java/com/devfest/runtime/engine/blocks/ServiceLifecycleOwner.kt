package com.devfest.runtime.engine.blocks

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

/**
 * A custom [LifecycleOwner] that can be explicitly started and destroyed.
 * Used by [CameraCaptureHandler] so CameraX can bind in a foreground-service context
 * where [androidx.lifecycle.ProcessLifecycleOwner] is not in STARTED state.
 */
class ServiceLifecycleOwner : LifecycleOwner {
    private val registry = LifecycleRegistry(this)

    override val lifecycle: Lifecycle get() = registry

    fun start() {
        registry.currentState = Lifecycle.State.STARTED
    }

    fun stop() {
        registry.currentState = Lifecycle.State.DESTROYED
    }
}
