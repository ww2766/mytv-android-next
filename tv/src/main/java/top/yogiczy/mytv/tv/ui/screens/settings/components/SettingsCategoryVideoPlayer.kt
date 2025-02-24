package top.yogiczy.mytv.tv.ui.screens.settings.components

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Switch
import top.yogiczy.mytv.core.util.utils.humanizeMs
import top.yogiczy.mytv.tv.ui.material.LocalPopupManager
import top.yogiczy.mytv.tv.ui.material.SimplePopup
import top.yogiczy.mytv.tv.ui.screens.components.SelectDialog
import top.yogiczy.mytv.tv.ui.screens.settings.SettingsViewModel
import top.yogiczy.mytv.tv.ui.screens.x5.UpdateViewModel
import top.yogiczy.mytv.tv.ui.screens.videoplayerdiaplaymode.VideoPlayerDisplayModeScreen
import top.yogiczy.mytv.tv.ui.utils.Configs

@Composable
fun SettingsCategoryVideoPlayer(
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel = viewModel(),
    updateViewModel: UpdateViewModel = viewModel(),
) {
    val context = LocalContext.current
    updateViewModel.checkUpdate(context)
    SettingsContentList(modifier) {
        item {
            SettingsListItem(
                modifier = Modifier.focusRequester(it),
                headlineContent = "渲染方式",
                trailingContent = settingsViewModel.videoPlayerRenderMode.label,
                onSelected = {
                    if (settingsViewModel.videoPlayerRenderMode == Configs.VideoPlayerRenderMode.SURFACE_VIEW)
                        settingsViewModel.videoPlayerRenderMode =
                            Configs.VideoPlayerRenderMode.TEXTURE_VIEW
                    else
                        settingsViewModel.videoPlayerRenderMode =
                            Configs.VideoPlayerRenderMode.SURFACE_VIEW
                },
            )
        }

        item {
            SettingsListItem(
                headlineContent = "解码",
                supportingContent = "默认硬解码，可选软解码",
                trailingContent = {
                    Switch(settingsViewModel.videoPlayerForceAudioSoftDecode, null)
                },
                onSelected = {
                    settingsViewModel.videoPlayerForceAudioSoftDecode =
                        !settingsViewModel.videoPlayerForceAudioSoftDecode
                },
            )
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            item {
                AnimatedVisibility(
                    visible = updateViewModel.isUpdateAvailable,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    val focusRequester = remember { FocusRequester() }

                    SettingsListItem(
                        modifier = Modifier.focusRequester(focusRequester),
                        headlineContent = "腾讯X5 WebView 安装",
                        supportingContent = updateViewModel.process,
                        onSelected = {
                            updateViewModel.loadX5(context,20,null)
                        },
                    )
                }
                AnimatedVisibility(
                    visible = updateViewModel.isSuccessInstalled,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    val focusRequester = remember { FocusRequester() }
                    SettingsListItem(
                        modifier = Modifier.focusRequester(focusRequester),
                        headlineContent = "启用腾讯X5WebView",
                        supportingContent = "默认使用系统内置WebView，开启使用腾讯X5内核，打开开关后，请重启本APP后生效",
                        trailingContent = {
                            Switch(!settingsViewModel.sysWebViewMode, null)
                        },
                        onSelected = {
                            settingsViewModel.sysWebViewMode =
                                !settingsViewModel.sysWebViewMode
                        },
                    )
                }
            }
        }

        item {
            SettingsListItem(
                headlineContent = "停止上一媒体项",
                trailingContent = {
                    Switch(settingsViewModel.videoPlayerStopPreviousMediaItem, null)
                },
                onSelected = {
                    settingsViewModel.videoPlayerStopPreviousMediaItem =
                        !settingsViewModel.videoPlayerStopPreviousMediaItem
                },
            )
        }

        item {
            SettingsListItem(
                headlineContent = "跳过多帧渲染",
                trailingContent = {
                    Switch(settingsViewModel.videoPlayerSkipMultipleFramesOnSameVSync, null)
                },
                onSelected = {
                    settingsViewModel.videoPlayerSkipMultipleFramesOnSameVSync =
                        !settingsViewModel.videoPlayerSkipMultipleFramesOnSameVSync
                },
            )
        }

        item {
            val popupManager = LocalPopupManager.current
            var visible by remember { mutableStateOf(false) }

            SettingsListItem(
                headlineContent = "全局显示模式",
                trailingContent = settingsViewModel.videoPlayerDisplayMode.label,
                onSelected = {
                    popupManager.push(it, true)
                    visible = true
                },
                remoteConfig = true,
            )

            SimplePopup(
                visibleProvider = { visible },
                onDismissRequest = { visible = false },
            ) {
                VideoPlayerDisplayModeScreen(
                    currentDisplayModeProvider = { settingsViewModel.videoPlayerDisplayMode },
                    onDisplayModeChanged = {
                        settingsViewModel.videoPlayerDisplayMode = it
                        visible = false
                    },
                )
            }
        }

        item {
            val popupManager = LocalPopupManager.current
            val focusRequester = remember { FocusRequester() }
            var visible by remember { mutableStateOf(false) }

            SettingsListItem(
                modifier = Modifier.focusRequester(focusRequester),
                headlineContent = "播放器加载超时",
                supportingContent = "影响超时换源、断线重连",
                trailingContent = settingsViewModel.videoPlayerLoadTimeout.humanizeMs(),
                onSelected = {
                    popupManager.push(focusRequester, true)
                    visible = true
                },
            )

            SelectDialog(
                visibleProvider = { visible },
                onDismissRequest = { visible = false },
                title = "播放器加载超时",
                currentDataProvider = { settingsViewModel.videoPlayerLoadTimeout },
                dataListProvider = { listOf(3, 5, 10, 15, 20, 25, 30).map { it.toLong() * 1000 } },
                dataText = { it.humanizeMs() },
                onDataSelected = {
                    settingsViewModel.videoPlayerLoadTimeout = it
                    visible = false
                },
            )
        }

        item {
            SettingsListItem(
                headlineContent = "播放器自定义UA",
                supportingContent = settingsViewModel.videoPlayerUserAgent,
                remoteConfig = true,
            )
        }
    }
    // 当组件重新显示时，请求焦点
    LaunchedEffect(Unit) {
            //focusRequester.requestFocus()
    }
}