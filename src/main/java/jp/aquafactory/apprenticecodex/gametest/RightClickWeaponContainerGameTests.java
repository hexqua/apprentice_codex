package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.enchantment.Enchantments;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.utility.PresetSpellContainerStateHelper;
import jp.aquafactory.apprenticecodex.utility.SpellCalibrationImbueHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class RightClickWeaponContainerGameTests {
    private RightClickWeaponContainerGameTests() {
    }

    @GameTest(template = "gametest/basic_floor")
    public static void mithrilDiscardsLegacyContainersWithoutChangingAdjustments(GameTestHelper helper) {
        var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(
                helper, new BlockPos(0, 2, 0), "mithril_container_cleanup");
        var item = ItemRegistry.MITHRIL_FREECAST_STAFF.get();
        var enchantment = helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                .getHolderOrThrow(Enchantments.WISDOM);
        // 空・魔法入り・復元データのみの全てを処理し、他の保存領域を壊さない。
        for (var variant = 0; variant < 3; ++variant) {
            var stack = new ItemStack(item);
            stack.enchant(enchantment, 1);
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                    stack, 0, new ItemStack(SILVER_RING.get())), "Silver Ring adjustment must be accepted");
            var originalData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            var container = ISpellContainer.create(1, true, false).mutableCopy();
            var spellData = new SpellData(SpellRegistry.MAGIC_MISSILE_SPELL.get(), 3);
            if (variant == 1) {
                container.addSpellAtIndex(spellData.getSpell(), spellData.getLevel(), 0, false);
            }
            if (variant != 2) {
                ISpellContainer.set(stack, container.toImmutable());
            }
            PresetSpellContainerStateHelper.rememberOverridden(stack, spellData);
            item.inventoryTick(stack, helper.getLevel(), player, 0, false);
            helper.assertFalse(ISpellContainer.isSpellContainer(stack), "Mithril must discard legacy spell containers");
            helper.assertTrue(originalData.equals(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()),
                    "Cleanup must preserve adjustments and remove remembered spell state");
            helper.assertTrue(stack.getEnchantmentLevel(enchantment) == 1, "Cleanup must preserve enchantments");
            var cleaned = stack.copy();
            SpellCalibrationImbueHelper.prepareTarget(stack);
            item.inventoryTick(stack, helper.getLevel(), player, 0, true);
            helper.assertTrue(ItemStack.isSameItemSameComponents(cleaned, stack),
                    "Bench preparation and repeated ticks must not recreate a container or modify clean data");
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void onlyImbuedWeaponsExposePresetInitialization(GameTestHelper helper) {
        for (var item : new Item[]{ItemRegistry.MITHRIL_FREECAST_STAFF.get(),
                ItemRegistry.REVOLVERCAST_STAFF.get(),
                ItemRegistry.SOULSTAINED_STEEL_SWINGCAST_STAFF.get()}) {
            helper.assertFalse(item instanceof IPresetSpellContainer, "Wheel weapons must not expose preset initialization");
            var stack = item.getDefaultInstance();
            SpellCalibrationImbueHelper.prepareTarget(stack);
            helper.assertFalse(ISpellContainer.isSpellContainer(stack), "Wheel weapons must remain container-free");
        }
        for (var item : new Item[]{ItemRegistry.SMASHCAST_SCEPTER.get(), ItemRegistry.CRYSTAL_BLADED_STAFF.get()}) {
            helper.assertTrue(item instanceof IPresetSpellContainer, "Imbued weapons must retain preset initialization");
        }
        helper.succeed();
    }
}
