package jp.aquafactory.apprenticecodex.datagen;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceLocation;
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
    public static final TagKey<DamageType> BYPASSES_IFRAME = create("bypasses_iframe");
    public static final TagKey<DamageType> RANGED_ATTACK = create("ranged_attack");
    public static final TagKey<DamageType> EXPLOSIONS = create("explosions");

    @Override
    protected void addTags(@NotNull HolderLookup.Provider pProvider) {
        // MAGIC_DAMAGE : 防御力無視、盾無視、ウィッチ抵抗.
        tag(MAGIC_DAMAGE).add(
                ARCANE_BEAM,
                ARCANE_BLAST,
                COMPOUND_PHIAL,
                GRACED_RAIN
        );

        // BYPASSES_IFRAME : 無敵時間無効.
        tag(BYPASSES_IFRAME).add(
                ARCHER_MULTIPLE_LAST,
                BULLET_STREAM,
                GRACED_RAIN,
                SKY_EDGE,
                TINY_LUMBERJACK,
                FLY_SWATTER,
                WORLD_FLATTER
        );

        // RANGED_ATTACK : 遠距離攻撃扱い(現状はガーディアンのトゲ無効)
        // 基本的にはほぼ全て入る.
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
                WORLD_FLATTER
        );

        // EXPLOSIONS : 爆発ダメージ(エンチャ、常にエンドラ有効、ガーディアンのトゲ無効)
        tag(EXPLOSIONS).add(
                FLY_SWATTER
        );
    }
}
