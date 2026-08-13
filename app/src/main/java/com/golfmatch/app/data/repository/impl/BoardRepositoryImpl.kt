package com.golfmatch.app.data.repository.impl

import com.golfmatch.app.data.api.ApiService
import com.golfmatch.app.data.dto.CreateBoardPostRequestDto
import com.golfmatch.app.data.mapper.toDomain
import com.golfmatch.app.domain.model.BoardPost
import com.golfmatch.app.domain.repository.BoardRepository
import javax.inject.Inject

class BoardRepositoryImpl @Inject constructor(
    private val api: ApiService
) : BoardRepository {

    override suspend fun getBoardPosts(before: String?, limit: Int): List<BoardPost> =
        api.getBoardPosts(before, limit).map { it.toDomain() }

    override suspend fun createBoardPost(content: String): BoardPost =
        api.createBoardPost(CreateBoardPostRequestDto(content)).toDomain()
}
