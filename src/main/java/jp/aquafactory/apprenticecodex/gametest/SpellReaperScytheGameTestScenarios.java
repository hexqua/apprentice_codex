package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.compat.Curios;
import jp.aquafactory.apprenticecodex.compat.malum.MalumCompatibility;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.item.SpellReaperScytheServerConfig;
import jp.aquafactory.apprenticecodex.gametest.malum.MalumScytheGameTestHelper;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.GameType;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.util.FakePlayer;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.ArrayList;
import java.util.UUID;

final class SpellReaperScytheGameTestScenarios extends ApprenticeCodexGameTestScenarios {
    private static final String BETTER_COMBAT_MOD_ID = "bettercombat";
    private static final ResourceKey<DamageType> MALUM_SCYTHE_SWEEP = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(MalumCompatibility.MOD_ID, "scythe_sweep")
    );
    private static final ResourceKey<DamageType> MALUM_SCYTHE_ASCENSION = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(MalumCompatibility.MOD_ID, "scythe_ascension")
    );
    private static final ResourceKey<Enchantment> MALUM_ASCENSION = malumEnchantmentKey("ascension");
    private static final ResourceKey<Enchantment> MALUM_REBOUND = malumEnchantmentKey("rebound");
    private static final ResourceLocation MALUM_ASCENSION_EFFECT = ResourceLocation.fromNamespaceAndPath(
            MalumCompatibility.MOD_ID,
            "ascension"
    );
    private static final ResourceLocation MALUM_SCYTHE_BOOMERANG = ResourceLocation.fromNamespaceAndPath(
            MalumCompatibility.MOD_ID,
            "scythe_boomerang"
    );
    private static final float DAMAGE_TOLERANCE = 1.0E-4F;

    private SpellReaperScytheGameTestScenarios() {
    }

    static void spellReaperScytheUsesVanillaSweepWithoutMalum(GameTestHelper helper) {
        if (ModList.get().isLoaded(MalumCompatibility.MOD_ID)
                || ModList.get().isLoaded(BETTER_COMBAT_MOD_ID)) {
            helper.succeed();
            return;
        }

        var context = prepareSweepAttack(helper, "spell_reaper_vanilla_sweep");
        performFullyChargedAttack(helper, context);

        assertDamageNear(helper, context.firstBystander(), context.firstBystanderHealth(), 1.0F,
                "Spell Reaper Scythe should retain vanilla sweep damage without Malum");
        helper.assertTrue(context.firstBystander().getLastDamageSource() != null
                        && !context.firstBystander().getLastDamageSource().is(MALUM_SCYTHE_SWEEP),
                "Vanilla sweep should not use Malum scythe sweep damage");
        helper.succeed();
    }

    static void spellReaperScytheUsesOneMalumSweepWithMalum(GameTestHelper helper) {
        if (!ModList.get().isLoaded(MalumCompatibility.MOD_ID)) {
            helper.succeed();
            return;
        }

        var context = prepareSweepAttack(helper, "spell_reaper_malum_sweep");
        var sweepingRatio = context.player().getAttributeValue(Attributes.SWEEPING_DAMAGE_RATIO);
        var expectedSweepDamage = (float) (context.player().getAttributeValue(Attributes.ATTACK_DAMAGE)
                * (0.5D + sweepingRatio * 0.33D));
        performFullyChargedAttack(helper, context);

        assertDamageNear(helper, context.firstBystander(), context.firstBystanderHealth(), expectedSweepDamage,
                "Spell Reaper Scythe should deal one Malum sweep hit to the first bystander");
        assertDamageNear(helper, context.secondBystander(), context.secondBystanderHealth(), expectedSweepDamage,
                "Spell Reaper Scythe should deal one Malum sweep hit to the second bystander");
        assertMalumSweepSource(helper, context.firstBystander());
        assertMalumSweepSource(helper, context.secondBystander());
        helper.succeed();
    }

    static void spellReaperScytheIsRecognizedByMalumSoulDataHandler(GameTestHelper helper) {
        if (!ModList.get().isLoaded(MalumCompatibility.MOD_ID)) {
            helper.succeed();
            return;
        }

        var player = new FakePlayer(
                helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "spell_reaper_malum_scythe_weapon")
        );
        var source = player.damageSources().playerAttack(player);

        var spellReaperScythe = new ItemStack(ItemRegistry.SPELL_REAPER_SCYTHE.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, spellReaperScythe);
        var resolvedSpellReaperScythe = MalumScytheGameTestHelper.getScytheWeapon(source, player);
        helper.assertTrue(resolvedSpellReaperScythe == player.getMainHandItem(),
                "Malum should resolve the actual Spell Reaper Scythe stack");

        var malumScytheItem = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(
                MalumCompatibility.MOD_ID,
                "soul_stained_steel_scythe"
        ));
        helper.assertTrue(malumScytheItem != Items.AIR, "Missing Malum scythe for GameTest");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(malumScytheItem));
        var resolvedMalumScythe = MalumScytheGameTestHelper.getScytheWeapon(source, player);
        helper.assertTrue(resolvedMalumScythe == player.getMainHandItem(),
                "Malum should keep resolving its native scythe stack");

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
        helper.assertTrue(MalumScytheGameTestHelper.getScytheWeapon(source, player).isEmpty(),
                "Malum should not resolve an ordinary sword as a scythe");
        helper.succeed();
    }

    static void spellReaperScytheDoesNotSweepWithNarrowEdge(GameTestHelper helper) {
        assertMalumNecklacePreventsSweep(helper, "necklace_of_the_narrow_edge", "narrow_edge");
    }

    static void spellReaperScytheDoesNotSweepWithHiddenBlade(GameTestHelper helper) {
        assertMalumNecklacePreventsSweep(helper, "necklace_of_the_hidden_blade", "hidden_blade");
    }

    static void spellReaperScytheRightClickWithoutAscensionIsNoOp(GameTestHelper helper) {
        // Epic Fightの入力・表示契約はScytheEpicFightGameTestsで検証する。
        if (ModList.get().isLoaded("epicfight")) { helper.succeed(); return; }
        var player = prepareUsePlayer(helper, "spell_reaper_no_ascension");
        var stack = player.getMainHandItem();
        var item = stack.getItem();
        var result = item.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);

        helper.assertTrue(result.getResult() == InteractionResult.CONSUME && player.isUsingItem(),
                "Spell Reaper Scythe without Ascension should begin charging");
        player.releaseUsingItem();
        helper.assertFalse(player.getCooldowns().isOnCooldown(item),
                "Spell Reaper Scythe without Ascension should not start a cooldown");
        helper.assertTrue(player.getActiveEffects().stream().noneMatch(SpellReaperScytheGameTestScenarios::isAscensionEffect),
                "Spell Reaper Scythe without Ascension should not grant the Ascension effect");
        helper.succeed();
    }

    static void spellReaperScytheRightClickWithReboundThrowsImmediately(GameTestHelper helper) {
        // Epic Fightの入力・表示契約はScytheEpicFightGameTestsで検証する。
        if (ModList.get().isLoaded("epicfight")) { helper.succeed(); return; }
        if (!ModList.get().isLoaded(MalumCompatibility.MOD_ID)) {
            helper.succeed();
            return;
        }

        var player = prepareUsePlayer(helper, "spell_reaper_rebound_throw");
        resolveMana(helper, player).setMana(1000);
        var stack = player.getMainHandItem();
        enchantMalum(helper, stack, MALUM_REBOUND, 1);
        var result = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);

        helper.assertTrue(result.getResult() == InteractionResult.CONSUME && !player.isUsingItem(),
                "Rebound should throw immediately without charging");
        var thrown = jp.aquafactory.apprenticecodex.item.spellreaperscythe.ScytheThrowManager.active(player);
        helper.assertTrue(thrown != null && thrown.isRebound(), "Rebound should create the independent thrown scythe");
        thrown.recall();
        helper.assertFalse(player.getCooldowns().isOnCooldown(stack.getItem()),
                "Rebound alone should not start a cooldown");
        helper.assertFalse(hasMalumScytheBoomerang(helper),
                "Rebound alone should not spawn a Malum scythe boomerang");
        helper.succeed();
    }

    static void spellReaperScytheRightClickTriggersMalumAscension(GameTestHelper helper) {
        // Epic Fightの入力・表示契約はScytheEpicFightGameTestsで検証する。
        if (ModList.get().isLoaded("epicfight")) { helper.succeed(); return; }
        if (!ModList.get().isLoaded(MalumCompatibility.MOD_ID)) {
            helper.succeed();
            return;
        }

        var context = prepareAscensionUse(helper, "spell_reaper_ascension", 1);
        var result = context.stack().getItem().use(helper.getLevel(), context.player(), InteractionHand.MAIN_HAND);

        helper.assertTrue(result.getResult().consumesAction(),
                "Ascension should consume Spell Reaper Scythe right-click");
        helper.assertTrue(context.target().getHealth() < context.initialTargetHealth(),
                "Ascension should damage a valid nearby target");
        helper.assertTrue(context.target().getLastDamageSource() != null
                        && context.target().getLastDamageSource().is(MALUM_SCYTHE_ASCENSION),
                "Ascension physical damage should use malum:scythe_ascension");
        helper.assertTrue(context.player().getActiveEffects().stream()
                        .anyMatch(SpellReaperScytheGameTestScenarios::isAscensionEffect),
                "Successful Ascension should grant the Ascension effect");
        helper.assertTrue(Math.abs(resolveMana(helper, context.player()).getMana() - 800.0F) <= DAMAGE_TOLERANCE,
                "Ascension level 1 should consume 200 mana");
        var secondScythe = new ItemStack(ItemRegistry.SPELL_REAPER_SCYTHE.get());
        helper.assertTrue(context.player().getCooldowns().isOnCooldown(secondScythe.getItem()),
                "Ascension cooldown should be shared by every Spell Reaper Scythe stack");
        assertCooldownDuration(helper, context.player(), context.stack().getItem(), 10);
        helper.succeed();
    }

    static void spellReaperScytheAscensionLevelControlsManaCost(GameTestHelper helper) {
        // Epic Fightの入力・表示契約はScytheEpicFightGameTestsで検証する。
        if (ModList.get().isLoaded("epicfight")) { helper.succeed(); return; }
        if (!ModList.get().isLoaded(MalumCompatibility.MOD_ID)) {
            helper.succeed();
            return;
        }

        var context = prepareAscensionUse(helper, "spell_reaper_ascension_three", 3);
        context.stack().getItem().use(helper.getLevel(), context.player(), InteractionHand.MAIN_HAND);
        helper.assertTrue(Math.abs(resolveMana(helper, context.player()).getMana() - 880.0F) <= DAMAGE_TOLERANCE,
                "Ascension level 3 should consume 120 mana");
        assertCooldownDuration(helper, context.player(), context.stack().getItem(), 10);
        helper.succeed();
    }

    static void spellReaperScytheAscensionRejectsInsufficientMana(GameTestHelper helper) {
        // Epic Fightの入力・表示契約はScytheEpicFightGameTestsで検証する。
        if (ModList.get().isLoaded("epicfight")) { helper.succeed(); return; }
        if (!ModList.get().isLoaded(MalumCompatibility.MOD_ID)) {
            helper.succeed();
            return;
        }

        var player = new CapturingActionBarFakePlayer(
                helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "spell_reaper_ascension_no_mana")
        );
        prepareUsePlayer(helper, player);
        var stack = player.getMainHandItem();
        enchantMalum(helper, stack, MALUM_ASCENSION, 2);
        var target = prepareTarget(helper, new BlockPos(2, 2, 3));
        var initialTargetHealth = target.getHealth();
        var magicData = resolveMana(helper, player);
        magicData.setMana(159.0F);

        var result = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);

        helper.assertTrue(result.getResult() == InteractionResult.CONSUME,
                "Insufficient Ascension mana should consume right-click without requesting a hand swing");
        helper.assertFalse(result.getResult().shouldSwing(),
                "Insufficient Ascension mana should not play the right-click hand swing");
        helper.assertTrue(Math.abs(magicData.getMana() - 159.0F) <= DAMAGE_TOLERANCE,
                "Rejected Ascension should not consume mana");
        helper.assertTrue(Math.abs(target.getHealth() - initialTargetHealth) <= DAMAGE_TOLERANCE,
                "Rejected Ascension should not damage targets");
        helper.assertFalse(player.getCooldowns().isOnCooldown(stack.getItem()),
                "Rejected Ascension should not start a cooldown");
        helper.assertTrue(player.getActiveEffects().stream()
                        .noneMatch(SpellReaperScytheGameTestScenarios::isAscensionEffect),
                "Rejected Ascension should not grant the Ascension effect");
        assertInsufficientManaMessage(helper, player.actionBarMessage());

        magicData.setMana(160.0F);
        stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(Math.abs(magicData.getMana()) <= DAMAGE_TOLERANCE,
                "Ascension should activate when current mana exactly equals its cost");
        helper.assertTrue(target.getHealth() < initialTargetHealth,
                "Exact-cost Ascension should retain its attack effect");
        helper.succeed();
    }

    static void spellReaperScytheAscensionUsesServerConfigAndZeroFloor(GameTestHelper helper) {
        // Epic Fightの入力・表示契約はScytheEpicFightGameTestsで検証する。
        if (ModList.get().isLoaded("epicfight")) { helper.succeed(); return; }
        if (!ModList.get().isLoaded(MalumCompatibility.MOD_ID)) {
            helper.succeed();
            return;
        }

        var values = new SpellReaperScytheServerConfig.Values(90, 50, 0);
        try (var ignored = ApprenticeCodexServerConfig.useSpellReaperScytheConfigOverrideForGameTest(values)) {
            var context = prepareAscensionUse(helper, "spell_reaper_ascension_config", 5);
            var magicData = resolveMana(helper, context.player());
            magicData.setMana(0.0F);
            var result = context.stack().getItem().use(
                    helper.getLevel(), context.player(), InteractionHand.MAIN_HAND
            );

            helper.assertTrue(values.ascensionManaCost(1) == 90,
                    "Configured Ascension level 1 mana cost should equal the base cost");
            helper.assertTrue(values.ascensionManaCost(2) == 40,
                    "Configured Ascension level 2 mana cost should apply one reduction");
            helper.assertTrue(values.ascensionManaCost(5) == 0,
                    "Configured Ascension mana cost should not become negative");
            helper.assertTrue(result.getResult().consumesAction(),
                    "Zero-cost Ascension should activate without mana");
            helper.assertTrue(context.target().getHealth() < context.initialTargetHealth(),
                    "Zero-cost Ascension should retain its attack effect");
            helper.assertFalse(context.player().getCooldowns().isOnCooldown(context.stack().getItem()),
                    "Zero configured cooldown should not add a cooldown");
        }
        helper.succeed();
    }

    static void spellReaperScytheAscensionCreativeBypassesManaAndCooldown(GameTestHelper helper) {
        // Epic Fightの入力・表示契約はScytheEpicFightGameTestsで検証する。
        if (ModList.get().isLoaded("epicfight")) { helper.succeed(); return; }
        if (!ModList.get().isLoaded(MalumCompatibility.MOD_ID)) {
            helper.succeed();
            return;
        }

        var context = prepareAscensionUse(helper, "spell_reaper_ascension_creative", 1);
        context.player().gameMode.changeGameModeForPlayer(GameType.CREATIVE);
        var magicData = resolveMana(helper, context.player());
        magicData.setMana(0.0F);
        context.stack().getItem().use(helper.getLevel(), context.player(), InteractionHand.MAIN_HAND);

        helper.assertTrue(context.target().getHealth() < context.initialTargetHealth(),
                "Creative Ascension should activate without mana");
        helper.assertTrue(Math.abs(magicData.getMana()) <= DAMAGE_TOLERANCE,
                "Creative Ascension should not consume mana");
        helper.assertFalse(context.player().getCooldowns().isOnCooldown(context.stack().getItem()),
                "Creative Ascension should not start a cooldown");
        helper.succeed();
    }

    static void spellReaperScytheAscensionTooltipShowsTranslatedNameAndMana(GameTestHelper helper) {
        // Epic Fightの入力・表示契約はScytheEpicFightGameTestsで検証する。
        if (ModList.get().isLoaded("epicfight")) { helper.succeed(); return; }
        if (!ModList.get().isLoaded(MalumCompatibility.MOD_ID)) {
            helper.succeed();
            return;
        }

        var stack = new ItemStack(ItemRegistry.SPELL_REAPER_SCYTHE.get());
        var noAscensionTooltip = new ArrayList<Component>();
        stack.getItem().appendHoverText(
                stack, Item.TooltipContext.of(helper.getLevel()), noAscensionTooltip, TooltipFlag.Default.NORMAL
        );
        helper.assertTrue(noAscensionTooltip.stream().noneMatch(SpellReaperScytheGameTestScenarios::isAscensionCostTooltip),
                "Spell Reaper Scythe without Ascension should not show an Ascension mana cost");

        enchantMalum(helper, stack, MALUM_ASCENSION, 2);
        var tooltip = new ArrayList<Component>();
        stack.getItem().appendHoverText(
                stack, Item.TooltipContext.of(helper.getLevel()), tooltip, TooltipFlag.Default.NORMAL
        );
        var costLine = tooltip.stream()
                .filter(SpellReaperScytheGameTestScenarios::isAscensionCostTooltip)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing Spell Reaper Ascension cost tooltip"));
        var contents = (TranslatableContents) costLine.getContents();
        var args = contents.getArgs();
        helper.assertTrue(args.length == 2 && args[0] instanceof Component,
                "Ascension cost tooltip should receive the translated enchantment name as its first argument");
        helper.assertTrue(args[1] instanceof Component manaComponent
                        && "160".equals(manaComponent.getString())
                        && manaComponent.getStyle().getColor() != null
                        && ChatFormatting.AQUA.getColor() != null
                        && manaComponent.getStyle().getColor().getValue() == ChatFormatting.AQUA.getColor(),
                "Ascension cost tooltip should show the level 2 mana cost in aqua");
        helper.succeed();
    }

    static void spellReaperScytheAscensionWinsOverForcedRebound(GameTestHelper helper) {
        // Epic Fightの入力・表示契約はScytheEpicFightGameTestsで検証する。
        if (ModList.get().isLoaded("epicfight")) { helper.succeed(); return; }
        if (!ModList.get().isLoaded(MalumCompatibility.MOD_ID)) {
            helper.succeed();
            return;
        }

        var context = prepareAscensionUse(helper, "spell_reaper_ascension_priority", 1);
        enchantMalum(helper, context.stack(), MALUM_REBOUND, 1);
        var result = context.stack().getItem().use(helper.getLevel(), context.player(), InteractionHand.MAIN_HAND);

        helper.assertTrue(result.getResult().consumesAction(),
                "Forced Ascension and Rebound should still consume right-click");
        helper.assertTrue(context.target().getHealth() < context.initialTargetHealth(),
                "Ascension should win over a force-applied Rebound enchantment");
        helper.assertFalse(hasMalumScytheBoomerang(helper),
                "Ascension priority should not spawn a Malum scythe boomerang");
        helper.succeed();
    }

    static void spellReaperScytheAscensionUsesMalumCurios(GameTestHelper helper) {
        // Epic Fightの入力・表示契約はScytheEpicFightGameTestsで検証する。
        if (ModList.get().isLoaded("epicfight")) { helper.succeed(); return; }
        if (!ModList.get().isLoaded(MalumCompatibility.MOD_ID)) {
            helper.succeed();
            return;
        }

        var normal = prepareAscensionUse(helper, "spell_reaper_ascension_normal", 1);
        normal.stack().getItem().use(helper.getLevel(), normal.player(), InteractionHand.MAIN_HAND);
        var normalDamage = normal.initialTargetHealth() - normal.target().getHealth();
        normal.target().discard();

        var narrow = prepareAscensionUse(helper, "spell_reaper_ascension_narrow", 1);
        equipMalumCurio(helper, narrow.player(), Curios.NECKLACE_SLOT, "necklace_of_the_narrow_edge");
        narrow.stack().getItem().use(helper.getLevel(), narrow.player(), InteractionHand.MAIN_HAND);
        var narrowDamage = narrow.initialTargetHealth() - narrow.target().getHealth();
        helper.assertTrue(narrowDamage > normalDamage,
                "Narrow Edge should increase Spell Reaper Ascension damage");
        narrow.target().discard();

        var rising = prepareAscensionUse(helper, "spell_reaper_ascension_rising", 1);
        equipMalumCurio(helper, rising.player(), "ring", "ring_of_the_rising_edge");
        rising.stack().getItem().use(helper.getLevel(), rising.player(), InteractionHand.MAIN_HAND);
        helper.assertTrue(rising.target().getDeltaMovement().y >= 0.5D,
                "Rising Edge should launch targets hit by Spell Reaper Ascension");
        helper.succeed();
    }

    static void spellReaperScytheBetterCombatKeepsNormalComboWhenSweepAllowed(GameTestHelper helper) {
        if (!ModList.get().isLoaded(BETTER_COMBAT_MOD_ID)) {
            helper.succeed();
            return;
        }

        var context = prepareSweepAttack(helper, "spell_reaper_bettercombat_normal_combo");
        assertBetterCombatAttack(
                helper,
                context.player(),
                0,
                "HORIZONTAL_PLANE",
                "bettercombat:two_handed_slash_horizontal_right"
        );
        assertBetterCombatAttack(
                helper,
                context.player(),
                1,
                "HORIZONTAL_PLANE",
                "bettercombat:two_handed_slash_horizontal_left"
        );
        helper.succeed();
    }

    static void spellReaperScytheBetterCombatUsesNoSweepCombo(GameTestHelper helper) {
        if (!ModList.get().isLoaded(BETTER_COMBAT_MOD_ID)
                || !ModList.get().isLoaded(MalumCompatibility.MOD_ID)) {
            helper.succeed();
            return;
        }

        var context = prepareSweepAttack(helper, "spell_reaper_bettercombat_no_sweep_combo");
        equipMalumNecklace(helper, context.player(), "necklace_of_the_narrow_edge");
        assertBetterCombatNoSweepCombo(helper, context.player(), "Narrow Edge");

        equipMalumNecklace(helper, context.player(), "necklace_of_the_hidden_blade");
        assertBetterCombatNoSweepCombo(helper, context.player(), "Hidden Blade");

        clearMalumNecklace(context.player());
        assertBetterCombatAttack(
                helper,
                context.player(),
                0,
                "HORIZONTAL_PLANE",
                "bettercombat:two_handed_slash_horizontal_right"
        );
        helper.succeed();
    }

    private static void assertMalumNecklacePreventsSweep(
            GameTestHelper helper,
            String necklaceId,
            String profileSuffix
    ) {
        if (!ModList.get().isLoaded(MalumCompatibility.MOD_ID)) {
            helper.succeed();
            return;
        }

        var context = prepareSweepAttack(helper, "spell_reaper_" + profileSuffix);
        equipMalumNecklace(helper, context.player(), necklaceId);
        performFullyChargedAttack(helper, context);

        helper.assertTrue(context.primaryTarget().getHealth() < context.primaryHealth(),
                "Spell Reaper Scythe should still damage its primary target with " + necklaceId);
        assertDamageNear(helper, context.firstBystander(), context.firstBystanderHealth(), 0.0F,
                "Spell Reaper Scythe should not sweep the first bystander with " + necklaceId);
        assertDamageNear(helper, context.secondBystander(), context.secondBystanderHealth(), 0.0F,
                "Spell Reaper Scythe should not sweep the second bystander with " + necklaceId);
        helper.succeed();
    }

    private static SweepAttackContext prepareSweepAttack(GameTestHelper helper, String profileName) {
        var player = new FullyChargedFakePlayer(
                helper.getLevel(),
                new GameProfile(UUID.randomUUID(), profileName)
        );
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        var playerPos = helper.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(2, 2, 1)));
        player.setPos(playerPos.x, playerPos.y, playerPos.z);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemRegistry.SPELL_REAPER_SCYTHE.get()));
        player.setSprinting(false);
        player.setDeltaMovement(Vec3.ZERO);
        player.setOnGround(true);

        var primaryTarget = prepareTarget(helper, new BlockPos(2, 2, 3));
        var firstBystander = prepareTarget(helper, new BlockPos(3, 2, 3));
        var secondBystander = prepareTarget(helper, new BlockPos(1, 2, 3));
        return new SweepAttackContext(
                player,
                primaryTarget,
                firstBystander,
                secondBystander,
                primaryTarget.getHealth(),
                firstBystander.getHealth(),
                secondBystander.getHealth()
        );
    }

    private static LivingEntity prepareTarget(GameTestHelper helper, BlockPos pos) {
        var target = helper.spawn(EntityType.SHEEP, pos);
        target.setNoAi(true);
        var maxHealth = target.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(100.0D);
        }
        target.setHealth(100.0F);
        return target;
    }

    private static void performFullyChargedAttack(GameTestHelper helper, SweepAttackContext context) {
        context.player().setSprinting(false);
        context.player().setDeltaMovement(Vec3.ZERO);
        context.player().setOnGround(true);
        context.player().fallDistance = 0.0F;
        context.player().walkDistO = context.player().walkDist;
        context.player().setSpeed(0.1F);
        helper.assertTrue(context.player().getAttackStrengthScale(0.5F) > 0.9F,
                "Spell Reaper Scythe sweep test requires a fully charged attack");
        helper.assertTrue(context.player().getMainHandItem().canPerformAction(ItemAbilities.SWORD_SWEEP)
                        == !ModList.get().isLoaded(MalumCompatibility.MOD_ID),
                "Spell Reaper Scythe sweep ability should match Malum availability");
        helper.assertTrue(context.player().getMainHandItem().getSweepHitBox(context.player(), context.primaryTarget())
                        .intersects(context.firstBystander().getBoundingBox()),
                "Spell Reaper Scythe sweep hit box should contain the first bystander");
        context.player().attack(context.primaryTarget());
    }

    private static void equipMalumNecklace(GameTestHelper helper, FakePlayer player, String necklaceId) {
        equipMalumCurio(helper, player, Curios.NECKLACE_SLOT, necklaceId);
    }

    private static void equipMalumCurio(
            GameTestHelper helper,
            FakePlayer player,
            String slotId,
            String curioId
    ) {
        var necklace = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(
                MalumCompatibility.MOD_ID,
                curioId
        ));
        helper.assertTrue(necklace != Items.AIR, "Missing Malum curio for GameTest: " + curioId);
        var curiosInventory = CuriosApi.getCuriosInventory(player)
                .orElseThrow(() -> new IllegalStateException("Missing Curios inventory for Malum curio GameTest"));
        curiosInventory.setEquippedCurio(slotId, 0, new ItemStack(necklace));
    }

    private static void clearMalumNecklace(FakePlayer player) {
        var curiosInventory = CuriosApi.getCuriosInventory(player)
                .orElseThrow(() -> new IllegalStateException("Missing Curios inventory for Malum necklace GameTest"));
        curiosInventory.setEquippedCurio(Curios.NECKLACE_SLOT, 0, ItemStack.EMPTY);
    }

    private static void assertBetterCombatNoSweepCombo(
            GameTestHelper helper,
            FakePlayer player,
            String necklaceName
    ) {
        assertBetterCombatAttack(
                helper,
                player,
                0,
                "VERTICAL_PLANE",
                "bettercombat:two_handed_slash_vertical_right"
        );
        assertBetterCombatAttack(
                helper,
                player,
                1,
                "VERTICAL_PLANE",
                "bettercombat:two_handed_slash_vertical_left"
        );
        helper.assertTrue(readBetterCombatAttackRangeBonus(player, 0) == 0.5D,
                "Spell Reaper Scythe should preserve Better Combat range with " + necklaceName);
        helper.assertTrue(readBetterCombatAttackDamageMultiplier(player, 0) == 1.0D,
                "Spell Reaper Scythe should preserve Better Combat damage with " + necklaceName);
    }

    private static void assertBetterCombatAttack(
            GameTestHelper helper,
            FakePlayer player,
            int comboCount,
            String expectedHitbox,
            String expectedAnimation
    ) {
        try {
            var attackHand = getBetterCombatAttackHand(player, comboCount);
            helper.assertTrue(attackHand != null,
                    "Spell Reaper Scythe should have a Better Combat attack for combo " + comboCount);
            var attack = attackHand.getClass().getMethod("attack").invoke(attackHand);
            var hitbox = attack.getClass().getMethod("hitbox").invoke(attack);
            var animation = (String) attack.getClass().getMethod("animation").invoke(attack);
            helper.assertTrue(expectedHitbox.equals(hitbox.toString()),
                    "Unexpected Better Combat hitbox for combo " + comboCount + ": " + hitbox);
            helper.assertTrue(expectedAnimation.equals(animation),
                    "Unexpected Better Combat animation for combo " + comboCount + ": " + animation);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to inspect Better Combat attack", exception);
        }
    }

    private static double readBetterCombatAttackRangeBonus(FakePlayer player, int comboCount) {
        try {
            var attackHand = getBetterCombatAttackHand(player, comboCount);
            var attributes = attackHand.getClass().getMethod("attributes").invoke(attackHand);
            return ((Number) attributes.getClass().getMethod("rangeBonus").invoke(attributes)).doubleValue();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to inspect Better Combat attack range", exception);
        }
    }

    private static double readBetterCombatAttackDamageMultiplier(FakePlayer player, int comboCount) {
        try {
            var attackHand = getBetterCombatAttackHand(player, comboCount);
            var attack = attackHand.getClass().getMethod("attack").invoke(attackHand);
            return ((Number) attack.getClass().getMethod("damageMultiplier").invoke(attack)).doubleValue();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to inspect Better Combat attack damage", exception);
        }
    }

    private static Object getBetterCombatAttackHand(FakePlayer player, int comboCount)
            throws ReflectiveOperationException {
        var playerAttackHelper = Class.forName("net.bettercombat.logic.PlayerAttackHelper");
        return playerAttackHelper.getMethod("getCurrentAttack", net.minecraft.world.entity.player.Player.class, int.class)
                .invoke(null, player, comboCount);
    }

    private static void assertDamageNear(
            GameTestHelper helper,
            LivingEntity target,
            float initialHealth,
            float expectedDamage,
            String message
    ) {
        var actualDamage = initialHealth - target.getHealth();
        helper.assertTrue(Math.abs(actualDamage - expectedDamage) <= DAMAGE_TOLERANCE,
                message + ": got " + actualDamage + " / expected " + expectedDamage);
    }

    private static void assertMalumSweepSource(GameTestHelper helper, LivingEntity target) {
        var source = target.getLastDamageSource();
        helper.assertTrue(source != null && source.is(MALUM_SCYTHE_SWEEP),
                "Spell Reaper Scythe bystander damage should use malum:scythe_sweep");
    }

    private static FakePlayer prepareUsePlayer(GameTestHelper helper, String profileName) {
        var player = new FakePlayer(
                helper.getLevel(),
                new GameProfile(UUID.randomUUID(), profileName)
        );
        prepareUsePlayer(helper, player);
        return player;
    }

    private static void prepareUsePlayer(GameTestHelper helper, FakePlayer player) {
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        var playerPos = helper.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(2, 2, 1)));
        player.setPos(playerPos.x, playerPos.y, playerPos.z);
        player.setYRot(0.0F);
        player.setXRot(0.0F);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemRegistry.SPELL_REAPER_SCYTHE.get()));
        var maxMana = player.getAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA);
        helper.assertTrue(maxMana != null, "Spell Reaper Scythe Ascension test requires MAX_MANA");
        maxMana.setBaseValue(1000.0D);
    }

    private static AscensionUseContext prepareAscensionUse(
            GameTestHelper helper,
            String profileName,
            int ascensionLevel
    ) {
        var player = prepareUsePlayer(helper, profileName);
        var stack = player.getMainHandItem();
        enchantMalum(helper, stack, MALUM_ASCENSION, ascensionLevel);
        resolveMana(helper, player).setMana(1000.0F);
        var target = prepareTarget(helper, new BlockPos(2, 2, 3));
        return new AscensionUseContext(player, stack, target, target.getHealth());
    }

    private static MagicData resolveMana(GameTestHelper helper, Player player) {
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Spell Reaper Scythe Ascension test requires MagicData");
        return magicData;
    }

    private static void assertInsufficientManaMessage(GameTestHelper helper, Component message) {
        helper.assertTrue(message != null && message.getContents() instanceof TranslatableContents contents
                        && "ui.apprenticecodex.spell_reaper_scythe.ascension_insufficient_mana"
                        .equals(contents.getKey()),
                "Insufficient Ascension mana should show the expected action-bar translation key");
        helper.assertTrue(message.getStyle().getColor() != null
                        && ChatFormatting.RED.getColor() != null
                        && message.getStyle().getColor().getValue() == ChatFormatting.RED.getColor(),
                "Insufficient Ascension mana action-bar message should be red");
        var args = ((TranslatableContents) message.getContents()).getArgs();
        helper.assertTrue(args.length == 1 && args[0] instanceof Component,
                "Insufficient Ascension mana message should receive the translated enchantment name");
    }

    private static boolean isAscensionCostTooltip(Component line) {
        return line.getContents() instanceof TranslatableContents contents
                && "item.apprenticecodex.spell_reaper_scythe.malum.ascension_cost".equals(contents.getKey());
    }

    private static void enchantMalum(
            GameTestHelper helper,
            ItemStack stack,
            ResourceKey<Enchantment> enchantmentKey,
            int level
    ) {
        var enchantment = helper.getLevel().registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(enchantmentKey);
        stack.enchant(enchantment, level);
    }

    private static void assertCooldownDuration(
            GameTestHelper helper,
            FakePlayer player,
            Item item,
            int expectedTicks
    ) {
        helper.assertTrue(player.getCooldowns().isOnCooldown(item),
                "Ascension should start a cooldown");
        for (int tick = 1; tick < expectedTicks; tick++) {
            player.getCooldowns().tick();
        }
        helper.assertTrue(player.getCooldowns().isOnCooldown(item),
                "Ascension cooldown ended before " + expectedTicks + " ticks");
        player.getCooldowns().tick();
        helper.assertFalse(player.getCooldowns().isOnCooldown(item),
                "Ascension cooldown should end after " + expectedTicks + " ticks");
    }

    private static boolean isAscensionEffect(net.minecraft.world.effect.MobEffectInstance effect) {
        return MALUM_ASCENSION_EFFECT.equals(BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value()));
    }

    private static boolean hasMalumScytheBoomerang(GameTestHelper helper) {
        for (var entity : helper.getLevel().getAllEntities()) {
            if (MALUM_SCYTHE_BOOMERANG.equals(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()))) {
                return true;
            }
        }
        return false;
    }

    private static ResourceKey<Enchantment> malumEnchantmentKey(String path) {
        return ResourceKey.create(
                Registries.ENCHANTMENT,
                ResourceLocation.fromNamespaceAndPath(MalumCompatibility.MOD_ID, path)
        );
    }

    private record SweepAttackContext(
            FakePlayer player,
            LivingEntity primaryTarget,
            LivingEntity firstBystander,
            LivingEntity secondBystander,
            float primaryHealth,
            float firstBystanderHealth,
            float secondBystanderHealth
    ) {
    }

    private record AscensionUseContext(
            FakePlayer player,
            ItemStack stack,
            LivingEntity target,
            float initialTargetHealth
    ) {
    }

    private static final class FullyChargedFakePlayer extends FakePlayer {
        private FullyChargedFakePlayer(ServerLevel level, GameProfile profile) {
            super(level, profile);
        }

        @Override
        public float getAttackStrengthScale(float adjustTicks) {
            // FakePlayer.tick() は空実装のため、攻撃処理そのものを検証できるよう充填済み状態を固定する。
            return 1.0F;
        }
    }

    private static final class CapturingActionBarFakePlayer extends FakePlayer {
        private Component actionBarMessage;

        private CapturingActionBarFakePlayer(ServerLevel level, GameProfile profile) {
            super(level, profile);
        }

        @Override
        public void displayClientMessage(Component chatComponent, boolean actionBar) {
            if (actionBar) {
                actionBarMessage = chatComponent;
            }
        }

        private Component actionBarMessage() {
            return actionBarMessage;
        }
    }
}
