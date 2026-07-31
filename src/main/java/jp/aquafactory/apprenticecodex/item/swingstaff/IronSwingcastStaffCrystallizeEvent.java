package jp.aquafactory.apprenticecodex.item.swingstaff;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class IronSwingcastStaffCrystallizeEvent {
    private IronSwingcastStaffCrystallizeEvent() {
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Mob mob)
                || !(mob.level() instanceof ServerLevel level)
                || !(mob.getKillCredit() instanceof Player player)
                || !player.getMainHandItem().is(ItemRegistry.IRON_SWINGCAST_STAFF.get())) {
            return;
        }

        var chance = ApprenticeCodexServerConfig.ironSwingcastStaffCrystallineArcaneShardDropChance();
        if (chance <= 0.0D || level.random.nextDouble() >= chance) {
            return;
        }

        // 通常lootへ混ぜないことで、LootingやPlunderによる個数補正から独立させる。
        mob.spawnAtLocation(new ItemStack(ItemRegistry.CRYSTALLINE_ARCANE_SHARD.get()));
        level.playSound(
                null,
                mob.getX(),
                mob.getY(),
                mob.getZ(),
                SoundRegistry.VANILLA_CRYSTALLIZE_MANA.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F
        );
    }
}
