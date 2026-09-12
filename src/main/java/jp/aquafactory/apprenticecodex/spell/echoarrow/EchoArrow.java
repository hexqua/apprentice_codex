package jp.aquafactory.apprenticecodex.spell.echoarrow;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.DamageMultiplierKey;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public class EchoArrow extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "echo_arrow");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(SchoolRegistry.ELDRITCH_RESOURCE)
            .setMaxLevel(4)
            .setCooldownSeconds(8)
            .build();

    public EchoArrow() {
        baseSpellPower = 1500;
        spellPowerPerLevel = 500;
        baseManaCost = 120;
        manaCostPerLevel = 30;
        castTime = 30;
    }
    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(), 2)),
                Component.translatable("ui.apprenticecodex.echo_arrow.followup_count", getArrowCount(spellLevel, caster)),
                Component.translatable("ui.apprenticecodex.echo_arrow.duration", Utils.timeFromTicks(getDuration(spellLevel, caster), 2))
        );
    }

    private float getDamage() {
        // 威力は本数ベースで決めるため、1発単位は変化しない.
        return 3 * ApprenticeCodexServerConfig.damageMultiplier(DamageMultiplierKey.ECHO_ARROW);
    }

    private int getArrowCount(int spellLevel, LivingEntity entity) {
        return Math.max(0, Math.round(getSpellPower(spellLevel, entity) / 100.0f));
    }

    private int getDuration(int spellLevel, LivingEntity entity) {
        // 初弾と追撃の飛行時間を除き、着弾から最後の発射までを表示する。
        int count = getArrowCount(spellLevel, entity);
        return count == 0 ? 0 : 10 + count - 1;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellId;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return config;
    }

    @Override
    public CastType getCastType() {
        return CastType.LONG;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundRegistry.ECHO_ARROW_CHARGE.get());
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundRegistry.ECHO_ARROW_CAST.get());
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.BOW_CHARGE_ANIMATION;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return AnimationHolder.none();
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (!level.isClientSide) {
            var arrow = new EchoArrowEntity(EntityRegistry.ECHO_ARROW.get(), level);
            var core = new EchoArrowCoreEntity(EntityRegistry.ECHO_ARROW_CORE.get(), level);
            // 当たり判定の大きさによらず、Iron'sのMagic/Fire Arrowと同じ発射位置にそろえる。
            var origin = entity.getEyePosition().add(entity.getForward()).add(0, -0.4, 0);
            float damage = getDamage();
            core.configure(entity, origin, damage, getArrowCount(spellLevel, entity), arrow);
            arrow.launch(entity, origin, entity.getLookAngle(), damage, true, EchoArrowEntity.SPEED, core);
            if (level.addFreshEntity(core)) {
                if (!level.addFreshEntity(arrow)) core.discard();
            }
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
