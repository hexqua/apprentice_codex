package jp.aquafactory.apprenticecodex.gametest.spell.summonweapon.echo;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.entity.SummonWeaponEntity;
import jp.aquafactory.apprenticecodex.gametest.spell.summonweapon.support.SummonWeaponTestSupport;
import jp.aquafactory.apprenticecodex.gametest.spell.targeting.support.TargetingTestFixture;
import jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle.FullautoRapidcastSpellrifle;
import jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle.FullautoRapidcastSpellrifleScrollStorage;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffAttackHandler;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffCastHelper;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.AbstractSummonWeaponSpell;
import jp.aquafactory.apprenticecodex.spell.higanbana.HiganbanaKatanaEntity;
import jp.aquafactory.apprenticecodex.utility.CombatOwnerUuidSource;
import jp.aquafactory.apprenticecodex.utility.RaycastTools;
import jp.aquafactory.apprenticecodex.utility.SpellCalibrationImbueHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;

import static jp.aquafactory.apprenticecodex.gametest.spell.senseevil.SenseEvilScenarios.spawnPositionedZombie;


final class EchoWeaponTestSupport {
    static final String TEMPLATE = "gametest/summon_echo";
    static final String BATCH = "apprenticecodex.multicast_echo_staff_isolated";

    private EchoWeaponTestSupport() {
    }

    static final class EchoPlayer extends FakePlayer {
        final List<Component> messages = new ArrayList<>();

        private EchoPlayer(GameTestHelper helper, String name) {
            super(helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
            gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
            getAttribute(AttributeRegistry.MAX_MANA).setBaseValue(20000);
        }

        @Override
        public void displayClientMessage(@NotNull Component component, boolean actionBar) {
            messages.add(component);
        }
    }

    static EchoPlayer createEchoPlayer(GameTestHelper helper, BlockPos pos, String name) {
        var player = new EchoPlayer(helper, name);
        var playerSlot = TargetingTestFixture.of(helper).entities(FakePlayer.class).size();
        // 同一テスト内のcaster同士が通常攻撃の標的にならないよう、領域内で離す。
        player.setPos(helper.absoluteVec(Vec3.atBottomCenterOf(pos.offset(64 + 24 * playerSlot, 4, 64))));
        player.setNoGravity(true);
        // callback単位の検証は未登録playerで更新を手動制御する。MagicManagerの自動詠唱更新・マナ回復を混ぜない。
        SummonWeaponTestSupport.trackPlayer(helper, player);
        TargetingTestFixture.of(helper).onClose(() -> MulticastEchoStaffCastHelper.onPlayerLoggedOut(new PlayerEvent.PlayerLoggedOutEvent(player)));
        return player;
    }

    record WeaponCastResult(UUID id, float damage) {
    }

    static WeaponCastResult castAndFinishWeapon(GameTestHelper helper, AbstractSummonWeaponSpell<?> spell, boolean repeated) {
        return castAndFinishWeapon(helper, spell, repeated, false);
    }

    static WeaponCastResult castAndFinishWeapon(GameTestHelper helper, AbstractSummonWeaponSpell<?> spell,
                                                boolean repeated, boolean rifleCast) {
        // 射線・遅延攻撃・派生物の移動を同じ専用領域で追跡する。
        var scope = TargetingTestFixture.of(helper);
        var player = createEchoPlayer(helper, new BlockPos(1, 4, 1), "echo_weapon_matrix");
        // 実攻撃の行列ではownerのUUID解決も通すためworldへ登録する。
        scope.add(player);
        player.setYRot(0);
        player.setXRot(0);
        var target = scope.track(spawnPositionedZombie(helper.getLevel(), player.position().add(
                spell == SpellRegistry.ARTISAN_SMASH.get() ? 0.7 : 0, 0,
                spell == SpellRegistry.BREACHING_ENEMY.get() ? 3 :
                        spell == SpellRegistry.HIGANBANA.get() || spell == SpellRegistry.ARTISAN_SMASH.get() ? 4 : 1.8)));
        target.setNoGravity(true);
        // 散弾の広がり全体を受ける標的を使い、worldの乱数seedを変更せず倍率を比較する。
        target.getAttribute(Attributes.SCALE).setBaseValue(spell == SpellRegistry.BREACHING_ENEMY.get() ? 4 : 2);
        target.refreshDimensions();
        scope.assertVisible(target);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
        target.getAttribute(Attributes.ARMOR).setBaseValue(0);
        target.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
        target.setHealth(target.getMaxHealth());
        var initialHealth = target.getHealth();
        var magic = MagicData.getPlayerMagicData(player);
        magic.setMana(10000);
        var weapons = new ArrayList<SummonWeaponEntity>();
        Consumer<EntityJoinLevelEvent> weaponObserver = event -> {
            if (event.getEntity() instanceof SummonWeaponEntity weapon && weapon.getOwner() == player)
                weapons.add(weapon);
        };
        scope.onClose(() -> NeoForge.EVENT_BUS.unregister(weaponObserver));
        NeoForge.EVENT_BUS.addListener(weaponObserver);
        try {
            scope.run(() -> {
                Runnable cast = () -> spell.castInstantWeapon(helper.getLevel(), 1, player, CastSource.SPELLBOOK, magic);
                if (rifleCast) {
                    var stack = new ItemStack(ItemRegistry.FULLAUTO_RAPIDCAST_SPELLRIFLE.get());
                    var rifle = (FullautoRapidcastSpellrifle) stack.getItem();
                    player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                    player.getInventory().add(new ItemStack(ItemRegistry.FULLAUTO_SPELL_CASTING_ROUND.get(), 2));
                    rifle.trySetCalibrationAdjustment(stack, 0,
                            new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get()), player.registryAccess());
                    if (repeated) {
                        helper.assertTrue(rifle.trySetCalibrationAdjustment(stack, 1,
                                new ItemStack(ItemRegistry.MULTICAST_ECHO_STAFF.get()), player.registryAccess()), "Echo adjustment must fit");
                    }
                    FullautoRapidcastSpellrifleScrollStorage.set(stack, 0,
                            SpellCalibrationImbueHelper.createScroll(new SpellData(spell, 1)), player.registryAccess());
                    helper.assertTrue(rifle.tryTriggerSelectedSpell(player, false), "Rifle weapon cast must start: " + spell.getSpellId());
                    if (spell.getCastType() == CastType.INSTANT) {
                        spell.castSpell(helper.getLevel(), magic.getCastingSpellLevel(), player, CastSource.SWORD, true);
                        spell.onServerCastTick(helper.getLevel(), 1, player, magic);
                        spell.onServerCastComplete(helper.getLevel(), 1, player, magic, false);
                    }
                } else if (repeated) {
                    MulticastEchoStaffAttackHandler.runRepeatedCast(player, spell, cast);
                } else {
                    cast.run();
                }
            });
            // ライフルは完了時に上流がCastDataを解放するため、生成時に捕捉した武器で追跡する。
            helper.assertTrue(!weapons.isEmpty(), "Cast must create a weapon: " + spell.getSpellId());
            var weapon = weapons.getFirst();
            var id = weapon.getUUID();
            if (magic.getAdditionalCastData() != null) magic.getAdditionalCastData().reset();
            magic.setAdditionalCastData(null);
            // 実際のEntity.tickを進め、一時MagicDataがなくても遅延攻撃が成立することを確認する。
            for (int tick = 0; tick < 60; tick++) {
                target.invulnerableTime = repeated ? 20 : 0;
                for (var entity : ownedEntities(helper, player)) {
                    if (!entity.isRemoved()) {
                        scope.step(entity, true);
                    }
                }
            }
            helper.assertTrue(target.getHealth() < initialHealth,
                    "Weapon must deal actual damage: " + spell.getSpellId() + " repeated=" + repeated
                            + " target=" + target.getBoundingBox() + " weapon=" + weapon.position()
                            + " eye=" + weapon.getEyePosition() + " look=" + weapon.getLookAngle()
                            + " aim=" + RaycastTools.raycastFromEye(player, 8, 1, e -> e == target)
                            + " ray=" + RaycastTools.raycast(weapon, weapon.getLookAngle(), 8, 0.25, e -> e == target));
            helper.assertTrue(weapon.isRemoved(), "Weapon must expire after attacking: " + spell.getSpellId());
            if (weapon instanceof HiganbanaKatanaEntity katana) {
                helper.assertTrue(katana.getRemainingSlashCount() == 0, "Higanbana must finish all four slashes");
            }
            return new WeaponCastResult(id, initialHealth - target.getHealth());
        } finally {
            scope.close();
        }
    }

    static List<Entity> ownedEntities(GameTestHelper helper, ServerPlayer player) {
        var scope = TargetingTestFixture.of(helper);
        var entities = scope.entities(Entity.class).stream().filter(entity -> ownedBy(entity, player)).toList();
        entities.forEach(scope::assertVisible);
        return entities;
    }

    private static boolean ownedBy(Entity entity, ServerPlayer player) {
        if (entity instanceof CombatOwnerUuidSource source && player.getUUID().equals(source.getCombatOwnerUuid()))
            return true;
        var visited = Collections.newSetFromMap(new IdentityHashMap<Entity, Boolean>());
        var current = entity;
        while (current instanceof TraceableEntity traceable && visited.add(current)) {
            var owner = traceable.getOwner();
            if (owner == player) return true;
            if (owner == null) return false;
            current = owner;
        }
        return false;
    }
}
