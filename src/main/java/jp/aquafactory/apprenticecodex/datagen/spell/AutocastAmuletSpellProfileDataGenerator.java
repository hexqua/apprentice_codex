package jp.aquafactory.apprenticecodex.datagen.spell;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletMobEffectCondition;
import jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletSpellProfile;
import jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletSpellProfileDefinition;
import jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletSpellProfileList;
import jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletSpellProfileManager;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.JsonCodecProvider;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public final class AutocastAmuletSpellProfileDataGenerator extends JsonCodecProvider<AutocastAmuletSpellProfileList> {
    public AutocastAmuletSpellProfileDataGenerator(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider,
            ExistingFileHelper existingFileHelper
    ) {
        super(
                output,
                PackOutput.Target.DATA_PACK,
                AutocastAmuletSpellProfileManager.DIRECTORY,
                PackType.SERVER_DATA,
                AutocastAmuletSpellProfileList.CODEC,
                lookupProvider,
                ApprenticeCodex.MODID,
                existingFileHelper
        );
    }

    @Override
    protected void gather() {
        unconditional(
                ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "profiles"),
                new AutocastAmuletSpellProfileList(createProfileDefinitions())
        );
    }

    public static List<AutocastAmuletSpellProfileDefinition> createProfileDefinitions() {
        return List.of(
                mobEffectProfile(
                        SpellRegistry.HEARTSTOP_SPELL,
                        MobEffectRegistry.HEARTSTOP,
                        0
                ),
                mobEffectProfile(
                        SpellRegistry.ABYSSAL_SHROUD_SPELL,
                        MobEffectRegistry.ABYSSAL_SHROUD,
                        0
                ),
                mobEffectProfile(
                        SpellRegistry.PLANAR_SIGHT_SPELL,
                        MobEffectRegistry.PLANAR_SIGHT,
                        20
                ),
                mobEffectProfile(
                        SpellRegistry.PLANAR_SIGHT_SPELL,
                        MobEffectRegistry.PLANAR_SIGHT,
                        20
                ),
                mobEffectProfile(
                        SpellRegistry.ECHOING_STRIKES_SPELL,
                        MobEffectRegistry.ECHOING_STRIKES,
                        0
                ),
                mobEffectProfile(
                        SpellRegistry.EVASION_SPELL,
                        MobEffectRegistry.EVASION,
                        0
                ),
                mobEffectProfile(
                        SpellRegistry.INVISIBILITY_SPELL,
                        MobEffectRegistry.TRUE_INVISIBILITY,
                        10
                ),
                mobEffectProfile(
                        SpellRegistry.ANGEL_WINGS_SPELL,
                        MobEffectRegistry.ANGEL_WINGS,
                        20
                ),
                mobEffectProfile(
                        SpellRegistry.FORTIFY_SPELL,
                        MobEffectRegistry.FORTIFY,
                        0
                ),
                mobEffectProfile(
                        SpellRegistry.HASTE_SPELL,
                        MobEffectRegistry.HASTENED,
                        20
                ),
                mobEffectProfile(
                        SpellRegistry.CHARGE_SPELL,
                        MobEffectRegistry.CHARGED,
                        20
                ),
                mobEffectProfile(
                        SpellRegistry.THUNDERSTORM_SPELL,
                        MobEffectRegistry.THUNDERSTORM,
                        0
                ),
                mobEffectProfile(
                        SpellRegistry.GLUTTONY_SPELL,
                        MobEffectRegistry.GLUTTONY,
                        0
                ),
                mobEffectProfile(
                        SpellRegistry.OAKSKIN_SPELL,
                        MobEffectRegistry.OAKSKIN,
                        0
                ),
                mobEffectProfile(
                        SpellRegistry.SPIDER_ASPECT_SPELL,
                        MobEffectRegistry.SPIDER_ASPECT,
                        0
                ),
                healthProfile(
                        SpellRegistry.HEAL_SPELL,
                        0.5f
                ),
                healthProfile(
                        SpellRegistry.GREATER_HEAL_SPELL,
                        0.2f
                )
        );
    }

    private static AutocastAmuletSpellProfileDefinition mobEffectProfile(
            Supplier<? extends AbstractSpell> spell,
            Supplier<? extends MobEffect> effect,
            int remainingTicksAtMost
    ) {
        return new AutocastAmuletSpellProfileDefinition(
                spellId(spell),
                new AutocastAmuletSpellProfile(
                        List.of(new AutocastAmuletMobEffectCondition(effectId(effect), remainingTicksAtMost)),
                        Optional.empty()
                )
        );
    }

    private static AutocastAmuletSpellProfileDefinition healthProfile(
            Supplier<? extends AbstractSpell> spell,
            float healthRatio
    ) {
        return new AutocastAmuletSpellProfileDefinition(
                spellId(spell),
                new AutocastAmuletSpellProfile(
                        List.of(),
                        Optional.of(healthRatio)
                )
        );
    }

    private static ResourceLocation spellId(Supplier<? extends AbstractSpell> spell) {
        return Objects.requireNonNull(spell.get().getSpellResource());
    }

    private static ResourceLocation effectId(Supplier<? extends MobEffect> effect) {
        return Objects.requireNonNull(BuiltInRegistries.MOB_EFFECT.getKey(effect.get()));
    }
}
