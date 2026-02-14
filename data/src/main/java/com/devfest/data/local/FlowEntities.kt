package com.devfest.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "flows")
data class FlowEntity(
    @PrimaryKey val id: String,
    val title: String,
    val graphJson: String,
    val explanation: String,
    val riskFlags: String
)
