package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.config.SpellConfigManager;
import io.redspace.ironsspellbooks.api.item.curios.AffinityData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.enchantment.Enchantments;
import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.MantleCalibration;
import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.MantleElementalDash;
import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.MantleEnergy;
import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.ShootingStarMantle;
import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.ShootingStarMantleRuntime;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;

import static io.redspace.ironsspellbooks.registries.ItemRegistry.ENDER_RUNE;
import static io.redspace.ironsspellbooks.registries.ItemRegistry.FIRE_RUNE;
import static io.redspace.ironsspellbooks.registries.ItemRegistry.ICE_RUNE;
import static io.redspace.ironsspellbooks.registries.ItemRegistry.LIGHTNING_RUNE;
import static io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class MantleElementalDashGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";
    private static final String BATCH = "mantle_elemental";
    private static SpellConfigManager previousConfig;

    private MantleElementalDashGameTests() { }

    @BeforeBatch(batch = BATCH)
    public static void initializeSpellConfig(ServerLevel level) {
        // GameTestには通常loginがなく、未同期の上流configは全魔法の最大Lvを1として返す。
        // 専用batchで通常のdatapack同期を行い、他のテストへ設定を残さない。
        previousConfig = SpellConfigManager.INSTANCE;
        SpellConfigManager.INSTANCE = new SpellConfigManager();
        SpellConfigManager.INSTANCE.handleServerConfigUpdate();
        SpellConfigManager.onDatapackSync(new OnDatapackSyncEvent(level.getServer().getPlayerList(), null));
    }

    @AfterBatch(batch = BATCH)
    public static void restoreSpellConfig(ServerLevel level) {
        SpellConfigManager.INSTANCE = previousConfig;
    }

    @GameTest(batch = BATCH, template = TEMPLATE)
    public static void allFourSchoolRunesAreExclusive(GameTestHelper helper) {
        var stack = new ItemStack(ItemRegistry.SHOOTING_STAR_MANTLE.get());
        var mantle = (ShootingStarMantle) stack.getItem();
        var runes = List.of(FIRE_RUNE.get(), LIGHTNING_RUNE.get(), ICE_RUNE.get(), ENDER_RUNE.get());
        for (var first : runes) {
            helper.assertTrue(mantle.trySetCalibrationAdjustment(stack, 0, new ItemStack(first)), "School rune must fit");
            for (var other : runes) helper.assertFalse(mantle.trySetCalibrationAdjustment(stack, 1, new ItemStack(other)), "All school runes must be mutually exclusive");
            mantle.trySetCalibrationAdjustment(stack, 0, ItemStack.EMPTY);
        }
        helper.succeed();
    }

    @GameTest(batch = BATCH, template = TEMPLATE)
    public static void baseConsumptionAndOverspendClamp(GameTestHelper helper) {
        var energy = new MantleEnergy(100, false, 0);
        for (int i = 0; i < 20; i++) energy = energy.tickUse();
        helper.assertTrue(energy.energy() == 99 && energy.spentTicks() == 0, "Basic consumption must remain one per second");
        helper.assertTrue(new MantleEnergy(1, false, 19).tickUse(2).recovering(), "Double rate must deplete without negative energy");
        helper.assertTrue(new MantleEnergy(2, false, 0).spend(20).energy() == 0, "Impact cost must clamp at zero");
        helper.succeed();
    }

    @GameTest(batch = BATCH, template = TEMPLATE)
    public static void missingSpellKeepsLevelOneDespiteAffinity(GameTestHelper helper) {
        for (var rune : List.of(FIRE_RUNE.get(), LIGHTNING_RUNE.get())) {
            var player = player(helper, "mantle_missing_spell_" + rune, rune, true);
            var spell = rune == FIRE_RUNE.get() ? SpellRegistry.BURNING_DASH_SPELL.get() : SpellRegistry.VOLT_STRIKE_SPELL.get();
            var ring = new ItemStack(ItemRegistry.ENCHANTED_CIRCLET.get());
            AffinityData.setAffinityData(ring, spell, 2);
            CuriosApi.getCuriosInventory(player).orElseThrow().setEquippedCurio("head", 0, ring);
            var dash = ShootingStarMantleRuntime.state(player).elemental;
            dash.input(player, 0, true, 1, 0);
            helper.assertTrue(dash.level() == 1 && Math.abs(dash.motion().horizontalDistance() - 16.0 / 15) < .001,
                    "Affinity without a selectable target spell must keep the level-one dash");
            ShootingStarMantleRuntime.clear(player);
        }
        helper.succeed();
    }

    @GameTest(batch = BATCH, template = TEMPLATE)
    public static void mantleScrollUsesTranscendenceAffinityAndSpellPower(GameTestHelper helper) {
        var player = player(helper, "mantle_resolved_fire", FIRE_RUNE.get(), true);
        var stack = ShootingStarMantleRuntime.findEquipped(player);
        var mantle = (ShootingStarMantle) stack.getItem();
        var spell = SpellRegistry.BURNING_DASH_SPELL.get();
        helper.assertTrue(mantle.trySetCalibrationAdjustment(stack, 1, new ItemStack(ItemRegistry.SCROLLWOVEN_PARCHMENT.get())),
                "Mantle scroll slot must be enabled");
        var scroll = new ItemStack(SCROLL.get());
        ISpellContainer.createScrollContainer(spell, 4, scroll);
        MantleCalibration.setScroll(stack, 0, scroll, player.registryAccess());
        var enchantment = player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.TRANSCENDENCE);
        stack.enchant(enchantment, 1);
        var ring = new ItemStack(ItemRegistry.ENCHANTED_CIRCLET.get());
        AffinityData.setAffinityData(ring, spell, 2);
        CuriosApi.getCuriosInventory(player).orElseThrow().setEquippedCurio("head", 0, ring);
        player.getAttribute(AttributeRegistry.SPELL_POWER).setBaseValue(1.2);
        player.getAttribute(AttributeRegistry.FIRE_SPELL_POWER).setBaseValue(1.4);
        var dash = ShootingStarMantleRuntime.state(player).elemental;
        dash.input(player, 0, true, 1, 0);
        helper.assertTrue(dash.level() == 7, "Stored level, Transcendence and Affinity must each apply once");
        helper.assertTrue(Math.abs(dash.motion().horizontalDistance() - (15 + 7 * 1.2 * 1.4) / 15) < .001,
                "Fire hover speed must use the resolved level and both power attributes");
        helper.assertTrue(dash.packet(player).level() == 7 && dash.packet(player).motion().equals(dash.motion()),
                "Resolved level and speed must be synchronized together");
        ShootingStarMantleRuntime.clear(player);
        helper.succeed();
    }

    @GameTest(batch = BATCH, template = TEMPLATE)
    public static void lightningUsesHighestEffectiveWheelOption(GameTestHelper helper) {
        var player = player(helper, "mantle_wheel_lightning", LIGHTNING_RUNE.get(), true);
        var spell = SpellRegistry.VOLT_STRIKE_SPELL.get();
        var ring = new ItemStack(ItemRegistry.ENCHANTED_CIRCLET.get());
        AffinityData.setAffinityData(ring, spell, 2);
        CuriosApi.getCuriosInventory(player).orElseThrow().setEquippedCurio("head", 0, ring);
        Consumer<SpellSelectionManager.SpellSelectionEvent> listener = event -> {
            if (event.getEntity() != player) return;
            event.addSelectionOption(new SpellData(spell, 3), "mantle_test_low", 0);
            event.addSelectionOption(new SpellData(spell, 7), "mantle_test_high", 0);
        };
        NeoForge.EVENT_BUS.addListener(listener);
        try {
            player.getAttribute(AttributeRegistry.LIGHTNING_SPELL_POWER).setBaseValue(1.4);
            var dash = ShootingStarMantleRuntime.state(player).elemental;
            dash.input(player, 0, true, 1, 0);
            helper.assertTrue(dash.level() == 9, "Highest wheel option must include Affinity exactly once");
            helper.assertTrue(Math.abs(dash.motion().horizontalDistance() - (15 + 9 * 1.4) / 15) < .001,
                    "Lightning speed must follow the selected effective spell level");
            var start = player.position();
            var target = helper.spawn(EntityType.PIG, new BlockPos(1, 3, 2));
            target.setNoAi(true);
            target.setPos(start.add(0, 0, .9));
            player.setPos(start.add(dash.motion()));
            var effect = player.getEffect(MantleElementalDash.effect(MantleElementalDash.LIGHTNING));
            helper.assertTrue(effect != null && !effect.getEffect().value().applyEffectTick(player, effect.getAmplifier())
                    && target.getHealth() < target.getMaxHealth(), "Variable-speed sweep must hit a target between endpoints");
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
            ShootingStarMantleRuntime.clear(player);
        }
        helper.succeed();
    }

    @GameTest(batch = BATCH, template = TEMPLATE)
    public static void hoverHasFiveBlockFloorAtZeroSpellPower(GameTestHelper helper) {
        var player = player(helper, "mantle_hover_floor", FIRE_RUNE.get(), true);
        player.getAttribute(AttributeRegistry.SPELL_POWER).setBaseValue(0);
        var dash = ShootingStarMantleRuntime.state(player).elemental;
        dash.input(player, 0, true, 1, 0);
        helper.assertTrue(dash.level() == 1 && Math.abs(dash.motion().horizontalDistance() - 1) < .001,
                "Zero spell power must retain the five-block hover movement");
        ShootingStarMantleRuntime.clear(player);
        helper.succeed();
    }

    @GameTest(batch = BATCH, template = TEMPLATE)
    public static void highLevelFireFlightKeepsUpstreamAcceleration(GameTestHelper helper) {
        var base = player(helper, "mantle_flight_base", FIRE_RUNE.get(), false);
        var boosted = player(helper, "mantle_flight_boosted", FIRE_RUNE.get(), false);
        boosted.getAttribute(AttributeRegistry.SPELL_POWER).setBaseValue(1.2);
        boosted.getAttribute(AttributeRegistry.FIRE_SPELL_POWER).setBaseValue(1.4);
        var spell = SpellRegistry.BURNING_DASH_SPELL.get();
        Consumer<SpellSelectionManager.SpellSelectionEvent> listener = event -> {
            if (event.getEntity() == boosted) event.addSelectionOption(new SpellData(spell, 10), "mantle_flight_test", 0);
        };
        NeoForge.EVENT_BUS.addListener(listener);
        try {
            for (var player : List.of(base, boosted)) {
                player.setOnGround(false);
                player.startFallFlying();
                ShootingStarMantleRuntime.tick(player);
            }
            var baseDash = ShootingStarMantleRuntime.state(base).elemental;
            var boostedDash = ShootingStarMantleRuntime.state(boosted).elemental;
            helper.assertTrue(baseDash.level() == 1 && boostedDash.level() == 10,
                    "Flight must use the matching selectable spell level");
            helper.assertTrue(boostedDash.motion().length() > baseDash.motion().length() * 1.8,
                    "High-level fire flight must keep upstream spell-power acceleration without a cap");
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
            ShootingStarMantleRuntime.clear(base);
            ShootingStarMantleRuntime.clear(boosted);
        }
        helper.succeed();
    }

    @GameTest(batch = BATCH, template = TEMPLATE)
    public static void fireHoverUsesInputAndStopsWithoutRepeating(GameTestHelper helper) {
        var player = player(helper, "mantle_fire_input", FIRE_RUNE.get(), true);
        player.setYRot(0);
        var dash = ShootingStarMantleRuntime.state(player).elemental;
        dash.input(player, 0, true, 0, 1);
        helper.assertTrue(dash.level() == 1 && Math.abs(dash.motion().x - 16.0 / 15) < .001 && Math.abs(dash.motion().z) < .001,
                "Fire hover must follow strafe at the level-one spell-power speed");
        helper.assertTrue(energy(player) == 90 && dash.invulnerable(player), "Fire hover must cost ten and grant scoped protection");
        var effect = player.getEffect(MantleElementalDash.effect(MantleElementalDash.FIRE));
        helper.assertTrue(effect != null && effect.tick(player, () -> { }), "Protection effect must survive the activation tick");
        for (int t = 1; t <= 5; t++) {
            int tick = t;
            helper.runAfterDelay(t, () -> {
                dash.input(player, tick, true, 0, 1);
                dash.tick(player);
                if (tick < 5) {
                    helper.assertTrue(effect.tick(player, () -> { }) && dash.invulnerable(player), "Effect ticking must not remove protection before the deadline");
                }
                if (tick == 5) {
                    helper.assertFalse(dash.active(), "Fire hover must not repeat on hold");
                    helper.assertTrue(player.getDeltaMovement().horizontalDistanceSqr() == 0 && energy(player) == 90, "Fire hover must stop and charge only once");
                    ShootingStarMantleRuntime.clear(player);
                    helper.succeed();
                }
            });
        }
    }

    @GameTest(batch = BATCH, template = TEMPLATE)
    public static void lightningHoverUsesSpellPowerAndNeverRepeatsOnHold(GameTestHelper helper) {
        var player = player(helper, "mantle_lightning_hold", LIGHTNING_RUNE.get(), true);
        player.setYRot(0);
        var dash = ShootingStarMantleRuntime.state(player).elemental;
        player.invulnerableTime = 7;
        dash.input(player, 0, true, 1, 0);
        helper.assertTrue(dash.level() == 1 && Math.abs(dash.motion().z - 16.0 / 15) < .001
                && Math.abs(dash.motion().x) < .001, "Lightning must use the level-one spell-power speed");
        helper.assertTrue(dash.end() == player.level().getGameTime() + 5 && energy(player) == 90, "Lightning must last five ticks and cost ten");
        helper.assertTrue(player.invulnerableTime == 7 && dash.invulnerable(player), "Hover protection must preserve preexisting hurt cooldown");
        helper.runAfterDelay(5, () -> {
            dash.input(player, 1, true, 0, -1);
            dash.tick(player);
            helper.assertFalse(dash.active() || dash.invulnerable(player), "Hold must not renew movement or protection");
            helper.assertTrue(player.getDeltaMovement().horizontalDistanceSqr() == 0 && energy(player) == 90, "Natural expiry must stop without another charge");
            dash.input(player, 2, false, 0, 0);
            dash.input(player, 3, true, 0, -1);
            helper.assertTrue(dash.level() == 1 && Math.abs(dash.motion().x + 16.0 / 15) < .001 && energy(player) == 80,
                    "Fresh press must sample direction without gaining levels");
            ShootingStarMantleRuntime.clear(player);
            helper.succeed();
        });
    }

    @GameTest(batch = BATCH, template = TEMPLATE)
    public static void lightningContactHasNoExtraChargeAndRequiresRelease(GameTestHelper helper) {
        var player = player(helper, "mantle_lightning_contact", LIGHTNING_RUNE.get(), true);
        var dash = ShootingStarMantleRuntime.state(player).elemental;
        dash.input(player, 0, true, 1, 0);
        var target = helper.spawn(EntityType.PIG, new BlockPos(1, 3, 1));
        target.setNoAi(true);
        target.setPos(player.position());
        var effect = player.getEffect(MantleElementalDash.effect(MantleElementalDash.LIGHTNING));
        helper.assertFalse(effect.getEffect().value().applyEffectTick(player, effect.getAmplifier()), "Actual upstream contact must terminate the effect");
        helper.assertTrue(target.getHealth() < target.getMaxHealth(), "Contact must retain upstream damage");
        helper.assertTrue(energy(player) == 90 && !dash.active(), "Contact must not add to the initial ten cost");
        dash.contact(player);
        dash.input(player, 1, true, -1, 0);
        helper.assertTrue(energy(player) == 90 && !dash.active(), "Duplicate contact and held input must not charge or restart");
        player.removeEffect(MantleElementalDash.effect(MantleElementalDash.LIGHTNING));
        target.discard();
        helper.runAfterDelay(1, () -> {
            dash.input(player, 2, false, 0, 0);
            dash.input(player, 3, true, -1, 0);
            helper.assertTrue(dash.active() && dash.level() == 1 && energy(player) == 80, "A fresh press must restart at level one");
            ShootingStarMantleRuntime.clear(player);
            helper.succeed();
        });
    }

    @GameTest(batch = BATCH, template = TEMPLATE)
    public static void fireFlightChargesEveryPressButNeverRepeatsOnHold(GameTestHelper helper) {
        var player = player(helper, "mantle_flight_dash", FIRE_RUNE.get(), false);
        var dash = ShootingStarMantleRuntime.state(player).elemental;
        player.setOnGround(false);
        player.startFallFlying();
        dash.tick(player);
        dash.input(player, 0, true, 0, 0);
        helper.assertTrue(dash.active() && dash.level() == 1 && energy(player) == 90, "Flight start and same-tick input must cost ten only once");
        helper.assertTrue(player.invulnerableTime == 20, "Flight must retain upstream hurt cooldown");
        helper.runAfterDelay(1, () -> {
            dash.input(player, 1, false, 0, 0);
            dash.input(player, 2, true, 0, 0);
            helper.assertTrue(energy(player) == 80 && dash.level() == 1, "New press during flight must restart at level one and pay ten");
        });
        for (int t = 2; t <= 16; t++) {
            int tick = t;
            helper.runAfterDelay(t, () -> {
                dash.input(player, tick + 2, true, 0, 0);
                dash.tick(player);
                if (tick == 16) {
                    helper.assertFalse(dash.active(), "Held input must not renew flight dash");
                    helper.assertTrue(energy(player) == 80 && player.isFallFlying(), "Expiry must preserve ordinary flight when energy remains");
                    ShootingStarMantleRuntime.clear(player);
                    helper.succeed();
                }
            });
        }
    }

    @GameTest(batch = BATCH, template = TEMPLATE)
    public static void invalidInputAndNormalSpellDoNotCauseImpactCost(GameTestHelper helper) {
        var player = player(helper, "mantle_dash_boundaries", LIGHTNING_RUNE.get(), true);
        var dash = ShootingStarMantleRuntime.state(player).elemental;
        dash.input(player, 0, true, Float.NaN, 0);
        helper.assertFalse(dash.active(), "Nonfinite input must never start a dash");
        dash.input(player, 1, true, 1, 0);
        dash.input(player, 0, false, 0, 0);
        dash.input(player, 2, true, 0, 1);
        helper.assertTrue(energy(player) == 90, "Old release and duplicate hold must not create another activation");
        player.addEffect(new MobEffectInstance(MobEffectRegistry.VOLT_STRIKE, 10, 8));
        dash.tick(player);
        helper.assertFalse(dash.active(), "Normal spell must take priority");
        helper.assertTrue(player.hasEffect(MobEffectRegistry.VOLT_STRIKE) && player.isAutoSpinAttack(), "Cancelling the mantle must preserve normal spell and spin flag");
        helper.assertTrue(energy(player) == 90, "Normal spell takeover must not charge impact cost");
        player.removeEffect(MobEffectRegistry.VOLT_STRIKE);
        ShootingStarMantleRuntime.clear(player);
        helper.succeed();
    }

    @GameTest(batch = BATCH, template = TEMPLATE)
    public static void paidHoverCompletesAfterDepletionButNeverRenews(GameTestHelper helper) {
        var player = player(helper, "mantle_dash_depleted", LIGHTNING_RUNE.get(), true);
        var stack = ShootingStarMantleRuntime.findEquipped(player);
        new MantleEnergy(1, false, 0).save(stack);
        var dash = ShootingStarMantleRuntime.state(player).elemental;
        dash.input(player, 0, true, 1, 0);
        helper.assertTrue(dash.active() && energy(player) == 0 && ShootingStarMantleRuntime.isHovering(player), "Paid interval must survive depletion");
        helper.runAfterDelay(5, () -> {
            dash.input(player, 1, true, 1, 0);
            ShootingStarMantleRuntime.tick(player);
            helper.assertFalse(dash.active() || ShootingStarMantleRuntime.isHovering(player), "Depleted interval must stop without renewal");
            ShootingStarMantleRuntime.clear(player);
            helper.succeed();
        });
    }

    private static int energy(FakePlayer player) { return MantleEnergy.read(ShootingStarMantleRuntime.findEquipped(player)).energy(); }

    @GameTest(batch = BATCH, template = TEMPLATE)
    public static void lightningWallContactStopsWithoutExtraCost(GameTestHelper helper) {
        var player = player(helper, "mantle_dash_wall", LIGHTNING_RUNE.get(), true);
        player.setYRot(0);
        var dash = ShootingStarMantleRuntime.state(player).elemental;
        dash.input(player, 0, true, 1, 0);
        // 上流の予測boxへ壁を置き、独自callbackだけでなく実際の衝突判定を通す。
        var wall = player.blockPosition().offset(0, 0, 1);
        player.level().setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState());
        player.level().setBlockAndUpdate(wall.above(), Blocks.STONE.defaultBlockState());
        var effect = player.getEffect(MantleElementalDash.effect(MantleElementalDash.LIGHTNING));
        helper.assertFalse(effect.getEffect().value().applyEffectTick(player, effect.getAmplifier()), "Wall collision must terminate the upstream effect");
        helper.assertTrue(!dash.active() && energy(player) == 90, "Wall collision must not add an impact cost");
        helper.assertTrue(player.getDeltaMovement().z < 0 && player.getDeltaMovement().y > 0, "Lightning impact must retain upstream recoil");
        ShootingStarMantleRuntime.clear(player);
        helper.succeed();
    }

    @GameTest(batch = BATCH, template = TEMPLATE)
    public static void externalRemovalStopsWithoutImpactOrHeldRestart(GameTestHelper helper) {
        var player = player(helper, "mantle_dash_removed", LIGHTNING_RUNE.get(), true);
        var dash = ShootingStarMantleRuntime.state(player).elemental;
        dash.input(player, 0, true, 1, 0);
        player.removeEffect(MantleElementalDash.effect(MantleElementalDash.LIGHTNING));
        dash.tick(player);
        helper.assertTrue(!dash.active() && energy(player) == 90, "External effect removal must cancel without an impact charge");
        helper.runAfterDelay(5, () -> {
            dash.input(player, 1, true, 1, 0);
            dash.tick(player);
            helper.assertFalse(dash.active(), "Held input must not restart an externally removed effect");
            ShootingStarMantleRuntime.clear(player);
            helper.succeed();
        });
    }

    private static FakePlayer player(GameTestHelper helper, String name, Item rune, boolean hover) {
        // basic_floorの高さは3で、GameTestが置く天井barrierが空中の試験位置に重なる。
        // 床を残してfixture内を空け、足元への視線を含め意図した壁との衝突と区別する。
        for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) for (int y = 1; y <= 5; y++) {
            helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
        }
        var player = new DamageablePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        player.setPos(helper.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(1, 3, 1))));
        var stack = new ItemStack(ItemRegistry.SHOOTING_STAR_MANTLE.get());
        ((ShootingStarMantle) stack.getItem()).trySetCalibrationAdjustment(stack, 0, new ItemStack(rune));
        ApprenticeCodexGameTestScenarios.equipCurio(player, "back", stack);
        ShootingStarMantleRuntime.refreshEquipment(player);
        player.setDeltaMovement(Vec3.ZERO);
        if (hover) ShootingStarMantleRuntime.toggle(player);
        return player;
    }

    private static final class DamageablePlayer extends FakePlayer {
        private DamageablePlayer(ServerLevel level, GameProfile profile) { super(level, profile); }

        @Override
        public boolean isInvulnerableTo(DamageSource source) { return false; }
    }

    @GameTest(batch = BATCH, template = TEMPLATE)
    public static void fireFlightCompletesWithOneOrTenEnergy(GameTestHelper helper) {
        var players = List.of(player(helper, "mantle_flight_one", FIRE_RUNE.get(), false),
                player(helper, "mantle_flight_ten", FIRE_RUNE.get(), false),
                player(helper, "mantle_flight_eleven", FIRE_RUNE.get(), false));
        int[] starting = {1, 10, 11};
        for (int i = 0; i < players.size(); i++) {
            var player = players.get(i);
            new MantleEnergy(starting[i], false, 0).save(ShootingStarMantleRuntime.findEquipped(player));
            player.setOnGround(false);
            player.startFallFlying();
            ShootingStarMantleRuntime.tick(player);
            helper.assertTrue(ShootingStarMantleRuntime.canFly(player), "Paid flight must survive an insufficient balance");
            helper.assertTrue(energy(player) == Math.max(0, starting[i] - 10), "Flight must charge immediately and clamp at zero");
            helper.assertFalse(ShootingStarMantleRuntime.packet(player, false, -1, false).recovering(), "Recovery presentation must wait until paid flight ends");
        }
        helper.runAfterDelay(1, () -> {
            for (int i = 0; i < 2; i++) {
                var dash = ShootingStarMantleRuntime.state(players.get(i)).elemental;
                long expiry = dash.end();
                dash.input(players.get(i), 0, true, 0, 0);
                helper.assertTrue(dash.end() == expiry, "Zero balance must not buy another interval");
            }
        });
        helper.runAfterDelay(15, () -> {
            for (int i = 0; i < players.size(); i++) {
                var player = players.get(i);
                ShootingStarMantleRuntime.tick(player);
                helper.assertFalse(ShootingStarMantleRuntime.state(player).elemental.active(), "Paid flight must expire after fifteen ticks");
                helper.assertTrue(player.isFallFlying() == (i == 2), "Only a positive remaining balance may continue ordinary flight");
                helper.assertTrue(ShootingStarMantleRuntime.packet(player, false, -1, false).recovering() == (i < 2), "Depleted flight must enter recovery on expiry");
                ShootingStarMantleRuntime.clear(player);
            }
            helper.succeed();
        });
    }

    @GameTest(batch = BATCH, template = TEMPLATE)
    public static void depletedFlightStillCancelsOnEquipmentRemoval(GameTestHelper helper) {
        var player = player(helper, "mantle_flight_remove", FIRE_RUNE.get(), false);
        new MantleEnergy(1, false, 0).save(ShootingStarMantleRuntime.findEquipped(player));
        player.setOnGround(false);
        player.startFallFlying();
        ShootingStarMantleRuntime.tick(player);
        ApprenticeCodexGameTestScenarios.equipCurio(player, "back", ItemStack.EMPTY);
        ShootingStarMantleRuntime.tick(player);
        helper.assertFalse(ShootingStarMantleRuntime.state(player).elemental.active() || player.isFallFlying(), "Unequip must override paid flight completion");
        ShootingStarMantleRuntime.clear(player);
        helper.succeed();
    }

    @GameTest(batch = BATCH, template = TEMPLATE)
    public static void lightningNeverActivatesDuringFlight(GameTestHelper helper) {
        var player = player(helper, "mantle_lightning_flight", LIGHTNING_RUNE.get(), false);
        player.setOnGround(false);
        player.startFallFlying();
        ShootingStarMantleRuntime.tick(player);
        var dash = ShootingStarMantleRuntime.state(player).elemental;
        dash.input(player, 0, true, 0, 0);
        helper.assertTrue(!dash.active() && energy(player) == 100 && player.isFallFlying(), "Lightning rune must preserve ordinary flight without casting");
        ShootingStarMantleRuntime.clear(player);
        helper.succeed();
    }

    @GameTest(batch = BATCH, template = TEMPLATE)
    public static void hoverProtectionCancelsDamageOnlyWhileActive(GameTestHelper helper) {
        var player = player(helper, "mantle_hover_protection", FIRE_RUNE.get(), true);
        // FakePlayerは自然tickされないため、初期の60tick保護を明示的に解除して実際の被ダメージを試す。
        try {
            var protection = ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
            protection.setAccessible(true);
            protection.setInt(player, 0);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to disable test player spawn protection", exception);
        }
        var dash = ShootingStarMantleRuntime.state(player).elemental;
        player.invulnerableTime = 0;
        float health = player.getHealth();
        dash.input(player, 0, true, 1, 0);
        player.hurt(player.damageSources().generic(), 3);
        helper.assertTrue(player.getHealth() == health && player.invulnerableTime == 0, "Scoped protection must cancel real damage without replacing hurt cooldown");
        helper.runAfterDelay(5, () -> {
            dash.tick(player);
            player.hurt(player.damageSources().generic(), 3);
            helper.assertTrue(player.getHealth() < health, "Damage must resume at the fifth-tick boundary");
            ShootingStarMantleRuntime.clear(player);
            helper.succeed();
        });
    }

    @GameTest(batch = BATCH, template = TEMPLATE)
    public static void lightningSweepsGroundTargetBetweenEndpoints(GameTestHelper helper) {
        var player = player(helper, "mantle_lightning_sweep", LIGHTNING_RUNE.get(), true);
        player.setYRot(0);
        var start = player.position();
        var target = helper.spawn(EntityType.CHICKEN, new BlockPos(1, 2, 2));
        target.setNoAi(true);
        target.setPos(start.add(0, -1.4, 1));
        var dash = ShootingStarMantleRuntime.state(player).elemental;
        dash.input(player, 0, true, 1, 0);
        player.setPos(start.add(0, 0, 2));
        var effect = player.getEffect(MantleElementalDash.effect(MantleElementalDash.LIGHTNING));
        helper.assertTrue(effect != null, "Lightning effect must be present");
        helper.assertTrue(MantleElementalDash.canTick(player, MantleElementalDash.LIGHTNING), "Sweep must run in a valid hover context");
        helper.assertTrue(player.level().noBlockCollision(player, player.getBoundingBox().expandTowards(0, 0, -2).deflate(.1)), "Sweep fixture must have an unobstructed body path");
        helper.assertTrue(player.level().getEntities(player, player.getBoundingBox().expandTowards(0, -1.5, -2).inflate(.25)).contains(target), "Ground target must be indexed in the swept search region");
        helper.assertTrue(Utils.hasLineOfSight(player.level(), player.getBoundingBox().getCenter().add(0, 0, -1), target.getBoundingBox().getCenter(), true), "Ground target must not be occluded by fixture blocks");
        helper.assertFalse(effect.getEffect().value().applyEffectTick(player, effect.getAmplifier()), "Swept lower hitbox must find a small ground target between endpoints");
        helper.assertTrue(target.getHealth() < target.getMaxHealth() && energy(player) == 90,
                "Swept hit must damage without a second charge: health=" + target.getHealth() + ", energy=" + energy(player)
                        + ", start=" + start + ", target=" + target.position() + ", body=" + player.getBoundingBox());
        helper.assertFalse(dash.invulnerable(player), "Contact must immediately end scoped protection");
        target.discard();
        ShootingStarMantleRuntime.clear(player);
        helper.succeed();
    }

    @GameTest(batch = BATCH, template = TEMPLATE)
    public static void lightningSweepNeverHitsBehindWall(GameTestHelper helper) {
        var player = player(helper, "mantle_lightning_occluded", LIGHTNING_RUNE.get(), true);
        player.setYRot(0);
        var start = player.position();
        var wall = player.blockPosition().offset(0, 0, 1);
        for (int x = -1; x <= 1; x++) for (int y = -1; y <= 2; y++) {
            player.level().setBlockAndUpdate(wall.offset(x, y, 0), Blocks.STONE.defaultBlockState());
        }
        var target = helper.spawn(EntityType.PIG, new BlockPos(1, 3, 3));
        target.setNoAi(true);
        target.setPos(start.add(0, 0, 2.5));
        var dash = ShootingStarMantleRuntime.state(player).elemental;
        dash.input(player, 0, true, 1, 0);
        player.setPos(start.add(0, 0, 2));
        var effect = player.getEffect(MantleElementalDash.effect(MantleElementalDash.LIGHTNING));
        helper.assertTrue(effect != null, "Lightning effect must be present");
        helper.assertFalse(effect.getEffect().value().applyEffectTick(player, effect.getAmplifier()), "Wall must end the swept attack");
        helper.assertTrue(target.getHealth() == target.getMaxHealth(), "Neither contact nor explosion may hit through the wall");
        target.discard();
        ShootingStarMantleRuntime.clear(player);
        helper.succeed();
    }
}
