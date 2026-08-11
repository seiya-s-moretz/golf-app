package com.golfmatch.app.ui.screen.mypage

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.golfmatch.app.ui.viewmodel.MyPageUiState
import kotlinx.datetime.Instant

/**
 * マイページ（プロフィール表示・編集）画面（技術設計書3-1章・7-1章、`D:\勉強\golf\基本設計書.md` 3-4章）。
 *
 * アベレージスコアが自己申告であることをUI上に明示する（PRD 3-1章）。
 */
@Composable
fun MyPageScreen(
    uiState: MyPageUiState,
    onNameChange: (String) -> Unit = {},
    onGenderChange: (String) -> Unit = {},
    onAgeChange: (String) -> Unit = {},
    onAreaSelected: (Area) -> Unit = {},
    onAverageScoreChange: (String) -> Unit = {},
    onPurposeSelected: (Purpose) -> Unit = {},
    onIntroductionChange: (String) -> Unit = {},
    onSaveClick: () -> Unit = {},
    onBlockedUsersClick: () -> Unit = {},
    onReportAdminClick: () -> Unit = {}
) {
    Scaffold { innerPadding ->
        when {
            uiState.isLoading -> LoadingContent(innerPadding)
            uiState.errorMessage != null && uiState.name.isEmpty() -> ErrorContent(innerPadding, uiState.errorMessage)
            else -> ProfileForm(
                innerPadding = innerPadding,
                uiState = uiState,
                onNameChange = onNameChange,
                onGenderChange = onGenderChange,
                onAgeChange = onAgeChange,
                onAreaSelected = onAreaSelected,
                onAverageScoreChange = onAverageScoreChange,
                onPurposeSelected = onPurposeSelected,
                onIntroductionChange = onIntroductionChange,
                onSaveClick = onSaveClick,
                onBlockedUsersClick = onBlockedUsersClick,
                onReportAdminClick = onReportAdminClick
            )
        }
    }
}

@Composable
private fun LoadingContent(padding: androidx.compose.foundation.layout.PaddingValues) {
    Box(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorContent(padding: androidx.compose.foundation.layout.PaddingValues, message: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = message, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ProfileForm(
    innerPadding: androidx.compose.foundation.layout.PaddingValues,
    uiState: MyPageUiState,
    onNameChange: (String) -> Unit,
    onGenderChange: (String) -> Unit,
    onAgeChange: (String) -> Unit,
    onAreaSelected: (Area) -> Unit,
    onAverageScoreChange: (String) -> Unit,
    onPurposeSelected: (Purpose) -> Unit,
    onIntroductionChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    onBlockedUsersClick: () -> Unit,
    onReportAdminClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(color = MaterialTheme.colorScheme.secondaryContainer, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Filled.Person, contentDescription = null)
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (uiState.phoneVerified) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                        contentDescription = null,
                        tint = if (uiState.phoneVerified) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (uiState.phoneVerified) "本人確認済み" else "本人確認未完了",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
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
            if (uiState.isAverageScoreSelfReported) {
                Text(
                    text = "※アベレージスコアは自己申告です",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
        if (uiState.saveSuccess) {
            Text(text = "プロフィールを保存しました", color = MaterialTheme.colorScheme.primary)
        }

        Button(
            onClick = onSaveClick,
            enabled = !uiState.isSaving,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState.isSaving) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp))
            } else {
                Text("保存する")
            }
        }

        OutlinedButton(
            onClick = onBlockedUsersClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("ブロック済みユーザー一覧")
        }

        // `User.is_admin=true`の運営メンバーにのみ表示する（技術設計書3-4章、ADR-0007）
        if (uiState.isAdmin) {
            OutlinedButton(
                onClick = onReportAdminClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("通報管理")
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

@Preview(showBackground = true, name = "表示・編集")
@Composable
private fun MyPageScreenPreview() {
    GolfMatchTheme {
        MyPageScreen(
            uiState = MyPageUiState(
                name = "山田太郎",
                gender = "male",
                age = "30",
                areaId = "area-1",
                areaName = "さいたま市",
                areaOptions = previewAreas(),
                averageScore = "90",
                purpose = Purpose.CASUAL,
                introduction = "よろしくお願いします",
                phoneVerified = true
            )
        )
    }
}

@Preview(showBackground = true, name = "ローディング")
@Composable
private fun MyPageScreenLoadingPreview() {
    GolfMatchTheme {
        MyPageScreen(uiState = MyPageUiState(isLoading = true))
    }
}

@Preview(showBackground = true, name = "エラー")
@Composable
private fun MyPageScreenErrorPreview() {
    GolfMatchTheme {
        MyPageScreen(uiState = MyPageUiState(errorMessage = "プロフィールの取得に失敗しました"))
    }
}
