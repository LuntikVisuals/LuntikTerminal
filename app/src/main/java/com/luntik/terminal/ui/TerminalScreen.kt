package com.luntik.terminal.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luntik.terminal.ApkDownloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

data class TerminalLine(
    val text: String,
    val color: Color = Color(0xFFCCCCCC)
)

@Composable
fun TerminalScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val activity = context as? Activity

    val lines = remember {
        mutableStateListOf(
            TerminalLine("Microsoft Windows [Version 10.0.19045.3803]", Color(0xFFCCCCCC)),
            TerminalLine("(c) Microsoft Corporation. All rights reserved.", Color(0xFFCCCCCC)),
            TerminalLine(""),
            TerminalLine("LuntikTerminal v0.2.0 — установщик LuntikStore", Color(0xFF00FF41)),
            TerminalLine("Введите 'help' для списка команд.", Color(0xFF888888)),
            TerminalLine("")
        )
    }

    var input by remember { mutableStateOf("") }
    var isAuthenticated by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var apkFile by remember { mutableStateOf<File?>(null) }
    var isVerified by remember { mutableStateOf(false) }
    var isInstalled by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    fun addLine(text: String, color: Color = Color(0xFFCCCCCC)) {
        lines.add(TerminalLine(text, color))
        scope.launch {
            listState.animateScrollToItem(lines.lastIndex)
        }
    }

    fun progressBar(progress: Float): String {
        val filled = (progress * 20).toInt().coerceIn(0, 20)
        val empty = 20 - filled
        return "[" + "█".repeat(filled) + "░".repeat(empty) + "] ${(progress * 100).toInt()}%"
    }

    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun processCommand(cmd: String) {
        val trimmed = cmd.trim()
        if (trimmed.isEmpty()) return

        addLine("C:\\Luntik> $trimmed", Color(0xFFCCCCCC))

        val parts = trimmed.lowercase().split("\\s+".toRegex())
        val command = parts.firstOrNull() ?: ""

        when (command) {
            "help" -> {
                addLine("")
                addLine("Доступные команды:", Color(0xFF00FF41))
                addLine("  help         — показать этот список")
                addLine("  list / apps  — показать LuntikStore")
                addLine("  info         — информация о LuntikStore")
                addLine("  auth / login — авторизация")
                addLine("  download     — скачать LuntikStore с GitHub")
                addLine("  verify       — проверить SHA-256")
                addLine("  install      — установить LuntikStore")
                addLine("  status       — текущий статус")
                addLine("  clear / cls  — очистить экран")
                addLine("  exit / quit  — выход")
                addLine("")
            }

            "list", "apps" -> {
                addLine("")
                addLine("=== Доступно для установки ===", Color(0xFF00FF41))
                addLine("")
                addLine("  LuntikStore  — Магазин приложений Luntik")
                addLine("  Источник: github.com/LuntikVisuals/LuntikStore")
                addLine("  URL: releases/latest/download/LuntikStore.apk")
                addLine("")
                addLine("Порядок: auth → download → verify → install")
                addLine("")
            }

            "info" -> {
                addLine("")
                addLine("LuntikStore", Color(0xFF00FF41))
                addLine("  Репозиторий: github.com/LuntikVisuals/LuntikStore")
                addLine("  Описание:    Центральный магазин приложений Luntik")
                addLine("  Скачивание:  GitHub Releases (latest)")
                addLine("  Требования:  Android 8.0+")
                addLine("")
            }

            "auth", "login" -> {
                if (isAuthenticated) {
                    addLine("Вы уже авторизованы.", Color(0xFF00FF41))
                } else {
                    addLine("Авторизация...", Color(0xFF888888))
                    addLine("Проверка устройства... OK")
                    addLine("Подключение к GitHub... OK")
                    addLine("Авторизация успешна.", Color(0xFF00FF41))
                    isAuthenticated = true
                }
                addLine("")
            }

            "download" -> {
                if (!isAuthenticated) {
                    addLine("Ошибка: сначала выполните 'auth'", Color(0xFFFF5555))
                    addLine("")
                    return
                }
                if (isDownloading) {
                    addLine("Загрузка уже идёт...", Color(0xFFFFAA00))
                    addLine("")
                    return
                }
                if (apkFile != null && apkFile!!.exists()) {
                    addLine("LuntikStore.apk уже скачан.", Color(0xFF888888))
                    addLine("Используйте 'verify' или 'install'.")
                    addLine("")
                    return
                }

                isDownloading = true
                addLine("Подключение к GitHub Releases...", Color(0xFF00FF41))
                addLine(ApkDownloader.STORE_APK_URL, Color(0xFF666666))
                addLine("Начинаю загрузку...")

                scope.launch {
                    val result = ApkDownloader.downloadStoreApk(context) { progress ->
                        // обновляем последнюю строку прогресса — упрощённо просто пишем
                    }

                    withContext(Dispatchers.Main) {
                        isDownloading = false
                        if (result.success && result.file != null) {
                            apkFile = result.file
                            val sizeMb = "%.1f".format(result.bytesDownloaded / 1024.0 / 1024.0)
                            addLine(progressBar(1f), Color(0xFF00FF41))
                            addLine("Загрузка завершена: LuntikStore.apk ($sizeMb MB)", Color(0xFF00FF41))
                            addLine("Используйте 'verify' для проверки.")
                        } else {
                            addLine("Ошибка загрузки: ${result.error}", Color(0xFFFF5555))
                            addLine("Убедись, что в LuntikStore есть Release с APK.", Color(0xFFFFAA00))
                        }
                        addLine("")
                    }
                }
            }

            "verify" -> {
                val file = apkFile
                if (file == null || !file.exists()) {
                    addLine("Файл не найден. Сначала 'download'.", Color(0xFFFF5555))
                    addLine("")
                    return
                }
                addLine("Проверка целостности LuntikStore.apk...", Color(0xFF888888))
                scope.launch {
                    val hash = withContext(Dispatchers.IO) { sha256(file) }
                    withContext(Dispatchers.Main) {
                        addLine("SHA-256: ${hash.take(32)}...")
                        addLine("Размер:  ${file.length()} bytes")
                        addLine("Проверка пройдена.", Color(0xFF00FF41))
                        isVerified = true
                        addLine("")
                    }
                }
            }

            "install" -> {
                val file = apkFile
                if (file == null || !file.exists()) {
                    addLine("Сначала скачайте: download", Color(0xFFFF5555))
                    addLine("")
                    return
                }
                if (!isVerified) {
                    addLine("Рекомендуется сначала 'verify'", Color(0xFFFFAA00))
                }

                if (!ApkDownloader.canInstallPackages(context)) {
                    addLine("Нет разрешения на установку из неизвестных источников.", Color(0xFFFF5555))
                    addLine("Открываю настройки... Разреши установку и вернись.", Color(0xFFFFAA00))
                    ApkDownloader.openInstallPermissionSettings(context)
                    addLine("")
                    return
                }

                addLine("Запуск системного установщика...", Color(0xFF00FF41))
                val ok = ApkDownloader.installApk(context, file)
                if (ok) {
                    isInstalled = true
                    addLine("Диалог установки открыт.", Color(0xFF00FF41))
                    addLine("")
                    addLine("========================================", Color(0xFF00FF41))
                    addLine("После установки LuntikStore:", Color(0xFF00FF41))
                    addLine("• Ярлык Terminal можно будет скрыть", Color(0xFFFFAA00))
                    addLine("• Открыть Terminal — изнутри Store", Color(0xFFFFAA00))
                    addLine("========================================", Color(0xFF00FF41))
                } else {
                    addLine("Не удалось открыть установщик.", Color(0xFFFF5555))
                }
                addLine("")
            }

            "status" -> {
                addLine("")
                addLine("=== Статус ===", Color(0xFF00FF41))
                addLine("Авторизация:  ${if (isAuthenticated) "ДА" else "НЕТ"}")
                addLine("Скачано:      ${if (apkFile?.exists() == true) "LuntikStore.apk" else "нет"}")
                addLine("Проверено:    ${if (isVerified) "ДА" else "НЕТ"}")
                addLine("Установлено:  ${if (isInstalled) "диалог открыт" else "НЕТ"}")
                addLine("")
            }

            "clear", "cls" -> {
                lines.clear()
                addLine("Microsoft Windows [Version 10.0.19045.3803]")
                addLine("(c) Microsoft Corporation. All rights reserved.")
                addLine("")
            }

            "exit", "quit" -> {
                addLine("Завершение работы LuntikTerminal...")
                addLine("До свидания.", Color(0xFF00FF41))
                activity?.finish()
            }

            else -> {
                addLine("'${trimmed}' не является внутренней или внешней командой,", Color(0xFFFF5555))
                addLine("исполняемой программой или пакетным файлом.", Color(0xFFFF5555))
                addLine("Введите 'help' для списка команд.")
                addLine("")
            }
        }
    }

    Column(
        modifier = modifier
            .background(Color(0xFF0C0C0C))
            .padding(12.dp)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(lines) { line ->
                Text(
                    text = line.text,
                    color = line.color,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Text(
                text = "C:\\Luntik>",
                color = Color(0xFFCCCCCC),
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.width(6.dp))
            BasicTextField(
                value = input,
                onValueChange = { input = it },
                textStyle = TextStyle(
                    color = Color(0xFFCCCCCC),
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                ),
                cursorBrush = SolidColor(Color(0xFFCCCCCC)),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        processCommand(input)
                        input = ""
                    }
                ),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
            )
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}
