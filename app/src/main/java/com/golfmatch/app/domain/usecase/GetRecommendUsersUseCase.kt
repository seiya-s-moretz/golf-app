package com.golfmatch.app.domain.usecase

import com.golfmatch.app.domain.model.User
import com.golfmatch.app.domain.repository.UserRepository
import javax.inject.Inject

/** おすすめユーザー一覧取得（レコメンドロジックはサーバー側で適用済み。技術設計書 6-5章） */
class GetRecommendUsersUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(): List<User> = userRepository.getRecommendedUsers()
}
