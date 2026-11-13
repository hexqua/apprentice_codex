package jp.aquafactory.apprenticecodex.compat.magitech;

import com.google.common.collect.MapMaker;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import jp.aquafactory.apprenticecodex.item.magiengineersstaff.MagiEngineersStaff;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.stln.magitech.content.entity.status.AttributeInit;
import net.stln.magitech.core.api.mana.ManaCapabilities;
import net.stln.magitech.data.DataAttachmentInit;
import net.stln.magitech.feature.element.Element;
import net.stln.magitech.feature.magic.charge.ChargeData;
import net.stln.magitech.feature.magic.spell.ISpell;
import net.stln.magitech.helper.ComponentHelper;
import net.stln.magitech.helper.CuriosHelper;

import java.util.Map;

public final class MagiEngineersStaffCompat {
    // 大杖のマナ効率0.4を意識し、1kJ=内部値1000として固定量を変換する。
    private static final float CONVERSION_COST = 10;
    private static final long CONVERSION_RECOVERY = 4000;
    // 合成ツールを継承せず、選択変更・持ち替えでも開始した魔法と発動元を維持する。
    private record Active(ISpell spell, ItemStack stack) {
    }

    // Entity.equalsはID比較のため、統合サーバーで同じIDのclient/server間に発動状態を共有させない。
    // weakKeysは参照同一性で比較し、ワールド終了後のEntityも保持し続けない。
    private static final Map<LivingEntity, Active> ACTIVE = new MapMaker().weakKeys().makeMap();
    public static final Element[] ELEMENTS = {Element.MANA, Element.EMBER, Element.GLACE, Element.SURGE,
            Element.PHANTOM, Element.TREMOR, Element.MAGIC, Element.FLOW, Element.HOLLOW, Element.LOGOS};

    private MagiEngineersStaffCompat() {
    }

    public static boolean isStaff(ItemStack stack) {
        return stack != null && stack.getItem() instanceof MagiEngineersStaff;
    }

    public static SchoolType school(Element element, LivingEntity caster) {
        return switch (element) {
            case EMBER -> SchoolRegistry.FIRE.get();
            case GLACE -> SchoolRegistry.ICE.get();
            case SURGE -> SchoolRegistry.LIGHTNING.get();
            case PHANTOM -> SchoolRegistry.HOLY.get();
            case TREMOR -> SchoolRegistry.ENDER.get();
            case MAGIC -> SchoolRegistry.EVOCATION.get();
            case FLOW -> SchoolRegistry.NATURE.get();
            case HOLLOW -> SchoolRegistry.BLOOD.get();
            case LOGOS -> SchoolRegistry.ELDRITCH.get();
            default -> school(highestElement(caster), caster);
        };
    }

    public static Element highestElement(LivingEntity caster) {
        Element highest = Element.EMBER;
        for (Element element : ELEMENTS) {
            if (element != Element.MANA && element != highest
                    && school(element, caster).getPowerFor(caster) > school(highest, caster).getPowerFor(caster)) {
                highest = element;
            }
        }
        return highest;
    }

    public static double schoolPower(Element element, LivingEntity caster) {
        return school(element, caster).getPowerFor(caster);
    }

    public static float outgoing(LivingEntity caster, float damage, Element element) {
        // Iron's由来の杖性能に、Threadbound・Curios等のMagitech側補正を加算する。
        double power = projectedAttribute(caster, AttributeInit.SPELL_POWER,
                caster.getAttributeValue(AttributeRegistry.SPELL_POWER));
        double elemental = element.getPowerAttribute()
                .map(attribute -> projectedAttribute(caster, attribute, schoolPower(element, caster))).orElse(1.0);
        return (float) (damage * Math.max(0, power + elemental - 1));
    }

    public static double coefficient(LivingEntity caster, Holder<Attribute> attribute) {
        double base = attribute.equals(AttributeInit.LAUNCH) ? 1.5 : 1;
        return Math.max(0.001, projectedAttribute(caster, attribute, base));
    }

    private static double projectedAttribute(LivingEntity caster, Holder<Attribute> attribute, double staffBase) {
        var instance = caster.getAttribute(attribute);
        if (instance == null) return staffBase;
        double value = staffBase + instance.getBaseValue() - attribute.value().getDefaultValue();
        for (var modifier : instance.getModifiers()) {
            if (modifier.operation() == AttributeModifier.Operation.ADD_VALUE) value += modifier.amount();
        }
        double added = value;
        // 杖の基礎値を置換し、外部の加算・基礎乗算・最終乗算を元の順序で適用する。
        for (var modifier : instance.getModifiers()) {
            if (modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE) value += added * modifier.amount();
        }
        for (var modifier : instance.getModifiers()) {
            if (modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) value *= 1 + modifier.amount();
        }
        return Math.max(0, value);
    }

    public static boolean begin(Player player, ItemStack stack) {
        var threadbound = CuriosHelper.getThreadBoundStack(player).orElse(ItemStack.EMPTY);
        var spells = ComponentHelper.getSpells(threadbound);
        if (spells.selected() < 0 || spells.selected() >= spells.spells().size()) return false;
        var spell = spells.getSelectedSpell();
        if (!spell.canCast(player.level(), player, stack)) return false;
        if (!player.level().isClientSide && MagicData.getPlayerMagicData(player).isCasting()) return false;
        if (spell.getConfig().continuous() || spell.getConfig().hasCharge()) {
            ACTIVE.put(player, new Active(spell, stack));
            player.startUsingItem(InteractionHand.MAIN_HAND);
        }
        // 効果・消費はserverで一度だけ実行し、MagitechのS2Cで演出を配信する。
        if (!player.level().isClientSide) spell.cast(player.level(), player, stack, InteractionHand.MAIN_HAND, true);
        return true;
    }

    public static void rememberClientCast(LivingEntity caster, ISpell spell) {
        var stack = caster.getMainHandItem();
        if (caster.level().isClientSide && isStaff(stack)
                && (spell.getConfig().continuous() || spell.getConfig().hasCharge())) {
            ACTIVE.put(caster, new Active(spell, stack));
            if (!caster.isUsingItem()) caster.startUsingItem(InteractionHand.MAIN_HAND);
        }
    }

    public static void tick(LivingEntity caster, ItemStack stack, int ticks) {
        var active = ACTIVE.get(caster);
        if (active == null || active.stack() != stack) return;
        if (!active.spell().canContinuousCast(caster.level(), caster, stack)) {
            caster.releaseUsingItem();
            return;
        }
        active.spell().tick(caster.level(), caster, stack, InteractionHand.MAIN_HAND, ticks, false);
    }

    public static void finish(LivingEntity caster) {
        var active = ACTIVE.remove(caster);
        if (active != null && !caster.level().isClientSide) {
            active.spell().end(caster.level(), caster, active.stack(), InteractionHand.MAIN_HAND, true);
        }
    }

    public static void cancel(LivingEntity caster) {
        var active = ACTIVE.remove(caster);
        if (active != null && !caster.level().isClientSide) {
            // stopUsingItem経由の取消では、チャージ済みでも終了効果を発動させない。
            caster.setData(DataAttachmentInit.SPELL_CHARGE, ChargeData.of(active.spell(), 1));
            active.spell().end(caster.level(), caster, active.stack(), InteractionHand.MAIN_HAND, true);
        }
    }

    public static void convertMana(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        var handler = player.getCapability(ManaCapabilities.MANA_CAPABLE_ENTITY);
        if (handler == null || handler.getMana() >= handler.getMaxMana()) return;
        var data = MagicData.getPlayerMagicData(player);
        float before = data.getMana();
        if (!Float.isFinite(before) || before < CONVERSION_COST) return;
        data.setMana(before - CONVERSION_COST);
        if (Math.abs(data.getMana() - (before - CONVERSION_COST)) < 0.0001F) handler.addMana(CONVERSION_RECOVERY);
        PacketDistributor.sendToPlayer(serverPlayer, new SyncManaPacket(data));
    }
}
