# Spectrumエンチャント連携の設計と1.20.1への引き継ぎ

## このブランチの対応

移植元は `eb44a9fc1bf8feb6a7a90742da1059a4664b2504`（PR #1044のレシピ・限界突破検証）。
起動構成は先行して追加した `runClientConnector` / `runGameTestServerConnector` を維持し、
本作成12種・強化10種を1.20.1の `recipes/spectrum/` へ生成する。
完成本は `result.item` とshort型の `lvl` を持つ `nbt.StoredEnchantments`、
強化は `item` と8皿合計の `item_count` を使う。Forgeの `conditions` / `forge:mod_loaded` で
Spectrum不在時の読込を省略する。通常上限や進捗の自動解放は変更しない。

1.8.15のserializerは強化を段階別レシピへ展開し、一部のIDをSpectrum名前空間へ移す。
GameTestでは全段階の入力境界・完成本・XP消費、保存済み所有者フラグによる限界突破判定、
上限超過後の効果、独立したRecipeManagerへの2回の再読込を確認する。
Connector変換後の型にはテスト内からreflectionで接続し、コンパイルとdatagenにFabric型を追加しない。
通常tickでのマルチブロック製作とEMI表示は、人間が `runClientConnector` で確認する。
Connector環境の起動とワールドログインはユーザー確認済み。今回追加するレシピの人間確認結果は未把握。

以下のmain節は移植元の仕様を示す。取り込み後のバージョン更新時に、この引き継ぎ文書を削除する予定。

## mainの実装

本Iの作成12種と強化10種を、Spectrumのクラスに依存しない専用DataProviderから生成する。
定義は `SpectrumEnchantmentRecipeDataGenerator` に集約し、出力は
`data/apprenticecodex/recipe/spectrum/{enchanter,enchantment_upgrade}/` とする。
Spectrumの型を参照するのは、導入時だけロードされるGameTestシナリオに限定する。
通常runtimeとdatagenにはSpectrumを追加せず、`compat` / `compat_easy_better` へ追加する。

検証は `runGameTestServerCompat`、人間の手動確認は `runClientCompat` を使う。
Spectrumのレシピ表示確認には、CompatへEMIだけを追加する `runClientCompatEmi` を使う。
このclient専用構成は1.21.1側の表示確認用であり、1.20.1側へそのまま移植しない。
Spectrum 1.12.9 / Revelationary 1.5.2 / Modonomicon 1.117.4をcompatへ追加する。
専用のSpectrum実行構成は設けず、必要時のclient切り分けは `runClient -PdevRuntimeMods=spectrum` で行う。
Magitech構成のModonomiconも1.117.4へ統一し、複数profile指定時の別版二重読込を避ける。
Spectrum 1.12.9の必要条件に合わせ、NeoForgeを21.1.249へ更新した。
新しい素材や外部アセットは同梱せず、既存アイテムのIDをレシピから参照する。
Pastel向けの変換・別名出力は行わない。

## レシピの契約

本作成は本1冊を中央に置き、外周に顔料4個・素材1を2個・素材2を2個。
素材1と素材2は異なるものとし、配置は「顔料、顔料、素材1、素材2」を2回繰り返す。
全本レシピは300 tick、100 XP。必要進捗はSpectrumのエンチャンター構造完成。
色・素材に加えて独自の終盤ゲートを設けない。

| エンチャント | 色 | 素材1 | 素材2 | 強化上限 | I→IIの1皿分 / XP |
|---|---|---|---|---:|---|
| Surge | 紫 | 神秘の燃え殻 | Storm Stone | VIII | 1 / 200 |
| Attunement | 緑 | 神秘の燃え殻 | Storm Stone | VIII | 1 / 200 |
| Tense | マゼンタ | 神秘の燃え殻 | 砂糖 | VIII | 1 / 200 |
| Alacrity | 黄 | 神秘の燃え殻 | 砂糖 | VIII | 1 / 200 |
| Reflux | 黒 | 神秘の燃え殻 | 秘術のエッセンス | VIII | 1 / 400 |
| Reservoir | シアン | 神秘の燃え殻 | 秘術のエッセンス | VIII | 1 / 400 |
| Transcendence | 紫 | 神秘の燃え殻 | 叡智の破片 | I（作成のみ） | — |
| Wisdom | 紫 | エンチャント瓶 | 結晶化した秘術の破片 | VI | 1 / 200 |
| Plunder | 水色 | 結晶化した秘術の破片 | 四葉のクローバー | VI | 2 / 200 |
| Red Energy | 赤 | レッドストーン | 安楽の果実 | VI | 2 / 400 |
| Glow Energy | 黄 | グロウストーンダスト | 安楽の果実 | IV | 2 / 400 |
| Synthesis | 赤 | Quitoxic Powder | 神秘の燃え殻 | I（作成のみ） | — |

Large Mug、Guzzle、Shell、Synchronization、Neutralizationは本作成・強化とも追加しない。

強化は本作成と同じ色の顔料だけを要求し、通常上限の前後を含め各段階で数量・XPを倍増する。
1皿64個（8皿合計512個）を超える定義は生成時に拒否する。
最終段階の合計顔料はVIII組512個、Wisdom128個、Plunder/Red Energy256個、Glow Energy64個。
通常のエンチャント定義の最大レベルは変更しない。

限界突破の許可はSpectrumの `milestones/unlock_overenchanting_with_enchanter` に委ねる。
調査時点ではこの進捗は未実装の `spectrum:__wip` を参照する。
MOD側から進捗を置き換えたり自動解放したりせず、modpack側で進捗を付与できる余地を保つ。

## 1.20.1で補正するもの

本作成・素材・色・コスト・強化上限は共通の移植対象。
NeoForge依存更新とmainのSpectrum runtime設定はそのままcherry-pickせず、Forge側で設計し直す。
特に、mainの `compat` / `compat_easy_better` へのSpectrum追加と必須MOD ID追加は1.21.1固有の起動構成変更である。
1.20.1の通常compatには引き継がず、次節のConnector専用構成へ分離する。

- 配置先を `recipe` から `recipes` へ変更して再生成する。
- 本の `result.id` / `components.minecraft:stored_enchantments` を、1.20.1用の
  `result.item` / `nbt.StoredEnchantments`（idとshort型lvl）へ変換する。
- 強化の `ingredient` を `item`、`ingredient_count_per_bowl` を `item_count` に変換し、数量は8倍する。
  旧形式の配列も先頭がI→II。Forge側のJSONは1.20.1版serializerと照合する。
- `neoforge:conditions` をForge側の条件付きレシピへ置き換える。
  Fabric側の `fabric:load_conditions` を無条件にコピーせず、Connector下での評価順も検証する。
- Spectrum素材とこのMODのアイテム・エンチャントIDが移植先に存在するか確認する。
- 通常上限、限界突破の進捗ID・所有者判定、強化後の実効果を1.20.1側で再確認する。
- 新旧配置の生成物を併置しない。`.cache` 管理外の古いJSONはrunDataだけでは削除されないため、差分で確認する。

## Connector検証環境

1.20.1ではFabric版SpectrumをConnector経由で使用するため、通常compatへ混ぜず専用構成を用意する。
Spectrum、Connector、Forgified Fabric APIと、採用版の必須MODをまとめて固定し、Java 17で実行する。
採用版はSpectrum 1.8.15、Connector beta.49、Forgified Fabric API 0.92.6+1.11.15、
Revelationary 1.3.9、Modonomicon 1.79.3、Trinkets 3.7.2、Cloth Config 11.1.136。
Connector Extras 1.11.2とForge版EMI 1.1.24もclient・GameTestへ常時導入する。
固定したModrinth IDと開発環境固有の補正は `gradle.properties` / `build.gradle` を正とする。
datagenは外部MODを起動せず実行できる構成を維持する。
専用構成の未導入を通常GameTestの失敗にせず、専用実行時は必須MOD検査で導入漏れを失敗させる。
本作成・強化・限界突破・再読込と、通常構成でレシピが安全に省略されることを検証する。

## mainのテスト境界

本レシピは上流serializerで読み込んだ定義に正しい入力・誤った入力を与え、完成本を確認する。
強化は全段階で必要顔料ちょうど・1個不足・XP1不足・誤素材を区別する。
上流の完成処理を直接呼び、保存された本のレベルとXP消費を確認する。
限界突破は所有者の保存済み許可フラグを切り替えて、上流装置のprivateなレシピ選択処理へ接続する。
これは通常tickによるマルチブロック全体のクラフト、進捗取得操作、client描画の検証とは区別する。
上限超過時の効果は実装備の属性、経験値イベント、Looting変換、フラスコから取り出した効果で確認する。
