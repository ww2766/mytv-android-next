package top.yogiczy.mytv.tv.ui.screens.settings.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.lifecycle.viewmodel.compose.viewModel
import top.yogiczy.mytv.core.data.utils.Constants
import top.yogiczy.mytv.core.data.utils.SP
import top.yogiczy.mytv.core.util.utils.humanizeMs
import top.yogiczy.mytv.tv.ui.screens.settings.SettingsViewModel

@Composable
fun SettingsCategoryHttp(
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel = viewModel(),
) {
    SettingsContentList(modifier) {
        item {
            SettingsListItem(
                modifier = Modifier.focusRequester(it),
                headlineContent = "HTTP请求重试次数",
                supportingContent = "影响直播源、节目单数据获取",
                trailingContent = Constants.HTTP_RETRY_COUNT.toString(),
                locK = true,
            )
        }

        item {
            SettingsListItem(
                headlineContent = "HTTP请求重试间隔时间",
                supportingContent = "影响直播源、节目单数据获取",
                trailingContent = Constants.HTTP_RETRY_INTERVAL.humanizeMs(),
                locK = true,
            )
        }

        item {
            SettingsListItem(
                headlineContent = "网络代理",
                supportingContent = "全局代理将影响整个APP的网络连接|${SP.proxyUri}|${SP.proxySites}",
                trailingContent = when (settingsViewModel.proxyType) {
                    SP.ProxyType.NO -> "禁用代理"
                    SP.ProxyType.ALL -> "全局代理"
                    SP.ProxyType.LIMIT -> "限定域名代理"
                },
                onSelected = {
                    settingsViewModel.proxyType =
                        SP.ProxyType.entries.let {
                            it[(it.indexOf(settingsViewModel.proxyType) + 1) % it.size]
                        }
                },
            )
        }
    }
}