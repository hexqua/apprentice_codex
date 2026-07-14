package jp.aquafactory.apprenticecodex.spell.paletteshift;

import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.pastelstaff.PastelStaff;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class PaletteShiftReceptionEvent {
    private static final int RECEPTION_DUST_COUNT = 72;
    private static final double RECEPTION_DUST_HORIZONTAL_SPREAD = 0.75D;
    private static final double RECEPTION_DUST_VERTICAL_SPREAD = 0.6D;
    private static final float RECEPTION_DUST_SCALE = 1.25F;

    private PaletteShiftReceptionEvent() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onSpellPreCast(SpellPreCastEvent event) {
        var player = event.getEntity();
        if (!player.hasEffect(EffectRegistry.PALETTE_RECEPTION.get())) {
            return;
        }

        if (SpellRegistry.PALETTE_SHIFT.get().getSpellId().equals(event.getSpellId())) {
            return;
        }

        var schoolType = resolveCastingSchoolType(event);
        var tintColor = resolveSchoolTintColor(schoolType);
        var applied = applyPastelAffinity(player, schoolType, tintColor);
        if (!applied) {
            return;
        }

        applyReceptionEffects(player, schoolType, tintColor);
        player.removeEffect(EffectRegistry.PALETTE_RECEPTION.get());
        // PaletteReception を最優先で消費するため、対象魔法の詠唱は常に中断する。
        event.setCanceled(true);
    }

    private static SchoolType resolveCastingSchoolType(SpellPreCastEvent event) {
        // 発動トリガー側で SchoolType を拡張した場合の差し替え点。
        return event.getSchoolType();
    }

    private static boolean applyPastelAffinity(Player player, SchoolType schoolType, int tintColor) {
        var mainHand = player.getMainHandItem();
        var offHand = player.getOffhandItem();

        var applied = applyPastelAffinityToStack(mainHand, schoolType, tintColor);
        if (offHand != mainHand) {
            applied |= applyPastelAffinityToStack(offHand, schoolType, tintColor);
        }
        return applied;
    }

    private static boolean applyPastelAffinityToStack(ItemStack stack, SchoolType schoolType, int tintColor) {
        if (!PastelStaff.isPastelStaff(stack)) {
            return false;
        }

        PastelStaff.writeStoneTintColor(stack, tintColor);
        PastelStaff.writeStoneAffinitySchool(stack, schoolType);
        return true;
    }

    private static void applyReceptionEffects(Player player, SchoolType schoolType, int tintColor) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        AudioTools.playSoundFromEntity(serverLevel, player, SoundRegistry.ABSORB.get(), SoundSource.PLAYERS);

        var dust = new DustParticleOptions(toVectorColor(tintColor), RECEPTION_DUST_SCALE);
        serverLevel.sendParticles(
                dust,
                player.getX(),
                player.getY() + player.getBbHeight() * 0.5D,
                player.getZ(),
                RECEPTION_DUST_COUNT,
                RECEPTION_DUST_HORIZONTAL_SPREAD,
                RECEPTION_DUST_VERTICAL_SPREAD,
                RECEPTION_DUST_HORIZONTAL_SPREAD,
                0.02D
        );

        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(
                    Component.translatable("ui.apprenticecodex.recepted_pastel_magic", schoolType.getDisplayName()).withStyle(ChatFormatting.GREEN)
            ));
        }
    }

    private static Vector3f toVectorColor(int rgb) {
        var red = ((rgb >> 16) & 0xFF) / 255.0F;
        var green = ((rgb >> 8) & 0xFF) / 255.0F;
        var blue = (rgb & 0xFF) / 255.0F;
        return new Vector3f(red, green, blue);
    }

    private static int resolveSchoolTintColor(SchoolType schoolType) {
        var color = schoolType.getDisplayName().getStyle().getColor();
        if (color == null) {
            return PastelStaff.DEFAULT_STONE_TINT_COLOR;
        }
        return color.getValue();
    }
}
