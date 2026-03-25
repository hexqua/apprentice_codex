---
name: forward-port-1-21-1
description: Standard workflow for forward-porting selected `main` commits from this 1.20.1 Forge repository into `1.21.1-main`. Use when work involves choosing non-merge SHAs, planning or reviewing `git cherry-pick -x`, resolving Forge to NeoForge differences, cleaning stale generated JSON, or checking 1.21.1 enchantment, repair, and tag behavior after a port.
---

# Forward Port 1.21.1

## Overview

`main` から `1.21.1-main` へ forward-port するときの標準手順を扱う。通常の実装タスクとして進めず、対象 SHA、generated cleanup、1.21.1 固有確認を先に固定してから作業する。

## Quick Start

1. 取り込み対象の非 `merge` コミットを洗い出し、取り込む SHA を確定する。
2. `references/port-checklist.md` を読み、`cherry-pick`、cleanup、検証までの順序を固定する。
3. datagen、resource 移動、削除・改名、旧書式移行を含む場合は `references/generated-cleanup.md` を読む。
4. 装備、武器、特殊アイテム、修理素材変更が含まれる場合は `references/enchant-repair.md` を読む。

## Workflow

### 1. 取り込み対象を確定する

- `merge` コミットはそのまま取り込まない。
- 1 機能を構成する連続コミット列を維持し、`git cherry-pick -x <sha...>` を使う。
- 合意なしでこの repo から port 作業を始めない。着手判断が済んだあとにこの skill を使う。

### 2. Datagen と resource の事故を先に防ぐ

- 削除・改名・出力パス変更・旧書式からの移行がある場合は、`runData` の前に影響する generated JSON を明示削除する。
- `src/generated/resources/.cache` は Git 管理外なので、`runData` だけでは古い JSON が消えない前提で扱う。
- `src/main/resources/data` の手置き JSON は datagen 管理外なので、配置移行や stale build 出力を別途確認する。

### 3. 1.21.1 固有差分を確認する

- enchant 適用可否は Java 側の override だけで完了と判断しない。
- `data/*/enchantment/*.json` の `supported_items` / `primary_items` と item tag を確認する。
- 修理可否は 1.20.1 の `isValidRepairItem` を port したかどうかではなく、1.21.1 側の素材定義と最終挙動で確認する。

### 4. 最後に検証する

- `./gradlew.bat runData` 後に `git diff --name-status -- src/generated/resources` を確認する。
- 必要に応じて `rg -n "forge:conditions|canApplyAtEnchantingTable|isBookEnchantable|supportsEnchantment|isValidRepairItem" src/generated/resources src/main/resources` を実行する。
- 最後に `./gradlew.bat build` を成功させる。

## References

- 標準手順: `references/port-checklist.md`
- generated cleanup 判断: `references/generated-cleanup.md`
- 付呪・修理・tag 差分: `references/enchant-repair.md`
