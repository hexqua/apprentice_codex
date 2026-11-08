package jp.aquafactory.apprenticecodex.gametest.crafting.calibration.support;

import java.util.List;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class CalibrationTargetCases {
    private CalibrationTargetCases() {}

    public record AdjustmentCase(Item item, int slots, Item adjustment) {
        public ItemStack targetStack() {
            return new ItemStack(item);
        }
    }

    public static List<AdjustmentCase> adjustmentCases() {
        return List.of(
                new AdjustmentCase(ItemRegistry.SCROLLCASTER_GAUNTLET.get(), 3, io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get()),
                new AdjustmentCase(ItemRegistry.REVOLVERCAST_STAFF.get(), 3, io.redspace.ironsspellbooks.registries.ItemRegistry.COOLDOWN_RUNE.get()),
                new AdjustmentCase(ItemRegistry.MITHRIL_FREECAST_STAFF.get(), 3, io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get()),
                new AdjustmentCase(ItemRegistry.MAGI_AGENT_SUIT_HOOD.get(), 3, io.redspace.ironsspellbooks.registries.ItemRegistry.FIRE_RUNE.get()),
                new AdjustmentCase(ItemRegistry.AUTOCAST_AMULET.get(), 3, ItemRegistry.WISDOM_SHARD.get()),
                new AdjustmentCase(ItemRegistry.SATELLITE_FOLLOWCAST_AMULET.get(), 3, io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get()),
                new AdjustmentCase(ItemRegistry.BULWARK_GREATSHIELD.get(), 3, ItemRegistry.WISDOM_SHARD.get()),
                new AdjustmentCase(ItemRegistry.PARRYCAST_BUCKLER.get(), 3, io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get()),
                new AdjustmentCase(ItemRegistry.REFLECTCAST_SHIELD.get(), 3, io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get()),
                new AdjustmentCase(ItemRegistry.IRON_SPELLCASTER_GUN.get(), 1, ItemRegistry.SILVER_SPELL_AMPLIFIER.get()),
                new AdjustmentCase(ItemRegistry.COPPER_SPELLCASTER_GUN.get(), 1, ItemRegistry.SILVER_SPELL_AMPLIFIER.get()),
                new AdjustmentCase(ItemRegistry.GOLD_SPELLCASTER_GUN.get(), 1, ItemRegistry.SILVER_SPELL_AMPLIFIER.get()),
                new AdjustmentCase(ItemRegistry.DIAMOND_SPELLCASTER_GUN.get(), 1, ItemRegistry.SILVER_SPELL_AMPLIFIER.get()),
                new AdjustmentCase(ItemRegistry.CHARGECAST_CATALYSTBOOK.get(), 3, io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get()),
                new AdjustmentCase(ItemRegistry.MAGI_AGENT_SUIT_COAT.get(), 3, io.redspace.ironsspellbooks.registries.ItemRegistry.FIRE_RUNE.get()),
                new AdjustmentCase(ItemRegistry.MAGI_AGENT_SUIT_LEGGINGS.get(), 3, io.redspace.ironsspellbooks.registries.ItemRegistry.FIRE_RUNE.get()),
                new AdjustmentCase(ItemRegistry.MAGI_AGENT_SUIT_BOOTS.get(), 3, io.redspace.ironsspellbooks.registries.ItemRegistry.FIRE_RUNE.get()),
                new AdjustmentCase(ItemRegistry.MALIGNANT_SPELLCASTER_GUN.get(), 1, ItemRegistry.SILVER_SPELL_AMPLIFIER.get()),
                new AdjustmentCase(ItemRegistry.CHROMATIC_MAGIA_DRESS_HAT.get(), 3, io.redspace.ironsspellbooks.registries.ItemRegistry.MANA_RUNE.get()),
                new AdjustmentCase(ItemRegistry.CHROMATIC_MAGIA_DRESS_COAT.get(), 3, io.redspace.ironsspellbooks.registries.ItemRegistry.MANA_RUNE.get()),
                new AdjustmentCase(ItemRegistry.CHROMATIC_MAGIA_DRESS_LEGGINGS.get(), 3, io.redspace.ironsspellbooks.registries.ItemRegistry.MANA_RUNE.get()),
                new AdjustmentCase(ItemRegistry.CHROMATIC_MAGIA_DRESS_BOOTS.get(), 3, io.redspace.ironsspellbooks.registries.ItemRegistry.MANA_RUNE.get()),
                new AdjustmentCase(ItemRegistry.ELEMENT_MAIDEN_ROBE_RIBBON.get(), 3, io.redspace.ironsspellbooks.registries.ItemRegistry.MANA_RUNE.get()),
                new AdjustmentCase(ItemRegistry.ELEMENT_MAIDEN_ROBE_ROBE.get(), 3, io.redspace.ironsspellbooks.registries.ItemRegistry.MANA_RUNE.get()),
                new AdjustmentCase(ItemRegistry.ELEMENT_MAIDEN_ROBE_LEGGINGS.get(), 3, io.redspace.ironsspellbooks.registries.ItemRegistry.MANA_RUNE.get()),
                new AdjustmentCase(ItemRegistry.ELEMENT_MAIDEN_ROBE_BOOTS.get(), 3, io.redspace.ironsspellbooks.registries.ItemRegistry.MANA_RUNE.get())
        );
    }
}
