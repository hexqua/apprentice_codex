# Generated resource の整理

`cherry-pick` が衝突なく適用できても、`main` の生成物をそのまま有効な 1.20.1 の出力とは扱わない。

## 検出対象になりやすい 1.21.1 の生成物

- 1.20.1 の `recipes` ではなく `data/<namespace>/recipe` にある recipe。
- `neoforge:conditions` など NeoForge 固有の condition。
- 1.20.1 で利用できない data component field や item stack の表現。
- Minecraft バージョン間で配置が変わった tag、registry、provider 出力。
- `main` で改名または削除された後も、移植先に stale output として残るファイル。

## 整理手順

1. 移植元コミットが触れる generated resource と手置き resource をすべて特定する。
2. 1.21.1 でのみ有効な出力を除去する。
3. resource を再生成する前に、移植先の datagen provider を補正する。
4. 必要な場合は 1.20.1 の datagen タスクを実行する。
5. 追加・変更・削除ファイルを一緒に確認し、移植先の生成結果を正とする。
6. 通常の build と関連する GameTest を実行する。
