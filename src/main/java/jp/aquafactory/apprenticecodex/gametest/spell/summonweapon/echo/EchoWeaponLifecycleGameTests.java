package jp.aquafactory.apprenticecodex.gametest.spell.summonweapon.echo;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.gametest.spell.targeting.support.TargetingTestFixture;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffAttackHandler;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffAttackProfile;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffAttackProfileManager;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffCastHelper;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.AbstractSummonWeaponSpell;
import jp.aquafactory.apprenticecodex.spell.IMulticastUnsupportedSpell;
import jp.aquafactory.apprenticecodex.spell.shiden.Shiden;
import jp.aquafactory.apprenticecodex.spell.shiden.ShidenKatanaEntity;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static jp.aquafactory.apprenticecodex.gametest.equipment.multicastechostaff.MulticastEchoStaffScenarios.*;
import static jp.aquafactory.apprenticecodex.gametest.spell.senseevil.SenseEvilScenarios.spawnPositionedZombie;
import static jp.aquafactory.apprenticecodex.gametest.spell.summonweapon.echo.EchoWeaponTestSupport.*;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class EchoWeaponLifecycleGameTests {
    private EchoWeaponLifecycleGameTests() {
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 80)
    public static void repeatedWeaponsPreserveAnotherNormalCast(GameTestHelper helper) {
        var player = createEchoPlayer(helper, new BlockPos(1, 3, 1), "echo_weapon_isolation");
        var stack = new ItemStack(ItemRegistry.MULTICAST_ECHO_STAFF.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var spell = SpellRegistry.SHIDEN.get();
        var magic = MagicData.getPlayerMagicData(player);
        var ids = new HashSet<UUID>();
        final AbstractSummonWeaponSpell.SummonWeaponSpellCastData[] heldData = {null};
        final Entity[] heldWeapon = {null};
        helper.runAtTickTime(1, () -> {
            beginMulticastEchoStaffCast(helper.getLevel(), player, stack, spell, 1, 1, 10000);
            ids.add(((AbstractSummonWeaponSpell.SummonWeaponSpellCastData) magic.getAdditionalCastData())
                    .getEntity(helper.getLevel()).getUUID());
            finishStartedSpellCast(helper.getLevel(), player, spell, 1);
            // 追加分の待機中に同じ型の通常詠唱を始める。型判定だけで武器を再利用するとここが壊れる。
            magic.initiateCast(spell, 1, 200, CastSource.SPELLBOOK, "mainhand");
            magic.setPlayerCastingItem(stack);
            spell.onServerPreCast(helper.getLevel(), 1, player, magic);
            heldData[0] = (AbstractSummonWeaponSpell.SummonWeaponSpellCastData) magic.getAdditionalCastData();
            heldWeapon[0] = heldData[0].getEntity(helper.getLevel());
        });
        var delay = ApprenticeCodexServerConfig.multicastEchoStaffDelayTicks();
        for (int repeat = 1; repeat <= 2; repeat++) {
            helper.runAtTickTime(1 + delay * repeat, () -> {
                var before = new HashSet<UUID>();
                ownedEntities(helper, player).forEach(e -> before.add(e.getUUID()));
                MulticastEchoStaffCastHelper.onPlayerTick(new PlayerTickEvent.Post(player));
                var created = ownedEntities(helper, player).stream()
                        .filter(e -> e instanceof ShidenKatanaEntity && !before.contains(e.getUUID())).toList();
                helper.assertTrue(created.size() == 1, "Each delayed cast must generate exactly one new weapon");
                ids.add(created.getFirst().getUUID());
                helper.assertTrue(magic.isCasting() && magic.getCastDurationRemaining() == 200,
                        "Repeated weapon must preserve the other normal cast: casting=" + magic.isCasting() + ", remaining=" + magic.getCastDurationRemaining());
                helper.assertTrue(magic.getAdditionalCastData() == heldData[0]
                                && heldData[0].getEntity(helper.getLevel()) == heldWeapon[0] && !heldWeapon[0].isRemoved(),
                        "Repeated weapon must preserve the other normal weapon and its UUID");
            });
        }
        helper.runAtTickTime(3 + delay * 2, () -> {
            helper.assertTrue(ids.size() == 3, "Normal plus two repeats must have three distinct weapon UUIDs");
            helper.assertTrue(Math.abs(magic.getMana() - (10000 - 3 * spell.getManaCost(1))) < 0.001,
                    "Only the normal cast and two repeats must consume mana");
            spell.onServerCastComplete(helper.getLevel(), 1, player, magic, true);
            ownedEntities(helper, player).forEach(Entity::discard);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 80)
    public static void markerRejectsMulticastWithoutRejectingNormalCast(GameTestHelper helper) {
        var spells = List.of(SpellRegistry.MANTIS_LEAP.get(), SpellRegistry.THERMAL_SLICE.get(), new UnsupportedShiden());
        for (var spell : spells) {
            helper.assertTrue(spell instanceof IMulticastUnsupportedSpell, "Movement spell must declare multicast rejection");
            var player = createEchoPlayer(helper, new BlockPos(1, 3, 1), "echo_weapon_marker");
            var stack = new ItemStack(ItemRegistry.MULTICAST_ECHO_STAFF.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            completeMulticastEchoStaffCast(helper.getLevel(), player, stack, spell, 1, 1, 10000);
            var magic = MagicData.getPlayerMagicData(player);
            helper.assertTrue(player.hasEffect(EffectRegistry.ECHO_SPELL), "Rejected multicast must retain EchoSpell");
            helper.assertTrue(player.messages.stream().filter(message -> message.getContents() instanceof TranslatableContents text
                            && text.getKey().equals("ui.apprenticecodex.multicast_echo_staff.cannot_cast")).count() == 1,
                    "Unsupported multicast must send the existing translated message exactly once");
            helper.assertTrue(!magic.isCasting() && magic.getMana() == 10000 - spell.getManaCost(1),
                    "Unsupported multicast must still complete and pay for one normal cast");
            var cooldown = magic.getPlayerCooldowns().getSpellCooldowns().get(spell.getSpellId());
            helper.assertTrue(cooldown != null && cooldown.getSpellCooldown() == MagicManager.getEffectiveSpellCooldown(spell, player, CastSource.SPELLBOOK),
                    "Rejected multicast must use the normal cooldown");
            helper.runAtTickTime(25, () -> {
                MulticastEchoStaffCastHelper.onPlayerTick(new PlayerTickEvent.Post(player));
                helper.assertTrue(magic.getMana() == 10000 - spell.getManaCost(1) && player.hasEffect(EffectRegistry.ECHO_SPELL),
                        "Rejected multicast must not queue a delayed cast: mana=" + magic.getMana() + ", expected=" + (10000 - spell.getManaCost(1)) + ", echo=" + player.hasEffect(EffectRegistry.ECHO_SPELL));
                ownedEntities(helper, player).forEach(Entity::discard);
            });
        }
        helper.runAtTickTime(26, helper::succeed);
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void failedInstantWeaponReleasesOnlyItsOwnWeapon(GameTestHelper helper) {
        var player = createEchoPlayer(helper, new BlockPos(1, 3, 1), "echo_weapon_failure");
        var magic = MagicData.getPlayerMagicData(player);
        var broken = new Shiden() {
            @Override
            protected void prepareWeaponForRelease(Level level, int spellLevel,
                                                   LivingEntity entity, MagicData data, @NotNull ShidenKatanaEntity weapon) {
                throw new IllegalStateException("Expected test failure during weapon preparation");
            }
        };
        magic.setMana(10000);
        boolean failed = false;
        try {
            broken.castInstantWeapon(helper.getLevel(), 1, player, CastSource.SPELLBOOK, magic);
        } catch (IllegalStateException expected) {
            failed = true;
        }
        helper.assertTrue(failed && ownedEntities(helper, player).isEmpty(), "Failed cast must release its generated weapon");
        magic.resetCastingState();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 40)
    public static void delayedWeaponProfilesRespectOverridesAndExpiry(GameTestHelper helper) {
        var player = createEchoPlayer(helper, new BlockPos(1, 3, 1), "echo_weapon_profiles");
        var spell = SpellRegistry.SHIDEN.get();
        var weapon = ((AbstractSummonWeaponSpell<?>) spell).onCastNoWeapon(helper.getLevel(), 1, player, MagicData.getPlayerMagicData(player));
        var target = TargetingTestFixture.of(helper).track(spawnPositionedZombie(helper.getLevel(), player.position().add(0, 0, 3)));
        target.setNoGravity(true);
        TargetingTestFixture.of(helper).assertVisible(target);
        var source = CombatTools.getDamageSource(helper.getLevel(), weapon, player,
                DamageTypes.SHIDEN);
        var profile = new MulticastEchoStaffAttackProfile(0.25, true, true, true, 2, 1);
        try (var config = ApprenticeCodexServerConfig.useMulticastEchoStaffAttackConfigOverrideForGameTest(true, 1.0);
             var profiles = MulticastEchoStaffAttackProfileManager.useProfilesForGameTest(Map.of(spell.getSpellResource(), profile))) {
            MulticastEchoStaffAttackHandler.runRepeatedCast(player, spell, () -> {
                MulticastEchoStaffAttackHandler.trackWeaponAttack(weapon);
                helper.assertTrue(MulticastEchoStaffAttackHandler.adjustCombatDamage(target, 8, source).baseAmount() == 2,
                        "Active and tracked paths must not apply the multiplier twice");
            });
            helper.assertTrue(MulticastEchoStaffAttackHandler.adjustCombatDamage(target, 8, source).baseAmount() == 2,
                    "Delayed weapon must retain the overridden profile");
        }
        try (var config = ApprenticeCodexServerConfig.useMulticastEchoStaffAttackConfigOverrideForGameTest(false, 1.0)) {
            helper.assertTrue(MulticastEchoStaffAttackHandler.adjustCombatDamage(target, 8, source).baseAmount() == 8,
                    "Disabling profiles must disable already tracked weapon adjustments");
        }
        helper.runAtTickTime(4, () -> {
            helper.assertTrue(MulticastEchoStaffAttackHandler.adjustCombatDamage(target, 8, source).baseAmount() == 8,
                    "Expired weapon tracking must stop applying adjustments");
            weapon.discard();
            target.discard();
            helper.succeed();
        });
    }

    private static final class UnsupportedShiden extends Shiden implements IMulticastUnsupportedSpell {
    }
}
