package top.yogiczy.mytv.tv.ui.screens.x5

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.tencent.smtt.sdk.QbSdk
import com.tencent.smtt.sdk.QbSdk.PreInitCallback
import com.tencent.smtt.sdk.TbsDownloader
import com.tencent.smtt.sdk.TbsListener
import top.yogiczy.mytv.core.data.utils.Logger
import top.yogiczy.mytv.core.util.utils.Downloader
import top.yogiczy.mytv.tv.ui.material.Snackbar
import top.yogiczy.mytv.tv.ui.material.SnackbarType
import java.io.File

class UpdateViewModel : ViewModel() {
    private val log = Logger.create(javaClass.simpleName)
    private var _isSuccessInstalled by mutableStateOf(false)
    val isSuccessInstalled get() = _isSuccessInstalled
    private var _isUpdating = false

    private var _isUpdateAvailable by mutableStateOf(false)
    val isUpdateAvailable get() = _isUpdateAvailable

    private var _process by mutableStateOf("单击进行安装")
    val process get() = _process

    var visible by mutableStateOf(false)
    init {
        //checkUpdate()
    }
    fun checkUpdate(context: Context?) {
        if(isX5CoreInstalled(context)){
            _isSuccessInstalled=true
            _isUpdateAvailable=false
        }else{
            _isUpdateAvailable=true
            _isSuccessInstalled=false
        }
    }

    suspend fun downloadAndUpdate(context: Context,latestFile: File) {
        if (!_isUpdateAvailable) return
        if (_isUpdating) return

        _isUpdating = true
        Snackbar.show(
            "开始下载",
            leadingLoading = true,
            duration = 10_000,
            id = "downloadProcess"
        )

        try {
            Downloader.downloadTo(getX5DownloadUrl(), latestFile.path) {
                Snackbar.show(
                    "正在下载: $it%",
                    leadingLoading = true,
                    duration = 10_000,
                    id = "downloadProcess"
                )
            }

            Snackbar.show("下载成功")
        } catch (ex: Exception) {
            Snackbar.show("下载失败", type = SnackbarType.ERROR)
        } finally {
            _isUpdating = false
        }


        // 安装TBS内核
        QbSdk.reset(context)

        QbSdk.setTbsListener(object : TbsListener {
            override fun onDownloadFinish(i: Int) {
                log.e("进行了tbs:onDownloadFinish $i")
            }

            override fun onDownloadProgress(i: Int) {
                log.e("进行了tbs:onDownloadProgress $i")
            }

            override fun onInstallFinish(i: Int) {
                log.e("进行了tbs:onInstallFinish $i")
                Snackbar.show("进行了tbs:onInstallFinish $i")
                if (i != 200) {
                    //latestFile.delete()
                }
                val canLoadX5 = QbSdk.canLoadX5(context)
                log.e("canLoadX5 canLoadX5:$canLoadX5")
                log.e("versionX5 getTbsVersion:"+QbSdk.getTbsVersion(context).toString())
            }
        })
        QbSdk.installLocalTbsCore(
            context, getX5CoreVersion(),
            latestFile.path
        )
    }
    private fun isCpu64Bit(): Boolean {
        for (abi in Build.SUPPORTED_ABIS) {
            if (abi.contains("64")) return true
        }
        return false
    }
    private fun getX5CoreVersion(): Int {
        return if(isCpu64Bit()){
            46007
        }else{
            45738
        }
    }
    private fun getX5DownloadUrl(): String {
            return "http://void-tech.cn/wp-content/uploads/2024/10/0${getX5CoreVersion()}_x5.tbs_.apk"
    }
    /**
     * 是否初始化x5成功
     *
     * @param context 上下文环境
     * @return
     */
    fun isX5CoreInstalled(context: Context?): Boolean {
        QbSdk.unForceSysWebView()
        log.e("isX5CoreInstalled getTbsVersion="+QbSdk.getTbsVersion(context).toString())
        val canLoadX5 = QbSdk.canLoadX5(context)
        log.e("isX5CoreInstalled canLoadX5 ="+canLoadX5.toString())
        val version = QbSdk.getTbsVersion(context)
        return version > 0 && canLoadX5
    }

    fun loadX5(context: Context,retry: Int,preInitCallback:PreInitCallback?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            _process="鉴于系统内置内核版本已满足使用需要，建议您继续使用系统内置内核插件，不需要X5安装。"
        }
        if (retry<=0){
            _process="本次X5安装尝试失败。建议请在其他时间段再次尝试。"
        }
        val log=Logger.create("initX5WebViewCore：loadX5(context: Context)")
        var cb: PreInitCallback? =null
        if (preInitCallback!=null){
            cb=preInitCallback
        }else {
            cb = object : PreInitCallback {
                override fun onCoreInitFinished() {
                    //x5内核初始化完成回调接口，此接口回调并表示已经加载起来了x5，有可能特殊情况下x5内核加载失败，切换到系统内核。
                    log.i("x5内核 onCoreInitFinished-->")
                }

                override fun onViewInitFinished(b: Boolean) {
                    //x5內核初始化完成的回调，为true表示x5内核加载成功，否则表示x5内核加载失败，会自动切换到系统内核。
                    log.i("x5内核 onViewInitFinished: 加载X5内核是否成功: $b")
                    _isSuccessInstalled = true
                    _isUpdateAvailable = false
                    checkUpdate(context)
                    _process=
                        "本次X5安装尝试结束，加载X5内核: ${
                            if (b) {
                                "成功"
                            } else {
                                "失败"
                            }
                        }"
                    Snackbar.show(_process)
                }
            }
        }

        val canLoadX5 = QbSdk.canLoadX5(context)
        log.i("x5内核  是否可以加载X5内核 -->$canLoadX5")
        if (canLoadX5) {
            log.i("x5内核 initX5 ")
            QbSdk.preInit(context, cb)
        } else {
            QbSdk.setDownloadWithoutWifi(true)
            QbSdk.setTbsListener(object : TbsListener {
                override fun onDownloadFinish(errCode: Int) {
                    log.i("x5内核 onDownloadFinish -->下载X5内核状态：$errCode")
                }

                override fun onInstallFinish(errCode: Int) {
                    log.i("x5内核 onInstallFinish -->安装X5内核：$errCode")
                    //                    if (i == TbsListener.ErrorCode.INSTALL_SUCCESS_AND_RELEASE_LOCK) {
                    if (errCode == TbsListener.ErrorCode.DOWNLOAD_INSTALL_SUCCESS) {
                        log.i("x5内核 initX5 ")
                        Snackbar.show("x5内核安装完成，开始初始化...")
                        _process="x5内核安装完成，开始初始化..."
                        QbSdk.preInit(context, cb)
                    }else{
                        _process="x5内核还有${retry-1}次尝试安装"
                        Snackbar.show("x5内核还有${retry-1}次尝试安装")
                        loadX5(context,retry-1,cb)
                    }
                }

                override fun onDownloadProgress(progress: Int) {
                    log.i("x5内核 onDownloadProgress -->下载X5内核进度：$progress")
                    _process="x5内核下载进度：$progress"
                }
            })
            Thread {
                log.i("x5内核  开始下载X5内核")
                Snackbar.show("x5内核开始下载...")
                TbsDownloader.startDownload(context)
            }.start()
        }

    }
}