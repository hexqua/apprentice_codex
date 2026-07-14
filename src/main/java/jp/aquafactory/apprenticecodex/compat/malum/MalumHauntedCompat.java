package jp.aquafactory.apprenticecodex.compat.malum;

import com.google.common.collect.Multimap;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.item.AbstractRightClickMagicWeaponItem;
import jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaff;
import jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbow;
import jp.aquafactory.apprenticecodex.item.manaforceblade.ManaForceBlade;
import jp.aquafactory.apprenticecodex.item.MulticastEchoStaff;
import jp.aquafactory.apprenticecodex.item.PastelStaff;
import jp.aquafactory.apprenticecodex.item.ZenithStaff;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class MalumHauntedCompat {
    private static final String MALUM_MOD_ID = "malum";
    private static final String LODESTONE_MOD_ID = "lodestone";
    private static final ResourceLocation MALUM_HAUNTED =
            ResourceLocation.fromNamespaceAndPath(MALUM_MOD_ID, "haunted");
    private static final ResourceLocation MALUM_ANIMATED =
            ResourceLocation.fromNamespaceAndPath(MALUM_MOD_ID, "animated");
    private static final ResourceLocation LODESTONE_MAGIC_DAMAGE =
            ResourceLocation.fromNamespaceAndPath(LODESTONE_MOD_ID, "magic_damage");
    private static final ThreadLocal<Integer> RECURSION_DEPTH = ThreadLocal.withInitial(() -> 0);

    private MalumHauntedCompat() {
    }

    public static boolean isHauntedEnchantment(ResourceLocation enchantmentId) {
        return MALUM_HAUNTED.equals(enchantmentId);
    }

    public static boolean isAnimatedEnchantment(ResourceLocation enchantmentId) {
        return MALUM_ANIMATED.equals(enchantmentId);
    }

    public static boolean isSupportedHauntedMainhandItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        var item = stack.getItem();
        // FocusStaffbow も main hand の魔法武器として Haunted 追撃を共有する。
        return item instanceof PastelStaff
                || item instanceof MulticastEchoStaff
                || item instanceof ZenithStaff
                || item instanceof AbstractRightClickMagicWeaponItem
                || item instanceof FocusStaffbow
                || item instanceof ChargedTwinBladeStaff
                || item instanceof ManaForceBlade;
    }

    public static double resolveHauntedMagicDamageBonus(ItemStack stack) {
        if (!isSupportedHauntedMainhandItem(stack) || !isMalumRuntimeAvailable()) {
            return 0.0D;
        }

        var magicDamageAttribute = ForgeRegistries.ATTRIBUTES.getValue(LODESTONE_MAGIC_DAMAGE);
        if (magicDamageAttribute == null) {
            return 0.0D;
        }

        var event = new ItemAttributeModifierEvent(
                stack,
                EquipmentSlot.MAINHAND,
                stack.getItem().getAttributeModifiers(EquipmentSlot.MAINHAND, stack)
        );
        MinecraftForge.EVENT_BUS.post(event);
        return sumAttributeAmount(event.getModifiers(), magicDamageAttribute, AttributeModifier.Operation.ADDITION);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!isMalumRuntimeAvailable() || event.getEntity().level().isClientSide || event.getAmount() <= 0.0F) {
            return;
        }

        if (event.getEntity().isDeadOrDying() || RECURSION_DEPTH.get() > 0 || event.getSource().is(DamageTypes.HAUNTED_BONUS)) {
            return;
        }

        var attacker = resolveAttacker(event.getSource());
        if (attacker == null || !isDirectWeaponAttack(event.getSource(), attacker)) {
            return;
        }

        var mainHandStack = attacker.getMainHandItem();
        if (!isSupportedHauntedMainhandItem(mainHandStack)) {
            return;
        }

        var hauntedBonus = resolveHauntedMagicDamageBonus(mainHandStack);
        if (hauntedBonus <= 0.0D) {
            return;
        }

        // Malum 1.20.1 は Haunted の実ダメージ発火が大鎌経路前提のため、
        // main hand の独自魔法武器でも同じ属性値ぶんの魔法追撃をここで補う。
        // 専用 DamageType を使って I-Frame を無視し、死亡メッセージも区別できるようにする。
        RECURSION_DEPTH.set(RECURSION_DEPTH.get() + 1);
        try {
            event.getEntity().hurt(createHauntedBonusDamageSource(attacker), (float) hauntedBonus);
        } finally {
            RECURSION_DEPTH.set(Math.max(0, RECURSION_DEPTH.get() - 1));
        }
    }

    public static DamageSource createHauntedBonusDamageSource(LivingEntity attacker) {
        return CombatTools.getDamageSource(attacker.level(), attacker, attacker, DamageTypes.HAUNTED_BONUS);
    }

    private static boolean isMalumRuntimeAvailable() {
        return ModList.get().isLoaded(MALUM_MOD_ID) && ModList.get().isLoaded(LODESTONE_MOD_ID);
    }

    private static LivingEntity resolveAttacker(DamageSource source) {
        if (source.getEntity() instanceof LivingEntity livingEntity) {
            return livingEntity;
        }

        return source.getDirectEntity() instanceof LivingEntity livingEntity ? livingEntity : null;
    }

    private static boolean isDirectWeaponAttack(DamageSource source, LivingEntity attacker) {
        if (source.getEntity() != attacker || source.getDirectEntity() != attacker) {
            return false;
        }

        return source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK)
                || source.is(net.minecraft.world.damagesource.DamageTypes.MOB_ATTACK)
                || "player".equals(source.getMsgId());
    }

    private static double sumAttributeAmount(
            Multimap<Attribute, AttributeModifier> modifiers,
            Attribute attribute,
            AttributeModifier.Operation operation
    ) {
        return modifiers.get(attribute).stream()
                .filter(modifier -> modifier.getOperation() == operation)
                .mapToDouble(AttributeModifier::getAmount)
                .sum();
    }

    public static ResourceLocation hauntedEnchantmentId() {
        return MALUM_HAUNTED;
    }

    public static ResourceLocation animatedEnchantmentId() {
        return MALUM_ANIMATED;
    }

    public static ResourceLocation lodestoneMagicDamageId() {
        return LODESTONE_MAGIC_DAMAGE;
    }

    public static Enchantment getHauntedEnchantment() {
        return ForgeRegistries.ENCHANTMENTS.getValue(MALUM_HAUNTED);
    }
}
