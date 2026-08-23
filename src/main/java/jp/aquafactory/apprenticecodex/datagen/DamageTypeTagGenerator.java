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
    private static final String FORGE_NAMESPACE = "forge";

    public DamageTypeTagGenerator(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, ExistingFileHelper existingFileHelper) {
        super(output, Registries.DAMAGE_TYPE, lookupProvider, ApprenticeCodex.MODID, existingFileHelper);
    }

    private static TagKey<DamageType> create(String name) {
        return create(ApprenticeCodex.MODID, name);
    }

    private static TagKey<DamageType> create(String namespace, String name) {
        return TagKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(namespace, name));
    }

    public static final TagKey<DamageType> FORGE_IS_MAGIC = create(FORGE_NAMESPACE, "is_magic");
    private static final TagKey<DamageType> MALUM_CAN_SOUL_SHATTER = create("malum", "can_soul_shatter");

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
                FUJIN
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
                FUJIN
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
                FUJIN
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

        // Lodestone連携: magic_proficiency は forge:is_magic を見て倍率補正する。
        // このタグへ乗せないと armor bypass 系でも魔法ダメージとして扱われない。
        tag(FORGE_IS_MAGIC).addTag(MAGIC_DAMAGE);

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
