# Enchantment と修理の確認

Minecraft 1.21.1 の data-driven enchantment 定義や tag で表現された動作は、1.20.1 Forge に直接対応しない場合がある。

## Backport 方針

1. `main` の JSON、tag、テストからプレイヤーに見える意図を確認する。
2. その意図を 1.20.1 Forge の hook と item override で再実装する。
3. 次の動作を個別に確認する。
   - `canApplyAtEnchantingTable`
   - `isBookEnchantable`
   - `isValidRepairItem`
   - 関連する enchantable、repair material、compatibility tag
4. サーバーでテスト可能な動作には GameTest を追加または移植する。
5. 避けられない差分は記録し、暗黙に完全同一と扱わない。

JSON や tag を衝突なく `cherry-pick` できたことだけで、1.20.1 でも同じ動作になると判断しない。
