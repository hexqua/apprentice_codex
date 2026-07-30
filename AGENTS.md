# AGENTS.md

このファイルは、このリポジトリで作業する人間/AIエージェント向けの共通ルールを定義する。

## 0. 言語ポリシー
- 本プロジェクトでのやり取り、ドキュメント、レビューコメントは原則として日本語を使用する。
- プレイヤー向けテキストは、`lang` による言語選択ができる場合は `lang` を使用し、言語選択ができない部分は英語を必須とする。
- `src` 内の `config` 配下にある `comment` 引数と、プレイヤー向けの GitHub Issue フォームは言語選択ができないため、英語を必須とする。
- GitHub PR は開発者向けのため原則として日本語を使用する。ただし、他の開発者が英語で作成した PR は許容し、日本語への統一を要求しない。
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
- 主要依存 MOD: Iron's Spells 'n Spellbooks（1.21.1-3.16.2）, Iron's Lib（1.21.1-2.1.0）, Curios（9.5.1+1.21.1）, GeckoLib（4.8.3）
- ローカルでは `JDK21_HOME` を設定し、必要に応じて `.\scripts\use-java.ps1` で `JAVA_HOME` を切り替える。
- `1.20.1-main`（1.20.1）を触る場合のみ Java 17 を使う。

## 3. 実行コマンド
- Java 21 を一時適用:
```powershell
.\scripts\use-java.ps1
```
- Java 17 を一時適用（`1.20.1-main` / 1.20.1 作業用）:
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
- optional MOD 連携 GameTest:
```powershell
.\scripts\use-java.ps1
./gradlew.bat runGameTestServerCompat
./gradlew.bat runGameTestServerEasyMagic
./gradlew.bat runGameTestServerBetterCombat
./gradlew.bat runGameTestServerEpicFight
```
- 開発クライアント:
```powershell
.\scripts\use-java.ps1
./gradlew.bat runClient
```
- optional MOD 付き開発クライアント:
```powershell
.\scripts\use-java.ps1
./gradlew.bat runClientCompat
./gradlew.bat runClientEasyMagic
./gradlew.bat runClientBetterCombat
./gradlew.bat runClientEpicFight
./gradlew.bat runClientEpicFightController
./gradlew.bat runClientCompatEasyBetter
```
- 一時的な optional MOD 追加:
```powershell
./gradlew.bat runClient "-PdevRuntimeMods=create,malum"
```
- IntelliJ IDEA 実行構成の同期:
```powershell
.\scripts\use-java.ps1
./gradlew.bat neoForgeIdeSync
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
- `runGameTestServer` は専用 world `run/codex_gametest_clean` を毎回初期化してから起動する。通常の手動確認用 `run/world` は削除しない。
- `runGameTestServerCompat` は Farmer's Delight / Create / Lodestone / Malum / Atlas API / Iron's Gems 'n Jewelry 連携の確認に使う。
- `runGameTestServerEasyMagic` は Puzzles Lib / Easy Magic 連携の確認に使う。
- `runGameTestServerBetterCombat` は Cloth Config / Better Combat 連携の確認に使う。
- `runGameTestServerEpicFight` は Epic Fight 連携の確認に使う。
- いずれかの GameTest task が一度でも失敗した場合は、後続実行が成功しても失敗を省略せず、`.codex/skills/report-gametest-failure` を使用して観測結果を最終報告に残す。
- `runClientEpicFightController` は Epic Fight / Controlify / YACL を入れ、実機コントローラー入力を確認する。
- `runClientCompatEasyBetter` は compat + Easy Magic + Better Combat を入れた実環境寄りの手動バランス確認用。Epic Fight は含めず、自動テスト対象にも含めない。
- IntelliJ IDEA から client 構成を起動して `Unsupported major.minor version 65.0` が出る場合は、Project SDK / Gradle JVM / 古い実行構成が Java 17 を参照している。IDEA 側を JDK 21 にそろえ、`neoForgeIdeSync` を再実行し、必要なら古い Minecraft run configuration を削除して再生成する。
- Botania は 1.21.1 側で API 依存を置いていないため、optional MOD profile には含めない。
- Better Combat と Epic Fight は干渉が大きいため、通常確認では同時投入しない。
- optional MOD の runtime 切替は Gradle の実行構成または `-PdevRuntimeMods=...` で行う。`build.gradle` の `runtimeOnly` / `localRuntime` コメントアウト解除運用は使わない。
- 通常確認で `clean` は付けない。必要時のみ `./gradlew.bat clean build` を使う。
- `runData` の出力先は `src/generated/resources` であり、`src/generated/resources/.cache` に記録された生成物だけが再生成・差分管理される前提で扱う。
- `src/generated/resources/.cache` は Git 管理外のため、branch 切替・`cherry-pick`・手動コピーで持ち込んだ古い JSON は `runData` だけでは削除されない場合がある。

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
2. 実装前に、変更を「共通ロジック」「1.21.1 / NeoForge 固有の接着コード」「generated/resource 更新」「backport 対象外」に分けて考える。`main` の共通機能・共通不具合修正、registry、datagen/resource、依存変更、refactor を計画・実装・レビューする場合は `.codex/skills/backport-ready-development` を使う。
3. 実装する。
4. `git diff` / `git diff --name-status` で、依頼範囲外の整形、rename、コメント削除、文字化け差分が混ざっていないことを確認する。
5. コード、リソース、依存、datagen に影響する変更では `./gradlew.bat build` を成功させる。このビルドには明らかな文字化けと UTF-8 BOM の検査も含める。ドキュメントのみの変更では省略してよいが、最終報告に理由を残す。
6. サーバー側の登録、データ読込、レシピ、生成、GameTest 対象構造に影響する変更では `./gradlew.bat runGameTestServer` を成功させる。
7. `main` から `1.20.1-main` への backport では、1.20.1 側で `./gradlew.bat runGameTestServer` と `./gradlew.bat build` を成功させる。
8. optional MOD 連携に影響する変更では、対象に応じて特殊 GameTest / client 構成を追加実行する。
   - Create / Lodestone / Malum / Farmer's Delight / Atlas API / Iron's Gems 'n Jewelry: `./gradlew.bat runGameTestServerCompat`
   - Easy Magic / エンチャントメニュー: `./gradlew.bat runGameTestServerEasyMagic`
   - Better Combat / offhand / weapon_attributes: `./gradlew.bat runGameTestServerBetterCombat`
   - Epic Fight / mixin / capabilities / item_skins: `./gradlew.bat runGameTestServerEpicFight`
   - client 側の連携確認: 対応する `runClient...` 構成
   - 組み合わせバランス確認: `./gradlew.bat runClientCompatEasyBetter`
9. client 専用 UI、renderer、screen、入力操作に影響する変更では必要に応じて `./gradlew.bat runClient` で確認する。
10. コミット前に `.codex/skills/review-local-change` を使い、未コミット差分を変更せずにレビューする。
11. コミットはレビューしやすく、backport 対象を選びやすい粒度に分ける。共通ロジックと NeoForge 固有接着コードを可能な範囲で別コミットにし、無関係な整形や広域整理を混ぜない。
12. 機能としてコミット列が完成したら `.codex/skills/review-feature-branch` を使い、対象 base branch との差分全体をレビューしてから PR を作成する。
13. `main` へ取り込む変更は必ずブランチ + PR で流し、直 push しない。
14. PR では必須の GitHub Actions `PR CI / build` と `PR CI / gametest`、Codex Cloud のスマートトリガーレビュー、人間のレビューを確認する。スマートトリガーレビューは補助であり、CI と人間の判断を置き換えない。
15. 外部レビュー結果がそろった後に `.codex/skills/review-feature-branch` を再実行し、readiness が `Ready` であることを確認する。
16. 通常は merge commit で取り込む。バージョン更新だけは rebase merge を使ってよい。squash merge は使わない。

## 6. Code Review Rules

### Skill の使い分け
- コミット前の staged / unstaged / untracked 差分または指定コミットは `.codex/skills/review-local-change` でレビューする。
- base branch に対する機能ブランチ全体、PR、CI、Codex Cloud、人間レビューを含む readiness は `.codex/skills/review-feature-branch` で確認する。
- どちらの Skill もデフォルトでは findings の報告だけを行い、明示指示なしに修正、stage、commit、push、PR コメント、merge を行わない。

### 常設レビュー規則
- 追加・変更した要素は既存の registry 構成と EventBus 初期化へ登録し、未登録のままにしない。
- client 専用参照は client 初期化または適切な実行環境境界へ隔離し、専用サーバーから読み込まれる共通コードへ追加しない。
- client 由来の入力は不正値を想定するが、client の視線・選択結果を server 側で完全再現することは一律に要求しない。射程、権限・所有権、対象の有効性、置換・破壊可否、コスト、影響範囲など、server が守るべき制約の突破と具体的な実害で判断する。
- UX のため意図的に client / server 間の差を許容する変更では、許容する挙動と拒否すべき境界の両方を GameTest で明示する。重要な server 側制約が守られた既存仕様として確認できる場合は厳格化を修正方針にせず、テスト不足を指摘する。判断できない挙動をテスト追加だけで仕様化しない。
- 既存の content ID、設定キー、保存データを維持する。変更が不可避な場合は互換経路または migration 方針を同じ変更で示す。
- generated/resource の削除・改名・出力パス変更では、旧出力の削除差分と target 側の再生成方針を追跡可能にし、stale 出力を残さない。
- 依存 MOD のバージョン条件は `gradle.properties`、`build.gradle`、`neoforge.mods.toml` で整合させる。
- Backport 候補では `.codex/skills/backport-ready-development` に従い、移植対象、1.20.1 側の補正、対象外要素を説明できるコミット列にする。

## 7. ドキュメント更新
- コード変更時に関連しやすいファイル: `gradle.properties`（バージョン）、`build.gradle`（依存/タスク）、`src/main/resources/META-INF/neoforge.mods.toml`（依存条件）、`README.md`（仕様/導入手順）、`THIRD_PARTY_NOTICES.md`（ライセンス）、`.codex/skills/**`（エージェント向け手順）。
- 実装変更と同一 PR/コミット内で関連ドキュメントを更新し、差分の理由が追跡できる状態にする。
- 実行手順や開発フローに影響する変更は `AGENTS.md` も同時更新する。
- GitHub Actions / Ruleset / merge / Codex Cloud レビュー運用に変更がある場合は `docs/github-pr-protection.md` も更新する。
- データパック系の利用者向け情報は順次 GitHub wiki へ移行する。README には詳細仕様を戻さない。

## 8. ブランチ間取り込み（1.21.1 -> 1.20.1）
- `main`（1.21.1 / NeoForge）を開発基準ブランチとし、新機能と共通不具合修正は原則として `main` へ先に実装する。
- `1.20.1-main`（1.20.1 / Forge）は保守・backport先として扱い、利用価値、実装差、検証コストを確認して取り込み対象を選ぶ。
- 両ブランチが Iron's Spells 'n Spellbooks 3.x を使用している間は、Minecraft / loader / 外部 MOD 固有差分を除き、可能な範囲で機能をそろえる。
- Iron's Spells 'n Spellbooks 4.x の安定版へ移行する前に、3.x 系最終対応時点で両ブランチの共通機能をそろえる。
- 4.x の Casting API 移行中は、`main` の新機能追加を原則停止し、移行作業と致命的不具合修正を優先する。
- 4.x 移行後の `1.20.1-main` は 3.x 系 LTS とし、完全な機能同一性は保証しない。既存ノウハウで低コストに実装できる要素は個別に採用を判断する。
- 将来 Minecraft 26.1.x 系へ対応する場合は、NeoForge の `main` を移植元とし、1.20.1 / Forge を経由するバケツリレーは行わない。
- `main` で backport 候補を計画・実装・レビューする場合は `.codex/skills/backport-ready-development` を使用する。
- `main` の非 `merge` コミットを `1.20.1-main` へ実際に取り込む場合は `.codex/skills/backport-1-20-1` を使用する。
- `main` と `1.20.1-main` の直接 `merge` は原則禁止とし、取り込み対象は個別コミット単位で扱う。
- `1.20.1-main` だけで必要になった修正は、`main` にも必要かを別途判断し、自動的な逆取り込みは行わない。

## 8.1 `main` の PR CI 運用
- `main` への反映は PR 経由のみとし、直接 push しない。
- required check は `PR CI / build` と `PR CI / gametest` とする。
- workflow は `pull_request` でのみ動かし、`pull_request_target` は使わない。
- CI では repository secrets を使わず、`GITHUB_TOKEN` は read-only に固定する。
- 通常変更は `merge commit` を使い、バージョン更新 PR だけ `rebase merge` を許可する。
- `squash merge` は使わない。
- bootstrap 詰まりを避けるため、workflow を target branch に載せる前に required check を有効化しない。手順は `docs/github-pr-protection.md` に従う。

## 9. Codex運用上の注意
- 日本語・中国語、翻訳、Markdown、resource、AGENTS.md、Skill を扱う場合や文字化けが疑われる場合は、英語で書かれた `.codex/skills/text-encoding-hygiene` を先に確認し、UTF-8（BOM なし）として扱う。
- 文字化け表示が見えた場合は編集を中断し、Skill の手順で正常表示を確認してから作業する。
- 編集は必要最小限の差分に限定し、変更後は `git diff` で依頼範囲外の削除や文字化けがないことを確認する。
- ユーザーが事前確認やローカルレビューを明示した作業では、明示指示があるまで push、PR 作成、リモート操作を行わない。

## 10. 直近の注意傾向
- 直近では Epic Fight 連携、簡体字中国語翻訳、説明文・字幕・GameTest 拡充が続いている。関連差分では、任意依存あり/なしの起動、翻訳キーの整合、サーバー側 GameTest の追加要否を優先して確認する。

## 11. 禁止事項
- 事前合意なしで大規模リファクタをしない。
- 機密情報をコミットしない。
