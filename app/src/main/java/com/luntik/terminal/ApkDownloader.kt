package com.luntik.terminal

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

object ApkDownloader {

    const val STORE_PACKAGE = "com.luntik.store"
    const val TERMINAL_PACKAGE = "com.luntik.terminal"

    const val STORE_APK_URL =
        "https://github.com/LuntikVisuals/LuntikStore/releases/latest/download/LuntikStore.apk"
    const val TERMINAL_APK_URL =
        "https://github.com/LuntikVisuals/LuntikTerminal/releases/latest/download/LuntikTerminal.apk"

    private const val STORE_API =
        "https://api.github.com/repos/LuntikVisuals/LuntikStore/releases/latest"
    private const val TERMINAL_API =
        "https://api.github.com/repos/LuntikVisuals/LuntikTerminal/releases/latest"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    data class DownloadResult(
        val success: Boolean,
        val file: File? = null,
        val error: String? = null,
        val bytesDownloaded: Long = 0
    )

    data class RemoteInfo(
        val available: Boolean,
        val tagName: String? = null,
        val publishedAt: String? = null,
        val apkUrl: String? = null,
        val apkName: String? = null,
        val error: String? = null
    )

    fun installedVersion(context: Context, packageName: String): String? {
        return try {
            val info = if (Build.VERSION.SDK_INT >= 33) {
                context.packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(packageName, 0)
            }
            info.versionName
        } catch (_: Exception) {
            null
        }
    }

    fun isInstalled(context: Context, packageName: String): Boolean =
        installedVersion(context, packageName) != null

    fun selfVersionName(context: Context): String {
        return installedVersion(context, context.packageName) ?: "?"
    }

    fun selfVersionCode(context: Context): Long {
        return try {
            val info = if (Build.VERSION.SDK_INT >= 33) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else {
                @Suppress("DEPRECATION")
                info.versionCode.toLong()
            }
        } catch (_: Exception) {
            0L
        }
    }

    suspend fun fetchLatestStore(): RemoteInfo = fetchLatest(STORE_API, ".apk")

    suspend fun fetchLatestTerminal(): RemoteInfo = fetchLatest(TERMINAL_API, ".apk")

    private suspend fun fetchLatest(apiUrl: String, assetSuffix: String): RemoteInfo =
        withContext(Dispatchers.IO) {
            try {
                val req = Request.Builder()
                    .url(apiUrl)
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "LuntikTerminal/0.4")
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        return@withContext RemoteInfo(
                            available = false,
                            error = "GitHub API HTTP ${resp.code}"
                        )
                    }
                    val body = resp.body?.string() ?: return@withContext RemoteInfo(
                        false, error = "Пустой ответ GitHub"
                    )
                    val json = JSONObject(body)
                    val tag = json.optString("tag_name", "")
                    val published = json.optString("published_at", "")
                    val assets = json.optJSONArray("assets")
                    var apkUrl: String? = null
                    var apkName: String? = null
                    if (assets != null) {
                        for (i in 0 until assets.length()) {
                            val a = assets.getJSONObject(i)
                            val name = a.optString("name", "")
                            if (name.endsWith(assetSuffix, ignoreCase = true)) {
                                apkUrl = a.optString("browser_download_url", null)
                                apkName = name
                                break
                            }
                        }
                    }
                    RemoteInfo(
                        available = true,
                        tagName = tag.ifBlank { null },
                        publishedAt = published.ifBlank { null },
                        apkUrl = apkUrl,
                        apkName = apkName
                    )
                }
            } catch (e: Exception) {
                RemoteInfo(available = false, error = e.message)
            }
        }

    suspend fun downloadUrl(
        context: Context,
        url: String,
        fileName: String,
        onProgress: (Float) -> Unit = {}
    ): DownloadResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "LuntikTerminal/0.4")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext DownloadResult(
                        success = false,
                        error = "HTTP ${response.code}: ${response.message}"
                    )
                }
                val body = response.body ?: return@withContext DownloadResult(
                    success = false,
                    error = "Пустой ответ"
                )
                val total = body.contentLength()
                val apkFile = File(context.cacheDir, fileName)
                body.byteStream().use { input ->
                    apkFile.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var downloaded = 0L
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            downloaded += read
                            if (total > 0) onProgress(downloaded.toFloat() / total)
                        }
                    }
                }
                if (apkFile.length() < 1000) {
                    apkFile.delete()
                    return@withContext DownloadResult(false, error = "Файл слишком маленький")
                }
                DownloadResult(true, file = apkFile, bytesDownloaded = apkFile.length())
            }
        } catch (e: Exception) {
            DownloadResult(false, error = e.message ?: "Ошибка сети")
        }
    }

    suspend fun downloadStoreApk(
        context: Context,
        onProgress: (Float) -> Unit = {}
    ): DownloadResult {
        val remote = fetchLatestStore()
        val url = remote.apkUrl ?: STORE_APK_URL
        return downloadUrl(context, url, "LuntikStore.apk", onProgress)
    }

    suspend fun downloadTerminalApk(
        context: Context,
        onProgress: (Float) -> Unit = {}
    ): DownloadResult {
        val remote = fetchLatestTerminal()
        val url = remote.apkUrl ?: TERMINAL_APK_URL
        return downloadUrl(context, url, "LuntikTerminal.apk", onProgress)
    }

    fun installApk(context: Context, apkFile: File): Boolean {
        return try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun canInstallPackages(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else true
    }

    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startActivity(
                Intent(
                    android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
