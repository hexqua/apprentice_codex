package jp.aquafactory.apprenticecodex.spell.flyswatter;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.DamageMultiplierKey;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.spell.AbstractSummonWeaponSpell;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import jp.aquafactory.apprenticecodex.utility.RaycastTools;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FlySwatter extends AbstractSummonWeaponSpell<FlySwatterLauncherEntity> {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "fly_swatter");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(SchoolRegistry.LIGHTNING_RESOURCE)
            .setMaxLevel(3)
            .setCooldownSeconds(12)
            .build();

    public FlySwatter() {
        super(FlySwatterLauncherEntity.class);
        baseSpellPower = 100;
        spellPowerPerLevel = 50;
        manaCostPerLevel = 5;
        baseManaCost = 10;
        castTime = 300;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.apprenticecodex.lock_on_count", Utils.stringTruncation(getLockOnCount(spellLevel, caster), 1)),
                Component.translatable("ui.apprenticecodex.lock_on_time", Utils.timeFromTicks(getLockOnInterval(spellLevel, caster), 1))
        );
    }

    private float getDamage(int spellLevel, LivingEntity entity) {
        var rawDamage = 3 + 2 * getSpellPower(spellLevel, entity) / 100.0f;
        return rawDamage * ApprenticeCodexServerConfig.damageMultiplier(DamageMultiplierKey.FLY_SWATTER);
    }

    private float getExplosionRadius(){
        // レベルで上がると制御しづらいので固定値.
        return 3.0f;
    }

    private double getRange() {
        return 128;
    }

    private int getLockOnCount(int spellLevel, LivingEntity entity) {
        return Math.min(8, Math.round(2 * getSpellPower(spellLevel, entity) / 100));
    }

    private int getLockOnInterval(int spellLevel, LivingEntity entity) {
        return Math.max(10, 40 - Math.round(10 * getSpellPower(spellLevel, entity) / 100));
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
        return CastType.CONTINUOUS;
    }

    @Override
    public final Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundRegistry.VANILLA_SUMMON_WEAPON.get());
    }

    @Override
    public final Optional<SoundEvent> getCastFinishSound() {
        return Optional.empty();
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.ANIMATION_CONTINUOUS_CAST_ONE_HANDED;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return AnimationHolder.none();
    }

    @Override
    protected SummonWeaponSpellCastData createCastData() {
        return new FlySwatterCastData();
    }

    @Override
    public FlySwatterLauncherEntity onCastNoWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        var summonWeapon = new FlySwatterLauncherEntity(EntityRegistry.FLY_SWATTER_LAUNCHER.get(), level, entity);
        summonWeapon.setDamage(getDamage(spellLevel, entity));
        summonWeapon.setRadius(getExplosionRadius());
        level.addFreshEntity(summonWeapon);
        return summonWeapon;
    }

    @Override
    public void onCastTickWithWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, @NotNull FlySwatterLauncherEntity weapon) {
        // FocusStaffbow の continuous 終了時は完了処理前に補正が外れるため、詠唱中の最新値を保持しておく。
        weapon.setDamage(getDamage(spellLevel, entity));
        if (playerMagicData.getAdditionalCastData() instanceof FlySwatterCastData castData && castData.lockOnEntityIdList.size() < getLockOnCount(spellLevel, entity)) {
            var result = RaycastTools.raycastFromEye(entity, getRange(), 0.5, e -> CombatTools.isValidCombatTarget(e, entity));
            if (result.hitEntity() != null) {
                var id = result.hitEntity().getId();
                if (castData.currentLockOnId != id) {
                    castData.currentLockOnId = id;
                    castData.currentLockOnTick = 0;
                } else {
                    ++castData.currentLockOnTick;
                    weapon.setCastingReticleEffect(castData.currentLockOnTick, result.hitPosition());
                    if (castData.currentLockOnTick >= getLockOnInterval(spellLevel, entity)) {
                        AudioTools.playSoundFromEntity(level, weapon, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.5f, 1.5f);
                        castData.lockOnEntityIdList.add(id);
                        castData.currentLockOnId = -1;
                        castData.currentLockOnTick = 0;
                        if (entity instanceof ServerPlayer server) {
                            server.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable(
                                    "ui.apprenticecodex.fly_swatter.lockon_entity", result.hitEntity().getName().getString(), castData.lockOnEntityIdList.size(), getLockOnCount(spellLevel, entity), this.getDisplayName(server)).withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD)));
                        }
                    }
                }
            } else {
                weapon.setCastingReticleEffect(castData.currentLockOnTick, null);
                castData.currentLockOnId = -1;
                castData.currentLockOnTick = 0;
            }
        } else {
            weapon.setCastingReticleEffect(0, null);
        }
    }

    @Override
    public CompleteCastTypes onCastCompleteWithWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, boolean cancelled, @NotNull FlySwatterLauncherEntity weapon) {
        if (playerMagicData.getAdditionalCastData() instanceof FlySwatterCastData castData) {
            if (!castData.lockOnEntityIdList.isEmpty()){
                weapon.setLockOnEntityList(castData.lockOnEntityIdList, level);
                weapon.startFiring(level, entity);
            }
        }

        return CompleteCastTypes.RELEASE_WEAPON;
    }

    public static class FlySwatterCastData extends SummonWeaponSpellCastData {
        private int currentLockOnId;
        private int currentLockOnTick;
        private List<Integer> lockOnEntityIdList = new ArrayList<>();

        @Override
        public void writeToBuffer(FriendlyByteBuf friendlyByteBuf) {
            super.writeToBuffer(friendlyByteBuf);
            friendlyByteBuf.writeInt(currentLockOnId);
            friendlyByteBuf.writeInt(currentLockOnTick);
            friendlyByteBuf.writeCollection(lockOnEntityIdList, FriendlyByteBuf::writeInt);
        }

        @Override
        public void readFromBuffer(FriendlyByteBuf friendlyByteBuf) {
            super.readFromBuffer(friendlyByteBuf);
            currentLockOnId = friendlyByteBuf.readInt();
            currentLockOnTick = friendlyByteBuf.readInt();
            lockOnEntityIdList = friendlyByteBuf.readList(FriendlyByteBuf::readInt);
        }

        @Override
        public void reset() {
            super.reset();
            currentLockOnId = -1;
            currentLockOnTick = 0;
            lockOnEntityIdList.clear();
        }

        @Override
        public CompoundTag serializeNBT() {
            var tag = super.serializeNBT();
            tag.putInt("CurrentLockOnId", currentLockOnId);
            tag.putInt("CurrentLockOnTick", currentLockOnTick);
            tag.putIntArray("LockOnEntityIdList", lockOnEntityIdList.stream().mapToInt(i -> i).toArray());
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            super.deserializeNBT(nbt);
            currentLockOnId = nbt.getInt("CurrentLockOnId");
            currentLockOnTick = nbt.getInt("CurrentLockOnTick");
            lockOnEntityIdList = Arrays.stream(nbt.getIntArray("LockOnEntityIdList")).boxed().collect(Collectors.toList());
        }
    }
}
