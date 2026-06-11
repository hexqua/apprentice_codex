package jp.aquafactory.apprenticecodex.item.curios.jumpcastcharm;

import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.TriggeredSpellCastHelper;
import jp.aquafactory.apprenticecodex.item.WeaponImbueCooldownHelper;
import jp.aquafactory.apprenticecodex.item.curios.manathruster.ManaThrusterContext;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class JumpcastCharmCastManager {
    private static final CastSource JUMPCAST_SOURCE = CastSource.SWORD;
    private static final Map<UUID, ActiveJumpcast> ACTIVE_JUMPCASTS = new HashMap<>();

    private JumpcastCharmCastManager() {
    }

    public static boolean tryCast(ServerPlayer player) {
        if (isMovementContextBlocked(player)) {
            return false;
        }

        var slotResult = getEquippedCharm(player);
        if (slotResult.isEmpty()) {
            return false;
        }

        var stack = slotResult.get().stack();
        if (!(stack.getItem() instanceof JumpcastCharm charm)) {
            return false;
        }

        charm.initializeSpellContainer(stack);
        charm.normalizeImbuedSpellContainer(stack);

        var spellData = getSpellData(stack);
        if (spellData == SpellData.EMPTY || !charm.canImbueSpell(spellData)) {
            return false;
        }

        var spell = spellData.getSpell();
        var spellLevel = spell.getLevelFor(spellData.getLevel(), player);
        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData == null) {
            return false;
        }
        if (magicData.isCasting()) {
            return false;
        }

        var castResult = spell.canBeCastedBy(spellLevel, JUMPCAST_SOURCE, magicData, player);
        if (castResult.message != null) {
            sendActionBar(player, castResult.message);
        }
        if (!castResult.isSuccess()) {
            return false;
        }

        var castingSlot = createCastingSlot(slotResult.get());
        var context = new ActiveJumpcast(spell.getSpellId(), JUMPCAST_SOURCE, stack.copy(), spellLevel);
        ACTIVE_JUMPCASTS.put(player.getUUID(), context);
        try {
            var casted = spell.attemptInitiateCast(
                    stack,
                    spellLevel,
                    player.level(),
                    player,
                    JUMPCAST_SOURCE,
                    true,
                    castingSlot
            );
            if (!casted) {
                return false;
            }

            if (spell.getCastType() == CastType.INSTANT) {
                completeInstantCastImmediately(player, spellLevel, spell, magicData);
            } else {
                TriggeredSpellCastHelper.applyLongCastDurationOverride(
                        player,
                        spellLevel,
                        spell,
                        magicData,
                        castingSlot,
                        spell.getCastType() == CastType.LONG ? 0 : null
                );
            }
            return true;
        } catch (RuntimeException exception) {
            ApprenticeCodex.LOGGER.warn(
                    "Failed to cast jumpcast charm spell {} for player {}",
                    spell.getSpellId(),
                    player.getGameProfile().getName(),
                    exception
            );
            return false;
        } finally {
            ACTIVE_JUMPCASTS.remove(player.getUUID());
        }
    }

    public static boolean isMovementContextBlocked(Player player) {
        return player.onGround()
                || ManaThrusterContext.isDisabled(player)
                || player.isInWaterOrBubble()
                || player.level().getFluidState(player.blockPosition()).is(FluidTags.WATER)
                || player.isSwimming();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onSpellCooldownAdded(SpellCooldownAddedEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        var context = ACTIVE_JUMPCASTS.get(player.getUUID());
        if (context == null || !context.matches(event.getSpell().getSpellId(), event.getCastSource())) {
            return;
        }

        event.setEffectiveCooldown(
                WeaponImbueCooldownHelper.getEffectiveSpellCooldown(
                        event.getSpell(),
                        player,
                        event.getCastSource(),
                        context.castingStack()
                ) + context.longCastExtensionTicks(player, event.getSpell().getCastType())
        );
    }

    private static Optional<SlotResult> getEquippedCharm(ServerPlayer player) {
        return CuriosApi.getCuriosInventory(player)
                .flatMap(inventory -> inventory.findFirstCurio(stack -> stack.getItem() instanceof JumpcastCharm));
    }

    private static SpellData getSpellData(ItemStack stack) {
        var spellContainer = ISpellContainer.get(stack);
        if (spellContainer == null || spellContainer.getMaxSpellCount() <= 0) {
            return SpellData.EMPTY;
        }

        var spellData = spellContainer.getSpellAtIndex(0);
        //noinspection ConstantValue
        return spellData == null ? SpellData.EMPTY : spellData;
    }

    private static String createCastingSlot(SlotResult slotResult) {
        return "jumpcast_charm_"
                + slotResult.slotContext().identifier()
                + "_"
                + slotResult.slotContext().index();
    }

    private static void sendActionBar(ServerPlayer player, net.minecraft.network.chat.Component message) {
        if (player.connection != null) {
            player.connection.send(new ClientboundSetActionBarTextPacket(message));
        }
    }

    private static void completeInstantCastImmediately(
            ServerPlayer player,
            int spellLevel,
            io.redspace.ironsspellbooks.api.spells.AbstractSpell spell,
            MagicData magicData
    ) {
        // MagicManager の次 tick を待たず、空中ジャンプ入力のサーバー処理内で INSTANT を発動し切る。
        spell.castSpell(player.level(), spellLevel, player, magicData.getCastSource(), true);
        spell.onServerCastComplete(player.level(), spellLevel, player, magicData, false);
    }

    private record ActiveJumpcast(
            String spellId,
            CastSource castSource,
            ItemStack castingStack,
            int spellLevel
    ) {
        private boolean matches(String spellId, CastSource castSource) {
            return this.spellId.equals(spellId) && this.castSource == castSource;
        }

        private int longCastExtensionTicks(ServerPlayer player, CastType castType) {
            if (castType != CastType.LONG) {
                return 0;
            }

            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.getSpell(spellId);
            if (spell == null || spell == io.redspace.ironsspellbooks.api.registry.SpellRegistry.none()) {
                return 0;
            }
            return Math.max(0, spell.getEffectiveCastTime(spellLevel, player));
        }
    }
}
