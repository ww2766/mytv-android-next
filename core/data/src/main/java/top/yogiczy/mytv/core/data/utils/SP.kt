package top.yogiczy.mytv.core.data.utils

import android.content.Context
import android.content.SharedPreferences

object SP {
    private val log = Logger.create(javaClass.simpleName)
    private const val SP_NAME = "mytv-android"
    private const val SP_MODE = Context.MODE_PRIVATE
    private lateinit var sp: SharedPreferences

    fun getInstance(context: Context): SharedPreferences =
        context.getSharedPreferences(SP_NAME, SP_MODE)

    fun init(context: Context) {
        sp = getInstance(context)
    }

    private fun <T> safeGet(key: String, defValue: T, op: (key: String, defValue: T) -> T): T {
        try {
            return op(key, defValue)
        } catch (ex: Exception) {
            log.e("SP", ex)
            sp.edit().remove(key).apply()
            return defValue
        }
    }

    fun getString(key: String, defValue: String) = safeGet(key, defValue, sp::getString)!!
    fun putString(key: String, value: String) = sp.edit().putString(key, value).apply()

    fun getStringSet(key: String, defValue: Set<String>): Set<String> =
        safeGet(key, defValue, sp::getStringSet)!!

    fun putStringSet(key: String, value: Set<String>) = sp.edit().putStringSet(key, value).apply()

    fun getInt(key: String, defValue: Int) = safeGet(key, defValue, sp::getInt)
    fun putInt(key: String, value: Int) = sp.edit().putInt(key, value).apply()

    fun getLong(key: String, defValue: Long) = safeGet(key, defValue, sp::getLong)
    fun putLong(key: String, value: Long) = sp.edit().putLong(key, value).apply()

    fun getFloat(key: String, defValue: Float) = safeGet(key, defValue, sp::getFloat)
    fun putFloat(key: String, value: Float) = sp.edit().putFloat(key, value).apply()

    fun getBoolean(key: String, defValue: Boolean) = safeGet(key, defValue, sp::getBoolean)
    fun putBoolean(key: String, value: Boolean) = sp.edit().putBoolean(key, value).apply()

    fun clear() = sp.edit().clear().apply()
    enum class KEY {
        /** ==================== 播放器 ==================== */
        /** 播放器 自定义ua */
        VIDEO_PLAYER_USER_AGENT,
        /** ==================== proxy ==================== */
        /** 代理 类型 direct 0 全局 1 限定域名 2 */
        PROXY_TYPE,

        /** 代理 IP 域名*/
        PROXY_URI,

        /** 代理 域名集合*/
        PROXY_SITES
    }
    enum class ProxyType(val value: Int) {
        /** 直通  不使用代理 */
        NO(0),

        /** 代理所有网络连接 */
        ALL(1),

        /** 代理指定域名的网络连接 */
        LIMIT(2);

        companion object {
            fun fromValue(value: Int): ProxyType {
                return entries.firstOrNull { it.value == value } ?: LIMIT
            }
        }
    }
    /** ==================== 播放器 ==================== */
    /** 播放器 自定义ua */
    var videoPlayerUserAgent: String
        get() = SP.getString(KEY.VIDEO_PLAYER_USER_AGENT.name, "").ifBlank {
            Constants.VIDEO_PLAYER_USER_AGENT
        }
        set(value) = SP.putString(KEY.VIDEO_PLAYER_USER_AGENT.name, value)
    /** ==================== PROXY ==================== */
    /** 代理 类型 */
    var proxyType: SP.ProxyType
        get() =SP.ProxyType.fromValue(
            SP.getInt(SP.KEY.PROXY_TYPE.name, SP.ProxyType.NO.value)
        )
        set(value) = SP.putInt(SP.KEY.PROXY_TYPE.name, value.value)

    /** 代理 URI */
    var proxyUri: String
        get() = SP.getString(SP.KEY.PROXY_URI.name, "")
        set(value) = SP.putString(SP.KEY.PROXY_URI.name, value)

    /** 代理 域名列表 */
    var proxySites: String
        get() = SP.getString(SP.KEY.PROXY_SITES.name, "")
        set(value) = SP.putString(SP.KEY.PROXY_SITES.name, value)
}