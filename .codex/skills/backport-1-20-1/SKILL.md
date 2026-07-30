---
name: backport-1-20-1
description: Minecraft 1.21.1 / NeoForgeの`main`から、選択した変更をMinecraft 1.20.1 / Forgeの`1.20.1-main`へ実際にbackportする。非merge SHAの選定、`git cherry-pick -x`、target側のAPI・resource補正、enchantment・修理・tag確認、Java 17検証に使用する。mainでの通常開発やbackport-ready設計レビュー、1.20.1固有修正には使用しない。
---

# 1.20.1 Backport

`main`（Minecraft 1.21.1 / NeoForge / Java 21）から選んだ変更を、`1.20.1-main`（Minecraft 1.20.1 / Forge / Java 17）へ取り込む。

## 手順

1. `main` から必要最小限の非 `merge` コミット列を選ぶ。
   - 1 機能または 1 修正ずつ扱う。
   - `merge` コミットは `cherry-pick` しない。
   - `git cherry-pick -x` で移植元を記録する。
2. 移植先のプラットフォーム詳細は `1.20.1-main` を正とする。
   - Forge の登録処理と EventBus の構成を維持する。
   - Java 17 で利用できる構文と API に限定する。
   - 1.20.1 の resource 配置と schema を維持する。
3. 文字列どおりのコピーではなく、backport としてコンフリクトを解決する。
   - 実現可能な範囲で共通動作と公開済み content ID を維持する。
   - loader、Minecraft、依存 MOD バージョン固有の補正は、レビュー性が上がる場合に後続コミットへ分ける。
   - `main` とそろえるためだけに、1.20.1 で利用できない、または適さない optional MOD を導入しない。
4. generated resource と手置き resource を確認する。
   - 1.21.1 の recipe は `data/<namespace>/recipe`、1.20.1 では `data/<namespace>/recipes` を使用する。
   - NeoForge condition、data component 前提など、1.21.1 固有の出力を除去する。
   - generated content を変更する場合は 1.20.1 側で datagen を行い、移植先の出力を正とする。
5. enchantment と修理動作を再確認する。
   - 1.21.1 の tag や data-driven rule は、1.20.1 では Java override や Forge hook が必要になる場合がある。
   - エンチャントテーブル適用、エンチャント本適用、修理素材を個別に確認する。
6. Java 17 で検証する。
   - `.\scripts\use-java.ps1 -Version 17` を実行する。
   - `./gradlew.bat build` を実行する。
   - サーバー登録、データ読込、recipe、生成、GameTest 対象動作に影響する場合は `./gradlew.bat runGameTestServer` を実行する。
   - optional MOD 連携に触れる場合は対応する検証タスクも実行する。
   - 既存テストが flaky と疑われる場合は再実行して根拠を報告し、既知の不安定さで新規失敗を隠さない。
7. コミット列をレビューする。
   - 取り込んだコミットを `main` まで追跡できる状態にする。
   - 移植先の補正コミットでは、1.20.1 側だけ異なる理由を説明する。
   - `main` を `1.20.1-main` へ直接 `merge` しない。

## 参照

- 作業開始前に[Backportチェックリスト](references/backport-checklist.md)を読む。
- generated/resourceを含む場合は[Generated resourceの整理](references/generated-cleanup.md)を読む。
- item、enchantment、修理、関連tagを含む場合は[Enchantmentと修理の確認](references/enchant-repair.md)を読む。
