package com.luntik.terminal.ui

import android.app.Activity
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luntik.terminal.ApkDownloader
import com.luntik.terminal.ui.theme.Glass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class TerminalLine(
    val text: String,
    val color: Color = Glass.TextPrimary.copy(alpha = 0.85f)
)

private const val PREFS = "luntik_terminal_prefs"
private const val KEY_BG = "bg_uri"
private const val KEY_AUTH = "device_auth"

@Composable
fun TerminalScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val activity = context as? Activity
    val prefs = remember { context.getSharedPreferences(PREFS, 0) }
    val scope = rememberCoroutineScope()

    var bgUri by remember { mutableStateOf(prefs.getString(KEY_BG, null)) }
    var showSettings by remember { mutableStateOf(false) }

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) { }
            prefs.edit().putString(KEY_BG, uri.toString()).apply()
            bgUri = uri.toString()
        }
    }

    val bgBitmap = remember(bgUri) {
        bgUri?.let { u ->
            try {
                context.contentResolver.openInputStream(Uri.parse(u))?.use {
                    BitmapFactory.decodeStream(it)
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    val lines = remember {
        mutableStateListOf(
            TerminalLine("Microsoft Windows [Version 10.0.19045.3803]", Glass.TextSecondary),
            TerminalLine("(c) Microsoft Corporation. All rights reserved.", Glass.TextSecondary),
            TerminalLine(""),
            TerminalLine("LuntikTerminal v0.4.0", Glass.Accent),
            TerminalLine("Команды: auth · verify · terminal · apdate · settings · help", Glass.TextMuted),
            TerminalLine("")
        )
    }

    var input by remember { mutableStateOf("") }
    var isAuthenticated by remember {
        mutableStateOf(prefs.getBoolean(KEY_AUTH, false))
    }
    var isDownloading by remember { mutableStateOf(false) }
    var apkFile by remember { mutableStateOf<File?>(null) }
    var showStoreActions by remember { mutableStateOf(false) }
    var storeNeedUpdate by remember { mutableStateOf(false) }
    var lastVerifyLog by remember { mutableStateOf<List<String>>(emptyList()) }

    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }

    fun addLine(text: String, color: Color = Glass.TextPrimary.copy(alpha = 0.85f)) {
        lines.add(TerminalLine(text, color))
        scope.launch { listState.animateScrollToItem(lines.lastIndex.coerceAtLeast(0)) }
    }

    fun runVerifyStore() {
        if (!isAuthenticated) {
            addLine("Сначала выполни: auth", Glass.Error)
            addLine("")
            return
        }
        addLine("=== VERIFY LuntikStore ===", Glass.Accent)
        addLine("Проверка установленной версии...", Glass.TextMuted)
        val local = ApkDownloader.installedVersion(context, ApkDownloader.STORE_PACKAGE)
        if (local == null) {
            addLine("Локально: не установлен", Glass.Warning)
        } else {
            addLine("Локально: v$local", Glass.Success)
        }
        addLine("Запрос к GitHub Releases...", Glass.TextMuted)
        scope.launch {
            val remote = ApkDownloader.fetchLatestStore()
            withContext(Dispatchers.Main) {
                if (!remote.available) {
                    addLine("Ошибка: ${remote.error ?: "нет данных"}", Glass.Error)
                    addLine("")
                    showStoreActions = true
                    storeNeedUpdate = true
                    return@withContext
                }
                addLine("GitHub tag: ${remote.tagName ?: "latest"}", Glass.Success)
                remote.publishedAt?.let { addLine("Опубликован: $it", Glass.TextMuted) }
                remote.apkName?.let { addLine("APK: $it", Glass.TextMuted) }

                val need = when {
                    local == null -> true
                    remote.tagName != null && !local.contains(remote.tagName.removePrefix("v")) -> true
                    else -> true // всегда даём кнопку обновить/переустановить
                }
                storeNeedUpdate = need
                if (local == null) {
                    addLine("Статус: нужно СКАЧАТЬ и установить", Glass.Warning)
                } else {
                    addLine("Статус: можно ОБНОВИТЬ / переустановить", Glass.Warning)
                }
                addLine("Готово. Используй кнопку ниже или download.", Glass.Success)
                addLine("")
                showStoreActions = true
                lastVerifyLog = listOf(
                    "local=$local",
                    "remote=${remote.tagName}",
                    "apk=${remote.apkName}"
                )
            }
        }
    }

    fun downloadAndInstallStore() {
        if (!isAuthenticated) {
            addLine("Сначала: auth", Glass.Error)
            return
        }
        if (isDownloading) {
            addLine("Уже качается...", Glass.Warning)
            return
        }
        if (!ApkDownloader.canInstallPackages(context)) {
            addLine("Нет разрешения на установку APK", Glass.Error)
            ApkDownloader.openInstallPermissionSettings(context)
            return
        }
        isDownloading = true
        addLine("Скачивание LuntikStore...", Glass.Accent)
        scope.launch {
            val result = ApkDownloader.downloadStoreApk(context)
            withContext(Dispatchers.Main) {
                isDownloading = false
                if (result.success && result.file != null) {
                    apkFile = result.file
                    val mb = "%.1f".format(result.bytesDownloaded / 1024.0 / 1024.0)
                    addLine("Скачано ($mb MB). Запуск установщика...", Glass.Success)
                    val ok = ApkDownloader.installApk(context, result.file)
                    if (ok) addLine("Диалог установки открыт.", Glass.Success)
                    else addLine("Не удалось открыть установщик", Glass.Error)
                } else {
                    addLine("Ошибка: ${result.error}", Glass.Error)
                }
                addLine("")
            }
        }
    }

    fun checkAndUpdateTerminal(autoStart: Boolean) {
        addLine("=== LuntikTerminal ===", Glass.Accent)
        val localVer = ApkDownloader.selfVersionName(context)
        val localCode = ApkDownloader.selfVersionCode(context)
        addLine("Сейчас: v$localVer (code $localCode)", Glass.TextMuted)
        addLine("Проверка GitHub...", Glass.TextMuted)
        scope.launch {
            val remote = ApkDownloader.fetchLatestTerminal()
            withContext(Dispatchers.Main) {
                if (!remote.available) {
                    addLine("Не удалось проверить: ${remote.error}", Glass.Error)
                    addLine("")
                    return@withContext
                }
                addLine("На GitHub: ${remote.tagName ?: "latest"}", Glass.Success)
                val hasNew = remote.tagName != null &&
                    remote.tagName != "latest" &&
                    !localVer.contains(remote.tagName.removePrefix("v"))
                // tag latest always offer update button path via apdate
                if (hasNew || remote.tagName == "latest") {
                    addLine("Доступна сборка на GitHub.", Glass.Warning)
                    addLine("Напиши: apdate  — чтобы обновить терминал", Glass.Accent)
                    if (autoStart) {
                        addLine("Запускаю обновление...", Glass.Accent)
                        if (!ApkDownloader.canInstallPackages(context)) {
                            addLine("Нужно разрешение на установку", Glass.Error)
                            ApkDownloader.openInstallPermissionSettings(context)
                            addLine("")
                            return@withContext
                        }
                        isDownloading = true
                        scope.launch {
                            val result = ApkDownloader.downloadTerminalApk(context)
                            withContext(Dispatchers.Main) {
                                isDownloading = false
                                if (result.success && result.file != null) {
                                    addLine("APK скачан. Установка...", Glass.Success)
                                    ApkDownloader.installApk(context, result.file)
                                } else {
                                    addLine("Ошибка: ${result.error}", Glass.Error)
                                }
                                addLine("")
                            }
                        }
                    } else {
                        addLine("")
                    }
                } else {
                    addLine("У тебя актуальная версия.", Glass.Success)
                    addLine("")
                }
            }
        }
    }

    fun processCommand(cmd: String) {
        val trimmed = cmd.trim()
        if (trimmed.isEmpty()) return
        addLine("C:\\Luntik> $trimmed", Glass.TextPrimary)

        val raw = trimmed.lowercase().trim()
        val command = raw.removePrefix("/").split("\\s+".toRegex()).firstOrNull() ?: ""

        when (command) {
            "help" -> {
                addLine("")
                addLine("Команды:", Glass.Accent)
                addLine("  auth       - привязать устройство")
                addLine("  verify     - проверить LuntikStore (версии + лог)")
                addLine("  download   - скачать и установить Store")
                addLine("  terminal   - проверка версии Terminal")
                addLine("  apdate     - обновить сам Terminal")
                addLine("  settings   - фон и настройки")
                addLine("  clear      - очистить экран")
                addLine("  exit       - выход")
                addLine("")
            }

            "auth", "login" -> {
                addLine("Привязка устройства...", Glass.TextMuted)
                addLine("Device ID: ${android.os.Build.MODEL}", Glass.TextMuted)
                addLine("Android: ${android.os.Build.VERSION.RELEASE}", Glass.TextMuted)
                addLine("Устройство привязано.", Glass.Success)
                isAuthenticated = true
                prefs.edit().putBoolean(KEY_AUTH, true).apply()
                addLine("Дальше: verify", Glass.Accent)
                addLine("")
            }

            "verify" -> runVerifyStore()

            "download", "install", "update" -> {
                // update for store via download
                if (command == "update") {
                    // if user meant terminal, they use apdate; here store
                    downloadAndInstallStore()
                } else {
                    downloadAndInstallStore()
                }
            }

            "terminal" -> checkAndUpdateTerminal(autoStart = false)

            "apdate", "upgrade" -> checkAndUpdateTerminal(autoStart = true)

            "settings", "setting", "cfg" -> {
                showSettings = true
                addLine("Открыты настройки (фон).", Glass.Accent)
                addLine("")
            }

            "clear", "cls" -> {
                lines.clear()
                addLine("Microsoft Windows [Version 10.0.19045.3803]", Glass.TextSecondary)
                addLine("(c) Microsoft Corporation. All rights reserved.", Glass.TextSecondary)
                addLine("")
            }

            "exit", "quit" -> {
                addLine("Выход...")
                activity?.finish()
            }

            else -> {
                addLine("'${trimmed}' — неизвестная команда.", Glass.Error)
                addLine("Введи help", Glass.TextMuted)
                addLine("")
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // background photo
        if (bgBitmap != null) {
            Image(
                bitmap = bgBitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.62f))
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(listOf(Glass.BgDeep, Glass.BgMid, Glass.BgDeep))
                    )
            )
        }

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Glass.Fill)
                        .border(1.dp, Glass.Border, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        "LuntikTerminal",
                        color = Glass.TextPrimary,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    "settings",
                    color = Glass.Accent,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showSettings = true }
                        .padding(8.dp)
                )
            }

            Spacer(Modifier.height(10.dp))

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(lines.size) { i ->
                    val line = lines[i]
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

            if (showStoreActions && isAuthenticated) {
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Glass.Accent)
                        .clickable(enabled = !isDownloading) { downloadAndInstallStore() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (isDownloading) "Скачивание..."
                        else if (ApkDownloader.isInstalled(context, ApkDownloader.STORE_PACKAGE))
                            "Скачать и обновить LuntikStore"
                        else "Скачать и установить LuntikStore",
                        color = Color.Black,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

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
                    "C:\\Luntik>",
                    color = Glass.Accent,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.width(8.dp))
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

        if (showSettings) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable { showSettings = false }
            )
            Column(
                Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Glass.BgMid)
                    .border(1.dp, Glass.Border, RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Text("Настройки", color = Glass.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Text("Фон терминала", color = Glass.TextSecondary, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Glass.Accent)
                        .clickable {
                            pickImage.launch("image/*")
                        }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Выбрать фото", color = Color.Black, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Glass.Border, RoundedCornerShape(12.dp))
                        .clickable {
                            prefs.edit().remove(KEY_BG).apply()
                            bgUri = null
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Сбросить фон", color = Glass.TextSecondary)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Закрыть",
                    color = Glass.Accent,
                    modifier = Modifier
                        .align(Alignment.End)
                        .clickable { showSettings = false }
                        .padding(8.dp)
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        if (isAuthenticated) {
            addLine("Устройство уже привязано. Можешь: verify", Glass.TextMuted)
            addLine("")
        }
    }
}
