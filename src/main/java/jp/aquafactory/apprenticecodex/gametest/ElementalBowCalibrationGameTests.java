package jp.aquafactory.apprenticecodex.gametest;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.block.spellcalibrationbench.SpellCalibrationBenchMenu;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBow;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowModeList;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatSyncEvents;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowScrollStorage;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;

import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ElementalBowCalibrationGameTests {
    private ElementalBowCalibrationGameTests() {
    }

    @GameTest(template = "gametest/basic_floor")
    public static void elementalBowMigrationPreservesOwnedComponents(GameTestHelper helper) {
        var lookup = helper.getLevel().registryAccess();
        var bow = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
        bow.setDamageValue(17);
        bow.setHoverName(Component.literal("Owned bow"));
        bow.enchant(Enchantments.POWER_ARROWS, 5);
        ISpellContainer.createScrollContainer(SpellRegistry.FIRE_ARROW_SPELL.get(), 10, bow);
        { var tag = bow.getOrCreateTag();
            tag.putString("ElementalBowShotMode", "magic");
            tag.putString("ElementalBowMode", "irons_spellbooks:fire");
            tag.putString("Unrelated", "preserved");
        }
        ElementalBowScrollStorage.migrate(bow);
        helper.assertFalse(ISpellContainer.isSpellContainer(bow), "Legacy generated spells must be removed");
        helper.assertTrue(bow.getDamageValue() == 17 && bow.getHoverName().getString().equals("Owned bow"),
                "Migration must preserve damage and name");
        helper.assertTrue(bow.getEnchantmentLevel(Enchantments.POWER_ARROWS) == 5,
                "Migration must preserve enchantments");
        helper.assertTrue(bow.getOrCreateTag().getString("Unrelated").equals("preserved"),
                "Migration must preserve unrelated custom data");
        helper.assertTrue(ElementalBow.getDisplayedSpellProfile(bow) == null, "Legacy selection must become physical");
        var scroll = BowGameTestSupport.createSpellScroll(SpellRegistry.FIRE_ARROW_SPELL.get());
        scroll.setHoverName(Component.literal("Owned scroll"));
        ElementalBow.setCalibrationScroll(bow, 0, scroll, lookup);
        var restored = ItemStack.of(bow.save(new CompoundTag()));
        ElementalBowScrollStorage.migrate(restored);
        helper.assertTrue(ItemStack.isSameItemSameTags(scroll, ElementalBow.getCalibrationScroll(restored, 0, lookup)),
                "Saved scroll components must survive serialization and repeated migration");
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void elementalBowBenchKeepsInactiveScrollsExtractable(GameTestHelper helper) {
        var player = BowGameTestSupport.createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_bench");
        var bow = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
        var menu = new SpellCalibrationBenchMenu(0, player.getInventory());
        menu.getSlot(0).set(bow);
        var scroll = BowGameTestSupport.createSpellScroll(SpellRegistry.FIRE_ARROW_SPELL.get());
        helper.assertTrue(ElementalBow.getEnabledCalibrationScrollSlotCount(bow) == 1, "A new bow must have one scroll slot");
        var restrictions = menu.getImbueRestrictionTooltipLines();
        helper.assertTrue(restrictions.size() == 1
                        && restrictions.get(0).getContents() instanceof TranslatableContents text
                        && text.getKey().equals("item.apprenticecodex.spellgun.tooltip.restrict_restrict_by_specific.elemental_bow"),
                "The bench must expose the arrow-spell restriction for its yellow slots and hover tooltip");
        helper.assertTrue(menu.getSlot(4).mayPlace(scroll), "The initial scroll slot must accept an arrow spell");
        helper.assertFalse(menu.getSlot(5).mayPlace(scroll), "Unexpanded slots must reject insertion");
        helper.assertFalse(menu.getSlot(1).mayPlace(new ItemStack(ItemRegistry.SCROLLWOVEN_PARCHMENT.get())),
                "Scrollwoven Parchment must not be a bow upgrade");
        helper.assertFalse(menu.getSlot(4).mayPlace(BowGameTestSupport.createSpellScroll(SpellRegistry.MAGIC_MISSILE_SPELL.get())),
                "Non-arrow spells must be rejected");
        for (int i = 1; i <= 3; i++) {
            var upgrade = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get());
            helper.assertTrue(menu.getSlot(i).mayPlace(upgrade), "Each adjustment slot must accept the shared slot upgrade");
            menu.getSlot(i).set(upgrade);
        }
        helper.assertTrue(ElementalBow.getEnabledCalibrationScrollSlotCount(bow) == 4, "Three upgrades must unlock four slots");
        for (int i = 4; i < 8; i++) {
            helper.assertTrue(menu.getSlot(i).mayPlace(scroll), "Duplicate arrow spells must be accepted");
            menu.getSlot(i).set(scroll.copy());
        }
        { var tag = bow.getOrCreateTag();
            tag.putString("ElementalBowShotMode", "magic");
            tag.putString("ElementalBowMode", ElementalBow.selectionIdForSlot(3).toString());
        }
        for (int i = 1; i <= 3; i++) menu.getSlot(i).set(ItemStack.EMPTY);
        helper.assertTrue(ElementalBow.getDisplayedSpellProfile(bow) == null, "An out-of-range selection must become physical");
        for (int i = 5; i < 8; i++) {
            helper.assertFalse(menu.getSlot(i).mayPlace(scroll), "Inactive slots must reject new scrolls");
            helper.assertTrue(menu.getSlot(i).mayPickup(player), "Inactive scrolls must remain extractable");
            var extracted = menu.getSlot(i).remove(1);
            helper.assertTrue(ItemStack.isSameItemSameTags(scroll, extracted), "Shrinking must preserve every scroll");
            helper.assertTrue(ElementalBow.getCalibrationScroll(bow, i - 4, helper.getLevel().registryAccess()).isEmpty(),
                    "Extraction must update saved storage without duplication");
        }
        helper.assertFalse(ISpellContainer.isSpellContainer(bow), "Bench operations must not create a bow spell container");
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void elementalBowDefinitionCodecAcceptsMinimalEntries(GameTestHelper helper) {
        var json = JsonParser.parseString("{\"values\":[{\"spell\":\"irons_spellbooks:fire_arrow\"},{\"spell\":\"irons_spellbooks:magic_arrow\",\"required_draw_ticks\":7}]}");
        var values = ElementalBowModeList.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(false, message -> { throw new IllegalStateException(message); }).values();
        helper.assertTrue(values.size() == 2 && values.get(0).requiredDrawTicks() == 20
                && values.get(1).requiredDrawTicks() == 7, "Only spell and optional draw ticks must be required");
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void elementalBowHeatSurvivesPlayerSaveWithoutLegacyState(GameTestHelper helper) {
        var player = BowGameTestSupport.createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_save");
        ElementalBowOverheatManager.applyOverheatAfterCast(player, 160);
        var expected = ElementalBowOverheatManager.getState(player);
        var saved = new CompoundTag();
        player.saveWithoutId(saved);
        var restored = BowGameTestSupport.createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_load");
        restored.load(saved);
        helper.assertTrue(ElementalBowOverheatManager.getState(restored).equals(expected),
                "Shared overheat must survive player serialization");
        restored.getPersistentData().put("ApprenticeCodexElementalBowOverheat", new CompoundTag());
        restored.getPersistentData().put("ApprenticeCodexElementalBowOverheatObserved", new CompoundTag());
        ElementalBowOverheatManager.cleanLegacyData(restored);
        helper.assertFalse(restored.getPersistentData().contains("ApprenticeCodexElementalBowOverheat"),
                "Legacy school heat must be cleaned");
        helper.assertTrue(ElementalBowOverheatManager.getState(restored).equals(expected),
                "Legacy cleanup must preserve shared heat");
        var clone = BowGameTestSupport.createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_clone");
        ElementalBowOverheatSyncEvents.onPlayerClone(
                new PlayerEvent.Clone(clone, restored, false));
        helper.assertTrue(ElementalBowOverheatManager.getState(clone).equals(expected),
                "Non-death clones must preserve shared heat");
        ElementalBowOverheatSyncEvents.onPlayerClone(
                new PlayerEvent.Clone(clone, restored, true));
        helper.assertFalse(ElementalBowOverheatManager.getState(clone).active(), "Death must reset shared heat");
        helper.succeed();
    }
}
