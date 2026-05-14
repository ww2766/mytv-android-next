package top.yogiczy.mytv.tv.ui.screens.epg.components

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import top.yogiczy.mytv.core.data.entities.epg.EpgProgramme
import top.yogiczy.mytv.core.data.entities.epg.EpgProgramme.Companion.isLive
import top.yogiczy.mytv.tv.ui.theme.MyTVTheme
import top.yogiczy.mytv.tv.ui.utils.focusOnLaunchedSaveable
import top.yogiczy.mytv.tv.ui.utils.handleKeyEvents
import top.yogiczy.mytv.tv.ui.utils.ifElse
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun EpgProgrammeItem(
    modifier: Modifier = Modifier,
    epgProgrammeProvider: () -> EpgProgramme = { EpgProgramme() },
    supportPlaybackProvider: () -> Boolean = { false },
    isPlaybackProvider: () -> Boolean = { false },
    hasReservedProvider: () -> Boolean = { false },
    onPlayback: () -> Unit = {},
    onReserve: () -> Unit = {},
    isInitialFocusProvider: () -> Boolean = { false },
    onFocused: () -> Unit = {},
) {
    val programme = epgProgrammeProvider()
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    var isFocused by remember { mutableStateOf(false) }
    val isLive = programme.isLive()

    val backgroundColor = if (isFocused) {
        MaterialTheme.colorScheme.onSurface
    } else if (isLive) {
        MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.1f)
    } else {
        androidx.compose.ui.graphics.Color.Transparent
    }

    val contentColor = if (isFocused) {
        MaterialTheme.colorScheme.surface
    } else if (isLive) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = modifier
            .ifElse(isInitialFocusProvider(), Modifier.focusOnLaunchedSaveable())
            .fillMaxWidth()
            .clip(ListItemDefaults.shape().shape)
            .background(backgroundColor)
            .onFocusChanged {
                isFocused = it.isFocused || it.hasFocus
                if (isFocused) onFocused()
            }
            .focusable()
            .handleKeyEvents(
                onSelect = {
                    if (programme.startAt < System.currentTimeMillis() && supportPlaybackProvider()) onPlayback()
                    else if (programme.startAt > System.currentTimeMillis()) onReserve()
                }
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "${timeFormat.format(programme.startAt)}    ${programme.title}",
            color = contentColor,
            maxLines = if (isFocused) Int.MAX_VALUE else 1,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f).padding(end = 8.dp)
        )

        androidx.compose.runtime.CompositionLocalProvider(
            LocalContentColor provides contentColor
        ) {
            if (isLive) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
            } else if (isPlaybackProvider()) {
                Text("正在回放", style = MaterialTheme.typography.bodyMedium)
            } else if (programme.startAt < System.currentTimeMillis() && supportPlaybackProvider()) {
                Text("回放", style = MaterialTheme.typography.bodyMedium)
            } else if (programme.startAt > System.currentTimeMillis()) {
                if (hasReservedProvider()) Text("已预约", style = MaterialTheme.typography.bodyMedium)
                else Text("预约", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Preview
@Composable
private fun EpgProgrammeItemPreview() {
    MyTVTheme {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            EpgProgrammeItem(
                epgProgrammeProvider = { EpgProgramme.EXAMPLE },
            )
            EpgProgrammeItem(
                epgProgrammeProvider = {
                    EpgProgramme.EXAMPLE.copy(
                        startAt = System.currentTimeMillis() - 200000,
                        endAt = System.currentTimeMillis() - 100000,
                    )
                },
            )
            EpgProgrammeItem(
                epgProgrammeProvider = {
                    EpgProgramme.EXAMPLE.copy(
                        startAt = System.currentTimeMillis() + 100000,
                        endAt = System.currentTimeMillis() + 200000,
                    )
                },
            )
            EpgProgrammeItem(
                epgProgrammeProvider = {
                    EpgProgramme.EXAMPLE.copy(
                        startAt = System.currentTimeMillis() + 100000,
                        endAt = System.currentTimeMillis() + 200000,
                    )
                },
                hasReservedProvider = { true },
            )
        }
    }
}