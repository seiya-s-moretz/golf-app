package com.golfmatch.app.data.repository.impl

import com.golfmatch.app.data.api.ApiService
import com.golfmatch.app.data.mapper.toDomain
import com.golfmatch.app.domain.model.MatchRequest
import com.golfmatch.app.domain.repository.MatchRepository
import com.golfmatch.app.domain.repository.MatchRequestDirection
import javax.inject.Inject

class MatchRepositoryImpl @Inject constructor(
    private val api: ApiService
) : MatchRepository {

    override suspend fun sendMatchRequest(toUserId: String): MatchRequest =
        api.sendMatchRequest(toUserId).toDomain()

    override suspend fun getMatchRequests(direction: MatchRequestDirection): List<MatchRequest> =
        api.getMatchRequests(direction.name.lowercase()).map { it.toDomain() }

    override suspend fun approveMatchRequest(matchRequestId: String): MatchRequest =
        api.approveMatchRequest(matchRequestId).toDomain()

    override suspend fun rejectMatchRequest(matchRequestId: String): MatchRequest =
        api.rejectMatchRequest(matchRequestId).toDomain()
}
