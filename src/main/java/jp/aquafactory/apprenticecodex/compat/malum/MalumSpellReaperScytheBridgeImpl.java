package jp.aquafactory.apprenticecodex.compat.malum;

import com.sammy.malum.common.item.IMalumEventResponder;
import com.sammy.malum.common.item.curiosities.weapons.scythe.MalumScytheItem;
import com.sammy.malum.core.handlers.enchantment.AscensionHandler;
import com.sammy.malum.registry.common.MalumDamageTypes;
import com.sammy.malum.registry.common.MalumParticleEffectTypes;
import com.sammy.malum.registry.common.MalumSoundEvents;
import com.sammy.malum.registry.common.enchantment.EnchantmentKeys;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import team.lodestar.lodestone.handlers.ItemEventHandler;
import team.lodestar.lodestone.helpers.SoundHelper;

import java.util.List;

final class MalumSpellReaperScytheBridgeImpl {
    private static final ResourceLocation RESPONDER_SOURCE_ID = ResourceLocation.fromNamespaceAndPath(
            ApprenticeCodex.MODID,
            "spell_reaper_scythe"
    );
    private static final IMalumEventResponder RESPONDER = new SpellReaperScytheResponder();
    private static boolean registered;

    static float throwMagicDamage(LivingEntity owner) {
        // 投擲時に保存し、後の持ち替えや手持ち弱体化でHauntedを失わない。
        var attribute = owner.getAttribute(team.lodestar.lodestone.registry.common.LodestoneAttributes.MAGIC_DAMAGE);
        return attribute == null ? 0 : (float) Math.max(0, attribute.getValue());
    }

    static float scytheProficiency(LivingEntity owner) {
        var attribute = owner.getAttribute(com.sammy.malum.registry.common.MalumAttributes.SCYTHE_PROFICIENCY);
        return attribute == null ? 1 : (float) attribute.getValue();
    }

    static int reboundLevel(Level level, ItemStack stack) {
        return EnchantmentKeys.getEnchantmentLevel(level, EnchantmentKeys.REBOUND, stack);
    }

    static boolean hasNarrowEdge(LivingEntity owner) {
        // canSweepはHidden Bladeでもfalseになるため、Narrow固有の判定を使う。
        return MalumScytheItem.isEnhanced(owner);
    }

    private MalumSpellReaperScytheBridgeImpl() {
    }

    static void register() {
        if (registered) {
            return;
        }
        registered = true;

        // Item本体をMalum型へ継承させず、本家大鎌と同じLodestoneの攻撃レスポンダー経路だけを追加する。
        ItemEventHandler.registerLookup(new ItemEventHandler.EventResponderSource(
                RESPONDER_SOURCE_ID,
                entity -> List.of(entity.getMainHandItem()),
                (entity, stack) -> stack.is(ItemRegistry.SPELL_REAPER_SCYTHE.get()) ? RESPONDER : null
        ));
    }

    static boolean shouldUseNoSweepCombo(LivingEntity attacker) {
        return !MalumScytheItem.canSweep(attacker);
    }

    static InteractionResult tryTriggerAscension(
            Level level,
            Player player,
            InteractionHand hand,
            ItemStack stack
    ) {
        var ascensionLevel = EnchantmentKeys.getEnchantmentLevel(level, EnchantmentKeys.ASCENSION, stack);
        if (ascensionLevel <= 0) {
            return InteractionResult.PASS;
        }

        var config = ApprenticeCodexServerConfig.spellReaperScytheConfig();
        var manaCost = config.ascensionManaCost(ascensionLevel);
        var creative = player.getAbilities().instabuild;
        if (level.isClientSide) {
            if (!creative && manaCost > 0 && !MalumSpellReaperScytheClientBridge.hasEnoughMana(manaCost)) {
                // CONSUMEは入力を処理済みにしつつ、SUCCESSと異なり右腕の使用モーションを要求しない。
                return InteractionResult.CONSUME;
            }

            // Malum本体と同様、跳躍だけは入力遅延を増やさないclient即時処理とする。
            // local cooldownも本家同様に予測するが、改造clientが突破できるのは移動だけに留める。
            // マナ消費・攻撃・server側cooldownの成立はserverだけが決定するため、不正耐性を下げる範囲もMalum本体相当となる。
            AscensionHandler.triggerAscension(level, player, hand, stack);
            return InteractionResult.SUCCESS;
        }

        var magicData = MagicData.getPlayerMagicData(player);
        if (!creative && manaCost > 0 && (magicData == null || magicData.getMana() < manaCost)) {
            var enchantment = level.registryAccess()
                    .lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(EnchantmentKeys.ASCENSION);
            player.displayClientMessage(Component.translatable(
                    "ui.apprenticecodex.spell_reaper_scythe.ascension_insufficient_mana",
                    Enchantment.getFullname(enchantment, ascensionLevel)
            ).withStyle(ChatFormatting.RED), true);
            return InteractionResult.CONSUME;
        }

        // 本家処理へ委譲し、Curios・Geas・damage type・cooldownの版固有仕様を重複実装しない。
        AscensionHandler.triggerAscension(level, player, hand, stack);
        if (!creative && manaCost > 0) {
            magicData.setMana(Math.max(0.0F, magicData.getMana() - manaCost));
            if (player instanceof ServerPlayer serverPlayer
                    && !(serverPlayer instanceof net.neoforged.neoforge.common.util.FakePlayer)) {
                // AscensionはIron'sの通常詠唱経路を通らないため、追加消費後のHUDを明示的に同期する。
                PacketDistributor.sendToPlayer(serverPlayer, new SyncManaPacket(magicData));
            }
        }
        return InteractionResult.SUCCESS;
    }

    private static final class SpellReaperScytheResponder implements IMalumEventResponder {
        @Override
        public void outgoingDamageEvent(
                LivingDamageEvent.Pre event,
                LivingEntity attacker,
                LivingEntity target,
                ItemStack stack
        ) {
            if (!(attacker.level() instanceof ServerLevel serverLevel)
                    || !event.getSource().is(MalumDamageTypes.SCYTHE_MELEE)) {
                return;
            }

            var particle = MalumParticleEffectTypes.SCYTHE_SLASH.createEffect()
                    .originatesFrom(attacker)
                    .targets(target)
                    .upwardOffset(-0.4F)
                    .forwardOffset(0.8F);
            MalumSpellReaperScytheParticleCompat.applyImbueSchoolColor(particle, stack);
            if (shouldUseNoSweepCombo(attacker)) {
                SoundHelper.playSound(attacker, MalumSoundEvents.SCYTHE_CUT.value(), 1.0F, 0.75F);
                particle.verticalSlashRotation().horizontalOffset(0.6F).spawn(serverLevel);
                return;
            }

            SoundHelper.playSound(attacker, MalumSoundEvents.SCYTHE_SWEEP.value(), 1.0F, 1.0F);
            particle.mirroredRandomly(attacker.getRandom()).spawn(serverLevel);
            MalumScytheItem.trySweep(attacker, target, event.getNewDamage());
        }
    }
}
