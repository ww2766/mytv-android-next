package top.yogiczy.mytv.tv.ui.screens.settings.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Switch
import androidx.tv.material3.Text
import kotlinx.coroutines.launch
import top.yogiczy.mytv.core.data.entities.epgsource.EpgSourceList
import top.yogiczy.mytv.core.data.repositories.epg.EpgRepository
import top.yogiczy.mytv.tv.ui.material.LocalPopupManager
import top.yogiczy.mytv.tv.ui.material.SimplePopup
import top.yogiczy.mytv.tv.ui.screens.components.SelectDialog
import top.yogiczy.mytv.tv.ui.screens.epgsource.EpgSourceScreen
import top.yogiczy.mytv.tv.ui.screens.settings.SettingsViewModel

@Composable
fun SettingsCategoryEpg(
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel = viewModel(),
) {
    val coroutineScope = rememberCoroutineScope()

    SettingsContentList(modifier) {
        item(key = "epgEnable") {
            SettingsListItem(
                modifier = Modifier.focusRequester(it),
                headlineContentProvider = { "节目单启用" },
                supportingContentProvider = { "首次加载时可能会较为缓慢" },
                trailingContentProvider = {
                    Switch(settingsViewModel.epgEnable, null)
                },
                onSelected = {
                    settingsViewModel.epgEnable = !settingsViewModel.epgEnable
                },
            )
        }

        item(key = "epgRefreshTimeThreshold") {
            val popupManager = LocalPopupManager.current
            val focusRequester = remember { FocusRequester() }
            var visible by remember { mutableStateOf(false) }

            SettingsListItem(
                modifier = Modifier.focusRequester(focusRequester),
                headlineContentProvider = { "节目单刷新时间阈值" },
                supportingContentProvider = { "时间不到${settingsViewModel.epgRefreshTimeThreshold}:00节目单将不会刷新" },
                trailingContentProvider = { Text("${settingsViewModel.epgRefreshTimeThreshold}:00") },
                onSelected = {
                    popupManager.push(focusRequester, true)
                    visible = true
                },
                remoteConfig = true,
            )

            SelectDialog(
                visibleProvider = { visible },
                onDismissRequest = { visible = false },
                title = "节目单刷新时间阈值",
                currentDataProvider = { settingsViewModel.epgRefreshTimeThreshold },
                dataListProvider = { (0..<13).toList() },
                dataText = { "${it}:00" },
                onDataSelected = {
                    settingsViewModel.epgRefreshTimeThreshold = it
                    visible = false
                },
            )
        }

        item(key = "epgSourceCurrent") {
            val popupManager = LocalPopupManager.current
            val focusRequester = remember { FocusRequester() }
            var isEpgSourceScreenVisible by remember { mutableStateOf(false) }

            SettingsListItem(
                modifier = Modifier.focusRequester(focusRequester),
                headlineContentProvider = { "自定义节目单" },
                trailingContentProvider = { Text(settingsViewModel.epgSourceCurrent.name) },
                onSelected = {
                    popupManager.push(focusRequester, true)
                    isEpgSourceScreenVisible = true
                },
                remoteConfig = true,
            )

            SimplePopup(
                visibleProvider = { isEpgSourceScreenVisible },
                onDismissRequest = { isEpgSourceScreenVisible = false },
            ) {
                EpgSourceScreen(
                    epgSourceListProvider = { settingsViewModel.epgSourceList },
                    currentEpgSourceProvider = { settingsViewModel.epgSourceCurrent },
                    onEpgSourceSelected = {
                        isEpgSourceScreenVisible = false
                        if (settingsViewModel.epgSourceCurrent != it) {
                            settingsViewModel.epgSourceCurrent = it
                            coroutineScope.launch {
                                EpgRepository(settingsViewModel.epgSourceCurrent).clearCache()
                            }
                        }
                    },
                    onEpgSourceDeleted = {
                        settingsViewModel.epgSourceList =
                            EpgSourceList(settingsViewModel.epgSourceList - it)
                    },
                )
            }
        }
    }
}
