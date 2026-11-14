package jp.aquafactory.apprenticecodex.compat.magitech;

import com.google.common.collect.MapMaker;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import jp.aquafactory.apprenticecodex.item.magiengineersstaff.MagiEngineersStaff;
import jp.aquafactory.apprenticecodex.network.packet.SyncMagiEngineersStaffCastPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncMagiEngineersStaffCastPacket.Phase;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.stln.magitech.MagitechRegistries;
import net.stln.magitech.content.entity.status.AttributeInit;
import net.stln.magitech.content.item.tool.toolitem.SpellCasterItem;
import net.stln.magitech.core.api.mana.ManaCapabilities;
import net.stln.magitech.data.DataAttachmentInit;
import net.stln.magitech.feature.element.Element;
import net.stln.magitech.feature.magic.MagicPerformanceHelper;
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
    private record Active(ISpell spell, ItemStack stack, InteractionHand hand, boolean released) {
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
            default -> highestSchool(caster);
        };
    }

    private static SchoolType highestSchool(LivingEntity caster) {
        var highest = SchoolRegistry.FIRE.get();
        // マナ属性は固定の属性対応表に制限せず、アドオンが登録した系統も対象にする。
        for (var school : SchoolRegistry.REGISTRY) {
            if (school.getPowerFor(caster) > highest.getPowerFor(caster)) highest = school;
        }
        return highest;
    }

    public static int conversionManaPerSecond() {
        return (int) (CONVERSION_COST * 20 / MagiEngineersStaff.CONVERSION_INTERVAL_TICKS);
    }

    public static long conversionKiloJoulesPerSecond() {
        return CONVERSION_RECOVERY * 20 / MagiEngineersStaff.CONVERSION_INTERVAL_TICKS / 1000;
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

    public static boolean begin(Player player, ItemStack stack, InteractionHand hand) {
        if (player.getItemInHand(hand) != stack || !isStaff(stack)) return false;
        // 上流杖の失敗がPASSでも、副手へフォールバックして両方の性能を合成させない。
        if (hand == InteractionHand.OFF_HAND && player.getMainHandItem().getItem() instanceof SpellCasterItem) return false;
        var previous = ACTIVE.get(player);
        if (previous != null && !previous.released()) return false;
        ACTIVE.remove(player);
        var threadbound = CuriosHelper.getThreadBoundStack(player).orElse(ItemStack.EMPTY);
        var spells = ComponentHelper.getSpells(threadbound);
        if (spells.selected() < 0 || spells.selected() >= spells.spells().size()) return false;
        var spell = spells.getSelectedSpell();
        if (!spell.canCast(player.level(), player, stack)) return false;
        if (!player.level().isClientSide && MagicData.getPlayerMagicData(player).isCasting()) return false;
        if (spell.getConfig().continuous() || spell.getConfig().hasCharge()) {
            ACTIVE.put(player, new Active(spell, stack, hand, false));
            player.startUsingItem(hand);
        }
        // 上流payloadは手を持たないため、この杖だけ専用S2Cで発動元を明示する。
        if (!player.level().isClientSide) {
            // 消費・cooldown同期より先に開始を通知し、client側canCastで演出を拒否させない。
            sync(player, spell, stack, hand, Phase.START);
            spell.cast(player.level(), player, stack, hand, false);
        }
        return true;
    }

    public static ItemStack displayWand(Player player) {
        var active = ACTIVE.get(player);
        if (active != null && !active.released()) return active.stack();
        var main = player.getMainHandItem();
        if (main.getItem() instanceof SpellCasterItem || isStaff(main)) return main;
        return player.getOffhandItem();
    }

    private static void sync(LivingEntity caster, ISpell spell, ItemStack stack, InteractionHand hand, Phase phase) {
        int chargeTicks = phase == Phase.START
                ? Math.max(0, (int) MagicPerformanceHelper.getEffectiveChargeTime(caster, stack, spell)) : 0;
        PacketDistributor.sendToPlayersInDimension((ServerLevel) caster.level(),
                new SyncMagiEngineersStaffCastPacket(caster.getId(), spell.getId(), stack.copy(), hand, phase,
                        chargeTicks));
    }

    public static void receive(LivingEntity caster, SyncMagiEngineersStaffCastPacket packet) {
        if (!caster.level().isClientSide || !isStaff(packet.stack())) return;
        var spell = MagitechRegistries.SPELL.get(packet.spellId());
        if (spell == null) return;
        var active = ACTIVE.get(caster);
        boolean matches = active != null && active.spell() == spell && active.hand() == packet.hand()
                && ItemStack.isSameItemSameComponents(active.stack(), packet.stack());
        if (packet.phase() == Phase.START) {
            if (active != null && !matches && !active.released()) return;
            var held = caster.getItemInHand(packet.hand());
            if (!ItemStack.isSameItemSameComponents(held, packet.stack())) return;
            if (caster.isUsingItem() && (caster.getUsedItemHand() != packet.hand() || caster.getUseItem() != held)) return;
            boolean released = matches && active.released();
            if (spell.getConfig().continuous() || spell.getConfig().hasCharge()) {
                ACTIVE.put(caster, new Active(spell, held, packet.hand(), released));
                if (!released) caster.startUsingItem(packet.hand());
            }
            caster.setData(DataAttachmentInit.SPELL_CHARGE, spell.getConfig().hasCharge()
                    ? ChargeData.of(spell, packet.chargeTicks()) : ChargeData.empty());
            spell.cast(caster.level(), caster, packet.stack(), packet.hand(), false);
        } else if (matches) {
            ACTIVE.remove(caster);
            // 遅延した終了通知で、持ち替え後の別アイテムの使用を停止しない。
            if (caster.isUsingItem() && (caster.getUsedItemHand() != packet.hand()
                    || caster.getUseItem() != active.stack())) return;
            caster.setData(DataAttachmentInit.SPELL_CHARGE, packet.phase() == Phase.CANCEL
                    ? ChargeData.of(spell, 1) : ChargeData.empty());
            spell.end(caster.level(), caster, packet.stack(), packet.hand(), false);
            caster.setData(DataAttachmentInit.SPELL_CHARGE, ChargeData.empty());
        }
    }

    public static void tick(LivingEntity caster, ItemStack stack, int ticks) {
        var active = ACTIVE.get(caster);
        if (active == null || active.released() || active.stack() != stack) return;
        if (caster.getItemInHand(active.hand()) != stack) {
            cancel(caster);
            return;
        }
        if (!active.spell().canContinuousCast(caster.level(), caster, stack)) {
            caster.releaseUsingItem();
            return;
        }
        active.spell().tick(caster.level(), caster, stack, active.hand(), ticks, false);
    }

    public static void finish(LivingEntity caster) {
        if (releaseClient(caster)) return;
        var active = ACTIVE.remove(caster);
        if (active != null && !caster.level().isClientSide) {
            var phase = caster.getData(DataAttachmentInit.SPELL_CHARGE).charge().remaining() > 0 ? Phase.CANCEL : Phase.END;
            active.spell().end(caster.level(), caster, active.stack(), active.hand(), false);
            sync(caster, active.spell(), active.stack(), active.hand(), phase);
        }
    }

    public static void cancel(LivingEntity caster) {
        if (releaseClient(caster)) return;
        var active = ACTIVE.remove(caster);
        if (active != null && !caster.level().isClientSide) {
            // stopUsingItem経由の取消では、チャージ済みでも終了効果を発動させない。
            caster.setData(DataAttachmentInit.SPELL_CHARGE, ChargeData.of(active.spell(), 1));
            active.spell().end(caster.level(), caster, active.stack(), active.hand(), false);
            sync(caster, active.spell(), active.stack(), active.hand(), Phase.CANCEL);
        }
    }

    private static boolean releaseClient(LivingEntity caster) {
        if (!caster.level().isClientSide) return false;
        // 開始S2Cより先に離した場合も、通知到着時に使用状態を再開させない。
        ACTIVE.computeIfPresent(caster, (key, active) -> new Active(active.spell(), active.stack(), active.hand(), true));
        return true;
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
