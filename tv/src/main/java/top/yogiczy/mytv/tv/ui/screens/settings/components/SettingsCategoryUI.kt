package top.yogiczy.mytv.tv.ui.screens.settings.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Switch
import androidx.tv.material3.Text
import top.yogiczy.mytv.core.data.utils.Constants
import top.yogiczy.mytv.core.util.utils.humanizeMs
import top.yogiczy.mytv.tv.ui.material.LocalPopupManager
import top.yogiczy.mytv.tv.ui.screens.components.SelectDialog
import top.yogiczy.mytv.tv.ui.screens.settings.SettingsViewModel
import top.yogiczy.mytv.tv.ui.utils.Configs
import java.text.DecimalFormat

@Composable
fun SettingsCategoryUI(
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel = viewModel(),
) {
    SettingsContentList(modifier) {
        item(key = "uiShowEpgProgrammeProgress") {
            SettingsListItem(
                modifier = Modifier.focusRequester(it),
                headlineContentProvider = { "节目进度" },
                supportingContentProvider = { "在频道项底部显示当前节目进度条" },
                trailingContentProvider = {
                    Switch(settingsViewModel.uiShowEpgProgrammeProgress, null)
                },
                onSelected = {
                    settingsViewModel.uiShowEpgProgrammeProgress =
                        !settingsViewModel.uiShowEpgProgrammeProgress
                },
            )
        }

        item(key = "uiShowEpgProgrammePermanentProgress") {
            SettingsListItem(
                headlineContentProvider = { "常驻底部节目进度" },
                supportingContentProvider = { "在播放器底部显示当前节目进度条" },
                trailingContentProvider = {
                    Switch(settingsViewModel.uiShowEpgProgrammePermanentProgress, null)
                },
                onSelected = {
                    settingsViewModel.uiShowEpgProgrammePermanentProgress =
                        !settingsViewModel.uiShowEpgProgrammePermanentProgress
                },
            )
        }

        item(key = "uiShowChannelLogo") {
            SettingsListItem(
                headlineContentProvider = { "台标显示" },
                trailingContentProvider = {
                    Switch(settingsViewModel.uiShowChannelLogo, null)
                },
                onSelected = {
                    settingsViewModel.uiShowChannelLogo = !settingsViewModel.uiShowChannelLogo
                },
            )
        }

        item(key = "uiUseClassicPanelScreen") {
            SettingsListItem(
                headlineContentProvider = { "经典选台界面" },
                supportingContentProvider = { "将选台界面替换为经典三段式结构" },
                trailingContentProvider = {
                    Switch(settingsViewModel.uiUseClassicPanelScreen, null)
                },
                onSelected = {
                    settingsViewModel.uiUseClassicPanelScreen =
                        !settingsViewModel.uiUseClassicPanelScreen
                },
            )
        }

        item(key = "uiTimeShowMode") {
            val timeShowRangeSeconds = Constants.UI_TIME_SCREEN_SHOW_DURATION / 1000

            SettingsListItem(
                headlineContentProvider = { "时间显示" },
                supportingContentProvider = {
                    when (settingsViewModel.uiTimeShowMode) {
                        Configs.UiTimeShowMode.HIDDEN -> "不显示时间"
                        Configs.UiTimeShowMode.ALWAYS -> "总是显示时间"
                        Configs.UiTimeShowMode.EVERY_HOUR -> "整点前后${timeShowRangeSeconds}s显示时间"
                        Configs.UiTimeShowMode.HALF_HOUR -> "半点前后${timeShowRangeSeconds}s显示时间"
                    }
                },
                trailingContentProvider = {
                    val text = when (settingsViewModel.uiTimeShowMode) {
                        Configs.UiTimeShowMode.HIDDEN -> "隐藏"
                        Configs.UiTimeShowMode.ALWAYS -> "常显"
                        Configs.UiTimeShowMode.EVERY_HOUR -> "整点"
                        Configs.UiTimeShowMode.HALF_HOUR -> "半点"
                    }
                    Text(text)
                },
                onSelected = {
                    settingsViewModel.uiTimeShowMode =
                        Configs.UiTimeShowMode.entries.let {
                            it[(it.indexOf(settingsViewModel.uiTimeShowMode) + 1) % it.size]
                        }
                },
            )
        }

        item(key = "uiScreenAutoCloseDelay") {
            val popupManager = LocalPopupManager.current
            val focusRequester = remember { FocusRequester() }
            var visible by remember { mutableStateOf(false) }

            SettingsListItem(
                modifier = Modifier.focusRequester(focusRequester),
                headlineContentProvider = { "超时自动关闭界面" },
                supportingContentProvider = { "影响选台界面，快捷操作等界面" },
                trailingContentProvider = {
                    Text(settingsViewModel.uiScreenAutoCloseDelay.humanizeMs())
                },
                onSelected = {
                    popupManager.push(focusRequester, true)
                    visible = true
                },
                remoteConfig = true,
            )

            SelectDialog(
                visibleProvider = { visible },
                onDismissRequest = { visible = false },
                title = "超时自动关闭界面",
                currentDataProvider = { settingsViewModel.uiScreenAutoCloseDelay },
                dataListProvider = { listOf(5, 10, 15, 20, 25, 30).map { it.toLong() * 1000 } },
                dataText = { it.humanizeMs() },
                onDataSelected = {
                    settingsViewModel.uiScreenAutoCloseDelay = it
                    visible = false
                },
            )
        }

        item {
            val popupManager = LocalPopupManager.current
            val focusRequester = remember { FocusRequester() }
            var visible by remember { mutableStateOf(false) }

            SettingsListItem(
                modifier = Modifier.focusRequester(focusRequester),
                headlineContent = "界面整体缩放比例",
                trailingContent = when (settingsViewModel.uiDensityScaleRatio) {
                    0f -> "自适应"
                    else -> "×${DecimalFormat("#.#").format(settingsViewModel.uiDensityScaleRatio)}"
                },
                onSelected = {
                    popupManager.push(focusRequester, true)
                    visible = true
                },
                remoteConfig = true,
            )

            SelectDialog(
                visibleProvider = { visible },
                onDismissRequest = { visible = false },
                title = "界面整体缩放比例",
                currentDataProvider = { settingsViewModel.uiDensityScaleRatio },
                dataListProvider = { listOf(0f) + (5..20).map { it * 0.1f } },
                dataText = {
                    when (it) {
                        0f -> "自适应"
                        else -> "×${DecimalFormat("#.#").format(it)}"
                    }
                },
                onDataSelected = {
                    settingsViewModel.uiDensityScaleRatio = it
                    visible = false
                },
            )
        }

        item {
            val popupManager = LocalPopupManager.current
            val focusRequester = remember { FocusRequester() }
            var visible by remember { mutableStateOf(false) }

            SettingsListItem(
                modifier = Modifier.focusRequester(focusRequester),
                headlineContent = "界面字体缩放比例",
                trailingContent = "×${DecimalFormat("#.#").format(settingsViewModel.uiFontScaleRatio)}",
                onSelected = {
                    popupManager.push(focusRequester, true)
                    visible = true
                },
                remoteConfig = true,
            )

            SelectDialog(
                visibleProvider = { visible },
                onDismissRequest = { visible = false },
                title = "界面字体缩放比例",
                currentDataProvider = { settingsViewModel.uiFontScaleRatio },
                dataListProvider = { (5..20).map { it * 0.1f } },
                dataText = { "×${DecimalFormat("#.#").format(it)}" },
                onDataSelected = {
                    settingsViewModel.uiFontScaleRatio = it
                    visible = false
                },
            )
        }

        item(key = "uiFocusOptimize") {
            SettingsListItem(
                headlineContentProvider = { "焦点优化" },
                supportingContentProvider = { "关闭后可解决触摸设备在部分场景下闪退" },
                trailingContentProvider = {
                    Switch(settingsViewModel.uiFocusOptimize, null)
                },
                onSelected = {
                    settingsViewModel.uiFocusOptimize = !settingsViewModel.uiFocusOptimize
                },
            )
        }
    }
}