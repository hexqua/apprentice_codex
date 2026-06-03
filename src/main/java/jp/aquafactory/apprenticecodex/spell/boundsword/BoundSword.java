package jp.aquafactory.apprenticecodex.spell.boundsword;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.DamageMultiplierKey;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class BoundSword extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "bound_sword");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.EVOCATION_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(30)
            .build();

    public BoundSword() {
        baseSpellPower = 100;
        spellPowerPerLevel = 50;
        baseManaCost = 50;
        manaCostPerLevel = 150;
        castTime = 30;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.apprenticecodex.bound_sword.sword_damage_attribute", Utils.stringTruncation(getWeaponDamage(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(getDuration(), 1))
        );
    }

    private float getWeaponDamage(int spellLevel, LivingEntity entity) {
        var rawDamage = 3 + 3 * getSpellPower(spellLevel, entity) / 100.0f;
        return rawDamage
                * (float) entity.getAttributeValue(AttributeRegistry.SUMMON_DAMAGE.get())
                * ApprenticeCodexServerConfig.damageMultiplier(DamageMultiplierKey.BOUND_SWORD);
    }

    int getDuration() {
        return 20 * 120;
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
    public int getRecastCount(int spellLevel, LivingEntity entity) {
        return 2;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundRegistry.VANILLA_POWER_ACTIVATE.get());
    }
    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundRegistry.VANILLA_SUMMON_WEAPON.get());
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
    public ICastDataSerializable getEmptyCastData() {
        return new BoundSwordCastData();
    }

    @Override
    public void onServerPreCast(Level level, int spellLevel, LivingEntity entity,
                                @Nullable MagicData playerMagicData) {
        super.onServerPreCast(level, spellLevel, entity, playerMagicData);

        if (!(entity instanceof ServerPlayer serverPlayer) || playerMagicData == null) {
            return;
        }

        var forceTryDualWield = !playerMagicData.getPlayerRecasts().hasRecastForSpell(this)
                && BoundSwordManager.hasDualWieldCompat()
                && serverPlayer.isShiftKeyDown();
        var castData = playerMagicData.getAdditionalCastData() instanceof BoundSwordCastData data
                ? data
                : new BoundSwordCastData();
        castData.forceTryDualWield = forceTryDualWield;
        playerMagicData.setAdditionalCastData(castData);

        if (forceTryDualWield) {
            serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(
                    Component.translatable("ui.apprenticecodex.bound_sword.force_try_dual_wield")
                            .withStyle(ChatFormatting.YELLOW)
            ));
        }
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (!level.isClientSide && entity instanceof ServerPlayer serverPlayer) {
            if (playerMagicData.getPlayerRecasts().hasRecastForSpell(this)) {
                BoundSwordManager.deactivate(serverPlayer, true);
            } else {
                var forceTryDualWield = playerMagicData.getAdditionalCastData() instanceof BoundSwordCastData castData
                        && castData.forceTryDualWield;
                BoundSwordManager.activate(serverPlayer, spellLevel, castSource, playerMagicData, this,
                        getWeaponDamage(spellLevel, entity), forceTryDualWield);
            }
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        return entity instanceof Player && super.checkPreCastConditions(level, spellLevel, entity, playerMagicData);
    }

    @Override
    public void onRecastFinished(ServerPlayer serverPlayer, RecastInstance recastInstance, RecastResult recastResult,
                                 ICastDataSerializable castDataSerializable) {
        BoundSwordManager.deactivate(serverPlayer, false);
        if (io.redspace.ironsspellbooks.registries.ItemRegistry.GREATER_CONJURERS_TALISMAN.get()
                .isEquippedBy(serverPlayer)) {
            return;
        }
        super.onRecastFinished(serverPlayer, recastInstance, recastResult, castDataSerializable);
    }

    public static class BoundSwordCastData implements ICastDataSerializable {
        private boolean forceTryDualWield;

        @Override
        public void writeToBuffer(FriendlyByteBuf friendlyByteBuf) {
            friendlyByteBuf.writeBoolean(forceTryDualWield);
        }

        @Override
        public void readFromBuffer(FriendlyByteBuf friendlyByteBuf) {
            forceTryDualWield = friendlyByteBuf.readBoolean();
        }

        @Override
        public void reset() {
            forceTryDualWield = false;
        }

        @Override
        public CompoundTag serializeNBT() {
            var tag = new CompoundTag();
            tag.putBoolean("ForceTryDualWield", forceTryDualWield);
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            forceTryDualWield = nbt.getBoolean("ForceTryDualWield");
        }
    }
}
