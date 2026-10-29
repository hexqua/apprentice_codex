package jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class PsychicDriveGauntletsCharge {
    public static final int MIN_TICKS = 5;
    public static final int MAX_TICKS = 20;
    public static final float MOVEMENT_FACTOR = 0.8F;
    public static final float FOV_FACTOR = 0.95F;
    private static final double PULL_BACK = 0.20;
    private static final double PULL_UP = 0.10;
    private static final double SHAKE = 0.01;

    private PsychicDriveGauntletsCharge() {}

    public static float amount(double ticks) {
        return (float) Mth.clamp(ticks / MAX_TICKS, 0, 1);
    }

    public static double range(float charge) { return PsychicDriveGauntletsRaycast.RANGE * (1 + charge); }

    public static Vec3 offset(double ticks) {
        var charge = amount(ticks);
        return new Vec3(Math.sin(ticks * 2) * SHAKE * charge,
                PULL_UP * charge + Math.cos(ticks * 2) * SHAKE * charge, PULL_BACK * charge);
    }

    public static float attackDamage(Player owner, ItemStack weapon) {
        var attribute = owner.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attribute == null) return 0;
        if (weapon == owner.getMainHandItem()) return (float) attribute.getValue();
        // 装備を一時交換すると外部MODの装備eventや属性cacheを汚すため、独立した属性で再計算する。
        var calculated = new AttributeInstance(Attributes.ATTACK_DAMAGE, ignored -> {});
        calculated.replaceFrom(attribute);
        for (var modifier : owner.getMainHandItem().getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE)) {
            calculated.removeModifier(modifier.getId());
        }
        for (var modifier : weapon.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE)) {
            calculated.removeModifier(modifier.getId());
            calculated.addTransientModifier(modifier);
        }
        return (float) calculated.getValue();
    }
}
