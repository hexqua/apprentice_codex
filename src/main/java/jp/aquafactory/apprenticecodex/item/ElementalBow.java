package jp.aquafactory.apprenticecodex.item;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowModeManager;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowModeManager.ResolvedDefinition;
import jp.aquafactory.apprenticecodex.particle.AdditiveGlowParticleOptions;
import jp.aquafactory.apprenticecodex.registry.ParticleRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.ForgeEventFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ElementalBow extends BowItem implements IPresetSpellContainer, ArcaneAnvilImbueBlockItem {
    public static final int READY_DRAW_TICKS = 22;
    private static final String MODE_TAG = "ElementalBowMode";
    private static final ItemStack ENCHANTMENT_PROBE_STACK = new ItemStack(Items.BOW);
    private static final float MANA_SAFE_MARGIN = 0.001F;
    private static final float PARTICLE_SIZE = 0.12F;
    private static final int PARTICLE_WHITEN_TICKS = 2;

    public ElementalBow() {
        super(new Properties().durability(384));
    }

    public static boolean isElementalSpell(@Nullable AbstractSpell spell) {
        return ElementalBowModeManager.isElementalSpell(spell);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand usedHand) {
        var stack = player.getItemInHand(usedHand);
        initializeSpellContainer(stack);
        if (player.isSecondaryUseActive()) {
            if (!level.isClientSide) {
                var nextModeId = resolveNextModeId(stack);
                setMode(stack, nextModeId);
                player.displayClientMessage(
                        Component.translatable(
                                        "ui.apprenticecodex.elemental_bow.mode_switched",
                                        getModeDisplayName(stack)
                                )
                                .withStyle(ChatFormatting.GOLD),
                        true
                );
                level.playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.35F, 1.1F);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        var mode = normalizeModeState(stack);
        if (mode == null) {
            var projectile = player.getProjectile(stack);
            if (!projectile.isEmpty() || !hasInfinity(stack) || player.getAbilities().instabuild) {
                return super.use(level, player, usedHand);
            }

            var nockResult = ForgeEventFactory.onArrowNock(stack, level, player, usedHand, true);
            if (nockResult != null) {
                return nockResult;
            }

            player.startUsingItem(usedHand);
            return InteractionResultHolder.consume(stack);
        }

        var ammoSource = resolveAmmoSource(player, stack);
        var canFireWithoutAmmo = player.getAbilities().instabuild || hasInfinity(stack);
        if (ammoSource == null && !canFireWithoutAmmo) {
            return InteractionResultHolder.fail(stack);
        }

        var profile = resolveSpellProfile(stack, mode);
        if (profile == null) {
            return InteractionResultHolder.fail(stack);
        }

        var requiredMana = profile.spell().getManaCost(profile.spellLevel());
        if (!player.getAbilities().instabuild) {
            var magicData = MagicData.getPlayerMagicData(player);
            if (magicData == null || magicData.getMana() + MANA_SAFE_MARGIN < requiredMana) {
                if (!level.isClientSide) {
                    player.displayClientMessage(createInsufficientManaMessage(profile.spell(), player), true);
                }
                return InteractionResultHolder.fail(stack);
            }
        }

        var nockResult = ForgeEventFactory.onArrowNock(stack, level, player, usedHand, ammoSource != null || canFireWithoutAmmo);
        if (nockResult != null) {
            return nockResult;
        }

        player.startUsingItem(usedHand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void initializeSpellContainer(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        var mode = normalizeModeState(stack);
        var profile = resolveSpellProfile(stack, mode);
        if (profile == null) {
            ISpellContainer.remove(stack);
            return;
        }

        var spellContainer = ISpellContainer.get(stack);
        if (spellContainer != null) {
            var spellData = spellContainer.getSpellAtIndex(0);
            if (spellData != SpellData.EMPTY
                    && spellData.getSpell() == profile.spell()
                    && spellData.getLevel() == profile.spellLevel()
                    && spellData.isLocked()
                    && spellContainer.getMaxSpellCount() == 1
                    && !spellContainer.isSpellWheel()) {
                return;
            }
        }

        // モード由来の spell 情報は tooltip と外部参照先で共有したいが、
        // 通常の spell wheel へは流さない。
        var mutable = ISpellContainer.create(1, false, false).mutableCopy();
        mutable.addSpellAtIndex(profile.spell(), profile.spellLevel(), 0, true);
        ISpellContainer.set(stack, mutable.toImmutable());
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        initializeSpellContainer(stack);
    }

    @Override
    public void onUseTick(@NotNull Level level, @NotNull LivingEntity entity, @NotNull ItemStack stack, int remainingUseDuration) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        var mode = normalizeModeState(stack);
        if (mode == null) {
            return;
        }

        var drawDuration = getUseDuration(stack) - remainingUseDuration;
        if (drawDuration <= 0 || drawDuration >= READY_DRAW_TICKS || drawDuration % 2 != 0) {
            return;
        }

        spawnChargeParticles(serverLevel, entity, mode);
    }

    @Override
    public void releaseUsing(@NotNull ItemStack stack, @NotNull Level level, @NotNull LivingEntity livingEntity, int timeLeft) {
        if (!(livingEntity instanceof Player player)) {
            return;
        }
        initializeSpellContainer(stack);

        var mode = normalizeModeState(stack);
        if (mode == null) {
            var projectile = player.getProjectile(stack);
            if (!projectile.isEmpty() || !hasInfinity(stack) || player.getAbilities().instabuild) {
                super.releaseUsing(stack, level, livingEntity, timeLeft);
                return;
            }

            releaseVanillaInfinityShot(stack, level, player, timeLeft);
            return;
        }

        releaseElementalShot(stack, level, player, timeLeft, mode);
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return Items.BOW.canApplyAtEnchantingTable(ENCHANTMENT_PROBE_STACK, enchantment);
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return Items.BOW.isBookEnchantable(ENCHANTMENT_PROBE_STACK, book);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> lines, @NotNull TooltipFlag flag) {
        initializeSpellContainer(stack);
        super.appendHoverText(stack, level, lines, flag);
        lines.add(
                Component.translatable("item.apprenticecodex.elemental_bow.mode", getModeDisplayName(stack))
                        .withStyle(ChatFormatting.GRAY)
        );
    }

    private void releaseVanillaInfinityShot(ItemStack stack, Level level, Player player, int timeLeft) {
        var drawDuration = getUseDuration(stack) - timeLeft;
        drawDuration = ForgeEventFactory.onArrowLoose(stack, level, player, drawDuration, true);
        if (drawDuration < 0) {
            return;
        }

        var power = getPowerForTime(drawDuration);
        if (power < 0.1F) {
            return;
        }

        fireVanillaArrow(level, player, stack, new ItemStack(Items.ARROW), power, true);
    }

    private void releaseElementalShot(ItemStack stack, Level level, Player player, int timeLeft, ResolvedDefinition mode) {
        var ammoSource = resolveAmmoSource(player, stack);
        var canFireWithoutAmmo = player.getAbilities().instabuild || hasInfinity(stack);
        var drawDuration = getUseDuration(stack) - timeLeft;
        drawDuration = ForgeEventFactory.onArrowLoose(stack, level, player, drawDuration, ammoSource != null || canFireWithoutAmmo);
        if (drawDuration < READY_DRAW_TICKS) {
            return;
        }

        // 属性ショットは server 側でのみ最終判定と詠唱を行う。
        // client 側でも同じ再判定を通すと、server が先にマナを消費した直後の同期値を見て
        // 「現在マナ 0」の不足表示だけが二重に出ることがある。
        if (level.isClientSide) {
            return;
        }

        var profile = resolveSpellProfile(stack, mode);
        if (profile == null) {
            return;
        }

        if (!player.getAbilities().instabuild) {
            if (ammoSource == null && !hasInfinity(stack)) {
                return;
            }

            var magicData = MagicData.getPlayerMagicData(player);
            var requiredMana = profile.spell().getManaCost(profile.spellLevel());
            if (magicData == null || magicData.getMana() + MANA_SAFE_MARGIN < requiredMana) {
                player.displayClientMessage(createInsufficientManaMessage(profile.spell(), player), true);
                return;
            }
        }

        if (!castElementalSpell(player, stack, profile)) {
            return;
        }

        if (!player.getAbilities().instabuild) {
            stack.hurtAndBreak(1, player, bowUser -> bowUser.broadcastBreakEvent(player.getUsedItemHand()));
            if (ammoSource != null && !hasInfinity(stack)) {
                consumeAmmo(player, ammoSource);
            }
        }

        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ARROW_SHOOT,
                SoundSource.PLAYERS,
                1.0F,
                1.0F / (player.getRandom().nextFloat() * 0.4F + 1.2F) + 0.35F
        );
        player.awardStat(Stats.ITEM_USED.get(this));
    }

    private boolean castElementalSpell(Player player, ItemStack stack, SpellCastProfile profile) {
        var slotId = player.getUsedItemHand() == InteractionHand.OFF_HAND
                ? SpellSelectionManager.OFFHAND
                : SpellSelectionManager.MAINHAND;
        var magicData = MagicData.getPlayerMagicData(player);
        var casted = profile.spell().attemptInitiateCast(
                stack,
                profile.spellLevel(),
                player.level(),
                player,
                CastSource.SWORD,
                true,
                slotId
        );
        if (!casted) {
            return false;
        }

        TriggeredSpellCastHelper.applyLongCastDurationOverride(
                player,
                profile.spellLevel(),
                profile.spell(),
                magicData,
                slotId,
                0
        );
        return true;
    }

    private void fireVanillaArrow(Level level, Player player, ItemStack bowStack, ItemStack ammoStack, float power, boolean infiniteAmmo) {
        if (!level.isClientSide) {
            var arrowItem = ammoStack.getItem() instanceof ArrowItem arrow ? arrow : (ArrowItem) Items.ARROW;
            var arrow = arrowItem.createArrow(level, ammoStack, player);
            arrow = customArrow(arrow);
            arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, power * 3.0F, 1.0F);
            if (power == 1.0F) {
                arrow.setCritArrow(true);
            }

            var powerLevel = bowStack.getEnchantmentLevel(Enchantments.POWER_ARROWS);
            if (powerLevel > 0) {
                arrow.setBaseDamage(arrow.getBaseDamage() + (double) powerLevel * 0.5D + 0.5D);
            }

            var punchLevel = bowStack.getEnchantmentLevel(Enchantments.PUNCH_ARROWS);
            if (punchLevel > 0) {
                arrow.setKnockback(punchLevel);
            }

            if (bowStack.getEnchantmentLevel(Enchantments.FLAMING_ARROWS) > 0) {
                arrow.setSecondsOnFire(100);
            }

            bowStack.hurtAndBreak(1, player, bowUser -> bowUser.broadcastBreakEvent(player.getUsedItemHand()));
            if (infiniteAmmo) {
                arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
            }

            level.addFreshEntity(arrow);
        }

        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ARROW_SHOOT,
                SoundSource.PLAYERS,
                1.0F,
                1.0F / (player.getRandom().nextFloat() * 0.4F + 1.2F) + power * 0.5F
        );
        player.awardStat(Stats.ITEM_USED.get(this));
    }

    private void spawnChargeParticles(ServerLevel level, LivingEntity entity, ResolvedDefinition mode) {
        var color = mode.color();
        var red = ((color >> 16) & 0xFF) / 255.0F;
        var green = ((color >> 8) & 0xFF) / 255.0F;
        var blue = (color & 0xFF) / 255.0F;
        var look = entity.getLookAngle();
        var side = look.cross(new net.minecraft.world.phys.Vec3(0.0D, 1.0D, 0.0D));
        if (side.lengthSqr() < 0.0001D) {
            side = new net.minecraft.world.phys.Vec3(1.0D, 0.0D, 0.0D);
        }
        side = side.normalize().scale(0.16D);
        var base = entity.getEyePosition().add(look.scale(0.45D)).add(0.0D, -0.18D, 0.0D);

        level.sendParticles(
                new AdditiveGlowParticleOptions(ParticleRegistry.ADDITIVE_SPARK.get(), PARTICLE_SIZE, red, green, blue, PARTICLE_WHITEN_TICKS),
                base.x + side.x,
                base.y + side.y,
                base.z + side.z,
                1,
                0.01D,
                0.01D,
                0.01D,
                0.0D
        );
        level.sendParticles(
                new AdditiveGlowParticleOptions(ParticleRegistry.ADDITIVE_SPARK.get(), PARTICLE_SIZE, red, green, blue, PARTICLE_WHITEN_TICKS),
                base.x - side.x,
                base.y - side.y,
                base.z - side.z,
                1,
                0.01D,
                0.01D,
                0.01D,
                0.0D
        );
    }

    @Nullable
    private SpellCastProfile resolveSpellProfile(ItemStack stack, @Nullable ResolvedDefinition mode) {
        if (mode == null) {
            return null;
        }
        return new SpellCastProfile(mode.spell(), mode.resolveSpellLevel(stack));
    }

    @Nullable
    public static DisplayedSpellProfile getDisplayedSpellProfile(ItemStack stack) {
        if (!(stack.getItem() instanceof ElementalBow elementalBow)) {
            return null;
        }

        var profile = elementalBow.resolveSpellProfile(stack, elementalBow.resolveConfiguredMode(stack));
        if (profile == null) {
            return null;
        }
        return new DisplayedSpellProfile(profile.spell(), profile.spellLevel());
    }

    public static Component createInsufficientManaMessage(AbstractSpell spell, @Nullable Player caster) {
        return Component.translatable("ui.irons_spellbooks.cast_error_mana", spell.getDisplayName(caster))
                .withStyle(ChatFormatting.RED);
    }

    @Nullable
    private ItemStack resolveAmmoSource(Player player, ItemStack bowStack) {
        // 外部矢筒系は格納仕様/API が mod ごとに揺れやすいため、現時点ではまず vanilla の矢取得面を固定する。
        // Supplementaries / Relics の個別連携は、1 本消費経路を安全に確定できた時点で別差分に分離する。
        var projectile = player.getProjectile(bowStack);
        return projectile.isEmpty() ? null : projectile;
    }

    private void consumeAmmo(Player player, ItemStack ammoStack) {
        ammoStack.shrink(1);
    }

    private static boolean hasInfinity(ItemStack stack) {
        return stack.getEnchantmentLevel(Enchantments.INFINITY_ARROWS) > 0;
    }

    @Nullable
    private ResolvedDefinition resolveConfiguredMode(ItemStack stack) {
        return ElementalBowModeManager.getResolvedDefinition(getStoredModeId(stack));
    }

    @Nullable
    private ResolvedDefinition normalizeModeState(ItemStack stack) {
        var tag = stack.getTag();
        if (tag == null || !tag.contains(MODE_TAG)) {
            return null;
        }

        var storedModeId = ResourceLocation.tryParse(tag.getString(MODE_TAG));
        var resolvedMode = ElementalBowModeManager.getResolvedDefinition(storedModeId);
        if (resolvedMode != null) {
            return resolvedMode;
        }

        clearStoredMode(stack);
        return null;
    }

    @Nullable
    private static ResourceLocation getStoredModeId(ItemStack stack) {
        var tag = stack.getTag();
        if (tag == null || !tag.contains(MODE_TAG)) {
            return null;
        }
        return ResourceLocation.tryParse(tag.getString(MODE_TAG));
    }

    @Nullable
    private static ResourceLocation resolveNextModeId(ItemStack stack) {
        var resolvedDefinitions = ElementalBowModeManager.getResolvedDefinitions();
        if (resolvedDefinitions.isEmpty()) {
            return null;
        }

        var currentModeId = getStoredModeId(stack);
        if (currentModeId == null) {
            return resolvedDefinitions.get(0).schoolId();
        }

        for (int index = 0; index < resolvedDefinitions.size(); index++) {
            if (!resolvedDefinitions.get(index).schoolId().equals(currentModeId)) {
                continue;
            }
            return index + 1 < resolvedDefinitions.size() ? resolvedDefinitions.get(index + 1).schoolId() : null;
        }

        return resolvedDefinitions.get(0).schoolId();
    }

    private static void setMode(ItemStack stack, @Nullable ResourceLocation modeId) {
        if (modeId == null) {
            clearStoredMode(stack);
        } else {
            stack.getOrCreateTag().putString(MODE_TAG, modeId.toString());
        }
        if (stack.getItem() instanceof ElementalBow elementalBow) {
            elementalBow.initializeSpellContainer(stack);
        }
    }

    private static void clearStoredMode(ItemStack stack) {
        var tag = stack.getTag();
        if (tag == null) {
            return;
        }
        tag.remove(MODE_TAG);
        if (tag.isEmpty()) {
            stack.setTag(null);
        }
    }

    private Component getModeDisplayName(ItemStack stack) {
        var mode = resolveConfiguredMode(stack);
        return mode != null ? mode.schoolType().getDisplayName() : Component.translatable("item.apprenticecodex.elemental_bow.mode.none");
    }

    private record SpellCastProfile(AbstractSpell spell, int spellLevel) {
    }

    public record DisplayedSpellProfile(AbstractSpell spell, int spellLevel) {
    }
}
