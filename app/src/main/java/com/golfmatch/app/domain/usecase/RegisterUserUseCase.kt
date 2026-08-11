package com.golfmatch.app.domain.usecase

import com.golfmatch.app.domain.model.AuthSession
import com.golfmatch.app.domain.model.Purpose
import com.golfmatch.app.domain.repository.AuthRepository
import javax.inject.Inject

/** プロフィール初回登録とアカウント作成（本人確認後、ADR-0003） */
class RegisterUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        registrationToken: String,
        name: String,
        gender: String,
        age: Int,
        areaId: String,
        averageScore: Int,
        purpose: Purpose,
        introduction: String
    ): AuthSession = authRepository.registerUser(
        registrationToken, name, gender, age, areaId, averageScore, purpose, introduction
    )
}
