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
- 開発対象: Minecraft 1.20.1
- Mod ローダー: Minecraft Forge 47.4.10
- 言語/実行環境: Java 17
- ビルドツール: Gradle Wrapper（`./gradlew` / `./gradlew.bat`）
- 主要依存 MOD: Iron's Spells 'n Spellbooks（1.20.1-3.15.4）, Curios（5.14.1+1.20.1）, GeckoLib（4.8.3）
- セットアップ手順:
1. 64bit の Java 17 をインストールし、`java -version` で確認する。
2. 既定の Java が 17 以外の場合は、ビルド実行前に一時的に `JAVA_HOME` を切り替える。
3. `./gradlew.bat --version` を実行し、JVM が Java 17 であることを確認する。
4. 必要に応じて IDE の Gradle プロジェクト再読み込みを実施する。

## 3. 実行コマンド
- PowerShell で Java 17 を一時適用（必要な場合）:
```powershell
# 必須: <<REPLACE_WITH_YOUR_JDK17_PATH>> を実際の JDK 17 パスに置換する
$env:JAVA_HOME='<<REPLACE_WITH_YOUR_JDK17_PATH>>'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
java -version
```
- ビルド（通常確認）:
```powershell
./gradlew.bat build
```
- クリーンビルド（必要時のみ）:
```powershell
./gradlew.bat clean build
```
- jar 出力確認:
```powershell
Get-ChildItem build\libs\*.jar
```
- 想定出力先:
`build\libs\apprentice_codex-<mod_version>+mc1.20.1.jar`
- 起動（開発クライアント）:
```powershell
./gradlew.bat runClient
```
- 結合テスト（GameTest サーバー）:
```powershell
./gradlew.bat runGameTestServer
```
- 注記: `runClient` は GUI（Minecraft クライアント）を起動するため、CI やヘッドレス環境では実行しない。
- 注記: `runGameTestServer` はサーバー側の登録・データ読込・レシピ・生成まわりの結合テストに使う。GUI を必要としないため、ヘッドレス環境でも実行しやすい。
- 注記: `runGameTestServer` では renderer / screen など client 専用の起動不良は検知できないため、その確認は別途 `runClient` で行う。
- 注記: 通常のビルド確認では `clean` を付けない。`clean` 実行後は開発実行環境の再生成（例: `genIntellijRuns`）が必要になる場合がある。
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
- 依存関係追加の方針: 必須依存を追加する場合は `src/main/resources/META-INF/mods.toml` の dependency 定義も更新する。
- 依存関係追加の方針: 外部アセット/ライブラリ利用時は `THIRD_PARTY_NOTICES.md` の追記要否を必ず確認する。

## 5. 変更フロー
1. 変更内容を 1〜2 文で決める（何を、なぜ変えるか）。
2. 実装前に、変更を「共通ロジック」「1.20.1 固有の接着コード」「generated/resource 更新」に分けて考え、forward-port 時に分離が必要な差分を整理する。
3. 実装する。
4. 差分確認を行い、依頼範囲外のコメント削除/改変と文字化け差分がないこと、無関係な整形・rename・広域整理が混在していないことを確認する。
5. `./gradlew.bat build` が成功することを確認する（ここだけ必須）。
6. サーバー側の登録・データ読込・レシピ・生成・GameTest 対象構造に影響する変更では、`./gradlew.bat runGameTestServer` が成功することを確認する。
7. 必要に応じて `./gradlew.bat runClient` で動作確認する。
8. 必要に応じて関連ドキュメントを更新する。
9. `main` へ反映する変更は、バージョン更新を含めて必ずブランチ + PR で流し、直 push しない。
10. `main` の PR は GitHub Actions の `PR CI / build-and-gametest` 通過を必須とし、通常は merge commit で取り込む。バージョン更新だけは rebase merge を使ってよい。

## 6. レビューチェックリスト
- 必須チェック項目: Java 17 環境で `./gradlew.bat build` が成功すること。
- 必須チェック項目: サーバー側の登録・データ読込・レシピ・生成に影響する変更では、`./gradlew.bat runGameTestServer` が成功すること。
- 必須チェック項目: `main` へ送る PR では GitHub Actions の `PR CI / build-and-gametest` が成功していること。
- 必須チェック項目: 追加・変更した要素の登録漏れ（Registry/EventBus）がないこと。
- 必須チェック項目: サーバー専用環境で問題となるクライアント専用参照を追加していないこと。
- 必須チェック項目: 1 機能が `cherry-pick` しやすい独立したコミット列として保たれ、無関係な整形・rename・広域整理が同一コミットに混ざっていないこと。
- 必須チェック項目: generated/resource の削除・改名・出力パス変更がある場合、forward-port 先で stale 出力に気づける差分になっていること。
- 必須チェック項目: 1.21.1 側で接着コードを書き直す可能性がある箇所に、意図・制約・移植判断に必要な日本語コメントが残っていること。
- リグレッション確認: 既存コンテンツの ID 変更や削除による互換性破壊を避ける。
- リグレッション確認: 依存 MOD バージョン条件を変更した場合、`mods.toml` と `gradle.properties` の整合性を確認する。

## 7. ドキュメント更新
- コード変更時に更新すべきファイル: `gradle.properties`（バージョン）、`build.gradle`（依存/タスク）、`src/main/resources/META-INF/mods.toml`（依存条件）、`README.md`（仕様/導入手順）、`THIRD_PARTY_NOTICES.md`（ライセンス）、`.codex/skills/**`（エージェント向け手順）。
- 更新ルール: 実装変更と同一 PR/コミット内で関連ドキュメントを更新し、差分の理由が追跡できる状態にする。
- 更新ルール: 実行手順や開発フローに影響する変更は `AGENTS.md` も同時更新する。
- 更新ルール: GitHub Actions / Ruleset / merge 運用に変更がある場合は `docs/github-pr-protection.md` も更新する。

## 8. ブランチ間取り込み準備（1.20.1 -> 1.21.1）
- 基本方針: `main`（1.20.1 / Forge）を開発基準ブランチとし、`1.21.1-main` への反映は forward-port（`cherry-pick`）前提で考える。
- 基本方針: このブランチでは 1.21.1 への port 作業を行わない。1.21.1 向けの調査、実装、差分確認、`cherry-pick` などの関連作業を開始する場合は、着手前に必ず人間/AI 間で明示的に確認を取り、合意がない限り作業してはならない。
- 基本方針: `main` と `1.21.1-main` の直接 `merge` は前提にせず、取り込み対象は個別コミット単位で扱う。
- 実作業の詳細手順は `.codex/skills/forward-port-1-21-1` を使用する。
- AGENTS.md では次の原則だけを常設ルールとして保持する。
1. 1 機能を独立した連続コミット系列として保ち、`git cherry-pick -x` しやすい差分構成を維持する。
2. 無関係な整形・rename・広域整理・loader 固有接着コードの書き換えを、共通ロジック変更と同一コミットに混ぜない。
3. generated/resource の削除・改名・出力パス変更を含む作業では、forward-port 先で stale 出力の確認が必要になる前提で、削除差分と移行意図を追える状態にする。
4. 1.21.1 側で再実装が必要になりそうな箇所は、Java や JSON の差分だけで意図が読めると決めつけず、日本語コメントで理由・制約・移植判断材料を残す。
- 運用ルール:
- 実装時に forward-port そのものを始めなくても、差分設計とコミット設計は `1.21.1-main` への取り込みやすさを意識して行う。
- 通常開発での自己確認やレビューでは `.codex/skills/forward-port-ready-development` を使い、port 手順そのものと混同しない。
- 1.21.1 側のみで必要になった修正をこのブランチへ持ち込むかは別途判断し、必要時のみ個別対応する。
- 同種コンフリクトの再解決コストを下げるため、`git config rerere.enabled true` を推奨する。

## 9. Codex運用上の注意（コメント保全/文字化け対策）
- 原因整理: Windows PowerShell 5.1（コードページ 932）で `Get-Content` 既定読み取りを使うと、UTF-8日本語が文字化けして表示される。
- 対策: 日本語を含むファイルをターミナルで読む前に、`[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)` を設定し、`Get-Content -Encoding UTF8` を使用する。
- 対策: PowerShell 5.1 で `Set-Content` / `Out-File` の既定エンコーディング書き込みは使わない（BOM付与や文字化け混入の原因になる）。
- 対策: シェル経由で保存が必要な場合は UTF-8 BOM なしを明示する（例: `[System.IO.File]::WriteAllText($path, $text, [System.Text.UTF8Encoding]::new($false))`）。
- 対策: 編集は必要最小限の差分に限定し、ファイル全体の再書き込みや無関係なコメント整理を行わない。
- 対策: 文字化けした表示（例: `縺` など）が出た状態では編集を続行しない。UTF-8指定で再読込して正常表示を確認してから編集する。
- 対策: 変更後は `git diff` を確認し、依頼範囲外コメントの削除と日本語の文字化け差分があれば修正してから完了とする。
- 対策: 必要に応じて `git diff | rg "^-\\s*(//|/\\*|\\*|#)"` でコメント削除行を検出し、依頼範囲内の変更かを確認する。

## 10. 禁止事項
- 事前合意なしで大規模リファクタをしない。
- 機密情報をコミットしない。
