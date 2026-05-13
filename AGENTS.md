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
- 開発対象: Minecraft 1.20.1
- Mod ローダー: Minecraft Forge 47.4.10
- 言語/実行環境: Java 17
- ビルドツール: Gradle Wrapper（Windows では `./gradlew.bat` を使用）
- 主要依存 MOD: Iron's Spells 'n Spellbooks（1.20.1-3.15.4）, Curios（5.14.1+1.20.1）, GeckoLib（4.8.3）
- ローカルでは `JDK17_HOME` を設定し、必要に応じて `.\scripts\use-java.ps1` で `JAVA_HOME` を切り替える。
- `1.21.1-main` を触る場合のみ Java 21 を使う。着手前に必ず合意を取る。

## 3. 実行コマンド
- Java 17 を一時適用:
```powershell
.\scripts\use-java.ps1
```
- Java 21 を一時適用（`1.21.1-main` 作業用）:
```powershell
.\scripts\use-java.ps1 -Version 21
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
- jar 出力確認:
```powershell
Get-ChildItem build\libs\*.jar
```
- `runClient` は GUI を起動するため、CI やヘッドレス環境では実行しない。
- `runGameTestServer` はサーバー側の登録、データ読込、レシピ、生成まわりの検証に使う。renderer / screen など client 専用の起動不良は別途 `runClient` で確認する。
- 通常確認で `clean` は付けない。必要時のみ `./gradlew.bat clean build` を使う。

## 4. コーディング規約
- クラス/インターフェースは `PascalCase`、メソッド/フィールド/ローカル変数は `camelCase`、定数は `UPPER_SNAKE_CASE` を使用する。
- レジストリ名、リソース ID、JSON ファイル名は `snake_case` を使用し、`apprenticecodex` 名前空間を前提にする。
- 追加要素の登録処理は既存の `registry` パッケージ構成に合わせ、初期化時に一元登録する。
- データ駆動で表現できる内容は `src/generated/resources` と datagen を優先し、ハードコードを最小化する。
- コメントは「何をしているか」より「なぜそうするか」を優先する。外部 MOD 仕様依存、ワークアラウンド、クライアント/サーバー差分、実行順依存、魔法値には日本語コメントを残す。
- テキストファイルは UTF-8（BOM なし）を原則とする。
- 日本語を含むファイルや文字化けが疑われるファイルを扱う場合は、英語で書かれた `.codex/skills/text-encoding-hygiene` を先に確認する。
- 依存関係の追加・更新は `gradle.properties` に集約し、必須依存を追加する場合は `mods.toml` も更新する。外部アセット/ライブラリ利用時は `THIRD_PARTY_NOTICES.md` の追記要否を確認する。

## 5. 変更フロー
1. 変更内容を 1〜2 文で決める（何を、なぜ変えるか）。
2. 実装前に、変更を「共通ロジック」「1.20.1 固有の接着コード」「generated/resource 更新」「ドキュメント」に分けて考える。通常開発では `.codex/skills/forward-port-ready-development`、実際の 1.21.1 移植では `.codex/skills/forward-port-1-21-1` を使う。
3. 実装する。
4. `git diff` / `git diff --name-status` で、依頼範囲外の整形、rename、コメント削除、文字化け差分が混ざっていないことを確認する。
5. コード、リソース、依存、datagen に影響する変更では `./gradlew.bat build` を成功させる。このビルドには明らかな文字化けと UTF-8 BOM の検査も含める。ドキュメントのみの変更では省略してよいが、最終報告に理由を残す。
6. サーバー側の登録、データ読込、レシピ、生成、GameTest 対象構造に影響する変更では `./gradlew.bat runGameTestServer` を成功させる。
7. client 専用 UI、renderer、screen、入力操作に影響する変更では必要に応じて `./gradlew.bat runClient` で確認する。
8. コミットはレビューしやすく、forward-port しやすい粒度に分ける。無関係な整形や広域整理を混ぜない。
9. `main` へ反映する変更は必ずブランチ + PR で流し、直 push しない。
10. PR では GitHub Actions の `PR CI / build-and-gametest`、Codex Cloud のスマートトリガーレビュー、人間のレビューを確認してから取り込む。スマートトリガーレビューは補助であり、CI と人間の判断を置き換えない。
11. 通常は merge commit で取り込む。バージョン更新だけは rebase merge を使ってよい。squash merge は使わない。

## 6. レビューチェックリスト
- Java 17 環境で必要な検証が通っていること。コード/リソース変更では `./gradlew.bat build` を必須とする。
- サーバー側の登録、データ読込、レシピ、生成に影響する変更では `./gradlew.bat runGameTestServer` が成功していること。
- `main` へ送る PR では GitHub Actions の `PR CI / build-and-gametest` が成功していること。
- Codex Cloud のスマートトリガーレビューで指摘が出ている場合、対応または明示的な見送り理由があること。
- 追加・変更した要素の登録漏れ（Registry/EventBus）がないこと。
- サーバー専用環境で問題となるクライアント専用参照を追加していないこと。
- 1 機能が `cherry-pick` しやすい独立したコミット列として保たれていること。
- generated/resource の削除・改名・出力パス変更がある場合、forward-port 先で stale 出力に気づける差分になっていること。
- 既存コンテンツの ID 変更や削除による互換性破壊を避けていること。
- 依存 MOD バージョン条件を変更した場合、`mods.toml` と `gradle.properties` の整合性が取れていること。

## 7. ドキュメント更新
- コード変更時に関連しやすいファイル: `gradle.properties`（バージョン）、`build.gradle`（依存/タスク）、`src/main/resources/META-INF/mods.toml`（依存条件）、`README.md`（仕様/導入手順）、`THIRD_PARTY_NOTICES.md`（ライセンス）、`.codex/skills/**`（エージェント向け手順）。
- 実装変更と同一 PR/コミット内で関連ドキュメントを更新し、差分の理由が追跡できる状態にする。
- 実行手順や開発フローに影響する変更は `AGENTS.md` も同時更新する。
- GitHub Actions / Ruleset / merge / Codex Cloud レビュー運用に変更がある場合は `docs/github-pr-protection.md` も更新する。
- データパック系の利用者向け情報は順次 GitHub wiki へ移行する。README には詳細仕様を戻さない。

## 8. ブランチ間取り込み準備（1.20.1 -> 1.21.1）
- `main`（1.20.1 / Forge）を開発基準ブランチとし、`1.21.1-main` への反映は forward-port（`cherry-pick`）前提で考える。
- このブランチでは 1.21.1 への port 作業を合意なしに開始しない。
- `main` と `1.21.1-main` の直接 `merge` は前提にせず、取り込み対象は個別コミット単位で扱う。
- 1 機能を独立した連続コミット系列として保ち、無関係な整形、rename、広域整理、loader 固有接着コードの書き換えを共通ロジック変更と混ぜない。
- 1.21.1 側で再実装が必要になりそうな箇所は、Java や JSON の差分だけで意図が読めると決めつけず、日本語コメントで理由、制約、移植判断材料を残す。
- 同種コンフリクトの再解決コストを下げるため、`git config rerere.enabled true` を推奨する。

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
