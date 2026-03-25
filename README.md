# Apprentice's Codex

## 概要

Iron's Spells 'n Spellbooks用の小さなアドオンMODです.

## 導入方法

- `mods`配下に`jar`を入れればOKです。
- 現時点でIron's Spells 'n Spellbooks及びそれの前提MOD以外の前提MODはありません.

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
