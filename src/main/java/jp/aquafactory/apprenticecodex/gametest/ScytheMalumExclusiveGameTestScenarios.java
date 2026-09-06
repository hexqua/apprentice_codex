package jp.aquafactory.apprenticecodex.gametest;

import com.sammy.malum.common.entity.scythe.LocalizedMaelstromEntity;
import com.sammy.malum.common.item.curiosities.weapons.scythe.MalumScytheItem;
import com.sammy.malum.core.handlers.SoulDataHandler;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.malum.MalumScytheMaelstromCompat;
import jp.aquafactory.apprenticecodex.compat.malum.MalumSpellReaperScytheBridge;
import jp.aquafactory.apprenticecodex.config.item.SpellReaperScytheServerConfig.Values;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.item.spellreaperscythe.*;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;

final class ScytheMalumExclusiveGameTestScenarios {
    private static final String TEMPLATE = "gametest/basic_floor";

    private static void equip(FakePlayer p, String slot, String item) {
        // 本家のruneスロットは初期状態で0枠。FakePlayerにも装備可能な枠を用意する。
        var stacks = CuriosApi.getCuriosInventory(p).orElseThrow().getCurios().get(slot).getStacks();
        if (stacks.getSlots() == 0) stacks.grow(1);
        CuriosApi.getCuriosInventory(p).orElseThrow().setEquippedCurio(slot, 0,
                new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("malum", item))));
        if (!item.equals("air") && stacks.getStackInSlot(0).isEmpty()) throw new IllegalStateException("Test curio was not equipped");
    }

    public static void maelstromCostBounds(GameTestHelper h) {
        var config = new Values(200, 40, 10);
        h.assertTrue(config.maelstromManaCost(1) == 250 && config.maelstromManaCost(2) == 220
                && config.maelstromManaCost(3) == 190 && config.maelstromManaCost(10) == 0,
                "Maelstrom must use its own level cost and clamp at zero");
        h.assertTrue(new Values(0, 0, 0, 0, 0, 0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE)
                .maelstromManaCost(Integer.MAX_VALUE) == 0, "Maelstrom cost must not overflow");
        h.succeed();
    }

    public static void maelstromReturnsAtFixedSpeedAndChargesOnce(GameTestHelper h) {
        if (!MalumSpellReaperScytheBridge.isAvailable()) { h.succeed(); return; }
        var p = ScytheReboundGameTests.player(h, 500);
        ScytheReboundGameTests.enchant(h, p.getMainHandItem(), "rebound", 1);
        equip(p, "ring", "ring_of_the_howling_maelstrom");
        ScytheReboundGameTests.use(h, p);
        var scythe = ScytheThrowManager.active(p);
        h.assertTrue(scythe != null && scythe.isMaelstrom(), "Ring must activate Maelstrom");
        try {
            equip(p, "ring", "air");
            for (int i = 0; i < 20; i++) scythe.tick();
            h.assertTrue(scythe.isReturning() && !scythe.isHovering() && !scythe.isRemoved(), "Arrival must begin moving return without hovering");
            var from = scythe.position();
            ScytheReboundGameTests.use(h, p);
            scythe.tick();
            h.assertTrue(Math.abs(scythe.position().distanceTo(from) - 1) < 0.001, "Repeated use must not accelerate return");
            p.getInventory().selected = 1;
            from = scythe.position(); scythe.checkOwner(); scythe.tick();
            h.assertTrue(Math.abs(scythe.position().distanceTo(from) - 1) < 0.001, "Slot change must preserve return speed");
            for (int i = 0; i < 20 && !scythe.isRemoved(); i++) scythe.tick();
            h.assertTrue(scythe.isRemoved() && MagicData.getPlayerMagicData(p).getMana() == 250,
                    "Flight, repeated use, and return must not spend additional mana");
        } finally { scythe.discard(); }
        h.succeed();
    }

    public static void reboundDamageAndWeaponSurviveRecall(GameTestHelper h) {
        if (!MalumSpellReaperScytheBridge.isAvailable()) { h.succeed(); return; }
        var p = ScytheReboundGameTests.player(h, 100);
        var scythe = new ScytheThrowEntity(EntityRegistry.SCYTHE_THROW.get(), h.getLevel());
        scythe.prepare(p, p.getMainHandItem(), 0, 16, ScytheThrowEntity.Mode.REBOUND);
        var source = CombatTools.getDamageSource(h.getLevel(), scythe, p, DamageTypes.SPELL_REAPER_SCYTHE_REBOUND);
        scythe.discard();
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        p.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(99);
        var data = MalumScytheItem.getScytheDamage(source, p);
        h.assertTrue(data.isBoomerang() && data.physicalDamage() == 10, "Geas must read captured throw damage after removal");
        h.assertTrue(SoulDataHandler.getScytheWeapon(source, p).getItem() instanceof SpellReaperScythe,
                "Geas must retain the thrown weapon after slot changes");
        h.succeed();
    }

    public static void cullingScalesPhysicalBelowHalfOnly(GameTestHelper h) {
        if (!MalumSpellReaperScytheBridge.isAvailable()) { h.succeed(); return; }
        var p = ScytheReboundGameTests.player(h, 100);
        equip(p, "rune", "rune_of_culling");
        var target = h.spawn(EntityType.HUSK, new BlockPos(2, 20, 5));
        target.setNoAi(true); target.getAttribute(Attributes.ARMOR).setBaseValue(0);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
        var scythe = new ScytheThrowEntity(EntityRegistry.SCYTHE_THROW.get(), h.getLevel());
        for (var mode : new ScytheThrowEntity.Mode[]{ScytheThrowEntity.Mode.NORMAL, ScytheThrowEntity.Mode.REBOUND}) {
            scythe.prepare(p, p.getMainHandItem(), 0, 16, mode);
            for (boolean continuous : new boolean[]{false, true}) {
                for (float health : new float[]{100, 90}) {
                    target.setHealth(health); target.invulnerableTime = 0;
                    ScytheThrowDamage.hit(h.getLevel(), scythe, p, target, p.getMainHandItem(), 10, 2, continuous);
                    float expected = ((health < 100 ? 14 : 10) + 2) * (continuous ? 0.1f : 1);
                    h.assertTrue(Math.abs(health - target.getHealth() - expected) < 0.01,
                            "Culling must affect only physical damage below half health: actual=" + (health-target.getHealth()) + ", expected=" + expected);
                }
            }
        }
        h.succeed();
    }

    public static void narrowMaelstromPlacesNativeStormOnce(GameTestHelper h) {
        if (!MalumSpellReaperScytheBridge.isAvailable()) { h.succeed(); return; }
        var p = ScytheReboundGameTests.player(h, 250); p.setXRot(0);
        equip(p, "ring", "ring_of_the_howling_maelstrom");
        ScytheNarrowGameTests.equip(p, "necklace_of_the_narrow_edge");
        ScytheReboundGameTests.enchant(h, p.getMainHandItem(), "rebound", 1);
        var target = h.spawn(EntityType.HUSK, new BlockPos(2, 20, 5)); target.setNoAi(true);
        ScytheReboundGameTests.use(h, p);
        var scythe = ScytheThrowManager.active(p);
        try {
            scythe.tick();
            h.assertTrue(scythe.isRemoved(), "Narrow Maelstrom must retain instant return");
            var storms = h.getLevel().getEntitiesOfClass(LocalizedMaelstromEntity.class, target.getBoundingBox().inflate(3), e -> e.getOwner() == p);
            h.assertTrue(storms.size() == 1, "First Narrow contact must place exactly one native storm");
            var storm = storms.getFirst();
            h.assertTrue(storm.returnTimer == 40 && Math.abs(storm.damage - 13) < 0.01
                    && storm.getPersistentData().getBoolean(MalumScytheMaelstromCompat.ORIGIN_MARKER),
                    "Native storm must retain the forty-tick lifetime, captured Narrow damage, and origin marker");
            storm.discard();
        } finally { scythe.discard(); }
        h.succeed();
    }

    public static void geasTriggersOnlyForReboundAndRetainsDelayedSource(GameTestHelper h) {
        if (!MalumSpellReaperScytheBridge.isAvailable()) { h.succeed(); return; }
        var p = ScytheReboundGameTests.player(h, 100);
        h.getLevel().addFreshEntity(p);
        var target = h.spawn(EntityType.HUSK, new BlockPos(2, 20, 5));
        target.setNoAi(true); target.getAttribute(Attributes.ARMOR).setBaseValue(0);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200); target.setHealth(200);
        var data = h.getLevel().getData(team.lodestar.lodestone.registry.common.LodestoneAttachmentTypes.WORLD_EVENT_DATA);
        var created = new java.util.ArrayList<team.lodestar.lodestone.systems.worldevent.WorldEventInstance>();
        var scythe = new ScytheThrowEntity(EntityRegistry.SCYTHE_THROW.get(), h.getLevel());
        try {
            h.assertTrue(com.sammy.malum.core.handlers.GeasEffectHandler.addGeasEffect(p,
                    com.sammy.malum.registry.common.magic.MalumGeasEffectTypes.PACT_OF_THE_REAPER.get()), "Test Reaper pact must equip");
            long seed = 0;
            // 近接30%では失敗し投擲60%では成功する値を固定し、確率試行をテストに持ち込まない。
            while (true) {
                float sample = net.minecraft.util.RandomSource.create(seed).nextFloat();
                if (sample > 0.3f && sample < 0.6f) break;
                seed++;
            }
            for (var mode : new ScytheThrowEntity.Mode[]{ScytheThrowEntity.Mode.NORMAL, ScytheThrowEntity.Mode.REBOUND}) {
                scythe.prepare(p, p.getMainHandItem(), 0, 16, mode);
                int before = data.inboundWorldEvents.size();
                target.invulnerableTime = 0; p.getRandom().setSeed(seed);
                ScytheThrowDamage.hit(h.getLevel(), scythe, p, target, p.getMainHandItem(), 10, 0, false);
                created.addAll(data.inboundWorldEvents.subList(before, data.inboundWorldEvents.size()));
                h.assertTrue(data.inboundWorldEvents.size() - before == (mode == ScytheThrowEntity.Mode.NORMAL ? 0 : 2),
                        "Only Rebound must trigger the two Reaper delayed cuts");
            }
            scythe.discard();
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            p.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(99);
            float health = target.getHealth();
            java.util.function.Consumer<net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Pre> listener = event -> {
                if (event.getEntity() == target && event.getSource().is(com.sammy.malum.registry.common.MalumDamageTypes.SCYTHE_COMBO)) {
                    h.assertTrue(event.getSource().getDirectEntity() == scythe, "Delayed cut must retain the removed projectile");
                    h.assertTrue(SoulDataHandler.getScytheWeapon(event.getSource(), p).getItem() instanceof SpellReaperScythe,
                            "Delayed cut must retain the captured weapon");
                }
            };
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(listener);
            try {
                for (var event : created) for (int tick = 0; tick < 10 && !event.discarded; tick++) event.tick(h.getLevel());
            } finally { net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(listener); }
            h.assertTrue(Math.abs(health - target.getHealth() - 4) < 0.01, "Reaper cuts must use captured damage after recall");
        } finally {
            scythe.discard(); p.discard(); data.inboundWorldEvents.removeAll(created);
        }
        h.succeed();
    }

    public static void profaneGluttonConsumesStacksOnDelayedCut(GameTestHelper h) {
        if (!MalumSpellReaperScytheBridge.isAvailable()) { h.succeed(); return; }
        var p = ScytheReboundGameTests.player(h, 100); h.getLevel().addFreshEntity(p);
        // アンデッドは毒を受け付けないため、毒の成立確認には通常の生物を使う。
        var target = h.spawn(EntityType.COW, new BlockPos(2, 20, 5)); target.setNoAi(true);
        target.getAttribute(Attributes.ARMOR).setBaseValue(0);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200); target.setHealth(200);
        var data = h.getLevel().getData(team.lodestar.lodestone.registry.common.LodestoneAttachmentTypes.WORLD_EVENT_DATA);
        var created = new java.util.ArrayList<team.lodestar.lodestone.systems.worldevent.WorldEventInstance>();
        var scythe = new ScytheThrowEntity(EntityRegistry.SCYTHE_THROW.get(), h.getLevel());
        try {
            h.assertTrue(com.sammy.malum.core.handlers.GeasEffectHandler.addGeasEffect(p,
                    com.sammy.malum.registry.common.magic.MalumGeasEffectTypes.PACT_OF_THE_PROFANE_GLUTTON.get()), "Test Glutton pact must equip");
            p.addEffect(new net.minecraft.world.effect.MobEffectInstance(com.sammy.malum.registry.common.MalumMobEffects.DESPERATE_NEED, 200, 6));
            for (var mode : new ScytheThrowEntity.Mode[]{ScytheThrowEntity.Mode.NORMAL, ScytheThrowEntity.Mode.REBOUND}) {
                scythe.prepare(p, p.getMainHandItem(), 0, 16, mode);
                int before = data.inboundWorldEvents.size(); target.invulnerableTime = 0;
                ScytheThrowDamage.hit(h.getLevel(), scythe, p, target, p.getMainHandItem(), 10, 0, false);
                created.addAll(data.inboundWorldEvents.subList(before, data.inboundWorldEvents.size()));
                h.assertTrue(data.inboundWorldEvents.size() - before == (mode == ScytheThrowEntity.Mode.NORMAL ? 0 : 1),
                        "Only Rebound must trigger Glutton's delayed cut");
            }
            int beforeStorm = data.inboundWorldEvents.size();
            target.invulnerableTime = 0;
            target.hurt(CombatTools.getDamageSource(h.getLevel(), scythe, p,
                    com.sammy.malum.registry.common.MalumDamageTypes.SCYTHE_MAELSTROM), 2);
            created.addAll(data.inboundWorldEvents.subList(beforeStorm, data.inboundWorldEvents.size()));
            h.assertTrue(data.inboundWorldEvents.size() - beforeStorm == 1, "Maelstrom damage must also trigger Glutton");
            scythe.discard();
            for (var event : created) for (int tick = 0; tick < 4 && !event.discarded; tick++) event.tick(h.getLevel());
            h.assertTrue(target.hasEffect(net.minecraft.world.effect.MobEffects.POISON), "Glutton cut must apply native poison");
            h.assertTrue(p.getEffect(com.sammy.malum.registry.common.MalumMobEffects.DESPERATE_NEED).getAmplifier() == 3,
                    "Glutton cut must consume native Desperate Need stacks");
        } finally { scythe.discard(); p.discard(); data.inboundWorldEvents.removeAll(created); }
        h.succeed();
    }

    public static void maelstromPulseAndNativeStormRespectAllies(GameTestHelper h) {
        if (!MalumSpellReaperScytheBridge.isAvailable()) { h.succeed(); return; }
        var p = ScytheReboundGameTests.player(h, 250);
        var enemy = h.spawn(EntityType.COW, new BlockPos(2, 20, 3));
        var ally = h.spawn(EntityType.COW, new BlockPos(3, 20, 3));
        for (var target : new net.minecraft.world.entity.animal.Cow[]{enemy, ally}) {
            target.setNoAi(true); target.setNoGravity(true);
            target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200); target.setHealth(200);
        }
        var board = h.getLevel().getScoreboard();
        var team = board.addPlayerTeam("scythe_" + p.getId()); team.setAllowFriendlyFire(false);
        board.addPlayerToTeam(p.getScoreboardName(), team); board.addPlayerToTeam(ally.getScoreboardName(), team);
        var scythe = new ScytheThrowEntity(EntityRegistry.SCYTHE_THROW.get(), h.getLevel());
        scythe.prepare(p, p.getMainHandItem(), 0, 16, ScytheThrowEntity.Mode.REBOUND);
        enemy.setPos(scythe.position().add(2.8, 0, 0));
        int delay = (int) (5 - h.getLevel().getGameTime() % 5);
        h.runAfterDelay(delay, () -> {
            var storm = new LocalizedMaelstromEntity(h.getLevel());
            try {
                h.assertTrue(h.getLevel().getGameTime() % 5 == 0, "Pulse test must run on the native five-tick cadence");
                enemy.invulnerableTime = 20;
                MalumScytheMaelstromCompat.tick(scythe);
                h.assertTrue(Math.abs(enemy.getHealth() - 198) < 0.01 && ally.getHealth() == 200,
                        "Moving storm must bypass immunity for twenty-percent damage while excluding allies");
                storm.setPos(scythe.position()); storm.setData(p, 10, 0, 0, 40);
                storm.getPersistentData().putBoolean(MalumScytheMaelstromCompat.ORIGIN_MARKER, true);
                com.sammy.malum.common.item.curiosities.curios.sets.scythe.CurioHowlingMaelstromRing.handleMaelstrom(h.getLevel(), p, storm);
                h.assertTrue(ally.getHealth() == 200 && Math.abs(enemy.getHealth() - 196) < 0.01 && storm.returnTimer == 41,
                        "Marked native storm must filter allies and retain native lifetime extension");
                storm.getPersistentData().remove(MalumScytheMaelstromCompat.ORIGIN_MARKER);
                com.sammy.malum.common.item.curiosities.curios.sets.scythe.CurioHowlingMaelstromRing.handleMaelstrom(h.getLevel(), p, storm);
                h.assertTrue(ally.getHealth() < 200, "Unmarked native storm must retain Malum target selection");
            } finally { storm.discard(); scythe.discard(); board.removePlayerTeam(team); }
            h.succeed();
        });
    }
}
