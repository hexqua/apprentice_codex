package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.ICraftsmansDelightAffectedSpell;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.UUID;

final class CraftsmansDelightCastingMobilityGameTestScenarios extends ApprenticeCodexGameTestScenarios {
    private CraftsmansDelightCastingMobilityGameTestScenarios() {
    }

    static void gracedRainCastingMobilityFollowsEquipmentAndCastLifecycle(GameTestHelper helper) {
        verifyCastingMobilityLifecycle(helper, SpellRegistry.GRACED_RAIN.get());
    }

    static void manaMendingCastingMobilityFollowsEquipmentAndCastLifecycle(GameTestHelper helper) {
        verifyCastingMobilityLifecycle(helper, SpellRegistry.MANA_MENDING.get());
    }

    static void harvestMoonCastingMobilityFollowsEquipmentAndCastLifecycle(GameTestHelper helper) {
        verifyCastingMobilityLifecycle(helper, SpellRegistry.HARVEST_MOON.get());
    }

    private static void verifyCastingMobilityLifecycle(GameTestHelper helper, AbstractSpell spell) {
        var player = new MobilityTestPlayer(helper.getLevel());
        // 雲の設置判定を隣接テストのブロックから分離し、移動補助だけを検証する。
        var position = helper.absoluteVec(new Vec3(0.5D, 12.0D, 0.5D));
        player.setPos(position.x, position.y, position.z);
        player.setXRot(-90.0F);
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        var tool = new ItemStack(Items.DIAMOND_PICKAXE);
        tool.setDamageValue(100);
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        var magicData = MagicData.getPlayerMagicData(player);
        var baselineSpeed = player.getAttributeValue(AttributeRegistry.CASTING_MOVESPEED);
        helper.assertTrue(spell instanceof ICraftsmansDelightAffectedSpell affectedSpell
                        && affectedSpell.isCraftsmansDelightCastingMobilityEnabled(),
                spell.getSpellId() + " should enable CraftsmansDelight casting mobility");

        beginCast(helper, spell, player, magicData);
        for (var tick = 0; tick < 8; tick++) {
            player.tickMobilityEffects();
            spell.onServerCastTick(helper.getLevel(), 1, player, magicData);
            assertMobility(helper, player, baselineSpeed, false, "Without ring");
        }
        spell.onServerCastComplete(helper.getLevel(), 1, player, magicData, false);

        equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));
        for (var cancelled : new boolean[]{false, true}) {
            beginCast(helper, spell, player, magicData);
            assertMobility(helper, player, baselineSpeed, true, "Cast start");
            // 5 tick の有効期間を超えて継続させ、初回付与だけでは成功しないようにする。
            for (var tick = 0; tick < 12; tick++) {
                player.tickMobilityEffects();
                spell.onServerCastTick(helper.getLevel(), 1, player, magicData);
                assertMobility(helper, player, baselineSpeed, true, "Continuous refresh");
            }

            equipRingCurio(player, ItemStack.EMPTY);
            for (var tick = 0; tick < 6; tick++) {
                player.tickMobilityEffects();
                spell.onServerCastTick(helper.getLevel(), 1, player, magicData);
            }
            assertMobility(helper, player, baselineSpeed, false, "Ring removed during cast");

            equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));
            spell.onServerCastTick(helper.getLevel(), 1, player, magicData);
            assertMobility(helper, player, baselineSpeed, true, "Ring equipped during cast");
            spell.onServerCastComplete(helper.getLevel(), 1, player, magicData, cancelled);
            expireMobility(player);
            assertMobility(helper, player, baselineSpeed, false, "Cast complete, cancelled=" + cancelled);
        }

        if (spell == SpellRegistry.MANA_MENDING.get()) {
            beginCast(helper, spell, player, magicData);
            // ロック済みの修理対象を持ち替えた場合、キャンセル経路では補助を更新しない。
            player.setItemInHand(InteractionHand.MAIN_HAND, tool.copy());
            expireMobility(player);
            spell.onServerCastTick(helper.getLevel(), 1, player, magicData);
            assertMobility(helper, player, baselineSpeed, false, "Repair target replaced");

            beginCast(helper, spell, player, magicData);
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            expireMobility(player);
            spell.onServerCastTick(helper.getLevel(), 1, player, magicData);
            assertMobility(helper, player, baselineSpeed, false, "Repair target removed");
        }
        helper.succeed();
    }

    private static void beginCast(GameTestHelper helper, AbstractSpell spell, MobilityTestPlayer player, MagicData magicData) {
        if (spell == SpellRegistry.MANA_MENDING.get()) {
            prepareManaMendingProcessTick(magicData);
        }
        helper.assertTrue(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                spell.getSpellId() + " should accept the mobility test cast");
        spell.onServerPreCast(helper.getLevel(), 1, player, magicData);
        if (spell == SpellRegistry.GRACED_RAIN.get()) {
            // 継続詠唱の雲は onCast で生成され、以後の tick はその雲を経由する。
            spell.onCast(helper.getLevel(), 1, player, CastSource.SPELLBOOK, magicData);
        }
    }

    private static void expireMobility(MobilityTestPlayer player) {
        for (var tick = 0; tick < 6; tick++) {
            player.tickMobilityEffects();
        }
    }

    private static void assertMobility(GameTestHelper helper, MobilityTestPlayer player,
                                       double baselineSpeed, boolean expectedActive, String context) {
        helper.assertTrue(player.hasEffect(EffectRegistry.CRAFTSMANS_DELIGHT_MOBILITY) == expectedActive,
                context + ": unexpected CraftsmansDelight mobility effect state");
        var expectedSpeed = baselineSpeed + (expectedActive ? 0.8D : 0.0D);
        helper.assertTrue(Math.abs(player.getAttributeValue(AttributeRegistry.CASTING_MOVESPEED) - expectedSpeed) < 1.0e-6D,
                context + ": casting movement speed should be " + expectedSpeed);
    }

    private static final class MobilityTestPlayer extends FakePlayer {
        private MobilityTestPlayer(ServerLevel level) {
            super(level, new GameProfile(UUID.randomUUID(), "craftsmans_mobility_test"));
        }

        private void tickMobilityEffects() {
            // world に登録しない FakePlayer の効果だけを、通常の失効処理で進める。
            tickEffects();
        }
    }
}
