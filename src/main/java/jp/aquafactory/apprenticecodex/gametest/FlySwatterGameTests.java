package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.AbstractSummonWeaponRecastSpell.SummonWeaponRecastSpellData;
import jp.aquafactory.apprenticecodex.spell.flyswatter.FlySwatter;
import jp.aquafactory.apprenticecodex.spell.flyswatter.FlySwatterLauncherEntity;
import jp.aquafactory.apprenticecodex.spell.flyswatter.FlySwatterProjectileEntity;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class FlySwatterGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";
    private static final String BATCH = "apprenticecodex.fly_swatter";

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void completedLocksAndFreeRecasts(GameTestHelper h) {
        try (var s = new Scene(h)) {
            s.zombie();
            float initialMana = s.data.getMana();
            s.begin();
            h.assertTrue(s.launchers().isEmpty(), "Initial warmup must not summon a launcher");
            s.finish();
            var launcher = s.launcher();
            var recast = s.data.getPlayerRecasts().getRecastInstance(s.spell.getSpellId());
            h.assertTrue(launcher.getLockOnCount() == 0 && recast.getRemainingRecasts() == 2,
                    "Initial summon must preserve all lock slots even with a target in sight");
            float mana = s.data.getMana();
            h.assertTrue(mana == initialMana - s.spell.getManaCost(1),
                    "Initial cast must pay mana once: initial=" + initialMana + ", actual=" + mana + ", cost=" + s.spell.getManaCost(1));
            h.assertTrue(s.spell.getEffectiveCastTime(1, s.owner) == 10, "Recast must take ten ticks");
            s.owner.setItemSlot(EquipmentSlot.FEET, new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_BOOTS.get()));
            h.assertTrue(s.spell.getEffectiveCastTime(1, s.owner) == 5, "Boots must shorten recast to five ticks");
            s.begin(); s.finish();
            h.assertTrue(launcher.getLockOnCount() == 1 && recast.getRemainingRecasts() == 1 && recast.getTicksRemaining() == 120,
                    "First recast must add one lock and refresh the six second window");
            s.begin(); s.finish();
            h.assertTrue(s.data.getMana() == mana && !s.data.getPlayerRecasts().hasRecastForSpell(s.spell),
                    "Last recast must be free and end the session");
            h.assertTrue(launcher.isReleased() && launcher.getLockOnCount() == 2,
                    "Repeated locks must remain separate shots");
            s.fireTicks(launcher, 9);
            h.assertTrue(s.projectiles().isEmpty(), "Launch sequence must retain its ten tick delay");
            s.fireTicks(launcher, 1);
            h.assertTrue(s.projectiles().size() == 1, "First shot must launch on tick ten");
            s.fireTicks(launcher, 2);
            h.assertTrue(s.projectiles().size() == 1, "Second shot must wait three ticks");
            s.fireTicks(launcher, 1);
            h.assertTrue(s.projectiles().size() == 2, "Completed locks must launch at a three tick interval");
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void failedAimAndCancelledCastPreserveSession(GameTestHelper h) {
        try (var s = new Scene(h)) {
            s.zombie(); s.begin(); s.finish(); s.begin(); s.finish();
            var launcher = s.launcher();
            var recast = s.data.getPlayerRecasts().getRecastInstance(s.spell.getSpellId());
            s.data.getPlayerRecasts().tick(1);
            int ticks = recast.getTicksRemaining();
            float mana = s.data.getMana();
            s.owner.setXRot(-90);
            h.assertFalse(s.spell.checkPreCastConditions(h.getLevel(), 1, s.owner, s.data), "Empty recast aim must fail");
            h.assertTrue(recast.getRemainingRecasts() == 1 && recast.getTicksRemaining() == ticks && s.data.getMana() == mana,
                    "Failed aim must not spend mana, consume a lock, or refresh the window");
            s.owner.setXRot(0); s.begin();
            s.spell.onServerCastComplete(h.getLevel(), 1, s.owner, s.data, true);
            h.assertTrue(launcher.getLockOnCount() == 1 && !launcher.isReleased()
                            && s.data.getPlayerRecasts().hasRecastForSpell(s.spell),
                    "Cancelling one recast must preserve completed locks");
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void timeoutCancelsOnlyUnfinishedRecast(GameTestHelper h) {
        try (var s = new Scene(h)) {
            s.zombie(); s.begin(); s.finish(); s.begin(); s.finish();
            var launcher = s.launcher();
            s.begin();
            float mana = s.data.getMana();
            for (int i = 0; i < 119; i++) s.data.getPlayerRecasts().tick(1);
            h.assertTrue(s.data.isCasting() && !launcher.isReleased(), "Recast must remain active through tick 119");
            s.data.getPlayerRecasts().tick(1);
            h.assertTrue(!s.data.isCasting() && launcher.isReleased() && launcher.getLockOnCount() == 1,
                    "Timeout must cancel unfinished recast and launch only completed locks");
            s.spell.castSpell(h.getLevel(), 1, s.owner, CastSource.SPELLBOOK, true);
            h.assertTrue(s.data.getMana() == mana && s.launchers().size() == 1,
                    "Stale completion must not charge mana or summon another launcher");
        }
        try (var s = new Scene(h)) {
            s.zombie(); s.begin(); s.finish();
            var other = SpellRegistry.LOCK_ON_RAY.get();
            s.data.initiateCast(other, 1, 100, CastSource.SPELLBOOK, "mainhand");
            for (int i = 0; i < 120; i++) s.data.getPlayerRecasts().tick(1);
            h.assertTrue(s.data.isCasting() && s.data.getCastingSpellId().equals(other.getSpellId()),
                    "Timeout must preserve unrelated casting");
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void abnormalSessionEndNeverFires(GameTestHelper h) {
        for (var result : RecastResult.values()) {
            if (result == RecastResult.TIMEOUT || result == RecastResult.USED_ALL_RECASTS) continue;
            try (var s = new Scene(h)) {
                s.zombie(); s.begin(); s.finish();
                var launcher = s.launcher();
                s.data.getPlayerRecasts().removeRecast(s.data.getPlayerRecasts().getRecastInstance(s.spell.getSpellId()), result);
                h.assertTrue(launcher.isRemoved() && s.projectiles().isEmpty(), "Abnormal end must discard without firing: " + result);
            }
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void invalidTargetsAndSessionSerialization(GameTestHelper h) {
        try (var s = new Scene(h)) {
            var target = s.zombie(); s.begin(); s.finish(); s.begin();
            // 詠唱開始後に視線が外れても、捕捉済みの対象を保持する。
            s.owner.setXRot(-90); s.finish();
            var launcher = s.launcher();
            h.assertTrue(launcher.getLockOnCount() == 1, "Aim changes must not replace the pending lock");
            var session = s.data.getPlayerRecasts().getRecastInstance(s.spell.getSpellId()).getCastData();
            var copy = (SummonWeaponRecastSpellData) s.spell.getEmptyCastData();
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            try { session.writeToBuffer(buffer); copy.readFromBuffer(buffer); }
            finally { buffer.release(); }
            h.assertTrue(copy.getEntity(h.getLevel()) == launcher, "Network roundtrip must preserve launcher identity");
            copy.reset(); copy.deserializeNBT(h.getLevel().registryAccess(), session.serializeNBT(h.getLevel().registryAccess()));
            h.assertTrue(copy.getEntity(h.getLevel()) == launcher, "NBT roundtrip must preserve launcher identity");
            target.discard();
            s.data.getPlayerRecasts().removeRecast(s.data.getPlayerRecasts().getRecastInstance(s.spell.getSpellId()), RecastResult.TIMEOUT);
            h.assertTrue(launcher.isRemoved() && s.projectiles().isEmpty(), "Lost targets must not be reassigned or fired upon");
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void emptyInitialSummonExpiresWithoutFiring(GameTestHelper h) {
        try (var s = new Scene(h)) {
            s.owner.setXRot(-90);
            s.begin(); s.finish();
            var launcher = s.launcher();
            h.assertTrue(launcher.getLockOnCount() == 0 && !launcher.isReleased(),
                    "Initial summon must require no target and add no lock");
            for (int i = 0; i < 119; i++) s.data.getPlayerRecasts().tick(1);
            h.assertTrue(!launcher.isRemoved(), "Empty launcher must wait the full six second window");
            s.data.getPlayerRecasts().tick(1);
            h.assertTrue(launcher.isRemoved() && s.projectiles().isEmpty(), "Empty timeout must discard without firing");
        }
        h.succeed();
    }

    private static final class Scene implements AutoCloseable {
        final GameTestHelper helper;
        final Vec3 origin;
        final FakePlayer owner;
        final FlySwatter spell = (FlySwatter) SpellRegistry.FLY_SWATTER.get();
        final MagicData data;
        final List<Entity> entities = new ArrayList<>();

        Scene(GameTestHelper helper) {
            this.helper = helper;
            origin = helper.absoluteVec(new Vec3(2, 30, 2));
            owner = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "fly_swatter_test"));
            owner.setPos(origin); owner.setYRot(-90); owner.setXRot(0); owner.setNoGravity(true);
            helper.getLevel().addFreshEntity(owner); entities.add(owner);
            data = MagicData.getPlayerMagicData(owner);
            // FakePlayerには通常ログイン時の同期初期化がない。
            data.setSyncedData(new SyncedSpellData(owner)); data.setMana(1000);
        }

        Zombie zombie() {
            var target = EntityType.ZOMBIE.create(helper.getLevel());
            target.setPos(origin.add(8, 0, 0)); target.setNoAi(true); target.setNoGravity(true);
            helper.getLevel().addFreshEntity(target); entities.add(target); return target;
        }
        void begin() {
            helper.assertTrue(spell.checkPreCastConditions(helper.getLevel(), 1, owner, data), "Scene target must be selectable");
            data.initiateCast(spell, 1, spell.getEffectiveCastTime(1, owner), CastSource.SPELLBOOK, "mainhand");
            spell.onServerPreCast(helper.getLevel(), 1, owner, data);
        }
        void finish() {
            spell.castSpell(helper.getLevel(), 1, owner, CastSource.SPELLBOOK, true);
            spell.onServerCastComplete(helper.getLevel(), 1, owner, data, false);
        }
        List<FlySwatterLauncherEntity> launchers() {
            return helper.getLevel().getEntitiesOfClass(FlySwatterLauncherEntity.class, owner.getBoundingBox().inflate(150))
                    .stream().filter(e -> e.getOwner() == owner).toList();
        }
        FlySwatterLauncherEntity launcher() { return launchers().getFirst(); }
        List<FlySwatterProjectileEntity> projectiles() {
            return helper.getLevel().getEntitiesOfClass(FlySwatterProjectileEntity.class, owner.getBoundingBox().inflate(150))
                    .stream().filter(e -> e.getOwner() == owner).toList();
        }
        void fireTicks(FlySwatterLauncherEntity launcher, int ticks) {
            for (int i = 0; i < ticks; i++) launcher.tickOnServer(helper.getLevel());
        }
        @Override public void close() {
            data.getPlayerRecasts().removeAll(RecastResult.COMMAND);
            data.resetCastingState(); data.resetAdditionalCastData();
            projectiles().forEach(Entity::discard); launchers().forEach(Entity::discard); entities.forEach(Entity::discard);
        }
    }
}
