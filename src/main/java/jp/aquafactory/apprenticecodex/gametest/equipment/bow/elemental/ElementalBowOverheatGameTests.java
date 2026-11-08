package jp.aquafactory.apprenticecodex.gametest.equipment.bow.elemental;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.MagicHelper;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBow;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowScrollStorage;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.concurrent.atomic.AtomicLong;

import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.elemental.ElementalBowTestSupport.*;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ElementalBowOverheatGameTests {
    private ElementalBowOverheatGameTests() {}

    @GameTest(template = "gametest/bow_casting")
    public static void elementalBowClampsPersistedFutureOverheat(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(12, 2, 12), "elemental_bow_future_overheat_test");

        helper.runAtTickTime(1, () -> {
            ElementalBowOverheatManager.applyOverheatAfterCast(player,
                    40
            );
            var schoolTag = player.getPersistentData()
                    .getCompound("ApprenticeCodexElementalBowSharedOverheat").getCompound("Slot0");
            schoolTag.putLong("ExpireGameTime", player.level().getGameTime() + 72000L);

            var state = ElementalBowOverheatManager.getState(player);

            helper.assertTrue(state.expireGameTime() <= player.level().getGameTime() + 40L,
                    "Elemental Bow stored overheat should be clamped to the last applied duration");
            helper.assertTrue(schoolTag.getLong("ExpireGameTime") == state.expireGameTime(),
                    "Elemental Bow persistent overheat NBT should be rewritten after clamping");
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/bow_casting", batch = "apprenticecodex.elemental_bow_overheat")
    public static void elementalBowSuppressesElementalArrowCooldown(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = useElementalBowSpellConfig(helper)) {
                var player = createEquipmentTestPlayer(helper, new BlockPos(12, 2, 12), "elemental_cooldown_independence");
                var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
                setElementalBowMode(stack, "fire");
                player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                player.getInventory().setItem(1, new ItemStack(Items.ARROW, 3));
                var magic = MagicData.getPlayerMagicData(player);
                magic.setMana(500);
                var spell = SpellRegistry.FIRE_ARROW_SPELL.get();
                // 本来の CD がなくても弓の射撃で新規登録しない。
                stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
                stack.getItem().releaseUsing(stack, helper.getLevel(), player, stack.getUseDuration(player) - 20);
                player.stopUsingItem();
                helper.assertFalse(magic.getPlayerCooldowns().isOnCooldown(spell), "Bow must not register a normal cooldown");
                MagicHelper.MAGIC_MANAGER.addCooldown(player, spell, CastSource.SPELLBOOK);
                var before = magic.getPlayerCooldowns().getSpellCooldowns().get(spell.getSpellId()).getCooldownRemaining();
                helper.assertTrue(before > 0 && magic.getPlayerCooldowns().isOnCooldown(spell), "The independent normal cooldown must be active");
                stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
                stack.getItem().releaseUsing(stack, helper.getLevel(), player, stack.getUseDuration(player) - 20);
                player.stopUsingItem();
                helper.assertTrue(stack.getDamageValue() == 2, "Bow must shoot even during the normal spell cooldown");
                helper.assertTrue(magic.getPlayerCooldowns().getSpellCooldowns().get(spell.getSpellId()).getCooldownRemaining() == before,
                        "Bow must preserve the existing normal cooldown");
            }
        });
    }

    @GameTest(template = "gametest/bow_casting", batch = "apprenticecodex.elemental_bow_overheat")
    public static void elementalBowConsumesAdditionalManaWhileOverheated(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(12, 2, 12), "elemental_bow_overheat_mana_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            setElementalBowShotSelection(stack, "magic", SchoolRegistry.FIRE_RESOURCE);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            player.getInventory().setItem(1, new ItemStack(Items.ARROW, 2));

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Elemental Bow overheat mana test could not resolve player mana data");

            var item = (ElementalBow) stack.getItem();
            ElementalBowScrollStorage.migrate(stack);
            var fireProfile = ElementalBow.getDisplayedSpellProfile(stack);
            helper.assertTrue(fireProfile != null, "Elemental Bow overheat mana test should resolve the active Fire profile");
            var fireArrow = fireProfile != null
                    ? fireProfile.spell()
                    : SpellRegistry.FIRE_ARROW_SPELL.get();
            var baseMana = fireProfile != null ? fireProfile.spell().getManaCost(fireProfile.spellLevel()) : fireArrow.getManaCost(1);

            magicData.setMana(300.0F);
            var initialMana = magicData.getMana();

            ElementalBowOverheatManager.applyOverheatAfterCast(player, fireArrow.getSpellCooldown());

            var extraMana = baseMana * 0.28F;
            helper.assertTrue(extraMana > 0.0F, "Elemental Bow should charge extra mana once Fire overheat is active");

            var overheatedUseResult = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(overheatedUseResult.getResult().consumesAction(),
                    "Elemental Bow should still allow a second overheated draw: " + overheatedUseResult.getResult());
            stack.getItem().releaseUsing(stack, helper.getLevel(), player, stack.getUseDuration(player) - ElementalBow.READY_DRAW_TICKS);
            player.stopUsingItem();

            var manaAfterOverheatedShot = magicData.getMana();
            helper.assertTrue(Math.abs(manaAfterOverheatedShot - (initialMana - baseMana - extraMana)) < 1.0e-3F,
                    "Elemental Bow overheated shot consumed the wrong mana: " + manaAfterOverheatedShot);
            var state = ElementalBowOverheatManager.getState(player);
            helper.assertTrue(state.active() && state.chainDepth() >= 2,
                    "Elemental Bow overheated shot should keep Fire overheat active and deepen the chain: " + state);
        });
    }

    @GameTest(template = "gametest/bow_casting", batch = "apprenticecodex.elemental_bow_overheat")
    public static void elementalBowSharesOverheatAcrossSpellsAndBows(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(12, 2, 12), "elemental_shared_heat");
            var first = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            setElementalBowMode(first, "fire");
            player.setItemInHand(InteractionHand.MAIN_HAND, first);
            ElementalBowOverheatManager.applyOverheatAfterCast(player, 300);
            long expiry = ElementalBowOverheatManager.getState(player).expireGameTime();
            var second = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            setElementalBowMode(second, "ender");
            player.setItemInHand(InteractionHand.MAIN_HAND, second);
            ElementalBowOverheatManager.applyOverheatAfterCast(player, 160);
            var state = ElementalBowOverheatManager.getState(player);
            helper.assertTrue(state.chainDepth() == 2 && state.expireGameTime() == expiry,
                    "Switching bows or spells must not shorten or reset overheat");
            ElementalBowOverheatManager.applyOverheatAfterCast(player, 0);
            helper.assertTrue(ElementalBowOverheatManager.getState(player).expireGameTime() == expiry,
                    "A zero cooldown must not clear existing overheat");
            helper.assertTrue(Mth.equal(ElementalBowOverheatManager.getAdditionalManaCost(player, 100), 132),
                    "All spells must share the same overheat cost at depth three");
        });
    }

    @GameTest(template = "gametest/bow_casting", batch = "apprenticecodex.elemental_bow_overheat", timeoutTicks = 80)
    public static void elementalBowOverheatRefreshesDurationAfterRepeatedCast(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(12, 2, 12), "elemental_heat_refresh");
        var first = new AtomicLong();
        helper.runAtTickTime(1, () -> {
            ElementalBowOverheatManager.applyOverheatAfterCast(player, 100);
            first.set(ElementalBowOverheatManager.getState(player).expireGameTime());
        });
        helper.runAtTickTime(20, () -> {
            ElementalBowOverheatManager.applyOverheatAfterCast(player, 100);
            var state = ElementalBowOverheatManager.getState(player);
            helper.assertTrue(state.chainDepth() == 2 && state.expireGameTime() == first.get() + 19,
                    "Repeated shots must extend expiry from the latest cast");
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/bow_casting", batch = "apprenticecodex.elemental_bow_overheat_mana_config")
    public static void elementalBowAdditionalManaUsesServerConfig(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = useElementalBowConfigOverrideForGameTest(
                    1.0D,
                    0.5D,
                    0.25D,
                    1.0D,
                    0,
                    0)) {
                var player = createEquipmentTestPlayer(helper, new BlockPos(12, 2, 12), "elemental_bow_overheat_mana_config_test");
                ElementalBowOverheatManager.applyOverheatAfterCast(player,
                        100
                );

                var extraMana = ElementalBowOverheatManager.getAdditionalManaCost(player,
                        100.0F
                );
                helper.assertTrue(Math.abs(extraMana - 75.0F) < 1.0e-3F,
                        "Elemental Bow additional mana should use its server config but got " + extraMana);
            }
        });
    }

    @GameTest(template = "gametest/bow_casting", batch = "apprenticecodex.elemental_bow_overheat_duration_config")
    public static void elementalBowOverheatDurationUsesServerConfig(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = useElementalBowConfigOverrideForGameTest(
                    1.0D,
                    0.20D,
                    0.08D,
                    2.0D,
                    30,
                    50)) {
                var player = createEquipmentTestPlayer(helper, new BlockPos(12, 2, 12), "elemental_bow_overheat_duration_config_test");

                ElementalBowOverheatManager.applyOverheatAfterCast(player,
                        10
                );
                var minState = ElementalBowOverheatManager.getState(player);
                helper.assertTrue(minState.active()
                                && minState.expireGameTime() - helper.getLevel().getGameTime() == 30,
                        "Elemental Bow overheat duration should use configured minimum: " + minState);

                ElementalBowOverheatManager.clear(player);
                ElementalBowOverheatManager.applyOverheatAfterCast(player,
                        100
                );
                var capState = ElementalBowOverheatManager.getState(player);
                helper.assertTrue(capState.active()
                                && capState.expireGameTime() - helper.getLevel().getGameTime() == 50,
                        "Elemental Bow overheat duration should use configured cap: " + capState);
            }
        });
    }
}
