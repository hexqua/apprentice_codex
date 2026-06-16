package jp.aquafactory.apprenticecodex.spell.dualacrobat;

import io.redspace.ironsspellbooks.api.events.CounterSpellEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.AbstractSummonWeaponSpell;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class DualAcrobatCounterSpellEvent {
    private static final String COUNTERSPELL_INTERRUPTED_TAG = "ApprenticeCodexDualAcrobatCounterspellInterrupted";
    private static final double FALLBACK_WEAPON_SEARCH_RADIUS = 16.0D;

    private DualAcrobatCounterSpellEvent() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onCounterSpell(CounterSpellEvent event) {
        if (event.isCanceled()) {
            return;
        }

        if (!(event.target instanceof LivingEntity target)) {
            return;
        }

        var magicData = MagicData.getPlayerMagicData(target);
        if (magicData == null) {
            return;
        }

        var nearbyWeapons = getNearbySmgs(target);
        if (!isDualAcrobatCasting(target, magicData) && nearbyWeapons.isEmpty()) {
            return;
        }

        interruptCastingSmg(target, magicData, nearbyWeapons);
        target.getPersistentData().putBoolean(COUNTERSPELL_INTERRUPTED_TAG, true);
    }

    public static boolean consumeCounterspellInterrupted(LivingEntity entity) {
        var tag = entity.getPersistentData();
        var interrupted = tag.getBoolean(COUNTERSPELL_INTERRUPTED_TAG);
        tag.remove(COUNTERSPELL_INTERRUPTED_TAG);
        return interrupted;
    }

    private static boolean isDualAcrobatCasting(LivingEntity target, MagicData magicData) {
        if (SpellRegistry.DUAL_ACROBAT.get().getSpellId().equals(magicData.getCastingSpellId())) {
            return true;
        }

        if (!(target.level() instanceof ServerLevel serverLevel)) {
            return false;
        }

        return magicData.getAdditionalCastData() instanceof AbstractSummonWeaponSpell.SummonWeaponSpellCastData castData
                && castData.getEntity(serverLevel) instanceof DualAcrobatSmgEntity;
    }

    private static void interruptCastingSmg(LivingEntity target, MagicData magicData, List<DualAcrobatSmgEntity> nearbyWeapons) {
        if (!(target.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (magicData.getAdditionalCastData() instanceof AbstractSummonWeaponSpell.SummonWeaponSpellCastData castData
                && castData.getEntity(serverLevel) instanceof DualAcrobatSmgEntity weapon) {
            weapon.startCounterspellInterruptedShooting();
        }

        for (var weapon : nearbyWeapons) {
            weapon.startCounterspellInterruptedShooting();
        }
    }

    private static List<DualAcrobatSmgEntity> getNearbySmgs(LivingEntity target) {
        if (!(target.level() instanceof ServerLevel serverLevel)) {
            return List.of();
        }

        return serverLevel.getEntitiesOfClass(
                DualAcrobatSmgEntity.class,
                new AABB(target.position(), target.position()).inflate(FALLBACK_WEAPON_SEARCH_RADIUS),
                weapon -> !weapon.isRemoved()
        );
    }
}
