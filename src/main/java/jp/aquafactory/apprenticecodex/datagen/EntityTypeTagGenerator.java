package jp.aquafactory.apprenticecodex.datagen;

import io.redspace.ironsspellbooks.util.ModTags;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagEntry;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public final class EntityTypeTagGenerator extends TagsProvider<EntityType<?>> {
    public EntityTypeTagGenerator(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, ExistingFileHelper existingFileHelper) {
        super(output, Registries.ENTITY_TYPE, lookupProvider, ApprenticeCodex.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(@NotNull HolderLookup.Provider provider) {
        // 1.20.1 の一般判定から漏れる相手だけを明示追加する。
        tag(TagRegistry.EntityTypes.COUNTS_AS_UNDEAD)
                .addOptional(ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "necromancer"));
        tag(TagRegistry.EntityTypes.GRAVITY_BOUND_DENYLIST);
        // 通常弾も含め、Iron'sの誘導と専用のモード選択を競合させない。
        tag(ModTags.GUIDING_BOLT_IMMUNE)
                .add(TagEntry.element(EntityRegistry.SACRED_ARROW.getId()))
                .add(TagEntry.element(EntityRegistry.LUNAR_AIM_ARROW.getId()));
    }
}
