package com.golfmatch.app.domain.usecase

import com.golfmatch.app.domain.model.Area
import com.golfmatch.app.domain.repository.AreaRepository
import javax.inject.Inject

/** 選択可能なエリア一覧の取得（ADR-0002） */
class GetAreasUseCase @Inject constructor(
    private val areaRepository: AreaRepository
) {
    suspend operator fun invoke(): List<Area> = areaRepository.getAreas()
}
