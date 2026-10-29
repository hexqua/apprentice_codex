package jp.aquafactory.apprenticecodex.compat.ironsartifice;

import io.redspace.irons_artifice.api.BulletImpactEvent;
import io.redspace.irons_artifice.api.ComposeShotEvent;
import io.redspace.irons_artifice.damage.DamageSources;
import io.redspace.irons_artifice.data.ComponentType;
import io.redspace.irons_artifice.entity.Bullet;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.modifier.commencemagimodule.CommenceMagiModuleConfigState;
import jp.aquafactory.apprenticecodex.item.modifier.commencemagimodule.CommenceMagiModuleModifier;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class CommenceMagiModuleEvents {
    public static final ComponentType<Impact> IMPACT = new ComponentType<>(
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "commence_magi_module_impact"), () -> null);

    private CommenceMagiModuleEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onCompose(ComposeShotEvent event) {
        var profile = event.getShotProfile();
        int count = profile.peek(CommenceMagiModuleModifier.MODULE_COUNT);
        if (count == 0) return;
        var values = event.getEntity().level().isClientSide
                ? CommenceMagiModuleConfigState.values() : ApprenticeCodexServerConfig.commenceMagiModuleConfig();
        profile.components().set(CommenceMagiModuleModifier.HEADSHOT_MULTIPLIER, values.headshotMultiplier(count));
        profile.components().set(CommenceMagiModuleModifier.MANA_COST, values.manaCost());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onImpact(BulletImpactEvent event) {
        var bullet = event.getBullet();
        if (bullet.level().isClientSide) return;
        var components = bullet.getProfile().components();
        // 貫通・跳弾・キャンセルを含む全ての命中で前回の判定を破棄する。
        components.remove(IMPACT);
        if (event.isCanceled() || bullet.getProfile().peek(CommenceMagiModuleModifier.MODULE_COUNT) == 0) return;
        if (event.getRayTraceResult() instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity living) {
            components.set(IMPACT, new Impact(living, hit.getLocation(), CombatTools.isHeadShot(living, hit.getLocation()), false));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamage(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide || !event.getSource().is(DamageSources.BULLET_DAMAGE_TYPE)
                || !(event.getSource().getDirectEntity() instanceof Bullet bullet)) return;
        var impact = bullet.getProfile().peek(IMPACT);
        if (impact == null || impact.target() != event.getEntity() || impact.consumed()) return;
        // ShotProfile.copyは浅いコピー。DAMAGE自体を変更すると散弾や後続の貫通対象へ漏れる。
        bullet.getProfile().components().set(IMPACT, new Impact(impact.target(), impact.position(), impact.headshot(), true));
        if (impact.headshot()) {
            double amount = event.getAmount() * bullet.getProfile().peek(CommenceMagiModuleModifier.HEADSHOT_MULTIPLIER);
            event.setAmount((float) Math.min(amount, Float.MAX_VALUE));
        }
    }

    public record Impact(LivingEntity target, Vec3 position, boolean headshot, boolean consumed) {
    }

}
