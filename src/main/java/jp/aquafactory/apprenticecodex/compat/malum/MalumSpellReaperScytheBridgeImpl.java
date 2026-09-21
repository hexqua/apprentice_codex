package jp.aquafactory.apprenticecodex.compat.malum;

import com.sammy.malum.common.enchantment.scythe.AscensionEnchantment;
import com.sammy.malum.common.item.curiosities.weapons.scythe.MalumScytheItem;
import com.sammy.malum.registry.common.AttributeRegistry;
import com.sammy.malum.registry.common.item.EnchantmentRegistry;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.spellreaperscythe.SpellReaperScytheClientConfigState;
import jp.aquafactory.apprenticecodex.mixin.LivingEntityAccessor;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import team.lodestar.lodestone.helpers.CurioHelper;
import team.lodestar.lodestone.registry.common.LodestoneAttributeRegistry;


final class MalumSpellReaperScytheBridgeImpl {
    private static boolean registered;

    static float throwMagicDamage(LivingEntity owner) {
        // 投擲時に保存し、後の持ち替えや手持ち弱体化でHauntedを失わない。
        var attribute = owner.getAttribute(LodestoneAttributeRegistry.MAGIC_DAMAGE.get());
        return attribute == null ? 0 : (float) Math.max(0, attribute.getValue());
    }

    static float scytheProficiency(LivingEntity owner) {
        var attribute = owner.getAttribute(AttributeRegistry.SCYTHE_PROFICIENCY.get());
        return attribute == null ? 1 : (float) attribute.getValue();
    }

    static int reboundLevel(Level level, ItemStack stack) {
        return stack.getEnchantmentLevel(EnchantmentRegistry.REBOUND.get());
    }

    static int ascensionLevel(Level level, ItemStack stack) {
        return stack.getEnchantmentLevel(EnchantmentRegistry.ASCENSION.get());
    }

    static void triggerEpicFightAscension(Player player, ItemStack stack) {
        if (ascensionLevel(player.level(), stack) <= 0) return;
        // 攻撃・マナ・装備効果は従来経路へ委譲し、跳躍のみserverから同期する。
        if (tryTriggerAscension(player.level(), player, InteractionHand.MAIN_HAND, stack) != InteractionResult.SUCCESS) return;
        var motion = player.getDeltaMovement();
        motion = new Vec3(motion.x,
                ((LivingEntityAccessor) player).apprenticecodex$getJumpPower() * 2.0D, motion.z);
        if (player.isSprinting()) {
            float yaw = player.getYRot() * ((float) Math.PI / 180F);
            double impulse = shouldUseNoSweepCombo(player) ? -0.6D : 0.75D;
            motion = motion.add(-Math.sin(yaw) * impulse, 0, Math.cos(yaw) * impulse);
        }
        player.setDeltaMovement(motion);
        player.hasImpulse = true;
        player.hurtMarked = true;
        ForgeHooks.onLivingJump(player);
        if (player instanceof ServerPlayer server && !(player instanceof FakePlayer)) {
            server.connection.send(new ClientboundSetEntityMotionPacket(player));
        }
    }

    static boolean hasNarrowEdge(LivingEntity owner) {
        // canSweepはHidden Bladeでもfalseになるため、Narrow固有の判定を使う。
        return CurioHelper.hasCurioEquipped(owner, com.sammy.malum.registry.common.item.ItemRegistry.NECKLACE_OF_THE_NARROW_EDGE.get());
    }

    private MalumSpellReaperScytheBridgeImpl() {
    }

    static void register() {
        if (registered) {
            return;
        }
        registered = true;

        // Lodestone 1.20.1 は外部レスポンダーの登録 API を持たないため、同じ Hurt 段階で本家へ委譲する。
        MinecraftForge.EVENT_BUS.addListener(MalumSpellReaperScytheBridgeImpl::onHurt);
    }

    private static void onHurt(LivingHurtEvent event) {
        if (event.isCanceled() || event.getAmount() <= 0
                || !(event.getSource().getEntity() instanceof LivingEntity attacker)) return;
        var stack = attacker.getMainHandItem();
        if (stack.is(ItemRegistry.SPELL_REAPER_SCYTHE.get())) {
            var scythe = (MalumScytheItem)
                    com.sammy.malum.registry.common.item.ItemRegistry.SOUL_STAINED_STEEL_SCYTHE.get();
            scythe.hurtEvent(event, attacker, event.getEntity(), stack);
        }
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
        var ascensionLevel = stack.getEnchantmentLevel(EnchantmentRegistry.ASCENSION.get());
        if (ascensionLevel <= 0) {
            return InteractionResult.PASS;
        }

        if (!player.isAlive() || player.isSpectator()
                || player.getCooldowns().isOnCooldown(stack.getItem())) return InteractionResult.CONSUME;
        var config = (level.isClientSide
                ? SpellReaperScytheClientConfigState.values()
                : ApprenticeCodexServerConfig.spellReaperScytheConfig());
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
            AscensionEnchantment.triggerAscension(level, player, hand, stack);
            return InteractionResult.SUCCESS;
        }

        var magicData = MagicData.getPlayerMagicData(player);
        if (!creative && manaCost > 0 && (magicData == null || magicData.getMana() < manaCost)) {
            var enchantment = EnchantmentRegistry.ASCENSION.get();
            player.displayClientMessage(Component.translatable(
                    "ui.apprenticecodex.spell_reaper_scythe.ascension_insufficient_mana",
                    enchantment.getFullname(ascensionLevel)
            ).withStyle(ChatFormatting.RED), true);
            return InteractionResult.CONSUME;
        }

        // 本家処理へ委譲し、Curios・Geas・damage type・cooldownの版固有仕様を重複実装しない。
        AscensionEnchantment.triggerAscension(level, player, hand, stack);
        if (!creative && manaCost > 0) {
            magicData.setMana(Math.max(0.0F, magicData.getMana() - manaCost));
            if (player instanceof ServerPlayer serverPlayer
                    && !(serverPlayer instanceof FakePlayer)) {
                // AscensionはIron'sの通常詠唱経路を通らないため、追加消費後のHUDを明示的に同期する。
                PacketDistributor.sendToPlayer(serverPlayer, new SyncManaPacket(magicData));
            }
        }
        return InteractionResult.SUCCESS;
    }

}
