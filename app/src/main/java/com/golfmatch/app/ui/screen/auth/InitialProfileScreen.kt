package com.golfmatch.app.ui.screen.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.golfmatch.app.domain.model.Area
import com.golfmatch.app.domain.model.Purpose
import com.golfmatch.app.ui.component.AreaPickerField
import com.golfmatch.app.ui.theme.GolfMatchTheme
import com.golfmatch.app.ui.viewmodel.InitialProfileUiState
import kotlinx.datetime.Instant

/**
 * プロフィール初期登録画面（技術設計書3-2章・7-2章）。
 *
 * 本人確認完了後の初回プロフィール入力。入力項目はマイページ編集画面
 * （[com.golfmatch.app.ui.screen.mypage.MyPageScreen]）と揃える
 * （アイコン・名前・性別・年齢・エリア・アベレージスコア・目的・自己紹介）。
 */
@Composable
fun InitialProfileScreen(
    uiState: InitialProfileUiState,
    onNameChange: (String) -> Unit = {},
    onGenderChange: (String) -> Unit = {},
    onAgeChange: (String) -> Unit = {},
    onAreaSelected: (Area) -> Unit = {},
    onAverageScoreChange: (String) -> Unit = {},
    onPurposeSelected: (Purpose) -> Unit = {},
    onIntroductionChange: (String) -> Unit = {},
    onSubmitClick: () -> Unit = {}
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "プロフィールを登録してください", style = MaterialTheme.typography.titleMedium)

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(color = MaterialTheme.colorScheme.secondaryContainer, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Filled.Person, contentDescription = null)
            }

            OutlinedTextField(
                value = uiState.name,
                onValueChange = onNameChange,
                label = { Text("名前") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.gender,
                onValueChange = onGenderChange,
                label = { Text("性別") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.age,
                onValueChange = onAgeChange,
                label = { Text("年齢") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            AreaPickerField(
                areas = uiState.areaOptions,
                selectedAreaName = uiState.areaName,
                onAreaSelected = onAreaSelected,
                modifier = Modifier.fillMaxWidth()
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = uiState.averageScore,
                    onValueChange = onAverageScoreChange,
                    label = { Text("アベレージスコア") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "※アベレージスコアは自己申告です",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "目的", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Purpose.entries.forEach { purpose ->
                        FilterChip(
                            selected = uiState.purpose == purpose,
                            onClick = { onPurposeSelected(purpose) },
                            label = { Text(purpose.label) }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = uiState.introduction,
                onValueChange = onIntroductionChange,
                label = { Text("自己紹介") },
                modifier = Modifier.fillMaxWidth().height(120.dp)
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
                    Text("登録する")
                }
            }
        }
    }
}

private fun previewAreas() = listOf(
    Area(
        areaId = "area-1",
        prefecture = "埼玉県",
        areaName = "さいたま市",
        displayOrder = 1,
        isActive = true,
        createdAt = Instant.parse("2026-08-01T00:00:00Z")
    )
)

@Preview(showBackground = true, name = "入力")
@Composable
private fun InitialProfileScreenPreview() {
    GolfMatchTheme {
        InitialProfileScreen(
            uiState = InitialProfileUiState(
                name = "山田太郎",
                gender = "male",
                age = "30",
                areaId = "area-1",
                areaName = "さいたま市",
                areaOptions = previewAreas(),
                averageScore = "90",
                purpose = Purpose.CASUAL,
                introduction = "よろしくお願いします"
            )
        )
    }
}

@Preview(showBackground = true, name = "登録中")
@Composable
private fun InitialProfileScreenSubmittingPreview() {
    GolfMatchTheme {
        InitialProfileScreen(uiState = InitialProfileUiState(name = "山田太郎", isSubmitting = true))
    }
}

@Preview(showBackground = true, name = "エラー")
@Composable
private fun InitialProfileScreenErrorPreview() {
    GolfMatchTheme {
        InitialProfileScreen(uiState = InitialProfileUiState(errorMessage = "入力内容を確認してください"))
    }
}
