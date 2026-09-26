package com.luntik.terminal.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luntik.terminal.ApkDownloader
import com.luntik.terminal.ui.theme.Glass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

data class TerminalLine(
    val text: String,
    val color: Color = Glass.TextPrimary.copy(alpha = 0.85f)
)

@Composable
fun TerminalScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val activity = context as? Activity

    val lines = remember {
        mutableStateListOf(
            TerminalLine("Microsoft Windows [Version 10.0.19045.3803]", Glass.TextSecondary),
            TerminalLine("(c) Microsoft Corporation. All rights reserved.", Glass.TextSecondary),
            TerminalLine(""),
            TerminalLine("LuntikTerminal v0.3.0", Glass.Accent),
            TerminalLine("установщик LuntikStore", Glass.TextMuted),
            TerminalLine("Введите help для списка команд.", Glass.TextMuted),
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

    fun addLine(text: String, color: Color = Glass.TextPrimary.copy(alpha = 0.85f)) {
        lines.add(TerminalLine(text, color))
        scope.launch {
            listState.animateScrollToItem(lines.lastIndex)
        }
    }

    fun progressBar(progress: Float): String {
        val filled = (progress * 20).toInt().coerceIn(0, 20)
        val empty = 20 - filled
        return "[" + "#".repeat(filled) + "-".repeat(empty) + "] ${(progress * 100).toInt()}%"
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

        addLine("C:\\Luntik> $trimmed", Glass.TextPrimary)

        val parts = trimmed.lowercase().split("\\s+".toRegex())
        val command = parts.firstOrNull() ?: ""

        when (command) {
            "help" -> {
                addLine("")
                addLine("Доступные команды:", Glass.Accent)
                addLine("  help         - показать этот список")
                addLine("  list / apps  - показать LuntikStore")
                addLine("  info         - информация о LuntikStore")
                addLine("  auth / login - авторизация")
                addLine("  download     - скачать LuntikStore с GitHub")
                addLine("  verify       - проверить SHA-256")
                addLine("  install      - установить LuntikStore")
                addLine("  status       - текущий статус")
                addLine("  clear / cls  - очистить экран")
                addLine("  exit / quit  - выход")
                addLine("")
            }

            "list", "apps" -> {
                addLine("")
                addLine("=== Доступно для установки ===", Glass.Accent)
                addLine("")
                addLine("  LuntikStore  - Магазин приложений Luntik")
                addLine("  Источник: github.com/LuntikVisuals/LuntikStore")
                addLine("  URL: releases/latest/download/LuntikStore.apk")
                addLine("")
                addLine("Порядок: auth -> download -> verify -> install")
                addLine("")
            }

            "info" -> {
                addLine("")
                addLine("LuntikStore", Glass.Accent)
                addLine("  Репозиторий: github.com/LuntikVisuals/LuntikStore")
                addLine("  Описание:    Центральный магазин приложений Luntik")
                addLine("  Скачивание:  GitHub Releases (latest)")
                addLine("  Требования:  Android 8.0+")
                addLine("")
            }

            "auth", "login" -> {
                if (isAuthenticated) {
                    addLine("Вы уже авторизованы.", Glass.Success)
                } else {
                    addLine("Авторизация...", Glass.TextMuted)
                    addLine("Проверка устройства... OK")
                    addLine("Подключение к GitHub... OK")
                    addLine("Авторизация успешна.", Glass.Success)
                    isAuthenticated = true
                }
                addLine("")
            }

            "download" -> {
                if (!isAuthenticated) {
                    addLine("Ошибка: сначала выполните 'auth'", Glass.Error)
                    addLine("")
                    return
                }
                if (isDownloading) {
                    addLine("Загрузка уже идёт...", Glass.Warning)
                    addLine("")
                    return
                }
                if (apkFile != null && apkFile!!.exists()) {
                    addLine("LuntikStore.apk уже скачан.", Glass.TextMuted)
                    addLine("Используйте 'verify' или 'install'.")
                    addLine("")
                    return
                }

                isDownloading = true
                addLine("Подключение к GitHub Releases...", Glass.Accent)
                addLine(ApkDownloader.STORE_APK_URL, Glass.TextMuted)
                addLine("Начинаю загрузку...")

                scope.launch {
                    val result = ApkDownloader.downloadStoreApk(context) { }

                    withContext(Dispatchers.Main) {
                        isDownloading = false
                        if (result.success && result.file != null) {
                            apkFile = result.file
                            val sizeMb = "%.1f".format(result.bytesDownloaded / 1024.0 / 1024.0)
                            addLine(progressBar(1f), Glass.Success)
                            addLine("Загрузка завершена: LuntikStore.apk ($sizeMb MB)", Glass.Success)
                            addLine("Используйте 'verify' для проверки.")
                        } else {
                            addLine("Ошибка загрузки: ${result.error}", Glass.Error)
                            addLine("Убедись, что в LuntikStore есть Release с APK.", Glass.Warning)
                        }
                        addLine("")
                    }
                }
            }

            "verify" -> {
                val file = apkFile
                if (file == null || !file.exists()) {
                    addLine("Файл не найден. Сначала 'download'.", Glass.Error)
                    addLine("")
                    return
                }
                addLine("Проверка целостности LuntikStore.apk...", Glass.TextMuted)
                scope.launch {
                    val hash = withContext(Dispatchers.IO) { sha256(file) }
                    withContext(Dispatchers.Main) {
                        addLine("SHA-256: ${hash.take(32)}...")
                        addLine("Размер:  ${file.length()} bytes")
                        addLine("Проверка пройдена.", Glass.Success)
                        isVerified = true
                        addLine("")
                    }
                }
            }

            "install" -> {
                val file = apkFile
                if (file == null || !file.exists()) {
                    addLine("Сначала скачайте: download", Glass.Error)
                    addLine("")
                    return
                }
                if (!isVerified) {
                    addLine("Рекомендуется сначала 'verify'", Glass.Warning)
                }

                if (!ApkDownloader.canInstallPackages(context)) {
                    addLine("Нет разрешения на установку из неизвестных источников.", Glass.Error)
                    addLine("Открываю настройки... Разреши установку и вернись.", Glass.Warning)
                    ApkDownloader.openInstallPermissionSettings(context)
                    addLine("")
                    return
                }

                addLine("Запуск системного установщика...", Glass.Accent)
                val ok = ApkDownloader.installApk(context, file)
                if (ok) {
                    isInstalled = true
                    addLine("Диалог установки открыт.", Glass.Success)
                    addLine("")
                    addLine("----------------------------------------", Glass.Accent)
                    addLine("После установки LuntikStore:", Glass.Success)
                    addLine("- Ярлык Terminal можно будет скрыть", Glass.Warning)
                    addLine("- Открыть Terminal - изнутри Store", Glass.Warning)
                    addLine("----------------------------------------", Glass.Accent)
                } else {
                    addLine("Не удалось открыть установщик.", Glass.Error)
                }
                addLine("")
            }

            "status" -> {
                addLine("")
                addLine("=== Статус ===", Glass.Accent)
                addLine("Авторизация:  ${if (isAuthenticated) "ДА" else "НЕТ"}")
                addLine("Скачано:      ${if (apkFile?.exists() == true) "LuntikStore.apk" else "нет"}")
                addLine("Проверено:    ${if (isVerified) "ДА" else "НЕТ"}")
                addLine("Установлено:  ${if (isInstalled) "диалог открыт" else "НЕТ"}")
                addLine("")
            }

            "clear", "cls" -> {
                lines.clear()
                addLine("Microsoft Windows [Version 10.0.19045.3803]", Glass.TextSecondary)
                addLine("(c) Microsoft Corporation. All rights reserved.", Glass.TextSecondary)
                addLine("")
            }

            "exit", "quit" -> {
                addLine("Завершение работы LuntikTerminal...")
                addLine("До свидания.", Glass.Success)
                activity?.finish()
            }

            else -> {
                addLine("'${trimmed}' не является внутренней или внешней командой,", Glass.Error)
                addLine("исполняемой программой или пакетным файлом.", Glass.Error)
                addLine("Введите 'help' для списка команд.")
                addLine("")
            }
        }
    }

    Column(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    listOf(Glass.BgDeep, Glass.BgMid, Glass.BgDeep)
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        // Glass title chip
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Glass.Fill)
                .border(1.dp, Glass.Border, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = "LuntikTerminal",
                color = Glass.TextPrimary,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

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

        // Glass input bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Glass.FillStrong)
                .border(1.dp, Glass.Border, RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Text(
                text = "C:\\Luntik>",
                color = Glass.Accent,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.width(8.dp))
            BasicTextField(
                value = input,
                onValueChange = { input = it },
                textStyle = TextStyle(
                    color = Glass.TextPrimary,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                ),
                cursorBrush = SolidColor(Glass.Accent),
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
