package com.golfmatch.app.domain.usecase

import com.golfmatch.app.domain.model.RoundJoinRequest
import com.golfmatch.app.domain.repository.RoundRepository
import javax.inject.Inject

/**
 * ラウンド参加申請の承認／却下（主催者のみ実行可能。サーバー側で認可検証、ADR-0001）
 */
class ApproveRoundJoinUseCase @Inject constructor(
    private val roundRepository: RoundRepository
) {
    suspend operator fun invoke(eventId: String, requestId: String, approve: Boolean): RoundJoinRequest =
        if (approve) {
            roundRepository.approveJoinRequest(eventId, requestId)
        } else {
            roundRepository.rejectJoinRequest(eventId, requestId)
        }
}
