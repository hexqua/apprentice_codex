package jp.aquafactory.apprenticecodex.item.elementalbow;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.WeaponImbueCooldownHelper;
import jp.aquafactory.apprenticecodex.utility.PersistentGameTimeSanitizer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public final class ElementalBowOverheatManager {
    private static final String ROOT = "ApprenticeCodexElementalBowSharedOverheat";
    private ElementalBowOverheatManager() {}

    private static long now(Player player) {
        // 全ディメンションで同一の時計を使い、持ち替え・移動で過熱を逃がさない。
        return player.getServer() != null ? player.getServer().overworld().getGameTime() : player.level().getGameTime();
    }

    public static void cleanLegacyData(Player player) {
        player.getPersistentData().remove("ApprenticeCodexElementalBowOverheat");
        player.getPersistentData().remove("ApprenticeCodexElementalBowOverheatObserved");
    }

    public static int resolveCooldownTicks(AbstractSpell spell, Player player) {
        return resolveConfiguredOverheatTicks(WeaponImbueCooldownHelper.getEffectiveSpellCooldownWithoutSwordMultiplier(
                spell, player, CastSource.SWORD));
    }

    public static float getAdditionalManaCost(Player player, float baseManaCost) {
        var state = getState(player);
        if (!state.active() || baseManaCost <= 0) return 0;
        float n = state.chainDepth();
        return baseManaCost * (ApprenticeCodexServerConfig.elementalBowOverheatAdditionalManaLinearMultiplier() * n
                + ApprenticeCodexServerConfig.elementalBowOverheatAdditionalManaQuadraticMultiplier() * n * n);
    }

    public static void applyOverheatAfterCast(Player player, int cooldownTicks) {
        var state = getState(player);
        int ticks = resolveConfiguredOverheatTicks(cooldownTicks);
        long expires = Math.max(state.expireGameTime(), now(player) + ticks);
        if (expires <= now(player)) return;
        var tag = new CompoundTag();
        tag.putLong("ExpireGameTime", expires);
        tag.putInt("ChainDepth", state.chainDepth() == Integer.MAX_VALUE ? Integer.MAX_VALUE : state.chainDepth() + 1);
        tag.putInt("LastAppliedCooldownTicks", (int) Math.min(Integer.MAX_VALUE, expires - now(player)));
        player.getPersistentData().put(ROOT, tag);
        sync(player);
    }

    public static OverheatState getState(Player player) {
        var tag = player.getPersistentData().getCompound(ROOT);
        int depth = tag.getInt("ChainDepth");
        long expiry = PersistentGameTimeSanitizer.repairPersistedFutureUntil(now(player),
                tag.getLong("ExpireGameTime"), Math.max(0, tag.getInt("LastAppliedCooldownTicks")));
        if (depth <= 0 || expiry <= now(player)) {
            player.getPersistentData().remove(ROOT);
            return new OverheatState(0, 0);
        }
        tag.putLong("ExpireGameTime", expiry);
        return new OverheatState(depth, expiry);
    }

    public static float getCooldownOverlayRatio(Player player) {
        var state = getState(player);
        int total = player.getPersistentData().getCompound(ROOT).getInt("LastAppliedCooldownTicks");
        return !state.active() || total <= 0 ? 0 : Mth.clamp((float) (state.expireGameTime() - now(player)) / total, 0, 1);
    }

    public static void clear(Player player) {
        player.getPersistentData().remove(ROOT);
        sync(player);
    }

    public static CompoundTag createSyncTag(Player player) {
        getState(player);
        return player.getPersistentData().getCompound(ROOT).copy();
    }

    public static void applySyncedState(Player player, @Nullable CompoundTag tag) {
        if (tag == null || tag.isEmpty()) player.getPersistentData().remove(ROOT);
        else player.getPersistentData().put(ROOT, tag.copy());
    }

    private static int resolveConfiguredOverheatTicks(int ticks) {
        double scaled = Math.ceil(Math.max(0, ticks) * ApprenticeCodexServerConfig.elementalBowOverheatDurationMultiplier());
        long result = (long) Math.min(Integer.MAX_VALUE, scaled);
        result = Math.max(result, ApprenticeCodexServerConfig.elementalBowOverheatDurationMinTicks());
        int cap = ApprenticeCodexServerConfig.elementalBowOverheatDurationCapTicks();
        return (int) (cap > 0 ? Math.min(cap, result) : result);
    }

    private static void sync(Player player) {
        if (player instanceof ServerPlayer serverPlayer) ElementalBowOverheatSync.syncToClient(serverPlayer);
    }

    public record OverheatState(int chainDepth, long expireGameTime) {
        public boolean active() { return chainDepth > 0; }
    }
}
