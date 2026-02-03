package jp.aquafactory.apprenticecodex.common.spells.quickarms;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.common.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.common.spells.AbstractFirearmSpell;
import jp.aquafactory.apprenticecodex.common.utility.AudioTools;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class QuickArms extends AbstractFirearmSpell<QuickArmsHandgunEntity> {

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "quick_arms");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(SchoolRegistry.LIGHTNING_RESOURCE)
            .setMaxLevel(10)
            .setCooldownSeconds(6)
            .build();

    public QuickArms() {
        super(QuickArmsHandgunEntity.class);
        baseSpellPower = 100;
        spellPowerPerLevel = 10;
        manaCostPerLevel = 5;
        baseManaCost = 30;
        castTime = 0;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.irons_spellbooks.recast_count", getBulletCount(spellLevel, caster)),
                Component.literal(ApprenticeCodex.NAME)
        );
    }

    private float getDamage(int spellLevel, LivingEntity entity) {
        // スペルパワーはintのため、設定値をそもそも100倍として考える.
        return 4 * getSpellPower(spellLevel, entity) / 100.0f;
    }

    @Override
    public int getBulletCount(int spellLevel, LivingEntity entity) {
        return 2 + Math.round(2 * getSpellPower(spellLevel, entity) / 100.0f);
    }

    @Override
    public int getDurationTick() {
        return 20 * 3;
    }

    private int getRange(){
        // ハンドガンイメージなので近距離(2チャンク程度)
        return 16 * 2;
    }

    private int getFirstDelay(){
        return 20;
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
        return CastType.INSTANT;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        // 再詠唱で制御できなさそうなのでこちらは音を無しにする.
        return Optional.empty();
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.empty();
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.ANIMATION_INSTANT_CAST;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return AnimationHolder.pass();
    }

    @Override
    protected boolean onPreRecastWithWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, @NotNull QuickArmsHandgunEntity weapon) {
        if (!weapon.canFire()){
            if (entity instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable("ui.apprenticecodex.quick_arms.during_standby", this.getDisplayName(serverPlayer)).withStyle(ChatFormatting.RED)));
            }
            return false;
        }

        return true;
    }

    @Override
    protected boolean onPreRecastNoWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        return false;
    }

    @Override
    public void onCastWithWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, @NotNull QuickArmsHandgunEntity weapon){
        weapon.fire(level);
        AudioTools.playSoundFromEntity(level, entity, SoundEvents.ARMOR_EQUIP_NETHERITE, SoundSource.PLAYERS, 2.0f);
    }

    @Override
    public QuickArmsHandgunEntity onCastNoWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData){
        var summonWeapon = new QuickArmsHandgunEntity(EntityRegistry.QUICK_ARMS_HANDGUN.get(), level, entity);
        summonWeapon.locateAimingPosition();
        summonWeapon.setDamage(getDamage(spellLevel, entity));
        summonWeapon.setRange(getRange());
        summonWeapon.setFireStandby(getFirstDelay());
        level.addFreshEntity(summonWeapon);
        AudioTools.playSoundFromEntity(level, entity, SoundEvents.SHULKER_TELEPORT, SoundSource.PLAYERS, 2.0f);
        return summonWeapon;
    }
}
