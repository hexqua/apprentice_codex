package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastType;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.RestrictedSpellImbuableItem;
import jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmulet;
import jp.aquafactory.apprenticecodex.item.shield.ParrycastBuckler;
import jp.aquafactory.apprenticecodex.item.shield.ReflectcastShield;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.utility.SpellRecastCompatibility;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellRecastCompatibilityGameTests {
    @GameTest(template = "gametest/basic_floor")
    public static void imbueScanRejectsUnknownRecastsAndContinues(GameTestHelper helper) {
        var spell = new CasterDependentRecastSpell();
        // JEIと金床が共用する判定を通し、外部魔法の例外で後続レベルの走査まで失わないことを確認する。
        for (var item : new Item[]{ItemRegistry.IRON_SPELLCASTER_GUN.get(),
                ItemRegistry.COPPER_SWINGCAST_STAFF.get(), ItemRegistry.IRON_SWINGCAST_STAFF.get(),
                ItemRegistry.PARRYCAST_BUCKLER.get(), ItemRegistry.REFLECTCAST_SHIELD.get()}) {
            var target = (RestrictedSpellImbuableItem) item;
            helper.assertTrue(!target.canImbueSpell(spell, 1),
                    "Unknown recast count must reject imbuement for " + item);
            helper.assertTrue(target.canImbueSpell(spell, 2),
                    "A failed level must not hide a later valid level for " + item);
            helper.assertTrue(!target.canImbueSpell(spell, 3),
                    "Known recasts must still reject imbuement for " + item);
        }
        var amulet = (AutocastAmulet) ItemRegistry.AUTOCAST_AMULET.get();
        for (int level : new int[]{1, 2, 3}) {
            helper.assertTrue(amulet.canImbueSpell(spell, level) == (level == 2),
                    "Amulet imbuement must reject unknown and positive recast counts while keeping valid levels");
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void unrestrictedEquipmentDoesNotQueryRecasts(GameTestHelper helper) {
        var spell = new CasterDependentRecastSpell();
        for (var item : new Item[]{ItemRegistry.DIAMOND_SPELLCASTER_GUN.get(), ItemRegistry.DIAMOND_SWINGCAST_STAFF.get()}) {
            var target = (RestrictedSpellImbuableItem) item;
            helper.assertTrue(target.canImbueSpell(spell, 1),
                    "Equipment without a recast restriction must accept caster-dependent spells for " + item);
            helper.assertTrue(target.canImbueSpell(spell, 3),
                    "Equipment without a recast restriction must keep accepting recast spells for " + item);
        }
        helper.assertTrue(spell.queryCount == 0, "Unrestricted equipment must not query recast counts");
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void runtimeChecksRejectUnknownRecastsWithoutCachingResults(GameTestHelper helper) {
        var spell = new CasterDependentRecastSpell();
        var buckler = (ParrycastBuckler) ItemRegistry.PARRYCAST_BUCKLER.get();
        var shield = (ReflectcastShield) ItemRegistry.REFLECTCAST_SHIELD.get();
        var amulet = (AutocastAmulet) ItemRegistry.AUTOCAST_AMULET.get();
        for (int level : new int[]{1, 2, 3}) {
            boolean expected = level == 2;
            helper.assertTrue(buckler.canUseConfiguredSpell(new ItemStack(buckler), spell, level) == expected,
                    "Buckler runtime check must use the same recast restriction as imbuement");
            helper.assertTrue(shield.canUseConfiguredSpell(new ItemStack(shield), spell, level) == expected,
                    "Shield runtime check must use the same recast restriction as imbuement");
            helper.assertTrue(amulet.canAutoCastSpell(new ItemStack(amulet), spell, level) == expected,
                    "Amulet runtime check must use the same recast restriction as imbuement");
        }
        helper.assertTrue(!shield.supportsManaBypass(spell),
                "Unknown recast count must not enable shield mana bypass");
        helper.assertTrue(!SpellRecastCompatibility.hasNoRecastWithoutCaster(spell, 1),
                "Caster-dependent recast count must initially be rejected");
        spell.requiresCaster = false;
        helper.assertTrue(SpellRecastCompatibility.hasNoRecastWithoutCaster(spell, 1),
                "A previous failure must not permanently cache a rejection after spell settings change");
        helper.succeed();
    }

    private static final class CasterDependentRecastSpell extends AbstractSpell {
        private final DefaultConfig config = new DefaultConfig();
        private boolean requiresCaster = true;
        private int queryCount;

        @Override
        public ResourceLocation getSpellResource() {
            return ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "test_caster_dependent_recast");
        }

        @Override
        public DefaultConfig getDefaultConfig() {
            return config;
        }

        @Override
        public CastType getCastType() {
            return CastType.INSTANT;
        }

        @Override
        public int getSpellCooldown() {
            return 1;
        }

        @Override
        public int getRecastCount(int spellLevel, LivingEntity caster) {
            queryCount++;
            // Cataclysm Spellbooksと同じく装備参照でnull例外を起こし、レベル依存も再現する。
            if (spellLevel == 1 && requiresCaster) {
                caster.getItemBySlot(EquipmentSlot.CHEST);
            }
            return spellLevel == 3 ? 1 : 0;
        }
    }
}
