package jp.aquafactory.apprenticecodex.damage;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageEffects;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DeathMessageType;

public final class DamageTypes {
    public static final ResourceKey<DamageType> ARCANE_BEAM = register("arcane_beam");
    public static final ResourceKey<DamageType> ARCANE_BLAST = register("arcane_blast");
    public static final ResourceKey<DamageType> ARCHER_MULTIPLE = register("archer_multiple");
    public static final ResourceKey<DamageType> ARCHER_MULTIPLE_LAST = register("archer_multiple_last");
    public static final ResourceKey<DamageType> BREACHING_ENEMY = register("breaching_enemy");
    public static final ResourceKey<DamageType> BULLET_STREAM = register("bullet_stream");
    public static final ResourceKey<DamageType> COMMENCE_FIRE = register("commence_fire");
    public static final ResourceKey<DamageType> COMPOUND_PHIAL = register("compound_phial");
    public static final ResourceKey<DamageType> GRACED_RAIN = register("graced_rain");
    public static final ResourceKey<DamageType> QUICK_ARMS = register("quick_arms");
    public static final ResourceKey<DamageType> SKY_EDGE = register("sky_edge");
    public static final ResourceKey<DamageType> TINY_LUMBERJACK = register("tiny_lumberjack");
    public static final ResourceKey<DamageType> FLY_SWATTER = register("fly_swatter");
    public static final ResourceKey<DamageType> FEATHER_RUSH = register("feather_rush");
    public static final ResourceKey<DamageType> WORLD_FLATTER = register("world_flatter");
    public static final ResourceKey<DamageType> SLASH_BLADE = register("slash_blade");
    public static final ResourceKey<DamageType> MANTIS_LEAP = register("mantis_leap");
    public static final ResourceKey<DamageType> PHALANX_CHARGE = register("phalanx_charge");
    public static final ResourceKey<DamageType> HIGANBANA = register("higanbana");

    private static ResourceKey<DamageType> register(String name){
        return ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, name));
    }

    private static String generateIdWithNamespace(ResourceKey<DamageType> key){
        return key.location().getNamespace() + "." + key.location().getPath();
    }

    private static DamageType createTemplate(ResourceKey<DamageType> key){
        return new DamageType(generateIdWithNamespace(key), DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0f, DamageEffects.HURT, DeathMessageType.DEFAULT);
    }

    @SuppressWarnings("DuplicatedCode")
    public static void generateDamageTypeData(BootstapContext<DamageType> context){
        context.register(ARCANE_BEAM, createTemplate(ARCANE_BEAM));
        context.register(ARCANE_BLAST, createTemplate(ARCANE_BLAST));
        context.register(ARCHER_MULTIPLE, createTemplate(ARCHER_MULTIPLE));
        context.register(ARCHER_MULTIPLE_LAST, createTemplate(ARCHER_MULTIPLE));
        context.register(BREACHING_ENEMY, createTemplate(BREACHING_ENEMY));
        context.register(BULLET_STREAM, createTemplate(BULLET_STREAM));
        context.register(COMMENCE_FIRE, createTemplate(COMMENCE_FIRE));
        context.register(COMPOUND_PHIAL, createTemplate(COMPOUND_PHIAL));
        context.register(GRACED_RAIN, createTemplate(GRACED_RAIN));
        context.register(QUICK_ARMS, createTemplate(QUICK_ARMS));
        context.register(SKY_EDGE, createTemplate(SKY_EDGE));
        context.register(TINY_LUMBERJACK, createTemplate(TINY_LUMBERJACK));
        context.register(FLY_SWATTER, createTemplate(FLY_SWATTER));
        context.register(FEATHER_RUSH, createTemplate(FEATHER_RUSH));
        context.register(WORLD_FLATTER, createTemplate(WORLD_FLATTER));
        context.register(SLASH_BLADE, createTemplate(SLASH_BLADE));
        context.register(MANTIS_LEAP, createTemplate(MANTIS_LEAP));
        context.register(PHALANX_CHARGE, createTemplate(PHALANX_CHARGE));
        context.register(HIGANBANA, createTemplate(HIGANBANA));
    }
}
