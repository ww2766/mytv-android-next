package top.yogiczy.mytv.tv.ui.screens.settings

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import top.yogiczy.mytv.core.data.entities.channel.ChannelGroupList
import top.yogiczy.mytv.tv.ui.rememberChildPadding
import top.yogiczy.mytv.tv.ui.screens.settings.components.SettingsCategoryContent
import top.yogiczy.mytv.tv.ui.screens.settings.components.SettingsCategoryList
import top.yogiczy.mytv.tv.ui.utils.captureBackKey
import top.yogiczy.mytv.tv.ui.utils.customBackground

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    channelGroupListProvider: () -> ChannelGroupList = { ChannelGroupList() },
    onClose: () -> Unit = {},
    settingsViewModel: SettingsViewModel = viewModel(),
) {
    val childPadding = rememberChildPadding()
    var currentCategory by remember { mutableStateOf(SettingsCategories.entries.first()) }

    var listHasFocus by remember { mutableStateOf(false) }
    var deferredCategory by remember { mutableStateOf(currentCategory) }

    LaunchedEffect(currentCategory, listHasFocus) {
        if (!listHasFocus) {
            // 如果焦点已经移出列表（进入右侧内容区），立即同步分类状态
            deferredCategory = currentCategory
        } else {
            // 否则（在左侧滑动时）执行防抖，避免性能开销
            delay(150)
            deferredCategory = currentCategory
        }
    }

    Box(
        modifier = modifier
            .captureBackKey { onClose() }
            .pointerInput(Unit) { detectTapGestures { } }
            .fillMaxSize()
            .customBackground()
            .padding(start = childPadding.start, end = childPadding.end),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(52.dp),
        ) {
            SettingsCategoryList(
                modifier = Modifier
                    .width(216.dp)
                    .onFocusChanged { listHasFocus = it.hasFocus },
                currentCategoryProvider = { currentCategory },
                onCategorySelected = { currentCategory = it },
            )

            SettingsCategoryContent(
                currentCategoryProvider = { deferredCategory },
                channelGroupListProvider = channelGroupListProvider,
            )
        }
    }
}