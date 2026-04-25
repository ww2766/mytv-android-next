package top.yogiczy.mytv.core.data.network

import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Authenticator
import okhttp3.ConnectionSpec
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import top.yogiczy.mytv.core.data.network.WebViewUtils.clearWebViewProxy
import top.yogiczy.mytv.core.data.utils.ChannelUtil
import top.yogiczy.mytv.core.data.utils.ChannelUtil.isProxyWebViewUrl
import top.yogiczy.mytv.core.data.utils.Logger
import top.yogiczy.mytv.core.data.utils.SP
import java.io.IOException
import java.net.InetSocketAddress
import java.net.MalformedURLException
import java.net.Proxy
import java.net.URI
import java.net.URL
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * 获取数据
 */
suspend fun proxyOkHttpFetchSource(sourceUrl: String): String {
    //log.d("获取远程直播源: $source")

    //val client = OkHttpClient()
    val client = getProxyOkHttpClient(sourceUrl)
    val request = Request.Builder().url(ChannelUtil.clearAllPrefixFromUrl(sourceUrl)).build()

    try {
        val response = client.newCall(request).await()

        if (!response.isSuccessful) throw Exception("${response.code}: ${response.message}")

        return withContext(Dispatchers.IO) {
            response.body?.string() ?: ""
        }
    } catch (ex: Exception) {
        //log.e("获取直播源失败", ex)
        throw Exception("获取直播源失败，请检查网络连接", ex)
    }
}
fun getProxyOkHttpClient(url: String): OkHttpClient {
    clearWebViewProxy()
    var client: OkHttpClient? =null
    //val urlString = "http://user:password@example.org:20171"
    var proxyUri: URL? =null
    try {
        proxyUri = URL(SP.proxyUri)

    }catch (ex: MalformedURLException) {
        ex.printStackTrace();
    }
    // 获取不安全的 Builder，而不是默认的 Builder
    val clientBuilder = getUnsafeOkHttpClientBuilder()
    // 获取服务器地址
    //val serverAddress = (proxyUri?.protocol ?: ) + "://" +proxyUri.host+":"+proxyUri.port.toString()
    if(proxyUri!=null && isUseProxy(url)) {
        // 获取用户信息
        val userInfo = proxyUri.authority.split("@")
        var username =""
        var password=""
        if(userInfo.size==2){
            val userInfoParts = userInfo.get(0).split(":".toRegex())
            if (userInfoParts.size==2) {
                username = userInfoParts.getOrElse(0) { "" }
                password = userInfoParts.getOrElse(1) { "" }
            }
        }
        var proxyType=Proxy.Type.DIRECT
        if (proxyUri.protocol.startsWith(Proxy.Type.HTTP.name, true)) {
            proxyType = Proxy.Type.HTTP
        } else if (proxyUri.protocol.startsWith(Proxy.Type.SOCKS.name, true)) {
            proxyType = Proxy.Type.SOCKS
        }
        val proxyAuthenticator: Authenticator = object : Authenticator {
            @Throws(IOException::class)
            override fun authenticate(route: Route?, response: Response): Request {
                val credential: String = Credentials.basic(username, password)
                return response.request.newBuilder()
                    .header("Proxy-Authorization", credential)
                    .build()
            }
        }
        try {
            clientBuilder.proxy(Proxy(proxyType, InetSocketAddress(proxyUri.host, proxyUri.port)))
                .proxyAuthenticator(proxyAuthenticator)
        }catch (ex:Exception)
        {
            println(ex)
            ex.printStackTrace()
        }
    }
    //println("Username: $username")
    //println("Password: $password")
    //println("Server Address: $serverAddress")

    return clientBuilder.build()
}

@OptIn(UnstableApi::class)
fun getProxyDataSourceFactory(url: String): DataSource.Factory {

    val client= getProxyOkHttpClient(url)

    val dataSourceFactory: DataSource.Factory =
        OkHttpDataSource.Factory(client).setUserAgent(SP.videoPlayerUserAgent);

    return dataSourceFactory
}
fun isUseProxy(url: String):Boolean{


    //禁用代理
    if(SP.proxyType == SP.ProxyType.NO){
        return false
    }
    //val urlString = "http://user:password@example.org:20171"
    val proxyUri = URL(SP.proxyUri)
    if(proxyUri.protocol.startsWith(Proxy.Type.HTTP.name,true) || proxyUri.protocol.startsWith(Proxy.Type.SOCKS.name,true)) {
        if(SP.proxyType == SP.ProxyType.ALL){
            //启动全局代理
            return true
        }
    }else{
        return false
    }
    //使用限定域名代理
    val isPrefixProxy=isProxyWebViewUrl(url)
    val uri= Uri.parse(ChannelUtil.clearProxyPrefixFromUrl(url))
    val sites=","+ SP.proxySites +","
    val isSiteProxy=  sites.indexOf(","+uri.host + ",",0,true)>-1

    return SP.proxyType == SP.ProxyType.LIMIT && (isPrefixProxy || isSiteProxy)
}

/**
 * 获取一个兼容性更强的、不安全的 OkHttpClient.Builder。
 * 它不仅信任所有 SSL 证书，还启用了更广泛的 TLS 版本和加密套件。
 * 警告：这会使应用容易受到中间人攻击。
 */
fun getUnsafeOkHttpClientBuilder(): OkHttpClient.Builder {
    try {
        // 1. 创建一个信任所有网站的 TrustManager (和之前一样)
        val trustAllCerts = arrayOf<TrustManager>(
            object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            }
        )

        // 2. 安装这个 TrustManager (和之前一样)
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())
        val sslSocketFactory = sslContext.socketFactory

        // 3. 创建一个更具兼容性的 ConnectionSpec
        // 这个规范允许使用旧版本的 TLS，以兼容更多的服务器
        val spec = ConnectionSpec.Builder(ConnectionSpec.COMPATIBLE_TLS)
            .build()

        // 4. 创建 OkHttpClient.Builder 并应用所有配置
        val builder = OkHttpClient.Builder()
        builder.sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
        builder.hostnameVerifier { _, _ -> true } // 不验证主机名
        builder.connectionSpecs(listOf(spec, ConnectionSpec.CLEARTEXT)) // 应用兼容性规范

        return builder
    } catch (e: Exception) {
        throw RuntimeException(e)
    }
}