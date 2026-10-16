package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsAttackState;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsAttackState.Input;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsAttackState.UseAction;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
@SuppressWarnings("unused") // GameTestはアノテーション走査で登録される。
public final class PsychicDriveGauntletsItemGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";
    private PsychicDriveGauntletsItemGameTests() {}

    private static FakePlayer player(GameTestHelper helper) {
        var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(helper,
                new BlockPos(1, 18, 1), "psychic_item");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemRegistry.PSYCHIC_DRIVE_GAUNTLETS.get()));
        return player;
    }

    @GameTest(template = TEMPLATE)
    public static void normalAttackRequiresAndSpendsManaOnce(GameTestHelper helper) {
        var player = player(helper);
        var data = MagicData.getPlayerMagicData(player);
        int cost = ApprenticeCodexServerConfig.psychicDriveGauntletsAttackManaCost();
        helper.assertTrue(cost == 20, "Normal attack default mana cost must be 20");
        data.setMana(cost - 1);
        var state = new PsychicDriveGauntletsAttackState();
        helper.assertTrue(!state.input(player, Input.PRESS, 100) && !state.active()
                        && data.getMana() == cost - 1, "Insufficient mana must reject without spending or reserving an arm");
        state.input(player, Input.RELEASE, 100);
        data.setMana(cost);
        helper.assertTrue(state.input(player, Input.PRESS, 101) && data.getMana() == 0,
                "An exact mana balance must pay immediately on accepted input");
        helper.assertTrue(!state.input(player, Input.PRESS, 101) && data.getMana() == 0,
                "Repeated press must not double spend");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void chargeReservesCapacityAndPaysAtMostTwentyTicks(GameTestHelper helper) {
        var player = player(helper);
        var data = MagicData.getPlayerMagicData(player);
        int cost = ApprenticeCodexServerConfig.psychicDriveGauntletsChargeManaCostPerTick();
        helper.assertTrue(cost == 3, "Charge default mana cost per tick must be three");
        data.setMana(cost * 20 - 1);
        var denied = new PsychicDriveGauntletsAttackState();
        helper.assertTrue(!denied.useInput(player, UseAction.START, InteractionHand.MAIN_HAND, 1, 100)
                        && !denied.charging() && data.getMana() == cost * 20 - 1,
                "Starting a charge must require the entire twenty-tick cost");
        data.setMana(cost * 20);
        var state = new PsychicDriveGauntletsAttackState();
        helper.assertTrue(state.useInput(player, UseAction.START, InteractionHand.MAIN_HAND, 1, 100)
                        && data.getMana() == cost * 20, "Starting must check capacity without paying up front");
        state.tick(player, 105);
        helper.assertTrue(data.getMana() == cost * 15, "Five elapsed ticks must consume five tick payments");
        state.useInput(player, UseAction.START, InteractionHand.MAIN_HAND, 1, 105);
        state.tick(player, 105);
        helper.assertTrue(data.getMana() == cost * 15, "Duplicate session and tick must not restart or double spend");
        state.tick(player, 120);
        data.setMana(10);
        state.tick(player, 140);
        helper.assertTrue(state.charging() && data.getMana() == 10,
                "Holding a fully charged attack must not consume additional mana");
        helper.assertTrue(state.releaseUse(player, 140), "Fully charged release must remain valid");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void chargeSurvivesManaDepletionAndPaysPartialRelease(GameTestHelper helper) {
        var player = player(helper);
        var data = MagicData.getPlayerMagicData(player);
        int cost = ApprenticeCodexServerConfig.psychicDriveGauntletsChargeManaCostPerTick();
        data.setMana(cost * 20);
        var state = new PsychicDriveGauntletsAttackState();
        helper.assertTrue(state.beginUse(player, InteractionHand.MAIN_HAND, 100), "Funded charge must start");
        helper.assertTrue(state.releaseUse(player, 110) && data.getMana() == cost * 10,
                "Partial release must pay only elapsed charging ticks even before a tick callback");
        data.setMana(cost * 20);
        state = new PsychicDriveGauntletsAttackState();
        helper.assertTrue(state.beginUse(player, InteractionHand.MAIN_HAND, 200), "Second charge must start");
        data.setMana(1);
        state.tick(player, 220);
        helper.assertTrue(state.charging() && data.getMana() == 0 && state.releaseUse(player, 220),
                "External mana depletion must clamp to zero and preserve a full charge");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void enchantmentsAndNonDamageableAnvilMerge(GameTestHelper helper) {
        var player = player(helper);
        var stack = player.getMainHandItem();
        var item = stack.getItem();
        helper.assertTrue(stack.getRarity() == Rarity.EPIC && stack.has(DataComponents.FIRE_RESISTANT)
                        && !stack.isDamageableItem() && item.getEnchantmentValue(stack) == 22 && stack.isEnchantable(),
                "Gauntlets must be epic, fire resistant, non-damageable and enchantable at value 22");
        var registry = helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        for (var key : List.of(Enchantments.SHARPNESS, Enchantments.SMITE, Enchantments.BANE_OF_ARTHROPODS,
                Enchantments.KNOCKBACK, Enchantments.FIRE_ASPECT, Enchantments.LOOTING)) {
            var enchantment = registry.getHolderOrThrow(key);
            helper.assertTrue(stack.supportsEnchantment(enchantment) && stack.isPrimaryItemFor(enchantment),
                    "Sword combat enchantment must be applicable: " + key.location());
        }
        for (var key : List.of(Enchantments.SWEEPING_EDGE, Enchantments.UNBREAKING, Enchantments.MENDING,
                Enchantments.VANISHING_CURSE, Enchantments.EFFICIENCY, Enchantments.POWER)) {
            var enchantment = registry.getHolderOrThrow(key);
            var book = new ItemStack(Items.ENCHANTED_BOOK);
            book.enchant(enchantment, 1);
            helper.assertTrue(!stack.supportsEnchantment(enchantment) && !stack.isPrimaryItemFor(enchantment)
                            && !stack.isBookEnchantable(book),
                    "Excluded enchantment must be rejected by all application paths: " + key.location());
        }
        var sharpness = registry.getHolderOrThrow(Enchantments.SHARPNESS);
        var first = stack.copy();
        first.enchant(sharpness, 1);
        var second = first.copy();
        var menu = new AnvilMenu(0, player.getInventory());
        menu.getSlot(0).set(first);
        menu.getSlot(1).set(second);
        menu.createResult();
        helper.assertTrue(menu.getSlot(2).getItem().getEnchantmentLevel(sharpness) == 2,
                "Two non-damageable gauntlets must merge enchantments at an anvil");
        var ids = new ArrayList<>(List.of(ResourceLocation.fromNamespaceAndPath("apprenticecodex", "wisdom")));
        if (ModList.get().isLoaded("malum")) {
            ids.add(ResourceLocation.fromNamespaceAndPath("malum", "spirit_plunder"));
            ids.add(ResourceLocation.fromNamespaceAndPath("malum", "haunted"));
            var animated = registry.getHolder(ResourceLocation.fromNamespaceAndPath("malum", "animated")).orElseThrow();
            helper.assertTrue(!stack.supportsEnchantment(animated) && !stack.isPrimaryItemFor(animated),
                    "Animated must remain excluded even with Malum loaded");
        }
        for (var id : ids) {
            var enchantment = registry.getHolder(id).orElseThrow();
            var book = new ItemStack(Items.ENCHANTED_BOOK);
            book.enchant(enchantment, 1);
            menu.getSlot(0).set(stack.copy());
            menu.getSlot(1).set(book);
            menu.createResult();
            helper.assertTrue(menu.getSlot(2).getItem().getEnchantmentLevel(enchantment) == 1,
                    "Anvil must apply supported enchantment books: " + id);
        }
        helper.succeed();
    }
}
