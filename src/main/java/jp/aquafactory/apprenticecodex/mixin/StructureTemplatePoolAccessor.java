package jp.aquafactory.apprenticecodex.mixin;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(StructureTemplatePool.class)
public interface StructureTemplatePoolAccessor {
    @Accessor("templates")
    ObjectArrayList<StructurePoolElement> apprenticecodex$getTemplates();

    @Accessor("rawTemplates")
    List<Pair<StructurePoolElement, Integer>> apprenticecodex$getRawTemplates();

    @Mutable
    @Accessor("rawTemplates")
    void apprenticecodex$setRawTemplates(List<Pair<StructurePoolElement, Integer>> rawTemplates);
}
