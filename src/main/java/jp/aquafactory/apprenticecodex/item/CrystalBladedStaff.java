package jp.aquafactory.apprenticecodex.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.item.UniqueItem;
import jp.aquafactory.apprenticecodex.item.crystalbladedstaff.CrystalBladedStaffManaRecoveryManager;
import jp.aquafactory.apprenticecodex.item.crystalbladedstaff.CrystalBladedStaffManaRecoveryManager.PendingManaRecovery;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.ManaSiphonOrbEffectPacket;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.renderer.item.CrystalBladedStaffRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.world.phys.Vec3;

public class CrystalBladedStaff extends Item implements GeoItem, IPresetSpellContainer, UniqueItem, NonDamageableAnvilMergeItem {
    private static final String MAIN_CONTROLLER = "main";
    private static final String ACTIVATE_ANIMATION = "activate";
    private static final String VANILLA_NAMESPACE = "minecraft";
    private static final ResourceLocation FORGE_SHIELDS_TAG_ID = ResourceLocation.fromNamespaceAndPath("forge", "shields");
    private static final ResourceLocation FORGE_TOOLS_SHIELDS_TAG_ID =
            ResourceLocation.fromNamespaceAndPath("forge", "tools/shields");
    private static final Set<ResourceLocation> ALLOWED_MAGIC_ITEM_ENCHANTMENTS = Set.of(
            ResourceLocation.fromNamespaceAndPath("apprenticecodex", "transcendence"),
            ResourceLocation.fromNamespaceAndPath("apprenticecodex", "wisdom")
    );
    private static final ItemStack DURABILITY_ENCHANTMENT_PROBE_STACK = new ItemStack(Items.ELYTRA);
    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation ANIM_ACTIVATE = RawAnimation.begin().thenPlay("activate");
    private static final UUID ATTACK_DAMAGE_MODIFIER_ID = Item.BASE_ATTACK_DAMAGE_UUID;
    private static final UUID ATTACK_SPEED_MODIFIER_ID = Item.BASE_ATTACK_SPEED_UUID;
    private static final UUID ENTITY_REACH_MODIFIER_ID =
            UUID.nameUUIDFromBytes("apprenticecodex:crystal_bladed_staff/entity_reach".getBytes(StandardCharsets.UTF_8));
    private static final UUID SPELL_POWER_MODIFIER_ID =
            UUID.nameUUIDFromBytes("apprenticecodex:crystal_bladed_staff/spell_power".getBytes(StandardCharsets.UTF_8));
    private static final double ATTACK_DAMAGE = 4.0D;
    private static final double ATTACK_SPEED = -2.4D;
    private static final double ENTITY_REACH_BONUS = 0.5D;
    private static final double SPELL_POWER_BONUS = 0.10D;
    private static final int ENCHANTMENT_VALUE = 14;
    private static final int MIN_ORB_COUNT = 4;
    private static final int MAX_ORB_COUNT = 8;
    private static final int SPELL_ORB_COUNT = 4;
    private static final int MIN_RETURN_DELAY_TICKS = 20;
    private static final int MAX_RETURN_DELAY_TICKS = 30;
    private static final int MIN_RETURN_DURATION_TICKS = 4;
    private static final int MAX_RETURN_DURATION_TICKS = 6;
    private static final float MANA_RECOVERY_PER_ORB = 2.5f;
    // クライアント描画と同じ値で待機開始位置を求め、launch 音の位置ずれを防ぐ。
    private static final float ORB_SCATTER_DURATION_TICKS = 6.0f;
    private static final double ORB_SCATTER_GRAVITY = 0.012d;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Multimap<Attribute, AttributeModifier> mainhandModifiers;

    public CrystalBladedStaff() {
        super(new Properties().stacksTo(1).rarity(Rarity.RARE));
        this.mainhandModifiers = buildMainhandModifiers();
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public void initializeSpellContainer(ItemStack itemStack) {
        if (itemStack == null || ISpellContainer.isSpellContainer(itemStack)) {
            return;
        }

        var spellContainer = ISpellContainer.create(1, true, false).mutableCopy();
        spellContainer.addSpell(SpellRegistry.MANA_SLASH.get(), 1, true);
        ISpellContainer.set(itemStack, spellContainer.toImmutable());
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private CrystalBladedStaffRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new CrystalBladedStaffRenderer();
                }

                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(
                new AnimationController<>(this, MAIN_CONTROLLER, 0, state -> {
                    state.setAnimation(ANIM_IDLE);
                    return PlayState.CONTINUE;
                }).triggerableAnim(ACTIVATE_ANIMATION, ANIM_ACTIVATE)
        );
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> lines,
                                @NotNull TooltipFlag flag) {
        lines.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, level, lines, flag);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if (slot == EquipmentSlot.MAINHAND) {
            return mainhandModifiers;
        }

        return super.getAttributeModifiers(slot, stack);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand usedHand) {
        var stack = player.getItemInHand(usedHand);
        if (usedHand != InteractionHand.MAIN_HAND || shouldPrioritizeOffhandUse(player)) {
            return InteractionResultHolder.pass(stack);
        }

        var castResult = tryCastSelectedSpell(player, stack);
        return switch (castResult) {
            case SUCCESS -> InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            case FAIL -> InteractionResultHolder.fail(stack);
            case NONE -> InteractionResultHolder.pass(stack);
        };
    }

    @Override
    public boolean canAttackBlock(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, Player player) {
        return !player.isCreative();
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

        if (isDurabilityTargetEnchantment(enchantment)) {
            return false;
        }

        if (ALLOWED_MAGIC_ITEM_ENCHANTMENTS.contains(enchantmentId)) {
            return true;
        }

        if (VANILLA_NAMESPACE.equals(enchantmentId.getNamespace())) {
            return enchantment.canApplyAtEnchantingTable(new ItemStack(Items.DIAMOND_SWORD));
        }

        return enchantment.canApplyAtEnchantingTable(new ItemStack(Items.DIAMOND_SWORD));
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        if (!super.isBookEnchantable(stack, book)) {
            return false;
        }

        var enchantments = EnchantmentHelper.getEnchantments(book);
        if (enchantments.isEmpty()) {
            return true;
        }

        return enchantments.keySet().stream()
                .allMatch(enchantment -> canApplyAtEnchantingTable(stack, enchantment));
    }

    @Override
    public boolean isAnvilMergeEnchantmentAllowed(ItemStack stack, Enchantment enchantment) {
        return canApplyAtEnchantingTable(stack, enchantment);
    }

    @Override
    public boolean isValidRepairItem(@NotNull ItemStack toRepair, ItemStack repair) {
        return repair.is(Items.DIAMOND) || super.isValidRepairItem(toRepair, repair);
    }

    @Override
    public boolean hurtEnemy(@NotNull ItemStack stack, @NotNull LivingEntity target, @NotNull LivingEntity attacker) {
        return true;
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ToolAction toolAction) {
        return ToolActions.SWORD_SWEEP == toolAction || super.canPerformAction(stack, toolAction);
    }

    public static boolean isFullyChargedAttack(Player player) {
        return player.getAttackStrengthScale(0.5f) > 0.9f;
    }

    public static boolean isCrystalBladedStaff(ItemStack stack) {
        return stack.getItem() instanceof CrystalBladedStaff;
    }

    private static boolean shouldPrioritizeOffhandUse(Player player) {
        var offhandStack = player.getOffhandItem();
        return offhandStack.getItem() instanceof AbstractSpellGunItem || isShieldLikeOffhandItem(offhandStack);
    }

    private static boolean isShieldLikeOffhandItem(ItemStack stack) {
        // 継承元ではなく Forge の盾契約で判定し、ShieldItem 非継承の MOD 盾や
        // Shield Expansion のタグ拡張にも追従する。
        return stack.canPerformAction(ToolActions.SHIELD_BLOCK)
                || stack.is(ItemTags.create(FORGE_SHIELDS_TAG_ID))
                || stack.is(ItemTags.create(FORGE_TOOLS_SHIELDS_TAG_ID));
    }

    private CastResult tryCastSelectedSpell(Player player, ItemStack stack) {
        if (!ISpellContainer.isSpellContainer(stack)) {
            initializeSpellContainer(stack);
        }

        var selectionOption = new SpellSelectionManager(player).getSelection();
        if (selectionOption == null || selectionOption.spellData == SpellData.EMPTY) {
            return CastResult.NONE;
        }

        var spellData = selectionOption.spellData;
        var spell = spellData.getSpell();
        var spellLevel = spell.getLevelFor(spellData.getLevel(), player);
        var casted = spell.attemptInitiateCast(
                stack,
                spellLevel,
                player.level(),
                player,
                selectionOption.getCastSource(),
                true,
                SpellSelectionManager.MAINHAND
        );

        return casted ? CastResult.SUCCESS : CastResult.FAIL;
    }

    public static void spawnManaSiphonOrbs(ServerPlayer serverPlayer, Vec3 impactPosition, int totalHitMobCount) {
        spawnManaSiphonOrbBurst(serverPlayer, impactPosition, resolveOrbCount(serverPlayer.serverLevel(), totalHitMobCount));
    }

    public static void spawnManaSiphonOrbsForSpell(ServerPlayer serverPlayer, Vec3 impactPosition) {
        spawnManaSiphonOrbBurst(serverPlayer, impactPosition, SPELL_ORB_COUNT);
    }

    private static void spawnManaSiphonOrbBurst(ServerPlayer serverPlayer, Vec3 impactPosition, int orbCount) {
        var serverLevel = serverPlayer.serverLevel();
        var random = serverLevel.random;
        var orbData = new ArrayList<ManaSiphonOrbEffectPacket.OrbData>(orbCount);

        for (int i = 0; i < orbCount; i++) {
            var scatter = randomScatterVector(random.nextDouble(), random.nextDouble(), random.nextDouble());
            var returnDelayTicks = random.nextInt(MAX_RETURN_DELAY_TICKS - MIN_RETURN_DELAY_TICKS + 1) + MIN_RETURN_DELAY_TICKS;
            var returnDurationTicks = random.nextInt(MAX_RETURN_DURATION_TICKS - MIN_RETURN_DURATION_TICKS + 1) + MIN_RETURN_DURATION_TICKS;
            var scale = 0.18f + random.nextFloat() * 0.06f;
            var phaseOffset = random.nextFloat() * ((float) Math.PI * 2.0f);
            var launchPosition = launchStartPosition(impactPosition, scatter, returnDelayTicks, phaseOffset);
            orbData.add(new ManaSiphonOrbEffectPacket.OrbData(
                    (float) scatter.x,
                    (float) scatter.y,
                    (float) scatter.z,
                    returnDelayTicks,
                    returnDurationTicks,
                    scale,
                    phaseOffset
            ));
            CrystalBladedStaffManaRecoveryManager.submit(serverLevel, new PendingManaRecovery(
                    serverPlayer.getUUID(),
                    serverLevel.getGameTime() + returnDelayTicks + returnDurationTicks,
                    MANA_RECOVERY_PER_ORB
            ));
            CrystalBladedStaffManaRecoveryManager.submitLaunchSound(serverLevel,
                    new CrystalBladedStaffManaRecoveryManager.PendingLaunchSound(
                            launchPosition,
                            serverLevel.getGameTime() + returnDelayTicks
                    ));
        }

        triggerActivateAnimation(serverPlayer, serverPlayer.getMainHandItem());
        triggerActivateAnimation(serverPlayer, serverPlayer.getOffhandItem());

        Networks.sendToTrackingEntityAndSelf(serverPlayer, new ManaSiphonOrbEffectPacket(
                impactPosition,
                serverPlayer.getId(),
                orbData
        ));
    }

    private static int resolveOrbCount(net.minecraft.server.level.ServerLevel serverLevel, int totalHitMobCount) {
        var random = serverLevel.random;
        var baseOrbCount = random.nextInt(MAX_ORB_COUNT - MIN_ORB_COUNT + 1) + MIN_ORB_COUNT;
        var orbPenalty = Math.max(0, totalHitMobCount - 1);
        return Math.max(1, baseOrbCount - orbPenalty);
    }

    private static boolean isDurabilityTargetEnchantment(Enchantment enchantment) {
        return enchantment.canApplyAtEnchantingTable(DURABILITY_ENCHANTMENT_PROBE_STACK);
    }

    private static net.minecraft.world.phys.Vec3 randomScatterVector(double rx, double ry, double rz) {
        var horizontalAngle = rx * Math.PI * 2.0;
        var horizontalSpeed = 0.16 + ry * 0.14;
        var verticalSpeed = 0.12 + rz * 0.14;
        return new net.minecraft.world.phys.Vec3(
                Math.cos(horizontalAngle) * horizontalSpeed,
                verticalSpeed,
                Math.sin(horizontalAngle) * horizontalSpeed
        );
    }

    private static Vec3 launchStartPosition(Vec3 impactPosition, Vec3 scatter, int returnDelayTicks, float phaseOffset) {
        var anchor = impactPosition.add(
                scatter.scale(ORB_SCATTER_DURATION_TICKS)
        ).add(0.0d, -ORB_SCATTER_GRAVITY * ORB_SCATTER_DURATION_TICKS * ORB_SCATTER_DURATION_TICKS, 0.0d);
        var hoverTicks = returnDelayTicks - ORB_SCATTER_DURATION_TICKS;
        var phase = hoverTicks * 0.14f + phaseOffset;
        return anchor.add(
                Math.cos(phase * 0.9f) * 0.05d,
                Math.sin(phase * 1.3f) * 0.03d,
                Math.sin(phase) * 0.05d
        );
    }

    private static void triggerActivateAnimation(ServerPlayer serverPlayer, ItemStack stack) {
        if (!isCrystalBladedStaff(stack)) {
            return;
        }

        var instanceId = GeoItem.getOrAssignId(stack, serverPlayer.serverLevel());
        ((CrystalBladedStaff) stack.getItem()).triggerAnim(serverPlayer, instanceId, MAIN_CONTROLLER, ACTIVATE_ANIMATION);
    }

    private static Multimap<Attribute, AttributeModifier> buildMainhandModifiers() {
        var builder = ImmutableMultimap.<Attribute, AttributeModifier>builder();
        builder.put(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(
                        ATTACK_DAMAGE_MODIFIER_ID,
                        "Weapon modifier",
                        ATTACK_DAMAGE,
                        AttributeModifier.Operation.ADDITION
                )
        );
        builder.put(
                Attributes.ATTACK_SPEED,
                new AttributeModifier(
                        ATTACK_SPEED_MODIFIER_ID,
                        "Weapon modifier",
                        ATTACK_SPEED,
                        AttributeModifier.Operation.ADDITION
                )
        );
        builder.put(
                ForgeMod.ENTITY_REACH.get(),
                new AttributeModifier(
                        ENTITY_REACH_MODIFIER_ID,
                        "apprenticecodex.crystal_bladed_staff.entity_reach",
                        ENTITY_REACH_BONUS,
                        AttributeModifier.Operation.ADDITION
                )
        );
        builder.put(
                AttributeRegistry.SPELL_POWER.get(),
                new AttributeModifier(
                        SPELL_POWER_MODIFIER_ID,
                        "apprenticecodex.crystal_bladed_staff.spell_power",
                        SPELL_POWER_BONUS,
                        AttributeModifier.Operation.MULTIPLY_BASE
                )
        );
        return builder.build();
    }

    private enum CastResult {
        NONE,
        SUCCESS,
        FAIL
    }
}
