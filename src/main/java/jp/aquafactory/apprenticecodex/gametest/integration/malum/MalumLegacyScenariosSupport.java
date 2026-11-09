package jp.aquafactory.apprenticecodex.gametest.integration.malum;

import jp.aquafactory.apprenticecodex.compat.malum.MalumHauntedCompat;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.datagen.DamageTypeTagGenerator;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.BowGameTestSupport.MALUM_MOD_ID;
import static jp.aquafactory.apprenticecodex.gametest.integration.malum.MalumTestSupport.LODESTONE_MAGIC_PROFICIENCY;
import static jp.aquafactory.apprenticecodex.gametest.integration.malum.MalumTestSupport.LODESTONE_MOD_ID;

final class MalumLegacyScenariosSupport {
    private MalumLegacyScenariosSupport() {}

    static void malumHauntedBonusResolvesFromSupportedMainhandWeapons(GameTestHelper helper) {
        helper.succeedIf(() -> {
            if (!ModList.get().isLoaded(MALUM_MOD_ID)) {
                return;
            }

            var haunted = MalumHauntedCompat.getHauntedEnchantment();
            helper.assertTrue(haunted != null, "malum:haunted is not registered");

            var pastelStaff = new ItemStack(ItemRegistry.PASTEL_STAFF.get());
            pastelStaff.enchant(haunted, 1);
            helper.assertTrue(MalumHauntedCompat.isSupportedHauntedMainhandItem(pastelStaff),
                    "Pastel Staff should be a supported Haunted main hand item");
            helper.assertTrue(MalumHauntedCompat.resolveHauntedMagicDamageBonus(pastelStaff) > 0.0D,
                    "Pastel Staff should resolve a positive Haunted magic damage bonus");

            var multicastEchoStaff = new ItemStack(ItemRegistry.MULTICAST_ECHO_STAFF.get());
            multicastEchoStaff.enchant(haunted, 1);
            helper.assertTrue(MalumHauntedCompat.isSupportedHauntedMainhandItem(multicastEchoStaff),
                    "Multicast Echo Staff should be a supported Haunted main hand item");
            helper.assertTrue(MalumHauntedCompat.resolveHauntedMagicDamageBonus(multicastEchoStaff) > 0.0D,
                    "Multicast Echo Staff should resolve a positive Haunted magic damage bonus");

            var crystalBladedStaff = new ItemStack(ItemRegistry.CRYSTAL_BLADED_STAFF.get());
            crystalBladedStaff.enchant(haunted, 1);
            helper.assertTrue(MalumHauntedCompat.isSupportedHauntedMainhandItem(crystalBladedStaff),
                    "Crystal Bladed Staff should be a supported Haunted main hand item");
            helper.assertTrue(MalumHauntedCompat.resolveHauntedMagicDamageBonus(crystalBladedStaff) > 0.0D,
                    "Crystal Bladed Staff should resolve a positive Haunted magic damage bonus");

            var focusStaffbow = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
            focusStaffbow.enchant(haunted, 1);
            helper.assertTrue(MalumHauntedCompat.isSupportedHauntedMainhandItem(focusStaffbow),
                    "Focus Staffbow should be a supported Haunted main hand item");
            helper.assertTrue(MalumHauntedCompat.resolveHauntedMagicDamageBonus(focusStaffbow) > 0.0D,
                    "Focus Staffbow should resolve a positive Haunted magic damage bonus");

            var chargedTwinBladeStaff = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
            chargedTwinBladeStaff.enchant(haunted, 1);
            helper.assertTrue(MalumHauntedCompat.isSupportedHauntedMainhandItem(chargedTwinBladeStaff),
                    "Charged Twin Blade Staff should be a supported Haunted main hand item");
            helper.assertTrue(MalumHauntedCompat.resolveHauntedMagicDamageBonus(chargedTwinBladeStaff) > 0.0D,
                    "Charged Twin Blade Staff should resolve a positive Haunted magic damage bonus");

            var manaForceBlade = new ItemStack(ItemRegistry.MANA_FORCE_BLADE.get());
            manaForceBlade.enchant(haunted, 1);
            helper.assertTrue(MalumHauntedCompat.isSupportedHauntedMainhandItem(manaForceBlade),
                    "Mana Force Blade should be a supported Haunted main hand item");
            helper.assertTrue(MalumHauntedCompat.resolveHauntedMagicDamageBonus(manaForceBlade) > 0.0D,
                    "Mana Force Blade should resolve a positive Haunted magic damage bonus");

            helper.assertFalse(MalumHauntedCompat.isSupportedHauntedMainhandItem(new ItemStack(ItemRegistry.IRON_SPELLCASTER_GUN.get())),
                    "Spellgun should stay outside Haunted support");
            helper.assertFalse(MalumHauntedCompat.isSupportedHauntedMainhandItem(new ItemStack(ItemRegistry.REFLECTCAST_SHIELD.get())),
                    "Reflectcast Shield should stay outside Haunted support");
        });
    }

    static void malumHauntedBonusUsesDedicatedDamageType(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var attacker = helper.spawn(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
            var source = MalumHauntedCompat.createHauntedBonusDamageSource(attacker);
            helper.assertTrue(source.is(DamageTypes.HAUNTED_BONUS),
                    "Haunted bonus should use apprenticecodex:haunted_bonus");
            helper.assertTrue(source.is(DamageTypeTagGenerator.MAGIC_DAMAGE),
                    "Haunted bonus should stay on the magic damage tag path");
            helper.assertTrue(source.is(DamageTypeTagGenerator.FORGE_IS_MAGIC),
                    "Haunted bonus should stay on the forge:is_magic path for Lodestone magic_proficiency");
            helper.assertTrue(source.is(DamageTypeTagGenerator.BYPASSES_IFRAME),
                    "Haunted bonus should bypass cooldown-based I-Frame checks");
        });
    }

    static void magicDamageTagActuallyScalesWithLodestoneMagicProficiency(GameTestHelper helper) {
        helper.succeedIf(() -> {
            if (!ModList.get().isLoaded(LODESTONE_MOD_ID)) {
                return;
            }

            var magicProficiency = ForgeRegistries.ATTRIBUTES.getValue(LODESTONE_MAGIC_PROFICIENCY);
            helper.assertTrue(magicProficiency != null, "lodestone:magic_proficiency is not registered");

            var attacker = helper.spawn(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
            var proficiencyInstance = attacker.getAttribute(magicProficiency);
            helper.assertTrue(proficiencyInstance != null, "Attacker is missing lodestone:magic_proficiency");

            var baselineTarget = helper.spawn(EntityType.SHEEP, new BlockPos(1, 2, 0));
            var amplifiedTarget = helper.spawn(EntityType.SHEEP, new BlockPos(2, 2, 0));
            var baseDamage = 4.0F;

            var baselineHealth = baselineTarget.getHealth();
            helper.assertTrue(baselineTarget.hurt(MalumHauntedCompat.createHauntedBonusDamageSource(attacker), baseDamage),
                    "Baseline haunted bonus damage should apply");
            var baselineTaken = baselineHealth - baselineTarget.getHealth();
            helper.assertTrue(Math.abs(baselineTaken - baseDamage) < 1.0e-4F,
                    "Baseline haunted bonus damage should stay unscaled at proficiency 1.0, actual=" + baselineTaken);

            proficiencyInstance.setBaseValue(1.5D);
            var amplifiedHealth = amplifiedTarget.getHealth();
            helper.assertTrue(amplifiedTarget.hurt(MalumHauntedCompat.createHauntedBonusDamageSource(attacker), baseDamage),
                    "Amplified haunted bonus damage should apply");
            var amplifiedTaken = amplifiedHealth - amplifiedTarget.getHealth();
            helper.assertTrue(Math.abs(amplifiedTaken - 6.0F) < 1.0e-4F,
                    "Amplified haunted bonus damage should scale to 6.0 at proficiency 1.5, actual=" + amplifiedTaken);
            helper.assertTrue(amplifiedTaken > baselineTaken,
                    "Amplified haunted bonus damage should exceed baseline damage");
        });
    }
}
