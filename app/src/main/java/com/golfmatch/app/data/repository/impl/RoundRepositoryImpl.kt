package com.golfmatch.app.data.repository.impl

import com.golfmatch.app.data.api.ApiService
import com.golfmatch.app.data.dto.CreateRoundEventRequestDto
import com.golfmatch.app.data.mapper.toDomain
import com.golfmatch.app.domain.model.RoundEvent
import com.golfmatch.app.domain.model.RoundJoinRequest
import com.golfmatch.app.domain.repository.RoundRepository
import kotlinx.datetime.Instant
import javax.inject.Inject

class RoundRepositoryImpl @Inject constructor(
    private val api: ApiService
) : RoundRepository {

    override suspend fun getRoundEvents(): List<RoundEvent> =
        api.getRoundEvents().map { it.toDomain() }

    override suspend fun getRoundEvent(eventId: String): RoundEvent =
        api.getRoundEvent(eventId).toDomain()

    override suspend fun createRoundEvent(
        clubName: String,
        datetime: Instant,
        fee: Int,
        capacity: Int
    ): RoundEvent = api.createRoundEvent(
        CreateRoundEventRequestDto(
            clubName = clubName,
            datetime = datetime.toString(),
            fee = fee,
            capacity = capacity
        )
    ).toDomain()

    override suspend fun applyJoin(eventId: String): RoundJoinRequest =
        api.applyRoundJoin(eventId).toDomain()

    override suspend fun getJoinRequests(eventId: String): List<RoundJoinRequest> =
        api.getRoundJoinRequests(eventId).map { it.toDomain() }

    override suspend fun approveJoinRequest(eventId: String, requestId: String): RoundJoinRequest =
        api.approveRoundJoinRequest(eventId, requestId).toDomain()

    override suspend fun rejectJoinRequest(eventId: String, requestId: String): RoundJoinRequest =
        api.rejectRoundJoinRequest(eventId, requestId).toDomain()
}
