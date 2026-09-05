package jp.aquafactory.apprenticecodex.item.spellreaperscythe;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class ScytheThrowManager {
    private static final String TOKEN = "apprenticecodex_scythe_throw";
    private static final Map<UUID, ScytheThrowEntity> ACTIVE = new HashMap<>();
    private static final Map<UUID, Charge> CHARGES = new HashMap<>();

    private ScytheThrowManager() {}

    public static UUID token(ItemStack stack) {
        var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return data.hasUUID(TOKEN) ? data.getUUID(TOKEN) : null;
    }

    public static boolean isThrown(ItemStack stack) {
        return stack.getItem() instanceof SpellReaperScythe && token(stack) != null;
    }

    static void mark(ItemStack stack, UUID token) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putUUID(TOKEN, token));
    }

    static void clear(ItemStack stack) {
        if (token(stack) == null) return;
        var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        data.remove(TOKEN);
        if (data.isEmpty()) stack.remove(DataComponents.CUSTOM_DATA);
        else stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
    }

    public static ScytheThrowEntity active(Player player) {
        var entity = ACTIVE.get(player.getUUID());
        return entity != null && !entity.isRemoved() && entity.level() == player.level() ? entity : null;
    }

    public static InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.fail(stack);
        if (level.isClientSide) {
            return ScytheThrowClient.use(player, stack);
        }
        var active = active(player);
        if (active != null) {
            active.recall();
            return InteractionResultHolder.consume(stack);
        }
        clear(stack);
        // client側で不足拒否した使用packetも届き得るため、新しい開始要求は時刻を更新する。
        // 再送で溜めを短縮することはできず、古いserver使用状態を次の正規操作へ持ち越さない。
        CHARGES.put(player.getUUID(), new Charge(level.getGameTime(), stack, player.getInventory().selected));
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    public static void release(Level level, Player player, ItemStack stack) {
        if (level.isClientSide) return;
        var charge = CHARGES.remove(player.getUUID());
        if (charge == null || charge.stack != stack || player.getMainHandItem() != stack
                || player.getInventory().selected != charge.slot || active(player) != null) return;
        var elapsed = level.getGameTime() - charge.started;
        if (elapsed < 10) return;
        var cost = ApprenticeCodexServerConfig.spellReaperScytheConfig().throwManaCost();
        if (!canPay(player, cost)) {
            insufficientMana(player);
            return;
        }
        var entity = new ScytheThrowEntity(EntityRegistry.SCYTHE_THROW.get(), level);
        entity.prepare(player, stack, charge.slot, Math.min(40, elapsed) * 0.25);
        if (!level.addFreshEntity(entity)) return;
        ACTIVE.put(player.getUUID(), entity);
        mark(stack, entity.getUUID());
        pay(player, cost);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                jp.aquafactory.apprenticecodex.registry.SoundRegistry.SCYTHE_THROW.get(),
                net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1f);
    }

    public static boolean canPay(Player player, int cost) {
        if (player.getAbilities().instabuild || cost == 0) return true;
        var data = MagicData.getPlayerMagicData(player);
        return data != null && Float.isFinite(data.getMana()) && data.getMana() >= cost;
    }

    public static void pay(Player player, int cost) {
        if (cost == 0 || player.getAbilities().instabuild) return;
        var data = MagicData.getPlayerMagicData(player);
        data.setMana(Math.max(0, data.getMana() - cost));
        if (player instanceof ServerPlayer server && !(server instanceof net.neoforged.neoforge.common.util.FakePlayer)) {
            PacketDistributor.sendToPlayer(server, new SyncManaPacket(data));
        }
    }

    public static void insufficientMana(Player player) {
        player.displayClientMessage(Component.translatable("ui.apprenticecodex.spell_reaper_scythe.throw_insufficient_mana",
                        player.getMainHandItem().getHoverName())
                .withStyle(ChatFormatting.RED), true);
    }

    static void forget(ScytheThrowEntity entity, Player owner, ItemStack original) {
        if (owner != null) {
            ACTIVE.remove(owner.getUUID(), entity);
            for (var stack : owner.getInventory().items) clearMatching(stack, entity.getUUID());
            clearMatching(owner.getOffhandItem(), entity.getUUID());
        }
        clearMatching(original, entity.getUUID());
    }

    private static void clearMatching(ItemStack stack, UUID id) {
        if (id.equals(token(stack))) clear(stack);
    }

    public static void sanitize(ItemStack stack, Player holder) {
        var id = token(stack);
        if (id == null) return;
        var entity = active(holder);
        if (entity == null || !id.equals(entity.getUUID())) clear(stack);
    }

    @SubscribeEvent
    public static void attributes(ItemAttributeModifierEvent event) {
        if (!isThrown(event.getItemStack())) return;
        event.removeModifier(Attributes.ATTACK_DAMAGE, Item.BASE_ATTACK_DAMAGE_ID);
        event.removeModifier(Attributes.ATTACK_SPEED, Item.BASE_ATTACK_SPEED_ID);
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        var player = event.getEntity();
        if (player.level().isClientSide) return;
        var entity = active(player);
        if (entity != null) entity.checkOwner();
        if (!player.isUsingItem()) CHARGES.remove(player.getUUID());
    }

    private static void abort(Player player) {
        CHARGES.remove(player.getUUID());
        var entity = ACTIVE.remove(player.getUUID());
        if (entity != null) entity.discard();
    }

    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { abort(event.getEntity()); }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) { abort(event.getEntity()); }
    @SubscribeEvent public static void clone(PlayerEvent.Clone event) { abort(event.getOriginal()); }
    @SubscribeEvent public static void stopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {
        ACTIVE.clear();
        CHARGES.clear();
    }

    private record Charge(long started, ItemStack stack, int slot) {}
}
