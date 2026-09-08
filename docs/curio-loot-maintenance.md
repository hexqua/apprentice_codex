# Curios loot の保守方針

## 基準と供給量

Iron's Spells 'n Spellbooks `1.21.1-3.16.3` の一般アクセサリ（`magic_items/basic_curios`）抽選先68テーブルを基準とする。互換MODの追加先はIron'sが指定するIDを踏襲し、未導入時は追加しない。実行時の自動検出は行わない。

CodexはIron'sの実効当選確率の半分で1個を独立追加する。既存の抽選枠の重みは変更しない。候補数で全体の確率を増減させず、既存のサーバー設定 `Loot.enableApprenticeCurioLoot` / `Loot.apprenticeCurioLootChanceMultiplier` を維持する。設定倍率1では両MODの合計期待入手数がIron's単独の1.5倍となる。

Vaultも生成された報酬リストへの追加であり、元の報酬を押し出さない。Ominous VaultはIron'sの限定リング枠として扱い、Codexからは追加しない。

| 対象 | Codexの確率（設定倍率1） |
|---|---:|
| 一般チェスト・魔法の本棚 | 2.5% |
| ネザー・Trial通常チェスト・Pyromancer基本倉庫 | 3.75% |
| 通常Trial Vault | 3/58（約5.17%） |
| good・Ancient City | 7.5% |
| Evoker Fort・Generic Magic Treasure | 10% |
| Ice Spider Den地下・塔、砕氷船の船長室 | 12.5% |
| Ice Spider Denのdungeon | 5% |
| Mountain Tower | 15% |
| Mangrove Hut・Pyromancer Supplies | 20% |
| Stronghold Library・End City・Citadel Vault | 25% |
| 互換MODのtreasure枠 | 40% |
| Catacombs Wall | 4.375% |
| Dead King Vault | 12.5% |

Catacombs WallのIron's実効確率は35%枠内の重み1/4による8.75%。通常Trial Vaultは重み3/29。依存更新時はpoolの条件だけでなく、重み・rolls・空エントリも確認する。

## 共通候補

構造物・Schoolによる品目の偏りは付けず、全対象で以下の14種類（総重み23）を使用する。追加時に魔法注入・エンチャントは行わない。

| 重み | アイテム |
|---|---|
| 2 | ScarletThirst、CraftsmansDelight、ProtectionSpellSupporter、EnchantedCirclet、SpellCastParryingRing、AutocastAmulet、SatelliteFollowcastAmulet、ManaShieldCharm、MonarchBondCharm |
| 1 | AttackcastRing、ManaThruster、JumpcastCharm、ManaManeuverGear、QuickcastScrollCartridge |

重み1はミスリルの材料費を考慮したもの。全候補にGrindRunner還元を用意する。今回追加した還元はManaManeuverGear／QuickcastScrollCartridgeがスクラップ各2個、MonarchBondCharmがスクラップ1個＋Evocation Rune1個。既存の還元量は変更しない。

### 除外理由

- 魔法書類：クラフトなど既存の入手経路を維持する。
- AbsorptionAmplifyAmulet：クラフトコストが高く、通常枠には重すぎる。
- SpellcasterAmmoPouch、SpellcasterQuiver、AshenCirclet：素材が比較的軽く、共通候補内で価値が低くなりやすい。
- MagiCompressorGadget：Create連携を前提とする。
- UndyingEmblem：不死のトーテムを材料とする特殊な価値をクラフト経路に残す。

## 廃止IDとデータパックへの影響

以下はいずれも`apprenticecodex`名前空間。継続するloot IDと設定キーは維持するが、旧リソースを上書きしていたデータパックは更新が必要。旧IDへの互換転送は設けない。既に生成済みのチェスト内容や所持品は変更しない。

| 廃止リソース | 後継・扱い |
|---|---|
| modifier `add_apprentice_curios_to_catacombs_crypt` | `add_apprentice_curios_to_dead_king_vault` |
| loot `chests/catacombs_crypt_curio_bonus` | `chests/dead_king_vault_curio_bonus` |
| modifier `add_apprentice_curios_to_ominous_vault` | 廃止。後継なし |
| loot `magic_items/ominous_vault_curios_bonus` | 廃止。後継なし |

Iron's側の削除済み`chests/catacombs/crypt_loot`への参照を現行`chests/catacombs/dead_king_vault`へ更新する。Ice Spider Denのdungeonは`add_apprentice_curios_to_ice_spider_dungeon`と`chests/ice_spider_dungeon_curio_bonus`へ分離する。

datagen後は旧JSONの削除をGit差分で確認する。`src/generated/resources/.cache`に登録されていない古いファイルはrunDataだけでは消えない。リリース時には上記のID変更とOminous追加廃止を案内する。

## Backport

共通候補・重み・固定倍率・還元方針はbackport候補。loot ID、Forgeのmodifier条件、resourceのディレクトリ形式は1.20.1側の依存版に合わせて再確認・再生成する。Trial専用枠は1.20.1へ移植しない。この変更では1.20.1側の実装・検証を行わない。
