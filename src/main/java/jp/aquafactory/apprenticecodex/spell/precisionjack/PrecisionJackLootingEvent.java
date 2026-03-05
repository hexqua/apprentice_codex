package jp.aquafactory.apprenticecodex.spell.precisionjack;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class PrecisionJackLootingEvent {
    private PrecisionJackLootingEvent() {
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        var source = event.getSource();
        if (!source.is(DamageTypes.PRECISION_JACK)) {
            return;
        }

        if (event.getEntity() instanceof Player) {
            return;
        }

        if (!(source.getDirectEntity() instanceof PrecisionJackKnifeEntity knife)) {
            return;
        }

        if (isDuplicateDropTriggered(event, knife) && appendAdditionalLootRoll(event)) {
            notifyDuplicateDropSuccess(knife);
        }

        var lootingBonus = Math.min(5, knife.getLootingBonus());
        if (lootingBonus <= 0) {
            return;
        }

        var random = event.getEntity().getRandom();
        for (var drop : event.getDrops()) {
            ItemStack itemStack = drop.getItem();
            if (itemStack.isEmpty()) {
                continue;
            }

            var extra = random.nextInt(lootingBonus + 1);
            if (extra > 0) {
                itemStack.grow(extra);
            }
        }
    }

    private static boolean isDuplicateDropTriggered(LivingDropsEvent event, PrecisionJackKnifeEntity knife) {
        var chancePercent = Math.min(30, knife.getDuplicateDropChancePercent());
        if (chancePercent <= 0) {
            return false;
        }

        return event.getEntity().getRandom().nextInt(100) < chancePercent;
    }

    private static boolean appendAdditionalLootRoll(LivingDropsEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel serverLevel)) {
            return false;
        }

        var target = event.getEntity();
        var source = event.getSource();
        var lootTable = serverLevel.getServer().reloadableRegistries().getLootTable(target.getLootTable());
        var hasAdditionalDrop = new boolean[]{false};

        var lootParamsBuilder = new LootParams.Builder(serverLevel)
                .withParameter(LootContextParams.THIS_ENTITY, target)
                .withParameter(LootContextParams.ORIGIN, target.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, source);

        if (event.isRecentlyHit() && target.getKillCredit() instanceof Player player) {
            lootParamsBuilder = lootParamsBuilder
                    .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, player)
                    .withLuck(player.getLuck());
        }

        var lootParams = lootParamsBuilder.create(LootContextParamSets.ENTITY);
        lootTable.getRandomItems(lootParams, itemStack -> {
            if (itemStack.isEmpty()) {
                return;
            }

            hasAdditionalDrop[0] = true;
            event.getDrops().add(new ItemEntity(serverLevel, target.getX(), target.getY(), target.getZ(), itemStack));
        });

        return hasAdditionalDrop[0];
    }

    private static void notifyDuplicateDropSuccess(PrecisionJackKnifeEntity knife) {
        if (knife.getOwner() instanceof ServerPlayer serverPlayer) {
            serverPlayer.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.0f);
        }
    }
}
