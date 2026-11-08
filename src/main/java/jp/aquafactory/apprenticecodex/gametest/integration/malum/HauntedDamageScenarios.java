package jp.aquafactory.apprenticecodex.gametest.integration.malum;

import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.datagen.DamageTypeTagGenerator;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.neoforged.fml.ModList;

import static jp.aquafactory.apprenticecodex.gametest.integration.malum.MalumTestSupport.LODESTONE_MAGIC_PROFICIENCY;
import static jp.aquafactory.apprenticecodex.gametest.integration.malum.MalumTestSupport.LODESTONE_MOD_ID;

final class HauntedDamageScenarios {
    private HauntedDamageScenarios() {}

    static final TagKey<DamageType> COMMON_IS_MAGIC = TagKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath("c", "is_magic")
    );

    static void hauntedBonusDamageTypeStaysOnMagicDamageTagPath(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var attacker = helper.spawn(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
            var source = CombatTools.getDamageSource(attacker.level(), attacker, attacker, DamageTypes.HAUNTED_BONUS);

            helper.assertTrue(source.is(DamageTypes.HAUNTED_BONUS),
                    "Haunted bonus should use apprenticecodex:haunted_bonus");
            helper.assertTrue(source.is(DamageTypeTagGenerator.MAGIC_DAMAGE),
                    "Haunted bonus should stay on the magic damage tag path");
            helper.assertTrue(source.is(COMMON_IS_MAGIC),
                    "Haunted bonus should stay on the c:is_magic path for Lodestone magic_proficiency");
            helper.assertTrue(source.is(DamageTypeTagGenerator.BYPASSES_IFRAME),
                    "Haunted bonus should bypass cooldown-based I-Frame checks");
        });
    }

    static void hauntedBonusDamageActuallyScalesWithLodestoneMagicProficiency(GameTestHelper helper) {
        helper.succeedIf(() -> {
            if (!ModList.get().isLoaded(LODESTONE_MOD_ID)) {
                return;
            }

            var magicProficiency = BuiltInRegistries.ATTRIBUTE.getOptional(LODESTONE_MAGIC_PROFICIENCY).orElse(null);
            helper.assertTrue(magicProficiency != null, "lodestone:magic_proficiency is not registered");

            var attacker = helper.spawn(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
            var proficiencyInstance = attacker.getAttribute(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(magicProficiency));
            helper.assertTrue(proficiencyInstance != null, "Attacker is missing lodestone:magic_proficiency");

            var baselineTarget = helper.spawn(EntityType.SHEEP, new BlockPos(1, 2, 0));
            var amplifiedTarget = helper.spawn(EntityType.SHEEP, new BlockPos(2, 2, 0));
            var baseDamage = 4.0F;

            var baselineHealth = baselineTarget.getHealth();
            helper.assertTrue(baselineTarget.hurt(
                            CombatTools.getDamageSource(attacker.level(), attacker, attacker, DamageTypes.HAUNTED_BONUS),
                            baseDamage),
                    "Baseline haunted bonus damage should apply");
            var baselineTaken = baselineHealth - baselineTarget.getHealth();
            helper.assertTrue(Math.abs(baselineTaken - baseDamage) < 1.0e-4F,
                    "Baseline haunted bonus damage should stay unscaled at proficiency 1.0, actual=" + baselineTaken);

            proficiencyInstance.setBaseValue(1.5D);
            var amplifiedHealth = amplifiedTarget.getHealth();
            helper.assertTrue(amplifiedTarget.hurt(
                            CombatTools.getDamageSource(attacker.level(), attacker, attacker, DamageTypes.HAUNTED_BONUS),
                            baseDamage),
                    "Amplified haunted bonus damage should apply");
            var amplifiedTaken = amplifiedHealth - amplifiedTarget.getHealth();
            helper.assertTrue(Math.abs(amplifiedTaken - 6.0F) < 1.0e-4F,
                    "Amplified haunted bonus damage should scale to 6.0 at proficiency 1.5, actual=" + amplifiedTaken);
            helper.assertTrue(amplifiedTaken > baselineTaken,
                    "Amplified haunted bonus damage should exceed baseline damage");
        });
    }
}
