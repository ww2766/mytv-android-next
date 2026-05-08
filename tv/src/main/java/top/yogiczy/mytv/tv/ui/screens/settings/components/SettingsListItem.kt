package top.yogiczy.mytv.tv.ui.screens.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import top.yogiczy.mytv.tv.ui.material.LocalPopupManager
import top.yogiczy.mytv.tv.ui.material.SimplePopup
import top.yogiczy.mytv.tv.ui.theme.MyTVTheme
import top.yogiczy.mytv.tv.ui.utils.handleKeyEvents

/**
 * 稳定化版本（Provider 模式）
 */
@Composable
fun SettingsListItem(
    modifier: Modifier = Modifier,
    headlineContentProvider: () -> String,
    supportingContentProvider: (() -> String?)? = null,
    trailingContentProvider: @Composable () -> Unit = {},
    trailingIconProvider: (() -> ImageVector?)? = null,
    onSelected: (() -> Unit)? = null,
    onLongSelected: () -> Unit = {},
    locKProvider: () -> Boolean = { false },
    remoteConfig: Boolean = false,
) {
    val popupManager = LocalPopupManager.current
    val focusRequester = remember { FocusRequester() }

    var showPush by remember { mutableStateOf(false) }

    var isFocused by remember { mutableStateOf(false) }

    val backgroundColor = if (isFocused) {
        MaterialTheme.colorScheme.onSurface
    } else {
        androidx.compose.ui.graphics.Color.Transparent
    }

    val contentColor = if (isFocused) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val supportingContent = supportingContentProvider?.invoke()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(ListItemDefaults.shape().shape)
            .background(backgroundColor)
            .focusRequester(focusRequester)
            .onFocusChanged {
                isFocused = it.isFocused || it.hasFocus
            }
            .focusable()
            .handleKeyEvents(
                isFocused = { isFocused },
                focusRequester = focusRequester,
                onSelect = {
                    if (onSelected != null) onSelected()
                    else if (remoteConfig) {
                        popupManager.push(focusRequester, true)
                        showPush = true
                    }
                },
                onLongSelect = { onLongSelected() },
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = headlineContentProvider(),
                color = contentColor,
                style = MaterialTheme.typography.titleMedium,
            )
            if (supportingContent != null) {
                Text(
                    text = supportingContent,
                    color = contentColor.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            androidx.compose.runtime.CompositionLocalProvider(
                LocalContentColor provides contentColor
            ) {
                trailingContentProvider()
                trailingIconProvider?.invoke()?.let {
                    Icon(it, contentDescription = null, modifier = Modifier.size(16.dp))
                }

                if (locKProvider()) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                }

                if (remoteConfig) {
                    Icon(
                        Icons.AutoMirrored.Default.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }

    SimplePopup(
        visibleProvider = { showPush },
        onDismissRequest = { showPush = false },
    ) {
        SettingsCategoryPush()
    }
}

/**
 * 兼容旧版本的重载 (支持 String 和 Composable trailingContent)
 */
@Composable
fun SettingsListItem(
    modifier: Modifier = Modifier,
    headlineContent: String,
    supportingContent: String? = null,
    trailingContent: @Composable () -> Unit = {},
    trailingIcon: ImageVector? = null,
    onSelected: (() -> Unit)? = null,
    onLongSelected: () -> Unit = {},
    locK: Boolean = false,
    remoteConfig: Boolean = false,
) {
    SettingsListItem(
        modifier = modifier,
        headlineContentProvider = { headlineContent },
        supportingContentProvider = { supportingContent },
        trailingContentProvider = trailingContent,
        trailingIconProvider = { trailingIcon },
        onSelected = onSelected,
        onLongSelected = onLongSelected,
        locKProvider = { locK },
        remoteConfig = remoteConfig,
    )
}

/**
 * 兼容旧版本的重载 (支持 String trailingContent)
 */
@Composable
fun SettingsListItem(
    modifier: Modifier = Modifier,
    headlineContent: String,
    supportingContent: String? = null,
    trailingContent: String,
    trailingIcon: ImageVector? = null,
    onSelected: () -> Unit = {},
    onLongSelected: () -> Unit = {},
    locK: Boolean = false,
    remoteConfig: Boolean = false,
) {
    SettingsListItem(
        modifier = modifier,
        headlineContent = headlineContent,
        supportingContent = supportingContent,
        trailingContent = { Text(trailingContent) },
        trailingIcon = trailingIcon,
        onSelected = onSelected,
        onLongSelected = onLongSelected,
        locK = locK,
        remoteConfig = remoteConfig,
    )
}

@Preview
@Composable
private fun SettingsListItemPreview() {
    MyTVTheme {
        SettingsListItem(
            headlineContent = "关于",
            supportingContent = "版本号",
            trailingContent = "1.0.0",
            trailingIcon = Icons.Default.Circle,
            remoteConfig = true,
            locK = true,
        )
    }
}