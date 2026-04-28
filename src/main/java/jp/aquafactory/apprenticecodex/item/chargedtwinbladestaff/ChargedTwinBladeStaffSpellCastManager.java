package jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.MagicHelper;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserCastHelper;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserManaHelper;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserSpellProfileManager;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserSpellValidator;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class ChargedTwinBladeStaffSpellCastManager {
    public static final int CONTINUOUS_IMPACT_CAST_TICKS = 20 * 5;
    private static final Map<ServerLevel, List<ContinuousImpactCastRuntime>> ACTIVE_CONTINUOUS_CASTS = new WeakHashMap<>();

    private ChargedTwinBladeStaffSpellCastManager() {
    }

    public static boolean tryCastAtImpact(
            ServerLevel level,
            ServerPlayer owner,
            ItemStack sourceStack,
            ChargedTwinBladeStaffSpellPayload payload,
            Vec3 impactPosition,
            Vec3 forward
    ) {
        if (!payload.isPresent()) {
            return false;
        }

        var spellData = payload.toSpellData();
        if (spellData == SpellData.EMPTY) {
            return false;
        }

        var spell = spellData.getSpell();
        var castSource = payload.castSource();
        var ownerMagicData = MagicData.getPlayerMagicData(owner);
        if (ownerMagicData == null) {
            return false;
        }

        var staffProfile = ChargedTwinBladeStaffSpellProfileManager.getProfile(spell);
        if (staffProfile.isPresent()) {
            return tryCastWithStaffProfile(
                    level,
                    owner,
                    sourceStack,
                    spellData,
                    staffProfile.get(),
                    ownerMagicData,
                    impactPosition,
                    forward,
                    castSource,
                    payload.castingSlot()
            );
        }

        if (SpellDispenserSpellProfileManager.getProfile(spell).isEmpty()) {
            notifyUnsupportedCast(owner, spellData, sourceStack);
            return false;
        }

        var manaAccess = new PlayerManaAccess(owner);
        if (!canOwnerCastWithManaAccess(owner, spellData, castSource, ownerMagicData, manaAccess)) {
            return false;
        }

        if (NeoForge.EVENT_BUS.post(new SpellPreCastEvent(owner, spell.getSpellId(), spellData.getLevel(), spell.getSchoolType(), castSource)).isCanceled()) {
            return false;
        }

        var validation = new SpellDispenserSpellValidator.ValidationResult(
                sourceStack.copy(),
                spellData,
                SpellDispenserSpellValidator.FailureReason.NONE
        );
        var ownerProfile = owner.getGameProfile();
        if (spell.getCastType() == CastType.CONTINUOUS) {
            // 1.20.1 では位置固定の継続魔法 owner を client が追跡できない spell があるため、
            // tracked anchor を含む Spell Dispenser の継続詠唱方式をそのまま流用する。
            var startResult = SpellDispenserCastHelper.tryStartContinuousCast(
                    level,
                    impactPosition,
                    forward,
                    validation,
                    sourceStack,
                    ownerProfile,
                    manaAccess,
                    castSource,
                    payload.castingSlot(),
                    CONTINUOUS_IMPACT_CAST_TICKS
            );
            if (!startResult.result().succeeded() || startResult.session() == null) {
                return false;
            }

            ACTIVE_CONTINUOUS_CASTS.computeIfAbsent(level, key -> new ArrayList<>()).add(
                    new ContinuousImpactCastRuntime(
                            owner.getUUID(),
                            impactPosition,
                            forward,
                            startResult.session(),
                            level.getGameTime() + CONTINUOUS_IMPACT_CAST_TICKS
                    )
            );
            return true;
        }

        var result = SpellDispenserCastHelper.tryCast(
                level,
                impactPosition,
                forward,
                validation,
                sourceStack,
                ownerProfile,
                manaAccess,
                castSource,
                payload.castingSlot()
        );
        if (!result.succeeded()) {
            return false;
        }

        addCooldownIfNeeded(owner, spellData, castSource);
        return true;
    }

    private static boolean tryCastWithStaffProfile(
            ServerLevel level,
            ServerPlayer owner,
            ItemStack sourceStack,
            SpellData spellData,
            ChargedTwinBladeStaffSpellProfile profile,
            MagicData ownerMagicData,
            Vec3 impactPosition,
            Vec3 forward,
            CastSource castSource,
            String castingSlot
    ) {
        var spell = spellData.getSpell();
        if (spell.getCastType() == CastType.CONTINUOUS) {
            // 専用プロファイル v1 は単発用途だけを対象にする。継続魔法は Spell Dispenser プロファイルへ明示登録して扱う。
            return false;
        }
        if (spell.getRecastCount(spellData.getLevel(), owner) > 0) {
            if (!profile.allowInitialRecast()) {
                return false;
            }
            if (ownerMagicData.getPlayerRecasts().hasRecastForSpell(spell)) {
                return false;
            }
        }

        var spellCaster = switch (profile.castMode()) {
            case PLAYER_SELF -> owner;
            case IMPACT_PROXY_OWNER_MAGIC -> createImpactProxy(level, owner, impactPosition, forward);
        };
        var manaAccess = new PlayerManaAccess(owner);
        if (!canOwnerCastWithManaAccess(owner, spellData, castSource, ownerMagicData, manaAccess)) {
            return false;
        }

        if (NeoForge.EVENT_BUS.post(new SpellPreCastEvent(owner, spell.getSpellId(), spellData.getLevel(), spell.getSchoolType(), castSource)).isCanceled()) {
            return false;
        }

        var originalMana = ownerMagicData.getMana();
        var restoreManaAfterCast = manaAccess.isManaConsumptionExempt();
        var originalSyncedData = ownerMagicData.getSyncedData();
        try {
            // Iron's の一部 spell は MagicData の詠唱状態と casting item を参照する。
            // SyncedSpellData はホイール選択も保持するため、代理 caster 用の一時データは必ず元へ戻す。
            ownerMagicData.setSyncedData(new SyncedSpellData(spellCaster));
            ownerMagicData.initiateCast(spell, spellData.getLevel(), 0, castSource, castingSlot);
            ownerMagicData.setPlayerCastingItem(sourceStack.copy());
            syncOwnerManaForImpactCast(manaAccess, ownerMagicData);
            if (!spell.checkPreCastConditions(level, spellData.getLevel(), spellCaster, ownerMagicData)) {
                return false;
            }
            syncOwnerManaForImpactCast(manaAccess, ownerMagicData);
            spell.onServerPreCast(level, spellData.getLevel(), spellCaster, ownerMagicData);

            if (!SpellDispenserManaHelper.tryConsumeSpellMana(manaAccess, spellData)) {
                return false;
            }

            syncOwnerManaForImpactCast(manaAccess, ownerMagicData);
            spell.onCast(level, spellData.getLevel(), spellCaster, castSource, ownerMagicData);
            syncOwnerManaForImpactCast(manaAccess, ownerMagicData);
            spell.onServerCastComplete(level, spellData.getLevel(), spellCaster, ownerMagicData, false);
        } catch (RuntimeException exception) {
            ApprenticeCodex.LOGGER.warn(
                    "Charged Twin Blade Staff impact cast exception: spell={}, castMode={}",
                    spell.getSpellResource(),
                    profile.castMode().getSerializedName(),
                    exception
            );
            return false;
        } finally {
            try {
                ownerMagicData.resetCastingState();
            } finally {
                if (restoreManaAfterCast) {
                    ownerMagicData.setMana(originalMana);
                }
                ownerMagicData.setSyncedData(originalSyncedData);
                originalSyncedData.syncToPlayer(owner);
            }
        }

        addCooldownIfNeeded(owner, spellData, castSource);
        return true;
    }

    private static FakePlayer createImpactProxy(
            ServerLevel level,
            ServerPlayer owner,
            Vec3 impactPosition,
            Vec3 forward
    ) {
        var proxy = FakePlayerFactory.get(level, new GameProfile(owner.getUUID(), owner.getGameProfile().getName()));
        proxy.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        proxy.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        proxy.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);

        var normalizedForward = forward.lengthSqr() > 1.0E-6D ? forward.normalize() : owner.getLookAngle();
        var yaw = (float) Mth.wrapDegrees(Mth.atan2(-normalizedForward.x, normalizedForward.z) * Mth.RAD_TO_DEG);
        var horizontal = Math.sqrt(normalizedForward.x * normalizedForward.x + normalizedForward.z * normalizedForward.z);
        var pitch = (float) Mth.wrapDegrees(-Mth.atan2(normalizedForward.y, horizontal) * Mth.RAD_TO_DEG);
        var feetY = impactPosition.y - proxy.getEyeHeight(proxy.getPose());
        proxy.moveTo(impactPosition.x, feetY, impactPosition.z, yaw, pitch);
        proxy.setYBodyRot(yaw);
        proxy.setYHeadRot(yaw);
        proxy.yBodyRotO = yaw;
        proxy.yHeadRotO = yaw;
        proxy.xRotO = pitch;
        return proxy;
    }

    private static boolean canOwnerCastWithManaAccess(
            ServerPlayer owner,
            SpellData spellData,
            CastSource castSource,
            MagicData ownerMagicData,
            PlayerManaAccess manaAccess
    ) {
        var originalMana = ownerMagicData.getMana();
        try {
            syncOwnerManaForImpactCast(manaAccess, ownerMagicData);
            return spellData.getSpell().canBeCastedBy(spellData.getLevel(), castSource, ownerMagicData, owner).isSuccess();
        } finally {
            if (manaAccess.isManaConsumptionExempt()) {
                ownerMagicData.setMana(originalMana);
            }
        }
    }

    private static void syncOwnerManaForImpactCast(PlayerManaAccess manaAccess, MagicData magicData) {
        // Iron's の pre-cast は MagicData のマナ値を見るため、creative 免除時だけ一時的に十分な値を見せる。
        magicData.setMana(manaAccess.isManaConsumptionExempt()
                ? SpellDispenserManaHelper.MAX_MANA
                : manaAccess.getCurrentMana());
    }

    private static void notifyUnsupportedCast(ServerPlayer owner, SpellData spellData, ItemStack sourceStack) {
        owner.displayClientMessage(
                Component.translatable(
                        "ui.apprenticecodex.charged_twin_blade_staff.unsupported_cast",
                        spellData.getSpell().getDisplayName(owner),
                        sourceStack.getHoverName()
                ),
                true
        );
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        var runtimes = ACTIVE_CONTINUOUS_CASTS.get(level);
        if (runtimes == null || runtimes.isEmpty()) {
            return;
        }

        var iterator = runtimes.iterator();
        while (iterator.hasNext()) {
            var runtime = iterator.next();
            var owner = level.getPlayerByUUID(runtime.ownerId());
            if (!(owner instanceof ServerPlayer serverPlayer) || serverPlayer.isDeadOrDying() || serverPlayer.isSpectator()) {
                SpellDispenserCastHelper.finishContinuousCast(level, runtime.session(), true);
                iterator.remove();
                continue;
            }

            SpellDispenserCastHelper.syncContinuousCastTransform(runtime.session(), runtime.position(), runtime.forward());
            if (level.getGameTime() >= runtime.finishAtGameTime()) {
                SpellDispenserCastHelper.finishContinuousCast(level, runtime.session(), false);
            } else if (SpellDispenserCastHelper.tickContinuousCast(level, runtime.session())) {
                continue;
            }

            applyCooldownIfNeeded(serverPlayer, runtime.session());
            iterator.remove();
        }

        if (runtimes.isEmpty()) {
            ACTIVE_CONTINUOUS_CASTS.remove(level);
        }
    }

    private static void applyCooldownIfNeeded(ServerPlayer owner, SpellDispenserCastHelper.ContinuousCastSession session) {
        if (session.consumeFinishedCooldownTicks() <= 0) {
            return;
        }

        addCooldownIfNeeded(owner, session.validation().spellData(), session.castSource());
    }

    private static void addCooldownIfNeeded(ServerPlayer owner, SpellData spellData, CastSource castSource) {
        var spell = spellData.getSpell();
        if (spell.getRecastCount(spellData.getLevel(), owner) > 0) {
            return;
        }

        MagicHelper.MAGIC_MANAGER.addCooldown(owner, spell, castSource);
    }

    private record ContinuousImpactCastRuntime(
            UUID ownerId,
            Vec3 position,
            Vec3 forward,
            SpellDispenserCastHelper.ContinuousCastSession session,
            long finishAtGameTime
    ) {
    }

    private static final class PlayerManaAccess implements SpellDispenserManaHelper.ManaAccess {
        private final ServerPlayer player;

        private PlayerManaAccess(ServerPlayer player) {
            this.player = player;
        }

        @Override
        public int getCurrentMana() {
            return Mth.floor(MagicData.getPlayerMagicData(player).getMana());
        }

        @Override
        public void setCurrentMana(int mana) {
            var magicData = MagicData.getPlayerMagicData(player);
            magicData.setMana(Math.max(0.0F, mana));
            PacketDistributor.sendToPlayer(player, new SyncManaPacket(magicData));
        }

        @Override
        public int getInventorySlotCount() {
            return 0;
        }

        @Override
        public @NotNull ItemStack getInventoryStack(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public void setInventoryStack(int slot, @NotNull ItemStack stack) {
        }

        @Override
        public boolean isManaConsumptionExempt() {
            return player.isCreative();
        }
    }
}
