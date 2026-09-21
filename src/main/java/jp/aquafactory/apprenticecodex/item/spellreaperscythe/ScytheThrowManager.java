package jp.aquafactory.apprenticecodex.item.spellreaperscythe;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.malum.MalumSpellReaperScytheBridge;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.TickEvent;

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
        var data = stack.getTag();
        return data != null && data.hasUUID(TOKEN) ? data.getUUID(TOKEN) : null;
    }

    public static boolean isThrown(ItemStack stack) {
        return stack.getItem() instanceof SpellReaperScythe && token(stack) != null;
    }

    static void mark(ItemStack stack, UUID token) {
        stack.getOrCreateTag().putUUID(TOKEN, token);
    }

    static void clear(ItemStack stack) {
        if (token(stack) == null) return;
        var data = stack.getOrCreateTag();
        data.remove(TOKEN);
        if (data.isEmpty()) stack.setTag(null);
    }

    public static ScytheThrowEntity active(Player player) {
        var entity = ACTIVE.get(player.getUUID());
        return entity != null && !entity.isRemoved() && entity.level() == player.level() ? entity : null;
    }

    public static InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND || !(stack.getItem() instanceof SpellReaperScythe)
                || !player.isAlive() || player.isSpectator()) return InteractionResultHolder.fail(stack);
        if (level.isClientSide) {
            return ScytheThrowClient.use(player, stack);
        }
        var active = active(player);
        if (active != null) {
            active.recall();
            return InteractionResultHolder.consume(stack);
        }
        clear(stack);
        int reboundLevel = MalumSpellReaperScytheBridge.reboundLevel(level, stack);
        if (reboundLevel > 0) {
            CHARGES.remove(player.getUUID());
            player.stopUsingItem();
            launchRebound(player, stack);
            return InteractionResultHolder.consume(stack);
        }
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
        if (elapsed < 10 || !player.isAlive() || player.isSpectator()
                || MalumSpellReaperScytheBridge.reboundLevel(level, stack) > 0) return;
        var cost = ApprenticeCodexServerConfig.spellReaperScytheConfig().throwManaCost();
        launch(level, player, stack, charge.slot, Math.min(40, elapsed) * 0.25, ScytheThrowEntity.Mode.NORMAL, cost);
    }

    public static boolean recall(Player player) {
        var entity = active(player);
        if (entity == null) return false;
        entity.recall();
        return true;
    }

    public static void launchNormal(Player player, ItemStack stack, double distance) {
        if (!Double.isFinite(distance)) return;
        launch(player.level(), player, stack, player.getInventory().selected,
                Mth.clamp(distance, 2.5D, 10.0D), ScytheThrowEntity.Mode.NORMAL,
                ApprenticeCodexServerConfig.spellReaperScytheConfig().throwManaCost());
    }

    public static void launchRebound(Player player, ItemStack stack) {
        int reboundLevel = MalumSpellReaperScytheBridge.reboundLevel(player.level(), stack);
        if (reboundLevel <= 0) return;
        var mode = MalumSpellReaperScytheBridge.hasNarrowEdge(player)
                ? ScytheThrowEntity.Mode.NARROW : ScytheThrowEntity.Mode.REBOUND;
        var config = ApprenticeCodexServerConfig.spellReaperScytheConfig();
        launch(player.level(), player, stack, player.getInventory().selected,
                mode == ScytheThrowEntity.Mode.NARROW ? 32 : 16, mode,
                config.reboundManaCost(reboundLevel));
    }

    private static void launch(Level level, Player player, ItemStack stack, int slot, double distance,
                               ScytheThrowEntity.Mode mode, int cost) {
        if (level.isClientSide || !player.isAlive() || player.isSpectator()
                || !(stack.getItem() instanceof SpellReaperScythe) || player.getMainHandItem() != stack
                || player.getInventory().selected != slot || active(player) != null) return;
        if (!canPay(player, cost)) {
            insufficientMana(player);
            return;
        }
        var entity = new ScytheThrowEntity(EntityRegistry.SCYTHE_THROW.get(), level);
        entity.prepare(player, stack, slot, distance, mode);
        if (!level.addFreshEntity(entity)) return;
        ACTIVE.put(player.getUUID(), entity);
        mark(stack, entity.getUUID());
        pay(player, cost);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundRegistry.SCYTHE_THROW.get(),
                SoundSource.PLAYERS, 0.8f, 1f);
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
        if (player instanceof ServerPlayer server && !(server instanceof FakePlayer)) {
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
        event.getModifiers().get(Attributes.ATTACK_DAMAGE).stream()
                .filter(modifier -> modifier.getId().equals(SpellReaperScythe.BASE_DAMAGE_ID)).toList()
                .forEach(modifier -> event.removeModifier(Attributes.ATTACK_DAMAGE, modifier));
        event.getModifiers().get(Attributes.ATTACK_SPEED).stream()
                .filter(modifier -> modifier.getId().equals(SpellReaperScythe.BASE_SPEED_ID)).toList()
                .forEach(modifier -> event.removeModifier(Attributes.ATTACK_SPEED, modifier));
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var player = event.player;
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
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) {
        ACTIVE.clear();
        CHARGES.clear();
    }

    private record Charge(long started, ItemStack stack, int slot) {}
}
