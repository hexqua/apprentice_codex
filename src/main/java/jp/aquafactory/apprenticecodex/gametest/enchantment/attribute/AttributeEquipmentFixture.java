package jp.aquafactory.apprenticecodex.gametest.enchantment.attribute;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.enchantment.AttributeEnchantmentType;
import jp.aquafactory.apprenticecodex.gametest.support.ConnectedPlayerTestSupport;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

final class AttributeEquipmentFixture implements AutoCloseable {
    private final GameTestHelper helper;
    private final ConnectedPlayerTestSupport.PlayerFixture fixture;
    private final Set<Holder<Attribute>> attributes;

    AttributeEquipmentFixture(GameTestHelper helper, Set<Holder<Attribute>> attributes) {
        this.helper = helper;
        this.fixture = ConnectedPlayerTestSupport.createPlayer(helper);
        this.attributes = attributes;
        try {
            for (var attribute : attributes) {
                var instance = player().getAttribute(attribute);
                helper.assertTrue(instance != null, "Missing player attribute: " + attribute);
                boolean mana = attribute.unwrapKey().orElseThrow().location().getPath().equals("max_mana");
                // 乗算の対象を非ゼロにし、割合系属性の上限へ張り付かない値で演算の違いを観測する。
                instance.setBaseValue(mana ? 200 : 0.2);
                instance.addTransientModifier(new AttributeModifier(id("flat"), mana ? 3 : 0.05,
                        AttributeModifier.Operation.ADD_VALUE));
                instance.addTransientModifier(new AttributeModifier(id("base"), 0.1,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                instance.addTransientModifier(new AttributeModifier(id("total"), 0.2,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
        } catch (RuntimeException | Error failure) {
            fixture.close();
            throw failure;
        }
    }

    ServerPlayer player() {
        return fixture.player();
    }

    void assertLevels(ItemStack plain, ItemStack levelOne, ItemStack levelTwo, EquipmentSlot slot,
                      Holder<Attribute> attribute, AttributeEnchantmentType type, String description) {
        assertLevels(plain, levelOne, levelTwo, slot, attribute, type, description, 1, 2);
    }

    void assertLevels(ItemStack plain, ItemStack levelOne, ItemStack levelTwo, EquipmentSlot slot,
                      Holder<Attribute> attribute, AttributeEnchantmentType type, String description,
                      int firstLevel, int secondLevel) {
        equip(ItemStack.EMPTY, slot);
        var unequipped = values();
        equip(plain, slot);
        var baseline = values();
        var formula = Formula.from(player().getAttribute(attribute));
        boolean additive = type == AttributeEnchantmentType.RESERVOIR;
        for (int level : new int[]{firstLevel, secondLevel, firstLevel, 0}) {
            equip(level == 0 ? plain : level == firstLevel ? levelOne : levelTwo, slot);
            for (var watched : attributes) {
                double expected = watched.equals(attribute)
                        ? formula.at(type.amountPerLevel() * level, additive) : baseline.get(watched);
                assertValue(watched, expected, description + " level=" + level);
            }
        }
        var wrongSlot = slot == EquipmentSlot.MAINHAND ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
        equip(plain, wrongSlot);
        var wrongBaseline = values();
        equip(levelTwo, wrongSlot);
        for (var watched : attributes) {
            assertValue(watched, wrongBaseline.get(watched), description + " wrong slot=" + wrongSlot);
        }
        equip(ItemStack.EMPTY, slot);
        for (var watched : attributes) {
            assertValue(watched, unequipped.get(watched), description + " removed");
        }
    }

    void assertIndependentSlots(ItemStack offhand, ItemStack curio, Holder<Attribute> attribute) {
        equipPair(offhand, curio);
        var formula = Formula.from(player().getAttribute(attribute));
        var enchantedOffhand = offhand.copy();
        var enchantedCurio = curio.copy();
        var reservoir = player().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(AttributeEnchantmentType.RESERVOIR.enchantmentKey());
        enchantedOffhand.enchant(reservoir, 1);
        enchantedCurio.enchant(reservoir, 1);
        for (int state : new int[]{3, 1, 2, 3, 0}) {
            equipPair((state & 1) != 0 ? enchantedOffhand : offhand, (state & 2) != 0 ? enchantedCurio : curio);
            assertValue(attribute, formula.at(Integer.bitCount(state)
                    * AttributeEnchantmentType.RESERVOIR.amountPerLevel(), true), "Independent offhand and Curios enchantments");
        }
    }

    private void equipPair(ItemStack offhand, ItemStack curio) {
        equip(offhand, EquipmentSlot.OFFHAND);
        CuriosApi.getCuriosInventory(player()).orElseThrow()
                .setEquippedCurio(CuriosSlotConstants.HEAD, 0, curio.copy());
        updateEquipment();
    }

    private void equip(ItemStack stack, EquipmentSlot slot) {
        for (var equipmentSlot : new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            player().setItemSlot(equipmentSlot, ItemStack.EMPTY);
        }
        var curios = CuriosApi.getCuriosInventory(player()).orElseThrow();
        curios.setEquippedCurio(CuriosSlotConstants.HEAD, 0, ItemStack.EMPTY);
        if (slot == null) {
            curios.setEquippedCurio(CuriosSlotConstants.HEAD, 0, stack.copy());
        } else {
            player().setItemSlot(slot, stack.copy());
        }
        updateEquipment();
    }

    private void updateEquipment() {
        // world未登録のplayerなので、通常のworld tickが発行する前後のイベントも再現する。
        // CuriosはPost、AccessoriesはPreで装備差分を反映するため、片側だけでは導入構成を正しく検証できない。
        var before = new EntityTickEvent.Pre(player());
        NeoForge.EVENT_BUS.post(before);
        helper.assertFalse(before.isCanceled(), "The equipment fixture must complete its entity tick");
        player().doTick();
        NeoForge.EVENT_BUS.post(new EntityTickEvent.Post(player()));
    }

    private Map<Holder<Attribute>, Double> values() {
        var result = new LinkedHashMap<Holder<Attribute>, Double>();
        for (var attribute : attributes) {
            result.put(attribute, player().getAttributeValue(attribute));
        }
        return result;
    }

    private void assertValue(Holder<Attribute> attribute, double expected, String description) {
        double actual = player().getAttributeValue(attribute);
        helper.assertTrue(Math.abs(actual - expected) < 1.0e-7,
                description + " attribute=" + attribute + " expected=" + expected + " actual=" + actual);
    }

    private static ResourceLocation id(String kind) {
        return ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "gametest_enchantment_" + kind);
    }

    @Override
    public void close() {
        fixture.close();
    }

    private record Formula(double baseAndFlat, double multipliedBase, double multipliedTotal) {
        static Formula from(AttributeInstance instance) {
            double flat = instance.getBaseValue();
            double base = 1;
            double total = 1;
            for (var modifier : instance.getModifiers()) {
                switch (modifier.operation()) {
                    case ADD_VALUE -> flat += modifier.amount();
                    case ADD_MULTIPLIED_BASE -> base += modifier.amount();
                    case ADD_MULTIPLIED_TOTAL -> total *= 1 + modifier.amount();
                }
            }
            return new Formula(flat, base, total);
        }

        double at(double amount, boolean additive) {
            return (baseAndFlat + (additive ? amount : 0))
                    * (multipliedBase + (additive ? 0 : amount)) * multipliedTotal;
        }
    }
}
