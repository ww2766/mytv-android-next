package top.yogiczy.mytv.tv.ui.screens.epg.components

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import top.yogiczy.mytv.tv.ui.theme.MyTVTheme
import top.yogiczy.mytv.tv.ui.utils.handleKeyEvents
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun EpgDayItem(
    modifier: Modifier = Modifier,
    dayProvider: () -> String = { "" }, // 格式：E MM-dd
    isSelectedProvider: () -> Boolean = { false },
    onDaySelected: () -> Unit = {},
) {
    val day = dayProvider()

    val dateFormat = SimpleDateFormat("E MM-dd", Locale.getDefault())
    val today = dateFormat.format(System.currentTimeMillis())
    val tomorrow =
        dateFormat.format(System.currentTimeMillis() + 24 * 3600 * 1000)
    val dayAfterTomorrow =
        dateFormat.format(System.currentTimeMillis() + 48 * 3600 * 1000)

    var isFocused by remember { mutableStateOf(false) }
    val isSelected = isSelectedProvider()

    val backgroundColor = if (isFocused) {
        MaterialTheme.colorScheme.onSurface
    } else if (isSelected) {
        MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.1f)
    } else {
        androidx.compose.ui.graphics.Color.Transparent
    }

    val contentColor = if (isFocused) {
        MaterialTheme.colorScheme.surface
    } else if (isSelected) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val lines = day.split(" ")
    val title = when (day) {
        today -> "今天"
        tomorrow -> "明天"
        dayAfterTomorrow -> "后天"
        else -> lines[0]
    }
    val subtitle = if (lines.size > 1) lines[1] else ""

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(ListItemDefaults.shape().shape)
            .background(backgroundColor)
            .onFocusChanged {
                val focused = it.isFocused || it.hasFocus
                if (focused && !isFocused) onDaySelected()
                isFocused = focused
            }
            .focusable()
            .handleKeyEvents(onSelect = onDaySelected)
            .padding(vertical = 8.dp, horizontal = 8.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            color = contentColor,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
        )
        if (subtitle.isNotEmpty()) {
            Text(
                text = subtitle,
                color = contentColor.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Preview
@Composable
private fun EpgDayItemPreview() {
    MyTVTheme {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            EpgDayItem(
                dayProvider = { "周一 07-09" },
            )

            EpgDayItem(
                dayProvider = { "周一 07-09" },
                isSelectedProvider = { true },
            )
        }
    }
}