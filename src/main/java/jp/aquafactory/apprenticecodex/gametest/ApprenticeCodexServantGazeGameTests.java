package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.servantgaze.ServantGaze;
import jp.aquafactory.apprenticecodex.spell.servantgaze.ServantGazeManager;
import jp.aquafactory.apprenticecodex.spell.servantgaze.ServantGazeStaffEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ApprenticeCodexServantGazeGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";
    // 周辺 Entity を扱う GameTest 同士の並列干渉を避け、ライフサイクル系と攻撃対象系も分離する。
    private static final String LIFECYCLE_ISOLATED_BATCH = "apprenticecodex.servant_gaze_lifecycle_isolated";
    private static final String TARGETING_ISOLATED_BATCH = "apprenticecodex.servant_gaze_targeting_isolated";

    private ApprenticeCodexServantGazeGameTests() {
    }

    @GameTest(template = TEMPLATE, batch = LIFECYCLE_ISOLATED_BATCH)
    public static void servantGazeRecastCreatesAndRemovesSingleStaff(GameTestHelper helper) {
        var player = createPlayer(helper, "servant_gaze_recast");
        var magicData = MagicData.getPlayerMagicData(player);
        servantGaze().onCast(helper.getLevel(), 1, player, CastSource.SPELLBOOK, magicData);

        helper.runAtTickTime(2, () -> {
            helper.assertTrue(findStaffs(helper, player).size() == 1,
                    "First Servant Gaze cast should create exactly one staff");
            helper.assertTrue(magicData.getPlayerRecasts().hasRecastForSpell(servantGaze()),
                    "First Servant Gaze cast should create an active recast");

            servantGaze().castSpell(helper.getLevel(), 1, player, CastSource.SPELLBOOK, true);
        });
        helper.runAtTickTime(4, () -> {
            helper.assertTrue(findStaffs(helper, player).isEmpty(),
                    "Servant Gaze recast should remove its staff");
            helper.assertFalse(magicData.getPlayerRecasts().hasRecastForSpell(servantGaze()),
                    "Servant Gaze recast should finish the active recast");
            helper.assertTrue(magicData.getPlayerCooldowns().isOnCooldown(servantGaze()),
                    "Servant Gaze recast should start the normal cooldown");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = LIFECYCLE_ISOLATED_BATCH)
    public static void directCounterspellRemovesServantGazeAndStartsCooldown(GameTestHelper helper) {
        var player = createPlayer(helper, "servant_gaze_direct_counterspell");
        var magicData = MagicData.getPlayerMagicData(player);
        activate(helper, player, 200, 1, 5.0F, 20.0, 15);

        helper.runAtTickTime(2, () -> {
            var staffs = findStaffs(helper, player);
            helper.assertTrue(staffs.size() == 1,
                    "Servant Gaze should expose exactly one staff to Counterspell");
            staffs.get(0).onAntiMagic(magicData);
        });
        helper.runAtTickTime(4, () -> {
            helper.assertTrue(findStaffs(helper, player).isEmpty(),
                    "Direct Counterspell should remove the Servant Gaze staff");
            helper.assertFalse(magicData.getPlayerRecasts().hasRecastForSpell(servantGaze()),
                    "Direct Counterspell should remove the Servant Gaze recast");
            helper.assertTrue(magicData.getPlayerCooldowns().isOnCooldown(servantGaze()),
                    "Direct Counterspell should start the normal cooldown");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = LIFECYCLE_ISOLATED_BATCH)
    public static void ownerCounterspellRemovesServantGazeAndStartsCooldown(GameTestHelper helper) {
        var player = createPlayer(helper, "servant_gaze_owner_counterspell");
        var magicData = MagicData.getPlayerMagicData(player);
        activate(helper, player, 200, 1, 5.0F, 20.0, 15);
        var recast = magicData.getPlayerRecasts().getRecastInstance(servantGaze().getSpellId());
        helper.assertTrue(recast != null, "Servant Gaze should create a recast before owner Counterspell");

        magicData.getPlayerRecasts().removeRecast(recast, RecastResult.COUNTERSPELL);

        helper.assertTrue(findStaffs(helper, player).isEmpty(),
                "Counterspelling the owner should remove the Servant Gaze staff");
        helper.assertFalse(magicData.getPlayerRecasts().hasRecastForSpell(servantGaze()),
                "Counterspelling the owner should remove the Servant Gaze recast");
        helper.assertTrue(magicData.getPlayerCooldowns().isOnCooldown(servantGaze()),
                "Counterspelling the owner should start the normal cooldown");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = LIFECYCLE_ISOLATED_BATCH)
    public static void servantGazeTimeoutStartsCooldownWithoutTalisman(GameTestHelper helper) {
        var player = createPlayer(helper, "servant_gaze_timeout");
        var magicData = MagicData.getPlayerMagicData(player);
        activate(helper, player, 1, 1, 5.0F, 20.0, 15);

        helper.runAtTickTime(3, () -> {
            helper.assertTrue(findStaffs(helper, player).isEmpty(),
                    "Servant Gaze should remove its staff after the fixed duration");
            helper.assertTrue(magicData.getPlayerCooldowns().isOnCooldown(servantGaze()),
                    "Servant Gaze timeout should start cooldown without the talisman");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = LIFECYCLE_ISOLATED_BATCH)
    public static void servantGazeTalismanSkipsCooldownOnlyOnTimeout(GameTestHelper helper) {
        var player = createPlayer(helper, "servant_gaze_talisman_timeout");
        var magicData = MagicData.getPlayerMagicData(player);
        equipGreaterConjurersTalisman(player);
        activate(helper, player, 1, 1, 5.0F, 20.0, 15);

        helper.runAtTickTime(3, () -> {
            helper.assertTrue(findStaffs(helper, player).isEmpty(),
                    "Servant Gaze should still remove its staff when the talisman suppresses cooldown");
            helper.assertFalse(magicData.getPlayerCooldowns().isOnCooldown(servantGaze()),
                    "Greater Conjurer's Talisman should suppress cooldown only after Servant Gaze timeout");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = LIFECYCLE_ISOLATED_BATCH)
    public static void servantGazeRelocationRecreatesStaffWithoutResettingRecast(GameTestHelper helper) {
        var player = createPlayer(helper, "servant_gaze_relocation");
        var magicData = MagicData.getPlayerMagicData(player);
        activate(helper, player, 200, 3, 9.0F, 12.0, 25);
        var recast = magicData.getPlayerRecasts().getRecastInstance(servantGaze().getSpellId());
        helper.assertTrue(recast != null, "Servant Gaze should create a recast before relocation");
        var oldUuid = ((ServantGaze.ServantGazeCastData) recast.getCastData()).getStaffUuid();
        var remaining = recast.getTicksRemaining();

        ServantGazeManager.relocate(player);

        var relocated = magicData.getPlayerRecasts().getRecastInstance(servantGaze().getSpellId());
        helper.assertTrue(relocated == recast, "Servant Gaze relocation should preserve the active recast");
        helper.assertTrue(relocated.getTicksRemaining() == remaining,
                "Servant Gaze relocation should preserve the remaining duration");
        var castData = (ServantGaze.ServantGazeCastData) relocated.getCastData();
        helper.assertTrue(!oldUuid.equals(castData.getStaffUuid()),
                "Servant Gaze relocation should bind the recast to a newly created staff");
        helper.assertTrue(findStaffs(helper, player).size() == 1,
                "Servant Gaze relocation should leave exactly one staff");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = TARGETING_ISOLATED_BATCH, timeoutTicks = 100)
    public static void servantGazeConsumesManaPerHighestHealthTarget(GameTestHelper helper) {
        var player = createPlayer(helper, "servant_gaze_targeting");
        var magicData = MagicData.getPlayerMagicData(player);
        magicData.setMana(20.0F);
        var low = createZombie(helper, new BlockPos(0, 2, 4), 5.0F);
        var middle = createZombie(helper, new BlockPos(3, 2, 1), 10.0F);
        var high = createZombie(helper, new BlockPos(4, 2, 3), 15.0F);
        activate(helper, player, 100, 1, 2.0F, 4.0, 10);

        // 対象選定後の飛翔体到達は周辺 test の負荷で前後するため、固定 tick ではなく結果成立を待つ。
        helper.succeedWhen(() -> {
            helper.assertTrue(magicData.getMana() == 0.0F,
                    "Servant Gaze should consume one mana payment for each selected target");
            helper.assertTrue(low.getHealth() == 5.0F,
                    "Servant Gaze should leave the lower-health third target unselected when mana is insufficient");
            helper.assertTrue(middle.getHealth() < 10.0F && high.getHealth() < 15.0F,
                    "Servant Gaze should attack the two highest-current-health visible targets");
        });
    }

    private static void activate(GameTestHelper helper, FakePlayer player, int duration, int spellLevel,
                                 float damage, double radius, int manaCost) {
        var magicData = MagicData.getPlayerMagicData(player);
        var castData = new ServantGaze.ServantGazeCastData(spellLevel, damage, radius, manaCost);
        var staff = new ServantGazeStaffEntity(EntityRegistry.SERVANT_GAZE_STAFF.get(), helper.getLevel(), player);
        ServantGazeManager.initialize(player, staff, duration, castData);
        helper.getLevel().addFreshEntity(staff);
        var recast = new RecastInstance(servantGaze().getSpellId(), spellLevel, 2, duration,
                CastSource.SPELLBOOK, castData);
        magicData.getPlayerRecasts().addRecast(recast, magicData);
    }

    private static FakePlayer createPlayer(GameTestHelper helper, String name) {
        var player = new FakePlayer((ServerLevel) helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
        // 構造端では従者の杖が未追跡の隣 chunk へ出るため、召喚位置も構造内に収まる中央へ置く。
        var position = helper.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(2, 2, 2)));
        player.setPos(position.x, position.y, position.z);
        var manaRegen = player.getAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MANA_REGEN.get());
        if (manaRegen != null) manaRegen.setBaseValue(0.0D);
        helper.getLevel().addFreshEntity(player);
        return player;
    }

    private static Zombie createZombie(GameTestHelper helper, BlockPos position, float health) {
        var zombie = EntityType.ZOMBIE.create(helper.getLevel());
        if (zombie == null) throw new IllegalStateException("Failed to create Servant Gaze test zombie");
        var absolute = helper.absoluteVec(Vec3.atBottomCenterOf(position));
        zombie.setPos(absolute.x, absolute.y, absolute.z);
        zombie.setNoAi(true);
        zombie.setHealth(health);
        helper.getLevel().addFreshEntity(zombie);
        return zombie;
    }

    private static java.util.List<ServantGazeStaffEntity> findStaffs(GameTestHelper helper, FakePlayer player) {
        return helper.getLevel().getEntitiesOfClass(ServantGazeStaffEntity.class,
                new AABB(player.position(), player.position()).inflate(32.0),
                staff -> staff.getOwner() != null && player.getUUID().equals(staff.getOwner().getUUID()));
    }

    private static void equipGreaterConjurersTalisman(FakePlayer player) {
        var curiosInventory = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player)
                .orElseThrow(() -> new IllegalStateException("Missing curios inventory for Servant Gaze talisman test"));
        curiosInventory.setEquippedCurio(io.redspace.ironsspellbooks.compat.Curios.NECKLACE_SLOT, 0,
                new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.GREATER_CONJURERS_TALISMAN.get()));
    }

    private static ServantGaze servantGaze() {
        return (ServantGaze) SpellRegistry.SERVANT_GAZE.get();
    }
}
