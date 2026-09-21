package jp.aquafactory.apprenticecodex.gametest;

import com.google.common.collect.Multimap;
import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.config.SpellConfigManager;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import jp.aquafactory.apprenticecodex.entity.SummonWeaponEntity;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowScrollStorage;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.OnDatapackSyncEvent;
import top.theillusivec4.curios.api.CuriosApi;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.UUID;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.malum.MalumHauntedCompat;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBow;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.registries.ForgeRegistries;
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
    static final TagKey<Item> CURIOS_BELT = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("curios", CuriosSlotConstants.BELT)
    );
    static final TagKey<Item> CURIOS_CHARM = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("curios", CuriosSlotConstants.CHARM)
    );
    static final UUID FOCUS_STAFFBOW_OVERCHARGE_MODIFIER_ID = UUID.fromString("a7dc54b6-a83c-4a5f-ae93-0cb49780fc8f");
    static final ResourceLocation MALUM_ANIMATED = MalumHauntedCompat.animatedEnchantmentId();

    private BowGameTestSupport() {
    }

    static FakePlayer createEquipmentTestPlayer(GameTestHelper helper, BlockPos pos, String profileName) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), profileName));
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        var absolutePos = helper.absoluteVec(Vec3.atBottomCenterOf(pos));
        player.setPos(absolutePos.x, absolutePos.y, absolutePos.z);
        return player;
    }

    static FakePlayer createEquipmentTestPlayer(ServerLevel level, BlockPos absolutePos, String profileName) {
        var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), profileName));
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
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
            int overheatDurationCapTicks) {
        return ApprenticeCodexServerConfig.useElementalBowConfigOverrideForGameTest(
                magicReadyDrawTicksMultiplier,
                overheatAdditionalManaLinearMultiplier,
                overheatAdditionalManaQuadraticMultiplier,
                overheatDurationMultiplier,
                overheatDurationMinTicks,
                overheatDurationCapTicks);
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
        var curiosInventory = CuriosApi.getCuriosInventory(player)
                .orElseThrow(() -> new IllegalStateException("Missing curios inventory for curio equip test"));
        curiosInventory.setEquippedCurio(slotId, 0, stack);
    }

    static <T extends SummonWeaponEntity> List<T> getOwnedSummonWeapons(
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

    static void assertElementalBowSelection(
            GameTestHelper helper,
            ItemStack stack,
            @Nullable String expectedShotMode,
            @Nullable ResourceLocation expectedSelectionId,
            String message
    ) {
        var tag = stack.getTag();
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
                Objects.equals(actualShotMode, expectedShotMode) && Objects.equals(actualSelectionId, "magic".equals(expectedShotMode) ? elementalBowSlotId(expectedSelectionId) : expectedSelectionId),
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
                        && Objects.equals("magic".equals(shotMode) ? elementalBowSlotId(selectionId) : selectionId, view.selection().selectionId()))
                .findFirst()
                .orElse(null);
    }

    static void setElementalBowShotSelection(ItemStack stack, String shotMode, @Nullable ResourceLocation selectionId) {
        ElementalBowScrollStorage.migrate(stack);
        if ("magic".equals(shotMode)) prepareElementalBowScrolls(stack);
        var resolvedSelectionId = "magic".equals(shotMode) ? elementalBowSlotId(selectionId) : selectionId;
        { var tag = stack.getOrCreateTag();
            tag.putString("ElementalBowShotMode", shotMode);
            if ("magic".equals(shotMode)) {
                if (selectionId != null) {
                    tag.putString("ElementalBowMode", resolvedSelectionId.toString());
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
        }
    }

    static void assertTranslatableKey(GameTestHelper helper, Component component, String expectedKey, String message) {
        var contents = component.getContents();
        helper.assertTrue(contents instanceof TranslatableContents,
                message + " (component was not translatable: " + component + ")");
        if (contents instanceof TranslatableContents translatableContents) {
            helper.assertTrue(expectedKey.equals(translatableContents.getKey()),
                    message + " (expected=" + expectedKey + ", actual=" + translatableContents.getKey() + ")");
        }
    }

    static void assertTooltipKeyAt(
            GameTestHelper helper,
            ItemStack stack,
            int index,
            String expectedKey,
            String message
    ) {
        var tooltipLines = new ArrayList<Component>();
        stack.getItem().appendHoverText(stack, helper.getLevel(), tooltipLines, TooltipFlag.Default.NORMAL);
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
        stack.getItem().appendHoverText(stack, helper.getLevel(), tooltipLines, TooltipFlag.Default.NORMAL);
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
        stack.getItem().appendHoverText(stack, helper.getLevel(), tooltipLines, TooltipFlag.Default.NORMAL);
        var present = tooltipLines.stream()
                .anyMatch(component -> component.getContents() instanceof TranslatableContents contents
                        && key.equals(contents.getKey()));
        helper.assertFalse(present, message + " (unexpected tooltip key=" + key + ")");
    }

    static double sumModifierAmount(
            Collection<AttributeModifier> modifiers,
            AttributeModifier.Operation operation
    ) {
        return modifiers.stream()
                .filter(modifier -> modifier.getOperation() == operation)
                .mapToDouble(AttributeModifier::getAmount)
                .sum();
    }

    static String describeModifiers(Multimap<Attribute, AttributeModifier> modifiers) {
        return modifiers.entries().stream()
                .map(entry -> ForgeRegistries.ATTRIBUTES.getKey(entry.getKey()) + "="
                        + entry.getValue().getAmount() + "@" + entry.getValue().getOperation())
                .collect(Collectors.joining(", "));
    }

    static ItemStack createSpellScroll(AbstractSpell spell) {
        var stack = new ItemStack(ItemRegistry.SCROLL.get());
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
        helper.assertTrue(spellData != SpellData.EMPTY,
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
        helper.assertTrue(spellData != SpellData.EMPTY,
                message + " (spell slot is empty at index " + index + ")");
        helper.assertTrue(spellData.getSpell() == expectedSpell,
                message + " (spell mismatch: " + spellData.getSpell().getSpellResource() + ")");
        helper.assertTrue(spellData.getLevel() == expectedLevel,
                message + " (level mismatch: " + spellData.getLevel() + ")");
        helper.assertTrue(spellData.isLocked() == expectedLocked,
                message + " (locked mismatch: " + spellData.isLocked() + ")");
    }

    static void setElementalBowMode(ItemStack stack, String mode) {
        setElementalBowShotSelection(stack, "magic", ResourceLocation.tryParse((mode.contains(":") ? mode : "irons_spellbooks:" + mode)));
    }

    static ApprenticeCodexServerConfig.GameTestConfigOverride useElementalBowSpellConfig(GameTestHelper helper) {
        var previous = SpellConfigManager.INSTANCE;
        SpellConfigManager.INSTANCE = new SpellConfigManager();
        SpellConfigManager.INSTANCE.handleServerConfigUpdate();
        SpellConfigManager.onDatapackSync(
                new OnDatapackSyncEvent(helper.getLevel().getServer().getPlayerList(), null));
        return () -> SpellConfigManager.INSTANCE = previous;
    }

    static ResourceLocation elementalBowSlotId(ResourceLocation school) {
        if (school == null) return null;
        return switch (school.toString()) {
            case "irons_spellbooks:fire" -> ElementalBow.selectionIdForSlot(0);
            case "irons_spellbooks:ender" -> ElementalBow.selectionIdForSlot(1);
            case "irons_spellbooks:nature" -> ElementalBow.selectionIdForSlot(2);
            default -> school;
        };
    }

    static void prepareElementalBowScrolls(ItemStack stack) {
        var lookup = ElementalBow.serializationLookup();
        var item = (ElementalBow) stack.getItem();
        var upgrade = new ItemStack(ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get());
        item.trySetCalibrationAdjustment(stack, 0, upgrade);
        item.trySetCalibrationAdjustment(stack, 1, upgrade);
        var spells = List.of(SpellRegistry.FIRE_ARROW_SPELL.get(),
                SpellRegistry.MAGIC_ARROW_SPELL.get(),
                SpellRegistry.POISON_ARROW_SPELL.get());
        for (int i = 0; i < spells.size(); i++) {
            if (ElementalBow.getCalibrationScroll(stack, i, lookup).isEmpty()) {
                var scroll = createSpellScroll(spells.get(i));
                ElementalBow.setCalibrationScroll(stack, i, scroll, lookup);
            }
        }
    }
}
