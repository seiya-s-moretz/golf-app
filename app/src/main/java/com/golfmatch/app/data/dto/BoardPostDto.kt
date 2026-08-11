package com.golfmatch.app.data.dto

import com.google.gson.annotations.SerializedName

/** 掲示板投稿DTO（技術設計書 6-6章） */
data class BoardPostDto(
    @SerializedName("post_id") val postId: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("content") val content: String,
    @SerializedName("created_at") val createdAt: String
)

/** `POST /board` リクエストボディ */
data class CreateBoardPostRequestDto(
    @SerializedName("content") val content: String
)
