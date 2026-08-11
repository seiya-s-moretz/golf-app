package com.golfmatch.app.domain.usecase

import com.golfmatch.app.domain.model.RoundEvent
import com.golfmatch.app.domain.repository.RoundRepository
import javax.inject.Inject

/**
 * ラウンド募集一覧取得（技術設計書 6-4章）。
 *
 * 技術設計書4章のusecase一覧には明記が無いが、他の一覧取得（[GetRecommendUsersUseCase]等）と
 * 同様にRepositoryへの薄い委譲としてUseCase層に置く（既存パターンとの整合性を優先した実装判断）。
 */
class GetRoundEventsUseCase @Inject constructor(
    private val roundRepository: RoundRepository
) {
    suspend operator fun invoke(): List<RoundEvent> = roundRepository.getRoundEvents()
}
