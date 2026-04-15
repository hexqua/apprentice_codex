# AGENTS.md

このファイルは、このリポジトリで作業する人間/AIエージェント向けの共通ルールを定義します。

## 0. 言語ポリシー
- 本プロジェクトでのやり取り、ドキュメント、レビューコメントは原則として日本語を使用する。
- ファイルをコミットする際のコミットメッセージは日本語で記述する。
- 外部資料が英語の場合は、日本語で要点を補足する。
- ツール仕様などで英語が必須の箇所のみ、必要最小限で英語を使用する。

## 1. 目的
- プロジェクトの目的: Apprentice's Codex を開発し、主に Iron's Spells 'n Spellbooks 向けの追加コンテンツを提供する。
- AGENTS.md を置く目的: 人間/AI エージェントの作業手順、品質基準、言語ポリシー（原則日本語）を統一し、再現性のある開発を行う。
- 技術スタックやバージョンなどの実装条件は「2. 開発環境」に記載する。

## 2. 開発環境
- 開発対象: Minecraft 1.21.1
- Mod ローダー: NeoForge 21.1.219
- 言語/実行環境: Java 21
- ビルドツール: Gradle Wrapper（`./gradlew` / `./gradlew.bat`）
- 主要依存 MOD: Iron's Spells 'n Spellbooks（1.21.1-3.15.4）, Curios（9.5.1+1.21.1）, GeckoLib（4.8.3）
- セットアップ手順:
1. 64bit の Java 21 をインストールし、`java -version` で確認する。
2. ユーザー環境変数 `JDK21_HOME` を設定する。`main`（1.20.1）も触る場合は `JDK17_HOME` も設定する。
3. PowerShell では `.\scripts\use-java.ps1` を実行して `JAVA_HOME` を切り替える。`JDKxx_HOME` が未設定でも `%USERPROFILE%\.jdks` と `%USERPROFILE%\.gradle\jdks` は自動検出される。
4. `./gradlew.bat --version` を実行し、Launcher/Daemon JVM が Java 21 であることを確認する。
5. 必要に応じて IDE の Gradle プロジェクト再読み込みを実施する。

## 3. 実行コマンド
- ユーザー環境変数の設定例（ローカル設定、リポジトリへはコミットしない）:
```powershell
setx JDK17_HOME "%USERPROFILE%\.jdks\ms-17.0.16"
setx JDK21_HOME "%USERPROFILE%\.jdks\ms-21.0.10"
```
- PowerShell で Java 21 を一時適用:
```powershell
.\scripts\use-java.ps1
```
- PowerShell で Java 17 を一時適用（`main` / 1.20.1 作業用）:
```powershell
.\scripts\use-java.ps1 -Version 17
```
- ビルド（通常確認）:
```powershell
.\scripts\use-java.ps1
./gradlew.bat build
```
- クリーンビルド（必要時のみ）:
```powershell
.\scripts\use-java.ps1
./gradlew.bat clean build
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
- 想定出力先:
`build\libs\apprentice_codex-<mod_version>+mc<minecraft_version>.jar`
- 起動（開発クライアント）:
```powershell
.\scripts\use-java.ps1
./gradlew.bat runClient
```
- 結合テスト（GameTest サーバー）:
```powershell
.\scripts\use-java.ps1
./gradlew.bat runGameTestServer
```
- 注記: `scripts/use-java.ps1` は Windows のローカル開発用。CI の Java 設定は workflow 側で別管理する。
- 注記: `scripts/use-java.ps1 -StopGradleDaemons` を使うと、切替前 JVM を掴んだ Gradle daemon を止めてから `--version` を確認できる。
- 注記: `runClient` は GUI（Minecraft クライアント）を起動するため、CI やヘッドレス環境では実行しない。
- 注記: `runGameTestServer` はサーバー側の登録・データ読込・レシピ・生成まわりの結合テストに使う。GUI を必要としないため、ヘッドレス環境でも実行しやすい。
- 注記: `runGameTestServer` では renderer / screen など client 専用の起動不良は検知できないため、その確認は別途 `runClient` で行う。
- 注記: 通常のビルド確認では `clean` を付けない。`clean` 実行後は開発実行環境の再生成や IDE 再同期が必要になる場合がある。
- 注記: `runData` の出力先は `src/generated/resources` であり、`src/generated/resources/.cache` に記録された生成物だけが再生成・差分管理される前提で扱う。
- 注記: `src/generated/resources/.cache` は Git 管理外のため、branch 切替・`cherry-pick`・手動コピーで持ち込んだ古い JSON は `runData` だけでは削除されない場合がある。
- 注記: `main` から `1.21.1-main` へ forward-port する場合は、後述の専用 Skill を使って作業手順と確認観点を確認する。
- 注記: 本プロジェクトでは Gradle Wrapper の実行はパス経由を前提にしないため、`./gradlew.bat` を使用する。
- Lint/Format:
`現時点では専用タスク未設定。必要時に追加する。`

## 4. コーディング規約
- 命名規則: クラス/インターフェースは `PascalCase`、メソッド/フィールド/ローカル変数は `camelCase`、定数は `UPPER_SNAKE_CASE` を使用する。
- 命名規則: レジストリ名・リソース ID・JSON ファイル名は `snake_case` を使用し、`apprenticecodex` 名前空間を前提にする。
- 設計方針: 追加要素の登録処理は既存の `registry` パッケージ構成に合わせ、初期化時に一元登録する。
- 設計方針: データ駆動で表現できる内容は `src/generated/resources` と datagen を優先し、ハードコードを最小化する。
- コメント方針: コメントは「何をしているか」より「なぜそうするか（意図・理由・制約）」を優先して記載する。
- コメント方針: 外部 MOD 仕様への依存、ワークアラウンド、クライアント/サーバー差分、実行順依存、魔法値を扱う箇所はコメント必須とする。
- コメント方針: 複雑な条件分岐、将来の拡張を前提にした設計判断、誤用しやすい API 利用箇所にはコメント推奨とする。
- コメント方針: 自明な処理の逐語説明コメントは避ける。
- コメント方針: 実装変更時はコメントも同時に更新し、不要になったコメントは削除する。
- コメント方針: コメント本文は原則日本語で、短く具体的に記述する。
- 文字コード方針: テキストファイルは UTF-8（BOM なし）を原則とする。UTF-8 BOM はビルド失敗の要因になるため使用しない。
- 依存関係追加の方針: 追加・更新するバージョンは `gradle.properties` に集約し、`build.gradle` から参照する。
- 依存関係追加の方針: 必須依存を追加する場合は `src/main/resources/META-INF/neoforge.mods.toml` の dependency 定義も更新する。
- 依存関係追加の方針: 外部アセット/ライブラリ利用時は `THIRD_PARTY_NOTICES.md` の追記要否を必ず確認する。

## 5. 変更フロー
1. 変更内容を 1〜2 文で決める（何を、なぜ変えるか）。
2. 実装する。
3. `./gradlew.bat build` が成功することを確認する（ここだけ必須）。
4. サーバー側の登録・データ読込・レシピ・生成・GameTest 対象構造に影響する変更では、`./gradlew.bat runGameTestServer` が成功することを確認する。
5. `main` から `1.21.1-main` への forward-port では、実装内容に関係なく `./gradlew.bat runGameTestServer` が成功することを確認する。
6. 必要に応じて `./gradlew.bat runClient` で動作確認する。
7. `1.21.1-main` へ取り込む変更は PR で流し、`PR CI / build-and-gametest` の成功を確認してから merge する。
8. 必要に応じて関連ドキュメントを更新する。

## 6. レビューチェックリスト
- 必須チェック項目: Java 21 環境で `./gradlew.bat build` が成功すること。
- 必須チェック項目: サーバー側の登録・データ読込・レシピ・生成に影響する変更では、`./gradlew.bat runGameTestServer` が成功すること。
- 必須チェック項目: `main` から `1.21.1-main` への forward-port では、実装内容に関係なく `./gradlew.bat runGameTestServer` が成功すること。
- 必須チェック項目: `1.21.1-main` 向け PR では `PR CI / build-and-gametest` が成功していること。
- 必須チェック項目: 追加・変更した要素の登録漏れ（Registry/EventBus）がないこと。
- 必須チェック項目: サーバー専用環境で問題となるクライアント専用参照を追加していないこと。
- リグレッション確認: 既存コンテンツの ID 変更や削除による互換性破壊を避ける。
- リグレッション確認: 依存 MOD バージョン条件を変更した場合、`neoforge.mods.toml` と `gradle.properties` の整合性を確認する。

## 7. ドキュメント更新
- コード変更時に更新すべきファイル: `gradle.properties`（バージョン）、`build.gradle`（依存/タスク）、`src/main/resources/META-INF/neoforge.mods.toml`（依存条件）、`README.md`（仕様/導入手順）、`THIRD_PARTY_NOTICES.md`（ライセンス）、`.codex/skills/**`（エージェント向け手順）。
- 更新ルール: 実装変更と同一 PR/コミット内で関連ドキュメントを更新し、差分の理由が追跡できる状態にする。
- 更新ルール: 実行手順や開発フローに影響する変更は `AGENTS.md` も同時更新する。

## 8. ブランチ間取り込み（1.20.1 -> 1.21.1）
- 基本方針: `main`（1.20.1）を開発基準ブランチとし、`1.21.1-main` への反映は forward-port（`cherry-pick`）で行う。
- 基本方針: `main` と `1.21.1-main` の直接 `merge` は原則禁止とし、必要な場合は事前合意を必須とする。
- 基本方針: `merge` コミットの直接 `cherry-pick`（`git cherry-pick -m` を含む）は禁止とし、取り込み対象は個別コミット単位で扱う。
- 実作業では `.codex/skills/forward-port-1-21-1` を使用する。
- Skill を使う理由: generated JSON の削除漏れ、1.21.1 の enchant/repair/tag 差分、旧版書式の残留確認は通常作業では不要であり、常設ルールと分離した方が見落としにくいため。
- AGENTS.md では次の原則だけを常設ルールとして保持する。
1. 取り込み前に対象コミットを個別 SHA で確定し、`git cherry-pick -x` を使う。
2. 削除・改名・出力パス変更・旧書式移行を含む datagen 作業では、`runData` 前に影響ディレクトリの generated JSON を明示削除する。
3. 移植後は実装内容に関係なく `./gradlew.bat runGameTestServer` と `./gradlew.bat build` を成功させ、generated 差分と旧書式の残留を確認する。
4. 1.21.1 の enchant 適用可否や修理可否は Java の override だけで完了と判断せず、enchantment JSON と item tag も確認する。
- 運用ルール:
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

## 9. Codex運用上の注意（コメント保全/文字化け対策）
- 原因整理: Windows PowerShell 5.1（コードページ 932）で `Get-Content` 既定読み取りを使うと、UTF-8日本語が文字化けして表示される。
- 対策: 日本語を含むファイルをターミナルで読む前に、`[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)` を設定し、`Get-Content -Encoding UTF8` を使用する。
- 対策: PowerShell 5.1 で `Set-Content` / `Out-File` の既定エンコーディング書き込みは使わない（BOM付与や文字化け混入の原因になる）。
- 対策: シェル経由で保存が必要な場合は UTF-8 BOM なしを明示する（例: `[System.IO.File]::WriteAllText($path, $text, [System.Text.UTF8Encoding]::new($false))`）。
- 対策: Python スクリプトが UTF-8 の skill / Markdown を読む場合、Windows 既定の `cp932` で `UnicodeDecodeError` になることがある。`quick_validate.py` などを実行するときは `PYTHONUTF8=1` を付けて UTF-8 モードで実行する。
- 例:
```powershell
$env:PYTHONUTF8='1'
python C:\Users\hexqu\.codex\skills\.system\skill-creator\scripts\quick_validate.py .codex/skills/forward-port-1-21-1
```
- 対策: 編集は必要最小限の差分に限定し、ファイル全体の再書き込みや無関係なコメント整理を行わない。
- 対策: 文字化けした表示（例: `縺` など）が出た状態では編集を続行しない。UTF-8指定で再読込して正常表示を確認してから編集する。
- 対策: 変更後は `git diff` を確認し、依頼範囲外コメントの削除と日本語の文字化け差分があれば修正してから完了とする。
- 対策: 必要に応じて `git diff | rg "^-\\s*(//|/\\*|\\*|#)"` でコメント削除行を検出し、依頼範囲内の変更かを確認する。

## 10. 禁止事項
- 事前合意なしで大規模リファクタをしない。
- 機密情報をコミットしない。
