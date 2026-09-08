package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public class SupportedDefensiveSpellGameTests extends ApprenticeCodexGameTestScenarios {
    private static final String TEMPLATE = "gametest/basic_floor";

    @GameTest(template = TEMPLATE, batch = "apprenticecodex.supported_defensive_spells")
    public static void discountsFollowCastEquipment(GameTestHelper helper) {
        var player = player(helper);
        var targets = List.of(SpellRegistry.EVASION_SPELL.get(), SpellRegistry.HEARTSTOP_SPELL.get(),
                SpellRegistry.ABYSSAL_SHROUD_SPELL.get());
        for (boolean equipped : new boolean[]{false, true, false}) {
            equip(player, equipped);
            for (var spell : targets) {
                var event = new SpellOnCastEvent(player, spell.getSpellId(), 1, 100, spell.getSchoolType(), CastSource.SPELLBOOK);
                NeoForge.EVENT_BUS.post(event);
                helper.assertTrue(event.getManaCost() == (equipped ? 50 : 100), "Discount must follow cast equipment: " + spell.getSpellId());
            }
            var other = SpellRegistry.FORTIFY_SPELL.get();
            var event = new SpellOnCastEvent(player, other.getSpellId(), 1, 100, other.getSchoolType(), CastSource.SPELLBOOK);
            NeoForge.EVENT_BUS.post(event);
            helper.assertTrue(event.getManaCost() == 100, "Fortify must remain outside the discount list");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "apprenticecodex.supported_defensive_spells")
    public static void evasionDoublesFinalHitsAndKeepsThemAfterUnequip(GameTestHelper helper) {
        var spell = SpellRegistry.EVASION_SPELL.get();
        for (int level = 1; level <= 3; level++) {
            for (boolean equipped : new boolean[]{false, true}) {
                var player = player(helper);
                int baseAmp = (int) spell.getSpellPower(level, player);
                helper.assertTrue(baseAmp == level - 1, "Test requires baseline Evasion amplitudes 0, 1, 2");
                equip(player, equipped);
                int expectedAmp = equipped ? 2 * baseAmp + 1 : baseAmp;
                cast(helper, player, spell, level);
                var effect = player.getEffect(MobEffectRegistry.EVASION);
                helper.assertTrue(effect != null && effect.getAmplifier() == expectedAmp && effect.getDuration() == 1200,
                        "Spell must double final hits without changing duration");
                helper.assertTrue(spell.getUniqueInfo(level, player).getFirst().getString().equals(
                                Component.translatable("ui.irons_spellbooks.hits_dodged", expectedAmp + 1).getString()),
                        "Spell information must display the granted hit count");
                equip(player, !equipped);
                var data = MagicData.getPlayerMagicData(player).getSyncedData();
                helper.assertTrue(data.getEvasionHitsRemaining() == expectedAmp, "Equipment change must not recalculate granted hits");
                float health = player.getHealth();
                for (int hit = 0; hit <= expectedAmp; hit++) {
                    damage(player, 1);
                    helper.assertTrue(player.getHealth() == health, "Every granted Evasion hit must prevent damage");
                    helper.assertTrue(data.getEvasionHitsRemaining() == expectedAmp - hit - 1, "Each hit must consume exactly one charge");
                }
                helper.assertFalse(player.hasEffect(MobEffectRegistry.EVASION), "Evasion must end after the final granted hit");
                damage(player, 1);
                helper.assertTrue(player.getHealth() < health, "Damage must resume after all charges are consumed");
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "apprenticecodex.supported_defensive_spells")
    public static void nonSpellEvasionAndFallDamageRemainUnchanged(GameTestHelper helper) {
        var player = player(helper);
        equip(player, true);
        // ポーション等と同じ通常の効果付与経路は、装備中でも増幅しない。
        player.addEffect(new MobEffectInstance(MobEffectRegistry.EVASION, 1200, 2));
        var data = MagicData.getPlayerMagicData(player).getSyncedData();
        helper.assertTrue(player.getEffect(MobEffectRegistry.EVASION).getAmplifier() == 2 && data.getEvasionHitsRemaining() == 2,
                "Non-spell Evasion must keep its original amplifier");
        float health = player.getHealth();
        player.hurt(helper.getLevel().damageSources().fall(), 2);
        helper.assertTrue(player.getHealth() < health && data.getEvasionHitsRemaining() == 2,
                "Fall damage must bypass Evasion without consuming charges");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "apprenticecodex.supported_defensive_spells")
    public static void heartstopAccumulatesPerHitAndSettlesWithoutEquipment(GameTestHelper helper) {
        for (boolean equippedAtRemoval : new boolean[]{false, true}) {
            var player = player(helper);
            cast(helper, player, SpellRegistry.HEARTSTOP_SPELL.get(), 1);
            var data = MagicData.getPlayerMagicData(player).getSyncedData();
            float health = player.getHealth();
            damage(player, 4);
            assertClose(helper, data.getHeartstopAccumulatedDamage(), 2, "Unequipped hit must accumulate half damage");
            equip(player, true);
            assertClose(helper, data.getHeartstopAccumulatedDamage(), 2, "Equipping must preserve old debt");
            damage(player, 4);
            assertClose(helper, data.getHeartstopAccumulatedDamage(), 3, "Equipped hit must add quarter damage");
            equip(player, false);
            assertClose(helper, data.getHeartstopAccumulatedDamage(), 3, "Unequipping must preserve old debt");
            damage(player, 4);
            assertClose(helper, data.getHeartstopAccumulatedDamage(), 5, "Later unequipped hit must again add half damage");
            helper.assertTrue(player.getHealth() == health, "Heartstop must defer incoming damage");
            equip(player, equippedAtRemoval);
            player.invulnerableTime = 0;
            player.removeEffect(MobEffectRegistry.HEARTSTOP);
            assertClose(helper, health - player.getHealth(), 5, "Removal must settle the same debt regardless of equipment");
            assertClose(helper, data.getHeartstopAccumulatedDamage(), 0, "Removal must clear the settled debt");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "apprenticecodex.supported_defensive_spells")
    public static void heartstopPreservesSoulWardProcessing(GameTestHelper helper) {
        if (ModList.get().isLoaded("malum")) {
            MalumScenario.verify(helper);
        }
        helper.succeed();
    }

    // optional MODの型は、導入を確認してから呼ぶクラスに隔離する。
    private static final class MalumScenario {
        private static void verify(GameTestHelper helper) {
            double[] remaining = new double[2];
            for (int index = 0; index < 2; index++) {
                var player = player(helper);
                player.getAttribute(com.sammy.malum.registry.common.MalumAttributes.SOUL_WARD_CAPACITY).setBaseValue(20);
                var ward = player.getData(com.sammy.malum.registry.common.MalumAttachmentTypes.SOUL_WARD);
                ward.setSoulWard(20);
                // まず通常被弾で実際にSoul Wardが動作していることを確認する。
                damage(player, 4);
                helper.assertTrue(ward.getSoulWard() < 20, "Soul Ward control must absorb damage");
                ward.setSoulWard(20);
                player.setHealth(player.getMaxHealth());
                equip(player, index == 1);
                cast(helper, player, SpellRegistry.HEARTSTOP_SPELL.get(), 1);
                damage(player, 4);
                remaining[index] = ward.getSoulWard();
                assertClose(helper, MagicData.getPlayerMagicData(player).getSyncedData().getHeartstopAccumulatedDamage(),
                        index == 1 ? 1 : 2, "Soul Ward must not change the Heartstop accumulation basis");
            }
            helper.assertTrue(Math.abs(remaining[0] - remaining[1]) < 0.001,
                    "Supporter must not change Soul Ward consumption during Heartstop");
        }
    }

    private static ServerPlayer player(GameTestHelper helper) {
        var player = createAssistWingsRider(helper, new BlockPos(2, 2, 2), "supported_defense");
        // スポーン直後の被弾拒否とHeartstop解除時の特殊分岐を避け、通常戦闘を再現する。
        for (int tick = 0; tick < 61; tick++) {
            player.tick();
        }
        player.tickCount = 100;
        return player;
    }

    private static void equip(ServerPlayer player, boolean equipped) {
        CuriosApi.getCuriosInventory(player).orElseThrow().setEquippedCurio(CuriosSlotConstants.BELT, 0,
                equipped ? new ItemStack(ItemRegistry.PROTECTION_SPELL_SUPPORTER.get()) : ItemStack.EMPTY);
    }

    private static void cast(GameTestHelper helper, ServerPlayer player, AbstractSpell spell, int level) {
        spell.onCast(helper.getLevel(), level, player, CastSource.SPELLBOOK, MagicData.getPlayerMagicData(player));
    }

    private static void damage(ServerPlayer player, float amount) {
        player.invulnerableTime = 0;
        player.hurt(player.level().damageSources().generic(), amount);
    }

    private static void assertClose(GameTestHelper helper, float actual, float expected, String message) {
        helper.assertTrue(Math.abs(actual - expected) < 0.001f, message + ": expected " + expected + ", got " + actual);
    }
}
