package top.yogiczy.mytv.tv.ui.screens.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.focusable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import top.yogiczy.mytv.tv.ui.rememberChildPadding
import top.yogiczy.mytv.tv.ui.screens.settings.LocalSettings
import top.yogiczy.mytv.tv.ui.screens.settings.SettingsCategories
import top.yogiczy.mytv.tv.ui.theme.MyTVTheme
import top.yogiczy.mytv.tv.ui.utils.focusOnLaunchedSaveable
import top.yogiczy.mytv.tv.ui.utils.handleKeyEvents
import top.yogiczy.mytv.tv.ui.utils.ifElse
import top.yogiczy.mytv.tv.ui.utils.saveFocusRestorer

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SettingsCategoryList(
    modifier: Modifier = Modifier,
    currentCategoryProvider: () -> SettingsCategories = { SettingsCategories.entries.first() },
    onCategorySelected: (SettingsCategories) -> Unit = {},
) {
    val childPadding = rememberChildPadding()

    LazyColumn(
        contentPadding = PaddingValues(top = childPadding.top, bottom = childPadding.bottom),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.ifElse(
            LocalSettings.current.uiFocusOptimize,
            Modifier.saveFocusRestorer(),
        ),
    ) {
        itemsIndexed(SettingsCategories.entries) { index, category ->
            val isSelected by remember { derivedStateOf { currentCategoryProvider() == category } }

            SettingsCategoryItem(
                modifier = Modifier.ifElse(index == 0, Modifier.focusOnLaunchedSaveable()),
                icon = category.icon,
                title = category.title,
                isSelectedProvider = { isSelected },
                onCategorySelected = { onCategorySelected(category) },
            )
        }
    }
}

@Composable
private fun SettingsCategoryItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    isSelectedProvider: () -> Boolean = { false },
    onCategorySelected: () -> Unit = {},
) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(isFocused) {
        if (isFocused) {
            // 极短的延迟，确保只有真正停下的焦点才触发选中逻辑
            delay(50)
            onCategorySelected()
        }
    }

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
                onRight = {
                    // 立即同步分类，不依赖 50ms 定时器
                    onCategorySelected()
                    focusManager.moveFocus(FocusDirection.Right)
                },
                onSelect = {
                    // 确认键同样立即同步分类
                    onCategorySelected()
                    focusManager.moveFocus(FocusDirection.Right)
                },
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(icon, contentDescription = title, tint = contentColor)
        Text(
            text = title,
            color = contentColor,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Preview
@Composable
private fun SettingsCategoryItemPreview() {
    MyTVTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SettingsCategoryItem(
                icon = SettingsCategories.ABOUT.icon,
                title = SettingsCategories.ABOUT.title,
            )

            SettingsCategoryItem(
                icon = SettingsCategories.ABOUT.icon,
                title = SettingsCategories.ABOUT.title,
                isSelectedProvider = { true },
            )
        }
    }
}

@Preview
@Composable
private fun SettingsCategoryListPreview() {
    MyTVTheme {
        SettingsCategoryList()
    }
}