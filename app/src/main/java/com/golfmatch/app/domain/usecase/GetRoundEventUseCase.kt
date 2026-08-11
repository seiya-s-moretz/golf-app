package com.golfmatch.app.domain.usecase

import com.golfmatch.app.domain.model.RoundEvent
import com.golfmatch.app.domain.repository.RoundRepository
import javax.inject.Inject

/**
 * ラウンド募集詳細取得（技術設計書 6-4章 `GET /round-events/{id}`）。
 *
 * 技術設計書4章のusecase一覧には明記が無いが、[GetRoundEventsUseCase]（一覧取得）と同様に
 * Repositoryへの薄い委譲としてUseCase層に置く（既存パターンとの整合性を優先した実装判断）。
 */
class GetRoundEventUseCase @Inject constructor(
    private val roundRepository: RoundRepository
) {
    suspend operator fun invoke(eventId: String): RoundEvent = roundRepository.getRoundEvent(eventId)
}
