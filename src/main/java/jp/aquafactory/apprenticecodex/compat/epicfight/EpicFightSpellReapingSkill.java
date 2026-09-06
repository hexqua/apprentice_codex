package jp.aquafactory.apprenticecodex.compat.epicfight;

import com.google.common.collect.MapMaker;
import jp.aquafactory.apprenticecodex.compat.malum.MalumSpellReaperScytheBridge;
import jp.aquafactory.apprenticecodex.item.spellreaperscythe.ScytheThrowManager;
import jp.aquafactory.apprenticecodex.item.spellreaperscythe.SpellReaperScythe;
import net.minecraft.client.KeyMapping;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import yesman.epicfight.client.input.EpicFightKeyMappings;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.network.server.SPSkillFeedback;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.modules.ChargeableSkill;
import yesman.epicfight.skill.weaponinnate.WeaponInnateSkill;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

import java.util.Map;

public final class EpicFightSpellReapingSkill extends WeaponInnateSkill implements ChargeableSkill {
    // Entity.equalsはID比較なので、統合serverのclient/server実体が等価になる。
    // weakKeysは参照同一性で比較し、両sideの状態分離と切断後の解放を両立する。
    private final Map<Player, Holding> holdings = new MapMaker().weakKeys().makeMap();

    public EpicFightSpellReapingSkill(WeaponInnateSkill.Builder<?> builder) { super(builder); }

    public static WeaponInnateSkill.Builder<?> builder() {
        return WeaponInnateSkill.createWeaponInnateBuilder(EpicFightSpellReapingSkill::new)
                .setActivateType(Skill.ActivateType.HELD).setResource(Skill.Resource.NONE);
    }

    @Override
    public void onInitiate(SkillContainer container, yesman.epicfight.api.event.EntityEventListener listener) {
        super.onInitiate(container, listener);
        listener.registerEvent(yesman.epicfight.api.event.EpicFightEventHooks.Animation.START_ACTION, event -> {
            if (container.getExecutor().isHoldingSkill(this)
                    && !event.getAnimation().equals(Animations.STEEL_WHIRLWIND_CHARGING)) abort(container.getExecutor());
        }, this);
    }

    @Override
    public boolean canExecute(SkillContainer container) {
        return container.getExecutor().getOriginal().getMainHandItem().getItem() instanceof SpellReaperScythe;
    }

    @Override
    public void updateContainer(SkillContainer container) {
        // Resource.NONEでも標準HUDはstackを見る。常時使用可能なアイコンとして表示し、蓄積は行わない。
        container.setStack(1);
        super.updateContainer(container);
    }

    @Override
    public void onRemoved(SkillContainer container) {
        abort(container.getExecutor());
        super.onRemoved(container);
    }

    @Override
    public void executeOnServer(SkillContainer container, CompoundTag args) {
        // 標準実装は保持していないCASTでcontainerをactivateする。中断後の遅延CASTは無視する。
        if (container.getExecutor().isHoldingSkill(this)) {
            onStopHolding(container, SPSkillFeedback.executed(container.getSlot()));
        }
    }

    @Override
    public void cancelOnClient(SkillContainer container, CompoundTag args) {
        if (container.getExecutor().isHoldingSkill(this)) container.getExecutor().resetHolding();
        super.cancelOnClient(container, args);
    }

    @Override
    public java.util.List<net.minecraft.network.chat.Component> getTooltipOnItem(
            ItemStack stack, yesman.epicfight.world.capabilities.item.CapabilityItem cap, PlayerPatch<?> patch) {
        return java.util.List.of(
                net.minecraft.network.chat.Component.translatable(getTranslationKey()).withStyle(net.minecraft.ChatFormatting.WHITE),
                net.minecraft.network.chat.Component.translatable(getTranslationKey() + ".tooltip").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isExecutableState(PlayerPatch<?> patch) {
        var player = patch.getOriginal();
        return patch.isEpicFightMode() && player.isAlive() && !player.isSpectator()
                && player.getMainHandItem().getItem() instanceof SpellReaperScythe
                && (ScytheThrowManager.isThrown(player.getMainHandItem())
                    || (!patch.isLogicalClient() && ScytheThrowManager.active(player) != null)
                    || patch.getEntityState().canUseSkill());
    }

    @Override
    public void startHolding(SkillContainer container) {
        var patch = container.getExecutor();
        var player = patch.getOriginal();
        var stack = player.getMainHandItem();
        boolean recall = ScytheThrowManager.isThrown(stack) || (!patch.isLogicalClient() && ScytheThrowManager.active(player) != null);
        boolean ascension = MalumSpellReaperScytheBridge.ascensionLevel(player.level(), stack) > 0;
        boolean rebound = MalumSpellReaperScytheBridge.reboundLevel(player.level(), stack) > 0;
        boolean normal = !recall && !ascension && !rebound;
        holdings.put(player, new Holding(stack, player.getInventory().selected, player.level().getGameTime(), normal));
        if (!patch.isLogicalClient()) {
            if (recall) ScytheThrowManager.recall(player);
            else if (ascension) MalumSpellReaperScytheBridge.triggerEpicFightAscension(player, stack);
            else if (rebound) ScytheThrowManager.launchRebound(player, stack);
            else patch.playAnimationSynchronized(Animations.STEEL_WHIRLWIND_CHARGING, 0.0F);
        }
    }

    public void validateHolding(PlayerPatch<?> patch) {
        var player = patch.getOriginal();
        var holding = holdings.get(player);
        if (holding == null) return;
        if (!patch.isHoldingSkill(this) || !patch.isEpicFightMode() || !player.isAlive() || player.isSpectator()
                || player.getMainHandItem() != holding.stack || player.getInventory().selected != holding.slot
                || (holding.normal && (patch.getEntityState().hurt() || player.isUsingItem()
                    || player.level().getGameTime() - holding.started > 60))) {
            abort(patch);
        }
    }

    public void abort(PlayerPatch<?> patch) {
        if (patch.isHoldingSkill(this)) {
            patch.resetHolding();
            if (!patch.isLogicalClient()) super.cancelOnServer(patch.getSkill(this), null);
        } else holdings.remove(patch.getOriginal());
    }

    @Override
    public void resetHolding(SkillContainer container) {
        var patch = container.getExecutor();
        var holding = holdings.remove(patch.getOriginal());
        if (holding != null && holding.normal) {
            if (patch.isLogicalClient()) patch.getAnimator().stopPlaying(Animations.STEEL_WHIRLWIND_CHARGING);
            else patch.stopPlaying(Animations.STEEL_WHIRLWIND_CHARGING);
        }
    }

    @Override
    public void cancelOnServer(SkillContainer container, CompoundTag args) {
        if (container.getExecutor().isHoldingSkill(this)) container.getExecutor().resetHolding();
        super.cancelOnServer(container, args);
    }

    @Override
    public void onStopHolding(SkillContainer container, SPSkillFeedback feedback) {
        var patch = container.getServerExecutor();
        var player = patch.getOriginal();
        var holding = holdings.get(player);
        if (holding != null && holding.normal && patch.isEpicFightMode() && player.isAlive() && !player.isSpectator()
                && !patch.getEntityState().hurt() && !player.isUsingItem()
                && player.getMainHandItem() == holding.stack && player.getInventory().selected == holding.slot
                && MalumSpellReaperScytheBridge.ascensionLevel(player.level(), holding.stack) == 0
                && MalumSpellReaperScytheBridge.reboundLevel(player.level(), holding.stack) == 0) {
            long elapsed = player.level().getGameTime() - holding.started;
            if (elapsed >= 6 && elapsed <= 60) ScytheThrowManager.launchNormal(player, holding.stack, distance(elapsed));
        }
        // resetHolding側で保持状態とモーションも必ず解除する。
        cancelOnServer(container, null);
    }

    public static double distance(long ticks) { return 2.5D + Math.clamp(ticks - 6, 0L, 24L) * 7.5D / 24.0D; }
    // 最低時間未満の解放もserverまで通し、発射せず保持だけを解除する。
    @Override public int getMinChargingTicks() { return 0; }
    @Override public int getMaxChargingTicks() { return 30; }
    @Override public int getAllowedMaxChargingTicks() { return 60; }
    @Override public void holdTick(SkillContainer container) {
        if (container.getExecutor().isLogicalClient()) EpicFightScytheClient.checkInterruption(container);
        if (!container.getExecutor().isHoldingSkill(this)) return;
        var holding = holdings.get(container.getExecutor().getOriginal());
        if (holding != null && holding.normal) ChargeableSkill.super.holdTick(container);
        else container.getExecutor().setChargingTicks(1);
    }
    @Override public KeyMapping getKeyMapping() { return EpicFightKeyMappings.WEAPON_INNATE_SKILL; }
    @Override public ResourceLocation getSkillTexture() {
        return ResourceLocation.fromNamespaceAndPath("epicfight", "textures/gui/skills/weapon_innate/steel_whirlwind.png");
    }
    private record Holding(ItemStack stack, int slot, long started, boolean normal) {}
}
