package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.function.Predicate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.UUID;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.malum.MalumHauntedCompat;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import jp.aquafactory.apprenticecodex.item.ElementalBow;
import jp.aquafactory.apprenticecodex.item.NonDamageableAnvilMergeItem;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;

final class BowGameTestSupport {
    static final String VANILLA_NAMESPACE = "minecraft";
    static final String MALUM_MOD_ID = "malum";
    static final TagKey<Item> MALUM_SOUL_HUNTER_WEAPON = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(MALUM_MOD_ID, "soul_hunter_weapon")
    );
    static final TagKey<Item> IRONS_STAFF = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "staff")
    );
    static final TagKey<Item> SPELLCASTER_WORKBENCH_EXTRACTABLE = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "spellcaster_workbench_extractable")
    );
    static final TagKey<Item> TOMAGIC_REVERSAL_WEAPON = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("traveloptics", "can_cast_reversal")
    );
    static final TagKey<Item> CURIOS_BACK = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("curios", CuriosSlotConstants.BACK)
    );
    static final TagKey<Item> CURIOS_CHARM = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("curios", CuriosSlotConstants.CHARM)
    );
    static final ResourceLocation FOCUS_STAFFBOW_OVERCHARGE_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(
            ApprenticeCodex.MODID,
            "focus_staffbow_overcharge"
    );
    static final ResourceLocation MALUM_ANIMATED =
            ResourceLocation.fromNamespaceAndPath(MALUM_MOD_ID, "animated");

    private BowGameTestSupport() {
    }

    static FakePlayer createEquipmentTestPlayer(GameTestHelper helper, BlockPos pos, String profileName) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), profileName));
        player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var absolutePos = helper.absoluteVec(Vec3.atBottomCenterOf(pos));
        player.setPos(absolutePos.x, absolutePos.y, absolutePos.z);
        return player;
    }

    static FakePlayer createEquipmentTestPlayer(ServerLevel level, BlockPos absolutePos, String profileName) {
        var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), profileName));
        player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var absoluteVec = Vec3.atBottomCenterOf(absolutePos);
        player.setPos(absoluteVec.x, absoluteVec.y, absoluteVec.z);
        return player;
    }

    static void setFocusStaffbowArrowCatalyst(FakePlayer player, ItemStack arrowStack) {
        player.getInventory().setItem(1, arrowStack.copy());
    }

    static ApprenticeCodexServerConfig.GameTestConfigOverride useFocusStaffbowConfigOverrideForGameTest(
            boolean enableContinuousFocusedCast,
            boolean enableManaLoan,
            boolean enableArrowCatalystRequirement,
            double pendingMaxLoanManaRatio,
            List<String> spellDenylist,
            boolean enableSpellAllowlist,
            List<String> spellAllowlist
    ) {
        return ApprenticeCodexServerConfig.useFocusStaffbowConfigOverrideForGameTest(
                enableContinuousFocusedCast,
                enableManaLoan,
                enableArrowCatalystRequirement,
                List.of("minecraft:arrow"),
                3.0D,
                2.0D,
                20,
                2.0D,
                1.0D,
                pendingMaxLoanManaRatio,
                spellDenylist,
                enableSpellAllowlist,
                spellAllowlist
        );
    }

    static ApprenticeCodexServerConfig.GameTestConfigOverride useElementalBowConfigOverrideForGameTest(
            double magicReadyDrawTicksMultiplier,
            double overheatAdditionalManaLinearMultiplier,
            double overheatAdditionalManaQuadraticMultiplier,
            double overheatDurationMultiplier,
            int overheatDurationMinTicks,
            int overheatDurationCapTicks,
            double powerArrowSpellLevelBonusPerLevel
    ) {
        return ApprenticeCodexServerConfig.useElementalBowConfigOverrideForGameTest(
                magicReadyDrawTicksMultiplier,
                overheatAdditionalManaLinearMultiplier,
                overheatAdditionalManaQuadraticMultiplier,
                overheatDurationMultiplier,
                overheatDurationMinTicks,
                overheatDurationCapTicks,
                powerArrowSpellLevelBonusPerLevel
        );
    }

    static int getFocusStaffbowArrowCount(Player player) {
        int count = 0;
        for (var stack : player.getInventory().items) {
            if (stack.getItem() instanceof ArrowItem) {
                count += stack.getCount();
            }
        }
        for (var stack : player.getInventory().offhand) {
            if (stack.getItem() instanceof ArrowItem) {
                count += stack.getCount();
            }
        }
        return count;
    }

    static void equipCurio(FakePlayer player, String slotId, ItemStack stack) {
        var curiosInventory = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player)
                .orElseThrow(() -> new IllegalStateException("Missing curios inventory for curio equip test"));
        curiosInventory.setEquippedCurio(slotId, 0, stack);
    }

    static <T extends jp.aquafactory.apprenticecodex.entity.SummonWeaponEntity> List<T> getOwnedSummonWeapons(
            GameTestHelper helper,
            FakePlayer owner,
            Class<T> weaponType
    ) {
        return helper.getLevel().getEntitiesOfClass(
                weaponType,
                new AABB(owner.position(), owner.position()).inflate(32.0),
                weapon -> {
                    var summonOwner = weapon.getOwner();
                    return summonOwner != null && owner.getUUID().equals(summonOwner.getUUID());
                }
        );
    }

    static void assertExactEnchantmentSurfaces(
            GameTestHelper helper,
            ItemStack stack,
            Set<ResourceLocation> expectedEnchantments,
            String itemName
    ) {
        assertExactEnchantmentSurfaces(
                helper,
                stack,
                expectedEnchantments,
                    expectedEnchantments,
                    expectedEnchantments,
                    expectedEnchantments,
                    expectedEnchantments,
                    itemName
        );
    }

    static void assertExactEnchantmentSurfaces(
            GameTestHelper helper,
            ItemStack stack,
            Set<ResourceLocation> expectedPrimaryEnchantments,
            Set<ResourceLocation> expectedSupportedEnchantments,
            Set<ResourceLocation> expectedDefinitionEnchantments,
            Set<ResourceLocation> expectedBookEnchantments,
            Set<ResourceLocation> expectedAnvilEnchantments,
            String itemName
    ) {
        var item = stack.getItem();
        var registryAccess = helper.getLevel().registryAccess();

        var actualPrimaryEnchantments = collectAllowedEnchantments(
                registryAccess,
                enchantment -> item.isPrimaryItemFor(stack, enchantment)
        );
        helper.assertTrue(actualPrimaryEnchantments.equals(expectedPrimaryEnchantments),
                itemName + " primary enchantments changed: "
                        + describeEnchantmentDifference(expectedPrimaryEnchantments, actualPrimaryEnchantments));

        var actualSupportedEnchantments = collectAllowedEnchantments(
                registryAccess,
                enchantment -> item.supportsEnchantment(stack, enchantment)
        );
        helper.assertTrue(actualSupportedEnchantments.equals(expectedSupportedEnchantments),
                itemName + " supported enchantments changed: "
                        + describeEnchantmentDifference(expectedSupportedEnchantments, actualSupportedEnchantments));

        var actualDefinitionEnchantments = collectAllowedEnchantments(
                registryAccess,
                enchantment -> enchantment.value().canEnchant(stack)
        );
        helper.assertTrue(actualDefinitionEnchantments.equals(expectedDefinitionEnchantments),
                itemName + " enchantment definition support changed: "
                        + describeEnchantmentDifference(expectedDefinitionEnchantments, actualDefinitionEnchantments));

        var actualBookEnchantments = collectAllowedEnchantments(
                registryAccess,
                enchantment -> item.isBookEnchantable(stack, createEnchantedBook(enchantment))
        );
        helper.assertTrue(actualBookEnchantments.equals(expectedBookEnchantments),
                itemName + " book enchantments changed: "
                        + describeEnchantmentDifference(expectedBookEnchantments, actualBookEnchantments));

        if (item instanceof NonDamageableAnvilMergeItem mergeItem) {
            var actualAnvilEnchantments = collectAllowedEnchantments(
                    registryAccess,
                    enchantment -> mergeItem.isAnvilMergeEnchantmentAllowed(stack, enchantment)
            );
            helper.assertTrue(actualAnvilEnchantments.equals(expectedAnvilEnchantments),
                    itemName + " anvil enchantments changed: "
                            + describeEnchantmentDifference(expectedAnvilEnchantments, actualAnvilEnchantments));
        }
    }

    static Set<ResourceLocation> expectedElementalBowEnchantments() {
        var bowStack = new ItemStack(Items.BOW);
        var expectedEnchantments = collectAllowedEnchantments(
                bowStack,
                enchantment -> Items.BOW.canApplyAtEnchantingTable(bowStack, enchantment)
        );
        expectedEnchantments.addAll(registryIdSet(
                EnchantmentRegistry.TRANSCENDENCE,
                EnchantmentRegistry.WISDOM,
                EnchantmentRegistry.PLUNDER,
                EnchantmentRegistry.SYNTHESIS
        ));
        return expectedEnchantments;
    }

    static Set<ResourceLocation> expectedElementalBowBookEnchantments() {
        var bowStack = new ItemStack(Items.BOW);
        var expectedEnchantments = collectAllowedEnchantments(
                bowStack,
                enchantment -> Items.BOW.isBookEnchantable(bowStack, createEnchantedBook(enchantment))
        );
        expectedEnchantments.addAll(registryIdSet(
                EnchantmentRegistry.TRANSCENDENCE,
                EnchantmentRegistry.WISDOM,
                EnchantmentRegistry.PLUNDER,
                EnchantmentRegistry.SYNTHESIS
        ));
        return expectedEnchantments;
    }

    @SafeVarargs
    static Set<ResourceLocation> registryIdSet(ResourceKey<Enchantment>... enchantments) {
        var ids = new LinkedHashSet<ResourceLocation>();
        for (var enchantment : enchantments) {
            ids.add(enchantment.location());
        }
        return ids;
    }

    static List<ResourceKey<Enchantment>> getRegisteredEnchantments() {
        return List.of(
                Enchantments.REFLUX,
                Enchantments.RESERVOIR,
                Enchantments.ALACRITY,
                Enchantments.TENSE,
                Enchantments.SURGE,
                Enchantments.ATTUNEMENT,
                Enchantments.TRANSCENDENCE,
                Enchantments.WISDOM,
                Enchantments.PLUNDER,
                Enchantments.GUZZLE,
                Enchantments.LARGE_MUG,
                Enchantments.RED_ENERGY,
                Enchantments.GLOW_ENERGY,
                Enchantments.SYNTHESIS
        );
    }

    static void assertElementalBowSelection(
            GameTestHelper helper,
            ItemStack stack,
            @Nullable String expectedShotMode,
            @Nullable ResourceLocation expectedSelectionId,
            String message
    ) {
        var tag = getCustomDataTag(stack);
        var actualShotMode = tag != null && tag.contains("ElementalBowShotMode")
                ? tag.getString("ElementalBowShotMode")
                : null;
        ResourceLocation actualSelectionId = null;
        if (tag != null) {
            if ("magic".equals(actualShotMode) && tag.contains("ElementalBowMode")) {
                actualSelectionId = ResourceLocation.tryParse(tag.getString("ElementalBowMode"));
            } else if (tag.contains("ElementalBowAmmoSelection")) {
                actualSelectionId = ResourceLocation.tryParse(tag.getString("ElementalBowAmmoSelection"));
            } else if (actualShotMode == null && tag.contains("ElementalBowMode")) {
                actualSelectionId = ResourceLocation.tryParse(tag.getString("ElementalBowMode"));
            }
        }
        helper.assertTrue(
                Objects.equals(actualShotMode, expectedShotMode) && Objects.equals(actualSelectionId, expectedSelectionId),
                message + ": expected shotMode=" + expectedShotMode + ", selection=" + expectedSelectionId
                        + " but got shotMode=" + actualShotMode + ", selection=" + actualSelectionId
        );
    }

    static String describeElementalBowSelectionView(ElementalBow.ModeSelectionView view) {
        return view.selection().selectionId() == null
                ? view.selection().shotMode()
                : view.selection().shotMode() + ":" + view.selection().selectionId();
    }

    @Nullable
    static ElementalBow.ModeSelectionView findElementalBowSelectionView(
            ServerPlayer player,
            ItemStack stack,
            String shotMode,
            @Nullable ResourceLocation selectionId
    ) {
        return ElementalBow.getAvailableSelectionViews(player, stack).stream()
                .filter(view -> shotMode.equals(view.selection().shotMode())
                        && Objects.equals(selectionId, view.selection().selectionId()))
                .findFirst()
                .orElse(null);
    }

    static void setElementalBowShotSelection(ItemStack stack, String shotMode, @Nullable ResourceLocation selectionId) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString("ElementalBowShotMode", shotMode);
            if ("magic".equals(shotMode)) {
                if (selectionId != null) {
                    tag.putString("ElementalBowMode", selectionId.toString());
                }
                tag.remove("ElementalBowAmmoSelection");
                return;
            }

            if (selectionId != null) {
                tag.putString("ElementalBowAmmoSelection", selectionId.toString());
            } else {
                tag.remove("ElementalBowAmmoSelection");
            }
            tag.remove("ElementalBowMode");
        });
    }

    static void assertTranslatableKey(
            GameTestHelper helper,
            Component component,
            String expectedKey,
            String message
    ) {
        helper.assertTrue(
                component.getContents() instanceof TranslatableContents contents
                        && expectedKey.equals(contents.getKey()),
                message + ": expected " + expectedKey + " but got " + component
        );
    }

    static void assertTooltipKeyAt(
            GameTestHelper helper,
            ItemStack stack,
            int index,
            String expectedKey,
            String message
    ) {
        var tooltipLines = new ArrayList<Component>();
        stack.getItem().appendHoverText(stack, Item.TooltipContext.of(helper.getLevel()), tooltipLines, TooltipFlag.Default.NORMAL);
        helper.assertTrue(tooltipLines.size() > index,
                message + " (tooltip line count=" + tooltipLines.size() + ")");
        assertTranslatableKey(helper, tooltipLines.get(index), expectedKey, message);
    }

    static void assertTooltipKeyUsesColor(
            GameTestHelper helper,
            ItemStack stack,
            String expectedKey,
            ChatFormatting expectedColor,
            String message
    ) {
        var tooltipLines = new ArrayList<Component>();
        stack.getItem().appendHoverText(stack, Item.TooltipContext.of(helper.getLevel()), tooltipLines, TooltipFlag.Default.NORMAL);
        var matchingLine = tooltipLines.stream()
                .filter(component -> component.getContents() instanceof TranslatableContents contents
                        && expectedKey.equals(contents.getKey()))
                .findFirst();
        helper.assertTrue(matchingLine.isPresent(),
                message + " (missing tooltip key=" + expectedKey + ")");
        if (matchingLine.isPresent()) {
            var expectedTextColor = TextColor.fromLegacyFormat(expectedColor);
            helper.assertTrue(Objects.equals(expectedTextColor, matchingLine.get().getStyle().getColor()),
                    message + " (expected=" + expectedTextColor + ", actual="
                            + matchingLine.get().getStyle().getColor() + ")");
        }
    }

    static void assertTooltipKeyAbsent(GameTestHelper helper, ItemStack stack, String key, String message) {
        var tooltipLines = new ArrayList<Component>();
        stack.getItem().appendHoverText(stack, Item.TooltipContext.of(helper.getLevel()), tooltipLines, TooltipFlag.Default.NORMAL);
        var present = tooltipLines.stream()
                .anyMatch(component -> component.getContents() instanceof TranslatableContents contents
                        && key.equals(contents.getKey()));
        helper.assertFalse(present, message + " (unexpected tooltip key=" + key + ")");
    }

    static Set<ResourceLocation> collectAllowedEnchantments(
            RegistryAccess registryAccess,
            Predicate<net.minecraft.core.Holder<Enchantment>> predicate
    ) {
        var allowedEnchantments = new LinkedHashSet<ResourceLocation>();
        var enchantments = registryAccess.lookupOrThrow(Registries.ENCHANTMENT).listElements()
                .sorted(Comparator.comparing(holder -> holder.key().location().toString()))
                .toList();

        for (var enchantment : enchantments) {
            if (predicate.test(enchantment)) {
                allowedEnchantments.add(enchantment.key().location());
            }
        }
        return allowedEnchantments;
    }

    static ItemStack createEnchantedBook(net.minecraft.core.Holder<Enchantment> enchantment) {
        return createEnchantedBook(enchantment, 1);
    }

    static ItemStack createEnchantedBook(net.minecraft.core.Holder<Enchantment> enchantment, int level) {
        var book = new ItemStack(Items.ENCHANTED_BOOK);
        book.enchant(enchantment, level);
        return book;
    }

    static String describeEnchantmentDifference(
            Set<ResourceLocation> expectedEnchantments,
            Set<ResourceLocation> actualEnchantments
    ) {
        var missingEnchantments = new LinkedHashSet<>(expectedEnchantments);
        missingEnchantments.removeAll(actualEnchantments);

        var unexpectedEnchantments = new LinkedHashSet<>(actualEnchantments);
        unexpectedEnchantments.removeAll(expectedEnchantments);

        return "missing=" + missingEnchantments + ", unexpected=" + unexpectedEnchantments;
    }

    static double sumModifierAmount(
            Collection<AttributeModifier> modifiers,
            AttributeModifier.Operation operation
    ) {
        return modifiers.stream()
                .filter(modifier -> modifier.operation() == operation)
                .mapToDouble(AttributeModifier::amount)
                .sum();
    }

    static String describeModifiers(com.google.common.collect.Multimap<Holder<Attribute>, AttributeModifier> modifiers) {
        return modifiers.entries().stream()
                .map(entry -> BuiltInRegistries.ATTRIBUTE.getKey(entry.getKey().value()) + "="
                        + entry.getValue().amount() + "@" + entry.getValue().operation())
                .collect(Collectors.joining(", "));
    }

    static ItemStack createSpellScroll(AbstractSpell spell) {
        var stack = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get());
        ISpellContainer.createScrollContainer(spell, 1, stack);
        return stack;
    }

    static void setSingleUnlockedSpell(GameTestHelper helper, ItemStack stack, AbstractSpell spell, int spellLevel) {
        var spellContainer = ISpellContainer.get(stack);
        helper.assertTrue(spellContainer != null, "Missing spell container before Focus Staffbow spell setup");

        var mutable = spellContainer.mutableCopy();
        if (mutable.getSpellAtIndex(0) != SpellData.EMPTY) {
            helper.assertTrue(mutable.removeSpellAtIndex(0),
                    "Failed to clear existing spell before Focus Staffbow spell setup");
        }
        helper.assertTrue(mutable.addSpellAtIndex(spell, spellLevel, 0, false),
                "Failed to prepare Focus Staffbow spell setup");
        ISpellContainer.set(stack, mutable.toImmutable());
    }

    static void assertSpellData(
            GameTestHelper helper,
            SpellData spellData,
            AbstractSpell expectedSpell,
            int expectedLevel,
            String message
    ) {
        helper.assertTrue(spellData != io.redspace.ironsspellbooks.api.spells.SpellData.EMPTY,
                message + " (spell data is empty)");
        helper.assertTrue(spellData.getSpell() == expectedSpell,
                message + " (spell mismatch: " + spellData.getSpell().getSpellResource() + ")");
        helper.assertTrue(spellData.getLevel() == expectedLevel,
                message + " (level mismatch: " + spellData.getLevel() + ")");
    }

    static void assertSpellData(
            GameTestHelper helper,
            ISpellContainer spellContainer,
            int index,
            AbstractSpell expectedSpell,
            int expectedLevel,
            boolean expectedLocked,
            String message
    ) {
        var spellData = spellContainer.getSpellAtIndex(index);
        helper.assertTrue(spellData != io.redspace.ironsspellbooks.api.spells.SpellData.EMPTY,
                message + " (spell slot is empty at index " + index + ")");
        helper.assertTrue(spellData.getSpell() == expectedSpell,
                message + " (spell mismatch: " + spellData.getSpell().getSpellResource() + ")");
        helper.assertTrue(spellData.getLevel() == expectedLevel,
                message + " (level mismatch: " + spellData.getLevel() + ")");
        helper.assertTrue(spellData.isLocked() == expectedLocked,
                message + " (locked mismatch: " + spellData.isLocked() + ")");
    }


    static void setElementalBowMode(ItemStack stack, String mode) {
        var normalizedMode = normalizeElementalBowModeId(mode);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString("ElementalBowMode", normalizedMode));
    }

    static int getEnchantmentLevel(ItemStack stack, net.minecraft.core.Holder<Enchantment> enchantment) {
        return EnchantmentHelper.getEnchantmentsForCrafting(stack).getLevel(enchantment);
    }

    static Set<ResourceLocation> requiredElementalBowExtraEnchantments() {
        return registryIdSet(
                Enchantments.TRANSCENDENCE,
                Enchantments.WISDOM,
                Enchantments.PLUNDER,
                Enchantments.SYNTHESIS
        );
    }

    static void assertReferenceItemEnchantmentsWithRequiredExtras(
            GameTestHelper helper,
            ItemStack stack,
            ItemStack referenceStack,
            Set<ResourceLocation> requiredExtraEnchantments,
            String itemName
    ) {
        var registryAccess = helper.getLevel().registryAccess();
        var expectedPrimaryEnchantments = expectedReferencePrimaryEnchantments(registryAccess, referenceStack, requiredExtraEnchantments);
        var expectedSupportedEnchantments = expectedReferenceSupportedEnchantments(registryAccess, referenceStack, requiredExtraEnchantments);
        var expectedDefinitionEnchantments = expectedReferenceDefinitionEnchantments(registryAccess, referenceStack, requiredExtraEnchantments);
        var expectedBookEnchantments = expectedReferenceBookEnchantments(registryAccess, referenceStack, requiredExtraEnchantments);

        assertExactEnchantmentSurfaces(
                helper,
                stack,
                expectedPrimaryEnchantments,
                expectedSupportedEnchantments,
                expectedDefinitionEnchantments,
                expectedBookEnchantments,
                expectedSupportedEnchantments,
                itemName
        );

        var enchantmentLookup = registryAccess.lookupOrThrow(Registries.ENCHANTMENT);
        for (var enchantmentId : requiredExtraEnchantments) {
            assertSingleEnchantmentSurfaces(
                    helper,
                    stack,
                    enchantmentLookup.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT, enchantmentId)),
                    true,
                    true,
                    true,
                    true,
                    stack.getItem() instanceof NonDamageableAnvilMergeItem ? Boolean.TRUE : null,
                    itemName + " required extra enchantment"
            );
        }
    }

    static void assertModifierAmount(
            GameTestHelper helper,
            ItemAttributeModifiers modifiers,
            Attribute attribute,
            EquipmentSlotGroup slotGroup,
            double expectedAmount,
            AttributeModifier.Operation operation,
            String message
    ) {
        var actualAmount = modifiers.modifiers().stream()
                .filter(entry -> entry.slot().equals(slotGroup))
                .filter(entry -> entry.attribute().value() == attribute)
                .filter(entry -> entry.modifier().operation() == operation)
                .mapToDouble(entry -> entry.modifier().amount())
                .sum();
        helper.assertTrue(Math.abs(actualAmount - expectedAmount) < 1.0e-9D,
                message + ": expected " + expectedAmount + " but got " + actualAmount);
    }


    static void assertSingleEnchantmentSurfaces(
            GameTestHelper helper,
            ItemStack stack,
            net.minecraft.core.Holder<Enchantment> enchantment,
            boolean expectedPrimary,
            boolean expectedSupported,
            boolean expectedDefinitionSupport,
            boolean expectedBook,
            Boolean expectedAnvil,
            String message
    ) {
        var item = stack.getItem();
        var enchantmentId = enchantment.unwrapKey().orElseThrow().location();

        helper.assertTrue(item.isPrimaryItemFor(stack, enchantment) == expectedPrimary,
                message + " primary rule changed for " + enchantmentId + ": expected " + expectedPrimary);
        helper.assertTrue(item.supportsEnchantment(stack, enchantment) == expectedSupported,
                message + " supported rule changed for " + enchantmentId + ": expected " + expectedSupported);
        helper.assertTrue(enchantment.value().canEnchant(stack) == expectedDefinitionSupport,
                message + " definition support changed for " + enchantmentId + ": expected " + expectedDefinitionSupport);
        helper.assertTrue(item.isBookEnchantable(stack, createEnchantedBook(enchantment)) == expectedBook,
                message + " book rule changed for " + enchantmentId + ": expected " + expectedBook);

        if (expectedAnvil != null) {
            helper.assertTrue(item instanceof NonDamageableAnvilMergeItem,
                    message + " anvil expectation requires NonDamageableAnvilMergeItem");
            var actualAnvil = ((NonDamageableAnvilMergeItem) item).isAnvilMergeEnchantmentAllowed(stack, enchantment);
            helper.assertTrue(actualAnvil == expectedAnvil,
                    message + " anvil rule changed for " + enchantmentId + ": expected " + expectedAnvil);
        }
    }

    static Set<ResourceLocation> expectedReferenceBookEnchantments(
            RegistryAccess registryAccess,
            ItemStack referenceStack,
            Set<ResourceLocation> requiredExtraEnchantments
    ) {
        return expectedReferenceEnchantments(
                registryAccess,
                requiredExtraEnchantments,
                enchantment -> referenceStack.getItem().isBookEnchantable(referenceStack, createEnchantedBook(enchantment))
        );
    }

    static Set<ResourceLocation> expectedReferenceDefinitionEnchantments(
            RegistryAccess registryAccess,
            ItemStack referenceStack,
            Set<ResourceLocation> requiredExtraEnchantments
    ) {
        return expectedReferenceEnchantments(
                registryAccess,
                requiredExtraEnchantments,
                enchantment -> enchantment.value().canEnchant(referenceStack)
        );
    }

    static Set<ResourceLocation> expectedReferencePrimaryEnchantments(
            RegistryAccess registryAccess,
            ItemStack referenceStack,
            Set<ResourceLocation> requiredExtraEnchantments
    ) {
        return expectedReferenceEnchantments(
                registryAccess,
                requiredExtraEnchantments,
                enchantment -> referenceStack.getItem().isPrimaryItemFor(referenceStack, enchantment)
        );
    }

    static Set<ResourceLocation> expectedReferenceSupportedEnchantments(
            RegistryAccess registryAccess,
            ItemStack referenceStack,
            Set<ResourceLocation> requiredExtraEnchantments
    ) {
        return expectedReferenceEnchantments(
                registryAccess,
                requiredExtraEnchantments,
                enchantment -> referenceStack.getItem().supportsEnchantment(referenceStack, enchantment)
        );
    }


    static Set<ResourceLocation> expectedReferenceEnchantments(
            RegistryAccess registryAccess,
            Set<ResourceLocation> requiredExtraEnchantments,
            Predicate<net.minecraft.core.Holder<Enchantment>> predicate
    ) {
        var expectedEnchantments = collectAllowedEnchantments(registryAccess, predicate);
        expectedEnchantments.addAll(requiredExtraEnchantments);
        return expectedEnchantments;
    }
}
