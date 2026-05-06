# Moderation Helper GUI

Клиентский мод для **Minecraft Java Edition 1.21.11** под **Fabric**. Это не PE/Bedrock.

Мод помогает модератору быстро открыть GUI по нику игрока в чате, выдать warn/mute/ban/ipban, сохранить доказательный скриншот чата, вести статистику текущей сессии, работать со списком недавних игроков и управлять записью OBS через obs-websocket.

## Что реализовано

- СКМ/колёсико мыши по строке чата открывает меню наказаний.
- Ник извлекается из строки чата с фильтрацией рангов, серверных слов, цветовых кодов и мусорных символов.
- Скриншот делается **до открытия GUI**, поэтому меню на скрин не попадает.
- Скрин сначала сохраняется во временную папку `temp`, а после выдачи наказания переносится в папку `warn`, `mute`, `ban` или `ipban`.
- Для сообщений с `Tick Speed`, `Reach`, `Fighting suspiciously`, `Block Interaction` скриншот не делается, но GUI может открыться.
- GUI: главное меню наказаний, выбор времени, выбор причины, быстрые причины, статистика, недавние игроки.
- H открывает только панель статистики и недавних игроков.
- G останавливает запись OBS.
- Кнопка `Вызвать на проверку` отправляет `/check {nick}` и запускает запись OBS.
- Над хотбаром отображается таймер `Идёт запись: 00:00`.
- При `ipban` запись OBS останавливается автоматически, кроме причины `3.8`.
- Автоочистка старых скриншотов: `DELETE`, `ARCHIVE` или `OFF`.
- Все бинды регистрируются в стандартных настройках управления Minecraft.

## Требования

- Minecraft Java Edition 1.21.11
- Fabric Loader 0.18.1 или новее
- Fabric API для 1.21.11
- Java 21
- Gradle 9.2+ или Gradle Wrapper с Gradle 9.2+
- OBS Studio с включённым obs-websocket, если нужна OBS-интеграция

## Сборка

Открой папку проекта в IntelliJ IDEA или в терминале.

```bash
gradle build
```

Готовый `.jar` появится в:

```text
build/libs/
```

Если у тебя нет Gradle, можно создать wrapper:

```bash
gradle wrapper --gradle-version 9.2.1
./gradlew build
```

На Windows:

```bat
gradlew.bat build
```

## Установка мода

1. Установи Fabric Loader для Minecraft 1.21.11.
2. Положи Fabric API в папку `.minecraft/mods`.
3. Положи собранный `moderation-helper-gui-1.0.0.jar` в `.minecraft/mods`.
4. Запусти игру.

## Настройки управления

Открой:

```text
Options -> Controls -> Key Binds -> Moderation Helper GUI
```

По умолчанию:

- `H` — открыть статистику и недавних игроков.
- `G` — остановить OBS-запись.
- СКМ/колёсико по нику в чате — открыть GUI наказаний.

Клавиши `H` и `G` можно менять в стандартных настройках Minecraft.

## Конфиг

Конфиг создаётся автоматически после первого запуска:

```text
.minecraft/config/moderation_helper_gui.json
```

Пример:

```json
{
  "obsEnabled": false,
  "obsHost": "localhost",
  "obsPort": 4455,
  "obsPassword": "",
  "maxRecentPlayers": 12,
  "screenshotCleanupMode": "DELETE",
  "screenshotRetentionDays": 30,
  "screenshotsFolder": "moderation_screenshots",
  "checkCommandTemplate": "/check {nick}",
  "quickReasons": [
    "3.1",
    "3.2",
    "3.3",
    "3.4",
    "3.5",
    "3.6",
    "3.7",
    "3.8",
    "spam",
    "flood",
    "cheats",
    "toxicity",
    "bug abuse"
  ]
}
```

### Параметры

- `obsEnabled` — включить/выключить OBS-интеграцию.
- `obsHost` — обычно `localhost`.
- `obsPort` — обычно `4455`.
- `obsPassword` — пароль obs-websocket.
- `maxRecentPlayers` — сколько последних игроков хранить в панели.
- `screenshotCleanupMode`:
  - `DELETE` — удалять старые скриншоты;
  - `ARCHIVE` — переносить старые скриншоты в архив;
  - `OFF` — не чистить.
- `screenshotRetentionDays` — срок хранения скриншотов в днях.
- `screenshotsFolder` — папка для скриншотов относительно `.minecraft`.
- `checkCommandTemplate` — шаблон команды проверки. Должен содержать `{nick}`.
- `quickReasons` — быстрые причины в GUI.

## OBS-websocket

В OBS:

1. Открой `Tools -> WebSocket Server Settings`.
2. Включи WebSocket server.
3. Поставь порт `4455`.
4. Включи пароль, если нужен.
5. Впиши такой же пароль в `moderation_helper_gui.json`.
6. В конфиге мода поставь:

```json
"obsEnabled": true
```

Если OBS выключен или недоступен, мод не должен крашить игру: он выведет сообщение в чат и лог.

## Как пользоваться

### Открытие меню по чату

1. Открой чат.
2. Наведи мышку на строку с ником.
3. Нажми СКМ/колёсико.
4. Мод определит ник, сделает временный скриншот чата и откроет GUI.

Если в строке есть `Tick Speed`, `Reach`, `Fighting suspiciously`, `Block Interaction`, скриншот не делается.

### Выдача наказания

1. Выбери `Warn`, `Mute`, `Ban` или `IPBan`.
2. Введи время, например:
   - `7d` — 7 дней;
   - `12h` — 12 часов.
3. Введи причину или выбери быструю причину.
4. Нажми `Выдать наказание`.

Команды отправляются от имени игрока:

```text
/warn nick duration reason
/mute nick duration reason
/ban nick duration reason
/ipban nick duration reason
```

### Проверка игрока

В меню наказаний нажми `Вызвать на проверку`.

Мод:

- отправит команду из `checkCommandTemplate`, по умолчанию `/check {nick}`;
- запустит запись OBS;
- покажет над хотбаром таймер записи.

Остановить запись можно клавишей `G`.

### IPBan и OBS

После выдачи `ipban` OBS-запись остановится автоматически.

Исключение: если причина ровно `3.8`, запись не останавливается.

### Недавние игроки

В главном меню и в панели H отображаются последние игроки.

- Нумерации нет.
- Дубликаты не добавляются: игрок поднимается наверх списка.
- Клик по нику копирует его в буфер обмена и открывает меню наказаний.

## Скриншоты

По умолчанию папки лежат здесь:

```text
.minecraft/moderation_screenshots/
```

Структура:

```text
moderation_screenshots/
  warn/
  mute/
  ban/
  ipban/
  temp/
  archive/
```

Временный файл:

```text
temp/{nick}_{datetime}.png
```

Итоговый файл:

```text
{nick}_{punishment}_{duration}_{reason}_{datetime}.png
```

Запрещённые символы в названии файла заменяются на `_`.

## Важное ограничение

Перехват клика по строке чата в Minecraft 1.21.11 сделан через Mixin и чтение внутреннего списка `ChatHud`. У Mojang/Fabric нет стабильного публичного API именно для “СКМ по нику в чате”. На обычных строках чата это работает нормально, но очень длинные переносимые сообщения могут потребовать подстройки геометрии в `ChatClickLocator`.
