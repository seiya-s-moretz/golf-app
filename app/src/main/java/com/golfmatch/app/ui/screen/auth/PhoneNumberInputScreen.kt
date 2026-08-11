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
import com.golfmatch.app.ui.viewmodel.PhoneNumberInputUiState

/**
 * 電話番号入力画面（技術設計書3-2章・7-2章、ADR-0003）。
 *
 * 本人確認フローの入口。電話番号を入力しSMSでOTPを送信する（`POST /auth/phone/otp`）。
 */
@Composable
fun PhoneNumberInputScreen(
    uiState: PhoneNumberInputUiState,
    onPhoneNumberChange: (String) -> Unit = {},
    onSubmitClick: () -> Unit = {}
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "電話番号を入力してください", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "SMSで確認コードをお送りします",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = uiState.phoneNumber,
                onValueChange = onPhoneNumberChange,
                label = { Text("電話番号") },
                placeholder = { Text("090xxxxxxxx") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )

            if (uiState.errorMessage != null) {
                Text(text = uiState.errorMessage, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = onSubmitClick,
                enabled = !uiState.isSubmitting,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp))
                } else {
                    Text("SMSを送信する")
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "入力")
@Composable
private fun PhoneNumberInputScreenPreview() {
    GolfMatchTheme {
        PhoneNumberInputScreen(uiState = PhoneNumberInputUiState(phoneNumber = "09012345678"))
    }
}

@Preview(showBackground = true, name = "送信中")
@Composable
private fun PhoneNumberInputScreenSubmittingPreview() {
    GolfMatchTheme {
        PhoneNumberInputScreen(uiState = PhoneNumberInputUiState(phoneNumber = "09012345678", isSubmitting = true))
    }
}

@Preview(showBackground = true, name = "エラー")
@Composable
private fun PhoneNumberInputScreenErrorPreview() {
    GolfMatchTheme {
        PhoneNumberInputScreen(uiState = PhoneNumberInputUiState(errorMessage = "電話番号を入力してください"))
    }
}
