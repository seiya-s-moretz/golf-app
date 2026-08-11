package com.golfmatch.app.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.golfmatch.app.domain.model.Area

/**
 * エリア選択共通部品（技術設計書4章 `ui/component/AreaPickerField.kt`、ADR-0002）。
 *
 * マイページ編集画面での住まい（エリア）選択に利用する。選択肢は`GetAreasUseCase`で取得した
 * `is_active=true` のエリアのみ（`AreaRepository`側で担保済み）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AreaPickerField(
    areas: List<Area>,
    selectedAreaName: String,
    modifier: Modifier = Modifier,
    onAreaSelected: (Area) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedAreaName,
            onValueChange = {},
            readOnly = true,
            label = { Text("住まい（エリア）") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            areas.forEach { area ->
                DropdownMenuItem(
                    text = { Text("${area.prefecture} ${area.areaName}") },
                    onClick = {
                        onAreaSelected(area)
                        expanded = false
                    }
                )
            }
        }
    }
}
