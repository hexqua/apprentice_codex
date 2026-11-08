package jp.aquafactory.apprenticecodex.gametest.runtime;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import java.util.LinkedHashMap;
import jp.aquafactory.apprenticecodex.gametest.support.SpellTestSupport;
import jp.aquafactory.apprenticecodex.registry.CreativeTabRegistry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;

final class CreativeTabScenarios {
    private CreativeTabScenarios() {}

    static void creativeTabSpellsStayGroupedBySchool(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var apprenticeEnabledSpells = SpellRegistry.getEnabledSpells().stream()
                    .filter(SpellTestSupport::isApprenticeSpell)
                    .toList();
            var creativeTabSpells = CreativeTabRegistry.getCreativeTabSpells();

            helper.assertFalse(creativeTabSpells.isEmpty(), "No apprentice spells were exported to the creative tab");
            helper.assertTrue(creativeTabSpells.size() == apprenticeEnabledSpells.size(),
                    "Creative tab spell count mismatch: expected " + apprenticeEnabledSpells.size() + " but got " + creativeTabSpells.size());

            var schoolOrder = new LinkedHashMap<ResourceLocation, Integer>();
            var orderIndex = 0;
            for (var schoolType : SchoolRegistry.REGISTRY) {
                schoolOrder.putIfAbsent(schoolType.getId(), orderIndex++);
            }

            var previousSchoolIndex = -1;
            for (AbstractSpell spell : creativeTabSpells) {
                var spellId = spell.getSpellResource();
                helper.assertTrue(spellId != null, "Creative tab spell id is null");
                var schoolType = spell.getSchoolType();
                helper.assertTrue(schoolType != null, "Creative tab spell school is null: " + spellId);

                var schoolIndex = schoolOrder.getOrDefault(schoolType.getId(), Integer.MAX_VALUE);
                helper.assertTrue(previousSchoolIndex <= schoolIndex,
                        "Creative tab spell order is mixed across schools at " + spellId + " (" + schoolType.getId() + ")");
                previousSchoolIndex = schoolIndex;
            }
        });
    }
}
