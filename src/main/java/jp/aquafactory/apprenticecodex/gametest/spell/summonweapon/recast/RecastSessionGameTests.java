package jp.aquafactory.apprenticecodex.gametest.spell.summonweapon.recast;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.entity.SummonWeaponEntity;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.remoteownercast.*;
import jp.aquafactory.apprenticecodex.spell.AbstractSummonWeaponRecastSpell;
import jp.aquafactory.apprenticecodex.spell.AbstractSummonWeaponRecastSpell.SummonWeaponRecastSpellData;
import jp.aquafactory.apprenticecodex.spell.commencefire.CommenceFireRifleEntity;
import jp.aquafactory.apprenticecodex.utility.RaycastTools;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class RecastSessionGameTests {
    private RecastSessionGameTests() {}

    static final String TEMPLATE = "gametest/basic_floor";

    static final String BATCH = "apprenticecodex.summon_weapon_recast";

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void commenceTimeoutRejectsStaleCompletion(GameTestHelper h) {
        try (var s = new Scene(h, (AbstractSummonWeaponRecastSpell<?>) SpellRegistry.COMMENCE_FIRE.get())) {
            s.begin(); s.finish();
            var weapon = (CommenceFireRifleEntity) s.weapon();
            s.begin();
            float mana = s.data.getMana();
            for (int i = 0; i < s.spell.getDurationTick(); i++) s.data.getPlayerRecasts().tick(1);
            h.assertTrue(!s.data.isCasting() && weapon.isRemoved(), "Timeout must cancel the unfinished rifle recast");
            s.spell.castSpell(h.getLevel(), 1, s.owner, CastSource.SPELLBOOK, true);
            h.assertTrue(s.weapons().isEmpty() && s.data.getMana() == mana && s.target.getHealth() == 200,
                    "Stale rifle completion must not resummon, charge mana, or deal damage");
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void missingWeaponRejectsBothCastStages(GameTestHelper h) {
        for (boolean duringCast : List.of(false, true)) {
            try (var s = new Scene(h, (AbstractSummonWeaponRecastSpell<?>) SpellRegistry.COMMENCE_FIRE.get())) {
                s.begin(); s.finish();
                var weapon = s.weapon();
                if (duringCast) s.begin();
                float mana = s.data.getMana();
                weapon.discard();
                if (duringCast) s.spell.castSpell(h.getLevel(), 1, s.owner, CastSource.SPELLBOOK, true);
                else h.assertFalse(s.spell.checkPreCastConditions(h.getLevel(), 1, s.owner, s.data),
                        "Missing weapon precondition must reject the attempt");
                h.assertTrue(!s.data.isCasting() && !s.data.getPlayerRecasts().hasRecastForSpell(s.spell)
                                && s.data.getMana() == mana && s.weapons().isEmpty() && s.target.getHealth() == 200,
                        "Missing weapon must end the session without an attack or new summon");
                h.assertTrue(s.data.getPlayerCooldowns().isOnCooldown(s.spell), "Missing weapon end must still add cooldown");
            }
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void cancelledRifleCastPreservesSession(GameTestHelper h) {
        try (var s = new Scene(h, (AbstractSummonWeaponRecastSpell<?>) SpellRegistry.COMMENCE_FIRE.get())) {
            s.begin(); s.finish();
            var recast = s.data.getPlayerRecasts().getRecastInstance(s.spell.getSpellId());
            int count = recast.getRemainingRecasts();
            s.begin();
            s.spell.onServerCastComplete(h.getLevel(), 1, s.owner, s.data, true);
            h.assertTrue(!s.weapon().isRemoved() && recast.getRemainingRecasts() == count,
                    "Cancelling a rifle cast must retain its session and shots");
            s.begin(); s.finish();
            h.assertTrue(s.target.getHealth() < 200, "A later valid recast must still attack");
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void remoteInitialSummonPreservesOwnerCasting(GameTestHelper h) {
        try (var s = new Scene(h, (AbstractSummonWeaponRecastSpell<?>) SpellRegistry.COMMENCE_FIRE.get())) {
            var other = SpellRegistry.LOCK_ON_RAY.get();
            s.data.initiateCast(other, 1, 100, CastSource.SPELLBOOK, "mainhand");
            var profile = new RemoteOwnerCastProfile(RemoteOwnerCastMode.PLAYER_SELF,
                    RemoteOwnerOriginMode.PLAYER_SELF, RemoteOwnerDirectionMode.PLAYER_LOOK, Optional.empty(), true);
            var result = RemoteOwnerCastRunner.tryCast(h.getLevel(), s.owner, ItemStack.EMPTY, new SpellData(s.spell, 1),
                    profile, RemoteOwnerCastOrigin.SATELLITE_FOLLOWCAST, s.owner.getEyePosition(),
                    s.owner.getLookAngle(), CastSource.SWORD, "mainhand", false);
            h.assertTrue(result.handled() && result.succeeded(), "Allowed remote initial summon must succeed");
            h.assertTrue(s.data.isCasting() && s.data.getCastingSpellId().equals(other.getSpellId()),
                    "Isolated remote summon must preserve the owner's unrelated cast");
            h.assertTrue(s.weapons().size() == 1 && s.data.getPlayerRecasts().hasRecastForSpell(s.spell),
                    "Remote summon must transfer its recast to the owner");
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void legacyWeaponDataAndDimensionValidation(GameTestHelper h) {
        try (var s = new Scene(h, (AbstractSummonWeaponRecastSpell<?>) SpellRegistry.COMMENCE_FIRE.get())) {
            s.begin(); s.finish();
            var weapon = s.weapon();
            var tag = new CompoundTag();
            tag.putUUID("Entity", weapon.getUUID());
            var session = new SummonWeaponRecastSpellData();
            session.deserializeNBT(tag);
            h.assertTrue(session.getEntity(h.getLevel()) == weapon, "Legacy UUID-only NBT must still resolve loaded weapons");
            tag.putString("Dimension", "minecraft:the_nether");
            session.deserializeNBT(tag);
            h.assertTrue(session.getEntity(h.getLevel()) == null, "A different dimension must reject the weapon");
            tag.remove("Dimension"); session.deserializeNBT(tag);
            weapon.discard();
            h.assertTrue(session.getEntity(h.getLevel()) == null, "Removed weapons must not remain usable");
        }
        h.succeed();
    }

    static final class Scene implements AutoCloseable {
        final GameTestHelper helper;
        final AbstractSummonWeaponRecastSpell<?> spell;
        final FakePlayer owner;
        final MagicData data;
        final Zombie target;

        Scene(GameTestHelper helper, AbstractSummonWeaponRecastSpell<?> spell) {
            this.helper = helper; this.spell = spell;
            // 1.20.1のGameTest原点は地下のため、射線と爆風が地形に遮られない上空へ置く。
            var origin = helper.absoluteVec(new Vec3(2, 260, 2));
            owner = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "summon_recast_test"));
            owner.setPos(origin); owner.setYRot(-90); owner.setXRot(0); owner.setNoGravity(true);
            // CommenceFireはgetViewVectorで頭の向きを参照する。FakePlayerには通常の更新tickがない。
            owner.setYHeadRot(-90); owner.yHeadRotO = -90;
            owner.getAttribute(AttributeRegistry.MAX_MANA.get()).setBaseValue(1000);
            helper.getLevel().addFreshEntity(owner);
            data = MagicData.getPlayerMagicData(owner);
            // FakePlayerは通常ログインを通らないため同期状態を初期化する。
            data.setSyncedData(new SyncedSpellData(owner)); data.setMana(1000);
            target = EntityType.ZOMBIE.create(helper.getLevel());
            target.setPos(origin.add(8, 0, 0)); target.setNoAi(true); target.setNoGravity(true);
            target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200); target.setHealth(200);
            target.getAttribute(Attributes.ARMOR).setBaseValue(0);
            helper.getLevel().addFreshEntity(target);
            var aim = RaycastTools.raycastFromEye(owner, 64, 0.5,
                    entity -> entity == target);
            helper.assertTrue(aim.hitEntity() == target,
                    "Scene must expose its target: eye=" + owner.getEyePosition(1) + ", look=" + owner.getViewVector(1)
                            + ", target=" + target.getBoundingBox() + ", hit=" + aim);
        }

        void assertRejectedRecastPreservesState() {
            var recast = data.getPlayerRecasts().getRecastInstance(spell.getSpellId());
            int count = recast.getRemainingRecasts();
            int remainingTicks = recast.getTicksRemaining();
            float mana = data.getMana();
            float health = target.getHealth();
            helper.assertFalse(spell.checkPreCastConditions(helper.getLevel(), 1, owner, data),
                    "Quick Arms must reject a recast while preparing or recoiling");
            helper.assertTrue(recast.getRemainingRecasts() == count && recast.getTicksRemaining() == remainingTicks
                            && data.getMana() == mana && target.getHealth() == health && !data.isCasting(),
                    "A rejected recast must preserve shots, timeout, mana, health, and casting state");
        }

        void begin() {
            helper.assertTrue(spell.checkPreCastConditions(helper.getLevel(), 1, owner, data), "Scene cast must pass preconditions");
            data.initiateCast(spell, 1, spell.getEffectiveCastTime(1, owner), CastSource.SPELLBOOK, "mainhand");
            spell.onServerPreCast(helper.getLevel(), 1, owner, data);
        }
        void finish() {
            target.invulnerableTime = 0;
            spell.castSpell(helper.getLevel(), 1, owner, CastSource.SPELLBOOK, true);
            spell.onServerCastComplete(helper.getLevel(), 1, owner, data, false);
        }
        List<SummonWeaponEntity> weapons() {
            return helper.getLevel().getEntitiesOfClass(SummonWeaponEntity.class, owner.getBoundingBox().inflate(150))
                    .stream().filter(e -> e.getOwner() == owner).toList();
        }
        SummonWeaponEntity weapon() { return weapons().get(0); }
        void weaponTicks(int ticks) {
            var weapon = weapon();
            for (int i = 0; i < ticks; i++) weapon.tickOnServer(helper.getLevel());
        }
        @Override public void close() {
            data.getPlayerRecasts().removeAll(RecastResult.COMMAND);
            data.resetCastingState(); weapons().forEach(Entity::discard); target.discard(); owner.discard();
        }
    }
}
