package com.golfmatch.app.domain.usecase

import com.golfmatch.app.domain.model.MatchRequest
import com.golfmatch.app.domain.repository.MatchRepository
import com.golfmatch.app.domain.repository.MatchRequestDirection
import javax.inject.Inject

/**
 * 自分宛/自分発のマッチング申請一覧取得（技術設計書 6-5章 `GET /users/me/match-requests`）。
 *
 * 技術設計書4章のusecase一覧には明記が無いが、[GetRoundJoinRequestsUseCase]等の他の一覧取得系UseCaseと
 * 同様にRepositoryへの薄い委譲としてUseCase層に置く（既存パターンとの整合性を優先した実装判断）。
 */
class GetMatchRequestsUseCase @Inject constructor(
    private val matchRepository: MatchRepository
) {
    suspend operator fun invoke(direction: MatchRequestDirection): List<MatchRequest> =
        matchRepository.getMatchRequests(direction)
}
