package com.luntik.terminal

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

object ApkDownloader {

    // Публичный URL релиза LuntikStore на GitHub
    // После первой успешной сборки + релиза будет работать
    const val STORE_APK_URL =
        "https://github.com/LuntikVisuals/LuntikStore/releases/latest/download/LuntikStore.apk"

    // Запасной URL (Actions artifact недоступен без токена, поэтому используем Releases)
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    data class DownloadResult(
        val success: Boolean,
        val file: File? = null,
        val error: String? = null,
        val bytesDownloaded: Long = 0
    )

    suspend fun downloadStoreApk(
        context: Context,
        onProgress: (Float) -> Unit = {}
    ): DownloadResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(STORE_APK_URL)
                .header("User-Agent", "LuntikTerminal/0.2")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext DownloadResult(
                        success = false,
                        error = "HTTP ${response.code}: ${response.message}. Возможно релиз ещё не создан."
                    )
                }

                val body = response.body ?: return@withContext DownloadResult(
                    success = false,
                    error = "Пустой ответ от сервера"
                )

                val total = body.contentLength()
                val apkFile = File(context.cacheDir, "LuntikStore.apk")

                body.byteStream().use { input ->
                    apkFile.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var downloaded = 0L
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            downloaded += read
                            if (total > 0) {
                                onProgress(downloaded.toFloat() / total)
                            }
                        }
                    }
                }

                if (apkFile.length() < 1000) {
                    apkFile.delete()
                    return@withContext DownloadResult(
                        success = false,
                        error = "Файл слишком маленький, возможно это не APK"
                    )
                }

                DownloadResult(
                    success = true,
                    file = apkFile,
                    bytesDownloaded = apkFile.length()
                )
            }
        } catch (e: Exception) {
            DownloadResult(
                success = false,
                error = e.message ?: "Неизвестная ошибка загрузки"
            )
        }
    }

    fun installApk(context: Context, apkFile: File): Boolean {
        return try {
            val uri: Uri = FileProvider.getUriForFile(
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
        } else {
            true
        }
    }

    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(
                android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}
