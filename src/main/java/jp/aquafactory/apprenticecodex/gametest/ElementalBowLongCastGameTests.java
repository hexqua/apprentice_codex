package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.item.curios.AffinityData;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.compat.Curios;
import io.redspace.ironsspellbooks.registries.DataAttachmentRegistry;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import io.redspace.ironsspellbooks.spells.fire.FireArrowSpell;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.enchantment.Enchantments;
import jp.aquafactory.apprenticecodex.item.curios.spellcastparryingring.SpellCastParryingRingDefenseEvent;
import jp.aquafactory.apprenticecodex.item.elementalbow.*;
import jp.aquafactory.apprenticecodex.network.packet.SyncElementalBowCastPacket;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.lunaraim.LunarAimCastData;
import jp.aquafactory.apprenticecodex.spell.sacredarrow.SacredArrowCastData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ElementalBowLongCastGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void transcendenceAndAffinityApplyOnceToInstantAndLongCasts(GameTestHelper h) {
        var players = new ArrayList<FakePlayer>();
        try (var config = useDamageTestConfig(h)) {
            for (var spell : List.of(SpellRegistry.LUNAR_AIM.get(),
                    io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get())) {
                var player = player(h, InteractionHand.MAIN_HAND, spell);
                players.add(player);
                var stack = player.getMainHandItem();
                var scroll = BowGameTestSupport.createSpellScroll(spell);
                ISpellContainer.createScrollContainer(spell, spell.getMaxLevel(), scroll);
                ElementalBow.setCalibrationScroll(stack, 0, scroll, h.getLevel().registryAccess());
                stack.enchant(h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(Enchantments.TRANSCENDENCE), 3);
                var ring = new ItemStack(ItemRegistry.ENCHANTED_CIRCLET.get());
                AffinityData.setAffinityData(ring, spell, 2);
                BowGameTestSupport.equipCurio(player, "head", ring);
                h.assertTrue(ElementalBow.getDisplayedSpellProfile(stack).spellLevel() == spell.getMaxLevel() + 1,
                        "Stored spell preview must apply only the fixed internal bonus");
                var selectedView = ElementalBow.getAvailableSelectionViews(player, stack).stream()
                        .filter(ElementalBow.ModeSelectionView::currentSelection).findFirst().orElseThrow();
                var expectedDisplayLevel = Integer.toString(spell.getMaxLevel() + 3);
                h.assertTrue(expectedDisplayLevel.equals(selectedView.badgeText())
                                && selectedView.displayName().getString().endsWith(" " + expectedDisplayLevel),
                        "Selection name and badge must include Transcendence and Affinity once");
                h.assertTrue(stack.getItem().use(h.getLevel(), player, InteractionHand.MAIN_HAND).getResult().consumesAction(),
                        "Enchanted bow must begin drawing with Affinity equipped");
            }
        } catch (RuntimeException | Error failure) {
            players.forEach(player -> { ElementalBowPendingCast.cancel(player); player.discard(); });
            throw failure;
        }
        h.runAfterDelay(30, () -> {
            try (var config = useDamageTestConfig(h)) {
                for (var player : players) {
                    var stack = player.getMainHandItem();
                    var profile = ElementalBow.getDisplayedSpellProfile(stack);
                    int expectedLevel = profile.spell().getMaxLevel() + 3;
                    int[] castLevels = {0, 0};
                    Consumer<SpellOnCastEvent> listener = event -> {
                        if (event.getEntity() == player) {
                            castLevels[0]++;
                            castLevels[1] = event.getOriginalSpellLevel();
                        }
                    };
                    NeoForge.EVENT_BUS.addListener(listener);
                    try {
                        float manaBefore = MagicData.getPlayerMagicData(player).getMana();
                        float expectedMana = ElementalBowRunes.baseManaCost(stack, player, profile.spell().getManaCost(expectedLevel));
                        stack.getItem().releaseUsing(stack, h.getLevel(), player, stack.getUseDuration(player) - 30);
                        h.assertTrue(castLevels[0] == 1 && castLevels[1] == expectedLevel,
                                "Bow must cast once at original level plus one Transcendence and two Affinity levels");
                        h.assertTrue(Math.abs(manaBefore - MagicData.getPlayerMagicData(player).getMana() - expectedMana) < 0.01F,
                                "Mana consumption must use the same effective level as casting");
                        h.assertTrue(stack.getEnchantmentLevel(h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                                        .getOrThrow(Enchantments.TRANSCENDENCE)) == 3,
                                "Casting must preserve legacy enchantment levels");
                    } finally {
                        NeoForge.EVENT_BUS.unregister(listener);
                    }
                }
                h.succeed();
            } finally {
                players.forEach(player -> { ElementalBowPendingCast.cancel(player); player.discard(); });
            }
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void damagePreservesBowDrawAndRingNeverParries(GameTestHelper h) {
        var players = new ArrayList<FakePlayer>();
        var spells = List.of(SpellRegistry.LUNAR_AIM.get(),
                io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get());
        try (var config = useDamageTestConfig(h)) {
            for (var spell : spells) for (var hand : InteractionHand.values()) {
                var player = damageablePlayer(h, hand, spell);
                players.add(player);
                // 毒の初期時刻による中断回避と無敵時間が、被弾検証を隠さないようにする。
                player.tickCount = 10;
                BowGameTestSupport.equipCurio(player, Curios.RING_SLOT,
                        new ItemStack(ItemRegistry.SPELL_CAST_PARRYING_RING.get()));
                h.assertTrue(SpellCastParryingRingDefenseEvent.isEquippedBy(player),
                        "Bow damage test must equip the ring");
                var stack = player.getItemInHand(hand);
                h.assertTrue(stack.getItem().use(h.getLevel(), player, hand).getResult().consumesAction(),
                        "Magic bow draw must start");
                hitBowFromFront(h, player);
                h.assertTrue(player.isUsingItem(), "Damage at draw start must preserve bow use");
                if (spell.getCastType() == CastType.LONG) {
                    h.assertTrue(ElementalBowPendingCast.isManagedCast(player), "Damage must preserve managed LONG casting");
                }
                assertUnspent(h, player, stack);
            }
        } catch (RuntimeException | Error failure) {
            players.forEach(player -> { ElementalBowPendingCast.cancel(player); player.discard(); });
            throw failure;
        }
        h.runAfterDelay(5, () -> {
            try (var config = useDamageTestConfig(h)) {
                for (var player : players) {
                    ElementalBowPendingCast.tick(player);
                    hitBowFromFront(h, player);
                    h.assertTrue(player.isUsingItem(), "Damage during charging must preserve bow use");
                }
            } catch (RuntimeException | Error failure) {
                players.forEach(player -> { ElementalBowPendingCast.cancel(player); player.discard(); });
                throw failure;
            }
        });
        h.runAfterDelay(30, () -> {
            try (var config = useDamageTestConfig(h)) {
                for (var player : players) {
                    var stack = player.getUseItem();
                    ElementalBowPendingCast.tick(player);
                    hitBowFromFront(h, player);
                    h.assertTrue(player.isUsingItem(), "Damage after charging must preserve bow use");
                    int[] casts = {0};
                    Consumer<SpellOnCastEvent> listener = event -> { if (event.getEntity() == player) casts[0]++; };
                    NeoForge.EVENT_BUS.addListener(listener);
                    try {
                        float manaBeforeRelease = MagicData.getPlayerMagicData(player).getMana();
                        stack.getItem().releaseUsing(stack, h.getLevel(), player, stack.getUseDuration(player) - 30);
                        h.assertTrue(casts[0] == 1 && stack.getDamageValue() == 1,
                                "Damaged bow draw must fire exactly once on release");
                        h.assertTrue(player.getInventory().getItem(2).getCount() == 2,
                                "Release must consume exactly one arrow");
                        h.assertTrue(MagicData.getPlayerMagicData(player).getMana() < manaBeforeRelease
                                        && ElementalBowOverheatManager.getState(player).active(),
                                "Release must consume mana and apply heat: mana=" + MagicData.getPlayerMagicData(player).getMana()
                                        + ", heat=" + ElementalBowOverheatManager.getState(player)
                                        + ", spell=" + ElementalBow.getDisplayedSpellProfile(stack));
                    } finally {
                        NeoForge.EVENT_BUS.unregister(listener);
                    }
                }
                h.succeed();
            } finally {
                players.forEach(player -> { ElementalBowPendingCast.cancel(player); player.discard(); });
            }
        });
    }

    private static ApprenticeCodexServerConfig.GameTestConfigOverride useDamageTestConfig(GameTestHelper h) {
        var config = BowGameTestSupport.useElementalBowSpellConfig(h);
        var previous = ElementalBowModeManager.createSnapshot();
        var definitions = new ArrayList<>(previous);
        // 標準プロファイルは LONG のため、データパックで追加可能な INSTANT も同じ操作で検証する。
        definitions.add(new ElementalBowModeDefinition(
                io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get().getSpellResource(), 20));
        ElementalBowModeManager.applySnapshot(definitions);
        return () -> {
            ElementalBowModeManager.applySnapshot(previous);
            config.close();
        };
    }

    private static void hitBowFromFront(GameTestHelper h, FakePlayer player) {
        var attacker = EntityType.ZOMBIE.create(h.getLevel());
        attacker.setPos(player.position().add(player.getLookAngle().scale(3)));
        player.invulnerableTime = 0;
        float health = player.getHealth();
        h.assertTrue(player.hurt(h.getLevel().damageSources().mobAttack(attacker), 2),
                "The ring must not parry a frontal hit during bow drawing");
        h.assertTrue(player.getHealth() < health, "Bow interruption protection must not prevent damage");
        attacker.discard();
    }

    @GameTest(template = TEMPLATE)
    public static void damageProtectionIgnoresSpellOverrideButRequiresBowOwnership(GameTestHelper h) {
        try (var config = BowGameTestSupport.useElementalBowSpellConfig(h)) {
            for (var hand : InteractionHand.values()) {
                var spell = new LifecycleSpell();
                var player = damageablePlayer(h, hand, io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_ARROW_SPELL.get());
                player.tickCount = 10;
                // レジストリ全体を変更せず、被ダメージ経路にも override を持つ同一の魔法を渡す。
                var magic = new MagicData(player) {
                    @Override public SpellData getCastingSpell() {
                        return new SpellData(spell, 1);
                    }
                };
                player.setData(DataAttachmentRegistry.MAGIC_DATA, magic);
                magic.setMana(1000);
                var stack = player.getItemInHand(hand);
                try {
                    h.assertTrue(ElementalBowPendingCast.begin(player, stack, hand, spell, 1, 20), "Probe draw must start");
                    h.assertTrue(spell.canBeInterrupted(player), "Probe must explicitly allow interruption");
                    hitBowFromFront(h, player);
                    h.assertTrue(ElementalBowPendingCast.isManagedCast(player), "Spell override must not cancel bow drawing");
                    h.assertTrue(ElementalBowPendingCast.release(player, stack, spell, 1, 20), "Damaged probe must release");
                    h.assertTrue(spell.casts == 1 && spell.cancelled == 0, "Protected probe must cast without cancellation");
                    // 弓の所持だけでは通常詠唱の中断やリング防御を変更しない。
                    magic.initiateCast(spell, 1, 20, CastSource.SPELLBOOK, "mainhand");
                    hitBowFromFront(h, player);
                    h.assertFalse(magic.isCasting(), "Ordinary casting while holding a bow must still be interrupted");
                } finally {
                    ElementalBowPendingCast.cancel(player);
                    magic.resetCastingState();
                    player.discard();
                }
                // 同一 tick の装備検索キャッシュを避け、最初から指輪を装備した別プレイヤーで比較する。
                var defender = damageablePlayer(h, hand, io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_ARROW_SPELL.get());
                var defenderMagic = MagicData.getPlayerMagicData(defender);
                try {
                    defender.tickCount = 10;
                    BowGameTestSupport.equipCurio(defender, Curios.RING_SLOT,
                            new ItemStack(ItemRegistry.SPELL_CAST_PARRYING_RING.get()));
                    h.assertTrue(SpellCastParryingRingDefenseEvent.isEquippedBy(defender),
                            "Ordinary casting control must equip the ring");
                    defenderMagic.getSyncedData();
                    defenderMagic.initiateCast(spell, 1, 20, CastSource.SPELLBOOK, "mainhand");
                    var attacker = EntityType.ZOMBIE.create(h.getLevel());
                    attacker.setPos(defender.position().add(defender.getLookAngle().scale(3)));
                    float health = defender.getHealth();
                    defender.hurt(h.getLevel().damageSources().mobAttack(attacker), 2);
                    h.assertTrue(defender.getHealth() == health && defenderMagic.isCasting(),
                            "The ring must still parry ordinary casting while a bow is held");
                    attacker.discard();
                } finally {
                    defenderMagic.resetCastingState();
                    defender.discard();
                }
            }
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void newObserversReceiveOnlyCurrentBowCast(GameTestHelper h) {
        try (var config = BowGameTestSupport.useElementalBowSpellConfig(h)) {
            var spell = SpellRegistry.LUNAR_AIM.get();
            var caster = player(h, InteractionHand.MAIN_HAND, spell);
            var observer = player(h, InteractionHand.MAIN_HAND, spell);
            var packets = new ArrayList<SyncElementalBowCastPacket>();
            var connection = new Connection(PacketFlow.SERVERBOUND);
            var channel = new EmbeddedChannel(connection);
            NetworkRegistry.configureMockConnection(connection);
            var previous = observer.connection;
            new ServerGamePacketListenerImpl(h.getLevel().getServer(), connection, observer,
                    CommonListenerCookie.createInitial(observer.getGameProfile(), false)) {
                @Override public void send(Packet<?> packet) {
                    if (packet instanceof ClientboundCustomPayloadPacket payload
                            && payload.payload() instanceof SyncElementalBowCastPacket cast) {
                        packets.add(cast);
                    }
                }
            };
            try {
                begin(h, caster, InteractionHand.MAIN_HAND);
                var event = new PlayerEvent.StartTracking(observer, caster);
                NeoForge.EVENT_BUS.post(event);
                h.assertTrue(packets.size() == 1 && packets.getFirst().active()
                                && packets.getFirst().playerId().equals(caster.getUUID())
                                && packets.getFirst().spellId().equals(spell.getSpellId()),
                        "A new observer must receive the caster's active bow spell");
                packets.clear();
                // 終了を観測できなかった後の再追跡で、過去の弓詠唱を再送しない。
                ElementalBowPendingCast.cancel(caster);
                NeoForge.EVENT_BUS.post(event);
                h.assertTrue(packets.isEmpty(), "Tracking after cancellation must not restore a finished bow cast");
                begin(h, caster, InteractionHand.MAIN_HAND);
                caster.stopUsingItem();
                NeoForge.EVENT_BUS.post(event);
                h.assertTrue(packets.isEmpty(), "Invalid pending state must not be sent before the next cleanup tick");
                ElementalBowPendingCast.cancel(caster);
                var magic = MagicData.getPlayerMagicData(caster);
                magic.initiateCast(spell, 1, 20, CastSource.SPELLBOOK, "mainhand");
                NeoForge.EVENT_BUS.post(event);
                h.assertTrue(packets.isEmpty(), "Ordinary casting must not be synchronized as a bow cast");
                magic.resetCastingState();
            } finally {
                ElementalBowPendingCast.cancel(caster);
                observer.connection = previous;
                channel.finishAndReleaseAll();
            }
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void lunarAimLocksAtDrawAndHoldsUntilRelease(GameTestHelper helper) {
        holdsTarget(helper, SpellRegistry.LUNAR_AIM.get(), InteractionHand.MAIN_HAND);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void sacredArrowLocksAtDrawInOffhand(GameTestHelper helper) {
        holdsTarget(helper, SpellRegistry.SACRED_ARROW.get(), InteractionHand.OFF_HAND);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void offhandHotbarChangesPreserveTargetAndDrawTime(GameTestHelper helper) {
        holdsTarget(helper, SpellRegistry.SACRED_ARROW.get(), InteractionHand.OFF_HAND, true);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void arrowVolleyPreservesPreconditionTarget(GameTestHelper helper) {
        holdsTarget(helper, io.redspace.ironsspellbooks.api.registry.SpellRegistry.ARROW_VOLLEY_SPELL.get(), InteractionHand.MAIN_HAND);
    }

    private static void holdsTarget(GameTestHelper h, AbstractSpell spell, InteractionHand hand) {
        holdsTarget(h, spell, hand, false);
    }

    private static void holdsTarget(GameTestHelper h, AbstractSpell spell, InteractionHand hand, boolean changeHotbar) {
        var player = player(h, hand, spell);
        var stack = player.getItemInHand(hand);
        var magic = MagicData.getPlayerMagicData(player);
        var target = EntityType.ZOMBIE.create(h.getLevel());
        target.setNoAi(true);
        target.setNoGravity(true);
        target.setPos(player.position().add(0, 0, 5));
        target.addEffect(new MobEffectInstance(MobEffectRegistry.GUIDING_BOLT, 200));
        h.getLevel().addFreshEntity(target);
        begin(h, player, hand);
        var captured = magic.getAdditionalCastData();
        h.assertTrue(captured != null, "Drawing must create targeting data before release");
        if (captured instanceof LunarAimCastData lunar) h.assertTrue(target.getUUID().equals(lunar.targetId()), "Lunar Aim must lock at draw start");
        if (captured instanceof SacredArrowCastData sacred) h.assertTrue(target.getUUID().equals(sacred.targetId()), "Sacred Arrow must lock at draw start");
        player.setYRot(180);
        int ticks = ElementalBow.resolveMagicRequiredDrawTicks(stack) + 10;
        long startedAt = h.getLevel().getGameTime();
        // FakePlayer は world のプレイヤー一覧に入れず、実際の server tick ごとに弓の tick を駆動する。
        for (int i = 1; i < ticks; i++) h.runAfterDelay(i, () -> {
            try (var config = BowGameTestSupport.useElementalBowSpellConfig(h)) {
                // 引き始めと異なるスロットのまま保持・発射し、詠唱時間や対象がリセットされないことを確認する。
                if (changeHotbar) player.getInventory().selected = 3 + (int) (h.getLevel().getGameTime() % 2);
                ElementalBowPendingCast.tick(player);
                h.assertTrue(magic.isCasting() && magic.getAdditionalCastData() == captured,
                        "Holding must preserve the original cast without automatic completion");
                h.assertTrue(magic.getCastDurationRemaining() == Math.max(0,
                                ElementalBow.resolveMagicRequiredDrawTicks(stack) - (h.getLevel().getGameTime() - startedAt)),
                        "Holding must preserve elapsed draw time across hotbar changes");
                assertUnspent(h, player, stack);
            }
        });
        h.runAfterDelay(ticks, () -> {
            int[] casts = {0};
            Consumer<SpellOnCastEvent> listener = event -> {
                if (event.getEntity() != player) return;
                casts[0]++;
                h.assertTrue(magic.getAdditionalCastData() == captured, "Release must not reacquire the target");
            };
            NeoForge.EVENT_BUS.addListener(listener);
            try (var config = BowGameTestSupport.useElementalBowSpellConfig(h)) {
                int elapsed = (int) (h.getLevel().getGameTime() - startedAt);
                stack.getItem().releaseUsing(stack, h.getLevel(), player, stack.getUseDuration(player) - elapsed);
                stack.getItem().releaseUsing(stack, h.getLevel(), player, stack.getUseDuration(player) - elapsed);
                h.assertTrue(casts[0] == 1 && stack.getDamageValue() == 1, "Release must cast and damage exactly once");
                h.assertTrue(player.getInventory().getItem(2).getCount() == 2, "Release must consume exactly one arrow");
                h.assertTrue(magic.getMana() < 1000 && ElementalBowOverheatManager.getState(player).active(), "Successful release must consume mana and apply heat");
                assertCleared(h, player);
                h.assertFalse(magic.getPlayerCooldowns().isOnCooldown(spell), "Held casting must bypass normal cooldown");
                h.succeed();
            } finally {
                NeoForge.EVENT_BUS.unregister(listener);
                ElementalBowPendingCast.cancel(player);
                target.discard();
            }
        });
    }

    @GameTest(template = TEMPLATE)
    public static void changingUsedBowOrMainhandSlotCancelsWithoutConsumption(GameTestHelper h) {
        try (var config = BowGameTestSupport.useElementalBowSpellConfig(h)) {
            for (var hand : InteractionHand.values()) {
                var player = player(h, hand, SpellRegistry.LUNAR_AIM.get());
                var stack = player.getItemInHand(hand);
                try {
                    begin(h, player, hand);
                    player.setItemInHand(hand, stack.copy());
                    ElementalBowPendingCast.tick(player);
                    assertCleared(h, player);
                    assertUnspent(h, player, stack);
                } finally {
                    ElementalBowPendingCast.cancel(player);
                }
            }
            var player = player(h, InteractionHand.MAIN_HAND, SpellRegistry.LUNAR_AIM.get());
            var stack = player.getMainHandItem();
            try {
                begin(h, player, InteractionHand.MAIN_HAND);
                player.getInventory().selected = 3;
                ElementalBowPendingCast.tick(player);
                assertCleared(h, player);
                assertUnspent(h, player, stack);
            } finally {
                ElementalBowPendingCast.cancel(player);
            }
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void cancellationAndFailedReleaseNeverConsume(GameTestHelper h) {
        try (var config = BowGameTestSupport.useElementalBowSpellConfig(h)) {
            for (int reason = 0; reason < 7; reason++) {
                var player = player(h, InteractionHand.MAIN_HAND, SpellRegistry.LUNAR_AIM.get());
                var stack = player.getMainHandItem();
                begin(h, player, InteractionHand.MAIN_HAND);
                switch (reason) {
                    case 0 -> stack.getItem().releaseUsing(stack, h.getLevel(), player, stack.getUseDuration(player) - 1);
                    case 1 -> player.setItemInHand(InteractionHand.MAIN_HAND, stack.copy());
                    case 2 -> player.stopUsingItem();
                    case 3 -> CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString("ElementalBowShotMode", "normal"));
                    case 4 -> ElementalBowPendingCast.cancel(player); // 画面・切断・移動からの共通経路。
                    case 5 -> {
                        MagicData.getPlayerMagicData(player).setMana(0);
                        stack.getItem().releaseUsing(stack, h.getLevel(), player, stack.getUseDuration(player) - 100);
                        MagicData.getPlayerMagicData(player).setMana(1000);
                    }
                    case 6 -> {
                        player.getInventory().setItem(2, ItemStack.EMPTY);
                        stack.getItem().releaseUsing(stack, h.getLevel(), player, stack.getUseDuration(player) - 100);
                        player.getInventory().setItem(2, new ItemStack(Items.ARROW, 3));
                    }
                }
                ElementalBowPendingCast.tick(player);
                ElementalBowPendingCast.cancel(player);
                assertUnspent(h, player, stack);
                assertCleared(h, player);
            }
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void externalCancellationClearsHoldAndAllowsRestart(GameTestHelper h) {
        try (var config = BowGameTestSupport.useElementalBowSpellConfig(h)) {
            var player = player(h, InteractionHand.MAIN_HAND, SpellRegistry.LUNAR_AIM.get());
            begin(h, player, InteractionHand.MAIN_HAND);
            var magic = MagicData.getPlayerMagicData(player);
            SpellRegistry.LUNAR_AIM.get().onServerCastComplete(h.getLevel(), 1, player, magic, true);
            assertCleared(h, player);
            begin(h, player, InteractionHand.MAIN_HAND);
            ElementalBowPendingCast.cancel(player);
            assertUnspent(h, player, player.getMainHandItem());
            assertCleared(h, player);
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void vetoedStartCleansTargetAndExistingCastIsPreserved(GameTestHelper h) {
        try (var config = BowGameTestSupport.useElementalBowSpellConfig(h)) {
            var player = player(h, InteractionHand.MAIN_HAND, SpellRegistry.LUNAR_AIM.get());
            var magic = MagicData.getPlayerMagicData(player);
            Consumer<SpellPreCastEvent> veto = event -> {
                if (event.getEntity() == player) event.setCanceled(true);
            };
            NeoForge.EVENT_BUS.addListener(veto);
            try {
                h.assertFalse(player.getMainHandItem().getItem().use(h.getLevel(), player, InteractionHand.MAIN_HAND)
                        .getResult().consumesAction(), "A vetoed start must fail");
                assertCleared(h, player);
                assertUnspent(h, player, player.getMainHandItem());
            } finally { NeoForge.EVENT_BUS.unregister(veto); }
            var existing = io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_ARROW_SPELL.get();
            magic.initiateCast(existing, 1, 20, CastSource.SPELLBOOK, "mainhand");
            var data = new LunarAimCastData(null, h.getLevel().dimension());
            magic.setAdditionalCastData(data);
            h.assertFalse(player.getMainHandItem().getItem().use(h.getLevel(), player, InteractionHand.MAIN_HAND)
                    .getResult().consumesAction(), "Another active cast must reject bow drawing");
            h.assertTrue(magic.isCasting() && existing.getSpellId().equals(magic.getCastingSpellId())
                    && magic.getAdditionalCastData() == data, "Failed bow start must preserve another cast");
            magic.resetCastingState();
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void lifecycleCallbacksAreScopedAndCompleteOnce(GameTestHelper h) {
        try (var config = BowGameTestSupport.useElementalBowSpellConfig(h)) {
            var spell = new LifecycleSpell();
            var player = player(h, InteractionHand.MAIN_HAND, io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_ARROW_SPELL.get());
            var stack = player.getMainHandItem();
            h.assertTrue(ElementalBowPendingCast.begin(player, stack, InteractionHand.MAIN_HAND, spell, 1, 20), "Probe cast must start");
            h.assertTrue(ElementalBowPendingCast.shouldBypassMagicManager(MagicData.getPlayerMagicData(player)),
                    "Standard MagicManager must bypass the managed cast");
            for (int i = 0; i < 3; i++) ElementalBowPendingCast.tick(player);
            h.assertTrue(spell.starts == 1 && spell.ticks == 3 && spell.casts == 0, "Holding must call start once and each managed tick once");
            h.assertTrue(ElementalBowPendingCast.release(player, stack, spell, 1, 20), "Ready probe must release");
            ElementalBowPendingCast.cancel(player);
            h.assertTrue(spell.casts == 1 && spell.completions == 1 && spell.cancelled == 0 && spell.scoped,
                    "Successful release must complete once with the bow scope active");
            h.assertFalse(ElementalBowCasting.isActive(player, spell), "Callback scope must not escape into later work");
            h.assertTrue(ElementalBowPendingCast.begin(player, stack, InteractionHand.MAIN_HAND, spell, 1, 20), "Probe must restart");
            ElementalBowPendingCast.cancel(player);
            ElementalBowPendingCast.cancel(player);
            h.assertTrue(spell.completions == 2 && spell.cancelled == 1, "Repeated cancellation must notify once");
            assertCleared(h, player);
        }
        h.succeed();
    }

    private static final class LifecycleSpell extends FireArrowSpell {
        int starts, ticks, casts, completions, cancelled;
        boolean scoped = true;

        @Override
        public boolean canBeInterrupted(Player player) {
            return true;
        }

        @Override
        public void onServerPreCast(Level level, int spellLevel,
                                    LivingEntity entity, MagicData magic) {
            starts++;
            scoped &= ElementalBowCasting.isActive((FakePlayer) entity, this);
        }

        @Override
        public void onServerCastTick(Level level, int spellLevel,
                                     LivingEntity entity, MagicData magic) {
            ticks++;
            scoped &= ElementalBowCasting.isActive((FakePlayer) entity, this);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity entity,
                           CastSource source, MagicData magic) {
            casts++;
            scoped &= ElementalBowCasting.isActive((FakePlayer) entity, this);
        }

        @Override
        public void onServerCastComplete(Level level, int spellLevel,
                                         LivingEntity entity, MagicData magic, boolean wasCancelled) {
            completions++;
            if (wasCancelled) cancelled++;
            scoped &= ElementalBowCasting.isActive((FakePlayer) entity, this);
            super.onServerCastComplete(level, spellLevel, entity, magic, wasCancelled);
        }
    }

    private static FakePlayer player(GameTestHelper h, InteractionHand hand, AbstractSpell spell) {
        var player = BowGameTestSupport.createEquipmentTestPlayer(h, new BlockPos(2, 80, 2), "bow_long");
        return preparePlayer(h, hand, spell, player);
    }

    private static FakePlayer damageablePlayer(GameTestHelper h, InteractionHand hand, AbstractSpell spell) {
        var player = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "bow_damage")) {
            @Override public boolean isInvulnerableTo(DamageSource source) {
                return false;
            }
        };
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        // FakePlayer の無敵と生成直後の保護を解除し、通常のダメージ経路を通す。
        try {
            var field = ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
            field.setAccessible(true);
            field.setInt(player, 0);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to disable spawn protection for GameTest", exception);
        }
        player.setPos(h.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(2, 80, 2))));
        // Curios のスロット初期化は entity の world 参加時に行われる。
        h.getLevel().addFreshEntity(player);
        return preparePlayer(h, hand, spell, player);
    }

    private static FakePlayer preparePlayer(GameTestHelper h, InteractionHand hand, AbstractSpell spell, FakePlayer player) {
        player.getAttribute(AttributeRegistry.MAX_MANA).setBaseValue(2000);
        MagicData.getPlayerMagicData(player).setMana(1000);
        var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
        ElementalBow.setCalibrationScroll(stack, 0, BowGameTestSupport.createSpellScroll(spell), h.getLevel().registryAccess());
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString("ElementalBowShotMode", "magic");
            tag.putString("ElementalBowMode", ElementalBow.selectionIdForSlot(0).toString());
        });
        player.setItemInHand(hand, stack);
        player.getInventory().setItem(2, new ItemStack(Items.ARROW, 3));
        return player;
    }

    private static void begin(GameTestHelper h, FakePlayer player, InteractionHand hand) {
        try (var config = BowGameTestSupport.useElementalBowSpellConfig(h)) {
            h.assertTrue(player.getItemInHand(hand).getItem().use(h.getLevel(), player, hand).getResult().consumesAction(),
                    "LONG bow draw must begin");
            h.assertTrue(ElementalBowPendingCast.isPending(player), "Drawing must own a pending cast");
        }
    }

    private static void assertUnspent(GameTestHelper h, FakePlayer player, ItemStack stack) {
        h.assertTrue(stack.getDamageValue() == 0 && player.getInventory().getItem(2).getCount() == 3
                && MagicData.getPlayerMagicData(player).getMana() == 1000
                && !ElementalBowOverheatManager.getState(player).active(), "Unfired casts must not spend resources or add heat");
    }

    private static void assertCleared(GameTestHelper h, FakePlayer player) {
        var magic = MagicData.getPlayerMagicData(player);
        h.assertTrue(!ElementalBowPendingCast.isPending(player) && !magic.isCasting()
                && magic.getAdditionalCastData() == null, "Completion must clear the hold and targeting data");
    }
}
