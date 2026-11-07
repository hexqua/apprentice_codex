package jp.aquafactory.apprenticecodex.gametest.spell.summonweapon.bound;

import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.boundweapon.BoundBowItem;
import jp.aquafactory.apprenticecodex.spell.boundbow.BoundBow;
import jp.aquafactory.apprenticecodex.spell.boundbow.BoundBowManager;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

import static jp.aquafactory.apprenticecodex.gametest.spell.summonweapon.bound.BoundWeaponTestSupport.*;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class BoundBowCastingGameTests {
    private BoundBowCastingGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void boundBowPowerLevelHonorsServerCap(GameTestHelper helper) {
        try (var ignored = ApprenticeCodexServerConfig.useBoundBowConfigOverrideForGameTest(4, 25.0F)) {
            helper.assertTrue(BoundBow.getPowerLevelForSpellPower(400.0F) == 4,
                    "Bound Bow Power level should be capped by server config");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void boundBowSnapshotsSummonDamageIntoArrowBaseDamage(GameTestHelper helper) {
        var player = createPlayer(helper, "bound_bow_summon_damage_test");
        var magicData = resolveMagicData(helper, player);
        try (var ignored = ApprenticeCodexServerConfig.useBoundBowConfigOverrideForGameTest(6, 25.0F)) {
            magicData.setMana(40.0F);
            var bow = BoundBowItem.create(UUID.randomUUID(), 1, helper.getLevel().registryAccess(), 1.5F);
            player.setItemInHand(InteractionHand.MAIN_HAND, bow);

            bow.getItem().releaseUsing(bow, helper.getLevel(), player, bow.getUseDuration(player) - 20);

            var arrow = getSingleArrow(helper, player);
            helper.assertTrue(Math.abs(arrow.getBaseDamage() - 3.25D) < 0.0001D,
                    "Bound Bow arrow base damage should pre-adjust snapshotted Summon Damage for Power");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void boundBowLegacyCreateUsesNeutralSummonDamage(GameTestHelper helper) {
        var player = createPlayer(helper, "bound_bow_neutral_summon_damage_test");
        var magicData = resolveMagicData(helper, player);
        try (var ignored = ApprenticeCodexServerConfig.useBoundBowConfigOverrideForGameTest(6, 25.0F)) {
            magicData.setMana(40.0F);
            var bow = BoundBowItem.create(UUID.randomUUID(), 1, helper.getLevel().registryAccess());
            player.setItemInHand(InteractionHand.MAIN_HAND, bow);

            bow.getItem().releaseUsing(bow, helper.getLevel(), player, bow.getUseDuration(player) - 20);

            var arrow = getSingleArrow(helper, player);
            helper.assertTrue(Math.abs(arrow.getBaseDamage() - 2.0D) < 0.0001D,
                    "Bound Bow legacy factory should keep neutral Summon Damage");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void boundBowGreaterConjurersTalismanSkipsRecastCooldown(GameTestHelper helper) {
        var player = createPlayer(helper, "bound_bow_greater_conjurer_cooldown_test");
        var magicData = resolveMagicData(helper, player);
        equipGreaterConjurersTalisman(player);

        BoundBowManager.activate(player, 1, CastSource.SPELLBOOK, magicData, boundBow(), 1);
        var recast = magicData.getPlayerRecasts().getRecastInstance(boundBow().getSpellId());
        helper.assertTrue(recast != null, "Bound Bow should create an active recast");

        magicData.getPlayerRecasts().removeRecast(recast, RecastResult.TIMEOUT);

        helper.assertFalse(magicData.getPlayerCooldowns().isOnCooldown(boundBow()),
                "Greater Conjurer's Talisman should suppress Bound Bow cooldown when the recast ends");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void boundBowGreaterConjurersTalismanSkipsManualRecastCooldown(GameTestHelper helper) {
        var player = createPlayer(helper, "bound_bow_greater_conjurer_manual_recast_test");
        var magicData = resolveMagicData(helper, player);
        equipGreaterConjurersTalisman(player);

        BoundBowManager.activate(player, 1, CastSource.SPELLBOOK, magicData, boundBow(), 1);
        boundBow().castSpell(helper.getLevel(), 1, player, CastSource.SPELLBOOK, true);

        helper.assertFalse(magicData.getPlayerRecasts().hasRecastForSpell(boundBow()),
                "Manual Bound Bow recast should remove the active recast");
        helper.assertFalse(magicData.getPlayerCooldowns().isOnCooldown(boundBow()),
                "Greater Conjurer's Talisman should suppress Bound Bow cooldown after manual recast deactivation");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void boundBowConsumesManaToForgeArrowWithoutAmmo(GameTestHelper helper) {
        var player = createPlayer(helper, "bound_bow_forge_arrow_test");
        var magicData = resolveMagicData(helper, player);
        try (var ignored = ApprenticeCodexServerConfig.useBoundBowConfigOverrideForGameTest(6, 25.0F)) {
            magicData.setMana(40.0F);
            var bow = BoundBowItem.create(UUID.randomUUID(), 0, helper.getLevel().registryAccess());
            player.setItemInHand(InteractionHand.MAIN_HAND, bow);

            var beforeArrows = countArrows(helper, player);
            bow.getItem().releaseUsing(bow, helper.getLevel(), player, bow.getUseDuration(player) - 20);

            helper.assertTrue(magicData.getMana() == 15.0F,
                    "Bound Bow should consume configured mana when forging an arrow");
            helper.assertTrue(countArrows(helper, player) == beforeArrows + 1,
                    "Bound Bow should fire an arrow when mana can forge one");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void boundBowCannotStartWithoutAmmoOrMana(GameTestHelper helper) {
        var player = createPlayer(helper, "bound_bow_no_mana_test");
        var magicData = resolveMagicData(helper, player);
        try (var ignored = ApprenticeCodexServerConfig.useBoundBowConfigOverrideForGameTest(6, 25.0F)) {
            magicData.setMana(10.0F);
            var bow = BoundBowItem.create(UUID.randomUUID(), 0, helper.getLevel().registryAccess());
            player.setItemInHand(InteractionHand.MAIN_HAND, bow);

            var result = bow.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(!result.getResult().consumesAction() && !player.isUsingItem()
                            && countArrows(helper, player) == 0 && magicData.getMana() == 10.0F,
                    "Bound Bow should not start drawing without ammo or enough mana");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void boundBowUsesVanillaArrowBeforeForging(GameTestHelper helper) {
        var player = createPlayer(helper, "bound_bow_vanilla_arrow_test");
        var magicData = resolveMagicData(helper, player);
        try (var ignored = ApprenticeCodexServerConfig.useBoundBowConfigOverrideForGameTest(6, 25.0F)) {
            magicData.setMana(40.0F);
            var bow = BoundBowItem.create(UUID.randomUUID(), 0, helper.getLevel().registryAccess());
            var arrows = new ItemStack(Items.ARROW, 2);
            player.setItemInHand(InteractionHand.MAIN_HAND, bow);
            player.getInventory().items.set(1, arrows);

            bow.getItem().releaseUsing(bow, helper.getLevel(), player, bow.getUseDuration(player) - 20);

            helper.assertTrue(magicData.getMana() == 40.0F,
                    "Bound Bow should not consume mana while a vanilla arrow is available");
            helper.assertTrue(player.getInventory().items.get(1).getCount() == 1,
                    "Bound Bow should consume a vanilla arrow first");
            getSingleArrow(helper, player);
        }
        helper.succeed();
    }
}
