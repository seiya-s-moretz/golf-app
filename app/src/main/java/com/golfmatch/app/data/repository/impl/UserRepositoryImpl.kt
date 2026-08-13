package com.golfmatch.app.data.repository.impl

import com.golfmatch.app.data.api.ApiService
import com.golfmatch.app.data.dto.UpdateUserRequestDto
import com.golfmatch.app.data.mapper.toDomain
import com.golfmatch.app.domain.model.Purpose
import com.golfmatch.app.domain.model.User
import com.golfmatch.app.domain.repository.UserRepository
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val api: ApiService
) : UserRepository {

    override suspend fun getUser(userId: String): User = api.getUser(userId).toDomain()

    override suspend fun updateUser(
        userId: String,
        name: String,
        gender: String,
        age: Int,
        areaId: String,
        averageScore: Int,
        purpose: Purpose,
        introduction: String
    ): User = api.updateUser(
        userId,
        UpdateUserRequestDto(
            name = name,
            gender = gender,
            age = age,
            areaId = areaId,
            averageScore = averageScore,
            purpose = purpose.name,
            introduction = introduction
        )
    ).toDomain()

    override suspend fun getRecommendedUsers(beforeId: String?, limit: Int): List<User> =
        api.getRecommendedUsers(beforeId, limit).map { it.toDomain() }

    override suspend fun blockUser(userId: String) = api.blockUser(userId)

    override suspend fun unblockUser(userId: String) = api.unblockUser(userId)

    override suspend fun getBlockedUsers(): List<User> =
        api.getBlockedUsers().map { it.toDomain() }
}
