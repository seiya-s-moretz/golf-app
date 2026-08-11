package com.golfmatch.app.domain.usecase

import com.golfmatch.app.domain.model.BoardPost
import com.golfmatch.app.domain.repository.BoardRepository
import javax.inject.Inject

/** 掲示板への新規投稿（テキストのみ、PRD 3-1章） */
class PostBoardMessageUseCase @Inject constructor(
    private val boardRepository: BoardRepository
) {
    suspend operator fun invoke(content: String): BoardPost = boardRepository.createBoardPost(content)
}
