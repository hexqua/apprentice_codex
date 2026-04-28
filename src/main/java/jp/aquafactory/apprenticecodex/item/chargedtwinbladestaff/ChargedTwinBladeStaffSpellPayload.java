package jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public record ChargedTwinBladeStaffSpellPayload(
        @Nullable ResourceLocation spellId,
        int spellLevel,
        String castSourceName,
        String castingSlot
) {
    private static final String SPELL_ID_TAG = "SpellId";
    private static final String SPELL_LEVEL_TAG = "SpellLevel";
    private static final String CAST_SOURCE_TAG = "CastSource";
    private static final String CASTING_SLOT_TAG = "CastingSlot";
    public static final ChargedTwinBladeStaffSpellPayload EMPTY =
            new ChargedTwinBladeStaffSpellPayload(null, 0, CastSource.NONE.name(), "");

    public boolean isPresent() {
        return spellId != null;
    }

    public SpellData toSpellData() {
        if (spellId == null) {
            return SpellData.EMPTY;
        }

        var spell = SpellRegistry.getSpell(spellId);
        return spell == null || spell == SpellRegistry.none() ? SpellData.EMPTY : new SpellData(spell, Math.max(1, spellLevel));
    }

    public CastSource castSource() {
        try {
            return CastSource.valueOf(castSourceName);
        } catch (IllegalArgumentException exception) {
            return CastSource.NONE;
        }
    }

    public CompoundTag save() {
        var tag = new CompoundTag();
        if (spellId != null) {
            tag.putString(SPELL_ID_TAG, spellId.toString());
            tag.putInt(SPELL_LEVEL_TAG, Math.max(1, spellLevel));
            tag.putString(CAST_SOURCE_TAG, castSourceName);
            tag.putString(CASTING_SLOT_TAG, castingSlot);
        }
        return tag;
    }

    public static ChargedTwinBladeStaffSpellPayload load(@Nullable CompoundTag tag) {
        if (tag == null || !tag.contains(SPELL_ID_TAG)) {
            return EMPTY;
        }

        return new ChargedTwinBladeStaffSpellPayload(
                ResourceLocation.tryParse(tag.getString(SPELL_ID_TAG)),
                Math.max(1, tag.getInt(SPELL_LEVEL_TAG)),
                tag.getString(CAST_SOURCE_TAG),
                tag.getString(CASTING_SLOT_TAG)
        );
    }

    public static ChargedTwinBladeStaffSpellPayload capture(@Nullable SpellSelectionManager.SelectionOption selection, Player player) {
        if (selection == null || selection.spellData == SpellData.EMPTY) {
            return EMPTY;
        }

        var spell = selection.spellData.getSpell();
        if (spell == null || spell == SpellRegistry.none() || spell.getSpellResource() == null) {
            return EMPTY;
        }

        var castType = spell.getCastType();
        if (castType != CastType.INSTANT && castType != CastType.LONG && castType != CastType.CONTINUOUS) {
            return EMPTY;
        }

        var profile = ChargedTwinBladeStaffSpellProfileManager.getProfile(spell);
        var hasRecast = spell.getRecastCount(selection.spellData.getLevel(), player) > 0;
        if (hasRecast && profile.filter(ChargedTwinBladeStaffSpellProfile::allowInitialRecast).isEmpty()) {
            return EMPTY;
        }
        if (hasRecast && MagicData.getPlayerMagicData(player).getPlayerRecasts().hasRecastForSpell(spell)) {
            return EMPTY;
        }

        return new ChargedTwinBladeStaffSpellPayload(
                spell.getSpellResource(),
                spell.getLevelFor(selection.spellData.getLevel(), player),
                selection.getCastSource().name(),
                selection.slot
        );
    }
}
