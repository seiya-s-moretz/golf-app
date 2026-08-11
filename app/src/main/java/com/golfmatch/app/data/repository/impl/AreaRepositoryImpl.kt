package com.golfmatch.app.data.repository.impl

import com.golfmatch.app.data.api.ApiService
import com.golfmatch.app.data.mapper.toDomain
import com.golfmatch.app.domain.model.Area
import com.golfmatch.app.domain.repository.AreaRepository
import javax.inject.Inject

class AreaRepositoryImpl @Inject constructor(
    private val api: ApiService
) : AreaRepository {
    override suspend fun getAreas(): List<Area> = api.getAreas().map { it.toDomain() }
}
