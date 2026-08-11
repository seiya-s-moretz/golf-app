package com.golfmatch.app.domain.usecase

import com.golfmatch.app.domain.model.RoundEvent
import com.golfmatch.app.domain.repository.RoundRepository
import kotlinx.datetime.Instant
import javax.inject.Inject

/** ラウンド募集の新規作成 */
class CreateRoundEventUseCase @Inject constructor(
    private val roundRepository: RoundRepository
) {
    suspend operator fun invoke(
        clubName: String,
        datetime: Instant,
        fee: Int,
        capacity: Int
    ): RoundEvent = roundRepository.createRoundEvent(clubName, datetime, fee, capacity)
}
