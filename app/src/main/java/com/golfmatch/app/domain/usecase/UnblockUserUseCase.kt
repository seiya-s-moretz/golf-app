package com.golfmatch.app.domain.usecase

import com.golfmatch.app.domain.repository.UserRepository
import javax.inject.Inject

/** ユーザーブロックの解除 */
class UnblockUserUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(userId: String) = userRepository.unblockUser(userId)
}
