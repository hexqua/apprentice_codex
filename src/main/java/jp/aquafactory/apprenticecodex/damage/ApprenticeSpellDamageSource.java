package jp.aquafactory.apprenticecodex.damage;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** 独自のDamageType・タグ・死亡メッセージを維持したまま、攻撃元の魔法を公開する。 */
public final class ApprenticeSpellDamageSource extends SpellDamageSource {
    private final DamageSource original;
    private boolean forcedIndirect;

    @SuppressWarnings("DataFlowIssue")
    public ApprenticeSpellDamageSource(DamageSource original, AbstractSpell spell, Entity target) {
        // 上流のconstructorはregistry取得にdirectEntityを要求する。実際のdirectEntityは元の値を返す。
        // causingEntityはDamageSourceが許容するnullを維持し、所有者不在時に攻撃者を捏造しない。
        super(original.getDirectEntity() != null ? original.getDirectEntity() : target,
                original.getEntity(), original.sourcePositionRaw(), spell);
        this.original = original;
        if (original instanceof SpellDamageSource source) {
            setLifestealPercent(source.getLifestealPercent());
            setFireTicks(source.getFireTime());
            setFreezeTicks(source.getFreezeTicks());
            setIFrames(source.getIFrames());
        }
    }

    @Override
    public @Nullable Entity getDirectEntity() {
        return original.getDirectEntity();
    }

    @Override
    public @Nullable ItemStack getWeaponItem() {
        return original.getWeaponItem();
    }

    @Override
    public boolean isDirect() {
        return !forcedIndirect && original.isDirect();
    }

    @Override
    public ApprenticeSpellDamageSource indirect() {
        forcedIndirect = true;
        return this;
    }

    @Override
    public @Nullable Vec3 getSourcePosition() {
        return original.getSourcePosition();
    }

    @Override
    public @NotNull Component getLocalizedDeathMessage(@NotNull LivingEntity target) {
        return original.getLocalizedDeathMessage(target);
    }

    @Override
    public boolean is(@NotNull TagKey<DamageType> key) {
        return original.is(key);
    }

    @Override
    public boolean is(@NotNull ResourceKey<DamageType> key) {
        return original.is(key);
    }

    @Override
    public @NotNull DamageType type() {
        return original.type();
    }

    @Override
    public @NotNull Holder<DamageType> typeHolder() {
        return original.typeHolder();
    }
}
