package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.CapabilityEvents;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.QuickcastCartridgeChargeState;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.curios.quickcastscrollcartridge.*;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.PlayLevelSoundEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class QuickcastCartridgeChargeGameTests extends ApprenticeCodexGameTestScenarios {
    private static final String TEMPLATE = "gametest/basic_floor";
    // 追加した投射物・ダメージ検証の配置を、既存の近傍entity探索テストから分離する。
    private static final String BATCH = "apprenticecodex.quickcast_cartridge_charge";

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void reloadFeedbackFollowsServerTransitions(GameTestHelper helper) {
        var messages = new ArrayList<Component>();
        var manual = new FakePlayer(helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "reload_feedback")) {
            @Override public void displayClientMessage(Component message, boolean actionBar) {
                if (actionBar) messages.add(message);
            }
        };
        manual.setPos(helper.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(0, 2, 0))));
        preparePlayer(manual, 1000);
        var early = prepare(helper, "reload_early_fx", 10);
        var tied = prepare(helper, "reload_tied_fx", 1000);
        var interrupted = prepare(helper, "reload_stop_fx", 10);
        var automatic = prepare(helper, "reload_auto_fx", 10);
        try (var sounds = new ReloadSoundObservation()) {
            for (var player : new ServerPlayer[]{manual, early, tied, interrupted}) {
                QuickcastCartridgeCasting.initiate(player);
                QuickcastCartridgeCasting.initiate(player);
            }
            helper.assertTrue(sounds.started == 0 && sounds.completed == 0,
                    "Shortage warnings and duplicate requests must not play reload sounds");
            helper.assertTrue(countReloadStartMessages(messages) == 0, "A shortage must not announce reload start");
        }
        helper.runAtTickTime(2, () -> {
            try (var sounds = new ReloadSoundObservation()) {
                for (var player : new ServerPlayer[]{manual, early, tied, interrupted}) {
                    QuickcastCartridgeCasting.initiate(player);
                    QuickcastCartridgeCharge.tick(player);
                    QuickcastCartridgeCharge.sync(player, false, true);
                }
                helper.assertTrue(sounds.started == 4 && sounds.completed == 0,
                        "Accepted reloads must each play one start sound, including after repeated synchronization");
                helper.assertTrue(countReloadStartMessages(messages) == 1, "Accepted reload must announce start exactly once");
                QuickcastCartridgeCharge.state(tied).consume(QuickcastCartridgeCharge.now(tied), 60);
                QuickcastCartridgeCharge.interruptReload(interrupted);
                helper.assertTrue(sounds.completed == 0, "Interrupting reload must not play completion");
            }
        });
        helper.runAtTickTime(11, () -> {
            try (var sounds = new ReloadSoundObservation()) {
                for (var player : new ServerPlayer[]{early, interrupted, automatic}) {
                    QuickcastCartridgeCharge.tick(player);
                    QuickcastCartridgeCharge.tick(player);
                }
                helper.assertTrue(sounds.started == 0 && sounds.completed == 1,
                        "Only automatic recovery during active reload must play completion");
            }
        });
        helper.runAtTickTime(63, () -> {
            try (var sounds = new ReloadSoundObservation()) {
                for (var player : new ServerPlayer[]{manual, tied}) {
                    QuickcastCartridgeCharge.tick(player);
                    QuickcastCartridgeCharge.interruptReload(player);
                    QuickcastCartridgeCharge.tick(player);
                }
                helper.assertTrue(sounds.started == 0 && sounds.completed == 2,
                        "Manual and simultaneous automatic completion must each play once");
                helper.assertTrue(countReloadStartMessages(messages) == 1, "Completion must not repeat the start notification");
                helper.succeed();
            } finally {
                for (var player : new ServerPlayer[]{manual, early, tied, interrupted, automatic}) {
                    QuickcastCartridgeCharge.forget(player);
                }
            }
        });
    }

    private static long countReloadStartMessages(List<Component> messages) {
        return messages.stream().filter(message -> message.getContents()
                instanceof TranslatableContents translated
                && translated.getKey().equals("ui.apprenticecodex.quickcast_scroll_cartridge.start_manual_reload")).count();
    }

    private static final class ReloadSoundObservation implements AutoCloseable {
        int started;
        int completed;
        final Consumer<PlayLevelSoundEvent.AtPosition> listener = event -> {
            if (event.getSound() == null) return;
            var sound = event.getSound().value();
            if (sound == SoundRegistry.VANILLA_COLLECT_MANA.get()) started++;
            if (sound == SoundRegistry.SPELLCHARGE.get()) completed++;
        };
        ReloadSoundObservation() { MinecraftForge.EVENT_BUS.addListener(listener); }
        @Override public void close() { MinecraftForge.EVENT_BUS.unregister(listener); }
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void animationMetadataMustNotCancelFirstLongCast(GameTestHelper helper) {
        var player = prepare(helper, "cartridge_first_long", 0);
        var stack = QuickcastCartridgeCasting.findEquipped(player);
        var spell = SpellRegistry.FIREBALL_SPELL.get();
        QuickcastScrollCartridge.setCalibrationScroll(stack, 0, createSpellScroll(spell));
        var magic = MagicData.getPlayerMagicData(player);
        magic.getPlayerCooldowns().addCooldown(spell, 1000);
        MinecraftForge.EVENT_BUS.post(new LivingEvent.LivingTickEvent(player));
        helper.assertTrue(QuickcastCartridgeCasting.initiate(player), "First LONG cast must initiate");
        MinecraftForge.EVENT_BUS.post(new LivingEvent.LivingTickEvent(player));
        helper.assertTrue(magic.isCasting(), "Curios metadata notifications must not cancel the first LONG cast");
        helper.assertTrue(QuickcastCartridgeCasting.hasReservation(player), "Metadata notification must preserve the reservation");
        equipCurio(player, "back", stack.copy());
        QuickcastCartridgeCasting.validate(player);
        helper.assertFalse(magic.isCasting(), "A different cartridge with identical data must cancel the cast");
        helper.assertTrue(QuickcastCartridgeCharge.state(player).available(QuickcastCartridgeCharge.now(player)),
                "Unequipping before activation must release the unspent charge");
        helper.succeed();
    }

    private static FakePlayer prepare(GameTestHelper helper, String name, long wait) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), name);
        return preparePlayer(player, wait);
    }

    private static FakePlayer prepareDamageable(GameTestHelper helper, String name) {
        // 通常のFakePlayerは常時無敵なので、残ダメージを通常hurt経路へ戻すケースだけ解除する。
        var player = new FakePlayer(helper.getLevel(),
                new GameProfile(UUID.randomUUID(), name)) {
            @Override public boolean isInvulnerableTo(DamageSource source) { return false; }
        };
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        try {
            var field = ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
            field.setAccessible(true);
            field.setInt(player, 0);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to disable spawn protection for GameTest", exception);
        }
        player.setPos(helper.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(0, 2, 0))));
        return preparePlayer(player, 1000);
    }

    private static FakePlayer preparePlayer(
            FakePlayer player, long wait) {
        var stack = new ItemStack(ItemRegistry.QUICKCAST_SCROLL_CARTRIDGE.get());
        QuickcastScrollCartridge.setCalibrationScroll(stack, 0, createSpellScroll(SpellRegistry.MAGIC_MISSILE_SPELL.get()));
        equipCurio(player, "back", stack);
        var magic = MagicData.getPlayerMagicData(player);
        magic.setMana(1000);
        magic.getPlayerCooldowns().addCooldown(SpellRegistry.MAGIC_MISSILE_SPELL.get(), 1000);
        if (wait > 0) QuickcastCartridgeCharge.state(player).consume(QuickcastCartridgeCharge.now(player), wait);
        QuickcastCartridgeCharge.tick(player);
        return player;
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void reloadUsesFinalDamageAndManaShield(GameTestHelper helper) {
        var full = prepare(helper, "cartridge_shield_full", 1000);
        var partial = prepareDamageable(helper, "cartridge_shield_partial");
        var absorbed = prepareDamageable(helper, "cartridge_absorbed");
        var shell = prepareDamageable(helper, "cartridge_shell");
        var shellAbsorbed = prepareDamageable(helper, "cartridge_shell_absorbed");
        equipCurio(full, "charm", new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get()));
        equipCurio(partial, "charm", new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get()));
        var shellCharm = new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get());
        shellCharm.enchant(EnchantmentRegistry.SHELL.get(), 1);
        equipCurio(shell, "charm", shellCharm);
        equipCurio(shellAbsorbed, "charm", shellCharm.copy());
        for (var player : new ServerPlayer[]{full, partial, absorbed, shell, shellAbsorbed}) QuickcastCartridgeCasting.initiate(player);
        helper.runAtTickTime(2, () -> {
            for (var player : new ServerPlayer[]{full, partial, absorbed, shell, shellAbsorbed}) QuickcastCartridgeCasting.initiate(player);
            MagicData.getPlayerMagicData(full).setMana(1000);
            MagicData.getPlayerMagicData(partial).setMana(1);
            var fullHit = postLivingAttackEventForGameTest(full, full.damageSources().lava(), 2);
            helper.assertTrue(fullHit.isCanceled(), "Mana Shield must fully cancel the protected hit");
            helper.assertTrue(QuickcastCartridgeCharge.isReloading(full), "A fully canceled hit must preserve reload");
            // Forgeでは元のhurtはcancelされ、残ダメージのhurtが通常処理を通る。
            partial.hurt(partial.damageSources().lava(), 10);
            helper.assertTrue(partial.getHealth() < partial.getMaxHealth(), "Partial protection must apply residual health damage");
            helper.assertFalse(QuickcastCartridgeCharge.isReloading(partial), "Positive final damage must interrupt reload");
            absorbed.setAbsorptionAmount(8);
            helper.assertTrue(absorbed.getAbsorptionAmount() == 8, "Absorption must be initialized before the test hit");
            absorbed.hurt(absorbed.damageSources().generic(), 5);
            helper.assertTrue(absorbed.getAbsorptionAmount() < 8, "The test hit must reach absorption processing");
            helper.assertTrue(QuickcastCartridgeCharge.isReloading(absorbed), "Zero final damage after absorption must preserve reload");
            float shellMana = ApprenticeCodexServerConfig.manaShieldCharmShellActivationManaCost() + 1;
            MagicData.getPlayerMagicData(shell).setMana(shellMana);
            MagicData.getPlayerMagicData(shellAbsorbed).setMana(shellMana);
            shellAbsorbed.setAbsorptionAmount(20);
            shell.hurt(shell.damageSources().lava(), 10);
            shellAbsorbed.hurt(shellAbsorbed.damageSources().lava(), 10);
            helper.assertTrue(shell.getHealth() < shell.getMaxHealth(), "Shell must allow residual health damage");
            helper.assertFalse(QuickcastCartridgeCharge.isReloading(shell), "Final Shell residual damage must interrupt reload");
            helper.assertTrue(shellAbsorbed.getAbsorptionAmount() < 20, "Shell residual damage must reach absorption");
            helper.assertTrue(QuickcastCartridgeCharge.isReloading(shellAbsorbed), "Fully absorbed Shell residual damage must preserve reload");
            QuickcastCartridgeCharge.forget(full);
            QuickcastCartridgeCharge.forget(partial);
            QuickcastCartridgeCharge.forget(absorbed);
            QuickcastCartridgeCharge.forget(shell);
            QuickcastCartridgeCharge.forget(shellAbsorbed);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void selectionRoundTripAndEquipmentInvalidateConfirmation(GameTestHelper helper) {
        var selection = prepare(helper, "cartridge_roundtrip", 1000);
        var equipment = prepare(helper, "cartridge_equipment_confirmation", 1000);
        QuickcastCartridgeCasting.initiate(selection);
        QuickcastCartridgeCasting.initiate(equipment);
        var synced = MagicData.getPlayerMagicData(selection).getSyncedData();
        synced.setSpellSelection(synced.getSpellSelection());
        QuickcastCartridgeCharge.equipmentChanged(equipment);
        helper.runAtTickTime(2, () -> {
            QuickcastCartridgeCasting.initiate(selection);
            QuickcastCartridgeCasting.initiate(equipment);
            helper.assertFalse(QuickcastCartridgeCharge.isReloading(selection), "Selection updates must invalidate old confirmation");
            helper.assertFalse(QuickcastCartridgeCharge.isReloading(equipment), "Identical replacement equipment must invalidate old confirmation");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void recoveryUsesAttributesAtReservation(GameTestHelper helper) {
        var player = prepare(helper, "cartridge_attribute", 0);
        var spell = SpellRegistry.MAGIC_MISSILE_SPELL.get();
        var cooldownAttribute = player.getAttribute(AttributeRegistry.COOLDOWN_REDUCTION.get());
        cooldownAttribute.setBaseValue(0.4);
        int effective = MagicManager.getEffectiveSpellCooldown(spell, player, CastSource.SPELLBOOK);
        var config = ApprenticeCodexServerConfig.quickcastCartridge();
        long expected = QuickcastCartridgeChargeState.recoveryTicks(effective, config.multiplier(), config.minimumTicks());
        helper.assertTrue(QuickcastCartridgeCasting.initiate(player), "Attribute-adjusted bypass must initiate");
        cooldownAttribute.setBaseValue(0);
        spell.castSpell(player.level(), 1, player, CastSource.SPELLBOOK, true);
        helper.assertTrue(QuickcastCartridgeCharge.state(player).recoveryDuration() == expected,
                "Attribute changes after reservation must not recalculate recovery duration");
        Utils.serverSideCancelCast(player);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void chargeTimeAndPersistence(GameTestHelper helper) {
        helper.assertTrue(QuickcastCartridgeChargeState.recoveryTicks(101, 1.5, 60) == 152,
                "Recovery must ceil the scaled full cooldown");
        helper.assertTrue(QuickcastCartridgeChargeState.recoveryTicks(0, 1, 60) == 60,
                "Recovery must respect the minimum");
        helper.assertTrue(QuickcastCartridgeChargeState.recoveryTicks(Integer.MAX_VALUE, Double.MAX_VALUE, 60) == Long.MAX_VALUE,
                "Recovery multiplication must saturate");
        helper.assertTrue(QuickcastCartridgeChargeState.addTime(Long.MAX_VALUE - 1, 60) == Long.MAX_VALUE,
                "Recovery timestamp addition must saturate");
        var player = prepare(helper, "cartridge_saved", 500);
        long until = QuickcastCartridgeCharge.state(player).recoveryUntil();
        var saved = Capabilities.getSpellDataOrNull(player).saveAll();
        Capabilities.getSpellDataOrNull(player).loadAll(saved);
        helper.assertTrue(QuickcastCartridgeCharge.state(player).recoveryUntil() == until,
                "Save and load must preserve the recovery deadline");
        var replacement = QuickcastCartridgeCasting.findEquipped(player).copy();
        equipCurio(player, "back", ItemStack.EMPTY);
        QuickcastCartridgeCharge.tick(player);
        equipCurio(player, "back", replacement);
        QuickcastCartridgeCharge.equipmentChanged(player);
        helper.assertTrue(QuickcastCartridgeCharge.state(player).recoveryUntil() == until,
                "Replacing the cartridge must not refill the charge");
        QuickcastCartridgeCharge.forget(player);
        helper.assertTrue(QuickcastCartridgeCharge.state(player).recoveryUntil() == until,
                "Logout cleanup must preserve recovery");
        // Forgeのclone処理は旧playerのcapabilityを無効化するため、旧個体の検証を先に済ませる。
        var clone = prepare(helper, "cartridge_clone", 0);
        CapabilityEvents.onPlayerClone(new PlayerEvent.Clone(clone, player, true));
        helper.assertTrue(QuickcastCartridgeCharge.state(clone).recoveryUntil() == until,
                "Death clone must preserve the recovery deadline");
        var restored = new QuickcastCartridgeChargeState();
        restored.consume(1000, 200);
        var tag = restored.save();
        restored.load(tag);
        restored.repairAfterLoad(100);
        helper.assertTrue(restored.recoveryUntil() == 300, "A rewound clock must clamp the saved wait to its known duration");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void reloadRequiresDistinctRequestsAndCompletes(GameTestHelper helper) {
        var player = prepare(helper, "cartridge_reload", 1000);
        helper.assertFalse(QuickcastCartridgeCasting.initiate(player), "First shortage must only warn");
        QuickcastCartridgeCasting.initiate(player);
        helper.assertFalse(QuickcastCartridgeCharge.isReloading(player), "Same-tick requests must not confirm reload");
        helper.runAtTickTime(2, () -> {
            QuickcastCartridgeCasting.initiate(player);
            helper.assertTrue(QuickcastCartridgeCharge.isReloading(player), "A second request must start reload");
            helper.assertFalse(MagicData.getPlayerMagicData(player).isCasting(), "Reload must not initiate a spell");
            // 位置変更や吹き飛ばしだけでリロードを拒否しない。
            player.setPos(player.position().add(1, 0, 0));
            player.setDeltaMovement(0.5, 0.2, 0);
            QuickcastCartridgeCharge.tick(player);
            helper.assertTrue(QuickcastCartridgeCharge.isReloading(player), "External movement must preserve reload");
        });
        helper.runAtTickTime(63, () -> {
            QuickcastCartridgeCharge.tick(player);
            helper.assertFalse(QuickcastCartridgeCharge.isReloading(player), "Reload must finish after sixty ticks");
            helper.assertTrue(QuickcastCartridgeCharge.state(player).available(QuickcastCartridgeCharge.now(player)),
                    "Manual reload must restore the charge before automatic recovery");
            helper.assertFalse(MagicData.getPlayerMagicData(player).isCasting(), "Reload completion must not cast automatically");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void confirmationRechecksCooldownChargeAndSelection(GameTestHelper helper) {
        var normal = prepare(helper, "cartridge_normal_priority", 1000);
        var recovered = prepare(helper, "cartridge_charge_priority", 1);
        var changed = prepare(helper, "cartridge_selection", 1000);
        var expired = prepare(helper, "cartridge_expired", 1000);
        for (var player : new ServerPlayer[]{normal, recovered, changed, expired}) QuickcastCartridgeCasting.initiate(player);
        long normalUntil = QuickcastCartridgeCharge.state(normal).recoveryUntil();
        helper.runAtTickTime(2, () -> {
            MagicData.getPlayerMagicData(normal).getPlayerCooldowns().clearCooldowns();
            helper.assertTrue(QuickcastCartridgeCasting.initiate(normal), "Expired spell cooldown must allow normal casting");
            helper.assertTrue(QuickcastCartridgeCharge.state(normal).recoveryUntil() == normalUntil,
                    "Normal casting must preserve charge recovery");
            helper.assertTrue(QuickcastCartridgeCasting.initiate(recovered), "Recovered charge must take precedence over reload");
            helper.assertTrue(QuickcastCartridgeCasting.hasReservation(recovered), "Cooldown bypass must reserve recovered charge");
            var stack = QuickcastCartridgeCasting.findEquipped(changed);
            QuickcastScrollCartridge.setCalibrationScroll(stack, 0, createSpellScroll(SpellRegistry.FIREBOLT_SPELL.get()));
            MagicData.getPlayerMagicData(changed).getPlayerCooldowns().addCooldown(SpellRegistry.FIREBOLT_SPELL.get(), 1000);
            QuickcastCartridgeCasting.initiate(changed);
            helper.assertFalse(QuickcastCartridgeCharge.isReloading(changed), "Changed selection must require a new warning");
            Utils.serverSideCancelCast(normal);
            Utils.serverSideCancelCast(recovered);
        });
        helper.runAtTickTime(21, () -> {
            QuickcastCartridgeCasting.initiate(expired);
            helper.assertFalse(QuickcastCartridgeCharge.isReloading(expired), "Expired confirmation must warn again");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void reloadInterruptionsAndAutomaticRecovery(GameTestHelper helper) {
        var attack = prepare(helper, "cartridge_attack", 1000);
        var use = prepare(helper, "cartridge_use", 1000);
        var cast = prepare(helper, "cartridge_cast_interrupt", 1000);
        var auto = prepare(helper, "cartridge_auto", 10);
        for (var player : new ServerPlayer[]{attack, use, cast, auto}) QuickcastCartridgeCasting.initiate(player);
        long until = QuickcastCartridgeCharge.state(attack).recoveryUntil();
        helper.runAtTickTime(2, () -> {
            for (var player : new ServerPlayer[]{attack, use, cast, auto}) {
                QuickcastCartridgeCasting.initiate(player);
                helper.assertTrue(QuickcastCartridgeCharge.isReloading(player), "Second shortage must start reload");
            }
            MinecraftForge.EVENT_BUS.post(new AttackEntityEvent(attack, use));
            helper.assertFalse(QuickcastCartridgeCharge.isReloading(attack), "Attacking must interrupt reload");
            helper.assertTrue(QuickcastCartridgeCharge.state(attack).recoveryUntil() == until, "Interruption must preserve automatic progress");
            MinecraftForge.EVENT_BUS.post(new LivingEntityUseItemEvent.Start(
                    use, new ItemStack(Items.APPLE), 32));
            helper.assertFalse(QuickcastCartridgeCharge.isReloading(use), "Using an item must interrupt reload");
            MagicData.getPlayerMagicData(cast).getPlayerCooldowns().clearCooldowns();
            helper.assertTrue(SpellRegistry.MAGIC_MISSILE_SPELL.get().attemptInitiateCast(ItemStack.EMPTY, 1,
                    cast.level(), cast, CastSource.SPELLBOOK, true, "mainhand"), "Other casting sources must interrupt and cast normally");
            helper.assertFalse(QuickcastCartridgeCharge.isReloading(cast), "Casting must interrupt reload");
            Utils.serverSideCancelCast(cast);
        });
        helper.runAtTickTime(11, () -> {
            QuickcastCartridgeCharge.tick(auto);
            helper.assertFalse(QuickcastCartridgeCharge.isReloading(auto), "Automatic recovery must finish reload early");
            helper.assertTrue(QuickcastCartridgeCharge.state(auto).available(QuickcastCartridgeCharge.now(auto)), "Automatic completion must restore charge");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void continuousReservationAndRecoveredCharge(GameTestHelper helper) {
        var player = prepare(helper, "cartridge_continuous", 0);
        var spell = SpellRegistry.FIRE_BREATH_SPELL.get();
        QuickcastScrollCartridge.setCalibrationScroll(QuickcastCartridgeCasting.findEquipped(player), 0, createSpellScroll(spell));
        var magic = MagicData.getPlayerMagicData(player);
        magic.getPlayerCooldowns().addCooldown(spell, 1000);
        helper.assertTrue(QuickcastCartridgeCasting.initiate(player), "Continuous cast must start with a reservation");
        Utils.serverSideCancelCast(player);
        helper.assertTrue(QuickcastCartridgeCharge.state(player).available(QuickcastCartridgeCharge.now(player)),
                "Continuous cancellation before activation must not consume");
        helper.assertTrue(QuickcastCartridgeCasting.initiate(player), "Released reservation must be reusable");
        spell.castSpell(player.level(), 1, player, CastSource.SPELLBOOK, false);
        helper.assertFalse(QuickcastCartridgeCharge.state(player).available(QuickcastCartridgeCharge.now(player)), "First effect must consume");
        // 回復済みでも、同じCONTINUOUS詠唱の反復は新しいチャージを消費しない。
        QuickcastCartridgeCharge.state(player).consume(QuickcastCartridgeCharge.now(player), 1);
        helper.runAtTickTime(2, () -> {
            QuickcastCartridgeCharge.tick(player);
            spell.castSpell(player.level(), 1, player, CastSource.SPELLBOOK, false);
            Utils.serverSideCancelCast(player);
            helper.assertTrue(QuickcastCartridgeCharge.state(player).available(QuickcastCartridgeCharge.now(player)),
                    "Later continuous effects and cancellation must preserve the recovered charge");
            helper.succeed();
        });
    }
}
