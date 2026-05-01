package top.yogiczy.mytv.core.data.network

import android.annotation.SuppressLint
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Proxy
import android.net.ProxyInfo
import android.net.Uri
import android.os.Build
import android.os.Parcelable
import android.webkit.CookieManager
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import top.yogiczy.mytv.core.data.utils.Loggable
import top.yogiczy.mytv.core.data.utils.SP
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.net.MalformedURLException
import java.net.URL
import java.util.Locale


@SuppressLint("PrivateApi")
object WebViewUtils : Loggable()  {

    @SuppressWarnings("deprecation")
    fun clearCookie() {
        CookieManager.getInstance().removeAllCookies(null)
    }

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.KITKAT)
    val API_KITKAT = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.LOLLIPOP)
    val API_LOLLIPOP = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP

    private val FIELD_APPLICATION_LOADED_APK: Field?
    private val FIELD_LOADED_APK_RECEIVERS: Field?
    private val METHOD_WEB_VIEW_CORE_SEND_STATIC_MESSAGE: Method?
    private val CONSTRUCTOR_PROXY_PROPERTIES: Constructor<Parcelable>?

    init {
        var applicationLoadedApkField: Field? = null
        var loadedApkReceiversField: Field? = null
        var webViewCoreSendStaticMessage: Method? = null
        var proxyPropertiesConstructor: Constructor<Parcelable>? = null

        if (API_KITKAT) {
            try {
                applicationLoadedApkField = Application::class.java.getField("mLoadedApk")
                loadedApkReceiversField = applicationLoadedApkField.type.getDeclaredField("mReceivers")
                loadedApkReceiversField.isAccessible = true
            } catch (e: Exception) {
                applicationLoadedApkField = null
                loadedApkReceiversField = null
            }
        }

        if (!API_KITKAT) {
            try {
                @SuppressLint("PrivateApi")
                val webViewCoreClass = Class.forName("android.webkit.WebViewCore")
                webViewCoreSendStaticMessage = webViewCoreClass.getDeclaredMethod(
                    "sendStaticMessage",
                    Int::class.javaPrimitiveType,
                    Any::class.java
                )
                webViewCoreSendStaticMessage?.isAccessible = true
            } catch (e: Exception) {
                webViewCoreSendStaticMessage = null
            }
        }

        if (!API_LOLLIPOP) {
            try {
                @SuppressLint("PrivateApi")
                val proxyPropertiesClass = Class.forName("android.net.ProxyProperties") as Class<Parcelable>
                proxyPropertiesConstructor = proxyPropertiesClass.getConstructor(String::class.java, Int::class.javaPrimitiveType, String::class.java)
            } catch (e: Exception) {
                proxyPropertiesConstructor = null
            }
        }

        FIELD_APPLICATION_LOADED_APK = applicationLoadedApkField
        FIELD_LOADED_APK_RECEIVERS = loadedApkReceiversField
        METHOD_WEB_VIEW_CORE_SEND_STATIC_MESSAGE = webViewCoreSendStaticMessage
        CONSTRUCTOR_PROXY_PROPERTIES = proxyPropertiesConstructor
    }

    private fun findProxyChangeReceiver(receivers: Map<*, *>): BroadcastReceiver? {
        for ((key, value) in receivers) {
            if (value is Map<*, *>) {
                val result = findProxyChangeReceiver(value)
                if (result != null) {
                    return result
                }
            } else {
                if (key is BroadcastReceiver && key.javaClass.name.contains("ProxyChangeListener")) {
                    return key
                }
            }
        }
        return null
    }

    //private val EXECUTOR = Executor { command -> command.run() }
    fun updateWebViewProxy(context: Context, url: String) {
        if (SP.proxyUri.isBlank()) return
        clearWebViewProxy()
        var proxyUri: URL? = null
        var uri: Uri? = null
        try {
            proxyUri = URL(SP.proxyUri)
            uri = Uri.parse(url)

        }catch (ex: MalformedURLException) {
            ex.printStackTrace();
            return
        }
        if(!isUseProxy(url)) {
            return
        }
        if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            val proxyConfig = ProxyConfig.Builder()
                .addProxyRule("${proxyUri.protocol}://${proxyUri.host}:${proxyUri.port}")
                //.addProxyRule("http://192.168.1.12:20171")
                .addBypassRule("127.0.0.1")
                .addBypassRule("localhost")
                .addDirect()
                .build()
            ProxyController.getInstance().setProxyOverride(proxyConfig, {
                log.e("OkHttpProxyManager:setProxyOverride execute")
            }, {
                log.e("OkHttpProxyManager:WebView代理改变");
            });
        } else {
            log.e("OkHttpProxyManager:WebView代理不支持");
            setHttpProxy(
                context,
                if (!proxyUri.protocol.lowercase(Locale.ROOT).startsWith("socks")) proxyUri else null
            )
        }
    }

    private fun setHttpProxy(context: Context, proxyUri: URL?) {
        val hostProperty = proxyUri?.host ?: ""
        val portProperty = proxyUri?.port?.toString() ?: ""
        System.setProperty("http.proxySet", "true");
        System.setProperty("http.proxyHost", hostProperty)
        System.setProperty("http.proxyPort", portProperty)
        System.setProperty("https.proxyHost", hostProperty)
        System.setProperty("https.proxyPort", portProperty)
        System.setProperty("http.nonProxyHosts","localhost|127.0.0.1")
        System.setProperty("https.nonProxyHosts","localhost|127.0.0.1")

        if (API_KITKAT) {
            if (FIELD_APPLICATION_LOADED_APK != null && FIELD_LOADED_APK_RECEIVERS != null &&
                (API_LOLLIPOP || CONSTRUCTOR_PROXY_PROPERTIES != null)
            ) {
                val applicationContext = context.applicationContext
                var receivers: Map<*, *>? = null
                try {
                    val loadedApk = FIELD_APPLICATION_LOADED_APK.get(applicationContext)
                    receivers = FIELD_LOADED_APK_RECEIVERS.get(loadedApk) as Map<*, *>?
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                val proxyChangeListener = receivers?.let { findProxyChangeReceiver(it) }
                if (proxyChangeListener != null) {
                    val intent = Intent(Proxy.PROXY_CHANGE_ACTION)
                    if (API_LOLLIPOP) {
                        val name = "android.intent.extra.PROXY_INFO"
                        intent.putExtra(
                            name,
                            proxyUri?.let { ProxyInfo.buildDirectProxy(it.host, it.port) }
                        )
                    } else {
                        val proxyProperties = proxyUri?.let {
                            try {
                                CONSTRUCTOR_PROXY_PROPERTIES?.newInstance(it.host, it.port, null)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                null
                            }
                        }
                        intent.putExtra("proxy", proxyProperties)
                    }
                    proxyChangeListener.onReceive(applicationContext, intent)
                }
            }
        } else {
            if (METHOD_WEB_VIEW_CORE_SEND_STATIC_MESSAGE != null && CONSTRUCTOR_PROXY_PROPERTIES != null) {
                try {
                    val messageProxyChanged = 193
                    val proxyProperties = proxyUri?.let {
                        CONSTRUCTOR_PROXY_PROPERTIES.newInstance(it.host, it.port, null)
                    }
                    METHOD_WEB_VIEW_CORE_SEND_STATIC_MESSAGE.invoke(null, messageProxyChanged, proxyProperties)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    fun clearWebViewProxy() {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            ProxyController.getInstance().clearProxyOverride({
                log.e(  "clearProxyOverride execute")
            }, {
                log.e("OkHttpProxyManager WebView代理改变");
            });
        } else {
            log.e("OkHttpProxyManager WebView代理不支持");
            System.setProperty("http.proxySet", "false");
            System.setProperty("http.proxyHost", "")
            System.setProperty("http.proxyPort", "")
            System.setProperty("https.proxyHost", "")
            System.setProperty("https.proxyPort", "")
        }
    }

}