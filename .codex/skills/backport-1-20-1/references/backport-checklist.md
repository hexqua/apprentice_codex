# 1.20.1 Backport チェックリスト

## Cherry-pick 前

- 移植元が `main` 上の非 `merge` コミットであることを確認する。
- 選択したコミット列が 1 つの機能または修正としてまとまっていることを確認する。
- 共通ロジック、NeoForge / 1.21.1 固有コード、resource / datagen、テスト、ドキュメントを分けて把握する。
- 同じ content ID とプレイヤーから見た動作を 1.20.1 でも維持できるか確認する。

## Cherry-pick 中

- `git cherry-pick -x` を使用する。
- 移植先の Forge 登録処理、event、capability、Java 17 制約を維持する。
- 1.21.1 の recipe 配置、NeoForge condition、data component、API をそのまま 1.20.1 へ持ち込まない。
- 履歴を確認しやすくなる場合は、プラットフォーム補正を別コミットに分ける。

## Cherry-pick 後

- 未解決の conflict marker と 1.21.1 固有の stale resource を検索する。
- generated resource が変わった場合は 1.20.1 側で datagen を行う。
- enchantment 適用、エンチャント本適用、修理素材、関連 tag を確認する。
- Java 17 の `build` と、影響範囲に必要な GameTest / optional MOD タスクを実行する。
- 実装動作を `main` と比較し、意図的に省いた要素や移植先固有差分を記録する。
