package com.golfmatch.app.domain.usecase

import com.golfmatch.app.domain.model.MatchRequest
import com.golfmatch.app.domain.repository.MatchRepository
import javax.inject.Inject

/** マッチング申請の送信（おすすめユーザー画面から） */
class SendMatchRequestUseCase @Inject constructor(
    private val matchRepository: MatchRepository
) {
    suspend operator fun invoke(toUserId: String): MatchRequest =
        matchRepository.sendMatchRequest(toUserId)
}
