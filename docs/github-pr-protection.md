# GitHub PR 保護設定

このリポジトリでは `main` への反映を PR に統一し、GitHub Actions の check run `build-and-gametest` が成功しない限りマージしない。Codex Cloud のスマートトリガーレビューは PR レビュー補助として使い、CI と人間の判断を置き換えない。

## 1. Actions セキュリティ設定

GitHub の `Settings` -> `Actions` -> `General` で次を設定する。

- `Actions permissions`: `Allow select actions and reusable workflows`
- GitHub 所有 action は許可する
- 明示許可する追加 action は `gradle/actions@50e97c2cd7a37755bbfafc9c5b7cafaece252f6e` のみとする
- `Workflow permissions`: `Read repository contents permission`
- `Allow GitHub Actions to create and approve pull requests`: `Off`
- `Require actions to be pinned to a full-length commit SHA`: `On`
- `Fork pull request workflows from outside collaborators`: 初回実行を承認制にできる設定がある場合は有効化する
- self-hosted runner を使わない場合は、repository / organization 側で self-hosted runner を未設定のまま維持する

補足:

- このリポジトリの CI は secrets を使わない。
- workflow は `pull_request` のみで動かし、`pull_request_target` は使わない。
- `gradle/actions/wrapper-validation` を先に実行し、PR から差し替えられた wrapper をそのまま叩かない。

## 2. `main` の Ruleset / Branch protection

GitHub の `Settings` -> `Rules` -> `Rulesets` で `main-pr-ci-protection` を作る。旧 UI の branch protection でもよいが、設定内容は同じにそろえる。

- Target branches: `main`
- Restrict deletions: `On`
- Block force pushes: `On`
- Require a pull request before merging: `On`
- Require status checks to pass before merging: `On`
- Required status checks:
  - `build-and-gametest`（GitHub Actions）
- Require branches to be up to date before merging: `On`

補足:

- GitHub の画面上では workflow 名込みで `PR CI / build-and-gametest` と表示されることがあるが、ruleset には GitHub Actions の check run `build-and-gametest` を登録する。

必要に応じて次も有効化する。

- Dismiss stale pull request approvals when new commits are pushed
- Require conversation resolution before merging

## 3. Codex Cloud スマートトリガーレビュー

- PR 作成後、Codex Cloud のスマートトリガーレビューを走らせる。
- 指摘が出た場合は、修正するか、見送る理由を PR 上で明示する。
- スマートトリガーレビューは必須 CI の代替にしない。`build-and-gametest` と人間のレビューを最終判断に使う。
- 将来、Codex Cloud 側で安定した check 名を required status check にできる状態になった場合のみ、Ruleset への追加を検討する。

## 4. Merge 運用

- `main` への直接 push は行わず、バージョン更新を含めて PR で流す。
- push / PR 作成前にローカルレビューを行う。
- このリポジトリでは通常変更は `Create a merge commit` を使う。
- バージョン更新 PR だけは、`main` 上の見た目を直積みに近づけるため `Rebase merge` を使ってよい。
- `Squash merge` は無効化し、`Rebase merge` は有効のまま残す。

## 5. 受け入れ確認

1. 軽微な変更で PR を作成し、`PR CI / build-and-gametest` の workflow が自動起動し、`build-and-gametest` check run が記録されることを確認する。
2. 意図的に GameTest を落とした PR で required check failure により merge できないことを確認する。
3. 修正 push 後に同じ check 名で再実行され、成功時のみ merge 可能になることを確認する。
4. `main` への直接 push が拒否されることを確認する。
5. Codex Cloud のスマートトリガーレビューが PR 上で確認でき、指摘の対応状況を追えることを確認する。
