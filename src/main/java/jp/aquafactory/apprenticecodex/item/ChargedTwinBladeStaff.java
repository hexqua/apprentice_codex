package jp.aquafactory.apprenticecodex.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import io.redspace.ironsspellbooks.item.UniqueItem;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import jp.aquafactory.apprenticecodex.entity.ChargedTwinBladeStaffThrownEntity;
import jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffClientRenderState;
import jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffClientTooltip;
import jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellPayload;
import jp.aquafactory.apprenticecodex.renderer.item.ChargedTwinBladeStaffRenderer;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
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
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
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
    private static final ItemStack DURABILITY_ENCHANTMENT_PROBE_STACK = new ItemStack(Items.ELYTRA);
    private static final ItemStack SWORD_ENCHANTMENT_PROBE_STACK = new ItemStack(Items.DIAMOND_SWORD);
    private static final ItemStack TRIDENT_ENCHANTMENT_PROBE_STACK = new ItemStack(Items.TRIDENT);
    private static final Set<String> EXTRA_ENCHANTMENTS = Set.of(
            "apprenticecodex:wisdom",
            "apprenticecodex:transcendence"
    );
    private static final String MAIN_CONTROLLER = "main";
    private static final double USING_IDLE_ANIMATION_SPEED = 4.0D;
    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("idle");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Multimap<Attribute, AttributeModifier> mainhandModifiers = buildMainhandModifiers();

    public ChargedTwinBladeStaff() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
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

        var usedTicks = getUseDuration(stack) - timeLeft;
        if (usedTicks < THROW_THRESHOLD_TICKS) {
            return;
        }

        if (EnchantmentHelper.getRiptide(stack) > 0) {
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
        var payload = EnchantmentHelper.hasChanneling(stack)
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
        level.playSound(null, projectile, SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0F, 1.0F);
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
        var riptideLevel = EnchantmentHelper.getRiptide(stack);
        var yRot = player.getYRot();
        var xRot = player.getXRot();
        var x = -Mth.sin(yRot * Mth.DEG_TO_RAD) * Mth.cos(xRot * Mth.DEG_TO_RAD);
        var y = -Mth.sin(xRot * Mth.DEG_TO_RAD);
        var z = Mth.cos(yRot * Mth.DEG_TO_RAD) * Mth.cos(xRot * Mth.DEG_TO_RAD);
        var distance = 3.0F * ((1.0F + riptideLevel) / 4.0F);
        var scale = distance / Mth.sqrt(x * x + y * y + z * z);
        player.push(x * scale, y * scale, z * scale);
        player.startAutoSpinAttack(20);
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
    public int getUseDuration(@NotNull ItemStack stack) {
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
    public boolean canPerformAction(ItemStack stack, ToolAction toolAction) {
        return ToolActions.SWORD_SWEEP == toolAction || super.canPerformAction(stack, toolAction);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        return slot == EquipmentSlot.MAINHAND ? mainhandModifiers : super.getAttributeModifiers(slot, stack);
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
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        var enchantmentId = ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
        if (enchantmentId == null) {
            return false;
        }

        if (enchantment.canApplyAtEnchantingTable(DURABILITY_ENCHANTMENT_PROBE_STACK)) {
            return false;
        }

        return EXTRA_ENCHANTMENTS.contains(enchantmentId.toString())
                || enchantment.canApplyAtEnchantingTable(SWORD_ENCHANTMENT_PROBE_STACK)
                || enchantment.canApplyAtEnchantingTable(TRIDENT_ENCHANTMENT_PROBE_STACK);
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        if (!super.isBookEnchantable(stack, book)) {
            return false;
        }

        var enchantments = EnchantmentHelper.getEnchantments(book);
        return enchantments.isEmpty() || enchantments.keySet().stream()
                .allMatch(enchantment -> canApplyAtEnchantingTable(stack, enchantment));
    }

    @Override
    public boolean isAnvilMergeEnchantmentAllowed(ItemStack stack, Enchantment enchantment) {
        return canApplyAtEnchantingTable(stack, enchantment);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> lines, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, level, lines, flag);

        if (EnchantmentHelper.getRiptide(stack) > 0) {
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

        if (EnchantmentHelper.hasChanneling(stack)) {
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
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private ChargedTwinBladeStaffRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
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
        return resolveThrownDamage(stack, MobType.UNDEFINED);
    }

    public static double resolveThrownDamage(ItemStack stack, MobType mobType) {
        // Item の攻撃力補正は素手基礎値を含まないため、投擲でも近接定義へ追従できるよう加算する。
        return BASE_PLAYER_ATTACK_DAMAGE + ATTACK_DAMAGE_BONUS + EnchantmentHelper.getDamageBonus(stack, mobType);
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
        var loyaltyLevel = EnchantmentHelper.getLoyalty(stack);
        return THROW_MANA_COST / (loyaltyLevel + 1.0F);
    }

    private static MagicData getMagicData(Player player) {
        return MagicData.getPlayerMagicData(player);
    }

    private static SoundEvent resolveRiptideSound(int riptideLevel) {
        return switch (riptideLevel) {
            case 3 -> SoundEvents.TRIDENT_RIPTIDE_3;
            case 2 -> SoundEvents.TRIDENT_RIPTIDE_2;
            default -> SoundEvents.TRIDENT_RIPTIDE_1;
        };
    }

    private static void syncMana(Player player, MagicData magicData) {
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new SyncManaPacket(magicData));
        }
    }

    private static Optional<Component> resolveUnsupportedSelectedSpellName() {
        var result = DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> ChargedTwinBladeStaffClientTooltip::resolveUnsupportedSpellName);
        return Optional.ofNullable(result).orElseGet(Optional::empty);
    }

    private static Multimap<Attribute, AttributeModifier> buildMainhandModifiers() {
        var builder = ImmutableMultimap.<Attribute, AttributeModifier>builder();
        builder.put(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(
                        Item.BASE_ATTACK_DAMAGE_UUID,
                        "Weapon modifier",
                        ATTACK_DAMAGE_BONUS,
                        AttributeModifier.Operation.ADDITION
                )
        );
        builder.put(
                Attributes.ATTACK_SPEED,
                new AttributeModifier(
                        Item.BASE_ATTACK_SPEED_UUID,
                        "Weapon modifier",
                        ATTACK_SPEED_BONUS,
                        AttributeModifier.Operation.ADDITION
                )
        );
        return builder.build();
    }
}
