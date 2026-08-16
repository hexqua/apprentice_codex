# Better Combat の攻撃距離

## 方針

Better Combat の武器属性 JSON は、Minecraft 1.20.1 と 1.21.1 で攻撃距離の表現および実際の攻撃感が異なる。

- `1.20.1-main` は Better Combat 1.20.1 の絶対距離 `attack_range` を使う。
- `main`（Minecraft 1.21.1）は Better Combat 1.21.1 の加算値 `range_bonus` を使う。`attack_range` は同版で deprecated である。
- 両バージョンの値は、一定値の加算・減算による機械的な変換では対応しない。
- 各武器のモデル形状、全長、重量感、持ち方、および意図する使用感を基準に、各バージョンで近い攻撃表現となるよう個別に設定する。
- ただし、遠距離武器に関してはBetter Combat 1.21.1 においても`attack_range` を0として使用するデータが存在するため、`attack_range` が0のデータは1.21.1でも存在を許容する。

例えば、細身で短い武器は短い到達距離または素早い攻撃表現を、大型で重量感のある武器は広い到達距離または重い攻撃表現を検討する。ただし、これらは固定の数値規則ではなく、武器ごとの選定の目安である。

同じ数値、同じ Better Combat の武器種、または同じ実効距離に揃えること自体を目的とせず、見た目とゲーム内で意図する使用感の整合を優先する。

### バージョン間のプリセット差

Minecraftは1.21.1でメイスが追加され、Better Combatは1.21.1においてのみスマッシュ攻撃が行いやすいプリセットが作成されている。

このプリセットは1.20.1には存在しないため、Minecraftのメイスと似たような性質を持つ`SmashcastScepter`に関しては似たようなアニメーションになるように独自に対応している。

そのため、`SmashcastSecpter`は`main`においてはプリセット継承そのまま、`1.20.1-main`においてはプリセットを使わず独自対応する。

## ブランチ間の扱い

`src/main/resources/data/apprenticecodex/weapon_attributes` は手置き resource であり、datagen では管理しない。コンボ、アニメーション、攻撃判定、音、およびバージョンごとの攻撃表現を JSON のままレビューできることを優先する。

このディレクトリを `main` と `1.20.1-main` の間で取り込む場合、武器属性 JSON をそのまま cherry-pick してはならない。対象バージョンの Better Combat 仕様に合わせ、対象武器の定義を個別に確認して設定する。

距離値を調整する際は、少なくとも次を確認する。

- 対象武器の外見・想定される重量感・持ち方
- 親 preset の有無と、そのバージョンにおける挙動
- 同系統の既存武器との相対的な到達距離・攻撃感
- 通常時および Better Combat のコンボ中に意図しない過剰な到達距離にならないこと

Better Combat の標準 preset を親にしている武器は、各バージョンの preset をそのまま利用する。現時点では `SmashcastScepter` が `bettercombat:vanilla_mace` を、`SpellSideEdge` と `SpellSideEdgeMirror` が`bettercombat:dagger` を継承しており、独自の距離設定対象ではない。

## 検証

距離値、固有コンボ、親 preset を変更した場合は、`runGameTestServerBetterCombat` で属性が読み込まれることを確認する。

必要に応じてゲーム内でも、対象武器の通常攻撃およびコンボ中の到達距離・攻撃表現が、モデルと意図した使用感に合うことを確認する。標準的な判断から意図的に外れる設定を行う場合は、JSON の近くまたは変更記録に選定理由を残す。