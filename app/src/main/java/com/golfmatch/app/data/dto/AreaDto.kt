package com.golfmatch.app.data.dto

import com.google.gson.annotations.SerializedName

/** エリアマスタDTO（技術設計書 6-2章、ADR-0002） */
data class AreaDto(
    @SerializedName("area_id") val areaId: String,
    @SerializedName("prefecture") val prefecture: String,
    @SerializedName("area_name") val areaName: String,
    @SerializedName("display_order") val displayOrder: Int,
    @SerializedName("is_active") val isActive: Boolean,
    @SerializedName("created_at") val createdAt: String
)
