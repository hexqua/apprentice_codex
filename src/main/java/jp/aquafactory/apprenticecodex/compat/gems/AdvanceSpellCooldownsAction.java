package jp.aquafactory.apprenticecodex.compat.gems;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.redspace.ironsjewelry.core.actions.IAction;
import io.redspace.ironsjewelry.core.data.BonusInstance;
import io.redspace.ironsjewelry.core.data.QualityScalar;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.math.BigDecimal;

public record AdvanceSpellCooldownsAction(QualityScalar ticks) implements IAction {
    public static final MapCodec<AdvanceSpellCooldownsAction> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
            QualityScalar.CODEC.fieldOf("ticks").forGetter(AdvanceSpellCooldownsAction::ticks)
    ).apply(builder, AdvanceSpellCooldownsAction::new));

    @Override
    public void apply(ServerLevel serverLevel, double quality, boolean applyToSelf, ServerPlayer wearer, Entity entity) {
        if (!((applyToSelf ? wearer : entity) instanceof ServerPlayer target)) {
            return;
        }
        int advanceTicks = sampledTicks(quality);
        if (advanceTicks <= 0) {
            return;
        }
        var cooldowns = MagicData.getPlayerMagicData(target).getPlayerCooldowns();
        if (cooldowns.hasCooldownsActive()) {
            // 発火時点で登録済みの魔法だけを進める。終了判定と表示同期も上流の処理に合わせる。
            cooldowns.tick(advanceTicks);
            cooldowns.syncToPlayer(target);
        }
    }

    private int sampledTicks(double quality) {
        return Math.max(0, (int) ticks.sample(quality));
    }

    @Override
    public Component formatTooltip(BonusInstance bonusInstance, boolean applyToSelf) {
        var amount = Component.literal(sampledSeconds(bonusInstance.quality())).withStyle(ChatFormatting.GREEN);
        return Component.translatable("action.apprenticecodex.advance_spell_cooldowns."
                + (applyToSelf ? "self" : "entity"), amount);
    }

    private String sampledSeconds(double quality) {
        // 1tick = 0.05秒を保持し、秒未満の効果が0秒と表示されないようにする。
        return BigDecimal.valueOf(sampledTicks(quality))
                .divide(BigDecimal.valueOf(20)).stripTrailingZeros().toPlainString();
    }

    @Override
    public Component simpleDescription(MutableComponent actionName) {
        // 上流の素材説明と同様に、完成品の品質ではなく品質1の基準値を表示する。
        return Component.translatable("action.apprenticecodex.advance_spell_cooldowns.description", sampledSeconds(1));
    }

    @Override
    public MapCodec<? extends IAction> codec() {
        return CODEC;
    }
}
