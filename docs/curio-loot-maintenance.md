# Curios loot の保守方針

## 基準と供給量

Iron's Spells 'n Spellbooks `1.20.1-3.16.3` の一般アクセサリ（`magic_items/basic_curios`）抽選先を基準とする。互換MODの追加先はIron'sが指定するIDを踏襲し、未導入時は追加しない。実行時の自動検出は行わない。

CodexはIron'sの実効当選確率の半分で1個を独立追加する。既存の抽選枠の重みは変更しない。候補数で全体の確率を増減させず、既存のサーバー設定 `Loot.enableApprenticeCurioLoot` / `Loot.apprenticeCurioLootChanceMultiplier` を維持する。設定倍率1では両MODの合計期待入手数がIron's単独の1.5倍となる。

Vaultも生成された報酬リストへの追加であり、元の報酬を押し出さない。1.20.1にはバニラのTrial／Ominous Vaultは存在しない。以下の銀塊は独立した確定報酬として追加する。

| 対象 | Codexの確率（設定倍率1） |
|---|---:|
| 一般チェスト・魔法の本棚 | 2.5% |
| ネザー・Pyromancer基本倉庫 | 3.75% |
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

Catacombs WallのIron's実効確率は35%枠内の重み1/4による8.75%。依存更新時はpoolの条件だけでなく、重み・rolls・空エントリも確認する。

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

Iron's 1.20.1-3.16.3のjarには`chests/catacombs/crypt_loot`も残っているが、同版の構造物NBTは`chests/catacombs/dead_king_vault`を参照するため追加先を更新する。旧cryptテーブルを明示参照するデータパックにはCodexの追加がなくなる。Ice Spider Denのdungeonは`add_apprentice_curios_to_ice_spider_dungeon`と`chests/ice_spider_dungeon_curio_bonus`へ分離する。

datagen後は旧JSONの削除をGit差分で確認する。`src/generated/resources/.cache`に登録されていない古いファイルはrunDataだけでは消えない。リリース時には上記のID変更と抽選候補・供給確率の変更を案内する。

## Backport

PR #923の非mergeコミット`25f200ff`、`fc74ea50`、`1442d7ee`、`3133f5ba`を移植する。Forgeのmodifier条件、Java 17のAPI、1.20.1のresource形式を使用する。既存のボーナスチェストへの異世界旅行ガイドブック追加は維持する。

Trial専用枠、Iron's Jewelryの金属素材連携（`ada55820`）、MalumのSoulwoven Pouch効率タグ（`55b31ae9`）は1.21.1専用のため対象外。依存MODの追加・更新は行わない。

## マナを纏った銀塊とSilver Ring

`mana_enveloped_silver_chunk`をVault報酬へ確定追加する。Bone Vault（`irons_spellbooks:chests/catacombs/dead_king_vault`）、Cinderous Vault（`irons_spellbooks:chests/citadel/citadel_vault`）は2〜3個を各50%で追加する。

`irons_spellbooks:append_loot`で既存報酬を保持し、Curios設定・確率倍率から独立させる。追加lootは`apprenticecodex:chests/silver_chunk_special`、modifierは`apprenticecodex:add_silver_chunk_to_special_vault`。新規設定は設けずdatapackで調整する。Tincture of Forgetfulnessで本人の受取履歴をリセットし、鍵を消費して再開封した場合も同じ追加を行う。過去の報酬への遡及配布はない。

通常作業台の中央にMithril Scrap1個、周囲に銀塊8個を置き、本物のSilver Ring1個を作成する。レシピIDは`apprenticecodex:silver_ring_from_mana_enveloped_silver_chunk`。銀塊は破砕還元せず、Silver Ringの既存Scrap1個還元を維持する。調整スロットの互換素材にはしない。スポナー・一般チェスト・ボス直接報酬には追加しない。

### 1.20.1固有の対応

基準は両側Iron's 3.16.3。アイテム・レシピ・Bone/Cinderous Vaultの2〜3個報酬を移植する。通常/OminousのバニラVault枠は対象外。1.20.1のMagic Vaultは登録されているが同版の構造物NBTに自然配置がなく、既定lootも空のため追加先にしない。地形・構造物は追加変更しない。

Forgeのloot table条件、`recipes` / `loot_tables`ディレクトリ、レシピresultの`item`形式へ補正して再生成する。JEI説明は通常・不吉なバニラVaultを案内しない文面へ変更する。1.20.1ではボス系Vaultが保証経路となる進行差を許容する。

### 検証方針

Java 17で`runData`、`build`、`runGameTestServer`を実行する。GameTestでは2種類のVault報酬・Curios設定からの独立性・基礎報酬の保持・対象外loot・作成材料不足・破砕経済を確認する。画像・JEI表示とTinctureによる実際の再開封操作は手動確認対象とする。
