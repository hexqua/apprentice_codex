package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle.FullautoRapidcastSpellrifle;
import jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle.FullautoRapidcastSpellrifleScrollStorage;
import jp.aquafactory.apprenticecodex.utility.SpellCalibrationImbueHelper;
import net.neoforged.neoforge.common.ModConfigSpec;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.entity.SummonWeaponEntity;
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
import jp.aquafactory.apprenticecodex.spell.higanbana.HiganbanaKatanaEntity;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import jp.aquafactory.apprenticecodex.utility.RaycastTools;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.InteractionHand;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.common.NeoForge;
import java.util.function.Consumer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.common.util.FakePlayer;
import com.mojang.authlib.GameProfile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class MulticastSummonWeaponGameTests extends ApprenticeCodexGameTestScenarios {
    private static final String TEMPLATE = "gametest/basic_floor";
    private static final String BATCH = "apprenticecodex.multicast_echo_staff_isolated";

    private static final class EchoPlayer extends FakePlayer {
        private final List<Component> messages = new ArrayList<>();

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

    private static EchoPlayer createEchoPlayer(GameTestHelper helper, BlockPos pos, String name) {
        var player = new EchoPlayer(helper, name);
        player.setPos(helper.absoluteVec(Vec3.atBottomCenterOf(pos)));
        return player;
    }

    // 全対応武器で「生成しただけ」「タイムアウトで消えただけ」を攻撃不成立として検出する。
    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void everySupportedWeaponAttacksAndExpires(GameTestHelper helper) {
        var spells = List.of(SpellRegistry.SHIDEN.get(), SpellRegistry.BREACHING_ENEMY.get(),
                SpellRegistry.SLASH_BLADE.get(), SpellRegistry.MOON_LIGHT.get(), SpellRegistry.SILENT_ASSASSIN.get(),
                SpellRegistry.ARTISAN_SMASH.get(), SpellRegistry.PRECISION_JACK.get(),
                SpellRegistry.HIGANBANA.get(), SpellRegistry.LETHAL_ASSAULT.get());
        try (var config = ApprenticeCodexServerConfig.useMulticastEchoStaffAttackConfigOverrideForGameTest(true, 1.0D)) {
            for (var spell : spells) {
                // 追跡期間は弾の寿命に合わせて調整するため固定せず、補正は下の実ダメージで検証する。
                helper.assertTrue(MulticastEchoStaffAttackProfileManager.getProfile(spell).isPresent(),
                        "Supported weapon must have an attack profile: " + spell.getSpellId());
                var ids = new HashSet<UUID>();
                float normalDamage = 0;
                for (int cast = 0; cast < 3; cast++) {
                    var result = castAndFinishWeapon(helper, (AbstractSummonWeaponSpell<?>) spell, cast != 0);
                    helper.assertTrue(ids.add(result.id()), "Every cast must create a new weapon: " + spell.getSpellId());
                    if (cast == 0) {
                        normalDamage = result.damage();
                    } else {
                        helper.assertTrue(Math.abs(result.damage() - normalDamage * 0.5F) < 0.02F,
                                "Every attack including delayed hits must be halved exactly once: " + spell.getSpellId()
                                        + " normal=" + normalDamage + " repeated=" + result.damage());
                    }
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void rifleEchoTracksSupportedSummonWeapons(GameTestHelper helper) {
        ModConfigSpec.BooleanValue enabled = ApprenticeCodexServerConfig.SPEC.getValues()
                .get("Items.FullautoRapidcastSpellrifle.echoCastEnabled");
        ModConfigSpec.DoubleValue damage = ApprenticeCodexServerConfig.SPEC.getValues()
                .get("Items.FullautoRapidcastSpellrifle.echoCastDamageMultiplier");
        boolean oldEnabled = enabled.get();
        double oldDamage = damage.get();
        try (var staffSettings = ApprenticeCodexServerConfig.useMulticastEchoStaffAttackConfigOverrideForGameTest(false, 9)) {
            enabled.set(true);
            damage.set(1.0);
            for (var spell : List.of(SpellRegistry.SHIDEN.get(), SpellRegistry.BREACHING_ENEMY.get(),
                    SpellRegistry.SLASH_BLADE.get(), SpellRegistry.MOON_LIGHT.get(), SpellRegistry.SILENT_ASSASSIN.get(),
                    SpellRegistry.ARTISAN_SMASH.get(), SpellRegistry.PRECISION_JACK.get(),
                    SpellRegistry.HIGANBANA.get(), SpellRegistry.LETHAL_ASSAULT.get())) {
                var normal = castAndFinishWeapon(helper, (AbstractSummonWeaponSpell<?>) spell, false, true);
                var echo = castAndFinishWeapon(helper, (AbstractSummonWeaponSpell<?>) spell, true, true);
                helper.assertTrue(Math.abs(echo.damage() - normal.damage() * 0.5F) < 0.02F,
                        "Rifle summon attacks must be halved exactly once, including delayed hits: "
                                + spell.getSpellId() + " normal=" + normal.damage() + " echo=" + echo.damage());
            }
        } finally {
            enabled.set(oldEnabled);
            damage.set(oldDamage);
        }
        helper.succeed();
    }

    private record WeaponCastResult(UUID id, float damage) {
    }

    private static WeaponCastResult castAndFinishWeapon(GameTestHelper helper, AbstractSummonWeaponSpell<?> spell, boolean repeated) {
        return castAndFinishWeapon(helper, spell, repeated, false);
    }

    private static WeaponCastResult castAndFinishWeapon(GameTestHelper helper, AbstractSummonWeaponSpell<?> spell,
                                                       boolean repeated, boolean rifleCast) {
        // templateの壁や成功表示ブロックが銃の照準を遮らない高さで検証する。
        var player = createEchoPlayer(helper, new BlockPos(1, 30, 1), "echo_weapon_matrix");
        player.setYRot(0);
        player.setXRot(0);
        helper.getLevel().addFreshEntity(player);
        var target = spawnPositionedZombie(helper.getLevel(), player.position().add(
                spell == SpellRegistry.ARTISAN_SMASH.get() ? 0.7 : 0, 0,
                spell == SpellRegistry.HIGANBANA.get() || spell == SpellRegistry.ARTISAN_SMASH.get() ? 4 : 1.8));
        target.setNoGravity(true);
        // 横に構える銃の射出高と散弾を受けられる標的にし、命中判定の境界から倍率検証を分離する。
        target.getAttribute(Attributes.SCALE).setBaseValue(2);
        target.refreshDimensions();
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
        target.getAttribute(Attributes.ARMOR).setBaseValue(0);
        target.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
        target.setHealth(target.getMaxHealth());
        var initialHealth = target.getHealth();
        var magic = MagicData.getPlayerMagicData(player);
        magic.setMana(10000);
        var weapons = new ArrayList<SummonWeaponEntity>();
        Consumer<EntityJoinLevelEvent> weaponObserver = event -> {
            if (event.getEntity() instanceof SummonWeaponEntity weapon && weapon.getOwner() == player) weapons.add(weapon);
        };
        NeoForge.EVENT_BUS.addListener(weaponObserver);
        try {
            // 散弾の拡散を固定し、通常発動と追加発動の命中条件をそろえる。
            helper.getLevel().getRandom().setSeed(451);
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
                        ++entity.tickCount;
                        entity.tick();
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
            NeoForge.EVENT_BUS.unregister(weaponObserver);
            ownedEntities(helper, player).forEach(Entity::discard);
            weapons.forEach(Entity::discard);
            target.discard();
            magic.resetCastingState();
            player.discard();
        }
    }

    private static List<Entity> ownedEntities(GameTestHelper helper, ServerPlayer player) {
        return helper.getLevel().getEntitiesOfClass(Entity.class, player.getBoundingBox().inflate(64),
                entity -> entity instanceof TraceableEntity traceable && traceable.getOwner() == player);
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
                        "Repeated weapon must not finish or tick the other normal cast");
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

    private static final class UnsupportedShiden extends Shiden implements IMulticastUnsupportedSpell {
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
                        "Rejected multicast must not queue a delayed cast");
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
        var target = spawnPositionedZombie(helper.getLevel(), player.position().add(0, 0, 3));
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
}
