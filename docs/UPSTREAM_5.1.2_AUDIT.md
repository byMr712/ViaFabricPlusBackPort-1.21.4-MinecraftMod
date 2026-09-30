# Аудит: upstream ViaFabricPlus 5.1.1 -> 5.1.2 (применимость к 1.21.4-форку)

**Дата:** 2026-09-29
**Источники:** `ViaFabricPlus-ver-26.3 (1).zip` (upstream `430c2520` = релиз 5.1.1) и
`ViaFabricPlus-ver-26.3 (2).zip` (upstream `639bb9ed` = 5.1.2-SNAPSHOT).
**Вердикт:** переносить нечего — **0 из 20 изменений применимы**. Код форка не менялся.

---

## Почему так

Два независимых препятствия:

1. **Направление эмуляции перевёрнуто.** Почти все новые фиксы upstream имеют вид
   `if (targetVersion.olderThanOrEqualTo(v26_2)) { <старое поведение> }`, т.е. это эмуляция
   поведения клиента **≤ 26.2** на клиенте **26.3**. Клиент этого форка — **1.21.4**, который
   нативно и *является* «старым» клиентом. Условие в форке всегда `true` → код попадёт в ветку,
   идентичную нативному поведению 1.21.4, т.е. в мёртвый код.

2. **Две мажорные версии разрыва.** Между 1.21.4 и 26.3 половина целей миксинов просто не
   существует: `BlockTransformer`, `BlockTransformers`, `DataComponents.BLOCK_TRANSFORMER`,
   `EntityFluidInteraction`, `LevelReader`, `LevelEvent`, `LevelEventHandler`, `ShelfBlock`,
   `SwingAnimation`, `InteractionResult.Success`, `ServerboundAcceptTeleportationPacket`,
   а также методы `MinecraftClient.pauseGame/continueAttack`,
   `MultiPlayerGameMode.startDestroyBlock/continueDestroyBlock/stopDestroyBlock/performUseItemOn`
   и `ItemStack.applyAfterUseComponentSideEffects`.

Форк подключается к серверам **1.7 — 26.3** (направление ViaBackwards: клиент *старше* сервера),
поэтому «новые» фиксы upstream к этой архитектуре не относятся.

---

## Проверка по каждому изменению

Проверено в байткоде `1.21.4-net.fabricmc.yarn.1_21_4.1.21.4+build.8-v2` из кэша fabric-loom.

| Коммит | Фикс | Вердикт | Что в 1.21.4 |
|---|---|---|---|
| `e8d8873a` | Не блокировать щитом для мотыги/лопаты (≤26.2) | ❌ | нет `BlockTransformer` / `DataComponents.BLOCK_TRANSFORMER` |
| `4f02320d` | Тушение костра лопатой (≤26.2) | ❌ | `ShovelItem.useOnBlock` **уже** вызывает `CampfireBlock.extinguish` |
| `4c7b2d6d` | Поток жидкости: tag-проверка → `BlockState#isSolid` (≤26.2) | ❌ | `FlowableFluid.isFlowBlocked` = `isSideSolidFullSquare`; форк уже адаптирует в `features/movement/water/MixinFlowableFluid` |
| `3b4bffbf` | Не применять use cooldown/remainder на блоках (≤26.2) | ❌ | нет `ItemStack.applyAfterUseComponentSideEffects` |
| `9bf51e03` | Звук всплеска зелья (≤26.2) | ❌ | нет класса `LevelEvent`; звук задаётся самим particle-типом |
| `0fa624dc` | Партиклы разрушения блока (≤26.2) | ❌ | нет `LevelEventHandler` / `addBreakingBlockEffects`; есть клиентский `ClientWorld.addBlockBreakParticles(BlockPos, BlockState)` |
| `1f912f3d` | Не останавливать разрушение на паузе (≤26.2) | ❌ | нет `MinecraftClient.pauseGame` и `stopDestroyBlock` |
| `7648d476` | Не останавливать разрушение на телепорте (≤26.2) | ❌ | нет `MultiPlayerGameMode.stopDestroyBlock` |
| `519c115f` | Взаимодействие с shelf-блоком (≤26.2) | ❌ | нет `ShelfBlock` (в 1.21.4 — `ChiseledBookshelfBlock`) |
| `8abd8500` | Элитра в лаве (≤26.2) | ❌ | `PlayerEntity.checkGliding` уже проверяет `isTouchingWater()` (только вода) |
| `4bd80383` | Взаимодействие с бродячим торговцем (≤26.2) | ❌ | `WanderingTraderEntity.interactMob` уже отдаёт `ActionResult.SUCCESS` |
| `41b229d5` | Взаимодействие с вагонеткой (≤26.2) | ❌ | `AbstractMinecartEntity.interact` уже отдаёт `ActionResult.SUCCESS` |
| `c76de147` | Взаимодействие с верблюдом (≤26.2) | ❌ | `CamelEntity.interactMob` = `ActionResult.SUCCESS`; точки входа `InteractionResult.FAIL` нет |
| `ba05b624` | Высота жидкости для взаимодействий (≤26.2) | ❌ | нет класса `EntityFluidInteraction` |
| `32a7478d` | Не включать step height в коллизии сущностей (≤26.2) | ❌ | `Entity.move` **уже** вызывает `getStepHeight()` (как в 26.3); форк уже имеет `features/movement/collision/MixinEntity` |
| `28064606` | Max-edge блоки с жидкостью (≤26.2) | ❌ | нет `LevelReader.containsAnyLiquid` (в 1.21.4 — `BlockView`, метода нет) |
| `509477fb` | Порядок пакетов телепорта в 1.21.2 | ❌ | `TeleportConfirmC2SPacket` содержит только `teleportId`, без позиции → текущий код `features/networking/packet_handling/MixinClientPlayNetworkHandler` уже корректен |
| `ca3591b9` | Реорганизация миксинов по подпакетам | ⬜ | структурное: у форка своя раскладка `features/<topic>/` |
| `40acdfbd` | Отключить `mavenLocal()` | ⬜ | билд: в `build-logic/.../vfp.base-conventions.gradle` `mavenLocal()` и так нет |
| `639bb9ed` | BaseProject → `build-logic` + version catalog | ⬜ | билд: у форка свой convention-плагин |
| `8f96c699` | Переводы Crowdin (`ja_jp`, `lzh`) | ⬜ | ключи новых фич; в форке их нет даже в `en_us.json` |

---

## Что делать, если появится следующий апстрим-релиз

Действует то же правило, но в зеркальном направлении: фикс применим, только если
1. его целевой класс/метод **есть** в 1.21.4, и
2. «старое» поведение **не совпадает** с нативным поведением 1.21.4, и
3. он касается версий **старше 1.21.4** (то есть реально влияет на коннект к старым серверам).

Фиксы вида «≤ 26.2 на 26.3-клиенте» отсеиваются сразу по пункту 1 и 3.

Перед переносом проверять байткод 1.21.4, а не текст upstream — расхождение имён
(yarn vs mojang) плюс два мажорных скачка версий делают «на глаз» ненадёжным:

    # yarn-mapped jar из кэша loom
    $jar = "$env:USERPROFILE\.gradle\caches\fabric-loom\minecraftMaven\net\minecraft\minecraft-merged\1.21.4-net.fabricmc.yarn.1_21_4.1.21.4+build.8-v2\minecraft-merged-1.21.4-net.fabricmc.yarn.1_21_4.1.21.4+build.8-v2.jar"
    javap -p -c -classpath $jar net.minecraft.item.ShovelItem
