package top.yogiczy.mytv.core.data.repositories

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yogiczy.mytv.core.data.utils.Globals
import java.io.File

/**
 * 用于将数据缓存至本地
 */
open class FileCacheRepository(
    private val fileName: String,
    private val isFullPath: Boolean = false,
) {
    private fun getCacheFile() =
        if (isFullPath) File(fileName) else File(Globals.cacheDir, fileName)

    suspend fun getCacheData(): String? = withContext(Dispatchers.IO) {
        val file = getCacheFile()
        if (file.exists()) file.readText()
        else null
    }

    private suspend fun setCacheData(data: String) = withContext(Dispatchers.IO) {
        val file = getCacheFile()
        file.writeText(data)
    }

    suspend fun getOrRefresh(cacheTime: Long, refreshOp: suspend () -> String): String {
        return getOrRefresh(
            { lastModified, _ -> System.currentTimeMillis() - lastModified >= cacheTime },
            refreshOp,
        )
    }

    /*suspend fun getOrRefresh(
        isExpired: (lastModified: Long, cacheData: String?) -> Boolean,
        refreshOp: suspend () -> String,
    ): String {
        var data = getCacheData()

        if (isExpired(getCacheFile().lastModified(), data)) {
            data = null
        }

        if (data.isNullOrBlank()) {
            data = refreshOp()
            setCacheData(data)
        }

        return data
    }*/
    suspend fun getOrRefresh(
        isExpired: (lastModified: Long, cacheData: String?) -> Boolean,
        refreshOp: suspend () -> String,
    ): String {
        val oldData = getCacheData()
        val cacheFile = getCacheFile()

        // 1. 如果缓存未过期且存在，直接返回
        if (!oldData.isNullOrBlank() && !isExpired(cacheFile.lastModified(), oldData)) {
            return oldData
        }

        // 2. 尝试刷新数据
        return try {
            val newData = refreshOp()
            if (newData.isNotBlank()) {
                setCacheData(newData)
                newData
            } else {
                // 如果刷新结果为空且有旧数据，回退
                oldData?.takeIf { it.isNotBlank() } ?: throw Exception("Refresh result is blank and no cache available")
            }
        } catch (ex: Exception) {
            // 3. 刷新失败，如果有旧缓存，回退并记录日志
            if (!oldData.isNullOrBlank()) {
                // 这里可以考虑注入一个 logger，或者暂时保持原样由外部感知
                oldData
            } else {
                // 4. 彻底失败，抛出异常
                throw ex
            }
        }
    }

    open suspend fun clearCache() {
        try {
            getCacheFile().delete()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
}