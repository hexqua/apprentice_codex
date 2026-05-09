# GitHub PR 保護設定（`1.21.1-main`）

このリポジトリでは `1.21.1-main` への反映を PR に統一し、`PR CI / build-and-gametest` が成功しない限りマージしない。Codex Cloud のスマートトリガーレビューは PR レビュー補助として使い、CI と人間の判断を置き換えない。

## 1. Actions セキュリティ設定

GitHub の `Settings` -> `Actions` -> `General` で次を設定する。

- `Actions permissions`: `Allow select actions and reusable workflows`
- GitHub 所有 action は許可する
- 明示許可する追加 action は `gradle/actions/wrapper-validation@50e97c2cd7a37755bbfafc9c5b7cafaece252f6e` のみとする
- `Workflow permissions`: `Read repository contents permission`
- `Allow GitHub Actions to create and approve pull requests`: `Off`
- `Require actions to be pinned to a full-length commit SHA`: `On`
- self-hosted runner は使わない

補足:

- この CI は secrets を使わない。
- workflow は `pull_request` のみで動かし、`pull_request_target` は使わない。
- `gradle/actions/wrapper-validation` を先に実行し、PR から差し替えられた wrapper をそのまま叩かない。

## 2. bootstrap 手順

target branch に workflow が存在しない段階で required check を先に有効化すると、`Expected — Waiting for status to be reported` のまま詰まることがある。`1.21.1-main` では次の順序を必ず守る。

1. `1.21.1-main` 向け workflow を含む PR を作る
2. required status check はまだ有効化しない
3. その PR で `PR CI / build-and-gametest` が 1 回成功することを確認する
4. PR を `Create a merge commit` で `1.21.1-main` に取り込む
5. `1.21.1-main` 上に `.github/workflows/pr-ci.yml` が存在することを確認する
6. その後で ruleset に `PR CI / build-and-gametest` を required check として追加する

## 3. `1.21.1-main` の Ruleset

GitHub の `Settings` -> `Rules` -> `Rulesets` で `1-21-1-main-pr-ci-protection` を作成する。

- Target branches: `1.21.1-main`
- Restrict deletions: `On`
- Block force pushes: `On`
- Require a pull request before merging: `On`
- Allowed merge methods: `merge`, `rebase`
- Required status checks:
  - `PR CI / build-and-gametest`
- Require branches to be up to date before merging: `On`

補足:

- `Squash and merge` は repository settings 側で無効化する。
- 通常変更は `Create a merge commit` を使う。
- バージョン更新 PR だけ `Rebase and merge` を使ってよい。

必要に応じて次も有効化する。

- Dismiss stale pull request approvals when new commits are pushed
- Require conversation resolution before merging

## 4. Codex Cloud スマートトリガーレビュー

- PR 作成後、Codex Cloud のスマートトリガーレビューを走らせる。
- 指摘が出た場合は、修正するか、見送る理由を PR 上で明示する。
- スマートトリガーレビューは必須 CI の代替にしない。`PR CI / build-and-gametest` と人間のレビューを最終判断に使う。
- 将来、Codex Cloud 側で安定した check 名を required status check にできる状態になった場合のみ、Ruleset への追加を検討する。

## 5. Merge 運用

- `1.21.1-main` への直接 push は行わず、バージョン更新を含めて PR で流す。
- このリポジトリでは通常変更は `Create a merge commit` を使う。
- バージョン更新 PR だけは、`1.21.1-main` 上の見た目を直積みに近づけるため `Rebase merge` を使ってよい。
- `Squash merge` は無効化し、`Rebase merge` は有効のまま残す。

## 6. 受け入れ確認

1. 軽微な変更で `1.21.1-main` 向け PR を作成し、`PR CI / build-and-gametest` が自動起動することを確認する。
2. workflow を含む最初の PR では、required check 未設定の状態で CI 成功後に merge できることを確認する。
3. workflow 反映後に ruleset へ `PR CI / build-and-gametest` を required check として追加する。
4. 以後の PR では、この check が成功しない限り merge できないことを確認する。
5. merge 可否まで確認したい場合は、無害な docs 変更を使った検証 PR を 1 本だけ merge してもよい。
6. Codex Cloud のスマートトリガーレビューが PR 上で確認でき、指摘の対応状況を追えることを確認する。
