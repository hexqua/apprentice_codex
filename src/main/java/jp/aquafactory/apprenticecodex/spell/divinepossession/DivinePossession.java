package jp.aquafactory.apprenticecodex.spell.divinepossession;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.armor.ElementMaidenRobeItem;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public class DivinePossession extends AbstractSpell {
    private static final String REQUIRED_ALL_PIECES_KEY =
            "ui.apprenticecodex.divine_possession.required_all_pieces";
    private static final EquipmentSlot[] REQUIRED_ARMOR_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "divine_possession");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(SchoolRegistry.HOLY_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(300)
            .setAllowCrafting(false)
            .build();

    public DivinePossession() {
        baseSpellPower = 0;
        spellPowerPerLevel = 50;
        baseManaCost = 600;
        manaCostPerLevel = 200;
        castTime = 0;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(getDurationTicks(spellLevel, caster), 1))
        );
    }

    @Override
    public boolean allowLooting() {
        // アイテム固有なのでドロップからも除外.
        return false;
    }

    private int getDurationTicks(int spellLevel, LivingEntity caster) {
        return 20 * 20 + Math.round(20 * 10 * getSpellPower(spellLevel, caster) / 100.0f);
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellId;
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
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundRegistry.KAMI.get());
    }

    @Override
    public CastResult canBeCastedBy(int spellLevel, CastSource castSource, MagicData playerMagicData, Player player) {
        if (!hasFullElementMaidenRobe(player)) {
            return new CastResult(
                    CastResult.Type.FAILURE,
                    Component.translatable(REQUIRED_ALL_PIECES_KEY).withStyle(ChatFormatting.RED)
            );
        }

        return super.canBeCastedBy(spellLevel, castSource, playerMagicData, player);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        entity.addEffect(new MobEffectInstance(
                EffectRegistry.DIVINE_POSSESSION,
                getDurationTicks(spellLevel, entity),
                0,
                false,
                false,
                true
        ));
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    private static boolean hasFullElementMaidenRobe(Player player) {
        if (player == null) {
            return false;
        }

        for (var slot : REQUIRED_ARMOR_SLOTS) {
            if (!(player.getItemBySlot(slot).getItem() instanceof ElementMaidenRobeItem)) {
                return false;
            }
        }
        return true;
    }
}
