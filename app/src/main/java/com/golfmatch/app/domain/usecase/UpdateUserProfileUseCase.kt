package com.golfmatch.app.domain.usecase

import com.golfmatch.app.domain.model.Purpose
import com.golfmatch.app.domain.model.User
import com.golfmatch.app.domain.repository.UserRepository
import javax.inject.Inject

/**
 * プロフィール更新（技術設計書 6-3章 `PUT /users/{id}`）。
 *
 * 技術設計書4章のusecase一覧には明記が無いが、マイページ画面の保存機能に必要なため
 * 既存の一覧取得系UseCaseと同様にRepositoryへの薄い委譲として新規追加する
 * （既存パターンとの整合性を優先した実装判断。詳細は実装メモを参照）。
 */
class UpdateUserProfileUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(
        userId: String,
        name: String,
        gender: String,
        age: Int,
        areaId: String,
        averageScore: Int,
        purpose: Purpose,
        introduction: String
    ): User = userRepository.updateUser(
        userId = userId,
        name = name,
        gender = gender,
        age = age,
        areaId = areaId,
        averageScore = averageScore,
        purpose = purpose,
        introduction = introduction
    )
}
