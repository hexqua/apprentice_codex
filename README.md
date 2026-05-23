# Apprentice's Codex

## 概要

Iron's Spells 'n Spellbooks用の小さなアドオンMODです.

## 開発環境

- 開発ブランチ `1.21.1-main` では Java 21 を使用します。
- `build.gradle` では Java toolchain を 21 に固定していますが、`gradlew.bat` 自体が使う JVM は `JAVA_HOME` または `PATH` に依存します。
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
- `1.21.1-main` で作業する場合:

```powershell
.\scripts\use-java.ps1
```

- `main`（1.20.1）で作業する場合:

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

## PR 運用

- `1.21.1-main` への取り込みは PR 経由で行い、`PR CI / build-and-gametest` の成功を必須とします。
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
