# Resource Diff Hygiene

## 前提

- `src/generated/resources` の差分は、port 先で stale 出力や旧書式残留を見抜ける形で残す必要がある。
- `src/main/resources/data` の手置き JSON は datagen 管理外なので、削除・移設・改名の意図が差分に見えていることが重要。

## 実装時の扱い

- resource の削除・改名・移設は、追加差分と同時に行っても削除差分が追える単位でコミットを分ける。
- datagen の出力変更を伴う場合は、Java 実装変更と generated 更新を分離できるか先に考える。
- 旧 path から新 path へ移す場合は、コメントやコミットメッセージで「何が旧扱いか」を明示する。

## build 前の確認

- `git diff --name-status` で `D` と rename を確認し、意図しない残留がないことを確認する。
- recipe、tag、loot、advancement など配置規約を持つ JSON は、対象バージョンで path が変わらないかを確認する。
- build 出力に stale file が混ざりやすい変更では、ソース差分だけでなく jar に残り得る旧配置も意識する。

## コメントを残すべき場面

- 外部 MOD や vanilla の loader/version 差分で path 選定が変わるとき。
- datagen では消せない手置き JSON を別管理しているとき。
- 1.21.1 側で別 path や別書式へ移す予定が見えているとき。
