package jp.aquafactory.apprenticecodex.mixin;

import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.Supplier;

@Mixin(value = SchoolType.class, remap = false)
public interface SchoolTypeAccessor {
    @Accessor(value = "powerAttribute", remap = false)
    Supplier<Attribute> apprenticecodex$getPowerAttribute();

    @Accessor(value = "resistanceAttribute", remap = false)
    Supplier<Attribute> apprenticecodex$getResistanceAttribute();
}
