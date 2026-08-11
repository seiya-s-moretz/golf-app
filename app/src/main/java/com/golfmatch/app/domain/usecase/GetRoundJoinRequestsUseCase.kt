package com.golfmatch.app.domain.usecase

import com.golfmatch.app.domain.model.RoundJoinRequest
import com.golfmatch.app.domain.repository.RoundRepository
import javax.inject.Inject

/**
 * 主催者による自分の募集への参加申請一覧取得（技術設計書 6-4章 `GET /round-events/{id}/join-requests`、ADR-0001）。
 *
 * 技術設計書4章のusecase一覧には明記が無いが、他の一覧取得系UseCaseと同様に
 * Repositoryへの薄い委譲としてUseCase層に置く（既存パターンとの整合性を優先した実装判断）。
 */
class GetRoundJoinRequestsUseCase @Inject constructor(
    private val roundRepository: RoundRepository
) {
    suspend operator fun invoke(eventId: String): List<RoundJoinRequest> = roundRepository.getJoinRequests(eventId)
}
