---
name: forward-port-ready-development
description: Keep 1.20.1 Forge changes easy to forward-port into `1.21.1-main`. Use when implementing or reviewing features, registry updates, datagen/resource edits, dependency changes, refactors, or commit splits in this repository and you need to keep cherry-pick boundaries, loader boundaries, and stale generated/resource risks visible.
---

# Forward-Port Ready Development

## Overview

1.20.1 Forge 側の通常開発で、`1.21.1-main` へ運びやすい差分構成を保つための確認手順を扱う。port 作業そのものではなく、「今この変更をどう切ると後で事故りにくいか」を先に整えるために使う。

## Quick Start

1. 変更を「共通ロジック」「1.20.1 固有接着コード」「generated/resource 更新」に分けて考える。
2. `references/commit-shaping.md` を読み、1 機能をどの粒度でコミットに分けるか決める。
3. datagen や JSON 配置変更がある場合は `references/resource-diff-hygiene.md` を読む。
4. Forge 固有 API や登録コードを触る場合は `references/loader-boundary.md` を読む。
5. 実装後に `git diff --name-status` と `./gradlew.bat build` で差分と build を確認する。

## Workflow

### 1. 変更面を分離する

- 共通ロジックと Forge 固有接着コードを同じ説明抜きの差分に押し込まない。
- 1.21.1 側で書き直しそうな箇所は、Java 実装の形だけで意図が伝わると決めつけない。
- 迷ったら「共通ロジックを先に独立させ、接着コードは別コミットに逃がす」を優先する。

### 2. コミットを移植しやすく切る

- 1 機能を独立した連続コミット系列で保つ。
- 無関係な整形、rename、広域整理を同じコミットへ混ぜない。
- resource の削除・改名・移設は、追加差分に埋もれさせず削除差分が見える形で残す。

### 3. 移植判断に必要な理由を残す

- 外部 MOD 仕様依存、ワークアラウンド、実行順依存、server/client 差分には日本語コメントを残す。
- コメントは「1.20.1 でこう書いた理由」と「1.21.1 側でそのまま持っていけると思わない方がよい理由」を短く示す。

### 4. 実装後に差分を洗う

- `git diff --name-status` で無関係差分の混入と削除差分の見落としを確認する。
- datagen や手置き JSON を触ったら、旧配置が消えているかを差分上で確認する。
- 最後に `./gradlew.bat build` を通し、レビュー時に「port しやすい差分か」を説明できる状態にする。

## References

- コミット分割と混在禁止例: `references/commit-shaping.md`
- generated / resource 差分の扱い: `references/resource-diff-hygiene.md`
- Forge 固有接着コードの境界: `references/loader-boundary.md`
