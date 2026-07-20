package jp.aquafactory.apprenticecodex.item.chargecastcatalystbook;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.datagen.DamageTypeTagGenerator;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class ChargecastCatalystbookCastEvents {
    private static final UUID CAST_POWER_MODIFIER_ID =
            UUID.fromString("c84b5248-7ec6-43f5-b460-52155309f74b");

    private ChargecastCatalystbookCastEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !ChargecastCatalystbook.isManagedCast(player, null)) {
            return;
        }
        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData.getCastDurationRemaining() <= 0
                || event.getSource().is(DamageTypeTagGenerator.LONG_CAST_IGNORE)
                || magicData.popMarkedPoison()
                || io.redspace.ironsspellbooks.registries.ItemRegistry.CONCENTRATION_AMULET.get().isEquippedBy(player)) {
            return;
        }
        Utils.serverSideCancelCast(player);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide
                || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData == null || !magicData.isCasting()
                || !(magicData.getPlayerCastingItem().getItem() instanceof ChargecastCatalystbook)
                || !ChargecastCatalystbook.hasWisdomShard(magicData.getPlayerCastingItem())) {
            return;
        }
        var castingSpell = magicData.getCastingSpell().getSpell();
        var internalSpell = ChargecastCatalystbook.getSelectedSpellData(magicData.getPlayerCastingItem()).getSpell();
        if (castingSpell == null || castingSpell == internalSpell) {
            return;
        }

        // Wisdom で内部選択とは別のホイール魔法を借りた場合だけ、Iron's の SpellContainer 判定では
        // 発動体を追跡できない。1.20.1 の同期スロットから元の手を解決し、持ち替え時だけ補完して中断する。
        var castingSlot = magicData.getSyncedData().getCastingEquipmentSlot();
        ItemStack heldStack = SpellSelectionManager.OFFHAND.equals(castingSlot)
                ? player.getOffhandItem()
                : SpellSelectionManager.MAINHAND.equals(castingSlot) ? player.getMainHandItem() : ItemStack.EMPTY;
        if (!ItemStack.isSameItemSameTags(heldStack, magicData.getPlayerCastingItem())) {
            Utils.serverSideCancelCast(player);
        }
    }

    public static void castWithPowerBonus(
            AbstractSpell spell,
            Level level,
            int spellLevel,
            ServerPlayer player,
            CastSource castSource,
            boolean resetRecastCount
    ) {
        if (!ChargecastCatalystbook.isManagedCast(player, spell)) {
            spell.castSpell(level, spellLevel, player, castSource, resetRecastCount);
            return;
        }
        if (ChargecastCatalystbookPresentationResolver.shouldDeferStartSound(spell)) {
            // pre-cast では server/client とも抑制したため、成功時だけサーバーから全員へ効果音を送る。
            spell.getCastStartSound().ifPresent(sound -> AudioTools.playSoundFromEntity(
                    level, player, sound, player.getSoundSource(), 2.0F, 1.0F, 0.2F
            ));
        }
        var attribute = player.getAttribute(AttributeRegistry.SPELL_POWER.get());
        if (attribute == null) {
            spell.castSpell(level, spellLevel, player, castSource, resetRecastCount);
            return;
        }
        var multiplier = ChargecastCatalystbook.resolveFinalSpellPowerMultiplier(
                player, ChargecastCatalystbook.getCastingStack(player)
        );
        attribute.removeModifier(CAST_POWER_MODIFIER_ID);
        var modifier = new AttributeModifier(
                CAST_POWER_MODIFIER_ID,
                "apprenticecodex.chargecast_catalystbook.cast_power",
                multiplier - 1.0D,
                AttributeModifier.Operation.MULTIPLY_TOTAL
        );
        attribute.addTransientModifier(modifier);
        try {
            spell.castSpell(level, spellLevel, player, castSource, resetRecastCount);
        } finally {
            attribute.removeModifier(CAST_POWER_MODIFIER_ID);
        }
    }

    public static void tickSpellUnlessChargecast(
            AbstractSpell spell,
            Level level,
            int spellLevel,
            net.minecraft.world.entity.LivingEntity caster,
            MagicData magicData
    ) {
        // 追加した待機時間中に INSTANT 魔法固有の tick 処理を走らせると、本来一度だけの効果が先行する。
        if (caster instanceof ServerPlayer player && ChargecastCatalystbook.isManagedCast(player, spell)) {
            return;
        }
        spell.onServerCastTick(level, spellLevel, caster, magicData);
    }
}
