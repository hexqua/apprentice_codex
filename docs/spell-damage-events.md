# 魔法ダメージのイベント連携

魔法によるダメージには `CombatTools.applySpellDamage` を使い、発生元の `AbstractSpell` を渡す。装備経由で詠唱した魔法も対象とする。アイテム固有の攻撃・追加ダメージは、schoolや魔法属性を持つ場合も既存の `applyDamage` / `applyUnscaledDamage` を使う。

LivingEntityへの魔法攻撃は、部位解決・保護判定・反復詠唱補正の後、召喚補正・耐性適用・`hurt` の前にIron'sの `SpellDamageEvent` を一度通知する。キャンセルと変更後の量を実ダメージへ反映する。非LivingEntityには上流と同様にこのイベントを発火しない。

イベントと実際の攻撃には同じ `ApprenticeSpellDamageSource` を使う。魔法IDと戦闘所有者を公開しつつ、独自DamageType、タグ、位置、直接攻撃元、死亡メッセージを維持する。Iron'sの標準factoryはDamageTypeを系統のものに置換するため、そのまま置き換えない。反復詠唱補正は共通処理で適用済みのため、イベント購読側では重ねない。

Spell's Atlas 1.91.3のダメージ集計は `SpellDamageEvent`、魔法・系統別討伐集計は実際の `SpellDamageSource` を使う。イベント対応によって、集計以外の属性リアクションや威力変更にも接続される。ダメージ集計は実際の体力減少量ではなくイベント時点の量になる。Atlasへの直接依存は追加しない。

## Backport候補

- 共通部分: 魔法とアイテムの入口分離、魔法ID、独自DamageTypeの維持、呼び出し元の移行、イベント契約の回帰テスト。
- 接着部分: NeoForgeのイベント通知とGameTest登録。1.20.1ではForge側のAPIへ合わせ、DamageSourceのoverrideとIron's 3.xのAPIを再確認する。
- generated/resource、保存形式、content IDの変更はない。今回の作業では実際のbackportとJava 17検証は行わない。
