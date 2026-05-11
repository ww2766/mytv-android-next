package top.yogiczy.mytv.tv.ui.screens.videoplayercontroller.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import top.yogiczy.mytv.tv.ui.utils.handleKeyEvents

@Composable
fun VideoPlayerControllerBtn(
    modifier: Modifier = Modifier,
    onSelect: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    IconButton(
        modifier = modifier
            .size(30.dp)
            .handleKeyEvents(onSelect = onSelect),
        onClick = {},
    ) {
        content()
    }
}

@Composable
fun VideoPlayerControllerBtn(
    modifier: Modifier = Modifier,
    imageVector: ImageVector,
    onSelect: () -> Unit = {},
) {
    VideoPlayerControllerBtn(
        modifier = modifier,
        onSelect = onSelect,
        content = {
            Icon(
                modifier = Modifier.size(18.dp),
                imageVector = imageVector,
                contentDescription = null,
            )
        },
    )
}