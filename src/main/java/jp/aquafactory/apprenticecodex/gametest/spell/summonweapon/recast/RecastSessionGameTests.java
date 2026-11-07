package jp.aquafactory.apprenticecodex.gametest.spell.summonweapon.recast;

import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.remoteownercast.*;
import jp.aquafactory.apprenticecodex.spell.AbstractSummonWeaponRecastSpell;
import jp.aquafactory.apprenticecodex.spell.AbstractSummonWeaponRecastSpell.SummonWeaponRecastSpellData;
import jp.aquafactory.apprenticecodex.spell.commencefire.CommenceFireRifleEntity;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Optional;

import static jp.aquafactory.apprenticecodex.gametest.spell.summonweapon.recast.RecastWeaponFixture.BATCH;
import static jp.aquafactory.apprenticecodex.gametest.spell.summonweapon.recast.RecastWeaponFixture.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class RecastSessionGameTests {
    private RecastSessionGameTests() {
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void commenceTimeoutRejectsStaleCompletion(GameTestHelper h) {
        try (var s = new RecastWeaponFixture(h, (AbstractSummonWeaponRecastSpell<?>) SpellRegistry.COMMENCE_FIRE.get())) {
            s.begin();
            s.finish();
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
            try (var s = new RecastWeaponFixture(h, (AbstractSummonWeaponRecastSpell<?>) SpellRegistry.COMMENCE_FIRE.get())) {
                s.begin();
                s.finish();
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
        try (var s = new RecastWeaponFixture(h, (AbstractSummonWeaponRecastSpell<?>) SpellRegistry.COMMENCE_FIRE.get())) {
            s.begin();
            s.finish();
            var recast = s.data.getPlayerRecasts().getRecastInstance(s.spell.getSpellId());
            int count = recast.getRemainingRecasts();
            s.begin();
            s.spell.onServerCastComplete(h.getLevel(), 1, s.owner, s.data, true);
            h.assertTrue(!s.weapon().isRemoved() && recast.getRemainingRecasts() == count,
                    "Cancelling a rifle cast must retain its session and shots");
            s.begin();
            s.finish();
            h.assertTrue(s.target.getHealth() < 200, "A later valid recast must still attack");
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void remoteInitialSummonPreservesOwnerCasting(GameTestHelper h) {
        try (var s = new RecastWeaponFixture(h, (AbstractSummonWeaponRecastSpell<?>) SpellRegistry.COMMENCE_FIRE.get())) {
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
        try (var s = new RecastWeaponFixture(h, (AbstractSummonWeaponRecastSpell<?>) SpellRegistry.COMMENCE_FIRE.get())) {
            s.begin();
            s.finish();
            var weapon = s.weapon();
            var tag = new CompoundTag();
            tag.putUUID("Entity", weapon.getUUID());
            var session = new SummonWeaponRecastSpellData();
            session.deserializeNBT(h.getLevel().registryAccess(), tag);
            h.assertTrue(session.getEntity(h.getLevel()) == weapon, "Legacy UUID-only NBT must still resolve loaded weapons");
            tag.putString("Dimension", "minecraft:the_nether");
            session.deserializeNBT(h.getLevel().registryAccess(), tag);
            h.assertTrue(session.getEntity(h.getLevel()) == null, "A different dimension must reject the weapon");
            tag.remove("Dimension");
            session.deserializeNBT(h.getLevel().registryAccess(), tag);
            weapon.discard();
            h.assertTrue(session.getEntity(h.getLevel()) == null, "Removed weapons must not remain usable");
        }
        h.succeed();
    }
}
