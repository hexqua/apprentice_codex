# AGENTS.md

このファイルは、このリポジトリで作業する人間/AIエージェント向けの共通ルールを定義する。

## 0. 言語ポリシー
- 本プロジェクトでのやり取り、ドキュメント、レビューコメントは原則として日本語を使用する。
- コミットメッセージは日本語で記述する。
- 外部資料が英語の場合は、日本語で要点を補足する。ツール仕様などで英語が必須の箇所のみ、必要最小限で英語を使用する。

## 1. 目的
- Apprentice's Codex を開発し、主に Iron's Spells 'n Spellbooks 向けの追加コンテンツを提供する。
- 作業手順、品質基準、言語ポリシー、レビュー観点をそろえ、再現性のある開発を行う。

## 2. 開発環境
- 開発対象: Minecraft 1.21.1
- Mod ローダー: NeoForge 21.1.219
- 言語/実行環境: Java 21
- ビルドツール: Gradle Wrapper（Windows では `./gradlew.bat` を使用）
- 主要依存 MOD: Iron's Spells 'n Spellbooks（1.21.1-3.15.4）, Curios（9.5.1+1.21.1）, GeckoLib（4.8.3）
- ローカルでは `JDK21_HOME` を設定し、必要に応じて `.\scripts\use-java.ps1` で `JAVA_HOME` を切り替える。
- `main`（1.20.1）を触る場合のみ Java 17 を使う。

## 3. 実行コマンド
- Java 21 を一時適用:
```powershell
.\scripts\use-java.ps1
```
- Java 17 を一時適用（`main` / 1.20.1 作業用）:
```powershell
.\scripts\use-java.ps1 -Version 17
```
- 通常ビルド:
```powershell
.\scripts\use-java.ps1
./gradlew.bat build
```
- GameTest サーバー:
```powershell
.\scripts\use-java.ps1
./gradlew.bat runGameTestServer
```
- 開発クライアント:
```powershell
.\scripts\use-java.ps1
./gradlew.bat runClient
```
- Datagen 再生成:
```powershell
.\scripts\use-java.ps1
./gradlew.bat runData
```
- jar 出力確認:
```powershell
Get-ChildItem build\libs\*.jar
```
- `runClient` は GUI を起動するため、CI やヘッドレス環境では実行しない。
- `runGameTestServer` はサーバー側の登録、データ読込、レシピ、生成まわりの検証に使う。renderer / screen など client 専用の起動不良は別途 `runClient` で確認する。
- 通常確認で `clean` は付けない。必要時のみ `./gradlew.bat clean build` を使う。
- `runData` の出力先は `src/generated/resources` であり、`src/generated/resources/.cache` に記録された生成物だけが再生成・差分管理される前提で扱う。
- `src/generated/resources/.cache` は Git 管理外のため、branch 切替・`cherry-pick`・手動コピーで持ち込んだ古い JSON は `runData` だけでは削除されない場合がある。
- `main` から `1.21.1-main` へ forward-port する場合は、後述の専用 Skill を使って作業手順と確認観点を確認する。

## 4. コーディング規約
- クラス/インターフェースは `PascalCase`、メソッド/フィールド/ローカル変数は `camelCase`、定数は `UPPER_SNAKE_CASE` を使用する。
- レジストリ名、リソース ID、JSON ファイル名は `snake_case` を使用し、`apprenticecodex` 名前空間を前提にする。
- 追加要素の登録処理は既存の `registry` パッケージ構成に合わせ、初期化時に一元登録する。
- データ駆動で表現できる内容は `src/generated/resources` と datagen を優先し、ハードコードを最小化する。
- コメントは「何をしているか」より「なぜそうするか」を優先する。外部 MOD 仕様依存、ワークアラウンド、クライアント/サーバー差分、実行順依存、魔法値には日本語コメントを残す。
- テキストファイルは UTF-8（BOM なし）を原則とする。
- 日本語を含むファイルや文字化けが疑われるファイルを扱う場合は、英語で書かれた `.codex/skills/text-encoding-hygiene` を先に確認する。
- 依存関係の追加・更新は `gradle.properties` に集約し、必須依存を追加する場合は `neoforge.mods.toml` も更新する。外部アセット/ライブラリ利用時は `THIRD_PARTY_NOTICES.md` の追記要否を確認する。

## 5. 変更フロー
1. 変更内容を 1〜2 文で決める（何を、なぜ変えるか）。
2. 実装する。
3. `git diff` / `git diff --name-status` で、依頼範囲外の整形、rename、コメント削除、文字化け差分が混ざっていないことを確認する。
4. コード、リソース、依存、datagen に影響する変更では `./gradlew.bat build` を成功させる。このビルドには明らかな文字化けと UTF-8 BOM の検査も含める。ドキュメントのみの変更では省略してよいが、最終報告に理由を残す。
5. サーバー側の登録、データ読込、レシピ、生成、GameTest 対象構造に影響する変更では `./gradlew.bat runGameTestServer` を成功させる。
6. `main` から `1.21.1-main` への forward-port では、実装内容に関係なく `./gradlew.bat runGameTestServer` と `./gradlew.bat build` を成功させる。
7. client 専用 UI、renderer、screen、入力操作に影響する変更では必要に応じて `./gradlew.bat runClient` で確認する。
8. コミットはレビューしやすく、forward-port しやすい粒度に分ける。無関係な整形や広域整理を混ぜない。
9. `1.21.1-main` へ取り込む変更は必ずブランチ + PR で流し、直 push しない。
10. PR では GitHub Actions の `PR CI / build-and-gametest`、Codex Cloud のスマートトリガーレビュー、人間のレビューを確認してから取り込む。スマートトリガーレビューは補助であり、CI と人間の判断を置き換えない。
11. 通常は merge commit で取り込む。バージョン更新だけは rebase merge を使ってよい。squash merge は使わない。

## 6. レビューチェックリスト
- Java 21 環境で必要な検証が通っていること。コード/リソース変更では `./gradlew.bat build` を必須とする。
- サーバー側の登録、データ読込、レシピ、生成に影響する変更では `./gradlew.bat runGameTestServer` が成功していること。
- `main` から `1.21.1-main` への forward-port では、実装内容に関係なく `./gradlew.bat runGameTestServer` と `./gradlew.bat build` が成功していること。
- `1.21.1-main` 向け PR では GitHub Actions の `PR CI / build-and-gametest` が成功していること。
- Codex Cloud のスマートトリガーレビューで指摘が出ている場合、対応または明示的な見送り理由があること。
- 追加・変更した要素の登録漏れ（Registry/EventBus）がないこと。
- サーバー専用環境で問題となるクライアント専用参照を追加していないこと。
- 1 機能が `cherry-pick` しやすい独立したコミット列として保たれていること。
- generated/resource の削除・改名・出力パス変更がある場合、forward-port 先で stale 出力に気づける差分になっていること。
- 既存コンテンツの ID 変更や削除による互換性破壊を避けていること。
- 依存 MOD バージョン条件を変更した場合、`neoforge.mods.toml` と `gradle.properties` の整合性が取れていること。

## 7. ドキュメント更新
- コード変更時に関連しやすいファイル: `gradle.properties`（バージョン）、`build.gradle`（依存/タスク）、`src/main/resources/META-INF/neoforge.mods.toml`（依存条件）、`README.md`（仕様/導入手順）、`THIRD_PARTY_NOTICES.md`（ライセンス）、`.codex/skills/**`（エージェント向け手順）。
- 実装変更と同一 PR/コミット内で関連ドキュメントを更新し、差分の理由が追跡できる状態にする。
- 実行手順や開発フローに影響する変更は `AGENTS.md` も同時更新する。
- GitHub Actions / Ruleset / merge / Codex Cloud レビュー運用に変更がある場合は `docs/github-pr-protection.md` も更新する。
- データパック系の利用者向け情報は順次 GitHub wiki へ移行する。README には詳細仕様を戻さない。

## 8. ブランチ間取り込み（1.20.1 -> 1.21.1）
- `main`（1.20.1）を開発基準ブランチとし、`1.21.1-main` への反映は forward-port（`cherry-pick`）で行う。
- `main` と `1.21.1-main` の直接 `merge` は原則禁止とし、必要な場合は事前合意を必須とする。
- `merge` コミットの直接 `cherry-pick`（`git cherry-pick -m` を含む）は禁止とし、取り込み対象は個別コミット単位で扱う。
- 実作業では `.codex/skills/forward-port-1-21-1` を使用する。
- Skill を使う理由: generated JSON の削除漏れ、1.21.1 の enchant/repair/tag 差分、旧版書式の残留確認は通常作業では不要であり、常設ルールと分離した方が見落としにくいため。
- 取り込み前に対象コミットを個別 SHA で確定し、`git cherry-pick -x` を使う。
- 削除・改名・出力パス変更・旧書式移行を含む datagen 作業では、`runData` 前に影響ディレクトリの generated JSON を明示削除する。
- 1.21.1 の enchant 適用可否や修理可否は Java の override だけで完了と判断せず、enchantment JSON と item tag も確認する。
- 1 機能を独立した連続コミット系列として保ち、`cherry-pick` しやすくする。
- `merge` コミットしか見つからない場合でも、その `merge` 自体は取り込まず、元になった個別コミットを洗い出してから取り込む。
- 取り込み判断で迷わないよう、`main` 側では無関係な整形・リネームの混在を避ける。
- 同種コンフリクトの再解決コストを下げるため、`git config rerere.enabled true` を推奨する。
- `1.21.1-main` のみで必要になった修正は、`main` への逆取り込みが必要かを別途判断し、必要時のみ個別対応で反映する。

## 8.1 `1.21.1-main` の PR CI 運用
- `1.21.1-main` への反映は PR 経由のみとし、直接 push しない。
- required check は `PR CI / build-and-gametest` とする。
- workflow は `pull_request` でのみ動かし、`pull_request_target` は使わない。
- CI では repository secrets を使わず、`GITHUB_TOKEN` は read-only に固定する。
- 通常変更は `merge commit` を使い、バージョン更新 PR だけ `rebase merge` を許可する。
- `squash merge` は使わない。
- bootstrap 詰まりを避けるため、workflow を target branch に載せる前に required check を有効化しない。手順は `docs/github-pr-protection.md` に従う。

## 9. Codex運用上の注意
- PowerShell 5.1 などの環境では、UTF-8 の日本語ファイルでも既定の `Get-Content` 表示が文字化けすることがある。この環境差は完全には解消できないため、運用でカバーする。
- 日本語を含むファイルは UTF-8 として読み書きし、文字化け表示が出た状態では編集しない。
- 日本語を含むファイルを PowerShell で読む前に `[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)` を設定し、`Get-Content -Encoding UTF8` または `[System.IO.File]::ReadAllText($path, [System.Text.Encoding]::UTF8)` を使う。
- PowerShell 5.1 では `Set-Content` / `Out-File` の既定エンコーディング書き込みを使わない。シェル経由で保存が必要な場合は UTF-8 BOM なしを明示する。
- Python などの検証ツールが既定エンコーディングで日本語ファイルを読めない場合は、`PYTHONUTF8=1` を一時設定して UTF-8 読みを強制する。
- 文字化け表示（例: `縺`）が見えた場合は編集を中断し、UTF-8 指定で再読込して正常表示を確認してから作業する。
- 文字化けが疑われる場合や日本語コメント・翻訳・ドキュメントを広く触る場合は、英語で書かれた `.codex/skills/text-encoding-hygiene` を先に確認する。
- 編集は必要最小限の差分に限定し、ファイル全体の再書き込みや無関係なコメント整理を避ける。
- 変更後は `git diff` を確認し、依頼範囲外のコメント削除や日本語の文字化け差分があれば修正してから完了する。
- コード、リソース、依存、datagen に影響する変更では `./gradlew.bat build` を通し、`checkTextEncodingHygiene` による明らかな文字化け検査も成功させる。
- ユーザーが事前確認やローカルレビューを明示した作業では、明示指示があるまで push、PR 作成、リモート操作を行わない。

## 10. 直近の注意傾向
- 直近では Epic Fight 連携、簡体字中国語翻訳、説明文・字幕・GameTest 拡充が続いている。関連差分では、任意依存あり/なしの起動、翻訳キーの整合、サーバー側 GameTest の追加要否を優先して確認する。

## 11. 禁止事項
- 事前合意なしで大規模リファクタをしない。
- 機密情報をコミットしない。
