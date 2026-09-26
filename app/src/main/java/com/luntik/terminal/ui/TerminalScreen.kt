package com.luntik.terminal.ui

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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

data class TerminalLine(
    val text: String,
    val color: Color = Color(0xFFCCCCCC)
)

@Composable
fun TerminalScreen(modifier: Modifier = Modifier) {
    val lines = remember {
        mutableStateListOf(
            TerminalLine("Microsoft Windows [Version 10.0.19045.3803]", Color(0xFFCCCCCC)),
            TerminalLine("(c) Microsoft Corporation. All rights reserved.", Color(0xFFCCCCCC)),
            TerminalLine(""),
            TerminalLine("LuntikTerminal v0.1.0 — скрытый установщик", Color(0xFF00FF41)),
            TerminalLine("Введите 'help' для списка команд.", Color(0xFF888888)),
            TerminalLine("")
        )
    }

    var input by remember { mutableStateOf("") }
    var isAuthenticated by remember { mutableStateOf(false) }
    var downloadedApps by remember { mutableStateOf(setOf<String>()) }
    var installedApps by remember { mutableStateOf(setOf<String>()) }

    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    fun addLine(text: String, color: Color = Color(0xFFCCCCCC)) {
        lines.add(TerminalLine(text, color))
        scope.launch {
            listState.animateScrollToItem(lines.lastIndex)
        }
    }

    fun processCommand(cmd: String) {
        val trimmed = cmd.trim()
        if (trimmed.isEmpty()) return

        addLine("C:\\Luntik> $trimmed", Color(0xFFCCCCCC))

        val parts = trimmed.lowercase().split("\\s+".toRegex())
        val command = parts.firstOrNull() ?: ""
        val arg = parts.getOrNull(1) ?: ""

        when (command) {
            "help" -> {
                addLine("")
                addLine("Доступные команды:", Color(0xFF00FF41))
                addLine("  help                 — показать этот список")
                addLine("  list / apps          — каталог приложений")
                addLine("  info <название>     — информация о приложении")
                addLine("  auth / login         — авторизация")
                addLine("  download <название> — скачать приложение")
                addLine("  verify <название>   — проверить целостность")
                addLine("  install <название>  — установить приложение")
                addLine("  status               — текущий статус")
                addLine("  clear / cls          — очистить экран")
                addLine("  exit / quit          — выход")
                addLine("")
            }

            "list", "apps" -> {
                addLine("")
                addLine("=== Каталог LuntikStore ===", Color(0xFF00FF41))
                addLine("")
                addLine("  [1] LuntikStore     — Магазин приложений  (обязательно)")
                addLine("  [2] LuntikAi        — Локальный ИИ-агент")
                addLine("  [3] Lumina          — Premium виджеты")
                addLine("")
                addLine("Используйте: info <название> или download <название>")
                addLine("")
            }

            "info" -> {
                when (arg) {
                    "luntikstore", "store" -> {
                        addLine("")
                        addLine("LuntikStore", Color(0xFF00FF41))
                        addLine("  Версия:     0.1.0")
                        addLine("  Размер:     ~4.2 MB")
                        addLine("  Описание:   Центральный магазин всех приложений Luntik.")
                        addLine("              Через него можно снова открыть Terminal.")
                        addLine("  Требования: Android 8.0+")
                        addLine("")
                    }
                    "luntikai", "ai" -> {
                        addLine("")
                        addLine("LuntikAi", Color(0xFF00FF41))
                        addLine("  Версия:     0.5.0")
                        addLine("  Размер:     ~8.1 MB")
                        addLine("  Описание:   Локальный самообучающийся ИИ для Android.")
                        addLine("")
                    }
                    "lumina" -> {
                        addLine("")
                        addLine("Lumina", Color(0xFF00FF41))
                        addLine("  Версия:     0.2.0")
                        addLine("  Размер:     ~6.5 MB")
                        addLine("  Описание:   Premium Android виджеты с liquid glass.")
                        addLine("")
                    }
                    else -> addLine("Приложение не найдено. Используйте 'list' для просмотра.", Color(0xFFFF5555))
                }
            }

            "auth", "login" -> {
                if (isAuthenticated) {
                    addLine("Вы уже авторизованы.", Color(0xFF00FF41))
                } else {
                    addLine("Авторизация...", Color(0xFF888888))
                    addLine("Проверка устройства... OK")
                    addLine("Генерация временного токена... OK")
                    addLine("Авторизация успешна.", Color(0xFF00FF41))
                    addLine("Теперь доступна загрузка приложений.")
                    isAuthenticated = true
                }
                addLine("")
            }

            "download" -> {
                if (!isAuthenticated) {
                    addLine("Ошибка: сначала выполните команду 'auth'", Color(0xFFFF5555))
                    addLine("")
                    return
                }
                when (arg) {
                    "luntikstore", "store" -> {
                        if ("luntikstore" in downloadedApps) {
                            addLine("LuntikStore уже скачан.", Color(0xFF888888))
                        } else {
                            addLine("Начинаю загрузку LuntikStore...", Color(0xFF00FF41))
                            addLine("[████████████████████████████████] 100%")
                            addLine("Загрузка завершена: LuntikStore.apk")
                            addLine("Используйте 'verify luntikstore' для проверки.")
                            downloadedApps = downloadedApps + "luntikstore"
                        }
                    }
                    "luntikai", "ai" -> {
                        addLine("Начинаю загрузку LuntikAi...")
                        addLine("[████████████████████████████████] 100%")
                        addLine("Загрузка завершена: LuntikAi.apk")
                        downloadedApps = downloadedApps + "luntikai"
                    }
                    "lumina" -> {
                        addLine("Начинаю загрузку Lumina...")
                        addLine("[████████████████████████████████] 100%")
                        addLine("Загрузка завершена: Lumina.apk")
                        downloadedApps = downloadedApps + "lumina"
                    }
                    else -> addLine("Неизвестное приложение. Используйте 'list'.", Color(0xFFFF5555))
                }
                addLine("")
            }

            "verify" -> {
                if (arg in downloadedApps) {
                    addLine("Проверка целостности $arg...", Color(0xFF888888))
                    addLine("SHA-256: a3f2b8c9d1e4f5a6b7c8d9e0f1a2b3c4")
                    addLine("Подпись: VALID")
                    addLine("Проверка пройдена успешно.", Color(0xFF00FF41))
                } else {
                    addLine("Файл не найден. Сначала скачайте приложение.", Color(0xFFFF5555))
                }
                addLine("")
            }

            "install" -> {
                if (arg !in downloadedApps) {
                    addLine("Сначала скачайте приложение командой 'download $arg'", Color(0xFFFF5555))
                    addLine("")
                    return
                }
                addLine("Запуск установки $arg...", Color(0xFF00FF41))
                addLine("Передача управления системному установщику...")
                addLine("(В реальной версии здесь откроется системный диалог установки)")
                installedApps = installedApps + arg

                if (arg == "luntikstore" || arg == "store") {
                    addLine("")
                    addLine("========================================", Color(0xFF00FF41))
                    addLine("LuntikStore успешно установлен!", Color(0xFF00FF41))
                    addLine("Ярлык LuntikTerminal будет скрыт.", Color(0xFFFFAA00))
                    addLine("Найти Terminal можно внутри LuntikStore.", Color(0xFFFFAA00))
                    addLine("========================================", Color(0xFF00FF41))
                }
                addLine("")
            }

            "status" -> {
                addLine("")
                addLine("=== Статус ===", Color(0xFF00FF41))
                addLine("Авторизация:  ${if (isAuthenticated) "ДА" else "НЕТ"}")
                addLine("Скачано:     ${if (downloadedApps.isEmpty()) "ничего" else downloadedApps.joinToString()}")
                addLine("Установлено: ${if (installedApps.isEmpty()) "ничего" else installedApps.joinToString()}")
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
        // История терминала
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

        // Строка ввода
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
