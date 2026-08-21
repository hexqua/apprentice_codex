package jp.aquafactory.apprenticecodex.spell.linearbuild;

import jp.aquafactory.apprenticecodex.config.spell.LinearBuildServerConfig;

public final class LinearBuildConfigState {
    private static int manaCostPerBlock = LinearBuildServerConfig.DEFAULT_MANA_COST_PER_BLOCK;

    private LinearBuildConfigState() {
    }

    public static int manaCostPerBlock() {
        return manaCostPerBlock;
    }

    public static void set(int manaCostPerBlock) {
        LinearBuildConfigState.manaCostPerBlock = Math.max(
                LinearBuildServerConfig.MIN_MANA_COST_PER_BLOCK,
                Math.min(LinearBuildServerConfig.MAX_MANA_COST_PER_BLOCK, manaCostPerBlock)
        );
    }

    public static void reset() {
        set(LinearBuildServerConfig.DEFAULT_MANA_COST_PER_BLOCK);
    }
}
