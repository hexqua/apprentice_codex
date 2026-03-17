# Enchant / Repair Checks

## 基本原則

- 1.21.1 では enchant 適用可否の多くが `data/*/enchantment/*.json` の `supported_items` / `primary_items` と item tag で決まる。
- `canApplyAtEnchantingTable` / `isBookEnchantable` / `supportsEnchantment` などのメソッド移植だけで完了と判断しない。
- 1.20.1 側で `isValidRepairItem` を実装していたアイテムは、1.21.1 で素材定義だけに任せてよいとは限らない。

## 防具

- `minecraft:head_armor`
- `minecraft:chest_armor`
- `minecraft:leg_armor`
- `minecraft:foot_armor`
- 必要に応じて `minecraft:enchantable/durability`
- 必要に応じて `minecraft:enchantable/equippable`
- 必要に応じて `minecraft:enchantable/vanishing`

防具を移植する場合は、上記 tag への登録漏れと対象 enchantment JSON の参照先を必ず照合する。

## 武器・ツール・特殊アイテム

- 対象 enchantment JSON が参照する `minecraft:enchantable/*` を洗い出す。
- 独自 tag がある場合は datagen 側と `src/generated/resources` 側の両方を確認する。
- Java 実装を port しただけで付与可否が戻ると思い込まない。

## 検証観点

- エンチャントテーブル
- 金床でのエンチャント本適用
- 素材修理

この 3 経路を対象アイテムごとに確認する。
