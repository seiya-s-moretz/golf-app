package com.golfmatch.app.data.mapper

import com.golfmatch.app.data.dto.AreaDto
import com.golfmatch.app.domain.model.Area
import kotlinx.datetime.Instant

fun AreaDto.toDomain(): Area = Area(
    areaId = areaId,
    prefecture = prefecture,
    areaName = areaName,
    displayOrder = displayOrder,
    isActive = isActive,
    createdAt = Instant.parse(createdAt)
)
