# LuntikTerminal

**Скрытый установщик LuntikStore** в стиле Windows CMD

Через LuntikTerminal скачивается и устанавливается **только LuntikStore**.
После установки ярлык LuntikTerminal исчезает.
Найти его снова можно только внутри LuntikStore.

## Как это работает

1. Запускаешь LuntikTerminal
2. Видишь плавную загрузку на фоне бинарного кода `0` `1`
3. Постепенно появляются две руки
4. Открывается терминал Windows CMD (`C:\\Luntik>`)
5. Через команды скачиваешь LuntikStore с GitHub
6. После установки ярлык Terminal пропадает

## Команды

| Команда | Описание |
|---------|----------|
| `help` | Список команд |
| `list` / `apps` | Показать LuntikStore |
| `info` | Информация о LuntikStore |
| `auth` / `login` | Авторизация |
| `download` | Скачать LuntikStore с GitHub |
| `verify` | Проверить целостность |
| `install` | Установить LuntikStore |
| `status` | Текущий статус |
| `clear` / `cls` | Очистить экран |
| `exit` / `quit` | Выход |

## Порядок установки

```
auth
download
verify
install
```

## Как скачать APK

1. **Actions** → последний успешный **Build APK**
2. Артефакт `luntikterminal-debug-apk`

## Связанные репозитории

- [LuntikStore](https://github.com/LuntikVisuals/LuntikStore) — магазин приложений

## Автор

LuntikVisuals
