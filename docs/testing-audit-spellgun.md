# 魔法銃系テストの機能別棚卸し

魔法銃系では、保存データと収納物の保護、消費・拒否・recast の状態遷移、装備と依存 MOD の接続を残し、定義の読み返しや同じ経路の重複を整理する。優先するのは、未登録の denylist 検証と成功判定の弱い recast 検証の改善、その後の重複統合と属性テストの実装備化である。

対象は `8168c23e6` 時点の `gametest/equipment/spellgun`。初回棚卸しの日付は 2026-11-07。判断基準は [テスト設計・整理方針](testing-policy.md) に従い、後続の変更で判明した事情に合わせて見直す。以下の棚卸し本文は初回時点の判断記録であり、件数の削減目標にはしない。実施内容と配置の現状は末尾の「実施結果」を参照する。

## 対象と機能の境界

`src/main/java/jp/aquafactory/apprenticecodex/gametest/equipment/spellgun` は14ファイル、94件の `@GameTest` 定義を含む。委譲先のシナリオを別のテストとして重複計上しない。

| 入口クラス | 件数 | 主な検証対象 |
| --- | ---: | --- |
| `EquipmentGameTests` | 35 | 銃本体27件、多目的ライフル7件、反射詠唱盾1件 |
| `FullautoRapidcastSpellrifleGameTestScenarios` | 16 | フルオートの発動、較正、反動、ADS、消費 |
| `FullautoEchoGameTests` | 9 | 残響較正とコスト・ダメージ・同期 |
| `MultipurposeStaffrifleReworkGameTests` | 5 | 多目的ライフルの保存枠、魔法書選択、LONG、反動 |
| `QuickcastCartridgeGameTests` | 8 | カートリッジの収納・装備・発動・予約 |
| `QuickcastCartridgeChargeGameTests` | 10 | 回復、再装填、中断、確認入力、保存復元 |
| `SpellgunAmmoConsumptionGameTests` | 4 | 弾薬節約 Attribute と装備・旧データ・消費経路の接続 |
| `SpellgunCooldownRulesGameTests` | 4 | CD境界、装備との処理順、recast の保存復元 |
| `RifleEquipmentAttributeGameTests` | 1 | ライフル2種の実装備・交換・解除後の属性 |
| `RifleSpellTooltipGameTests` | 2 | 保存枠の読み取りと残響プロファイル同期 |
| **合計** | **94** | |

残る4ファイルはシナリオまたは補助処理であり、登録件数は0件である。

- `EquipmentSpellGunGameTestScenarios`
- `MultipurposeStaffrifleGameTestScenarios`
- `SpellgunCooldownTestSupport`
- `StaffrifleDenylistScenarios`

カートリッジ18件は、銃の弾薬ではなく独立した Curios 装備の検証である。将来は `equipment.quickcastcartridge` 等へまとめる。`reflectcastShieldCastRestrictionsFollowCalibration` は盾の機能パッケージへ移す。銀の魔法増幅器による銃の較正は、銃の装備挙動として銃側に残してよい。

`RifleSpellTooltipGameTests.echoProfileSyncReplacesAndClearsClientIds` は共有の `MulticastEchoStaffAttackProfileManager` と packet の検証である。残響杖側の同期機能へ配置し、ライフルは利用側の表示データを確認する。これらの移動では、プロジェクト全体の登録件数は減らない。

較正受付は `crafting/calibration`、エンチャント適性・効果は `equipment/enchantment`、Better Combat の持ち手解決は `equipment/offhand` にも関連テストがある。これらは共通機能の検証として扱い、銃側へ同じ全組み合わせを再追加しない。今回の94件には含めない。

## 維持する保証

| 機能 | 維持する結果と境界 | 現在の検証経路 |
| --- | --- | --- |
| 銃の発動・拒否 | 保存済みの非対応魔法の拒否、別の詠唱の維持、最大マナ例外の有効範囲、持ち手の制約 | `tryTriggerImbuedSpell`、`use` 等の直接呼び出しと状態確認 |
| 保存と抽出 | 上書き魔法の抽出可否、抽出済み状態の維持、旧 locked データの回復、収納物の非消失 | normalization、メニュー操作、ItemStack の保存・復元 |
| ライフルの発動 | 多目的のマナ免除とフルオートの通常マナ消費、弾薬不足時の拒否、同一入力の制限 | 実際の発動入口、魔法の完了処理、イベント handler、rate limiter |
| CD | 元のCDと実効CDの区別、閾値・下限、装備短縮後のLONG加算、recast 完了時の武器切替 | 純粋計算、handler 直接呼び出し、EventBus、recast のNBT往復 |
| 弾薬節約 | 複数の Attribute 補正、装備解除、旧データ、弾薬とマナの例外設定の接続 | Attribute 実値、ログイン等の handler、消費イベント |
| 保存枠と較正 | 拡張解除後もスクロールを保持・抽出できること、保存されたLONGの使用可否、魔法書との選択分離 | 較正メニュー、保存・復元、選択解決、発動入口 |
| 残響 | 開始時の必要マナと実消費、割引・Zenith・免除の組み合わせ、無敵時間、発射後の武器切替 | 発動入口、魔法の明示的な完了、実ダメージ、生成済み投射物の情報 |
| カートリッジ | 予約と消費の区別、回復、再装填確認・中断、装備交換、最終ダメージによる中断、保存復元 | 装備・発動・イベント経路と期限付きのtick進行 |
| 依存との接続 | Epic Fight の持ち手・capability、Better Combat の論理offhand、Malum有無によるレシピ読込 | 対応プロファイルでの実状態・分岐確認 |

保存復元、実発動、handler 単体は保証範囲が異なる。似た名前のテストをまとめる場合も、通している経路と境界条件を残す。

## 優先する書き換え候補

### 未登録の多目的ライフル denylist

初回時点の `StaffrifleDenylistScenarios`（整理後は削除） の `multipurposeStaffrifleServerDenylistBlocksSpecialCastSpell` は、`@GameTest` がなく、IDEの呼び出し階層でも呼び出し元がない。94件に含まれず、現在は実行されない。

製品側の `MultipurposeStaffrifle.tryTriggerSelectedSpell` には denylist による拒否処理が存在する。後続変更では、既にスクロールを保存した状態から設定を変更し、実際の発動入口が拒否すること、弾薬・マナ・詠唱状態を変えないことを登録済みのテストで確認する。設定を戻した場合と、フルオート側の独立した設定を変更した場合も区別する。

フルオートの `fullautoEnchantmentsAndIndependentDenylist` も、denylist 部分は述語の直接確認である。エンチャント適性のケースと分け、両ライフルの発動拒否を機能単位で検証する候補とする。既存の述語確認を削除するのは、その接続を確認できてからとする。

### 弾薬なし recast の成功判定

`spellcasterGunRecastCastBypassesAmmoRequirement` は、弾薬不足による初回拒否でも `use` が入力を消費することを期待する。その後の recast でも入力消費を確認しているため、この戻り値だけでは発動成功を区別できない。

入力消費のUX契約を維持しつつ、recast 残数、実行された効果、詠唱状態など、その魔法の成功を識別できる結果を確認する。後半のEventBus経由の弾薬非消費確認は残す。ライフル2種の recast handler 検証は実装経路が異なるため、一律に重複扱いにしない。

### 銃の属性と銀の魔法増幅器

次の4件は、主にmodifierの定義・イベント出力・手動で作った `AttributeInstance` を確認している。

- `spellcasterGunsRemoveBaseSpellPowerButKeepSurge`
- `silverSpellAmplifierMovesAllSpellgunAttributesToOffhand`
- `silverSpellAmplifierKeepsDualSpellgunModifiersIndependent`
- `silverSpellAmplifierMovesUpgradeOrbModifiersToOffhand`

較正前後、mainhand/offhand、両手装備、片方の解除、Upgrade Orbとの適用順と保存復元を、実際のプレイヤーの装備更新後の属性で確認する形へ改める。演算を区別できる基礎値や別の補正も用いる。IDの分離やslot変換など、原因特定に有効な定義確認を残す場合は、実値確認との役割を分ける。

既存の `RifleEquipmentAttributeGameTests` はライフル2種の無補正を検証するため、この4件の代替にはならない。fixture はポリシーの ServerPlayer と `doTick()` の注意に従う。

### 弾種と薬莢返却

`multipurposeStaffrifleUsesDedicatedAmmoAndCasingReturnPolicy` と `fullautoRapidcastSpellrifleUsesDedicatedAmmoAndCasingReturnPolicy` は、弾アイテム、薬莢アイテム、返却率のgetterを確認する。

弾薬を実際に消費し、対応する弾だけが減ること、非消費時に薬莢を作らないこと、返却された薬莢の種類と収納先を確認する形へ書き換える。有限回の抽選で返却の発生を要求せず、返却分岐の制御方法を先に設計する。0.2や0.9の完全一致だけを維持する目的で、world付きテストを残さない。

## 統合と削除の候補

| 候補 | 統合後に残す検証 | 判断条件 |
| --- | --- | --- |
| `goldSpellcasterGunImbuedSpellStaysRemovableAfterNormalization` と `...AfterSaveLoad` | normalization直後と保存・復元後の両方の抽出可否 | 復元後のrepairが保存前の不具合を隠す可能性があるため、保存前のassertionを移してから単独ケースを削除する |
| `multipurposeStaffrifleInstantCastConsumesAmmoAndAppliesCooldownPolicy` | `multipurposeStaffrifleCastsAtZeroMana` の実発動経路での一発消費とCD結果 | 現在の実発動テストの `isOnCooldown` だけではCD量を固定しないため、必要な結果を補ってから旧handlerケースを統合する |
| `fullautoRapidcastSpellrifleSpecialCooldownPolicyMatchesDefaults` | 純粋計算の境界と、設定・item・イベント間の接続 | `FullautoCooldownPolicy.calculate` の境界テスト、EventBusを通す `fullautoEquipmentOrderMatchesPreview` と役割を整理する。非既定値の設定が利用先へ届くことを確保し、既定値3例の重複を縮小する |
| `fullautoLongSupportAndCooldownOrder` のCD部分 | LONGの保存・Silver Ringによる有効化と、装備短縮・詠唱速度・イベント順 | 較正と収納物保護は残す。期待値に同じitem計算を呼ぶCD部分は、独立した計算境界とEventBus検証へ寄せる |
| 両ライフルの `...RateLimitIgnoresLegacyPersistentNbt` | 旧NBTの無視、同tick拒否、logout解除 | 多目的は固定間隔、フルオートは設定間隔で別実装。準備・観測の共通化候補とし、両実装を通すケースは残す |

`spellcasterGunTooltipsUseCommonOperationDescriptions`、`spellcasterRoundTooltipsUseSharedKeys`、両ライフルの `...TooltipShowsControlsBeforeShiftHint` は、静的なキー・順序の全件固定を削除または代表ケースへ縮小する候補である。Epic Fight有無による警告、利用可能な魔法種別、無効な保存魔法のエラー内容、可変の表示引数は、独立した分岐や状態を検出する部分として区別する。

## 純粋計算の実行先（再検討後の方針）

- `fullautoThresholdAndFloorAreIndependentOfEffectiveCooldown` は、worldを使わず引数だけで `FullautoCooldownPolicy.calculate` を確認する独立したGameTestとして維持する。設定を読む `resolve` とイベント接続の検証は別途残す。
- `fullautoRecoilDistributesAndOverlapsWithoutLosingAngle` と `fullautoRecoilBuildsRecoversAndReducesWhileAiming` は、明示時刻で反動の計算・状態遷移を確認するGameTestとする。合計角度、重複サンプリング、時間巻戻し、上限、回復を残す。
- `recoilAndAdjustmentsRetainSingleShotContract` に混在していた反動計算は `multipurposeRepeatedShotsKeepTheSameRecoilWithoutBuildup` として分離し、較正・旧エンチャントデータのGameTestも維持する。
- `chargeTimeAndPersistence` の丸め・下限・飽和計算は `quickcastRecoveryRoundsUpRespectsTheMinimumAndSaturates` として分離する。Capabilityの保存復元、死亡clone、装備交換、時計巻戻しの状態修復は元のGameTestで維持する。

計算5件は一度JUnitへ移したが、普段から全件を回す運用と、`runGameTestServer...` 単独実行時の漏れを避けるため、GameTestへ戻した。毎回サーバーを起動する運用では、この切り出しで起動時間は減らない。別のテスト基盤は単体での高速な反復実行が必要になった時点で再検討し、GameTest件数だけを成果指標にしない。

## 表示と同期の保証範囲

`storedScrollsRemainVisibleWithoutMutatingSelection` は、無効枠・選択・Transcendence・Wisdomを表示データへ反映し、読み取りでItemStackを変更しないことを確認する。表示に関係する名前でも、保存資産と選択解決の検証として維持する。

`configSyncPreservesDisplayPercentagesAndResets` と `echoProfileSyncReplacesAndClearsClientIds` は、packetのCodec往復とclient用の共有状態への直接適用・resetを確認する。実通信、ログアウトイベントからresetへの接続、実際の画面描画の検証としては扱わない。

## 安定性の確認対象

カートリッジの再装填等は複数tickにまたがる状態遷移であり、一律の削除対象にしない。`reloadFeedbackFollowsServerTransitions` など、後のcallbackに後片付けを置くテストは、それ以前のassertion失敗でも `forget` 等が走る形へ改善する候補とする。既存batchの分離と、期限内に成立すべき完了条件も維持する。

同期処理内でconfigを変更して `finally` で戻すテストと、複数tickにわたって共有状態を保持するテストを区別する。共有configを使うことだけでflakyと断定しない。今回の棚卸しではGameTestを実行しておらず、新たなflakyの再現結果はない。

## 初回時点の着手順序

1. denylistの未登録シナリオとrecastの弱い成功判定を、実際の利用経路を通す検証へ改める。
2. 金の銃のnormalizationと保存復元、多目的ライフルのINSTANT発動の重複を、必要なassertionを移して統合する。
3. 銃と銀の魔法増幅器の属性検証、弾薬・薬莢検証を書き換える。
4. カートリッジ・盾・共有プロファイル同期の所属を整理する。純粋計算も独立したGameTestにまとめ、全件実行の入口をそろえる。

共通の検証意図はbackport候補だが、ServerPlayerの接続fixture、packetのCodec、装備・保存APIはtarget側の補正を伴う。この棚卸し自体は製品仕様を変更しない。

## 実施結果（2026-11-07）

製品コード・保存形式・MOD依存のバージョンは変更せず、テスト・関連文書を整理した。初回の94件は、同じ機能範囲を担当する92件のGameTestになった。全体のGameTest定義は1885件から1883件へ変わった。いずれも `required = false` の上流修正検知用1件を含む定義数であり、実際に連携処理を通った件数や必須テストだけの件数とは区別する。

| 対応 | 整理後の保証・配置 |
| --- | --- |
| denylist | 未登録の述語シナリオを削除し、`RifleDenylistGameTests` の2件で保存後の設定変更、実発動の拒否、資源非消費、設定復元後の成功、他方の設定との独立、スクロール保持を確認する |
| recast | 入力消費の戻り値に加え、弾薬なしの利用で残数が消費されることを確認する。後半のEventBus経由の弾薬非消費確認は維持する |
| 保存復元と発動の統合 | 金の銃はnormalization直後の抽出可否を保存復元テストへ移した。多目的の一発消費とCDは、ゼロマナからの実発動へ統合した |
| CD | 設定の非既定値、装備短縮、実効詠唱速度、EventBusの処理順とitem previewの接続を同じケースで確認する。既定値のgetter例とLONG収納テスト内の自己参照した期待値は削除した |
| 属性 | `SpellgunEquipmentAttributeGameTests` の4件へ移し、接続付きServerPlayerの装備更新後の値を確認する。6種のエンチャント、較正の着脱、両手の独立、Upgrade Orbの適用順・保存復元を維持する |
| 弾薬・薬莢 | `RifleAmmunitionGameTests` の2件で別弾種の拒否、実発動での一発消費、薬莢の在庫・ポーチへの返却、返却しない出目、弾薬不在時の非生成を確認する。出目を直接制御し、ポーチ装備による返却分岐の変化も通す |
| 静的tooltip | 弾・薬莢のキー全件照合と両ライフルの固定順序テストを削除した。銃共通の入力キー引数は代表1種へ縮小し、Epic Fight有無の警告、魔法種別、動的な引数・収納物の非変更は残した |
| 所属 | カートリッジ18件を `equipment.quickcastcartridge`、反射詠唱盾1件を `equipment.shield`、共有プロファイル同期1件を `equipment.multicastechostaff` へ移した。計算の分離後はカートリッジ19件、`equipment.spellgun` 自体は71件になった |
| 計算の分離 | CD境界1件を `SpellgunCooldownRulesGameTests`、フルオート反動2件と多目的反動1件を `SpellrifleRecoilGameTests`、回復時間の丸め・下限・飽和1件を `QuickcastCartridgeChargeTimeGameTests` に登録した。設定・イベント接続、較正、Capabilityの保存復元・clone・装備交換・時計修復は別のGameTestとして残した |
| 後片付け | カートリッジのplayer準備時に終了listenerを登録し、途中のassertion失敗・timeoutでも予約・再装填状態を破棄する |

両ライフルのrate limiterは実装と間隔の決まり方が異なるため、各実装を通す既存ケースを維持した。少量の準備コードの共通化は今回の必須作業にしなかった。

機能・計算のテストは各 `runGameTestServer...` で対応構成の一式を実行する。JUnitの依存・バージョン指定・実行設定と専用のMaven Central定義は削除した。PR CIは既存のgametest jobでこれらの計算テストも実行し、check名やworkflowは変更していない。

Curiosの装備検索には同じgame tick内のcacheがある。ポーチなしの検索後、同tickで装備を追加して収納する準備は避け、ポーチ経路は最初から装備済みの別playerで検証する。乱数を制御する匿名playerでは、ローカル変数名が継承フィールドに隠されないようにする。

前段の整理では `build`、JUnit5件、通常構成を含む9構成の必須テストが成功した。GameTestへ戻した後は `build` と通常構成のGameTest（1883定義）の必須成功を確認した。戻した5件はそれぞれ1回ずつ登録し、元の30個のassertionを維持している。optional MOD連携の実装・検証内容は今回変更していないため追加構成は再実行せず、incompatibles の既知の任意失敗の扱いも維持する。途中の失敗と追加実行の経緯は作業報告へ残す。

登録一覧を初回と比較し、追加2件、統合・削除6件、既存の混在ケースから計算だけを分離して追加した2件で、全体では2件減となる。3件の改名は機能の分離に伴うものであり、所属だけを移した20件は識別名と実行条件を維持した。
