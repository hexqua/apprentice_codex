package jp.aquafactory.apprenticecodex.gametest;

import com.google.common.collect.ImmutableMap;
import io.redspace.ironsspellbooks.api.config.SpellConfigHolder;
import io.redspace.ironsspellbooks.api.config.SpellConfigManager;
import io.redspace.ironsspellbooks.api.config.SpellConfigParameter;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.entity.mobs.frozen_humanoid.FrozenHumanoid;
import io.redspace.ironsspellbooks.entity.spells.ice_tomb.IceTombEntity;
import io.redspace.ironsspellbooks.entity.spells.icicle.IcicleProjectile;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public class SupportedIceTombGameTests extends ApprenticeCodexGameTestScenarios {
    private static final String TEMPLATE = "gametest/basic_floor";

    @GameTest(template = TEMPLATE, batch = "apprenticecodex.supported_ice_tomb")
    public static void releasePathsShatterExactlyOnce(GameTestHelper helper) {
        var player = player(helper);
        equip(player, true);
        for (int path = 0; path < 3; path++) {
            var tomb = cast(helper, player, 3);
            var position = tomb.position();
            float health = player.getHealth();
            if (path == 0) {
                tomb.setLifetime(0);
                // Entityの経過tickは通常ServerLevelが進めるため、直接tickするテストでは明示する。
                tomb.tickCount = 1;
                tomb.tick();
            } else if (path == 1) {
                player.setShiftKeyDown(true);
                player.rideTick();
            } else {
                tomb.hurt(helper.getLevel().damageSources().generic(), 2);
            }
            assertBurst(helper, player, tomb, 3);
            helper.assertTrue(player.getHealth() == health, "Shatter must not damage its owner");
            helper.assertTrue(player.position().distanceTo(position) < 4, "Release must not cast Frost Step teleport");
            tomb.destroyTomb();
            helper.assertTrue(shards(helper, tomb).size() == 8, "Repeated cleanup must not shatter again");
            clearShards(helper, tomb);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "apprenticecodex.supported_ice_tomb")
    public static void equipmentAndUnsupportedReleases(GameTestHelper helper) {
        var player = player(helper);
        var lateEquip = cast(helper, player, 1);
        equip(player, true);
        lateEquip.hurt(helper.getLevel().damageSources().generic(), 2);
        assertBurst(helper, player, lateEquip, 1);
        clearShards(helper, lateEquip);
        for (int path = 0; path < 6; path++) {
            equip(player, true);
            var tomb = cast(helper, player, 1);
            switch (path) {
                case 0 -> {
                    equip(player, false);
                    tomb.hurt(helper.getLevel().damageSources().generic(), 2);
                }
                case 1 -> tomb.onAntiMagic(MagicData.getPlayerMagicData(player));
                case 2 -> tomb.kill();
                case 3 -> player.stopRiding();
                case 4 -> tomb.destroyTomb();
                default -> tomb.discard();
            }
            helper.assertTrue(shards(helper, tomb).isEmpty(), "Unsupported release must not shatter: " + path);
            player.stopRiding();
        }
        var unmarked = new IceTombEntity(helper.getLevel(), player);
        unmarked.setPos(player.position());
        helper.getLevel().addFreshEntity(unmarked);
        player.startRiding(unmarked, true);
        unmarked.hurt(helper.getLevel().damageSources().generic(), 2);
        helper.assertTrue(shards(helper, unmarked).isEmpty(), "Non-spell tomb must not shatter");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "apprenticecodex.supported_ice_tomb")
    public static void levelPersistenceAndReleasePower(GameTestHelper helper) {
        var player = player(helper);
        equip(player, true);
        for (int level : new int[]{1, 4, 8}) {
            player.getAttribute(AttributeRegistry.SPELL_POWER).setBaseValue(1);
            var tomb = cast(helper, player, level);
            var tag = new CompoundTag();
            tomb.saveWithoutId(tag);
            helper.assertTrue(tag.getUUID("Owner").equals(player.getUUID()), "Saved owner must remain caster");
            player.stopRiding();
            var restored = new IceTombEntity(helper.getLevel(), player);
            restored.load(tag);
            // このプレイヤーはworld未登録。UUID保存を上で検査し、復元後の参照だけを補う。
            restored.setOwner(player);
            helper.getLevel().addFreshEntity(restored);
            player.startRiding(restored, true);
            // 詠唱時の威力を保存してしまう回帰を検出する。
            player.getAttribute(AttributeRegistry.SPELL_POWER).setBaseValue(2);
            restored.hurt(helper.getLevel().damageSources().generic(), 2);
            assertBurst(helper, player, restored, level);
            clearShards(helper, restored);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "apprenticecodex.supported_ice_tomb")
    public static void discountAndDisabledFrostStep(GameTestHelper helper) throws ReflectiveOperationException {
        var player = player(helper);
        var spell = SpellRegistry.ICE_TOMB_SPELL.get();
        equip(player, true);
        var event = new SpellOnCastEvent(player, spell.getSpellId(), 1, 30, spell.getSchoolType(), CastSource.SPELLBOOK);
        NeoForge.EVENT_BUS.post(event);
        helper.assertTrue(event.getManaCost() == 15, "Ice Tomb cast event must halve mana cost");
        var manager = SpellConfigManager.getInstance();
        var field = SpellConfigManager.class.getDeclaredField("config");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        var original = (Map<AbstractSpell, SpellConfigHolder>) field.get(manager);
        var replacement = new HashMap<>(original);
        var config = new SpellConfigHolder();
        config.set(SpellConfigParameter.ENABLED, false);
        config.set(SpellConfigParameter.POWER_MULTIPLIER, 2.0);
        replacement.put(SpellRegistry.FROST_STEP_SPELL.get(), config);
        try {
            field.set(manager, ImmutableMap.copyOf(replacement));
            helper.assertTrue(!SpellRegistry.FROST_STEP_SPELL.get().isEnabled(), "Frost Step must be disabled in this scenario");
            var tomb = cast(helper, player, 4);
            tomb.hurt(helper.getLevel().damageSources().generic(), 2);
            assertBurst(helper, player, tomb, 4);
            helper.assertTrue(Math.abs(shards(helper, tomb).getFirst().getDamage() - 14) < 0.001,
                    "Disabled Frost Step must retain configured power multiplier");
            clearShards(helper, tomb);
        } finally {
            field.set(manager, original);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "apprenticecodex.supported_ice_tomb")
    public static void puffDealsFrostStepDamage(GameTestHelper helper) {
        var player = player(helper);
        equip(player, true);
        var tomb = cast(helper, player, 4);
        var target = EntityType.COW.create(helper.getLevel());
        target.setNoAi(true);
        target.setPos(tomb.position().add(1.5, 0, 0));
        helper.getLevel().addFreshEntity(target);
        var ally = EntityType.COW.create(helper.getLevel());
        ally.setNoAi(true);
        ally.setPos(tomb.position().add(-1.5, 0, 0));
        helper.getLevel().addFreshEntity(ally);
        var blocked = EntityType.COW.create(helper.getLevel());
        blocked.setNoAi(true);
        blocked.setPos(tomb.position().add(0, 0, 2));
        helper.getLevel().addFreshEntity(blocked);
        for (int x = 0; x <= 4; x++) {
            for (int y = 9; y <= 14; y++) {
                helper.setBlock(new BlockPos(x, y, 3), net.minecraft.world.level.block.Blocks.STONE);
            }
        }
        var scoreboard = helper.getLevel().getScoreboard();
        var team = scoreboard.addPlayerTeam("ice_" + player.getUUID().toString().substring(0, 8));
        team.setAllowFriendlyFire(false);
        scoreboard.addPlayerToTeam(player.getScoreboardName(), team);
        scoreboard.addPlayerToTeam(ally.getScoreboardName(), team);
        float before = target.getHealth();
        float expected = SpellRegistry.FROST_STEP_SPELL.get().getSpellPower(4, player) * 0.5f;
        try {
            tomb.hurt(helper.getLevel().damageSources().generic(), 2);
            helper.assertTrue(Math.abs(before - target.getHealth() - expected) < 0.001,
                    "Puff must deal half of Frost Step shatter damage before projectile impacts");
            helper.assertTrue(ally.getHealth() == ally.getMaxHealth(), "Puff must protect allies");
            helper.assertTrue(blocked.getHealth() == blocked.getMaxHealth(), "Puff must respect line of sight");
            assertBurst(helper, player, tomb, 4);
        } finally {
            scoreboard.removePlayerTeam(team);
            clearShards(helper, tomb);
            target.discard();
            ally.discard();
            blocked.discard();
        }
        helper.succeed();
    }

    private static ServerPlayer player(GameTestHelper helper) {
        return createAssistWingsRider(helper, new BlockPos(2, 10, 2), "ice_tomb_test");
    }

    private static IceTombEntity cast(GameTestHelper helper, ServerPlayer player, int level) {
        player.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(2, 10, 2)));
        SpellRegistry.ICE_TOMB_SPELL.get().onCast(helper.getLevel(), level, player,
                CastSource.SPELLBOOK, MagicData.getPlayerMagicData(player));
        helper.assertTrue(player.getVehicle() instanceof IceTombEntity, "Ice Tomb must mount caster");
        return (IceTombEntity) player.getVehicle();
    }

    private static void equip(ServerPlayer player, boolean equipped) {
        CuriosApi.getCuriosInventory(player).orElseThrow().setEquippedCurio(CuriosSlotConstants.BELT, 0,
                equipped ? new ItemStack(ItemRegistry.PROTECTION_SPELL_SUPPORTER.get()) : ItemStack.EMPTY);
    }

    private static List<IcicleProjectile> shards(GameTestHelper helper, IceTombEntity tomb) {
        return helper.getLevel().getEntitiesOfClass(IcicleProjectile.class, tomb.getBoundingBox().inflate(4));
    }

    private static void clearShards(GameTestHelper helper, IceTombEntity tomb) {
        shards(helper, tomb).forEach(IcicleProjectile::discard);
    }

    private static void assertBurst(GameTestHelper helper, ServerPlayer player, IceTombEntity tomb, int level) {
        helper.assertTrue(tomb.isRemoved(), "Tomb must be removed before shatter");
        var shards = shards(helper, tomb);
        helper.assertTrue(shards.size() == 8, "Release must create exactly eight shards, got " + shards.size());
        float expected = SpellRegistry.FROST_STEP_SPELL.get().getSpellPower(level, player);
        for (var shard : shards) {
            helper.assertTrue(shard.getOwner() == player, "Shard owner must be caster");
            helper.assertTrue(Math.abs(shard.getDamage() - expected) < 0.001, "Shard must use release-time Frost Step power");
            helper.assertTrue(!shard.isNoGravity(), "Shatter shards must retain gravity");
        }
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(FrozenHumanoid.class, tomb.getBoundingBox().inflate(4)).isEmpty(),
                "Shatter must not spawn a visible ice shadow");
    }
}
