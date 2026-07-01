package jp.aquafactory.apprenticecodex.item.spellthrowablecard;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.jei.IJeiInfoItem;
import jp.aquafactory.apprenticecodex.entity.spellthrowablecard.AbstractSpellThrowableCardEntity;
import jp.aquafactory.apprenticecodex.item.ImbueTooltipHelper;
import jp.aquafactory.apprenticecodex.item.RestrictedSpellImbuableItem;
import jp.aquafactory.apprenticecodex.item.WeaponImbueCooldownPolicyItem;
import jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellPayload;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastOrigin;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastRules;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractSpellThrowableCardItem extends Item implements RestrictedSpellImbuableItem,
        WeaponImbueCooldownPolicyItem, IJeiInfoItem {
    public static final int SPELL_SLOT_COUNT = 1;
    public static final float THROW_POWER = 1.6F;
    public static final String CASTING_SLOT = "spell_throwable_card";
    private static final String JEI_INFO_KEY_PREFIX = "jei." + ApprenticeCodex.MODID + ".spell_throwable_cards.desc_";
    private static final String JEI_INFO_GROUP_ID = ApprenticeCodex.MODID + ":spell_throwable_cards";

    protected AbstractSpellThrowableCardItem() {
        super(new Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON));
    }

    @Override
    public @NotNull ItemStack getDefaultInstance() {
        var stack = super.getDefaultInstance();
        initializeSpellContainer(stack);
        return stack;
    }

    @Override
    public void onCraftedBy(@NotNull ItemStack stack, @NotNull Level level, @NotNull Player player) {
        super.onCraftedBy(stack, level, player);
        initializeSpellContainer(stack);
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (!level.isClientSide) {
            initializeSpellContainer(stack);
        }
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand usedHand) {
        var stack = player.getItemInHand(usedHand);
        initializeSpellContainer(stack);

        if (level.isClientSide) {
            return InteractionResultHolder.consume(stack);
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.fail(stack);
        }

        var spellData = getPrimarySpellData(stack);
        var validationMessage = validateThrowStart(serverPlayer, stack, spellData);
        if (validationMessage != null) {
            serverPlayer.displayClientMessage(validationMessage.copy().withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }

        var thrownStack = stack.copy();
        thrownStack.setCount(1);
        var projectile = createProjectile(level, serverPlayer, thrownStack);
        projectile.shootFromRotation(serverPlayer, serverPlayer.getXRot(), serverPlayer.getYRot(), 0.0F, THROW_POWER, 1.0F);
        level.addFreshEntity(projectile);
        level.playSound(null, projectile, SoundRegistry.VANILLA_CARD_THROW.get(), SoundSource.PLAYERS, 0.6F, 1.2F);
        if (!serverPlayer.getAbilities().instabuild) {
            stack.shrink(1);
        }
        serverPlayer.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    protected abstract AbstractSpellThrowableCardEntity createProjectile(Level level, Player owner, ItemStack thrownStack);

    public void initializeSpellContainer(ItemStack stack) {
        if (stack == null || stack.isEmpty() || ISpellContainer.isSpellContainer(stack)) {
            return;
        }
        ISpellContainer.set(stack, ISpellContainer.create(SPELL_SLOT_COUNT, false, false));
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        return getPrimarySpellData(stack) != SpellData.EMPTY || super.isFoil(stack);
    }

    @Override
    public boolean canImbueSpell(SpellData spellData) {
        return spellData != SpellData.EMPTY && canImbueSpell(spellData.getSpell(), spellData.getLevel());
    }

    @Override
    public boolean canImbueSpell(@Nullable AbstractSpell spell, int spellLevel) {
        return RemoteOwnerCastRules.checkImbue(spell, spellLevel, RemoteOwnerCastOrigin.CHARGED_TWIN_BLADE_STAFF_IMPACT)
                .isAllowed();
    }

    @Override
    public void normalizeImbuedSpellContainer(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        initializeSpellContainer(stack);
        var current = ISpellContainer.get(stack);
        if (current == null) {
            return;
        }

        var spellData = current.getSpellAtIndex(0);
        var normalized = ISpellContainer.create(SPELL_SLOT_COUNT, false, false).mutableCopy();
        if (spellData != SpellData.EMPTY && canImbueSpell(spellData)) {
            normalized.addSpellAtIndex(spellData.getSpell(), spellData.getLevel(), 0, false);
        }
        ISpellContainer.set(stack, normalized.toImmutable());
    }

    @Override
    public ItemStack createArcaneAnvilImbueResult(ItemStack baseStack, SpellData spellData) {
        var resultStack = baseStack.copy();
        initializeSpellContainer(resultStack);
        var mutable = ISpellContainer.create(SPELL_SLOT_COUNT, false, false).mutableCopy();
        mutable.addSpellAtIndex(spellData.getSpell(), spellData.getLevel(), 0, false);
        ISpellContainer.set(resultStack, mutable.toImmutable());
        normalizeImbuedSpellContainer(resultStack);
        return resultStack;
    }

    @Override
    public boolean canRemoveWorkbenchSpell(ItemStack stack, ISpellContainer spellContainer, int spellIndex, SpellData spellData) {
        // カードは作成時にスクロールを消費しないため、抽出を許すとスクロールを複製できてしまう.
        return false;
    }

    public SpellData getPrimarySpellData(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !ISpellContainer.isSpellContainer(stack)) {
            return SpellData.EMPTY;
        }
        var spellContainer = ISpellContainer.get(stack);
        if (spellContainer == null) {
            return SpellData.EMPTY;
        }
        var spellData = spellContainer.getSpellAtIndex(0);
        return spellData == null ? SpellData.EMPTY : spellData;
    }

    public ChargedTwinBladeStaffSpellPayload createImpactPayload(ItemStack stack) {
        var spellData = getPrimarySpellData(stack);
        if (spellData == SpellData.EMPTY || spellData.getSpell().getSpellResource() == null) {
            return ChargedTwinBladeStaffSpellPayload.EMPTY;
        }
        return new ChargedTwinBladeStaffSpellPayload(
                spellData.getSpell().getSpellResource(),
                Math.max(1, spellData.getLevel()),
                CastSource.SWORD.name(),
                CASTING_SLOT
        );
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> lines,
                                @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, level, lines, flag);
        lines.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.YELLOW));
        appendThrowableCardTooltip(lines);
    }

    @Override
    public List<Component> getImbueRestrictionTooltipLines() {
        return collectRestrictTooltipSection();
    }

    @Override
    public String getJeiInfoTranslationKeyPrefix() {
        return JEI_INFO_KEY_PREFIX;
    }

    @Override
    public @Nullable String getJeiInfoGroupId() {
        return JEI_INFO_GROUP_ID;
    }

    @Override
    public boolean ignoresWeaponImbueCooldownMultiplier(ItemStack stack, @Nullable AbstractSpell spell, CastSource castSource) {
        return castSource == CastSource.SWORD;
    }

    private @Nullable Component validateThrowStart(ServerPlayer player, ItemStack stack, SpellData spellData) {
        if (spellData == SpellData.EMPTY || spellData.getSpell() == null || spellData.getSpell() == SpellRegistry.none()) {
            return Component.translatable("ui." + ApprenticeCodex.MODID + ".spell_throwable_cards.not_imbued");
        }
        if (!canImbueSpell(spellData)) {
            return Component.translatable("ui." + ApprenticeCodex.MODID + ".charged_twin_blade_staff.unsupported_cast",
                    spellData.getSpell().getDisplayName(player),
                    stack.getHoverName());
        }

        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData == null) {
            return Component.translatable("ui." + ApprenticeCodex.MODID + ".spell_throwable_cards.not_imbued");
        }
        var castResult = spellData.getSpell().canBeCastedBy(spellData.getLevel(), CastSource.SWORD, magicData, player);
        return castResult.isSuccess()
                ? null
                : castResult.message == null ? Component.empty() : castResult.message;
    }

    private static void appendThrowableCardTooltip(List<Component> lines) {
        ImbueTooltipHelper.appendBlankLineIfNeeded(lines);
        if (ImbueTooltipHelper.appendHintIfDetailsHidden(lines)) {
            return;
        }

        ImbueTooltipHelper.appendTooltipSection(
                lines,
                collectAbilityTooltipSection(),
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ability_title",
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ability_none"
        );
        ImbueTooltipHelper.appendTooltipSection(
                lines,
                collectRestrictTooltipSection(),
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.restrict_title",
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.restrict_none"
        );
    }

    private static List<Component> collectAbilityTooltipSection() {
        var translatedLines = new ArrayList<Component>();
        translatedLines.add(ImbueTooltipHelper.translatableGray(
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ability_long_to_instant"
        ));
        translatedLines.add(ImbueTooltipHelper.translatableGray(
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ability_extend_cooldown"
        ));
        return translatedLines;
    }

    private static List<Component> collectRestrictTooltipSection() {
        var translatedLines = new ArrayList<Component>();
        translatedLines.add(ImbueTooltipHelper.translatableGray(
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.restrict_restrict_accept_summon_recast"
        ));
        translatedLines.add(ImbueTooltipHelper.translatableGray(
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.restrict_restrict_by_profile"
        ));
        return translatedLines;
    }
}
