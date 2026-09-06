package jp.aquafactory.apprenticecodex.compat.malum;

import com.sammy.malum.common.entity.scythe.LocalizedMaelstromEntity;
import com.sammy.malum.registry.common.MalumDamageTypes;
import com.sammy.malum.registry.common.item.MalumItems;
import jp.aquafactory.apprenticecodex.item.spellreaperscythe.ScytheThrowEntity;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import team.lodestar.lodestone.helpers.CurioHelper;
import team.lodestar.lodestone.helpers.DamageTypeHelper;

public final class MalumScytheMaelstromCompat {
    public static final String ORIGIN_MARKER = "apprenticecodex_scythe_maelstrom";
    private MalumScytheMaelstromCompat() {}

    public static boolean hasRing(LivingEntity owner) {
        return CurioHelper.hasCurioEquipped(owner, MalumItems.RING_OF_THE_HOWLING_MAELSTROM.get());
    }

    public static void tick(ScytheThrowEntity scythe) {
        if (scythe.level().isClientSide) {
            MalumScytheMaelstromClient.tick(scythe);
            return;
        }
        if (!(scythe.level() instanceof ServerLevel level) || level.getGameTime() % 5 != 0
                || !(scythe.getOwner() instanceof LivingEntity owner)) return;
        // 本家handleMaelstromは専用Entity型の移動とreturnTimerまで変更するため、ダメージ部分だけを接続する。
        // 周期・倍率・無敵時間解除・damage typeはMalum 1.8.2と同じ。対象選別は独自投擲に揃える。
        var bounds = com.sammy.malum.registry.common.entity.MalumEntities.SCYTHE_BOOMERANG.get()
                .getDimensions().makeBoundingBox(scythe.position()).inflate(2);
        for (var target : level.getEntities(scythe, bounds,
                entity -> CombatTools.isValidCombatTarget(entity, owner))) {
            target.invulnerableTime = 0;
            var source = DamageTypeHelper.create(level, MalumDamageTypes.SCYTHE_MAELSTROM, scythe, owner);
            boolean success = target.hurt(source, scythe.getPhysicalDamage() * 0.2f);
            if (success && target instanceof LivingEntity living && !living.isDeadOrDying() && scythe.getMagicDamage() > 0) {
                target.invulnerableTime = 0;
                target.hurt(DamageTypeHelper.create(level, MalumDamageTypes.VOODOO, scythe, owner), scythe.getMagicDamage() * 0.2f);
            }
        }
    }

    public static void place(ScytheThrowEntity scythe, Entity target) {
        var storm = new LocalizedMaelstromEntity(scythe.level(), target.getX(), target.getY() + target.getBbHeight() * 0.75, target.getZ());
        storm.setData(scythe.getOwner(), scythe.getPhysicalDamage(), scythe.getMagicDamage(), 0, 40);
        storm.setItem(scythe.getWeaponSnapshot());
        storm.getPersistentData().putBoolean(ORIGIN_MARKER, true);
        scythe.level().addFreshEntity(storm);
    }
}
