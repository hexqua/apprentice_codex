---
name: forward-port-1-21-1
description: Forward-port tasks from the `main` branch (Minecraft 1.20.1) to `1.21.1-main` in this repository. Use when work involves `git cherry-pick -x`, conflict resolution between the two branches, `runData` regeneration, cleanup of stale generated JSON, or checking 1.21.1-specific enchantment, repair, and item-tag differences after a port.
---

# Forward Port 1.21.1

## Overview

`main` から `1.21.1-main` へ forward-port するときの標準手順を扱う。通常の実装タスクとして進めず、移植専用の確認項目を先に固定してから作業する。

## Quick Start

1. 移植対象の非 `merge` コミットを洗い出し、取り込む SHA を確定する。
2. 日本語ファイルや文字化けが疑われる差分を扱う場合は、先に `.codex/skills/text-encoding-hygiene` を読む。
3. `references/port-checklist.md` を読み、`cherry-pick`、generated cleanup、`runData`、`runGameTestServer`、build までの順序を固定する。
4. 取り込み対象に装備・武器・特殊アイテム・修理素材変更が含まれる場合は `references/enchant-repair.md` も読む。
5. generated JSON の削除範囲に迷う場合は `references/generated-cleanup.md` を読む。

## Workflow

### 1. 取り込み方針を固める

- `merge` コミットはそのまま取り込まない。
- 1 機能ごとに連続したコミット系列を保ち、`git cherry-pick -x <sha...>` で取り込む。
- コンフリクト時は `1.21.1-main` の既存差分を優先し、port に不要な 1.20.1 実装を戻さない。

### 2. Datagen 事故を先に防ぐ

- 削除・改名・出力パス変更・旧書式からの移行がある場合は、`runData` の前に影響する generated JSON を明示削除する。
- `src/generated/resources/.cache` は Git 管理外なので、`runData` だけでは古い JSON が消えない前提で扱う。
- `src/main/resources/data` 配下の手置き datapack JSON は datagen では直らない。custom recipe などを port する場合は、1.21.1 側の配置規約との差分も別途確認する。
- 削除範囲の判断基準は `references/generated-cleanup.md` を使う。

### 3. 1.21.1 差分を確認する

- enchant 適用可否は Java 側の override だけで完了と判断しない。
- `data/*/enchantment/*.json` の `supported_items` / `primary_items` と、参照される item tag を確認する。
- 武器系は独自 tag だけでなく、`minecraft:enchantable/sword` / `weapon` / `sharp_weapon` / `fire_aspect` など vanilla 側の item tag も確認する。
- 修理可否は 1.20.1 の `isValidRepairItem` 実装が不要になったと決めつけず、個別 override と素材定義を確認する。
- 詳細な確認観点は `references/enchant-repair.md` を使う。

### 4. 検証する

- `./gradlew.bat runData` 後に `git diff --name-status -- src/generated/resources` を確認し、不要 JSON の削除漏れを見逃さない。
- custom recipe など手置き JSON を動かした場合は、`src/main/resources/data` と `build/resources/main/data` の両方に旧配置が残っていないことを確認する。
- 必要に応じて `rg -n "forge:conditions|canApplyAtEnchantingTable|isBookEnchantable|supportsEnchantment|isValidRepairItem" src/generated/resources` を実行し、1.20.1 前提が残っていないか確認する。
- enchant 可否の GameTest を直す場合は、`Item#supportsEnchantment` などの Java 側だけでなく `Enchantment#canEnchant(ItemStack)` も検証対象に含める。
- forward-port では変更内容に関係なく `./gradlew.bat runGameTestServer` を実行し、GameTest が通ることを確認する。
- 最後に `./gradlew.bat build` を成功させ、文字化け検査も通す。

## References

- 標準手順: `references/port-checklist.md`
- 付呪・修理・tag 差分: `references/enchant-repair.md`
- generated cleanup 判断: `references/generated-cleanup.md`
