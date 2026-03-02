package jp.aquafactory.apprenticecodex.datagen;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

import static jp.aquafactory.apprenticecodex.damage.DamageTypes.*;

public final class DamageTypeTagGenerator extends TagsProvider<DamageType> {
    public DamageTypeTagGenerator(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, ExistingFileHelper existingFileHelper) {
        super(output, Registries.DAMAGE_TYPE, lookupProvider, ApprenticeCodex.MODID, existingFileHelper);
    }

    private static TagKey<DamageType> create(String name) {
        return TagKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, name));
    }

    public static final TagKey<DamageType> MAGIC_DAMAGE = create("magic_damage");
    public static final TagKey<DamageType> FIRE_DAMAGE = create("fire_damage");
    public static final TagKey<DamageType> BYPASSES_IFRAME = create("bypasses_iframe");
    public static final TagKey<DamageType> RANGED_ATTACK = create("ranged_attack");
    public static final TagKey<DamageType> CODEX_MAGIC = create("codex_magic");
    public static final TagKey<DamageType> EXPLOSIONS = create("explosions");

    private static final TagKey<DamageType> MALUM_CAN_SOUL_SHATTER = TagKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath("malum", "can_soul_shatter")
    );

    @SafeVarargs
    private void addTagLinks(TagKey<DamageType> target, TagKey<DamageType>... sources) {
        var appender = tag(target);
        for (var source : sources) {
            appender.addTag(source);
        }
    }

    @Override
    protected void addTags(@NotNull HolderLookup.Provider pProvider) {
        // CODEX_MAGIC: このMODの魔法由来ダメージ全体(Malum互換などで使用)
        tag(CODEX_MAGIC).add(
                ARCANE_BEAM,
                ARCANE_BLAST,
                ARCHER_MULTIPLE,
                ARCHER_MULTIPLE_LAST,
                BREACHING_ENEMY,
                BULLET_STREAM,
                COMMENCE_FIRE,
                COMPOUND_PHIAL,
                GRACED_RAIN,
                QUICK_ARMS,
                SKY_EDGE,
                TINY_LUMBERJACK,
                FLY_SWATTER,
                FEATHER_RUSH,
                WORLD_FLATTER,
                SLASH_BLADE,
                MANTIS_LEAP,
                PHALANX_CHARGE,
                HIGANBANA,
                THERMAL_PROCESS,
                MOON_LIGHT,
                PRECISION_JACK
        );

        // MAGIC_DAMAGE : 防御力無視、盾無視、ウィッチ抵抗.
        tag(MAGIC_DAMAGE).add(
                ARCANE_BEAM,
                ARCANE_BLAST,
                COMPOUND_PHIAL,
                GRACED_RAIN,
                HIGANBANA,
                MOON_LIGHT
        );

        // FIRE_DAMAGE: 火炎耐性有効.
        tag(FIRE_DAMAGE).add(
                THERMAL_PROCESS
        );

        // BYPASSES_IFRAME: 無敵時間を無視するダメージ.
        tag(BYPASSES_IFRAME).add(
                ARCHER_MULTIPLE_LAST,
                BULLET_STREAM,
                GRACED_RAIN,
                SKY_EDGE,
                TINY_LUMBERJACK,
                FLY_SWATTER,
                WORLD_FLATTER,
                SLASH_BLADE,
                MANTIS_LEAP,
                HIGANBANA,
                MOON_LIGHT
        );

        // RANGED_ATTACK: 遠距離攻撃扱い(現状はガーディアンのトゲ無効) ※召喚武器は遠距離扱い.
        tag(RANGED_ATTACK).add(
                ARCANE_BEAM,
                ARCANE_BLAST,
                ARCHER_MULTIPLE,
                ARCHER_MULTIPLE_LAST,
                BREACHING_ENEMY,
                BULLET_STREAM,
                COMMENCE_FIRE,
                COMPOUND_PHIAL,
                GRACED_RAIN,
                QUICK_ARMS,
                SKY_EDGE,
                TINY_LUMBERJACK,
                FLY_SWATTER,
                FEATHER_RUSH,
                WORLD_FLATTER,
                SLASH_BLADE,
                MANTIS_LEAP,
                PHALANX_CHARGE,
                HIGANBANA,
                THERMAL_PROCESS,
                MOON_LIGHT,
                PRECISION_JACK
        );

        // EXPLOSIONS: 爆発扱いのダメージ.
        tag(EXPLOSIONS).add(
                FLY_SWATTER
        );

        // Malum連携: 魔法ダメージ全体をSoul Shatter判定対象にする.
        tag(MALUM_CAN_SOUL_SHATTER).addTag(CODEX_MAGIC);

        // バニラダメージタイプタグ.
        addTagLinks(DamageTypeTags.ALWAYS_HURTS_ENDER_DRAGONS, EXPLOSIONS);
        addTagLinks(DamageTypeTags.AVOIDS_GUARDIAN_THORNS, RANGED_ATTACK, EXPLOSIONS);
        addTagLinks(DamageTypeTags.BYPASSES_ARMOR, MAGIC_DAMAGE);
        addTagLinks(DamageTypeTags.BYPASSES_COOLDOWN, BYPASSES_IFRAME);
        addTagLinks(DamageTypeTags.BYPASSES_SHIELD, MAGIC_DAMAGE);
        addTagLinks(DamageTypeTags.IGNITES_ARMOR_STANDS, FIRE_DAMAGE);
        addTagLinks(DamageTypeTags.IS_EXPLOSION, EXPLOSIONS);
        addTagLinks(DamageTypeTags.IS_FIRE, FIRE_DAMAGE);
        addTagLinks(DamageTypeTags.WITCH_RESISTANT_TO, MAGIC_DAMAGE);
    }
}
