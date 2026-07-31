package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.gui.overlays.SpellSelection;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.malum.MalumMnemonicBladeBridge;
import jp.aquafactory.apprenticecodex.item.swingstaff.SoulstainedSteelSwingcastStaff;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ApprenticeCodexSoulstainedSteelSwingcastStaffGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";
    private static final ResourceLocation HEX_BOLT = ResourceLocation.fromNamespaceAndPath("malum", "hex_bolt");
    private static final ResourceLocation SPIRIT_INFUSION_RECIPE = ResourceLocation.fromNamespaceAndPath(
            ApprenticeCodex.MODID, "malum/spirit_infusion/soulstained_steel_swingcast_staff");

    private ApprenticeCodexSoulstainedSteelSwingcastStaffGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void soulstainedSteelSwingcastStaffExposesRequestedItemContract(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (SoulstainedSteelSwingcastStaff) ItemRegistry.SOULSTAINED_STEEL_SWINGCAST_STAFF.get();
            var stack = new ItemStack(item);
            item.initializeSpellContainer(stack);

            helper.assertFalse(stack.isDamageableItem(), "Soulstained Steel Swingcast Staff should have infinite durability");
            helper.assertTrue(stack.getRarity() == Rarity.COMMON,
                    "Soulstained Steel Swingcast Staff should have common rarity");
            helper.assertTrue(item.getEnchantmentValue(stack) == SoulstainedSteelSwingcastStaff.ENCHANTMENT_VALUE,
                    "Soulstained Steel Swingcast Staff enchantment value should match the Soulstained Steel Spell Amplifier");
            helper.assertFalse(ISpellContainer.isSpellContainer(stack),
                    "Soulstained Steel Swingcast Staff should not use an Iron's spell container");
            var spiritInfusionRecipe = helper.getLevel().getRecipeManager().byKey(SPIRIT_INFUSION_RECIPE);
            if (ModList.get().isLoaded("malum")) {
                helper.assertTrue(spiritInfusionRecipe.isPresent(),
                        "Soulstained Steel Swingcast Staff Spirit Infusion recipe should be loaded with Malum");
            } else {
                helper.assertTrue(spiritInfusionRecipe.isEmpty(),
                        "Soulstained Steel Swingcast Staff Spirit Infusion recipe should not be loaded without Malum");
            }

            var modifiers = item.getAttributeModifiers(EquipmentSlot.MAINHAND, stack);
            var attackDamage = modifiers.get(Attributes.ATTACK_DAMAGE).stream()
                    .mapToDouble(net.minecraft.world.entity.ai.attributes.AttributeModifier::getAmount)
                    .sum();
            var attackSpeed = modifiers.get(Attributes.ATTACK_SPEED).stream()
                    .mapToDouble(net.minecraft.world.entity.ai.attributes.AttributeModifier::getAmount)
                    .sum();
            helper.assertTrue(Math.abs(1.0D + attackDamage - 3.0D) < 1.0e-6D,
                    "Soulstained Steel Swingcast Staff displayed attack damage should be 3");
            helper.assertTrue(Math.abs(4.0D + attackSpeed - 1.6D) < 1.0e-6D,
                    "Soulstained Steel Swingcast Staff displayed attack speed should be 1.6");

            var tooltip = new ArrayList<net.minecraft.network.chat.Component>();
            item.appendHoverText(stack, helper.getLevel(), tooltip, TooltipFlag.Default.NORMAL);
            helper.assertTrue(tooltip.size() == 3,
                    "Soulstained Steel Swingcast Staff should append one ability line after the common weapon help");
            helper.assertTrue(tooltip.get(2).getContents() instanceof TranslatableContents contents
                            && "item.apprenticecodex.soulstained_steel_swingcast_staff.desc".equals(contents.getKey()),
                    "Soulstained Steel Swingcast Staff ability tooltip should occupy the Swingcast common-help position");
        });
    }

    @GameTest(template = TEMPLATE)
    public static void soulstainedSteelSwingcastStaffResolvesBladeCountFromMana(GameTestHelper helper) {
        helper.succeedIf(() -> {
            helper.assertTrue(SoulstainedSteelSwingcastStaff.resolveBladeCount(9.99F) == 0,
                    "Less than 10 mana should not fire a Mnemonic Blade");
            helper.assertTrue(SoulstainedSteelSwingcastStaff.resolveBladeCount(10.0F) == 1,
                    "10 mana should fire one Mnemonic Blade");
            helper.assertTrue(SoulstainedSteelSwingcastStaff.resolveBladeCount(29.99F) == 2,
                    "Less than 30 mana should fire two Mnemonic Blades");
            helper.assertTrue(SoulstainedSteelSwingcastStaff.resolveBladeCount(30.0F) == 3,
                    "30 mana should fire three Mnemonic Blades");
            helper.assertTrue(SoulstainedSteelSwingcastStaff.resolveBladeCount(100.0F) == 3,
                    "Mnemonic Blade burst size should be capped at three");
        });
    }

    @GameTest(template = TEMPLATE)
    public static void soulstainedSteelSwingcastStaffRightClickUsesSelectedSpellAndDefersToPriorityOffhand(
            GameTestHelper helper
    ) {
        helper.succeedIf(() -> {
            var staff = new ItemStack(ItemRegistry.SOULSTAINED_STEEL_SWINGCAST_STAFF.get());
            var amplifier = new ItemStack(ItemRegistry.SOULSTAINED_STEEL_SPELL_AMPLIFIER.get());
            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
            ISpellContainer.createImbuedContainer(spell, 1, amplifier);

            var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(
                    helper,
                    new BlockPos(0, 4, 0),
                    "soulstained_staff_right_click"
            );
            player.setItemInHand(InteractionHand.MAIN_HAND, staff);
            player.setItemInHand(InteractionHand.OFF_HAND, amplifier);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null,
                    "Soulstained Steel Swingcast Staff right-click test requires MagicData");
            magicData.setMana(1000.0F);
            magicData.getSyncedData().setSpellSelection(new SpellSelection(SpellSelectionManager.OFFHAND, 0));

            var selection = new SpellSelectionManager(player).getSelection();
            helper.assertTrue(selection != null && selection.spellData.getSpell() == spell,
                    "Soulstained Steel Swingcast Staff should resolve the spell imbued into the offhand amplifier");
            var result = staff.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Soulstained Steel Swingcast Staff right click should cast the selected spell but got "
                            + result.getResult());
            helper.assertTrue(ItemStack.isSameItemSameTags(magicData.getPlayerCastingItem(), staff),
                    "Soulstained Steel Swingcast Staff should remain the casting item");
            helper.assertTrue(SpellSelectionManager.MAINHAND.equals(magicData.getCastingEquipmentSlot()),
                    "Soulstained Steel Swingcast Staff right click should mark the mainhand casting slot");
            helper.assertFalse(ISpellContainer.isSpellContainer(staff),
                    "Soulstained Steel Swingcast Staff right click should not create an Iron's spell container");

            magicData.resetCastingState();
            magicData.getPlayerCooldowns().removeCooldown(spell.getSpellId());
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD));
            var deferredResult = staff.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(deferredResult.getResult() == net.minecraft.world.InteractionResult.PASS,
                    "Soulstained Steel Swingcast Staff should defer to a priority offhand item but got "
                            + deferredResult.getResult());
            helper.assertFalse(magicData.isCasting(),
                    "Soulstained Steel Swingcast Staff should not cast before a priority offhand item");
        });
    }

    @GameTest(template = TEMPLATE)
    public static void soulstainedSteelSwingcastStaffFiresAvailableMnemonicBlades(GameTestHelper helper) {
        var item = (SoulstainedSteelSwingcastStaff) ItemRegistry.SOULSTAINED_STEEL_SWINGCAST_STAFF.get();
        var stack = new ItemStack(item);
        var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(
                helper,
                new BlockPos(0, 4, 0),
                "soulstained_staff_burst"
        );
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Soulstained Steel Swingcast Staff test requires MagicData");
        magicData.setMana(25.0F);
        var before = countHexBolts(helper);

        var triggered = item.tryTriggerSpellOnSwing(player, InteractionHand.MAIN_HAND, true);
        if (!MalumMnemonicBladeBridge.isAvailable()) {
            helper.assertFalse(triggered, "Soulstained Steel Swingcast Staff should stay inert without Malum");
            helper.assertTrue(Math.abs(magicData.getMana() - 25.0F) < 1.0e-6F,
                    "Missing Malum should not consume mana");
            helper.succeed();
            return;
        }

        helper.assertTrue(triggered, "Soulstained Steel Swingcast Staff should fire when Malum is loaded");
        helper.assertTrue(Math.abs(magicData.getMana() - 5.0F) < 1.0e-6F,
                "Two Mnemonic Blades should consume 20 mana");
        helper.runAfterDelay(1, () -> {
            helper.assertTrue(countHexBolts(helper) - before == 2,
                    "25 mana should spawn exactly two Malum Mnemonic Blades");
            helper.succeed();
        });
    }

    private static int countHexBolts(GameTestHelper helper) {
        var count = 0;
        for (var entity : helper.getLevel().getAllEntities()) {
            if (HEX_BOLT.equals(ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()))) {
                count++;
            }
        }
        return count;
    }
}
