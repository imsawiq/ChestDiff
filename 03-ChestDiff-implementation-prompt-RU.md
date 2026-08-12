# MASTER PROMPT РЕАЛИЗАЦИИ — ChestDiff

> Передай этот файл целиком сильному coding agent'у в пустом репозитории.
> Его задача — **реализовать мод**, а не пересказать идею или выдать план без кода.

## Миссия

Создай **ChestDiff** — отполированный полностью клиентский Fabric-мод, который запоминает контейнеры, реально открытые игроком, и показывает **что изменилось с момента последнего наблюдения этого же контейнера**.

Главный продуктовый посыл:

> **Открыл сундук — сразу увидел, что изменилось с прошлого раза.**

Пример:

```text
Изменения с прошлого наблюдения — 2d 7h ago

+ 128 Iron Ingots
+  17 Diamonds
-   1 Elytra
-  32 Golden Carrots

12 stack'ов просто переставлены
```

ChestDiff — не grief logger и не серверный audit log. Он не знает, **кто** менял содержимое и в какой точный момент, если клиент этого не наблюдал. Это локальная память состояний контейнеров, которые сервер уже законно показал клиенту.

## Обязательная техническая база

Сделай мод как **полностью клиентский Fabric-мод для Minecraft Java Edition**. На сервере не должно требоваться ничего: ни плагина, ни мода, ни кастомного протокола. Не отправляй на сервер собственные пакеты.

### Стратегия мультиверсионности

Используй **Stonecutter** как основную систему мультиверсионной сборки и препроцессинга.

- Gradle-плагин: `dev.kikugie.stonecutter`
- Базовая версия плагина на момент написания ТЗ: **0.9.7**
- Gradle-скрипты предпочтительно на Kotlin DSL.
- Один Git-репозиторий, одна логическая кодовая база.
- Stonecutter-ветвления использовать только там, где реально меняется Minecraft/Fabric API.
- Не копировать целые классы под каждую версию без необходимости.
- Самую новую поддерживаемую сборку (`26.1.2-fabric`) держать активным Stonecutter-окружением в репозитории.
- Добавить CI, который с чистого checkout собирает **все** поддерживаемые версии.

Поддержать отдельными Stonecutter target/node следующие стабильные версии, если официальные metadata подтверждают их существование:

`1.21`, `1.21.1`, `1.21.2`, `1.21.3`, `1.21.4`, `1.21.5`, `1.21.6`, `1.21.7`, `1.21.8`, `1.21.9`, `1.21.10`, `1.21.11`, `26.1`, `26.1.1`, `26.1.2`.

Если соседние версии действительно могут использовать один и тот же исходный код, это нормально, но итоговые jar и Modrinth metadata всё равно должны корректно объявлять реально поддерживаемые версии.

### Fabric / Loom / mappings

Используй **Fabric Loader + Fabric API + Fabric Loom**. Перед фиксацией версий зависимостей сверяйся с актуальными официальными Fabric metadata. **Не выдумывай номера версий библиотек.**

Критический разрыв:

- Minecraft `<= 1.21.11`: используй Loom-конфигурацию для обфусцированного Minecraft (`net.fabricmc.fabric-loom-remap`, где это соответствует актуальной документации).
- Minecraft `>= 26.1`: используй Loom-конфигурацию для необфусцированного Minecraft (`net.fabricmc.fabric-loom`, где это соответствует актуальной документации).
- На всём диапазоне, включая старые `1.21.x`, используй **официальные Mojang mappings** через Loom.
- Не строй старую половину проекта на Yarn, а новую на Mojang mappings, если нет подтверждённого технического блокера.
- На `26.1+` не должно быть зависимости от Yarn mappings.

Java toolchains:

- `1.21.x`: Java 21, если официальные требования конкретного релиза не говорят иначе.
- `26.1.x`: Java 25.
- Сам Gradle может запускаться на более новом JDK, но каждый Stonecutter target должен компилироваться правильным toolchain и иметь рабочий production run.

### Структура репозитория

Держи структуру примерно такой:

```text
/
├─ build.gradle.kts
├─ settings.gradle.kts
├─ stonecutter.gradle.kts
├─ gradle.properties
├─ versions/ или Stonecutter-managed metadata
├─ src/main/java/...
├─ src/main/resources/...
├─ .github/workflows/build.yml
├─ LICENSE
├─ README.md
├─ CHANGELOG.md
└─ docs/
```

Код самого мода предпочтительно писать на **Java**. Kotlin в runtime-коде использовать только при реальной выгоде. Для многоверсионных mixin'ов и миграций Java обычно проще поддерживать.

### Политика зависимостей

Минимизируй runtime-зависимости.

Обязательные:
- Fabric Loader
- только реально нужные модули Fabric API

Опционально:
- интеграция с Mod Menu, если он установлен

Не делай YACL, Cloth Config, Architectury или другой config-framework обязательным только ради одного экрана настроек. Если ванильный Screen + небольшой versioned JSON config дают меньше рисков на диапазоне `1.21 → 26.1`, используй их.

### Client-only и требования для Modrinth

- в metadata: `environment: client`
- мод должен работать, когда установлен только у игрока, при входе на обычный vanilla/Fabric сервер
- никакой телеметрии
- никакой аналитики
- никаких скрытых HTTP-запросов
- никаких remote config
- никакого X-Ray
- никакой автоматизации PvP
- никакого aim assist
- никакой автоматизации движения
- никаких макросов/автокликеров/реплея последовательностей ввода
- не утверждать, что мод знает данные, которых клиент на самом деле не получал
- все данные хранить локально
- если мод хранит историю/данные, обязательно дать понятную кнопку их удаления

### Планка качества

Это должен быть не proof-of-concept, а **релизный Modrinth-мод**.

Требования:
- отсутствие крашей при отсутствии опционального контекста
- защитная обработка modded screens, modded menus и modded items
- никакого блокирующего дискового I/O на render thread
- никакого бесконечного роста истории
- никакого log spam при обычной игре
- все пользовательские строки через translation keys
- клавиатурная навигация там, где это разумно
- tooltips для неочевидных элементов
- нормальные empty states
- стабильные миграции config/data schema
- подробное логирование только при включённом debug-режиме
- production run перед релизом
- unit tests для чистой логики
- GameTest/client tests там, где они реально полезны
- GitHub Actions для сборки всех Stonecutter target
- понятное имя артефакта, например `<modid>-<modversion>+mc<mcversion>.jar`

### Как должен вести себя coding agent

Не останавливайся на scaffold, псевдокоде или списке TODO.

1. Сначала проверь актуальную официальную документацию Fabric/Stonecutter и metadata зависимостей.
2. Создай полный репозиторий.
3. Реализуй рабочий MVP с настоящей логикой.
4. Запусти сборку.
5. Исправь compile/runtime ошибки активной версии.
6. Собери все Stonecutter targets.
7. Исправь cross-version поломки минимальным количеством препроцессорных веток.
8. Запусти тесты.
9. Подготовь релизные `README.md` и `CHANGELOG.md`.
10. Не оставляй заглушек, фиктивных реализаций и core-функций с комментариями "TODO later".

Если конкретный Minecraft hook отличается по версиям, **не угадывай имя метода по памяти**. Открой generated sources/bytecode нужного target и трассируй реальный путь вызовов.

## Продуктовые принципы

1. **Semantic diff, а не spam по слотам.**
2. **Один реальный контейнер должен иметь одну стабильную identity.**
3. **Никогда не утверждать, кто внёс изменения.**
4. **Никогда не раскрывать содержимое неоткрытых storage.**
5. **Открытие контейнера должно ощущаться нативно и без фриза.**
6. **История должна быть bounded.**
7. **Неопределённость virtual GUI нужно явно показывать.**

## Основной UX

### При открытии контейнера

Когда текущий container получил синхронизированное содержимое и diff готов, показать небольшой unobtrusive summary:

```text
ChestDiff
С прошлого наблюдения (вчера 21:42):
+128 Iron
-1 Elytra
ещё 3 изменения
```

Дать кнопку или keybind для открытия полного diff.

Не показывать огромный modal каждый раз.

### Первое посещение

Показать:

`Первое наблюдение — snapshot сохранён`

Никакого выдуманного diff.

### Полный экран diff

Разделы:

```text
Добавлено
+128 Iron Ingot
+17 Diamond

Убрано
-1 Elytra
-32 Golden Carrot

Изменено
Diamond Pickaxe
Durability: 1203 -> 1104
(только если continuity реально можно разумно вывести)

Переставлено
12 stack'ов перемещены, общее количество не изменилось
```

Возможности:

- search/filter;
- hide rearrangements;
- compare with previous snapshot;
- выбрать старый snapshot из history;
- copy container coordinates;
- открыть settings.

### Экран истории контейнеров

Карточка:

```text
Large Chest
Overworld
124, 68, -431

Последнее наблюдение: 8m ago
Snapshots: 8
Последний diff: -1 Elytra, +64 Rockets
```

Сортировка:

- last seen;
- dimension;
- distance from player, если dimension совпадает;
- custom name;
- recent changes.

**Не сканировать chunks** ради заполнения списка. Только локальная база уже наблюдавшихся контейнеров.

## Модель `ContainerIdentity`

Это критическая часть.

Не используй только screen title.

Модель:

```text
ContainerIdentity
- WorldScope
- ContainerKind
- stable locator
- epoch/generation
```

Варианты:

```text
BlockContainerIdentity
EntityContainerIdentity
EnderStorageIdentity
VirtualContainerIdentity
UnknownContainerIdentity
```

### `WorldScope`

Истории должны быть разделены по:

- singleplayer save identity;
- либо multiplayer server identity;
- player profile там, где storage персональный.

Два разных сервера с сундуком на `100 64 100` — **разные контейнеры**.

### Block containers

Хранить:

- dimension;
- block position/positions;
- block/container type;
- optional custom display name;
- epoch/generation.

Поддержать минимум:

- chest;
- trapped chest;
- large/double chest;
- barrel;
- shulker box;
- hopper;
- dispenser;
- dropper;
- furnace;
- smoker;
- blast furnace;
- другие vanilla block-backed menus, если identity определяется надёжно.

### Correlation между menu и block

Не выдумывай координаты.

Menu opening может не содержать block position напрямую. Поэтому:

1. перед взаимодействием/на interaction запомни block/entity target, с которым игрок реально взаимодействовал;
2. если совместимый menu открылся через короткое число tick'ов, свяжи их;
3. проверь menu/container type против target;
4. если уверенности недостаточно — использовать `VirtualContainerIdentity`/`UnknownContainerIdentity`.

Никаких "скорее всего это chest на координатах..." без evidence.

### Canonicalization double chest

Левая и правая half одного double chest должны всегда давать одну identity.

Если client world state позволяет:

- прочитать chest half/facing;
- определить соседнюю половину;
- получить две позиции;
- отсортировать позиции по стабильному правилу;
- использовать canonical pair.

Клик по другой половине не должен создавать отдельную history.

### Ender Chest

Ender inventory принадлежит игроку, а не конкретному placed block.

Использовать:

```text
EnderStorageIdentity(WorldScope, playerProfile)
```

Открытие Ender Chest в другом месте должно попадать в ту же history.

Не писать:
`Ender Chest at 500 70 500 changed`,
словно contents привязаны к блоку.

### Entity containers

Для chest minecart, chest boat и других container entities, если клиент видит стабильный entity UUID:

```text
EntityContainerIdentity(WorldScope, entityUuid, entityKind)
```

Если UUID/identity нельзя надёжно сохранить — degradation в unknown/ephemeral mode лучше, чем ложное объединение.

### Virtual GUI

Серверы часто используют chest-like screens как shop, menu, selector.

Если нет trustworthy world/entity identity:

```text
VirtualContainerIdentity(
  worldScope,
  menuType,
  titleFingerprint,
  sessionIdentity
)
```

Не объединяй все меню с title `Shop` навечно в один container.

По умолчанию можно вообще не вести долгую history ephemeral virtual GUI, пока пользователь не включит это явно.

## Замена контейнера и `epoch`

Coordinates недостаточно: сундук можно сломать и поставить новый на том же месте.

Следи **только за client-observed block changes** вокруг уже известных storage positions.

Если клиент увидел:

```text
known chest block
-> air / другой block
-> chest снова появился
```

то:

- предыдущий epoch закрыть;
- generation увеличить;
- новый container на тех же coords считать новой identity.

Если момент разрушения был пропущен, а новый snapshot радикально отличается, можно показать:

`Возможна замена/сброс контейнера`

Но эвристика должна быть консервативной. Не создавай новый epoch только потому, что кто-то полностью поменял содержимое.

## Snapshot model

```text
ContainerSnapshot
- snapshotId
- containerIdentity
- capturedAt
- gameTick/session
- titleAtCapture
- slotCount
- slots: SlotState[]
- aggregateIndex
```

`SlotState`:

```text
index
ItemFingerprint
count
```

### `ItemFingerprint`

Сравнение должно учитывать не только item ID.

Канонический fingerprint без count включает:

- registry item ID;
- data components/components;
- damage;
- custom name;
- enchantments;
- potion/custom data;
- relevant metadata.

Используй deterministic canonical representation + hash.

Например:
- enchanted Diamond Pickaxe != unenchanted Diamond Pickaxe;
- два shulker box с разным содержимым != один fingerprint, если содержимое легитимно доступно через item components.

## Когда делать authoritative snapshot

Не сохраняй snapshot сразу в constructor screen'а, пока слоты ещё могут быть пустыми/не синхронизированными.

Перед реализацией проследи реальный menu synchronization path конкретной Minecraft-версии.

Рекомендуемая модель:

```text
container screen/menu open
-> создать ContainerObservationSession

приходят/применяются authoritative slot/content updates
-> дождаться достаточно полного initial sync

-> capture OPENED_CURRENT_STATE
-> сравнить с прошлым persisted baseline
-> показать "changes since last visit"

пользователь двигает вещи в текущем открытии

close / settled debounce
-> capture FINAL_SESSION_STATE
-> он становится новым baseline
```

Не плодить snapshots на каждый одиночный slot packet.

Используй debounce/coalescing и понимание состояния menu.

## Semantic Diff Engine

Должен быть чистым unit-testable модулем.

Input:

```text
oldSnapshot
newSnapshot
```

Output:

```text
ContainerDiff
- added[]
- removed[]
- modified[]
- rearranged[]
- unchangedSummary
- confidence/warnings
```

### Aggregate diff

Сначала агрегировать количество по точному `ItemFingerprint`:

```text
oldTotals[fingerprint]
newTotals[fingerprint]

delta = new - old
```

Если:
- `delta > 0` -> added;
- `delta < 0` -> removed;
- `delta == 0` -> количество не изменилось.

Это главный способ не считать перестановку предметов удалением+добавлением.

### Rearrangement detection

Если aggregate totals полностью совпадают, но slot layout отличается:

- классифицировать как rearranged;
- посчитать затронутые slots/stacks;
- **не показывать** `-64 Diamonds` и `+64 Diamonds`.

Пример:

```text
было:
slot 2 = Diamond x64

стало:
slot 8 = Diamond x64
```

Результат:

`Diamond x64 rearranged`

а не add/remove.

### Modified-item pairing

Опционально находить вероятные modifications:

- base registry item совпадает;
- count relationship разумный;
- отличается небольшой набор components;
- slot/evidence поддерживают continuity.

Примеры:

- durability изменился;
- custom name изменился;
- enchantments изменились.

Но быть консервативным.

Если pairing сомнительный:
- лучше показать `removed old + added new`;
- не выдумывать `modified`.

### Nested container items

Если `ItemStack` shulker box содержит легитимно видимые data components с его содержимым, fingerprint должен отличать разные shulker contents.

Но top-level diff не должен автоматически превращаться в сотню строк внутреннего diff.

Можно добавить:

`Shulker Box changed [Inspect]`

и отдельный nested diff, **только если данные реально присутствовали на клиенте**.

## Семантика baseline

`Changes since last observed` означает:

**текущий initial synchronized state** сравнивается с **последним завершённым snapshot предыдущего observation session**.

То есть:

```text
previous persisted baseline
      vs
current initial synchronized snapshot
      -> diff "что изменилось пока меня здесь не было"

текущий пользователь двигает предметы

на close:
final snapshot становится следующим baseline
```

Это критически важно.

ChestDiff не должен через 10 секунд после открытия сказать пользователю, что вещи, которые **он сам только что передвинул**, являются изменениями "с прошлого визита".

## Persistence

Объём данных умеренный.

Структура:

```text
config/chestdiff/config.json

chestdiff-data/
  <scope-id>/
    manifest.json
    containers/
      <container-hash>.json.gz
```

В container file можно хранить:

- identity metadata;
- bounded snapshot history;
- last N diffs либо вычислять их on demand.

Требования:

- atomic temp write + rename;
- background I/O;
- gzip/compression;
- schema version;
- migration code;
- corruption recovery, сохраняющий читаемые snapshots.

### Retention defaults

Разумный default:

- 20 snapshots на container;
- max age около 30 дней для unpinned;
- favorite/pinned containers могут храниться дольше;
- global disk cap, например configurable 128–512 MB;
- LRU cleanup старых unpinned histories.

UI actions:

- `Delete this container history`;
- `Delete current world/server history`;
- `Delete all ChestDiff data`.

## Overlay / integration

Показывай маленький overlay/widget, который не закрывает inventory slots.

Например:

```text
[ Δ 5 изменений ]
```

По click/key открывается полный diff.

Требования:

- не полагаться на старые raw OpenGL вызовы;
- использовать актуальные Minecraft/Fabric rendering abstraction конкретной версии;
- rendering спрятать за version adapter;
- учитывать, что внутри поздних `1.21.x` и далее rendering APIs менялись;
- animation простая и отключаемая;
- настройка `Disable automatic overlay`, но history продолжает записываться.

## Configuration

Versioned JSON config.

Настройки:

- overlay enabled;
- overlay duration;
- show rearrangements;
- snapshots per container;
- retention age;
- global disk cap;
- save virtual containers: default OFF;
- record furnace-like utility containers;
- favorite/pinned containers;
- debug logging;
- coordinate copy format;
- time display format.

## Архитектура

Пример:

```text
<group>.chestdiff
├─ ChestDiffClient
├─ observation/
│  ├─ ContainerObservationManager
│  ├─ ContainerObservationSession
│  ├─ MenuSnapshotAdapter
│  └─ InteractionCorrelation
├─ identity/
│  ├─ ContainerIdentity
│  ├─ ContainerIdentityResolver
│  ├─ WorldScope
│  └─ BlockContainerCanonicalizer
├─ snapshot/
│  ├─ ContainerSnapshot
│  ├─ SlotState
│  └─ ItemFingerprint
├─ diff/
│  ├─ SemanticDiffEngine
│  ├─ ContainerDiff
│  ├─ DiffEntry
│  └─ RearrangementDetector
├─ storage/
├─ ui/
│  ├─ DiffOverlay
│  ├─ DiffScreen
│  ├─ ContainerHistoryScreen
│  └─ SettingsScreen
├─ compat/
└─ mixin/
```

## Multi-version adapters

Semantic diff и storage logic должны быть Minecraft-independent.

Предполагаемые границы:

```text
ScreenMenuAdapter
StackComponentAdapter
BlockContainerAdapter
ClientInteractionAdapter
GuiRenderAdapter
RegistryIdAdapter
WorldScopeAdapter
```

Stonecutter conditions держи локально в этих слоях и mixin'ах.

Изменение renderer, names или component APIs на конкретной версии не должно заставлять переписывать `SemanticDiffEngine`.

## Производительность

- никаких chunk scans;
- никаких world scans;
- не пересчитывать fingerprint всех слотов каждый frame;
- fingerprint считать только при snapshot-relevant изменениях;
- кешировать canonical fingerprint, когда безопасно;
- diff complexity примерно `O(slotCount + uniqueFingerprints)`;
- disk write только async;
- overlay rendering максимально лёгкий;
- обычный double chest должен открываться без заметного stutter.

## Тесты

### Unit tests semantic diff

Покрыть:

- no change;
- simple add;
- simple remove;
- count increase;
- count decrease;
- pure slot move;
- consolidation двух stacks в один;
- split одного stack в два;
- multiple identical stacks rearranged;
- enchanted vs unenchanted;
- named vs unnamed;
- durability modification pairing;
- shulker component fingerprint change;
- unknown/corrupt component serialization;
- snapshot schema migration.

### Identity tests

Проверить:

- одна double chest через обе half -> одна identity;
- одинаковые coords на двух серверах -> разные identity;
- Ender Chest из разных placed blocks -> одна Ender storage identity;
- наблюдаемое разрушение/замена chest -> новый epoch;
- virtual GUI не превращается в world chest;
- разные chest minecart entity UUID -> разные identities.

### Acceptance scenarios

1. Первый раз открыл chest -> baseline сохранён.
2. Закрыл, содержимое изменилось с другой стороны, снова открыл -> правильный semantic diff.
3. Только переставил stacks -> нет fake add/remove.
4. В текущей session сам передвинул вещи -> final baseline обновился, но эти действия не выдаются как away-changes.
5. Restart -> history сохранилась.
6. Double chest через обе половины -> одна history.
7. Другой сервер с теми же coords -> другая history.
8. Ender Chest semantics корректна.
9. Virtual shop GUI не получает fake coordinates.
10. Повреждённый newest storage file не крашит Minecraft.
11. Overlay можно выключить.
12. Серверу ChestDiff не нужен.
13. Все Stonecutter targets запускаются.

## Правила формулировок в UI

Допустимые формулировки:

- `Изменения с прошлого наблюдения`
- `Последнее наблюдение`
- `Наблюдалось: <time>`
- `Содержимое отличается от предыдущего snapshot`
- `Возможная замена контейнера`

Запрещённые, если нет реальных доказательств:

- `Player X забрал`
- `Кто-то украл`
- `Server log говорит`
- `Этот предмет был удалён ровно в 03:14`

Если изменение произошло, пока клиент был далеко/offline, точное время изменения **неизвестно**. Известны только два времени наблюдения.

## README / позиционирование на Modrinth

Short description:

> Показывает, что изменилось в сундуках и других контейнерах с момента, когда ты открывал их в прошлый раз.

Первый README пример:

```text
Вчера:
64 Diamonds, 1 Elytra

Сегодня:
63 Diamonds, 1 Elytra, 128 Rockets

ChestDiff:
-1 Diamond
+128 Rockets
```

Сразу после примера честно написать:

> ChestDiff — клиентская память, а не серверный audit log. Мод сравнивает только состояния контейнера, которые твой клиент реально видел, и не может определить, кто внёс изменение.

## Критерий готовности

Не считать проект завершённым, пока:

- first-visit baseline реально работает;
- повторное открытие создаёт semantic diff;
- rearrangements не превращаются в fake add/remove;
- double chest identity работает;
- Ender Chest identity работает правильно;
- world/server scopes полностью изолированы;
- replacement epoch работает хотя бы при наблюдаемом block replacement;
- persistence переживает restart;
- storage bounded;
- deletion controls работают;
- overlay существует;
- full diff UI существует;
- render thread не блокируется на I/O;
- все заявленные Stonecutter targets собираются;
- production client запускается минимум на представительном `1.21.x` и `26.1.x`;
- README готов к Modrinth;
- в core-функционале не осталось TODO/заглушек.
