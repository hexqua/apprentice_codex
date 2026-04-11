# GitHub PR 保護設定（`1.21.1-main`）

このリポジトリでは `1.21.1-main` への反映を PR に統一し、`PR CI / build-and-gametest` が成功しない限りマージしない。

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

## 4. 受け入れ確認

1. 軽微な変更で `1.21.1-main` 向け PR を作成し、`PR CI / build-and-gametest` が自動起動することを確認する。
2. workflow を含む最初の PR では、required check 未設定の状態で CI 成功後に merge できることを確認する。
3. workflow 反映後に ruleset へ `PR CI / build-and-gametest` を required check として追加する。
4. 以後の PR では、この check が成功しない限り merge できないことを確認する。
