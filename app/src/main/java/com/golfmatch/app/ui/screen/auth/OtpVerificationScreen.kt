package com.golfmatch.app.ui.screen.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.golfmatch.app.ui.theme.GolfMatchTheme
import com.golfmatch.app.ui.viewmodel.OtpVerificationUiState

/**
 * OTP認証画面（技術設計書3-2章・7-2章、ADR-0003）。
 *
 * SMSで受信した確認コードを入力し検証する（`POST /auth/phone/verify` または `POST /auth/login`。
 * 新規/既存ユーザー分岐の詳細は[com.golfmatch.app.ui.viewmodel.OtpVerificationViewModel]のKDoc参照）。
 */
@Composable
fun OtpVerificationScreen(
    uiState: OtpVerificationUiState,
    onOtpCodeChange: (String) -> Unit = {},
    onVerifyClick: () -> Unit = {}
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "確認コードを入力してください", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "${uiState.phoneNumber} 宛にSMSを送信しました",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = uiState.otpCode,
                onValueChange = onOtpCodeChange,
                label = { Text("確認コード") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            if (uiState.errorMessage != null) {
                Text(text = uiState.errorMessage, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = onVerifyClick,
                enabled = !uiState.isVerifying,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isVerifying) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp))
                } else {
                    Text("認証する")
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "入力")
@Composable
private fun OtpVerificationScreenPreview() {
    GolfMatchTheme {
        OtpVerificationScreen(
            uiState = OtpVerificationUiState(phoneNumber = "09012345678", otpCode = "123456")
        )
    }
}

@Preview(showBackground = true, name = "検証中")
@Composable
private fun OtpVerificationScreenVerifyingPreview() {
    GolfMatchTheme {
        OtpVerificationScreen(
            uiState = OtpVerificationUiState(phoneNumber = "09012345678", otpCode = "123456", isVerifying = true)
        )
    }
}

@Preview(showBackground = true, name = "エラー")
@Composable
private fun OtpVerificationScreenErrorPreview() {
    GolfMatchTheme {
        OtpVerificationScreen(
            uiState = OtpVerificationUiState(
                phoneNumber = "09012345678",
                errorMessage = "確認コードを入力してください"
            )
        )
    }
}
