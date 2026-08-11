package com.golfmatch.app.domain.usecase

import com.golfmatch.app.domain.model.User
import com.golfmatch.app.domain.repository.UserRepository
import javax.inject.Inject

/**
 * ユーザー単体取得（技術設計書 6-3章 `GET /users/{id}`）。
 *
 * マイページ画面での自分自身のプロフィール取得、および掲示板画面での投稿者情報解決
 * （[BoardPost][com.golfmatch.app.domain.model.BoardPost] は `userId` のみ保持するため）に利用する。
 * 技術設計書4章のusecase一覧には明記が無いが、既存の一覧取得系UseCaseと同様にRepositoryへの
 * 薄い委譲として置く（既存パターンとの整合性を優先した実装判断）。
 */
class GetUserUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(userId: String): User = userRepository.getUser(userId)
}
