package jp.aquafactory.apprenticecodex.item;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import io.redspace.ironsspellbooks.item.UniqueItem;
import jp.aquafactory.apprenticecodex.compat.malum.MalumCompatibility;
import jp.aquafactory.apprenticecodex.entity.ChargedTwinBladeStaffThrownEntity;
import jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffClientRenderState;
import jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffClientTooltip;
import jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellPayload;
import jp.aquafactory.apprenticecodex.renderer.item.ChargedTwinBladeStaffRenderer;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public final class ChargedTwinBladeStaff extends Item implements GeoItem, NonDamageableAnvilMergeItem, UniqueItem {
    public static final int THROW_THRESHOLD_TICKS = 10;
    private static final int USE_DURATION = 72000;
    private static final float THROW_POWER = 2.5F;
    private static final int THROW_MANA_COST = 100;
    private static final int RIPTIDE_MANA_COST = 50;
    private static final int ENCHANTMENT_VALUE = 15;
    private static final double BASE_PLAYER_ATTACK_DAMAGE = 1.0D;
    private static final double ATTACK_DAMAGE_BONUS = 10.0D;
    private static final double ATTACK_SPEED_BONUS = -3.0D;
    private static final double SPELL_POWER_BONUS = 0.10D;
    private static final ResourceLocation SPELL_POWER_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath("apprenticecodex", "charged_twin_blade_staff/spell_power");
    private static final ItemStack DURABILITY_ENCHANTMENT_PROBE_STACK = new ItemStack(Items.ELYTRA);
    private static final ItemStack SWORD_ENCHANTMENT_PROBE_STACK = new ItemStack(Items.DIAMOND_SWORD);
    private static final ItemStack TRIDENT_ENCHANTMENT_PROBE_STACK = new ItemStack(Items.TRIDENT);
    private static final Set<String> EXTRA_ENCHANTMENTS = Set.of(
            "apprenticecodex:wisdom"
    );
    private static final String MAIN_CONTROLLER = "main";
    private static final double USING_IDLE_ANIMATION_SPEED = 4.0D;
    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("idle");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ItemAttributeModifiers mainhandModifiers = buildMainhandModifiers();

    public ChargedTwinBladeStaff() {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.RARE)
                .fireResistant()
                .attributes(buildMainhandModifiers()));
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand usedHand) {
        var stack = player.getItemInHand(usedHand);
        if (usedHand != InteractionHand.MAIN_HAND) {
            return InteractionResultHolder.fail(stack);
        }

        player.startUsingItem(usedHand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(@NotNull ItemStack stack, @NotNull Level level, @NotNull LivingEntity livingEntity, int timeLeft) {
        if (!(livingEntity instanceof Player player)) {
            return;
        }

        var usedTicks = stack.getUseDuration(livingEntity) - timeLeft;
        if (usedTicks < THROW_THRESHOLD_TICKS) {
            return;
        }

        if (getRiptideLevel(stack) > 0) {
            handleRiptideRelease(level, player, stack);
            return;
        }

        if (level.isClientSide) {
            return;
        }

        if (!canSpendThrowMana(player, stack)) {
            return;
        }

        spendThrowMana(player, stack);
        var payload = hasChanneling(stack)
                ? ChargedTwinBladeStaffSpellPayload.capture(new SpellSelectionManager(player).getSelection(), player)
                : ChargedTwinBladeStaffSpellPayload.EMPTY;
        var projectile = new ChargedTwinBladeStaffThrownEntity(
                EntityRegistry.CHARGED_TWIN_BLADE_STAFF_THROWN.get(),
                level,
                player,
                stack.copy(),
                payload
        );
        projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, THROW_POWER, 1.0F);
        level.addFreshEntity(projectile);
        level.playSound(null, projectile, SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
        player.awardStat(Stats.ITEM_USED.get(this));
    }

    private static void handleRiptideRelease(Level level, Player player, ItemStack stack) {
        if (!canSpendRiptideMana(player)) {
            return;
        }

        if (!level.isClientSide) {
            spendRiptideMana(player);
        }

        player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
        var riptideLevel = getRiptideLevel(stack);
        var yRot = player.getYRot();
        var xRot = player.getXRot();
        var x = -Mth.sin(yRot * Mth.DEG_TO_RAD) * Mth.cos(xRot * Mth.DEG_TO_RAD);
        var y = -Mth.sin(xRot * Mth.DEG_TO_RAD);
        var z = Mth.cos(yRot * Mth.DEG_TO_RAD) * Mth.cos(xRot * Mth.DEG_TO_RAD);
        var distance = 3.0F * ((1.0F + riptideLevel) / 4.0F);
        var scale = distance / Mth.sqrt(x * x + y * y + z * z);
        player.push(x * scale, y * scale, z * scale);
        player.startAutoSpinAttack(20, 8.0F, stack);
        if (player.onGround()) {
            player.move(MoverType.SELF, new Vec3(0.0D, 1.1999999D, 0.0D));
        }

        var sound = resolveRiptideSound(riptideLevel);
        level.playSound(null, player, sound, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    @Override
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack stack) {
        return UseAnim.SPEAR;
    }

    @Override
    public int getUseDuration(@NotNull ItemStack stack, @NotNull LivingEntity entity) {
        return USE_DURATION;
    }

    @Override
    public boolean canAttackBlock(@NotNull BlockState state, @NotNull Level level, @NotNull net.minecraft.core.BlockPos pos, Player player) {
        return !player.isCreative();
    }

    @Override
    public boolean hurtEnemy(@NotNull ItemStack stack, @NotNull LivingEntity target, @NotNull LivingEntity attacker) {
        return true;
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility itemAbility) {
        return itemAbility == ItemAbilities.SWORD_SWEEP || super.canPerformAction(stack, itemAbility);
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        return mainhandModifiers;
    }

    @Override
    public boolean isEnchantable(@NotNull ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return ENCHANTMENT_VALUE;
    }

    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        if (super.supportsEnchantment(stack, enchantment)) {
            return true;
        }

        var enchantmentId = enchantment.unwrapKey().map(ResourceKey::location).orElse(null);
        if (enchantmentId == null) {
            return false;
        }

        if (DURABILITY_ENCHANTMENT_PROBE_STACK.supportsEnchantment(enchantment)) {
            return false;
        }

        if (MalumCompatibility.isMagicCapableWeaponEnchantment(stack, enchantmentId)) {
            return true;
        }

        if (MalumCompatibility.isSpiritPlunderSupported(stack, enchantmentId)) {
            return true;
        }

        return EXTRA_ENCHANTMENTS.contains(enchantmentId.toString())
                || SWORD_ENCHANTMENT_PROBE_STACK.supportsEnchantment(enchantment)
                || TRIDENT_ENCHANTMENT_PROBE_STACK.supportsEnchantment(enchantment);
    }

    @Override
    public boolean isPrimaryItemFor(ItemStack stack, Holder<Enchantment> enchantment) {
        return super.isPrimaryItemFor(stack, enchantment) || supportsEnchantment(stack, enchantment);
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        if (!super.isBookEnchantable(stack, book)) {
            return false;
        }

        var enchantments = EnchantmentHelper.getEnchantmentsForCrafting(book);
        return enchantments.isEmpty() || enchantments.keySet().stream()
                .allMatch(enchantment -> supportsEnchantment(stack, enchantment));
    }

    @Override
    public boolean isAnvilMergeEnchantmentAllowed(ItemStack stack, Holder<Enchantment> enchantment) {
        var enchantmentId = enchantment.unwrapKey().map(ResourceKey::location).orElse(null);
        if (enchantmentId == null) {
            return false;
        }
        if (DURABILITY_ENCHANTMENT_PROBE_STACK.supportsEnchantment(enchantment)) {
            return false;
        }
        if (MalumCompatibility.isMagicCapableWeaponEnchantment(stack, enchantmentId)) {
            return true;
        }
        if (MalumCompatibility.isSpiritPlunderSupported(stack, enchantmentId)) {
            return true;
        }
        return EXTRA_ENCHANTMENTS.contains(enchantmentId.toString())
                || SWORD_ENCHANTMENT_PROBE_STACK.supportsEnchantment(enchantment)
                || TRIDENT_ENCHANTMENT_PROBE_STACK.supportsEnchantment(enchantment);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context, @NotNull List<Component> lines, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);

        if (getRiptideLevel(stack) > 0) {
            lines.add(Component.translatable(
                    "item.apprenticecodex.charged_twin_blade_staff.desc.reptide",
                    Mth.ceil(RIPTIDE_MANA_COST)
            ).withStyle(ChatFormatting.AQUA));
        } else {
            lines.add(Component.translatable(
                    "item.apprenticecodex.charged_twin_blade_staff.desc.throwable",
                    Mth.ceil(getThrowManaCost(stack))
            ).withStyle(ChatFormatting.AQUA));
        }

        if (hasChanneling(stack)) {
            lines.add(Component.translatable("item.apprenticecodex.charged_twin_blade_staff.desc.channeling")
                    .withStyle(ChatFormatting.YELLOW));
            resolveUnsupportedSelectedSpellName().ifPresent(spellName ->
                    lines.add(Component.translatable(
                            "item.apprenticecodex.charged_twin_blade_staff.desc.channeling.not_supported",
                            spellName
                    ).withStyle(ChatFormatting.RED))
            );
        } else {
            lines.add(Component.translatable("item.apprenticecodex.charged_twin_blade_staff.desc.channeling.hint")
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private ChargedTwinBladeStaffRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (renderer == null) {
                    renderer = new ChargedTwinBladeStaffRenderer();
                }

                return renderer;
            }
        });
    }

    @Override
    public boolean isPerspectiveAware() {
        return true;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(this, MAIN_CONTROLLER, 0, state -> {
            var stack = state.getData(DataTickets.ITEMSTACK);
            var perspective = state.getData(DataTickets.ITEM_RENDER_PERSPECTIVE);
            state.setAnimation(ANIM_IDLE);
            state.getController().setAnimationSpeed(
                    ChargedTwinBladeStaffClientRenderState.shouldAccelerateIdle(stack, perspective)
                            ? USING_IDLE_ANIMATION_SPEED
                            : 1.0D
            );
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    public static double resolveThrownDamage(ItemStack stack) {
        return BASE_PLAYER_ATTACK_DAMAGE + ATTACK_DAMAGE_BONUS;
    }

    public static float resolveThrownDamage(ServerLevel level, ItemStack stack, Entity target, DamageSource damageSource) {
        // Item の攻撃力補正は素手基礎値を含まないため、投擲でも近接定義へ追従できるよう加算する。
        return EnchantmentHelper.modifyDamage(level, stack, target, damageSource, (float) resolveThrownDamage(stack));
    }

    private static boolean canSpendThrowMana(Player player, ItemStack stack) {
        return player.getAbilities().instabuild || getMagicData(player).getMana() >= getThrowManaCost(stack);
    }

    private static boolean canSpendRiptideMana(Player player) {
        return player.getAbilities().instabuild || getMagicData(player).getMana() >= RIPTIDE_MANA_COST;
    }

    private static void spendThrowMana(Player player, ItemStack stack) {
        if (player.getAbilities().instabuild) {
            return;
        }

        var magicData = getMagicData(player);
        magicData.setMana(Math.max(0.0F, magicData.getMana() - getThrowManaCost(stack)));
        syncMana(player, magicData);
    }

    private static void spendRiptideMana(Player player) {
        if (player.getAbilities().instabuild) {
            return;
        }

        var magicData = getMagicData(player);
        magicData.setMana(Math.max(0.0F, magicData.getMana() - RIPTIDE_MANA_COST));
        syncMana(player, magicData);
    }

    private static float getThrowManaCost(ItemStack stack) {
        var loyaltyLevel = getLoyaltyLevel(stack);
        return THROW_MANA_COST / (loyaltyLevel + 1.0F);
    }

    private static MagicData getMagicData(Player player) {
        return MagicData.getPlayerMagicData(player);
    }

    private static SoundEvent resolveRiptideSound(int riptideLevel) {
        return switch (riptideLevel) {
            case 3 -> SoundEvents.TRIDENT_RIPTIDE_3.value();
            case 2 -> SoundEvents.TRIDENT_RIPTIDE_2.value();
            default -> SoundEvents.TRIDENT_RIPTIDE_1.value();
        };
    }

    private static void syncMana(Player player, MagicData magicData) {
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new SyncManaPacket(magicData));
        }
    }

    private static Optional<Component> resolveUnsupportedSelectedSpellName() {
        return FMLEnvironment.dist == Dist.CLIENT
                ? ChargedTwinBladeStaffClientTooltip.resolveUnsupportedSpellName()
                : Optional.empty();
    }

    private static ItemAttributeModifiers buildMainhandModifiers() {
        var builder = ItemAttributeModifiers.builder();
        // Iron's の upgrade 処理は同 Attribute/Operation の既存補正 1 本だけを置換するため、基礎補正は重複させない。
        builder.add(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(
                        Item.BASE_ATTACK_DAMAGE_ID,
                        ATTACK_DAMAGE_BONUS,
                        AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
        );
        builder.add(
                Attributes.ATTACK_SPEED,
                new AttributeModifier(
                        Item.BASE_ATTACK_SPEED_ID,
                        ATTACK_SPEED_BONUS,
                        AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
        );
        builder.add(
                AttributeRegistry.SPELL_POWER,
                new AttributeModifier(
                        SPELL_POWER_MODIFIER_ID,
                        SPELL_POWER_BONUS,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                ),
                EquipmentSlotGroup.MAINHAND
        );
        return builder.build();
    }

    private static int getRiptideLevel(ItemStack stack) {
        return getEnchantmentLevel(stack, net.minecraft.world.item.enchantment.Enchantments.RIPTIDE);
    }

    private static int getLoyaltyLevel(ItemStack stack) {
        return getEnchantmentLevel(stack, net.minecraft.world.item.enchantment.Enchantments.LOYALTY);
    }

    private static boolean hasChanneling(ItemStack stack) {
        return getEnchantmentLevel(stack, net.minecraft.world.item.enchantment.Enchantments.CHANNELING) > 0;
    }

    private static int getEnchantmentLevel(ItemStack stack, ResourceKey<Enchantment> enchantmentKey) {
        for (var entry : EnchantmentHelper.getEnchantmentsForCrafting(stack).entrySet()) {
            if (entry.getKey().is(enchantmentKey)) {
                return entry.getIntValue();
            }
        }
        return 0;
    }
}
