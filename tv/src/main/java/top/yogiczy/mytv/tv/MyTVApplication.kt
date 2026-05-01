package top.yogiczy.mytv.tv

import android.app.Application
import com.tencent.smtt.export.external.TbsCoreSettings
import com.tencent.smtt.sdk.QbSdk
import top.yogiczy.mytv.core.data.AppData
import top.yogiczy.mytv.core.data.utils.Logger
import java.io.File
import java.util.HashMap

class MyTVApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppData.init(applicationContext)
        UnsafeTrustManager.enableUnsafeTrustManager()

        val log = Logger.create("X5Init")
        
        // 1. 仅在检测到版本变动或手动标记时清理一次
        val sp = getSharedPreferences("x5_config", MODE_PRIVATE)
        val lastCleanVersion = sp.getInt("last_clean_version", 0)
        if (lastCleanVersion < 44286) {
            clearTbsCache()
            sp.edit().putInt("last_clean_version", 44286).apply()
        }

        // 2. 修复文件权限
        fixTbsDexPermissions()

        try {
            // 显式解除可能存在的强制系统内核锁定
            QbSdk.unForceSysWebView()
            
            val settings = HashMap<String, Any>()
            settings[TbsCoreSettings.TBS_SETTINGS_USE_PRIVATE_CLASSLOADER] = true
            // Android 14 核心：开启服务模式
            settings[TbsCoreSettings.TBS_SETTINGS_USE_DEXLOADER_SERVICE] = true
            settings[TbsCoreSettings.TBS_SETTINGS_USE_SPEEDY_CLASSLOADER] = true
            // 禁用重命名逻辑，防止 Android 14 路径安全冲突
            settings["tbs_core_load_rename"] = false
            settings["use_st7"] = true
            
            QbSdk.initTbsSettings(settings)
            QbSdk.setDownloadWithoutWifi(true)
            
            // 开启 TBS 内部日志
            QbSdk.setTbsLogClient(object : com.tencent.smtt.utils.TbsLogClient(this@MyTVApplication) {
                override fun writeLog(s: String?) {
                    if (s?.contains("error", ignoreCase = true) == true || s?.contains("fail", ignoreCase = true) == true) {
                        log.e("TBS_INTERNAL_ERR: $s")
                    } else {
                        log.d("TBS_INTERNAL: $s")
                    }
                }
            })
            
            // 初始化前执行权限修复
            fixTbsDexPermissions()
            
            log.i("正在以 Android 14 兼容模式初始化X5...")
            
            QbSdk.initX5Environment(this, object : QbSdk.PreInitCallback {
                override fun onCoreInitFinished() {
                    val canLoad = QbSdk.canLoadX5(this@MyTVApplication)
                    log.i("X5内核 onCoreInitFinished, canLoadX5: $canLoad")
                }

                override fun onViewInitFinished(isX5: Boolean) {
                    val ver = QbSdk.getTbsVersion(this@MyTVApplication)
                    log.i("X5内核初始化结果: $isX5, version: $ver")
                    
                    // Android 14 下严禁在加载失败时自动删除，否则会陷入安装死循环
                    // 我们保留内核，通过权限修复让它在下次启动时生效
                }
            })
        } catch (e: Exception) {
            log.e("X5内核初始化捕获到异常: ${e.message}", e)
        }
    }

    private fun clearTbsCache() {
        val log = Logger.create("X5Init")
        try {
            listOf("app_tbs", "app_tbs_64", "app_tbs_share").forEach { path ->
                val dir = File(applicationInfo.dataDir, path)
                if (dir.exists()) {
                    val deleted = dir.deleteRecursively()
                    log.w("已清理旧内核目录 $path: $deleted")
                }
            }
        } catch (e: Exception) {
            log.e("清理内核目录失败", e)
        }
    }

    private fun fixTbsDexPermissions() {
        val log = Logger.create("X5Init")
        try {
            val paths = listOf("app_tbs", "app_tbs_64", "app_tbs_share")
            // 白名单：配置、锁、XML文件必须允许写入
            val whiteListSuffix = listOf(".conf", ".txt", ".xml", ".lock", ".prop")
            val whiteListNames = listOf("tbslock.txt", "tbs_pms.xml")
            
            paths.forEach { path ->
                val tbsDir = File(applicationInfo.dataDir, path)
                if (tbsDir.exists()) {
                    tbsDir.walkTopDown().forEach { file ->
                        if (file.isFile) {
                            val isWhiteListed = whiteListNames.any { file.name.equals(it, ignoreCase = true) } || 
                                              whiteListSuffix.any { file.name.endsWith(it, ignoreCase = true) }
                            
                            if (!isWhiteListed) {
                                // 只有可执行代码类文件（.jar, .dex, .so）才在 Android 14 上强制只读
                                if (file.name.endsWith(".jar") || file.name.endsWith(".dex") || file.name.endsWith(".so")) {
                                    if (file.canWrite()) {
                                        file.setWritable(false, false)
                                        file.setReadOnly()
                                    }
                                }
                            } else {
                                // 确保白名单文件（配置/锁）必须可写
                                if (!file.canWrite()) {
                                    file.setWritable(true, false)
                                    log.d("配置权限恢复: ${file.name}")
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            log.e("权限修复出错", e)
        }
    }
}
