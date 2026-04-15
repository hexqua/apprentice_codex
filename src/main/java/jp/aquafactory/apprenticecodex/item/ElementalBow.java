package jp.aquafactory.apprenticecodex.item;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.util.MinecraftInstanceHelper;
import io.redspace.ironsspellbooks.util.TooltipsUtils;
import jp.aquafactory.apprenticecodex.particle.AdditiveGlowParticleOptions;
import jp.aquafactory.apprenticecodex.registry.ParticleRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
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
    private static final int FIRE_COLOR = 0xFF8A2A;
    private static final int ENDER_COLOR = 0xCC55FF;
    private static final int NATURE_COLOR = 0x68D66A;

    public ElementalBow() {
        super(new Properties().durability(384));
    }

    public static boolean isElementalSpell(@Nullable AbstractSpell spell) {
        return spell == io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_ARROW_SPELL.get()
                || spell == io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_ARROW_SPELL.get()
                || spell == io.redspace.ironsspellbooks.api.registry.SpellRegistry.POISON_ARROW_SPELL.get();
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand usedHand) {
        var stack = player.getItemInHand(usedHand);
        initializeSpellContainer(stack);
        if (player.isSecondaryUseActive()) {
            if (!level.isClientSide) {
                var nextMode = getMode(stack).next();
                setMode(stack, nextMode);
                player.displayClientMessage(
                        Component.translatable(
                                        "ui.apprenticecodex.elemental_bow.mode_switched",
                                        nextMode.getDisplayName()
                                )
                                .withStyle(ChatFormatting.GOLD),
                        true
                );
                level.playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.35F, 1.1F);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        var mode = getMode(stack);
        if (mode == ElementalMode.NONE) {
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

        var profile = resolveSpellProfile(stack, getMode(stack));
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
                    && spellContainer.getMaxSpellCount() == 1) {
                return;
            }
        }

        // mode と弓エンチャントから導出した疑似 Imbue を実 container に反映し、
        // tooltip と外部連携先が同じ spell 情報を見るようにする。
        ISpellContainer.createImbuedContainer(profile.spell(), profile.spellLevel(), stack);
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

        var mode = getMode(stack);
        if (mode == ElementalMode.NONE) {
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

        var mode = getMode(stack);
        if (mode == ElementalMode.NONE) {
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
        super.appendHoverText(stack, level, lines, flag);
        initializeSpellContainer(stack);
        lines.add(
                Component.translatable(
                                "item.apprenticecodex.elemental_bow.mode",
                                getMode(stack).getDisplayName()
                        )
                        .withStyle(ChatFormatting.GRAY)
        );
        lines.addAll(buildElementalSpellTooltipSection(stack));
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

    private void releaseElementalShot(ItemStack stack, Level level, Player player, int timeLeft, ElementalMode mode) {
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

    private void spawnChargeParticles(ServerLevel level, LivingEntity entity, ElementalMode mode) {
        var color = mode.getSparkColor();
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
    private SpellCastProfile resolveSpellProfile(ItemStack stack, ElementalMode mode) {
        if (mode == ElementalMode.NONE) {
            return null;
        }

        var spell = switch (mode) {
            case FIRE -> io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_ARROW_SPELL.get();
            case ENDER -> io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_ARROW_SPELL.get();
            case NATURE -> io.redspace.ironsspellbooks.api.registry.SpellRegistry.POISON_ARROW_SPELL.get();
            case NONE -> null;
        };

        var spellLevel = 1 + stack.getEnchantmentLevel(Enchantments.POWER_ARROWS);
        if (mode == ElementalMode.FIRE && stack.getEnchantmentLevel(Enchantments.FLAMING_ARROWS) > 0) {
            spellLevel += 2;
        }
        spellLevel = Mth.clamp(spellLevel, spell.getMinLevel(), spell.getMaxLevel());
        return new SpellCastProfile(mode, spell, spellLevel);
    }

    @Nullable
    public static DisplayedSpellProfile getDisplayedSpellProfile(ItemStack stack) {
        if (!(stack.getItem() instanceof ElementalBow elementalBow)) {
            return null;
        }

        var profile = elementalBow.resolveSpellProfile(stack, getMode(stack));
        if (profile == null) {
            return null;
        }
        return new DisplayedSpellProfile(profile.spell(), profile.spellLevel());
    }

    public static List<Component> buildElementalSpellTooltipSection(ItemStack stack) {
        var displayedSpellProfile = getDisplayedSpellProfile(stack);
        if (displayedSpellProfile == null) {
            return List.of();
        }

        var player = MinecraftInstanceHelper.getPlayer();
        if (!(player instanceof LocalPlayer localPlayer)) {
            return List.of();
        }

        var spellData = new SpellData(displayedSpellProfile.spell(), displayedSpellProfile.spellLevel(), true);
        return TooltipsUtils.formatActiveSpellTooltip(stack, spellData, CastSource.SWORD, localPlayer)
                .stream()
                .map(component -> (Component) component)
                .toList();
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

    private static ElementalMode getMode(ItemStack stack) {
        var tag = stack.getTag();
        return ElementalMode.fromSerializedName(tag == null ? "" : tag.getString(MODE_TAG));
    }

    private static void setMode(ItemStack stack, ElementalMode mode) {
        if (mode == ElementalMode.NONE) {
            var tag = stack.getTag();
            if (tag != null) {
                tag.remove(MODE_TAG);
                if (tag.isEmpty()) {
                    stack.setTag(null);
                }
            }
            if (stack.getItem() instanceof ElementalBow elementalBow) {
                elementalBow.initializeSpellContainer(stack);
            }
            return;
        }

        stack.getOrCreateTag().putString(MODE_TAG, mode.serializedName);
        if (stack.getItem() instanceof ElementalBow elementalBow) {
            elementalBow.initializeSpellContainer(stack);
        }
    }

    private record SpellCastProfile(ElementalMode mode, AbstractSpell spell, int spellLevel) {
    }

    public record DisplayedSpellProfile(AbstractSpell spell, int spellLevel) {
    }

    private enum ElementalMode {
        NONE("none", "item.apprenticecodex.elemental_bow.mode.none", 0),
        FIRE("fire", "item.apprenticecodex.elemental_bow.mode.fire", FIRE_COLOR),
        ENDER("ender", "item.apprenticecodex.elemental_bow.mode.ender", ENDER_COLOR),
        NATURE("nature", "item.apprenticecodex.elemental_bow.mode.nature", NATURE_COLOR);

        private final String serializedName;
        private final String translationKey;
        private final int sparkColor;

        ElementalMode(String serializedName, String translationKey, int sparkColor) {
            this.serializedName = serializedName;
            this.translationKey = translationKey;
            this.sparkColor = sparkColor;
        }

        public Component getDisplayName() {
            return Component.translatable(translationKey);
        }

        public int getSparkColor() {
            return sparkColor;
        }

        public ElementalMode next() {
            return switch (this) {
                case NONE -> FIRE;
                case FIRE -> ENDER;
                case ENDER -> NATURE;
                case NATURE -> NONE;
            };
        }

        public static ElementalMode fromSerializedName(String serializedName) {
            for (var mode : values()) {
                if (mode.serializedName.equals(serializedName)) {
                    return mode;
                }
            }
            return NONE;
        }
    }
}
