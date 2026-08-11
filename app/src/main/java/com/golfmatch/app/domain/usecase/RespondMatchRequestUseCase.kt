package com.golfmatch.app.domain.usecase

import com.golfmatch.app.domain.model.MatchRequest
import com.golfmatch.app.domain.repository.MatchRepository
import javax.inject.Inject

/** 受信したマッチング申請の承認／却下（認可はto_user_id本人のみ、サーバー側で検証） */
class RespondMatchRequestUseCase @Inject constructor(
    private val matchRepository: MatchRepository
) {
    suspend operator fun invoke(matchRequestId: String, approve: Boolean): MatchRequest =
        if (approve) {
            matchRepository.approveMatchRequest(matchRequestId)
        } else {
            matchRepository.rejectMatchRequest(matchRequestId)
        }
}
