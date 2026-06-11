package jp.aquafactory.apprenticecodex.item.curios.manathruster;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.SyncManaThrusterActivePacket;
import jp.aquafactory.apprenticecodex.particle.AdditiveGlowParticleOptions;
import jp.aquafactory.apprenticecodex.registry.ParticleRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class ManaThrusterFlightManager {
    private static final int SOUND_INTERVAL_TICKS = 10;
    private static final int SMOKE_PARTICLE_COUNT = 6;
    private static final int SPARK_PARTICLE_COUNT = 5;
    private static final double PARTICLE_FOOT_Y_OFFSET = 0.08D;
    private static final double PARTICLE_HORIZONTAL_SPREAD = 0.22D;
    private static final double PARTICLE_VERTICAL_SPREAD = 0.04D;
    private static final double PARTICLE_SPEED = 0.01D;
    private static final ResourceLocation MANA_REGEN_SUPPRESSION_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "mana_thruster_natural_mana_regen_suppression");
    private static final AttributeModifier MANA_REGEN_SUPPRESSION_MODIFIER = new AttributeModifier(
            MANA_REGEN_SUPPRESSION_MODIFIER_ID,
            -1.0D,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
    );

    private static final Map<UUID, State> STATES = new HashMap<>();

    private ManaThrusterFlightManager() {
    }

    static void onCurioTick(SlotContext slotContext) {
        var entity = slotContext.entity();
        if (entity.level().isClientSide || !(entity instanceof ServerPlayer player)) {
            return;
        }
        if (!isPrimaryEquippedCurio(slotContext)) {
            return;
        }

        tickEquippedPlayer(player);
    }

    static void onUnequip(SlotContext slotContext) {
        if (slotContext.entity() instanceof ServerPlayer player) {
            clear(player);
        }
    }

    public static void setJumpInput(ServerPlayer player, boolean active) {
        setJumpInput(player, active, 0.0F, 0.0F);
    }

    public static void setJumpInput(ServerPlayer player, boolean active, float strafeInput, float forwardInput) {
        if (ManaThrusterContext.isDisabled(player)) {
            clear(player);
            if (active) {
                syncInactive(player);
            }
            return;
        }
        if (!active) {
            var state = STATES.get(player.getUUID());
            if (state != null) {
                state.jumpInputActive = false;
                state.strafeInput = 0.0F;
                state.forwardInput = 0.0F;
            }
            return;
        }
        if (player.onGround()) {
            // ラグ時にサーバーが地上判定で拒否した場合も、クライアントの予測推進を即座に止める。
            clear(player);
            syncInactive(player);
            return;
        }

        var state = state(player);
        state.jumpInputActive = true;
        state.strafeInput = strafeInput;
        state.forwardInput = forwardInput;
    }

    public static void tickEquippedPlayer(ServerPlayer player) {
        var state = STATES.get(player.getUUID());
        if (state == null) {
            return;
        }
        if (ManaThrusterContext.isManaRecoveryFree(player)) {
            setManaRecoverySuppressed(player, state, false);
        }
        if (player.onGround() || ManaThrusterContext.isDisabled(player)) {
            clear(player);
            return;
        }
        if (!state.jumpInputActive) {
            return;
        }

        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData == null) {
            deactivateThrust(player, state);
            return;
        }
        var manaCost = Math.max(0.0F, ApprenticeCodexServerConfig.manaThrusterManaCostPerTick());
        if (magicData.getMana() + 1.0e-4F < manaCost) {
            deactivateThrust(player, state);
            return;
        }

        ManaThrusterMovement.applyThrust(player, state.strafeInput, state.forwardInput);
        player.fallDistance = 0.0F;
        setManaRecoverySuppressed(player, state, !ManaThrusterContext.isManaRecoveryFree(player));

        if (manaCost > 0.0F) {
            magicData.setMana(Math.max(0.0F, magicData.getMana() - manaCost));
            syncMana(player, magicData);
        }

        playEffects(player, state);
    }

    public static boolean isManaRecoverySuppressed(ServerPlayer player) {
        var state = STATES.get(player.getUUID());
        return state != null && state.regenSuppressedUntilLanding;
    }

    public static void clear(ServerPlayer player) {
        removeManaRegenSuppressionModifier(player);
        STATES.remove(player.getUUID());
    }

    private static void deactivateThrust(ServerPlayer player, State state) {
        state.jumpInputActive = false;
        state.strafeInput = 0.0F;
        state.forwardInput = 0.0F;
        syncInactive(player);
    }

    private static void playEffects(ServerPlayer player, State state) {
        var level = player.serverLevel();
        var gameTime = level.getGameTime();
        if (state.lastSoundGameTime == Long.MIN_VALUE || gameTime - state.lastSoundGameTime >= SOUND_INTERVAL_TICKS) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundRegistry.MANA_JET.get(), SoundSource.PLAYERS, 0.7F, 1.0F);
            state.lastSoundGameTime = gameTime;
        }

        spawnParticles(level, player);
    }

    private static void spawnParticles(ServerLevel level, ServerPlayer player) {
        var random = level.random;
        var base = footPosition(player);
        for (var i = 0; i < SMOKE_PARTICLE_COUNT; ++i) {
            var color = randomManaJetColor(random);
            level.sendParticles(
                    new DustParticleOptions(color, 0.85F + random.nextFloat() * 0.35F),
                    base.x,
                    base.y,
                    base.z,
                    1,
                    PARTICLE_HORIZONTAL_SPREAD,
                    PARTICLE_VERTICAL_SPREAD,
                    PARTICLE_HORIZONTAL_SPREAD,
                    PARTICLE_SPEED
            );
        }
        for (var i = 0; i < SPARK_PARTICLE_COUNT; ++i) {
            var color = randomManaJetColor(random);
            level.sendParticles(
                    createSparkOptions(color, random),
                    base.x,
                    base.y,
                    base.z,
                    1,
                    PARTICLE_HORIZONTAL_SPREAD,
                    PARTICLE_VERTICAL_SPREAD,
                    PARTICLE_HORIZONTAL_SPREAD,
                    PARTICLE_SPEED
            );
        }
    }

    private static Vec3 footPosition(ServerPlayer player) {
        return new Vec3(player.getX(), player.getY() + PARTICLE_FOOT_Y_OFFSET, player.getZ());
    }

    private static Vector3f randomManaJetColor(RandomSource random) {
        var t = random.nextFloat();
        var red = Mth.lerp(t, 0.28F, 0.62F);
        var green = Mth.lerp(t, 0.78F, 0.36F);
        var blue = Mth.lerp(t, 1.0F, 0.95F);
        return new Vector3f(red, green, blue);
    }

    private static AdditiveGlowParticleOptions createSparkOptions(Vector3f color, RandomSource random) {
        return new AdditiveGlowParticleOptions(
                ParticleRegistry.ADDITIVE_SPARK.get(),
                0.18F + random.nextFloat() * 0.08F,
                color.x(),
                color.y(),
                color.z(),
                2,
                8,
                3,
                0.75F,
                1.25F,
                0.7F,
                1.0F,
                0.02F,
                0.58F,
                0.45F,
                true
        );
    }

    private static void syncMana(ServerPlayer player, MagicData magicData) {
        if (!(player instanceof FakePlayer)) {
            PacketDistributor.sendToPlayer(player, new SyncManaPacket(magicData));
        }
    }

    private static void syncInactive(ServerPlayer player) {
        if (!(player instanceof FakePlayer)) {
            Networks.sendToPlayer(player, new SyncManaThrusterActivePacket(false));
        }
    }

    private static void setManaRecoverySuppressed(ServerPlayer player, State state, boolean suppressed) {
        state.regenSuppressedUntilLanding = suppressed;
        if (suppressed) {
            applyManaRegenSuppressionModifier(player);
        } else {
            removeManaRegenSuppressionModifier(player);
        }
    }

    private static void applyManaRegenSuppressionModifier(ServerPlayer player) {
        var manaRegenAttribute = player.getAttribute(AttributeRegistry.MANA_REGEN);
        if (manaRegenAttribute == null) {
            return;
        }
        if (manaRegenAttribute.getModifier(MANA_REGEN_SUPPRESSION_MODIFIER_ID) == null) {
            manaRegenAttribute.addTransientModifier(MANA_REGEN_SUPPRESSION_MODIFIER);
        }
    }

    private static void removeManaRegenSuppressionModifier(ServerPlayer player) {
        var manaRegenAttribute = player.getAttribute(AttributeRegistry.MANA_REGEN);
        if (manaRegenAttribute != null) {
            manaRegenAttribute.removeModifier(MANA_REGEN_SUPPRESSION_MODIFIER_ID);
        }
    }

    private static boolean isPrimaryEquippedCurio(SlotContext slotContext) {
        return CuriosApi.getCuriosInventory(slotContext.entity())
                .flatMap(inventory -> inventory.findFirstCurio(stack -> stack.getItem() instanceof ManaThruster))
                .map(slotResult -> slotResult.slotContext().index() == slotContext.index()
                        && slotResult.slotContext().identifier().equals(slotContext.identifier()))
                .orElse(false);
    }

    private static State state(ServerPlayer player) {
        return STATES.computeIfAbsent(player.getUUID(), ignored -> new State());
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clear(player);
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clear(player);
        }
    }

    private static final class State {
        private boolean jumpInputActive;
        private boolean regenSuppressedUntilLanding;
        private float strafeInput;
        private float forwardInput;
        private long lastSoundGameTime = Long.MIN_VALUE;
    }
}
