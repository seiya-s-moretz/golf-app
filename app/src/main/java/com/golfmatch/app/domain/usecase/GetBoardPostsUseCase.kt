package com.golfmatch.app.domain.usecase

import com.golfmatch.app.domain.model.BoardPost
import com.golfmatch.app.domain.repository.BoardRepository
import javax.inject.Inject

/**
 * 掲示板投稿一覧取得（技術設計書 6-6章）。
 *
 * 技術設計書4章のusecase一覧には明記が無いが、他の一覧取得（[GetRoundEventsUseCase]等）と
 * 同様にRepositoryへの薄い委譲としてUseCase層に置く（既存パターンとの整合性を優先した実装判断）。
 */
class GetBoardPostsUseCase @Inject constructor(
    private val boardRepository: BoardRepository
) {
    suspend operator fun invoke(before: String? = null, beforeId: String? = null, limit: Int = 20): List<BoardPost> =
        boardRepository.getBoardPosts(before, beforeId, limit)
}
