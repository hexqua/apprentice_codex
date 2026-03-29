# Apprentice's Codex

## 概要

Iron's Spells 'n Spellbooks用の小さなアドオンMODです.

## 導入方法

- `mods`配下に`jar`を入れればOKです。
- 現時点でIron's Spells 'n Spellbooks及びそれの前提MOD以外の前提MODはありません.

## 開発時テスト

- 通常のビルド確認:

```powershell
./gradlew.bat build
```

- サーバー側の結合テスト:

```powershell
./gradlew.bat runGameTestServer
```

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

## データパック調整

- 親和ポーションの素材は、既定では各 school の `focus` を使います。
- `focus` が他 school と衝突する場合は、`data/apprenticecodex/school_affinity_catalysts/*.json` で school ごとに親和用触媒を別指定できます。
- 親和用触媒ではレシピ曖昧化を避けるため、`minecraft:redstone` は `minecraft:redstone_block`、`minecraft:glowstone_dust` は `minecraft:glowstone` として扱います。
- 形式例:

```json
{
  "overrides": [
    {
      "school": "examplemod:some_school",
      "item": "minecraft:amethyst_shard"
    }
  ]
}
```

- 同じ item に複数 school を割り当てた場合、その item を共有する school だけ親和レシピが無効になります。

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
