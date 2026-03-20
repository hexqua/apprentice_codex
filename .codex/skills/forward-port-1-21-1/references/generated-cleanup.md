# Generated Cleanup Guide

## 前提

- `src/generated/resources/.cache` は Git 管理外なので、branch 切替・`cherry-pick`・手動コピーで混入した古い JSON は `runData` だけでは消えないことがある。
- forward-port では「生成し直せば整う」と決めつけない。
- `src/main/resources/data` に置いた手製の datapack JSON は datagen 管理外なので、配置移行や削除漏れを別途確認する必要がある。

## 削除範囲の決め方

1. 通常は変更範囲のディレクトリだけを削除する。
2. 対象種別単位で判断したい場合は `src/generated/resources/data/apprenticecodex/<対象種別>` まで広げる。
3. 出力パス変更や旧書式移行が広範囲に及ぶ場合だけ `src/generated/resources/data/apprenticecodex` まで広げる。

## runData 後の確認

- `git diff --name-status -- src/generated/resources` を確認し、不要 JSON が削除差分になっていることを確認する。
- 必要に応じて `rg -n "forge:conditions|canApplyAtEnchantingTable|isBookEnchantable|supportsEnchantment|isValidRepairItem" src/generated/resources` を実行し、旧前提の残留を確認する。

## 手置き JSON の確認

- custom recipe など `src/main/resources/data` に置いたファイルは、対象バージョンの配置規約を確認する。1.21.1 側で `data/<modid>/recipe/` を使っているのに、旧版由来の `data/<modid>/recipes/` を残すとロードされないことがある。
- 手置き JSON を移動・削除した後は、`build/resources/main/data/<modid>` 側に旧配置が残っていないか確認する。Gradle の増分処理で stale output が残ると、ソースでは消えていても jar に再混入する。
