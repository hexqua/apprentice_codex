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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class ChargecastCatalystbookCastEvents {
    private static final ResourceLocation CAST_POWER_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(
            ApprenticeCodex.MODID,
            "chargecast_catalystbook_cast_power"
    );

    private ChargecastCatalystbookCastEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingAttack(LivingIncomingDamageEvent event) {
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
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
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
        if (!ItemStack.isSameItemSameComponents(heldStack, magicData.getPlayerCastingItem())) {
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
        var attribute = player.getAttribute(AttributeRegistry.SPELL_POWER);
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
                multiplier - 1.0D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
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
