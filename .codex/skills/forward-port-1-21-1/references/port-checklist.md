# Forward-Port Checklist

## 標準手順

1. `main` に入った変更から、取り込み候補の非 `merge` コミットを列挙する。
2. 実際に取り込む SHA を確定する。`merge` コミット自体は取り込まない。
3. `git cherry-pick -x <sha1> [<sha2> ...]` で取り込む。
4. コンフリクト時は `1.21.1-main` の既存差分を優先しつつ、必要最小限の手動調整だけを行う。
5. datagen 対象に削除・改名・出力パス変更・旧書式移行が含まれる場合は、`runData` 前に影響ディレクトリの generated JSON を削除する。
6. `./gradlew.bat runData` を実行し、1.21.1 の現行実装で生成物を上書きする。
7. `git diff --name-status -- src/generated/resources` を確認し、不要 JSON が削除差分として出ていることを確認する。
8. `src/main/resources/data` に手置きの custom recipe などがある場合は、1.21.1 側の配置規約を確認する。特に recipe 系は `recipes/` のままではなく `recipe/` へ揃える必要がないかを先に確認する。
9. 手置き JSON を移動・削除した場合は、`build/resources/main/data` に旧配置が残っていないことを確認する。必要なら旧ディレクトリを掃除してから jar を作り直す。
10. 必要に応じて `rg -n "forge:conditions|canApplyAtEnchantingTable|isBookEnchantable|supportsEnchantment|isValidRepairItem" src/generated/resources src/main/resources` を実行し、1.20.1 由来の旧前提が残っていないことを確認する。
11. `./gradlew.bat build` を実行し、取り込み後の検証成功を確認する。

## 実務ルール

- 1 機能を独立した連続コミット系列として保ち、`cherry-pick` しやすくする。
- `main` 側で無関係な整形・rename が混ざっている場合は、取り込み前に対象を絞り直す。
- 同種コンフリクトの再解決コストを下げるため、`git config rerere.enabled true` を推奨する。
- `1.21.1-main` のみで必要になった修正は、`main` へ逆取り込みが必要かを別途判断する。

## 読み分け

- 装備、武器、Curios、特殊アイテムが含まれる場合は `enchant-repair.md` も読む。
- generated の削除範囲に迷う場合は `generated-cleanup.md` を読む。
