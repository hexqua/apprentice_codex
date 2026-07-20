package jp.aquafactory.apprenticecodex.mixin;

import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import io.redspace.ironsspellbooks.player.ClientSpellCastHelper;
import io.redspace.ironsspellbooks.render.animation.AnimationHelper;
import jp.aquafactory.apprenticecodex.event.client.ClientPlacementPreviewManager;
import jp.aquafactory.apprenticecodex.event.client.ClientMultipurposeStaffrifleCastContext;
import jp.aquafactory.apprenticecodex.event.client.ClientSwingcastStaffCastContext;
import jp.aquafactory.apprenticecodex.compat.bettercombat.BetterCombatScrollcasterGauntletCompat;
import jp.aquafactory.apprenticecodex.item.CastAnimationOverrideItem;
import jp.aquafactory.apprenticecodex.item.chargecastcatalystbook.ChargecastCatalystbook;
import jp.aquafactory.apprenticecodex.item.chargecastcatalystbook.ChargecastCatalystbookClientCastIntent;
import jp.aquafactory.apprenticecodex.item.chargecastcatalystbook.ChargecastCatalystbookStartSoundContext;
import jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbow;
import jp.aquafactory.apprenticecodex.item.multipurposestaffrifle.MultipurposeStaffrifle;
import jp.aquafactory.apprenticecodex.item.mithrilfreecaststaff.MithrilFreecastStaff;
import jp.aquafactory.apprenticecodex.item.revolvercaststaff.RevolvercastStaff;
import jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbowClientPresentationState;
import jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbowStartSoundContext;
import jp.aquafactory.apprenticecodex.item.swingstaff.AbstractSwingcastStaffItem;
import jp.aquafactory.apprenticecodex.item.shield.ReflectcastShield;
import jp.aquafactory.apprenticecodex.item.shield.ReflectcastShieldClientEffectState;
import jp.aquafactory.apprenticecodex.utility.SpellSelectionStackResolver;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.neoforged.fml.ModList;

import java.util.UUID;

@Mixin(value = ClientSpellCastHelper.class, remap = false)
public abstract class ClientSpellCastHelperMixin {
    @Unique
    private static final String apprentice_codex$BETTER_COMBAT_MOD_ID = "bettercombat";

    // 特殊アイテム発動時だけ開始アニメーションを差し替え、音や入力抑制など他の前処理は元の spell 実装へ委ねる.
    @Inject(
            method = "handleClientBoundOnCastStarted",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void handleClientBoundOnCastStarted(UUID castingEntityId, String spellId, int spellLevel, CallbackInfo ci) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        var player = minecraft.level.getPlayerByUUID(castingEntityId);
        if (player == null) {
            return;
        }

        var spell = SpellRegistry.getSpell(spellId);
        var castingSlot = ClientMagicData.getSyncedSpellData(player).getCastingEquipmentSlot();
        // 配置 preview は開始時 target を固定したいので、clientbound の cast start に合わせて初期化する。
        ClientPlacementPreviewManager.beginPreview(spell, player, spellLevel);
        var castingStack = apprentice_codex$resolveCastingStack(player, castingSlot);
        var focusStaffbowRightClickPresentation =
                FocusStaffbowClientPresentationState.activatePending(castingEntityId, spellId);
        ClientSwingcastStaffCastContext.tryActivate(castingEntityId, castingStack, spell);
        ClientMultipurposeStaffrifleCastContext.tryActivate(castingEntityId, castingStack, spell);
        if (player == minecraft.player && castingStack.getItem() instanceof ReflectcastShield) {
            ReflectcastShieldClientEffectState.beginLocalSuccessFlash(
                    apprentice_codex$resolveCastingHand(castingSlot),
                    spellId
            );
        }

        var animationStack = apprentice_codex$resolveCastStartAnimationStack(
                player,
                castingStack,
                spell,
                castingSlot,
                focusStaffbowRightClickPresentation
        );
        if (!(animationStack.getItem() instanceof CastAnimationOverrideItem animationOverrideItem)) {
            if (apprentice_codex$shouldHandleFocusStaffbowShortcutStart(player, castingSlot, focusStaffbowRightClickPresentation)) {
                spell.getCastStartAnimation().getForPlayer()
                        .ifPresent(resourceLocation -> AnimationHelper.animatePlayerStart(player, resourceLocation));
                apprentice_codex$runClientPreCast(
                        spell,
                        spellLevel,
                        player,
                        apprentice_codex$resolveCastingHand(castingSlot),
                        false
                );
                ci.cancel();
            }
            return;
        }

        if (apprentice_codex$shouldSuppressCastStartAnimation(player, animationStack, spell)) {
            apprentice_codex$runClientPreCast(
                    spell,
                    spellLevel,
                    player,
                    apprentice_codex$resolveClientPreCastHand(castingSlot, focusStaffbowRightClickPresentation),
                    focusStaffbowRightClickPresentation
            );
            ci.cancel();
            return;
        }

        if (!animationOverrideItem.shouldOverrideCastStartAnimation(animationStack, spell)) {
            return;
        }

        if (!focusStaffbowRightClickPresentation) {
            animationOverrideItem.getCastStartAnimation(animationStack, spell, spellLevel)
                    .getForPlayer()
                    .ifPresent(resourceLocation -> AnimationHelper.animatePlayerStart(player, resourceLocation));
        }
        apprentice_codex$runClientPreCast(
                spell,
                spellLevel,
                player,
                apprentice_codex$resolveClientPreCastHand(castingSlot, focusStaffbowRightClickPresentation),
                focusStaffbowRightClickPresentation
        );
        ci.cancel();
    }

    // 0tick に短縮した LONG は開始直後に終了パケットも飛ぶため、元 spell の Finish をそのまま使うと二重モーションになる。
    @Redirect(
            method = "handleClientBoundOnCastFinished",
            at = @At(
                    value = "INVOKE",
                    target = "Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;getCastFinishAnimation()Lio/redspace/ironsspellbooks/api/util/AnimationHolder;"
            )
    )
    private static AnimationHolder redirectSpellGunCastFinishAnimation(AbstractSpell spell, UUID castingEntityId, String spellId, boolean cancelled) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return spell.getCastFinishAnimation();
        }

        var player = minecraft.level.getPlayerByUUID(castingEntityId);
        if (player == null) {
            return spell.getCastFinishAnimation();
        }

        var castingSlot = ClientMagicData.getSyncedSpellData(player).getCastingEquipmentSlot();
        var focusStaffbowRightClickPresentation =
                FocusStaffbowClientPresentationState.hasActive(castingEntityId, spellId);
        var castingStack = apprentice_codex$resolveSpellAnimationStack(
                player,
                spell,
                castingSlot,
                focusStaffbowRightClickPresentation
        );
        if (!(castingStack.getItem() instanceof CastAnimationOverrideItem animationOverrideItem)) {
            return spell.getCastFinishAnimation();
        }

        if (animationOverrideItem.shouldOverrideCastFinishAnimation(castingStack, spell)) {
            return animationOverrideItem.getCastFinishAnimation(castingStack, spell, cancelled);
        }

        return apprentice_codex$shouldSuppressCastFinishAnimation(player, castingStack, spell)
                ? AnimationHolder.pass()
                : spell.getCastFinishAnimation();
    }

    @Inject(
            method = "handleClientBoundOnCastFinished",
            at = @At("HEAD")
    )
    private static void handleClientBoundOnCastFinished(UUID castingEntityId, String spellId, boolean cancelled, CallbackInfo ci) {
        // 完了/キャンセルの区別なく preview をここで落とし、残留を防ぐ。
        ClientPlacementPreviewManager.finishPreview(net.minecraft.resources.ResourceLocation.tryParse(spellId));
    }

    @Inject(
            method = "handleClientBoundOnCastFinished",
            at = @At("RETURN")
    )
    private static void handleClientBoundOnCastFinishedReturn(UUID castingEntityId, String spellId, boolean cancelled, CallbackInfo ci) {
        ClientSwingcastStaffCastContext.clearFinished(castingEntityId, spellId);
        ClientMultipurposeStaffrifleCastContext.clearFinished(castingEntityId, spellId);
        FocusStaffbowClientPresentationState.clear(castingEntityId);
    }

    @Unique
    private static void apprentice_codex$runClientPreCast(
            AbstractSpell spell,
            int spellLevel,
            net.minecraft.world.entity.player.Player player,
            InteractionHand hand,
            boolean suppressFocusStaffbowStartSound
    ) {
        if (ChargecastCatalystbookClientCastIntent.matchesActive(player.getUUID(), spell)) {
            ChargecastCatalystbookStartSoundContext.runSuppressed(player.getUUID(), () ->
                    apprentice_codex$runClientPreCastWithoutChargecastSound(
                            spell, spellLevel, player, hand, suppressFocusStaffbowStartSound
                    )
            );
            return;
        }

        apprentice_codex$runClientPreCastWithoutChargecastSound(
                spell, spellLevel, player, hand, suppressFocusStaffbowStartSound
        );
    }

    @Unique
    private static void apprentice_codex$runClientPreCastWithoutChargecastSound(
            AbstractSpell spell,
            int spellLevel,
            net.minecraft.world.entity.player.Player player,
            InteractionHand hand,
            boolean suppressFocusStaffbowStartSound
    ) {
        if (!suppressFocusStaffbowStartSound) {
            spell.onClientPreCast(player.level(), spellLevel, player, hand, null);
            return;
        }

        FocusStaffbowStartSoundContext.runSuppressed(player.getUUID(), () ->
                spell.onClientPreCast(player.level(), spellLevel, player, hand, null)
        );
    }

    @Unique
    private static ItemStack apprentice_codex$resolveCastingStack(net.minecraft.world.entity.player.Player player, String castingSlot) {
        if (SpellSelectionManager.MAINHAND.equals(castingSlot)) {
            return player.getMainHandItem();
        }
        if (SpellSelectionManager.OFFHAND.equals(castingSlot)) {
            return apprentice_codex$resolveOffhandStack(player);
        }
        // Curios 発動では identifier_index が同期されるため、装備中の実スタックから animation override を解決する。
        return SpellSelectionStackResolver.resolveSelectionStack(player, castingSlot);
    }

    @Unique
    private static InteractionHand apprentice_codex$resolveCastingHand(String castingSlot) {
        return SpellSelectionManager.OFFHAND.equals(castingSlot) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    @Unique
    private static InteractionHand apprentice_codex$resolveClientPreCastHand(String castingSlot,
                                                                             boolean focusStaffbowRightClickPresentation) {
        // FocusStaffbow 右クリック中は offhand spell でも bow 側の見た目だけを残し、腕の二重アニメを避ける。
        return focusStaffbowRightClickPresentation ? InteractionHand.MAIN_HAND : apprentice_codex$resolveCastingHand(castingSlot);
    }

    @Unique
    private static ItemStack apprentice_codex$resolveCastStartAnimationStack(Player player, ItemStack castingStack,
                                                                             @Nullable AbstractSpell spell,
                                                                             String castingSlot,
                                                                             boolean focusStaffbowRightClickPresentation) {
        if (focusStaffbowRightClickPresentation) {
            var mainHand = player.getMainHandItem();
            if (apprentice_codex$hasCastStartAnimationOverride(player, mainHand, spell)) {
                return mainHand;
            }
        }

        if (apprentice_codex$hasCastStartAnimationOverride(player, castingStack, spell)) {
            return castingStack;
        }
        if (apprentice_codex$shouldIgnoreFocusStaffbowFallback(player, castingSlot, focusStaffbowRightClickPresentation)) {
            return ItemStack.EMPTY;
        }

        // Curios の spellbook slot など、castingEquipmentSlot が手スロットを指さない経路では
        // 実際の発動体を手持ちから再解決しないと start override が拾えない。
        if (apprentice_codex$isHandCastingSlot(castingSlot)) {
            return ItemStack.EMPTY;
        }

        var mainHand = player.getMainHandItem();
        if (mainHand != castingStack && apprentice_codex$hasCastStartAnimationOverride(player, mainHand, spell)) {
            return mainHand;
        }

        var offHand = apprentice_codex$resolveOffhandStack(player);
        if (offHand != castingStack && apprentice_codex$hasCastStartAnimationOverride(player, offHand, spell)) {
            return offHand;
        }

        return ItemStack.EMPTY;
    }

    @Unique
    private static ItemStack apprentice_codex$resolveSpellAnimationStack(net.minecraft.world.entity.player.Player player,
                                                                         AbstractSpell spell,
                                                                         String castingSlot,
                                                                         boolean focusStaffbowRightClickPresentation) {
        if (focusStaffbowRightClickPresentation) {
            var mainHand = player.getMainHandItem();
            if (apprentice_codex$hasCastAnimationOverride(player, mainHand, spell)) {
                return mainHand;
            }
        }

        var castingStack = apprentice_codex$resolveCastingStack(player, castingSlot);
        if (apprentice_codex$hasCastAnimationOverride(player, castingStack, spell)) {
            return castingStack;
        }
        if (apprentice_codex$shouldIgnoreFocusStaffbowFallback(player, castingSlot, focusStaffbowRightClickPresentation)) {
            return ItemStack.EMPTY;
        }

        if (apprentice_codex$isHandCastingSlot(castingSlot)) {
            return ItemStack.EMPTY;
        }

        var mainHand = player.getMainHandItem();
        if (mainHand != castingStack && apprentice_codex$hasCastAnimationOverride(player, mainHand, spell)) {
            return mainHand;
        }

        var offHand = apprentice_codex$resolveOffhandStack(player);
        if (offHand != castingStack && apprentice_codex$hasCastAnimationOverride(player, offHand, spell)) {
            return offHand;
        }

        return ItemStack.EMPTY;
    }

    @Unique
    private static ItemStack apprentice_codex$resolveOffhandStack(Player player) {
        if (ModList.get().isLoaded(apprentice_codex$BETTER_COMBAT_MOD_ID)
                && BetterCombatScrollcasterGauntletCompat.isRescueActive(player)) {
            return BetterCombatScrollcasterGauntletCompat.getPhysicalOffhandStack(player);
        }
        return player.getOffhandItem();
    }

    @Unique
    private static boolean apprentice_codex$isHandCastingSlot(String castingSlot) {
        return SpellSelectionManager.MAINHAND.equals(castingSlot) || SpellSelectionManager.OFFHAND.equals(castingSlot);
    }

    @Unique
    private static boolean apprentice_codex$shouldIgnoreFocusStaffbowFallback(Player player, String castingSlot,
                                                                              boolean focusStaffbowRightClickPresentation) {
        return !focusStaffbowRightClickPresentation
                && !SpellSelectionManager.MAINHAND.equals(castingSlot)
                && player.getMainHandItem().getItem() instanceof FocusStaffbow;
    }

    @Unique
    private static boolean apprentice_codex$shouldHandleFocusStaffbowShortcutStart(Player player, String castingSlot,
                                                                                   boolean focusStaffbowRightClickPresentation) {
        return !focusStaffbowRightClickPresentation
                && !SpellSelectionManager.MAINHAND.equals(castingSlot)
                && player.getMainHandItem().getItem() instanceof FocusStaffbow;
    }

    @Unique
    private static boolean apprentice_codex$hasCastStartAnimationOverride(Player player, ItemStack stack, @Nullable AbstractSpell spell) {
        if (!(stack.getItem() instanceof CastAnimationOverrideItem animationOverrideItem)) {
            return false;
        }

        if (stack.getItem() instanceof ChargecastCatalystbook) {
            return ChargecastCatalystbookClientCastIntent.matches(player.getUUID(), stack, spell);
        }

        return apprentice_codex$shouldSuppressCastStartAnimation(player, stack, spell)
                || animationOverrideItem.shouldOverrideCastStartAnimation(stack, spell);
    }

    @Unique
    private static boolean apprentice_codex$hasCastAnimationOverride(Player player, ItemStack stack, @Nullable AbstractSpell spell) {
        if (!(stack.getItem() instanceof CastAnimationOverrideItem animationOverrideItem)) {
            return false;
        }

        if (stack.getItem() instanceof ChargecastCatalystbook) {
            return ChargecastCatalystbookClientCastIntent.matchesActive(player.getUUID(), stack, spell);
        }

        return apprentice_codex$shouldSuppressCastStartAnimation(player, stack, spell)
                || animationOverrideItem.shouldOverrideCastStartAnimation(stack, spell)
                || animationOverrideItem.shouldOverrideCastFinishAnimation(stack, spell)
                || apprentice_codex$shouldSuppressCastFinishAnimation(player, stack, spell);
    }

    @Unique
    private static boolean apprentice_codex$shouldSuppressCastStartAnimation(Player player, ItemStack stack, @Nullable AbstractSpell spell) {
        if (!(stack.getItem() instanceof CastAnimationOverrideItem animationOverrideItem)) {
            return false;
        }

        if (stack.getItem() instanceof AbstractSwingcastStaffItem
                || stack.getItem() instanceof MithrilFreecastStaff
                || stack.getItem() instanceof RevolvercastStaff) {
            return ClientSwingcastStaffCastContext.matches(player.getUUID(), stack, spell);
        }
        if (stack.getItem() instanceof MultipurposeStaffrifle) {
            return ClientMultipurposeStaffrifleCastContext.matches(player.getUUID(), stack, spell);
        }

        return animationOverrideItem.shouldSuppressCastStartAnimation(stack, spell);
    }

    @Unique
    private static boolean apprentice_codex$shouldSuppressCastFinishAnimation(Player player, ItemStack stack, @Nullable AbstractSpell spell) {
        if (!(stack.getItem() instanceof CastAnimationOverrideItem animationOverrideItem)) {
            return false;
        }

        if (stack.getItem() instanceof AbstractSwingcastStaffItem
                || stack.getItem() instanceof MithrilFreecastStaff
                || stack.getItem() instanceof RevolvercastStaff) {
            return ClientSwingcastStaffCastContext.matches(player.getUUID(), stack, spell);
        }
        if (stack.getItem() instanceof MultipurposeStaffrifle) {
            return ClientMultipurposeStaffrifleCastContext.matches(player.getUUID(), stack, spell);
        }

        return animationOverrideItem.shouldSuppressCastFinishAnimation(stack, spell);
    }
}
