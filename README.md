# Apprentice's Codex

## 概要

Iron's Spells 'n Spellbooks用の小さなアドオンMODです.

## 開発環境

- このブランチ（`1.20.1-main` / 1.20.1）では Java 17 を使用します。
- `main` では Java 21 を使用します。
- `build.gradle` では Java toolchain を 17 に固定していますが、`gradlew.bat` 自体が使う JVM は `JAVA_HOME` または `PATH` に依存します。
- ローカル開発では PowerShell 用の [`scripts/use-java.ps1`](scripts/use-java.ps1) を使う前提にします。

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
- `1.20.1-main`（1.20.1）で作業する場合:

```powershell
.\scripts\use-java.ps1
```

- `main` で作業する場合:

```powershell
.\scripts\use-java.ps1 -Version 21
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

- `runGameTestServerCompat` は Farmer's Delight / Create / Botania / Lodestone / Malum 連携を確認します。
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

- IntelliJ IDEA で実行構成を再生成する場合:

```powershell
./gradlew.bat genIntellijRuns
```

- `genIntellijRuns` 後、通常の `runClient` に加えて次の client 構成を使えます。
  - `runClientCompat`
  - `runClientEasyMagic`
  - `runClientBetterCombat`
  - `runClientEpicFight`
  - `runClientEpicFightController`
  - `runClientCompatEasyBetter`
- `runClientEpicFightController` は Epic Fight / Controlify Forgified / YACL を入れ、実機コントローラー入力を確認する構成です。
- `runClientCompatEasyBetter` は compat + EasyMagic + Better Combat を入れた実環境寄りの手動バランス確認用です。Epic Fight は含めず、自動テスト対象にもしていません。
- 一時的な組み合わせ確認では Gradle プロパティでも追加できます。

```powershell
./gradlew.bat runClient "-PdevRuntimeMods=create,malum"
./gradlew.bat runClient "-PdevRuntimeMods=epic_fight"
```

- `devRuntimeMods` には `compat`, `easy_magic`, `better_combat`, `epic_fight`, `epic_fight_controller`, `compat_easy_better` または個別名（`create`, `botania`, `malum`, `controlify` など）をカンマ区切りで指定できます。
- Better Combat と Epic Fight は干渉が大きいため、同時投入は通常確認では避けます。

## GitHub 運用

- `1.20.1-main` への反映は、バージョン更新を含めてすべて PR 経由で行います。
- PR では GitHub Actions の `PR CI / build` が必須です。`PR CI / gametest` も実行しますが、1.20.1 側では任意チェックとして結果を確認します。
- optional MOD 付きの特殊 GameTest / client 起動は required CI に含めません。必要な変更ではローカルまたは Codex 実行結果を PR コメントや最終報告に残します。
- Codex Cloud のスマートトリガーレビューをレビュー補助として使います。人間の判断と CI 通過を置き換えるものではありません。
- CI は GitHub-hosted runner 上で `pull_request` イベントだけを使い、repository secrets は使いません。
- workflow の action はフル SHA pin を前提にし、`GITHUB_TOKEN` は read-only に制限します。
- 通常のマージ方法は `merge commit` を使います。
- バージョン更新だけは、`1.20.1-main` 上で不要なマージコミットを増やさないために `rebase merge` を使って構いません。
- `squash merge` は使いません。
- GitHub 側の具体的な Ruleset / Actions 設定手順は [docs/github-pr-protection.md](docs/github-pr-protection.md) を参照してください。

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
