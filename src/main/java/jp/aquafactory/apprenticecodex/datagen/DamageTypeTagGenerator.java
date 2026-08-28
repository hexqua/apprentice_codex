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
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

import static jp.aquafactory.apprenticecodex.damage.ApprenticeDamageTypeTags.TRIGGERS_IRONS_JEWELRY_PROJECTILE_HIT;
import static jp.aquafactory.apprenticecodex.damage.DamageTypes.*;

public final class DamageTypeTagGenerator extends TagsProvider<DamageType> {
    public DamageTypeTagGenerator(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, ExistingFileHelper existingFileHelper) {
        super(output, Registries.DAMAGE_TYPE, lookupProvider, ApprenticeCodex.MODID, existingFileHelper);
    }

    private static TagKey<DamageType> create(String name) {
        return TagKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, name));
    }

    public static final TagKey<DamageType> IGNORE_ARMOR = create("ignore_armor");
    public static final TagKey<DamageType> MAGIC_DAMAGE = create("magic_damage");
    public static final TagKey<DamageType> FIRE_DAMAGE = create("fire_damage");
    public static final TagKey<DamageType> BYPASSES_IFRAME = create("bypasses_iframe");
    public static final TagKey<DamageType> IGNORES_FLOATMOUNT_BROOM_IFRAME =
            create("ignores_floatmount_broom_iframe");
    public static final TagKey<DamageType> RANGED_ATTACK = create("ranged_attack");
    public static final TagKey<DamageType> CODEX_MAGIC = create("codex_magic");
    public static final TagKey<DamageType> EXPLOSIONS = create("explosions");
    public static final TagKey<DamageType> SUMMON_DAMAGE = create("summon_damage");
    private static final TagKey<DamageType> COMMON_IS_MAGIC = TagKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath("c", "is_magic")
    );

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
    protected void addTags(@NotNull HolderLookup.Provider provider) {
        // Datapackや他MODが箒固有の疑似i-Frameに関与しないDamageTypeを追加するための拡張口。
        tag(IGNORES_FLOATMOUNT_BROOM_IFRAME);

        // Iron's Jewelryはdirect entityのProjectile継承だけを見るため、実体弾を持たない銃撃を明示する。
        tag(TRIGGERS_IRONS_JEWELRY_PROJECTILE_HIT).add(
                BREACHING_ENEMY,
                BULLET_STREAM,
                QUICK_ARMS,
                SILENT_ASSASSIN,
                TIRO_VOLLEY,
                LETHAL_ASSAULT,
                DUAL_ACROBAT,
                COMMENCE_FIRE,
                SHOCK,
                ARCANE_BLAST
        );

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
                SILENT_ASSASSIN,
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
                PRECISION_JACK,
                GRIND_RUNNER,
                MANA_SLASH,
                AUTO_TURRET,
                ILLUMINATE_STELLAR,
                UNITE_LUNA,
                HEALING_BLOOM,
                SHOCK,
                TIRO_VOLLEY,
                MAGIC_SPEAR,
                FROST_RUNE,
                MYSTIC_SHIELD,
                INSCRIBE_ICE,
                INSCRIBE_ICE_BURST,
                HEAVENLY_FIST,
                LETHAL_ASSAULT,
                DUAL_ACROBAT,
                ARTISAN_SMASH,
                ANCHOR_BLINK,
                TOTEM_OF_PERMAFROST,
                FIELD_OVERSEER,
                SERVANT_GAZE,
                FUJIN,
                COMBUSTION_JET,
                BLOOD_BRAND,
                BLOOD_BRAND_BURST,
                BLOOD_BRAND_HIGANBANA_BURST
        );

        // IGNORE_ARMOR : 防御力無視、盾無視、ウィッチ抵抗(Malumは無関係)
        tag(IGNORE_ARMOR).add(
                ARCANE_BEAM,
                ARCANE_BLAST,
                COMPOUND_PHIAL,
                GRACED_RAIN,
                MOON_LIGHT,
                UNITE_LUNA,
                HEALING_BLOOM
        );

        // FIRE_DAMAGE: 火炎耐性有効.
        tag(FIRE_DAMAGE).add(
                THERMAL_PROCESS,
                COMBUSTION_JET
        );

        // BYPASSES_IFRAME: 無敵時間を無視するダメージ.
        tag(BYPASSES_IFRAME).add(
                ARCHER_MULTIPLE_LAST,
                BULLET_STREAM,
                GRACED_RAIN,
                SKY_EDGE,
                TINY_LUMBERJACK,
                FLY_SWATTER,
                SLASH_BLADE,
                MANTIS_LEAP,
                HIGANBANA,
                MOON_LIGHT,
                GRIND_RUNNER,
                ILLUMINATE_STELLAR,
                HEALING_BLOOM,
                HAUNTED_BONUS,
                TIRO_VOLLEY,
                MAGIC_SPEAR,
                INSCRIBE_ICE_BURST,
                LETHAL_ASSAULT,
                DUAL_ACROBAT,
                FUJIN,
                BLOOD_BRAND_BURST,
                BLOOD_BRAND_HIGANBANA_BURST
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
                SILENT_ASSASSIN,
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
                PRECISION_JACK,
                GRIND_RUNNER,
                MANA_SLASH,
                AUTO_TURRET,
                ILLUMINATE_STELLAR,
                UNITE_LUNA,
                HEALING_BLOOM,
                SHOCK,
                TIRO_VOLLEY,
                MANA_FORCE_BLADE,
                MAGIC_SPEAR,
                FROST_RUNE,
                MYSTIC_SHIELD,
                INSCRIBE_ICE,
                INSCRIBE_ICE_BURST,
                HEAVENLY_FIST,
                LETHAL_ASSAULT,
                DUAL_ACROBAT,
                ARTISAN_SMASH,
                ANCHOR_BLINK,
                TOTEM_OF_PERMAFROST,
                FIELD_OVERSEER,
                SERVANT_GAZE,
                FUJIN,
                COMBUSTION_JET,
                BLOOD_BRAND,
                BLOOD_BRAND_BURST,
                BLOOD_BRAND_HIGANBANA_BURST
        );

        // EXPLOSIONS: 爆発扱いのダメージ.
        tag(EXPLOSIONS).add(
                FLY_SWATTER,
                MAGIC_SPEAR,
                ARTISAN_SMASH
        );

        // SUMMON_DAMAGE: 召喚のネックレス系のダメージブーストが乗る.
        tag(SUMMON_DAMAGE).add(
                ARCHER_MULTIPLE,
                ARCHER_MULTIPLE_LAST,
                HEALING_BLOOM,
                AUTO_TURRET,
                TIRO_VOLLEY,
                TOTEM_OF_PERMAFROST,
                FIELD_OVERSEER,
                SERVANT_GAZE
        );

        // MAGIC_DAMAGE: Magic Proficiencyでダメージが伸びる(Malum限定)
        tag(MAGIC_DAMAGE).add(
                ARCANE_BEAM,
                ARCANE_BLAST,
                COMPOUND_PHIAL,
                MOON_LIGHT,
                UNITE_LUNA,
                HAUNTED_BONUS,
                MYSTIC_SHIELD,
                ANCHOR_BLINK,
                HOVERRIDE_BROOM
        );

        // Malum連携: 魔法ダメージ全体をSoul Shatter判定対象にする.
        tag(MALUM_CAN_SOUL_SHATTER).addTag(CODEX_MAGIC).add(
                HAUNTED_BONUS,
                MANA_FORCE_BLADE,
                HOVERRIDE_BROOM
        );

        // Lodestone連携: magic_proficiency / magic_resistance が参照する c:is_magic へ接続する.
        addTagLinks(COMMON_IS_MAGIC, MAGIC_DAMAGE);

        // バニラダメージタイプタグ.
        addTagLinks(DamageTypeTags.ALWAYS_HURTS_ENDER_DRAGONS, EXPLOSIONS);
        addTagLinks(DamageTypeTags.AVOIDS_GUARDIAN_THORNS, RANGED_ATTACK, EXPLOSIONS);
        addTagLinks(DamageTypeTags.BYPASSES_ARMOR, IGNORE_ARMOR);
        addTagLinks(DamageTypeTags.BYPASSES_COOLDOWN, BYPASSES_IFRAME);
        addTagLinks(DamageTypeTags.BYPASSES_SHIELD, IGNORE_ARMOR);
        addTagLinks(DamageTypeTags.IGNITES_ARMOR_STANDS, FIRE_DAMAGE);
        addTagLinks(DamageTypeTags.IS_EXPLOSION, EXPLOSIONS);
        addTagLinks(DamageTypeTags.IS_FIRE, FIRE_DAMAGE);
        addTagLinks(DamageTypeTags.WITCH_RESISTANT_TO, IGNORE_ARMOR);
    }
}
