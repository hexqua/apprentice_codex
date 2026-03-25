# Commit Shaping

## 基本原則

- 1 機能を、`cherry-pick` しやすい独立した連続コミット系列として保つ。
- 共通ロジック、Forge 固有接着コード、generated/resource 更新を同じコミットに押し込まない。
- 無関係な整形、rename、広域整理を機能コミットへ混ぜない。

## 推奨する切り方

1. 共通ロジック変更
2. Forge 固有接着コード変更
3. datagen / generated 更新
4. README / AGENTS / skill 更新

この順で分けられるなら、そのまま保つ。1 つにまとめないと成立しない場合だけ理由をコメントやコミットメッセージに残す。

## 避ける例

- registry 変更と import 整理を同じコミットに入れる。
- item 実装変更と大量の JSON rename を同じコミットに入れる。
- port と無関係なコメント整理を紛れ込ませる。

## レビュー時の確認

- どのコミットを `cherry-pick -x` するかを、SHA 単位で説明できるか。
- 1.21.1 側で要らない Forge 接着コードを、port 時に簡単に捨てられるか。
- generated の削除差分が追加差分に埋もれていないか。
