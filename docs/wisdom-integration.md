# 他 MOD 装備の Wisdom 対応（Minecraft 1.21.1）

アイテムタグの JSON を MOD の `src/main/resources` に同梱することで、Apprentice's Codex を必須依存にせず Wisdom へ対応できます。Java API の参照や `WisdomPolicy` の実装は不要です。同じファイルをデータパックへ配置しても対応できます。

## 使用するタグ

| タグ ID | 役割 |
| --- | --- |
| `apprenticecodex:wisdom_enchantable` | 通常付与（エンチャントテーブル・本）の対象にする。 |
| `apprenticecodex:wisdom/held` | メインハンド・オフハンドで経験値増加を有効にする。 |
| `apprenticecodex:wisdom/armor` | 防具スロットで経験値増加を有効にする。 |
| `apprenticecodex:wisdom/curios` | Curios の実装備スロットで経験値増加を有効にする。 |

通常付与のタグと、使用する装備場所の効果タグへそれぞれ追加してください。複数の装備場所へ対応する場合は、必要な効果タグすべてへ追加します。Curios の装備可能スロットを増やす機能は含まれません。

## 杖を通常付与・手持ち効果へ対応させる例

次の2ファイルを同梱します。名前空間は自 MOD のものへ変更せず、`apprenticecodex` を使ってください。

```text
data/apprenticecodex/tags/item/wisdom_enchantable.json
data/apprenticecodex/tags/item/wisdom/held.json
```

両ファイルの内容を次のようにします。`examplemod:mage_staff` は対応させるアイテム ID へ置き換えます。

```json
{
  "replace": false,
  "values": ["examplemod:mage_staff"]
}
```

防具なら `wisdom/held.json` の代わりに `wisdom/armor.json`、Curios 装備なら `wisdom/curios.json` を用意します。Apprentice's Codex 不在時は Wisdom 自体が存在せず、これらのタグによる Wisdom の効果は発生しません。タグだけの対応に、必須依存の宣言や Java の連携クラスは必要ありません。

第三者の互換データパックなど、対象アイテムの MOD も不在になり得る場合は、欠けたエントリでタグの読み込みを失敗させないよう次の形式を使います。

```json
{
  "replace": false,
  "values": [
    { "id": "examplemod:mage_staff", "required": false }
  ]
}
```

## 効果と注意点

- タグで許可しただけでは経験値は増えません。装備に Wisdom が付与されている必要があります。
- 手持ちはレベル当たり +20% で、両手の高い方のレベルを採用します。防具・Curios は各装備のレベル当たり +5% を合算します。Mob 撃破・採掘の両方へ適用され、補正後の端数は切り上げます。
- 付与タグだけの追加では効果は発動しません。効果タグだけの追加では通常付与できませんが、コマンドや独自の付与経路で Wisdom が付いた場合は発動します。
- エンチャントテーブルでの付与には、アイテム自身のエンチャント可能性・エンチャント値などの条件も必要です。アイテム独自の付与制限もタグだけでは解除されません。
- 既存の `WisdomPolicy` による許可も維持します。効果タグは許可を追加するもので、既存ポリシーの許可を取り消しません。手持ちを許可しないポリシーの装備も、明示的に `wisdom/held` へ追加すると手持ちで発動します。
- タグはデータパックの `/reload` で再読み込みできます。効果対象の独自キャッシュはありません。

この仕様は 1.21.1 向けです。1.20.1 へ移植する場合は `tags/items` の配置と Forge 側の付与判定への接続を別途補正する必要があります。
