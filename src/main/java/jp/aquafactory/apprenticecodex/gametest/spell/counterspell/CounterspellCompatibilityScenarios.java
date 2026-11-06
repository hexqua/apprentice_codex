package jp.aquafactory.apprenticecodex.gametest.spell.counterspell;

import io.redspace.ironsspellbooks.api.events.CounterSpellEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.effect.MagicMobEffect;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.effect.PhalanxStance;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.assistwings.AssistWingsWingEntity;
import jp.aquafactory.apprenticecodex.spell.automagnet.AutoMagnetFamiliarEntity;
import jp.aquafactory.apprenticecodex.spell.automagnet.AutoMagnetFamiliarManager;
import jp.aquafactory.apprenticecodex.spell.compoundphial.CompoundPhialProjectileEntity;
import jp.aquafactory.apprenticecodex.spell.demicreatorwings.DemicreatorWings;
import jp.aquafactory.apprenticecodex.spell.demicreatorwings.DemicreatorWingsManager;
import jp.aquafactory.apprenticecodex.spell.extract.ExtractPotionProjectileEntity;
import jp.aquafactory.apprenticecodex.spell.flyswatter.FlySwatterProjectileEntity;
import jp.aquafactory.apprenticecodex.spell.illuminatestellar.IlluminateStellarStarEntity;
import jp.aquafactory.apprenticecodex.spell.inscribeice.InscribeIceDaggerEntity;
import jp.aquafactory.apprenticecodex.spell.magicspear.MagicSpearMissileEntity;
import jp.aquafactory.apprenticecodex.spell.manaslash.ManaSlashProjectileEntity;
import jp.aquafactory.apprenticecodex.spell.mysticshield.MysticShieldDefenseEvent;
import jp.aquafactory.apprenticecodex.spell.mysticshield.MysticShieldProjectileEntity;
import jp.aquafactory.apprenticecodex.spell.mysticshield.MysticShieldShieldEntity;
import jp.aquafactory.apprenticecodex.spell.phalanxcharge.PhalanxCounterSpellEvent;
import jp.aquafactory.apprenticecodex.spell.skyedge.SkyEdgeProjectileEntity;
import jp.aquafactory.apprenticecodex.spell.spectralwing.SpectralWingFlightEvent;
import jp.aquafactory.apprenticecodex.spell.uniteluna.UniteLunaMoonEntity;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import jp.aquafactory.apprenticecodex.utility.PotionContentsHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import static jp.aquafactory.apprenticecodex.gametest.spell.assistwings.AssistWingsScenarios.setAssistWingsState;
import static jp.aquafactory.apprenticecodex.gametest.spell.healingbloom.HealingBloomScenarios.castHealingBloom;
import static jp.aquafactory.apprenticecodex.gametest.spell.healingbloom.HealingBloomScenarios.createHealingBloomPlayer;
import static jp.aquafactory.apprenticecodex.gametest.spell.healingbloom.HealingBloomScenarios.getOwnedHealingBlooms;
import static jp.aquafactory.apprenticecodex.gametest.spell.healingbloom.HealingBloomScenarios.getSingleLivingHealingBloom;
import static jp.aquafactory.apprenticecodex.gametest.spell.healingbloom.HealingBloomScenarios.setHealingBloomFruitCount;
import static jp.aquafactory.apprenticecodex.gametest.spell.mysticshield.MysticShieldScenarios.beginMysticShieldCast;
import static jp.aquafactory.apprenticecodex.gametest.spell.mysticshield.MysticShieldScenarios.completeMysticShieldCast;
import static jp.aquafactory.apprenticecodex.gametest.support.CurioTestSupport.postLivingAttackEventForGameTest;
import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;
import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createTrackedEquipmentTestPlayer;

final class CounterspellCompatibilityScenarios {
    private CounterspellCompatibilityScenarios() {}

    static void counterspellCompatMagicMobEffectsAreMagicMobEffects(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertMagicMobEffect(helper, EffectRegistry.ARCANE_CHARGE.get(), "ArcaneCharge");
            assertMagicMobEffect(helper, EffectRegistry.SENSE_SENSOR.get(), "SenseSensor");
            assertMagicMobEffect(helper, EffectRegistry.ECHO_SPELL.get(), "EchoSpell");
            assertMagicMobEffect(helper, EffectRegistry.MIST_FORM.get(), "MistFormEffect");
            assertMagicMobEffect(helper, EffectRegistry.PALETTE_RECEPTION.get(), "PaletteReception");
            assertMagicMobEffect(helper, EffectRegistry.SPECTRAL_WING.get(), "SpectralWingEffect");
        });
    }

    static void assertMagicMobEffect(GameTestHelper helper, MobEffect effect, String effectName) {
        helper.assertTrue(effect instanceof MagicMobEffect,
                effectName + " should be removable by Counterspell as MagicMobEffect");
    }

    static void spectralWingEffectRemovalClearsFlightState(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 8, 0), "spectral_wing_counterspell_test");
            SpellRegistry.SPECTRAL_WING.get().onCast(helper.getLevel(), 1, player, CastSource.SPELLBOOK, MagicData.getPlayerMagicData(player));

            var spellData = Capabilities.getSpellDataOrNull(player);
            helper.assertTrue(spellData != null, "Spectral Wing test player should have Codex spell data");
            var state = spellData.get(CodexSpellStateTypeRegister.SPECTRAL_WING_STATE);
            helper.assertTrue(state.active && state.startedBySpell,
                    "Spectral Wing cast should activate flight state before effect removal");
            helper.assertTrue(player.hasEffect(EffectRegistry.SPECTRAL_WING),
                    "Spectral Wing cast should apply visual MagicMobEffect before removal");

            player.removeEffect(EffectRegistry.SPECTRAL_WING);
            SpectralWingFlightEvent.onPlayerTick(
                    new PlayerTickEvent.Pre(player)
            );

            state = spellData.get(CodexSpellStateTypeRegister.SPECTRAL_WING_STATE);
            helper.assertFalse(player.hasEffect(EffectRegistry.SPECTRAL_WING),
                    "Removing Spectral Wing effect should not be refreshed from stale state");
            helper.assertFalse(state.active || state.startedBySpell || state.launchGraceTicks != 0 || state.waterGraceTicks != 0,
                    "Removing Spectral Wing effect should clear flight state");
            helper.assertFalse(player.isFallFlying(),
                    "Removing Spectral Wing effect should stop fall flying");
        });
    }

    static void counterspellCompatMagicConstructsDeactivateSafely(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var counterCaster = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "counterspell_construct_caster_test");
            var counterMagicData = MagicData.getPlayerMagicData(counterCaster);

            var assistOwner = createEquipmentTestPlayer(helper, new BlockPos(0, 4, 0), "assist_wings_antimagic_owner_test");
            var assistWing = new AssistWingsWingEntity(EntityRegistry.ASSIST_WINGS_WING.get(), level, assistOwner);
            level.addFreshEntity(assistWing);
            setAssistWingsState(assistOwner, 1, assistWing.getId());
            ((AntiMagicSusceptible) assistWing).onAntiMagic(counterMagicData);
            var assistState = Capabilities.getSpellDataOrNull(assistOwner).get(CodexSpellStateTypeRegister.ASSIST_WINGS_STATE);
            helper.assertTrue(assistWing.isRemoved(), "Assist Wings wing should be discarded by anti-magic");
            helper.assertTrue(assistState.localEntityId == -1 && assistState.doneJump == 1,
                    "Assist Wings anti-magic should clear only the managed wing id");

            var magnetOwner = createEquipmentTestPlayer(helper, new BlockPos(2, 2, 0), "auto_magnet_antimagic_owner_test");
            AutoMagnetFamiliarManager.activate(magnetOwner, 6.0D, 0.0D);
            var familiar = level.getEntitiesOfClass(AutoMagnetFamiliarEntity.class, magnetOwner.getBoundingBox().inflate(4.0D))
                    .stream()
                    .filter(entity -> entity.getOwner() == magnetOwner)
                    .findFirst()
                    .orElse(null);
            helper.assertTrue(familiar != null, "AutoMagnet anti-magic test should spawn a familiar");
            ((AntiMagicSusceptible) familiar).onAntiMagic(counterMagicData);
            var magnetState = Capabilities.getSpellDataOrNull(magnetOwner).get(CodexSpellStateTypeRegister.AUTO_MAGNET_STATE);
            helper.assertTrue(familiar.isRemoved(), "AutoMagnet familiar should be discarded by anti-magic");
            helper.assertFalse(magnetState.active || magnetState.getFamiliarUuid() != null,
                    "AutoMagnet anti-magic should clear active state and familiar UUID");

            var demicreatorOwner = createEquipmentTestPlayer(helper, new BlockPos(4, 2, 0), "demicreator_wings_antimagic_owner_test");
            var demicreatorSpell = (DemicreatorWings) SpellRegistry.DEMICREATOR_WINGS.get();
            var demicreatorMagicData = MagicData.getPlayerMagicData(demicreatorOwner);
            DemicreatorWingsManager.activate(demicreatorOwner, 1, CastSource.SPELLBOOK, demicreatorMagicData, demicreatorSpell);
            DemicreatorWingsManager.ensureFlightGranted(demicreatorOwner);
            var core = DemicreatorWingsManager.getManagedCore(demicreatorOwner);
            helper.assertTrue(core instanceof AntiMagicSusceptible, "Demicreator Wings core should be anti-magic susceptible");
            ((AntiMagicSusceptible) core).onAntiMagic(counterMagicData);
            var demicreatorState = Capabilities.getSpellDataOrNull(demicreatorOwner).get(CodexSpellStateTypeRegister.DEMICREATOR_WINGS_STATE);
            helper.assertFalse(demicreatorState.active || demicreatorState.coreEntityId >= 0 || demicreatorState.wingEntityId >= 0 || demicreatorState.grantedFlight,
                    "Demicreator Wings anti-magic should reset managed state");
            helper.assertFalse(demicreatorOwner.getAbilities().mayfly || demicreatorOwner.getAbilities().flying,
                    "Demicreator Wings anti-magic should strip granted flight");
            helper.assertFalse(demicreatorMagicData.getPlayerRecasts().hasRecastForSpell(demicreatorSpell),
                    "Demicreator Wings anti-magic should remove its recast");

        });
    }

    static void healingBloomAntiMagicUsesDeathCleanup(GameTestHelper helper) {
        var level = helper.getLevel();
        var owner = createHealingBloomPlayer(helper, new BlockPos(0, 2, 0), "healing_bloom_antimagic_owner_test");
        var anchorPos = new BlockPos(0, 2, 0);
        var absoluteAnchorPos = helper.absolutePos(anchorPos);
        helper.setBlock(anchorPos.below(), Blocks.STONE);
        castHealingBloom(helper, owner, 1, anchorPos, false);
        var bloom = getSingleLivingHealingBloom(helper, owner);
        setHealingBloomFruitCount(bloom, 2);
        bloom.setOwner(owner);
        level.setBlockAndUpdate(absoluteAnchorPos.above(), BlockRegistry.HEALING_BLOOM_LIGHT.get().defaultBlockState());

        var counterCaster = createEquipmentTestPlayer(helper, new BlockPos(2, 2, 0), "healing_bloom_antimagic_caster_test");
        ((AntiMagicSusceptible) bloom).onAntiMagic(MagicData.getPlayerMagicData(counterCaster));

        helper.runAtTickTime(3, () -> {
            var spellData = Capabilities.getSpellDataOrNull(owner);
            helper.assertTrue(spellData != null, "Healing Bloom anti-magic test should resolve owner spell data");
            helper.assertTrue(getOwnedHealingBlooms(helper, owner).isEmpty(),
                    "Healing Bloom anti-magic should leave no living managed bloom");
            helper.assertTrue(spellData.get(CodexSpellStateTypeRegister.HEALING_BLOOM_STATE).getBloomUuid() == null,
                    "Healing Bloom anti-magic should clear managed bloom UUID");
            helper.assertTrue(!level.getBlockState(absoluteAnchorPos.above()).is(BlockRegistry.HEALING_BLOOM_LIGHT.get()),
                    "Healing Bloom anti-magic should remove its light block");
            var droppedBerries = level.getEntitiesOfClass(ItemEntity.class, new AABB(absoluteAnchorPos).inflate(1.5D)).stream()
                    .filter(itemEntity -> itemEntity.getItem().is(ItemRegistry.COMFORT_BERRIES.get()))
                    .mapToInt(itemEntity -> itemEntity.getItem().getCount())
                    .sum();
            helper.assertTrue(droppedBerries == 2,
                    "Healing Bloom anti-magic should use normal death drops for stored fruit");
            helper.succeed();
        });
    }

    static void counterspellCompatProjectilesFizzleHarmlessly(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = (ServerLevel) helper.getLevel();
            var caster = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "counterspell_projectile_fizzle_test");
            var target = EntityType.SHEEP.create(level);
            helper.assertTrue(target != null, "Failed to create Counterspell projectile harmless target");
            spawnCounterspellTestEntity(helper, target, new Vec3(2.5D, 2.0D, 2.5D));
            var targetHealth = target.getHealth();

            var compoundPhial = new CompoundPhialProjectileEntity(EntityRegistry.COMPOUND_PHIAL_PROJECTILE.get(), level, caster);
            compoundPhial.setDamage(20.0F);
            compoundPhial.setSplashRadius(4.0F);
            compoundPhial.setPotionColorRandom(level);
            spawnCounterspellTestEntity(helper, compoundPhial, new Vec3(2.5D, 2.0D, 2.5D));
            assertAntiMagicDiscard(helper, caster, compoundPhial, "Compound Phial");

            var extract = new ExtractPotionProjectileEntity(EntityRegistry.EXTRACT_POTION_PROJECTILE.get(), level, caster);
            extract.setItem(PotionContentsHelper.createPotionStack(Items.SPLASH_POTION, Potions.POISON.value()));
            spawnCounterspellTestEntity(helper, extract, new Vec3(2.5D, 2.0D, 2.5D));
            assertAntiMagicDiscard(helper, caster, extract, "Extract potion");

            var flySwatter = new FlySwatterProjectileEntity(EntityRegistry.FLY_SWATTER_PROJECTILE.get(), level, caster);
            flySwatter.setDamage(20.0F);
            flySwatter.setRadius(4.0F);
            spawnCounterspellTestEntity(helper, flySwatter, new Vec3(2.5D, 2.0D, 2.5D));
            assertAntiMagicDiscard(helper, caster, flySwatter, "Fly Swatter");

            var stellar = new IlluminateStellarStarEntity(EntityRegistry.ILLUMINATE_STELLAR_STAR.get(), level, caster);
            stellar.setDamage(20.0F);
            stellar.setDriftProfile(new Vec3(1.0D, 0.0D, 0.0D));
            stellar.setFallbackTarget(target);
            spawnCounterspellTestEntity(helper, stellar, new Vec3(2.5D, 2.0D, 2.5D));
            assertAntiMagicDiscard(helper, caster, stellar, "Illuminate Stellar");

            var manaSlash = new ManaSlashProjectileEntity(EntityRegistry.MANA_SLASH_PROJECTILE.get(), level, caster);
            manaSlash.setDamage(20.0F);
            manaSlash.shoot(new Vec3(1.0D, 0.0D, 0.0D));
            spawnCounterspellTestEntity(helper, manaSlash, new Vec3(2.5D, 2.0D, 2.5D));
            assertAntiMagicDiscard(helper, caster, manaSlash, "Mana Slash");

            var mysticShield = new MysticShieldProjectileEntity(EntityRegistry.MYSTIC_SHIELD_PROJECTILE.get(), level, caster);
            mysticShield.setDamage(20.0F);
            mysticShield.shoot(new Vec3(1.0D, 0.0D, 0.0D));
            spawnCounterspellTestEntity(helper, mysticShield, new Vec3(2.5D, 2.0D, 2.5D));
            assertAntiMagicDiscard(helper, caster, mysticShield, "Mystic Shield projectile");

            var skyEdge = new SkyEdgeProjectileEntity(EntityRegistry.SKY_EDGE_PROJECTILE.get(), level, caster);
            skyEdge.setDamage(20.0F);
            skyEdge.setProjectileVelocity(new Vec3(1.0D, 0.0D, 0.0D), 1.0D);
            spawnCounterspellTestEntity(helper, skyEdge, new Vec3(2.5D, 2.0D, 2.5D));
            assertAntiMagicDiscard(helper, caster, skyEdge, "Sky Edge");

            var inscribeIce = new InscribeIceDaggerEntity(EntityRegistry.INSCRIBE_ICE_DAGGER.get(), level, caster);
            inscribeIce.setDamage(20.0F);
            inscribeIce.setBurstDamage(20.0F);
            inscribeIce.setProjectileVelocity(new Vec3(1.0D, 0.0D, 0.0D), 1.0D);
            spawnCounterspellTestEntity(helper, inscribeIce, new Vec3(2.5D, 2.0D, 2.5D));
            assertAntiMagicDiscard(helper, caster, inscribeIce, "Inscribe Ice dagger");

            assertHealthUnchanged(helper, target, targetHealth,
                    "Counterspell projectile fizzle should not damage nearby targets");
            helper.assertFalse(target.hasEffect(MobEffects.POISON),
                    "Counterspell projectile fizzle should not apply potion effects");
        });
    }

    static void magicSpearAntiMagicBurstDoesNotRestart(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = (ServerLevel) helper.getLevel();
            var caster = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "magic_spear_antimagic_repeat_test");
            var target = EntityType.SHEEP.create(level);
            helper.assertTrue(target != null, "Failed to create Magic Spear anti-magic target");
            spawnCounterspellTestEntity(helper, target, new Vec3(2.5D, 2.0D, 2.5D));
            var targetHealth = target.getHealth();

            var spear = new MagicSpearMissileEntity(EntityRegistry.MAGIC_SPEAR_MISSILE.get(), level, caster);
            spear.setup(20.0F, new Vec3(1.0D, 0.0D, 0.0D), new Vec3(0.0D, 0.0D, 1.0D), target);
            spawnCounterspellTestEntity(helper, spear, new Vec3(2.5D, 2.0D, 2.5D));

            var magicData = MagicData.getPlayerMagicData(caster);
            ((AntiMagicSusceptible) spear).onAntiMagic(magicData);
            helper.assertTrue(spear.isBursting(), "Magic Spear should enter harmless burst state after anti-magic");

            for (var i = 0; i < 7 && !spear.isRemoved(); ++i) {
                spear.tick();
            }

            ((AntiMagicSusceptible) spear).onAntiMagic(magicData);
            helper.assertTrue(spear.isBursting(), "Repeated anti-magic should not leave harmless burst state");

            for (var i = 0; i < 7 && !spear.isRemoved(); ++i) {
                spear.tick();
            }

            helper.assertTrue(spear.isRemoved(),
                    "Repeated anti-magic should not restart Magic Spear harmless burst lifetime");
            assertHealthUnchanged(helper, target, targetHealth,
                    "Magic Spear harmless anti-magic burst should not damage nearby targets");
        });
    }

    static void uniteLunaAntiMagicAmplifiesBurst(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = (ServerLevel) helper.getLevel();
            var caster = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "unite_luna_antimagic_test");
            var target = EntityType.SHEEP.create(level);
            helper.assertTrue(target != null, "Failed to create Unite Luna anti-magic target");
            target.setNoAi(true);
            spawnCounterspellTestEntity(helper, target, new Vec3(10.0D, 2.0D, 2.5D));
            var targetHealth = target.getHealth();

            var moon = new UniteLunaMoonEntity(EntityRegistry.UNITE_LUNA_MOON.get(), level, caster);
            moon.setDamage(2.0F);
            spawnCounterspellTestEntity(helper, moon, new Vec3(2.5D, 2.0D, 2.5D));

            helper.assertTrue(moon instanceof AntiMagicSusceptible,
                    "Unite Luna moon should implement AntiMagicSusceptible");
            ((AntiMagicSusceptible) moon).onAntiMagic(MagicData.getPlayerMagicData(caster));

            helper.assertTrue(moon.getBurstKind() == UniteLunaMoonEntity.BURST_KIND_EXPLOSION,
                    "Unite Luna anti-magic should force explosion burst");
            helper.assertTrue(Math.abs(moon.getBurstCubeSize() - 22.0F) < 0.001F,
                    "Unite Luna anti-magic burst cube size should be doubled max: " + moon.getBurstCubeSize());
            var damageTaken = targetHealth - target.getHealth();
            helper.assertTrue(Math.abs(damageTaken - 6.0F) < 0.1F,
                    "Unite Luna anti-magic should triple damage inside doubled range: " + damageTaken);

            var healthAfterFirstBurst = target.getHealth();
            ((AntiMagicSusceptible) moon).onAntiMagic(MagicData.getPlayerMagicData(caster));
            helper.assertTrue(Math.abs(target.getHealth() - healthAfterFirstBurst) < 0.001F,
                    "Repeated Unite Luna anti-magic should not reapply burst damage");
        });
    }

    static void counterspellCompatSpecialPlayerTargetBehaviors(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();

            var mysticCaster = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mystic_shield_counterspell_target_test");
            mysticCaster.setYRot(0.0f);
            mysticCaster.setXRot(0.0f);
            var attacker = helper.spawn(EntityType.ZOMBIE, new BlockPos(0, 2, 3));
            beginMysticShieldCast(level, mysticCaster, 1);
            var frontAttack = postLivingAttackEventForGameTest(
                    mysticCaster,
                    CombatTools.getDamageSource(level, attacker, DamageTypes.SHOCK),
                    8.0f
            );
            helper.assertTrue(frontAttack.isCanceled(), "Mystic Shield setup should store front damage before Counterspell");

            var mysticShieldEntity = level.getEntitiesOfClass(
                            MysticShieldShieldEntity.class,
                            mysticCaster.getBoundingBox().inflate(4.0D)
                    ).stream()
                    .filter(entity -> entity.getOwner() == mysticCaster)
                    .findFirst()
                    .orElse(null);
            helper.assertTrue(mysticShieldEntity != null, "Mystic Shield cast should spawn a shield entity before Counterspell");
            helper.assertFalse(mysticShieldEntity instanceof AntiMagicSusceptible,
                    "Mystic Shield shield entity should not be a direct Counterspell target");

            var counterCaster = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 5), "mystic_shield_counterspell_caster_test");
            var mysticCounterspell = new CounterSpellEvent(counterCaster, mysticCaster);
            MysticShieldDefenseEvent.onCounterSpell(mysticCounterspell);
            helper.assertFalse(mysticCounterspell.isCanceled(),
                    "Mystic Shield should not cancel the CounterSpellEvent itself");
            helper.assertTrue(mysticShieldEntity.isFading(),
                    "Mystic Shield should break its shield entity immediately when Counterspell hits the caster");
            completeMysticShieldCast(level, mysticCaster, 1, true);
            var reflected = level.getEntitiesOfClass(MysticShieldProjectileEntity.class, mysticCaster.getBoundingBox().inflate(4.0D));
            helper.assertTrue(reflected.isEmpty(),
                    "Mystic Shield interrupted by Counterspell should clear stored damage without firing a reflection");

            var phalanxTarget = createEquipmentTestPlayer(helper, new BlockPos(4, 2, 0), "phalanx_counterspell_target_test");
            phalanxTarget.setYRot(0.0f);
            phalanxTarget.setXRot(0.0f);
            phalanxTarget.addEffect(new MobEffectInstance(
                    BuiltInRegistries.MOB_EFFECT.wrapAsHolder(EffectRegistry.PHALANX_STANCE.get()),
                    20,
                    PhalanxStance.MOVE_SPEED_ENABLED_AMPLIFIER,
                    false,
                    false,
                    true
            ));
            var phalanxFrontCaster = createEquipmentTestPlayer(helper, new BlockPos(4, 2, 3), "phalanx_counterspell_front_test");
            var frontCounterspell = new CounterSpellEvent(phalanxFrontCaster, phalanxTarget);
            PhalanxCounterSpellEvent.onCounterSpell(frontCounterspell);
            helper.assertTrue(frontCounterspell.isCanceled(),
                    "Enhanced Phalanx stance should cancel Counterspell from the front");

            var phalanxBackCaster = createEquipmentTestPlayer(helper, new BlockPos(4, 2, -3), "phalanx_counterspell_back_test");
            var backCounterspell = new CounterSpellEvent(phalanxBackCaster, phalanxTarget);
            PhalanxCounterSpellEvent.onCounterSpell(backCounterspell);
            helper.assertFalse(backCounterspell.isCanceled(),
                    "Enhanced Phalanx stance should not cancel Counterspell from behind");

            var normalPhalanxTarget = createEquipmentTestPlayer(helper, new BlockPos(8, 2, 0), "phalanx_counterspell_normal_target_test");
            normalPhalanxTarget.setYRot(0.0f);
            normalPhalanxTarget.addEffect(new MobEffectInstance(
                    BuiltInRegistries.MOB_EFFECT.wrapAsHolder(EffectRegistry.PHALANX_STANCE.get()),
                    20,
                    PhalanxStance.FIXED_AMPLIFIER,
                    false,
                    false,
                    true
            ));
            var normalFrontCaster = createEquipmentTestPlayer(helper, new BlockPos(8, 2, 3), "phalanx_counterspell_normal_front_test");
            var normalCounterspell = new CounterSpellEvent(normalFrontCaster, normalPhalanxTarget);
            PhalanxCounterSpellEvent.onCounterSpell(normalCounterspell);
            helper.assertFalse(normalCounterspell.isCanceled(),
                    "Unenhanced Phalanx stance should not cancel Counterspell");

            var phalanxMysticTarget = createTrackedEquipmentTestPlayer(helper, new BlockPos(12, 2, 0), "mystic_shield_phalanx_cancel_target_test");
            phalanxMysticTarget.setYRot(0.0f);
            phalanxMysticTarget.setXRot(0.0f);
            phalanxMysticTarget.addEffect(new MobEffectInstance(
                    BuiltInRegistries.MOB_EFFECT.wrapAsHolder(EffectRegistry.PHALANX_STANCE.get()),
                    20,
                    PhalanxStance.MOVE_SPEED_ENABLED_AMPLIFIER,
                    false,
                    false,
                    true
            ));
            beginMysticShieldCast(level, phalanxMysticTarget, 1);
            var phalanxMysticAttacker = helper.spawn(EntityType.ZOMBIE, new BlockPos(12, 2, 3));
            var phalanxMysticFrontAttack = postLivingAttackEventForGameTest(
                    phalanxMysticTarget,
                    CombatTools.getDamageSource(level, phalanxMysticAttacker, DamageTypes.SHOCK),
                    8.0f
            );
            helper.assertTrue(phalanxMysticFrontAttack.isCanceled(),
                    "Mystic Shield setup should store front damage before a Phalanx-canceled Counterspell");

            var phalanxMysticCounterCaster = createEquipmentTestPlayer(helper, new BlockPos(12, 2, 5), "mystic_shield_phalanx_cancel_caster_test");
            var phalanxCanceledCounterspell = new CounterSpellEvent(phalanxMysticCounterCaster, phalanxMysticTarget);
            PhalanxCounterSpellEvent.onCounterSpell(phalanxCanceledCounterspell);
            helper.assertTrue(phalanxCanceledCounterspell.isCanceled(),
                    "Enhanced Phalanx stance should cancel Counterspell before Mystic Shield observes it");
            MysticShieldDefenseEvent.onCounterSpell(phalanxCanceledCounterspell);
            completeMysticShieldCast(level, phalanxMysticTarget, 1, false);
            var reflectedAfterCanceledCounterspell = level.getEntitiesOfClass(
                    MysticShieldProjectileEntity.class,
                    phalanxMysticTarget.getBoundingBox().inflate(4.0D)
            );
            helper.assertTrue(reflectedAfterCanceledCounterspell.size() == 1,
                    "Mystic Shield should keep stored reflection when Counterspell is canceled but got "
                            + reflectedAfterCanceledCounterspell.size());
        });
    }

    static void assertAntiMagicDiscard(GameTestHelper helper, FakePlayer caster, Entity entity, String entityName) {
        helper.assertTrue(entity instanceof AntiMagicSusceptible,
                entityName + " should implement AntiMagicSusceptible");
        ((AntiMagicSusceptible) entity).onAntiMagic(MagicData.getPlayerMagicData(caster));
        helper.assertTrue(entity.isRemoved(), entityName + " should be discarded by anti-magic");
    }

    static void assertHealthUnchanged(GameTestHelper helper, LivingEntity target, float expectedHealth, String message) {
        helper.assertTrue(Math.abs(target.getHealth() - expectedHealth) < 0.001F,
                message + ": expected=" + expectedHealth + ", actual=" + target.getHealth());
    }

    static void spawnCounterspellTestEntity(GameTestHelper helper, Entity entity, Vec3 localPos) {
        var absolutePos = helper.absoluteVec(localPos);
        entity.moveTo(absolutePos.x, absolutePos.y, absolutePos.z, 0.0F, 0.0F);
        entity.setDeltaMovement(Vec3.ZERO);
        helper.getLevel().addFreshEntity(entity);
    }
}
