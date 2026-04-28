package jp.aquafactory.apprenticecodex.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.item.SpellSlotUpgradeItem;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import jp.aquafactory.apprenticecodex.renderer.item.ManaForceBladeRenderer;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class ManaForceBlade extends SwordItem implements GeoItem, IPresetSpellContainer, RestrictedSpellImbuableItem,
        SpellSlotUpgradeableItem, NonDamageableAnvilMergeItem {
    public static final float DISPLAY_ATTACK_DAMAGE = 6.0F;
    public static final int COOLDOWN_TICKS = 40;

    private static final int DURABILITY = 2031;
    private static final int USE_DURATION = 72000;
    private static final int ENCHANTMENT_VALUE = 15;
    private static final double ATTACK_DAMAGE_MODIFIER_AMOUNT = DISPLAY_ATTACK_DAMAGE - 1.0D;
    private static final double ATTACK_SPEED_MODIFIER_AMOUNT = -2.0D;
    private static final String ATTACK_MANA_SPENT_TICK_TAG = "apprenticecodex:mana_force_blade_attack_mana_spent_tick";
    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("idle");
    private static final ItemStack SWORD_ENCHANTMENT_PROBE_STACK = new ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD);
    private static final Set<String> EXTRA_ENCHANTMENTS = Set.of(
            "apprenticecodex:surge",
            "apprenticecodex:attunement",
            "apprenticecodex:wisdom",
            "apprenticecodex:transcendence"
    );

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Multimap<Holder<Attribute>, AttributeModifier> mainhandModifiers = buildMainhandModifiers();

    public ManaForceBlade() {
        super(Tiers.DIAMOND, new Item.Properties()
                .stacksTo(1)
                .durability(DURABILITY)
                .rarity(Rarity.RARE)
                .attributes(SwordItem.createAttributes(Tiers.DIAMOND, 0, (float) ATTACK_SPEED_MODIFIER_AMOUNT)));
        GeoItem.registerSyncedAnimatable(this);
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
    public void initializeSpellContainer(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty() || ISpellContainer.isSpellContainer(itemStack)) {
            return;
        }

        ISpellContainer.set(itemStack, ISpellContainer.create(1, true, false));
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand usedHand) {
        var stack = player.getItemInHand(usedHand);
        if (usedHand != InteractionHand.MAIN_HAND) {
            return InteractionResultHolder.pass(stack);
        }

        player.startUsingItem(usedHand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(@NotNull ItemStack stack, @NotNull Level level, @NotNull LivingEntity livingEntity, int timeLeft) {
        if (!level.isClientSide && livingEntity instanceof Player player) {
            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        }
    }

    @Override
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack stack) {
        return UseAnim.BLOCK;
    }

    @Override
    public int getUseDuration(@NotNull ItemStack stack, @NotNull LivingEntity entity) {
        return USE_DURATION;
    }

    @Override
    public boolean hurtEnemy(@NotNull ItemStack stack, @NotNull LivingEntity target, @NotNull LivingEntity attacker) {
        if (hasImbuedSpell(stack) && !attacker.level().isClientSide && attacker instanceof Player player) {
            trySpendAttackManaOncePerTick(player, stack);
        }

        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context, @NotNull List<Component> lines,
                                @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);

        lines.add(Component.translatable("item.apprenticecodex.mana_force_blade.desc").withStyle(ChatFormatting.GRAY));
        if (hasImbuedSpell(stack)) {
            lines.add(Component.translatable(
                    "item.apprenticecodex.mana_force_blade.desc.imbue_help",
                    Mth.ceil(resolveBladeAttackManaCost(stack))
            ).withStyle(ChatFormatting.AQUA));
        } else {
            lines.add(Component.translatable("item.apprenticecodex.mana_force_blade.desc.no_imbue")
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        // Iron's の upgrade 処理は同 Attribute/Operation の既存補正 1 本を置換するため、表示前に自前補正を合算しておく。
        var modifiers = OffhandMagicModifierHelper.buildEquippedModifiers(mainhandModifiers, stack, "mana_force_blade");
        var builder = ItemAttributeModifiers.builder();
        for (var entry : modifiers.entries()) {
            builder.add(entry.getKey(), entry.getValue(), EquipmentSlotGroup.MAINHAND);
        }
        return builder.build();
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
        return enchantmentId != null
                && (EXTRA_ENCHANTMENTS.contains(enchantmentId.toString())
                || SWORD_ENCHANTMENT_PROBE_STACK.supportsEnchantment(enchantment));
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
        return enchantmentId != null
                && (EXTRA_ENCHANTMENTS.contains(enchantmentId.toString())
                || SWORD_ENCHANTMENT_PROBE_STACK.supportsEnchantment(enchantment));
    }

    @Override
    public boolean isValidRepairItem(@NotNull ItemStack toRepair, @NotNull ItemStack repair) {
        return repair.is(io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                || super.isValidRepairItem(toRepair, repair);
    }

    @Override
    public boolean canPerformAction(@NotNull ItemStack stack, @NotNull ItemAbility itemAbility) {
        return itemAbility == ItemAbilities.SWORD_SWEEP || super.canPerformAction(stack, itemAbility);
    }

    @Override
    public boolean canImbueSpell(SpellData spellData) {
        return spellData != SpellData.EMPTY && canImbueSpell(spellData.getSpell(), spellData.getLevel());
    }

    @Override
    public boolean canImbueSpell(@Nullable AbstractSpell spell, int spellLevel) {
        return spell != null
                && spell != io.redspace.ironsspellbooks.api.registry.SpellRegistry.none()
                && (spell.getCastType() == CastType.INSTANT || spell.getCastType() == CastType.LONG);
    }

    @Override
    public void normalizeImbuedSpellContainer(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        SpellData spellData = SpellData.EMPTY;
        if (ISpellContainer.isSpellContainer(stack)) {
            var spellContainer = ISpellContainer.get(stack);
            if (spellContainer != null && spellContainer.getMaxSpellCount() > 0) {
                spellData = spellContainer.getSpellAtIndex(0);
            }
        }

        var normalized = ISpellContainer.create(1, true, false).mutableCopy();
        if (canImbueSpell(spellData)) {
            normalized.addSpellAtIndex(spellData.getSpell(), spellData.getLevel(), 0, true);
        }
        ISpellContainer.set(stack, normalized.toImmutable());
    }

    @Override
    public boolean canRemoveWorkbenchSpell(ItemStack stack, ISpellContainer spellContainer, int spellIndex, SpellData spellData) {
        return false;
    }

    @Override
    public ItemStack createSpellSlotUpgradeResult(ItemStack baseStack, SpellSlotUpgradeItem upgradeItem) {
        return ItemStack.EMPTY;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private ManaForceBladeRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (renderer == null) {
                    renderer = new ManaForceBladeRenderer();
                }

                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(this, "main", 0, state -> {
            state.setAnimation(ANIM_IDLE);
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    public static boolean isManaForceBlade(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ManaForceBlade;
    }

    public static float resolveBladeAttackDamage(ItemStack stack) {
        return DISPLAY_ATTACK_DAMAGE;
    }

    public static float resolveBladeAttackManaCost(ItemStack stack) {
        return resolveBladeAttackDamage(stack) * 3.0F;
    }

    public static boolean hasImbuedSpell(ItemStack stack) {
        return MagicTools.getImbuedSpellSchool(stack) != null;
    }

    public static float resolveFinalAttackDamage(LivingEntity attacker, ItemStack stack) {
        return resolveBladeAttackDamage(stack) * resolveDamageMultiplier(attacker, stack);
    }

    public static float resolveDamageMultiplier(LivingEntity attacker, ItemStack stack) {
        var imbuedSchool = MagicTools.getImbuedSpellSchool(stack);
        if (imbuedSchool == null) {
            return 1.0F;
        }

        var spellPower = (float) attacker.getAttributeValue(AttributeRegistry.SPELL_POWER);
        Attribute schoolPowerAttribute = MagicTools.resolveSchoolPowerAttribute(imbuedSchool);
        var schoolSpellPower = schoolPowerAttribute == null
                ? 1.0F
                : (float) attacker.getAttributeValue(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(schoolPowerAttribute));
        return spellPower * schoolSpellPower;
    }

    public static void spendMana(Player player, float manaCost) {
        if (manaCost <= 0.0F || player.getAbilities().instabuild) {
            return;
        }

        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData == null) {
            return;
        }

        magicData.setMana(Math.max(0.0F, magicData.getMana() - manaCost));
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new SyncManaPacket(magicData));
        }
    }

    private static void trySpendAttackManaOncePerTick(Player player, ItemStack stack) {
        var tag = player.getPersistentData();
        var now = player.level().getGameTime();
        if (tag.contains(ATTACK_MANA_SPENT_TICK_TAG) && tag.getLong(ATTACK_MANA_SPENT_TICK_TAG) == now) {
            return;
        }

        // BetterCombat は複数対象を同一 tick 内で個別に攻撃処理するため、攻撃 1 回分の消費に揃える。
        tag.putLong(ATTACK_MANA_SPENT_TICK_TAG, now);
        spendMana(player, resolveBladeAttackManaCost(stack));
    }

    private static Multimap<Holder<Attribute>, AttributeModifier> buildMainhandModifiers() {
        var builder = ImmutableMultimap.<Holder<Attribute>, AttributeModifier>builder();
        builder.put(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(
                        Item.BASE_ATTACK_DAMAGE_ID,
                        ATTACK_DAMAGE_MODIFIER_AMOUNT,
                        AttributeModifier.Operation.ADD_VALUE
                )
        );
        builder.put(
                Attributes.ATTACK_SPEED,
                new AttributeModifier(
                        Item.BASE_ATTACK_SPEED_ID,
                        ATTACK_SPEED_MODIFIER_AMOUNT,
                        AttributeModifier.Operation.ADD_VALUE
                )
        );
        return builder.build();
    }
}
