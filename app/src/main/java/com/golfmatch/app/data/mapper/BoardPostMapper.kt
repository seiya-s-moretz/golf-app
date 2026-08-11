package com.golfmatch.app.data.mapper

import com.golfmatch.app.data.dto.BoardPostDto
import com.golfmatch.app.domain.model.BoardPost
import kotlinx.datetime.Instant

fun BoardPostDto.toDomain(): BoardPost = BoardPost(
    postId = postId,
    userId = userId,
    content = content,
    createdAt = Instant.parse(createdAt)
)
