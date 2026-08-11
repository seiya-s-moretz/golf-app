package com.golfmatch.app.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.golfmatch.app.domain.model.RoundEvent
import com.golfmatch.app.ui.theme.GolfMatchTheme
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * ラウンド募集カード（技術設計書4章 `ui/component`、`D:\勉強\golf\基本設計書.md` 3-1章）。
 *
 * 表示項目: ゴルフ倶楽部名・日時・費用・募集人数（現在人数/募集人数）。
 */
@Composable
fun RoundEventCard(
    roundEvent: RoundEvent,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = roundEvent.clubName, style = MaterialTheme.typography.titleMedium)
            Text(text = formatDateTime(roundEvent.datetime), style = MaterialTheme.typography.bodyMedium)
            Text(text = formatFee(roundEvent.fee), style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "募集人数 ${roundEvent.current}人 / ${roundEvent.capacity}人",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun formatDateTime(instant: Instant): String {
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val date = local.date
    val time = local.time
    return "%04d/%02d/%02d %02d:%02d".format(date.year, date.monthNumber, date.dayOfMonth, time.hour, time.minute)
}

private fun formatFee(fee: Int): String = "%,d円".format(fee)

@Preview(showBackground = true)
@Composable
private fun RoundEventCardPreview() {
    GolfMatchTheme {
        RoundEventCard(
            roundEvent = RoundEvent(
                eventId = "event-1",
                clubName = "さいたまゴルフ倶楽部",
                datetime = Instant.parse("2026-09-01T08:00:00Z"),
                fee = 8000,
                capacity = 4,
                current = 2,
                createdBy = "user-1",
                createdAt = Instant.parse("2026-08-01T00:00:00Z")
            )
        )
    }
}
