package jp.aquafactory.apprenticecodex.gametest;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.malum.MalumSpellReaperScytheBridge;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.item.spellreaperscythe.ScytheThrowDamage;
import jp.aquafactory.apprenticecodex.item.spellreaperscythe.ScytheThrowEntity;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ScytheProficiencyGameTests {
    @GameTest(template = "gametest/basic_floor")
    public static void proficiencyScalesOnlyPhysicalAndExcludesGeasTags(GameTestHelper h) {
        var p = ScytheThrowGameTests.player(h, 100);
        var target = h.spawn(EntityType.HUSK, new BlockPos(2, 2, 4));
        target.setNoAi(true);
        target.getAttribute(Attributes.ARMOR).setBaseValue(0);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
        var projectile = new ScytheThrowEntity(EntityRegistry.SCYTHE_THROW.get(), h.getLevel());
        float proficiency = 1;
        if (MalumSpellReaperScytheBridge.isAvailable()) {
            var attribute = h.getLevel().registryAccess().registryOrThrow(Registries.ATTRIBUTE)
                    .getHolder(ResourceLocation.fromNamespaceAndPath("malum", "scythe_proficiency")).orElseThrow();
            p.getAttribute(attribute).setBaseValue(2);
            proficiency = 2;
        }
        for (boolean continuous : new boolean[]{false, true}) {
            // Cullingの体力条件も満たすが、属性だけを適用する独自投擲に処刑補正を混ぜない。
            target.setHealth(90); target.invulnerableTime = 0;
            ScytheThrowDamage.hit(h.getLevel(), projectile, p, target, p.getMainHandItem(), 10, 2, continuous);
            float expected = (10 * proficiency + 2) * (continuous ? 0.1f : 1);
            h.assertTrue(Math.abs(90 - target.getHealth() - expected) < 0.01,
                    "Proficiency must scale only physical damage once: actual=" + (90 - target.getHealth()) + ", expected=" + expected);
        }
        var registry = h.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        for (var type : java.util.List.of(DamageTypes.SPELL_REAPER_SCYTHE_THROW, DamageTypes.SPELL_REAPER_SCYTHE_THROW_CONTINUOUS,
                DamageTypes.SPELL_REAPER_SCYTHE_THROW_MAGIC, DamageTypes.SPELL_REAPER_SCYTHE_THROW_CONTINUOUS_MAGIC)) {
            var holder = registry.getHolderOrThrow(type);
            for (var tag : new String[]{"is_scythe", "triggers_scythe_combo"}) {
                h.assertFalse(holder.is(TagKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath("malum", tag))),
                        "Common throw must not trigger Malum Culling or Geas via " + tag);
            }
        }
        h.succeed();
    }
}
