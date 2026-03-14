package jp.aquafactory.apprenticecodex.item;

import io.redspace.ironsspellbooks.api.item.weapons.ExtendedSwordItem;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.item.UniqueItem;
import io.redspace.ironsspellbooks.item.weapons.StaffItem;
import io.redspace.ironsspellbooks.item.weapons.StaffTier;
import jp.aquafactory.apprenticecodex.item.crystalbladedstaff.CrystalBladedStaffManaRecoveryManager;
import jp.aquafactory.apprenticecodex.item.crystalbladedstaff.CrystalBladedStaffManaRecoveryManager.PendingLaunchSound;
import jp.aquafactory.apprenticecodex.item.crystalbladedstaff.CrystalBladedStaffManaRecoveryManager.PendingManaRecovery;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.ManaSiphonOrbEffectPacket;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Set;

public class CrystalBladedStaff extends StaffItem implements GeoItem, IPresetSpellContainer, UniqueItem {
    private static final String MAIN_CONTROLLER = "main";
    private static final String ACTIVATE_ANIMATION = "activate";
    private static final ResourceLocation ENTITY_REACH_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath("apprenticecodex", "crystal_bladed_staff_entity_reach");
    private static final ResourceLocation SPELL_POWER_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath("apprenticecodex", "crystal_bladed_staff_spell_power");
    private static final Set<ResourceLocation> EXTRA_SUPPORTED_ENCHANTMENTS = Set.of(
            ResourceLocation.fromNamespaceAndPath("apprenticecodex", "transcendence"),
            ResourceLocation.fromNamespaceAndPath("apprenticecodex", "wisdom")
    );
    private static final Set<ResourceLocation> EXCLUDED_ENCHANTMENTS = Set.of(
            ResourceLocation.withDefaultNamespace("fortune"),
            ResourceLocation.withDefaultNamespace("silk_touch")
    );
    private static final StaffTier WEAPON_TIER = new StaffTier(5.0F, -2.4F);
    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation ANIM_ACTIVATE = RawAnimation.begin().thenPlay("activate");
    private static final double ENTITY_REACH_BONUS = 1.0D;
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
    private final ItemAttributeModifiers mainhandModifiers;

    public CrystalBladedStaff() {
        super(new Properties()
                .stacksTo(1)
                .rarity(Rarity.RARE)
                .attributes(buildBaseAttributeModifiers()));
        this.mainhandModifiers = buildBaseAttributeModifiers();
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public boolean hasCustomRendering() {
        return true;
    }

    @Override
    public void initializeSpellContainer(ItemStack itemStack) {
        if (itemStack == null || ISpellContainer.isSpellContainer(itemStack)) {
            return;
        }

        // Datagen 時は SpellRegistry 未バインドのため、初期呪文の注入をスキップする。
        if (!SpellRegistry.MANA_SLASH.isBound()) {
            return;
        }

        var spellContainer = ISpellContainer.create(1, true, false).mutableCopy();
        spellContainer.addSpell(SpellRegistry.MANA_SLASH.get(), 1, true);
        ISpellContainer.set(itemStack, spellContainer.toImmutable());
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
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        return mainhandModifiers;
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
    public boolean canAttackBlock(@NotNull BlockState state, @NotNull Level level, @NotNull net.minecraft.core.BlockPos pos, Player player) {
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
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        var enchantmentId = enchantment.unwrapKey().map(key -> key.location()).orElse(null);
        if (enchantmentId != null && EXCLUDED_ENCHANTMENTS.contains(enchantmentId)) {
            return false;
        }

        if (super.supportsEnchantment(stack, enchantment)) {
            return true;
        }

        return enchantmentId != null && EXTRA_SUPPORTED_ENCHANTMENTS.contains(enchantmentId);
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
        if (enchantments.isEmpty()) {
            return true;
        }

        return enchantments.keySet().stream().allMatch(enchantment -> supportsEnchantment(stack, enchantment));
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
    public boolean canPerformAction(ItemStack stack, ItemAbility itemAbility) {
        return itemAbility == ItemAbilities.SWORD_SWEEP || super.canPerformAction(stack, itemAbility);
    }

    public static boolean isFullyChargedAttack(Player player) {
        return player.getAttackStrengthScale(0.5f) > 0.9f;
    }

    public static boolean isCrystalBladedStaff(ItemStack stack) {
        return stack.getItem() instanceof CrystalBladedStaff;
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
            CrystalBladedStaffManaRecoveryManager.submitLaunchSound(serverLevel, new PendingLaunchSound(
                    launchPosition,
                    serverLevel.getGameTime() + returnDelayTicks
            ));
        }

        triggerActivateAnimation(serverPlayer, serverPlayer.getMainHandItem());
        triggerActivateAnimation(serverPlayer, serverPlayer.getOffhandItem());
        Networks.sendToTrackingEntityAndSelf(serverPlayer, new ManaSiphonOrbEffectPacket(impactPosition, serverPlayer.getId(), orbData));
    }

    private static int resolveOrbCount(ServerLevel serverLevel, int totalHitMobCount) {
        var random = serverLevel.random;
        var baseOrbCount = random.nextInt(MAX_ORB_COUNT - MIN_ORB_COUNT + 1) + MIN_ORB_COUNT;
        var orbPenalty = Math.max(0, totalHitMobCount - 1);
        return Math.max(1, baseOrbCount - orbPenalty);
    }

    private static Vec3 randomScatterVector(double rx, double ry, double rz) {
        var horizontalAngle = rx * Math.PI * 2.0;
        var horizontalSpeed = 0.16 + ry * 0.14;
        var verticalSpeed = 0.12 + rz * 0.14;
        return new Vec3(
                Math.cos(horizontalAngle) * horizontalSpeed,
                verticalSpeed,
                Math.sin(horizontalAngle) * horizontalSpeed
        );
    }

    private static Vec3 launchStartPosition(Vec3 impactPosition, Vec3 scatter, int returnDelayTicks, float phaseOffset) {
        var anchor = impactPosition.add(scatter.scale(ORB_SCATTER_DURATION_TICKS))
                .add(0.0d, -ORB_SCATTER_GRAVITY * ORB_SCATTER_DURATION_TICKS * ORB_SCATTER_DURATION_TICKS, 0.0d);
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

    private static ItemAttributeModifiers buildBaseAttributeModifiers() {
        return buildAttributeModifiers(ExtendedSwordItem.createAttributes(WEAPON_TIER));
    }

    private static ItemAttributeModifiers buildAttributeModifiers(ItemAttributeModifiers baseModifiers) {
        var builder = ItemAttributeModifiers.builder();
        for (var entry : baseModifiers.modifiers()) {
            builder.add(entry.attribute(), entry.modifier(), entry.slot());
        }
        builder.add(
                Attributes.ENTITY_INTERACTION_RANGE,
                new AttributeModifier(ENTITY_REACH_MODIFIER_ID, ENTITY_REACH_BONUS, AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.MAINHAND
        );
        builder.add(
                AttributeRegistry.SPELL_POWER,
                new AttributeModifier(SPELL_POWER_MODIFIER_ID, SPELL_POWER_BONUS, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                EquipmentSlotGroup.MAINHAND
        );
        return builder.build();
    }

    private static boolean shouldPrioritizeOffhandUse(Player player) {
        var offhandItem = player.getOffhandItem().getItem();
        // 盾はクールダウン中でも常に優先し、Crystal 側の右クリック復帰を防ぐ。
        return offhandItem instanceof ShieldItem || offhandItem instanceof AbstractSpellGunItem;
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

    private enum CastResult {
        NONE,
        SUCCESS,
        FAIL
    }
}
