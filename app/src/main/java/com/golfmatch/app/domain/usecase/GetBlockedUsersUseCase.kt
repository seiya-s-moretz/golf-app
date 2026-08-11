package com.golfmatch.app.domain.usecase

import com.golfmatch.app.domain.model.User
import com.golfmatch.app.domain.repository.UserRepository
import javax.inject.Inject

/**
 * 自分がブロックしたユーザー一覧取得（技術設計書 6-3章 `GET /users/me/blocks`）。
 *
 * 技術設計書4章のusecase一覧には明記が無いが、[GetMatchRequestsUseCase]等の他の一覧取得系UseCaseと
 * 同様にRepositoryへの薄い委譲としてUseCase層に置く（既存パターンとの整合性を優先した実装判断）。
 * `UserRepository.getBlockedUsers()`自体は既存実装済み。
 */
class GetBlockedUsersUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(): List<User> = userRepository.getBlockedUsers()
}
