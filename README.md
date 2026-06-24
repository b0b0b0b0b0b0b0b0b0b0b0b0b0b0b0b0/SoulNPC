# SoulNPC


## Требования

| Компонент | Версия |
|-----------|--------|
| Сервер | Paper **1.21+** |
| Java | **21** |
| Обязательно | [PacketEvents](https://github.com/retrooper/packetevents) |

**Softdepend (опционально):** PlaceholderAPI, SkinsRestorer, ViaVersion / ViaBackwards / ViaRewind, Geyser-Spigot.

---

## Установка

1. Положить **PacketEvents** и **SoulNPC** в `plugins/`.
2. Запустить сервер — создадутся `plugins/SoulNPC/config.yml`, `gui/`, `lang/`, при `storage.type: yaml` — папка `npcs/`.
3. Настроить `config.yml` (язык, storage, performance).
4. Создать NPC: `/soulnpc create` затем `/soulnpc edit`, или командами и конфигом.

---

## Хранение NPC

В `config.yml` → `storage`:

```yaml
storage:
  type: yaml          # yaml | sqlite | mysql
  yaml:
    folder: npcs
  sqlite:
    file: storage/soulnpc.db
  mysql:
    host: localhost
    port: 3306
    database: soulnpc
    username: root
    password: ""
    pool-size: 10
```

| Режим | Описание |
|-------|----------|
| **yaml** (default) | Один файл на NPC: `plugins/SoulNPC/npcs/<id>.yml` |
| **sqlite** | Payload (YAML) в SQLite-таблице |
| **mysql** | То же через HikariCP |

- Смена `storage.type` — **только после перезапуска** (`/soulnpc reload` предупредит).
- Ключ `general.npcFolder` устарел; используйте `storage.yaml.folder`.
- Миграция данных между backend'ами **не меняет** активный `storage.type`:

```text
/soulnpc migrate <from> <to> [--dry-run] [--overwrite]
```

`from` / `to`: `yaml`, `sqlite`, `mysql`. Без `--overwrite` существующие ID пропускаются.

---

## Права

| Permission | Default | Назначение |
|------------|---------|------------|
| `soulnpc.use` | **true** | Клики по NPC (право для игроков) |
| `soulnpc.admin` | op | reload, list, tp, info, respawn, skin, stick, migrate |
| `soulnpc.create` | op | `/soulnpc create` |
| `soulnpc.delete` | op | удаление NPC |
| `soulnpc.edit` | op | GUI-редактор, pose, guicancel |
| `soulnpc.bypass.cooldown` | op | обход кулдауна клика |

---

## Команды

Алиасы: `/snpc`, `/npc`.

| Команда | Право | Описание |
|---------|-------|----------|
| `/soulnpc` / `help` | любое из admin/create/delete/edit | справка |
| `/soulnpc create [id] [тип]` | create | без id — авто `1`, `2`, `3…`; тип: `player`, `zombie`, `fox_snow`… |
| `/soulnpc delete <id>` | delete | удалить |
| `/soulnpc edit` | edit | GUI редактора |
| `/soulnpc list` | admin | список NPC |
| `/soulnpc tp <id>` | admin | телепорт к NPC |
| `/soulnpc info <id>` | admin | информация |
| `/soulnpc respawn <id>` | admin | переспавн |
| `/soulnpc reload` | admin | config + NPC |
| `/soulnpc migrate …` | admin | копирование данных между storage |
| `/soulnpc skin <id> [ник]` | admin | скин по нику / SkinRestorer |
| `/soulnpc skin <id> url <url>` | admin | скин по URL |
| `/soulnpc skin <id> file <имя>` | admin | файл из `plugins/SoulNPC/skins/` |
| `/soulnpc stick` | admin | палка инспектора (ЛКМ → ID в чат) |
| `/soulnpc pose copy\|apply <id>` | edit | буфер позы player-модели |
| `/soulnpc guicancel` | edit | отмена ввода в чат из GUI |

---

## GUI-редактор

Открывается через `/soulnpc edit`. Подменю:

| Меню | Содержимое |
|------|------------|
| **Admin** | список NPC, пагинация, reload всех, ЛКМ → редактор, ПКМ → tp (admin) |
| **Edit** | голограмма, glow, collidable, look-at, tp ко мне, respawn, delete |
| **Строки** | TextDisplay-строки; Shift+LMB — TEXT ↔ ITEM |
| **Действия** | actions v2: клик, тип, delay, value (чат по СКМ) |
| **Одеть** | экипировка player-модели |
| **Свечение** | glow + цвет контура |
| **Спавн предметов** | декоративные item entity у ног |

Layout слотов — `plugins/SoulNPC/gui/admin.yml`, `npc-edit.yml`.

---

## Клики и actions

Два формата в `npcs/<id>.yml` → `interaction`:

### Legacy (списки строк)

Префиксы: `[message]`, `[player]`, `[console]`, `[op]`, `[switchserver]`.  
Списки: `right-click-commands`, `left-click-commands`, `middle-click-commands`, `shift-right-click-commands`, `shift-left-click-commands`.  
Строки с `#` — подсказки, не выполняются.

### Actions v2 (приоритет)

```yaml
interaction:
  actions:
    - click: RIGHT          # LEFT, MIDDLE, SHIFT_*, ANY
      type: MESSAGE         # PLAYER_CMD, CONSOLE_CMD, OP_CMD, SWITCH_SERVER
      value: "<green>Привет!</green>"
      delay-ticks: 0
      cooldown-seconds: 0   # 0 = глобальный cooldown NPC
```

- **ANY** — срабатывает на любой клик; порядок: сначала specific, потом ANY.
- **delay-ticks** — цепочка с накопленной задержкой.
- **SWITCH_SERVER** — BungeeCord `Connect`; fallback: консольная `send <player> <server>`.
- Плейсхолдеры: `{player}`, `{uuid}`, `{npc}`, `{world}` + **PlaceholderAPI** (если установлен).
- Per-NPC: `permission`, `cooldown-ms`, `click-sound`, `deny-sound`.

При загрузке пустой `actions` + legacy-строки → автоматическая одноразовая миграция в actions.

---

## Видимость

Per-NPC в `npcs/<id>.yml`:

```yaml
packet-view-distance: 0      # 0 = config performance.packet-view-distance
hologram-view-distance: 0    # 0 = packet или global hologram
visibility-permission: ""    # пусто = все; иначе player.hasPermission
```

Настраивается в GUI редактора (слоты visibility). Проверка при spawn packet-модели и голограммы.

---

## Скины (player-NPC)

В `appearance`:

| Поле | Описание |
|------|----------|
| `skin-source` | `NICK`, `URL`, `FILE`, `MINESKIN_ID` |
| `profile` | ник / ключ SkinRestorer |
| `skin-url` | URL текстуры |
| `skin-file` | файл в `plugins/SoulNPC/skins/` |
| `skin-layers` | маска частей скина (`-1` = все слои) |

Приоритет резолверов: URL/FILE → SkinRestorer → Mojang.  
Экипировка player-модели — отдельно в GUI «Одеть NPC».

---

## Голограмма

- Строка **name** + до 13 **extra-lines** (`NpcHologramLineData`).
- Тип строки: **TEXT** (MiniMessage) или **ITEM** (ItemDisplay: material, CMD, ItemsAdder/Nexo id, scale).
- PAPI в тексте при показе (если PlaceholderAPI есть).
- Скрытие строк, offset/spacing/scale, see-through, shadow, background — в GUI и yaml.

---

## Mob-NPC

- `appearance.type: MOB` + `entity-type` (`zombie`, `fox_snow`, …).
- **mob-properties** — map ключ → значение:

  | Ключ | Пример |
  |------|--------|
  | `baby` | `true` / `false` |
  | `fox_variant` | `RED`, `SNOW` |
  | `sheep_color` | `RED`, `BLUE`, … |
  | `creeper_charged` | `true` |
  | `villager_profession` | `farmer`, `librarian`, … |
  | `wolf_collar` | цвет ошейника |

- **mob-equipment** — main/off hand, helmet, chest, legs, boots (packet equipment).
- Анимации MOB_POSE, статичные позы (allay, bat, …) — как раньше.

---

## Public API

Плагин грузится на **STARTUP**; API доступен после enable.

```java
SoulNpcApi api = SoulNpcApi.get();
NpcRegistry registry = api.getRegistry();

registry.getById("1");
registry.getAll();
registry.create(npcFileData);
registry.delete("1");
```

**События:**

| Событие | Когда |
|---------|-------|
| `SoulNpcClickEvent` | до выполнения actions; **Cancellable** |
| `SoulNpcCreateEvent` | после создания NPC |
| `SoulNpcDeleteEvent` | после удаления |

**Фабрика actions:** `NpcActionFactory.message(...)`, `playerCommand(...)`, `switchServer(...)`, `withDelay(...)`, …

API не проверяет Bukkit-permissions — вызывающий плагин ответственен за авторизацию.

---

## Файлы на диске

```
plugins/SoulNPC/
├── config.yml          # основные настройки, storage, performance
├── gui/
│   ├── admin.yml
│   └── npc-edit.yml
├── lang/
│   ├── ru.yml
│   └── en.yml
├── npcs/               # при storage.type: yaml
│   └── 1.yml
├── skins/              # PNG для skin-source: FILE
└── storage/
    └── soulnpc.db      # при storage.type: sqlite
```

Тексты игрокам — `lang/*.yml`. Числа, permissions, layout GUI — config/gui settings в Java + yaml на диске.
