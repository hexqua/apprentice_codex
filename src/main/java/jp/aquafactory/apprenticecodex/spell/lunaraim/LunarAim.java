package jp.aquafactory.apprenticecodex.spell.lunaraim;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import io.redspace.ironsspellbooks.network.casting.SyncTargetingDataPacket;
import net.neoforged.neoforge.network.PacketDistributor;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.DamageMultiplierKey;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public class LunarAim extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "lunar_aim");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(SchoolRegistry.ICE_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(8)
            .build();

    public LunarAim() {
        baseSpellPower = 400;
        spellPowerPerLevel = 100;
        baseManaCost = 60;
        manaCostPerLevel = 15;
        castTime = 40;
    }
    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.aoe_damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(getExplodeSize(), 2)),
                Component.translatable("ui.irons_spellbooks.projectile_count", getProjectileCount())
        );
    }

    private float getDamage(int spellLevel, LivingEntity entity) {
        var rawDamage = getSpellPower(spellLevel, entity) / 100.0f;
        return rawDamage * ApprenticeCodexServerConfig.damageMultiplier(DamageMultiplierKey.LUNAR_AIM);
    }

    private float getExplodeSize(){
        return 2.5f;
    }

    private int getProjectileCount(){
        return 4;
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
        return Optional.of(SoundRegistry.MAGIC_ARROW_CHARGE.get());
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(jp.aquafactory.apprenticecodex.registry.SoundRegistry.STELLAR_LAUNCH.get());
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
    public void onServerPreCast(Level level, int spellLevel, LivingEntity entity, MagicData magicData) {
        super.onServerPreCast(level, spellLevel, entity, magicData);
        if (level instanceof ServerLevel && magicData != null) magicData.setAdditionalCastData(selectTarget(entity, true));
    }

    private LunarAimCastData selectTarget(LivingEntity caster, boolean showTargeting) {
        var target = CombatTools.findLookCombatTarget(caster, 128, 1);
        if (target != null && caster instanceof ServerPlayer player) {
            // UtilのLivingEntity用constructorを避け、クリスタルも同じ対象表示データへ載せる。
            // 開始処理のない即時発動では、表示を解除する詠唱完了通知がないため送らない。
            if (showTargeting) PacketDistributor.sendToPlayer(player, new SyncTargetingDataPacket(this, List.of(target.getUUID())));
            player.displayClientMessage(Component.translatable("ui.irons_spellbooks.spell_target_success",
                    target.getDisplayName().getString(), getDisplayName(player)).withStyle(ChatFormatting.GREEN), true);
        }
        return new LunarAimCastData(target == null ? null : target.getUUID(), caster.level().dimension());
    }

    @Override
    public void onServerCastComplete(Level level, int spellLevel, LivingEntity entity, MagicData magicData, boolean cancelled) {
        super.onServerCastComplete(level, spellLevel, entity, magicData, cancelled);
        clearTarget(magicData);
    }

    private static void clearTarget(MagicData data) {
        if (data != null && data.getAdditionalCastData() instanceof LunarAimCastData) data.setAdditionalCastData(null);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            // 剣など開始処理を通らない発動のみ補完する。選出済みの対象なしは維持する。
            var data = playerMagicData != null && playerMagicData.getAdditionalCastData() instanceof LunarAimCastData stored
                    ? stored : selectTarget(entity, false);
            var target = data.targetId() != null && data.dimension().equals(level.dimension())
                    ? server.getEntity(data.targetId()) : null;
            // 当たり判定の大きさによらず、Iron'sのMagic/Fire Arrowと同じ発射位置にそろえる。
            var origin = entity.getEyePosition().add(entity.getForward()).add(0, -0.4, 0);
            for (int i = 0; i < getProjectileCount(); i++) {
                var arrow = new LunarAimArrowEntity(EntityRegistry.LUNAR_AIM_ARROW.get(), level);
                var direction = Vec3.directionFromRotation(entity.getXRot(), entity.getYRot() - 60 + 40 * i);
                arrow.launch(entity, origin, direction, getDamage(spellLevel, entity), getExplodeSize(), target);
                server.addFreshEntity(arrow);
            }
            clearTarget(playerMagicData);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
