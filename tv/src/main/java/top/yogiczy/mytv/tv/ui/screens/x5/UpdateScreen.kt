package top.yogiczy.mytv.tv.ui.screens.x5

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yogiczy.mytv.core.util.utils.ApkInstaller

import top.yogiczy.mytv.tv.ui.material.PopupContent
import top.yogiczy.mytv.tv.ui.material.Snackbar
import top.yogiczy.mytv.tv.ui.material.SnackbarType
import top.yogiczy.mytv.tv.ui.screens.settings.SettingsViewModel
import top.yogiczy.mytv.tv.ui.screens.x5.components.UpdateContent
import top.yogiczy.mytv.tv.ui.utils.captureBackKey
import top.yogiczy.mytv.tv.ui.utils.captureBackKey


@Composable
fun UpdateScreen(
    modifier: Modifier = Modifier,
    updateViewModel: UpdateViewModel = viewModel(),
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()


    LaunchedEffect(Unit) {
        delay(3000)
        updateViewModel.checkUpdate(context)
        if (updateViewModel.isUpdateAvailable) {
             //updateViewModel.visible = true
        }
    }


    LaunchedEffect(updateViewModel.isSuccessInstalled) {
        if (updateViewModel.isSuccessInstalled) return@LaunchedEffect
        //currentRecomposeScope.invalidate()
        //return@run
    }

    PopupContent(
        visibleProvider = { updateViewModel.visible },
        onDismissRequest = { updateViewModel.visible = false },
    ) {
        UpdateContent(
            modifier = modifier
                .captureBackKey { updateViewModel.visible = false }
                .pointerInput(Unit) { detectTapGestures { } },
            onDismissRequest = { updateViewModel.visible = false },
            //isUpdateAvailableProvider = { updateViewModel.isUpdateAvailable },
            updateViewModel=updateViewModel,
            onUpdateAndInstall = {
                updateViewModel.visible = false
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        updateViewModel.downloadAndUpdate(context)
                    } catch (ex: Exception) {

                        Snackbar.show(
                            ex.message.toString(),
                            type = SnackbarType.ERROR,
                        )
                    }

                }
            },
        )
    }
}

