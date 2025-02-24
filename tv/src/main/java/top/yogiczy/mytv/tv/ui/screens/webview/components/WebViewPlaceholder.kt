package top.yogiczy.mytv.tv.ui.screens.webview.components

import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import top.yogiczy.mytv.tv.ui.theme.MyTVTheme

@Composable
fun WebViewPlaceholder(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(0.8f)),
    ) {
        Text(
            text = "混合模式（webview）",
            modifier = Modifier.align(Alignment.Center),
            style = MaterialTheme.typography.titleLarge,
        )
    }
}
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun WebViewCover(
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().focusable(false).alpha(0f)
        .pointerInteropFilter { event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN,
                MotionEvent.ACTION_MOVE,
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> true // 拦截所有触摸事件
                else -> false
            }
        }
    )
}
@Preview(device = "id:Android TV (720p)")
@Composable
private fun WebViewPlaceholderPreview() {
    MyTVTheme {
        WebViewPlaceholder()
    }
}