package com.devfest.runtime.engine.blocks

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

class BatteryLevelCondition(private val context: Context) {

    fun check(minLevel: Int = 0, maxLevel: Int = 100): Boolean {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            context.registerReceiver(null, filter)
        }
        
        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        
        if (level == -1 || scale == -1) return false

        val batteryPct = (level * 100 / scale.toFloat()).toInt()
        
        return batteryPct in minLevel..maxLevel
    }
}
