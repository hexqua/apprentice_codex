package jp.aquafactory.apprenticecodex.gametest.equipment.craftsmansdelight;

import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static jp.aquafactory.apprenticecodex.gametest.support.CurioTestSupport.equipRingCurio;
import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class CraftsmansDelightCostGameTests {
    private CraftsmansDelightCostGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void craftsmansDiscountsRegisteredSpellEventsOnlyWhileEquipped(GameTestHelper helper) {
        for (var spell : List.of(
                io.redspace.ironsspellbooks.api.registry.SpellRegistry.TOUCH_DIG.get(),
                io.redspace.ironsspellbooks.api.registry.SpellRegistry.SPECTRAL_HAMMER_SPELL.get(),
                SpellRegistry.HARVEST_MOON.get(), SpellRegistry.EARTH_FORGE.get())) {
            assertDiscount(helper, spell);
        }
        helper.succeed();
    }

    private static void assertDiscount(GameTestHelper helper, AbstractSpell spell) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(2, 1, 2), "craftsmans_cost");
        // 基礎CDと装備軽減の順序を検証するため、上流のCD属性は中立に固定する。
        player.getAttribute(AttributeRegistry.COOLDOWN_REDUCTION).setBaseValue(1.0D);
        for (var equipped : new boolean[]{false, true, false}) {
            equipRingCurio(player, equipped ? new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()) : ItemStack.EMPTY);
            var mana = new SpellOnCastEvent(player, spell.getSpellId(), 1, 15, spell.getSchoolType(), CastSource.SPELLBOOK);
            NeoForge.EVENT_BUS.post(mana);
            helper.assertTrue(mana.getManaCost() == (equipped ? 8 : 15),
                    spell.getSpellId() + ": mana discount must follow current equipment");
            var cooldown = new SpellCooldownAddedEvent.Pre(spell.getSpellCooldown(), spell, player, CastSource.SPELLBOOK);
            NeoForge.EVENT_BUS.post(cooldown);
            var expected = equipped ? Math.max(1, (int) (spell.getSpellCooldown() * 0.5D)) : spell.getSpellCooldown();
            helper.assertTrue(cooldown.getEffectiveCooldown() == expected,
                    spell.getSpellId() + ": unexpected cooldown, equipped=" + equipped);
        }
        var unrelated = SpellRegistry.COMMENCE_FIRE.get();
        equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));
        var mana = new SpellOnCastEvent(player, unrelated.getSpellId(), 1, 15, unrelated.getSchoolType(), CastSource.SPELLBOOK);
        NeoForge.EVENT_BUS.post(mana);
        helper.assertTrue(mana.getManaCost() == 15, "Ring must not discount unrelated spells");
        var cooldown = new SpellCooldownAddedEvent.Pre(unrelated.getSpellCooldown(), unrelated, player, CastSource.SPELLBOOK);
        NeoForge.EVENT_BUS.post(cooldown);
        helper.assertTrue(cooldown.getEffectiveCooldown() == unrelated.getSpellCooldown(),
                "Ring must not reduce unrelated spell cooldown");
    }
}
