package jp.aquafactory.apprenticecodex.item.curios;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.compat.Curios;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.SyncScarletThirstHealthPacket;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

public class ScarletThirst extends Item implements ICurioItem {
    final String slotIdentifier;

    public ScarletThirst() {
        super(new Item.Properties().stacksTo(1));
        slotIdentifier = Curios.RING_SLOT;
    }

    @Override
    public void onEquip(SlotContext slotContext, ItemStack prevStack, ItemStack stack) {
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        var entity = slotContext.entity();
        var level = entity.level();
        if (level.isClientSide) {
            return;
        }

        // クルーズが使える最低体力を満たさない場合は発動しない.
        if (entity.getHealth() <= 4f) {
            return;
        }

        // クルーズ/パニックともに20tickごとにのみ判定する.
        if (entity.tickCount % 20 != 0) {
            return;
        }

        var magicData = MagicData.getPlayerMagicData(entity);
        if (magicData == null || !(entity instanceof ServerPlayer player)) {
            return;
        }

        var currentMana = magicData.getMana();
        var maxMana = (float) player.getAttributeValue(AttributeRegistry.MAX_MANA.get());
        if (maxMana <= 0f) {
            return;
        }

        var manaRatio = currentMana / maxMana;

        // パニックが発動する場合はクルーズを発動させない.
        if (manaRatio <= 0.15f && entity.getHealth() > 6f) {
            magicData.addMana(150);
            drainHealthSilentNoKill(player, 4f);
            AudioTools.playSoundFromEntity(level, entity, SoundEvents.PLAYER_HURT_DROWN, SoundSource.PLAYERS);
            return;
        }

        if (manaRatio <= 0.5f) {
            magicData.addMana(50);
            drainHealthSilentNoKill(player, 1f);
            AudioTools.playSoundFromEntity(level, entity, SoundEvents.PLAYER_HURT_DROWN, SoundSource.PLAYERS);
        }
    }

    @Override
    public List<Component> getSlotsTooltip(List<Component> tooltips, ItemStack stack) {
        if (slotIdentifier != null) {
            // Curiosっぽい共通ヘッダ.
            tooltips.add(Component.empty());
            tooltips.add(Component.translatable("curios.modifiers." + this.slotIdentifier).withStyle(ChatFormatting.GOLD));

            // 本体.
            tooltips.add(Component.literal(" ").append(Component.translatable(getDescriptionId() + ".desc")).withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
        }

        return tooltips;
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return true;
    }

    private static void drainHealthSilentNoKill(ServerPlayer player, float amount) {
        var newHp = Math.max(1.0f, player.getHealth() - amount);
        player.setHealth(newHp);
        Networks.sendToPlayer(player, new SyncScarletThirstHealthPacket(newHp));
    }
}
