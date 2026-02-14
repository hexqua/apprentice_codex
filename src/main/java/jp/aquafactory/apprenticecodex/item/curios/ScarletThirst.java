package jp.aquafactory.apprenticecodex.item.curios;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.compat.Curios;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
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
        var entity = slotContext.entity();
        //noinspection resource
        var level = entity.level();

        if (level.isClientSide) {
            return;
        }

        ApprenticeCodex.LOGGER.debug("Equip: ScarletThirst");
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        var entity = slotContext.entity();
        //noinspection resource
        var level = entity.level();
        if (level.isClientSide){
            return;
        }

        ApprenticeCodex.LOGGER.debug("Unequip: ScarletThirst");
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        var entity = slotContext.entity();
        //noinspection resource
        var level = entity.level();
        if (level.isClientSide) {
            return;
        }

        // 体力がハート2個以下なら動かない.
        if (entity.getHealth() <= 4f) {
            return;
        }

        var magicData = MagicData.getPlayerMagicData(entity);
        if (magicData == null || !(entity instanceof ServerPlayer player)) {
            return;
        }

        var currentMana = magicData.getMana();
        var maxMana = (float) player.getAttributeValue(AttributeRegistry.MAX_MANA.get());
        var manaRetio = currentMana / maxMana;

        // クルーズ(常時稼働イメージ、弱め、1秒おき)
        if (entity.tickCount % 20 == 0 && manaRetio <= 0.5f) {
            // ハート0.5→50マナ.
            magicData.addMana(50);
            drainHealthSilentNoKill(entity, 1f);
            AudioTools.playSoundFromEntity(level, entity, SoundEvents.PLAYER_HURT_DROWN, SoundSource.PLAYERS);
        }

        // パニック(緊急時稼働イメージ、復帰優先で効率悪め、0.5秒おき)
        if (entity.tickCount % 10 == 5 && manaRetio <= 0.15f) {
            // ハート1→100マナ.
            magicData.addMana(100);
            drainHealthSilentNoKill(entity, 2f);
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

    private static void drainHealthSilentNoKill(LivingEntity entity, float amount) {
        var newHp = Math.max(1.0f, entity.getHealth() - amount);
        // todo:mixinで画面揺れを止める.
        entity.setHealth(newHp);
    }
}
