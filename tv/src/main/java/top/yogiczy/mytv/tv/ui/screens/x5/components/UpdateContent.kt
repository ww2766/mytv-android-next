package top.yogiczy.mytv.tv.ui.screens.x5.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.tv.material3.WideButton
import com.tencent.smtt.sdk.QbSdk
import top.yogiczy.mytv.tv.ui.screens.x5.UpdateViewModel
import top.yogiczy.mytv.tv.ui.theme.MyTVTheme
import top.yogiczy.mytv.tv.ui.tooling.PreviewWithLayoutGrids
import top.yogiczy.mytv.tv.ui.utils.customBackground
import top.yogiczy.mytv.tv.ui.utils.focusOnLaunched
import top.yogiczy.mytv.tv.ui.utils.handleKeyEvents

@Composable
fun UpdateContent(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit = {},
    //isUpdateAvailableProvider: () -> Boolean = { false },
    updateViewModel: UpdateViewModel = viewModel(),
    onUpdateAndInstall: () -> Unit = {},
) {
    val context: Context = LocalContext.current
    val isCanInstall by remember { mutableStateOf(updateViewModel.isUpdateAvailable) }
    val isInstalled  by remember { mutableStateOf( updateViewModel.isSuccessInstalled)}
    Row(
        modifier = modifier
            .fillMaxSize()
            .customBackground()
            .padding(horizontal = 130.dp, vertical = 88.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {

        //if (isUpdateAvailableProvider()) {
            // 使用 AnimatedVisibility 控制 Row 的显示和隐藏，并添加动画
        AnimatedVisibility(
            visible = isCanInstall,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                modifier = Modifier.width(340.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    "请阅读以下事项",
                    style = MaterialTheme.typography.headlineMedium
                )

                LazyColumn {
                    item {
                        Text("如果您不能正常的收看网页内嵌的视频内容，您可以尝试下载并安装腾讯X5内核WebView，安装成功后在此处切换：设置>播放器>启用腾讯X5WebView。因腾讯X5内核服务器限制原因，可能会安装失败，请多次尝试或者其他时间段再次尝试。", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                WideButton(
                    modifier = Modifier
                        .focusOnLaunched()
                        .handleKeyEvents(onSelect = onUpdateAndInstall),
                    onClick = { },
                    title = { Text("立即安装") },
                )
            }
        }
        AnimatedVisibility(
            visible = isInstalled,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                modifier = Modifier.width(340.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    "腾讯X5WebView提示",
                    style = MaterialTheme.typography.headlineMedium
                )

                LazyColumn {
                    item {
                        Text("已成功安装腾讯X5WebView内核(版本号：${QbSdk.getTbsVersion(context)} 启用状态：${QbSdk.isTbsCoreInited()})，如果您不能正常的收看网页内嵌的视频内容，您可以尝试切换到腾讯X5WebView内核，请在在此处开启：设置>播放器>启用腾讯X5WebView ", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            WideButton(
                modifier = Modifier
                    .focusOnLaunched()
                    .handleKeyEvents(onSelect = onDismissRequest),
                onClick = { },
                title = { Text("返回") },
            )
        }
    }
}

@Preview(device = "id:Android TV (720p)")
@Composable
private fun UpdateDialogPreview() {
    MyTVTheme {
        PreviewWithLayoutGrids {
            UpdateContent(
            )
        }
    }
}