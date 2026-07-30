# Apprentice's Codex

## 概要

Iron's Spells 'n Spellbooks用の小さなアドオンMODです.

## 開発環境

- 開発ブランチ `main` では Java 21 を使用します。
- `main` は Minecraft 1.21.1 / NeoForge の主要開発ブランチです。
- Minecraft 1.20.1 / Forge は `1.20.1-main` で保守し、`main` の変更から対象を選んで backport します。
- `build.gradle` では Java toolchain を 21 に固定していますが、`gradlew.bat` 自体が使う JVM は `JAVA_HOME` または `PATH` に依存します。
- ローカル開発では PowerShell 用の [`scripts/use-java.ps1`](scripts/use-java.ps1) を使う前提にします。

### ブランチとサポート方針

- 新機能と共通不具合修正は、原則として `main` へ先に実装します。
- `1.20.1-main` は保守ブランチとして扱い、利用価値と移植コストを確認したうえで選択的に backport します。
- 両ブランチが Iron's Spells 'n Spellbooks 3.x を使用している間は、Minecraft / loader / 外部 MOD 固有差分を除き、可能な範囲で機能をそろえます。
- 1.21.1 側が Iron's Spells 'n Spellbooks 4.x へ移行した後は、1.20.1 側を 3.x 系の LTS として扱い、完全な機能同一性は保証しません。
- 詳細な開発・backport手順は [`AGENTS.md`](AGENTS.md) を参照してください。

## Java 切替

- 推奨: ユーザー環境変数 `JDK17_HOME` / `JDK21_HOME` を設定します。
- `JDKxx_HOME` が未設定でも、スクリプトは `%USERPROFILE%\.jdks` と `%USERPROFILE%\.gradle\jdks` を自動検出します。
- スクリプトは Windows のローカル開発用です。CI の Java 設定は workflow 側で別管理します。

- 例: ユーザー環境変数を設定する

```powershell
setx JDK17_HOME "%USERPROFILE%\.jdks\ms-17.0.16"
setx JDK21_HOME "%USERPROFILE%\.jdks\ms-21.0.10"
```

- `setx` 実行後は PowerShell を開き直してから使います。
- `main` で作業する場合:

```powershell
.\scripts\use-java.ps1
```

- `1.20.1-main`（1.20.1）で作業する場合:

```powershell
.\scripts\use-java.ps1 -Version 17
```

- 実行ポリシーでブロックされる環境では、次の形でも実行できます。

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\use-java.ps1
```

## 導入方法

- `mods`配下に`jar`を入れればOKです。
- 現時点でIron's Spells 'n Spellbooks及びそれの前提MOD以外の前提MODはありません.

## 開発時テスト

- 通常のビルド確認:

```powershell
.\scripts\use-java.ps1
./gradlew.bat build
```

- サーバー側の結合テスト:

```powershell
.\scripts\use-java.ps1
./gradlew.bat runGameTestServer
```

- `runGameTestServer` は専用 world `run/codex_gametest_clean` を毎回初期化してから起動します。通常の手動確認用 `run/world` は削除しません。
- optional MOD 連携を含む GameTest は、通常 CI には入れず必要時に個別実行します。

```powershell
./gradlew.bat runGameTestServerCompat
./gradlew.bat runGameTestServerEasyMagic
./gradlew.bat runGameTestServerBetterCombat
./gradlew.bat runGameTestServerEpicFight
```

- `runGameTestServerCompat` は Farmer's Delight / Create / Lodestone / Malum / Atlas API / Iron's Gems 'n Jewelry 連携を確認します。
- `runGameTestServerEasyMagic` は Puzzles Lib / Easy Magic 連携を確認します。
- `runGameTestServerBetterCombat` は Cloth Config / Better Combat 連携を確認します。
- `runGameTestServerEpicFight` は Epic Fight 連携を確認します。
- これらの特殊 GameTest では、対象 optional MOD が読み込まれていない場合に失敗します。
- `runGameTestServer` では現在、次の項目を確認します。
- Registry と動的登録の確認:
  item / block / block entity / entity / mob effect / enchantment / attribute / potion / recipe serializer / recipe type / creative tab / apprenticecodex の spell / School Affinity の動的 effect・potion・catalyst
- レシピ読込の確認:
  custom serializer を使う recipe と custom recipe type の recipe が `RecipeManager` に載ること
- サーバー側生成の確認:
  代表的な custom block / block entity が実際に生成でき、custom entity type がサーバー world 上で生成可能なこと
- クリエイティブタブ順の確認:
  apprenticecodex の scroll が School 順で並び、school をまたいでごちゃ混ぜにならないこと
- 注意:
  これらはサーバー側の起動・読込・登録ミスの検知が主目的です。renderer や screen など client 専用の起動不良は別途 `runClient` で確認が必要です。

### optional MOD 付き client 起動

- IntelliJ IDEA で実行構成を同期する場合:

```powershell
.\scripts\use-java.ps1
./gradlew.bat neoForgeIdeSync
```

- IDEA から起動して `Unsupported major.minor version 65.0` が出る場合は、IDEA が Java 17 の Project SDK / Gradle JVM / 古い実行構成を掴んでいます。Project SDK と Gradle JVM を JDK 21 にしてから `neoForgeIdeSync` を再実行し、必要なら古い Minecraft run configuration を削除して再生成してください。
- Gradle 同期後、通常の `runClient` に加えて次の client 構成を使えます。
  - `runClientCompat`
  - `runClientEasyMagic`
  - `runClientBetterCombat`
  - `runClientEpicFight`
  - `runClientEpicFightController`
  - `runClientCompatEasyBetter`
- `runClientEpicFightController` は Epic Fight / Controlify / YACL を入れ、実機コントローラー入力を確認する構成です。
- `runClientCompat` は Farmer's Delight / Create / Lodestone / Malum / Atlas API / Iron's Gems 'n Jewelry を入れた連携確認用の構成です。
- `runClientCompatEasyBetter` は compat + Easy Magic + Better Combat を入れた実環境寄りの手動バランス確認用です。Epic Fight は含めず、自動テスト対象にもしていません。
- 一時的な組み合わせ確認では Gradle プロパティでも追加できます。

```powershell
./gradlew.bat runClient "-PdevRuntimeMods=create,malum"
./gradlew.bat runClient "-PdevRuntimeMods=epic_fight"
```

- `devRuntimeMods` には `compat`, `easy_magic`, `better_combat`, `epic_fight`, `epic_fight_controller`, `compat_easy_better` または個別名（`create`, `lodestone`, `malum`, `atlas_api`, `irons_jewelry`, `controlify` など）をカンマ区切りで指定できます。`irons_jewelry` は Atlas API も同時に追加します。
- Botania は 1.21.1 側で API 依存を置いていないため、optional MOD profile には含めていません。
- Better Combat と Epic Fight は干渉が大きいため、同時投入は通常確認では避けます。

## PR 運用

- `main` への取り込みは PR 経由で行い、`PR CI / build` と `PR CI / gametest` の成功を必須とします。
- `1.20.1-main` への backport も PR 経由で行い、対象コミットと1.20.1固有の補正内容を説明します。
- optional MOD 付きの特殊 GameTest / client 起動は required CI に含めません。必要な変更ではローカルまたは Codex 実行結果を PR コメントや最終報告に残します。
- Codex Cloud のスマートトリガーレビューをレビュー補助として使います。人間の判断と CI 通過を置き換えるものではありません。
- GitHub Actions は `pull_request` でのみ実行し、`pull_request_target` は使いません。
- CI では secrets を使いません。workflow 権限は read-only に固定します。
- 通常の PR は `Create a merge commit` を使います。
- バージョン更新 PR だけは `Rebase and merge` を使ってかまいません。
- `Squash and merge` は使いません。
- GitHub 側の設定手順と bootstrap 順序は `docs/github-pr-protection.md` を参照してください。

## ライセンスや使用について

### 許可(Permissions)

- modpackにはご自由にどうぞ
- 前提modにするのもご自由にどうぞ
- リソースパックもご自由にどうぞ
- スクリーンショット、プレイ動画、動画配信も収益化含めご自由にどうぞ

### 禁止(Restrictions)

- `THIRD_PARTY_NOTICES.md`にある素材のライセンスを破るのはやめてください

### License

- Code: MIT (see `LICENSE`).
- Original assets created for this project: CC0-1.0 (free to use).
- Third-party assets: see `THIRD_PARTY_NOTICES.md` for the applicable licenses.
