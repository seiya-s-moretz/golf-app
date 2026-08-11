package com.golfmatch.app.domain.usecase

import com.golfmatch.app.domain.model.RoundJoinRequest
import com.golfmatch.app.domain.repository.RoundRepository
import javax.inject.Inject

/** ラウンド募集への参加申請（ADR-0001：この時点ではRoundEvent.currentは加算されない） */
class ApplyRoundJoinUseCase @Inject constructor(
    private val roundRepository: RoundRepository
) {
    suspend operator fun invoke(eventId: String): RoundJoinRequest =
        roundRepository.applyJoin(eventId)
}
