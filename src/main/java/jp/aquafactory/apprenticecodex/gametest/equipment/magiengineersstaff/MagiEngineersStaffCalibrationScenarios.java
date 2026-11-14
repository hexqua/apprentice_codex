package jp.aquafactory.apprenticecodex.gametest.equipment.magiengineersstaff;

import com.mojang.authlib.GameProfile;
import jp.aquafactory.apprenticecodex.block.spellcalibrationbench.SpellCalibrationBenchMenu;
import jp.aquafactory.apprenticecodex.compat.magitech.MagiEngineersStaffCalibration;
import jp.aquafactory.apprenticecodex.gametest.crafting.support.WorkbenchMenuScope;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentTooltip;
import jp.aquafactory.apprenticecodex.item.magiengineersstaff.MagiEngineersStaff;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.stln.magitech.content.entity.magicentity.aetherix.AetherixEntity;
import net.stln.magitech.content.entity.status.AttributeInit;
import net.stln.magitech.content.item.ItemInit;
import net.stln.magitech.content.item.component.ComponentInit;
import net.stln.magitech.content.item.component.SpellComponent;
import net.stln.magitech.core.api.mana.ManaCapabilities;
import net.stln.magitech.feature.element.Element;
import net.stln.magitech.feature.magic.MagicPerformanceHelper;
import net.stln.magitech.feature.magic.spell.SpellInit;
import net.stln.magitech.feature.magic.spell.property.SpellPropertyInit;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static jp.aquafactory.apprenticecodex.gametest.support.WorkbenchTestSupport.createSpellCalibrationBenchMenuWithTarget;

final class MagiEngineersStaffCalibrationScenarios {
    private record Case(Item item, double charge, double cooldown, double launch, double efficiency) {}

    private MagiEngineersStaffCalibrationScenarios() {}

    private static List<Case> cases() {
        return List.of(new Case(ItemInit.HIGH_PURITY_FLUORITE.get(), 1, 1, 1.05, 1.15),
                new Case(ItemInit.MAGIC_CRYSTAL.get(), 0.85, 0.85, 1.5, 1.15),
                new Case(ItemInit.SURGE_CRYSTAL.get(), 1.15, 1, 1.5, 0.7),
                new Case(ItemInit.GLACE_CRYSTAL.get(), 1, 1.15, 1.5, 0.7),
                new Case(ItemInit.HOLLOW_CRYSTAL.get(), 1, 1, 1.8, 0.7));
    }

    private static FakePlayer player(GameTestHelper helper) {
        return new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "magi_calibration"));
    }

    static void storage(GameTestHelper helper) {
        var player = player(helper);
        try (var menus = new WorkbenchMenuScope()) {
            for (var test : cases()) {
                var stack = new ItemStack(ItemRegistry.MAGI_ENGINEERS_STAFF.get());
                var item = (MagiEngineersStaff) stack.getItem();
                var menu = menus.track(createSpellCalibrationBenchMenuWithTarget(player, stack), player);
                helper.assertTrue(menu.hasCalibrationTarget() && !menu.isScrollSlotEnabled(0),
                        "Staff must expose adjustments without scroll slots");
                for (int slot = 0; slot < 3; slot++) {
                    var input = new ItemStack(test.item());
                    input.set(DataComponents.CUSTOM_NAME, Component.literal("Stored calibration " + slot));
                    var target = menu.getSlot(SpellCalibrationBenchMenu.ADJUSTMENT_MENU_SLOT_START + slot);
                    helper.assertTrue(target.mayPlace(input) && target.getMaxStackSize(input) == 1,
                            "All three slots must accept repeated material with a limit of one");
                    target.set(input);
                    helper.assertTrue(ItemStack.matches(item.getCalibrationAdjustment(stack, slot), input),
                            "Menu insertion must persist the complete inserted item");
                }
                var before = stack.copy();
                helper.assertFalse(item.trySetCalibrationAdjustment(stack, 0, new ItemStack(Items.DIRT)),
                        "Unsupported material must be rejected");
                helper.assertFalse(item.trySetCalibrationAdjustment(stack, 3, new ItemStack(test.item())),
                        "A fourth slot must be rejected");
                helper.assertTrue(ItemStack.matches(stack, before), "Rejected insertions must preserve stored assets");
                var lookup = helper.getLevel().registryAccess();
                var restored = ItemStack.parse(lookup, stack.saveOptional(lookup)).orElseThrow();
                var tooltip = (CalibrationAdjustmentTooltip) item.getTooltipImage(restored).orElseThrow();
                helper.assertTrue(tooltip.items().size() == 3, "Tooltip must expose all three adjustment slots");
                for (int slot = 0; slot < 3; slot++) {
                    helper.assertTrue(ItemStack.matches(item.getCalibrationAdjustment(restored, slot),
                                    tooltip.items().get(slot)), "Restored tooltip must preserve inserted components");
                    var target = menu.getSlot(SpellCalibrationBenchMenu.ADJUSTMENT_MENU_SLOT_START + slot);
                    var removed = target.remove(1);
                    helper.assertTrue(removed.is(test.item()) && item.getCalibrationAdjustment(stack, slot).isEmpty(),
                            "Removal must return the material and clear only its stored slot");
                }
            }
        }
        helper.succeed();
    }

    static void performance(GameTestHelper helper) {
        var player = player(helper);
        var item = (MagiEngineersStaff) ItemRegistry.MAGI_ENGINEERS_STAFF.get();
        for (var test : cases()) {
            var stack = new ItemStack(item);
            assertPerformance(helper, player, stack, 1, 1, 1.5, 1);
            double damage = MagicPerformanceHelper.getOutgoingMagicDamage(player, stack, 25, 3, Element.EMBER);
            for (int slot = 0; slot < 3; slot++) {
                helper.assertTrue(item.trySetCalibrationAdjustment(stack, slot, new ItemStack(test.item())),
                        "Performance fixture must accept repeated calibration material");
                double fraction = (slot + 1) / 3.0;
                assertPerformance(helper, player, stack, 1 + (test.charge() - 1) * fraction,
                        1 + (test.cooldown() - 1) * fraction, 1.5 + (test.launch() - 1.5) * fraction,
                        1 + (test.efficiency() - 1) * fraction);
            }
            close(helper, MagicPerformanceHelper.getOutgoingMagicDamage(player, stack, 25, 3, Element.EMBER),
                    damage, "Calibration must not change outgoing damage");
            for (int slot = 0; slot < 3; slot++) item.trySetCalibrationAdjustment(stack, slot, ItemStack.EMPTY);
            assertPerformance(helper, player, stack, 1, 1, 1.5, 1);
        }
        var mixed = new ItemStack(item);
        item.trySetCalibrationAdjustment(mixed, 0, new ItemStack(ItemInit.HIGH_PURITY_FLUORITE.get()));
        item.trySetCalibrationAdjustment(mixed, 1, new ItemStack(ItemInit.MAGIC_CRYSTAL.get()));
        item.trySetCalibrationAdjustment(mixed, 2, new ItemStack(ItemInit.SURGE_CRYSTAL.get()));
        assertPerformance(helper, player, mixed, 1, 0.95, 1.35, 1);
        // 属性の演算順を区別できる値で、基礎値変更・加算・二種類の乗算を組み合わせる。
        var charge = Objects.requireNonNull(player.getAttribute(AttributeInit.CHARGE_SPEED));
        charge.setBaseValue(1.2);
        charge.addTransientModifier(new AttributeModifier(ResourceLocation.withDefaultNamespace("magi_add"),
                0.1, AttributeModifier.Operation.ADD_VALUE));
        charge.addTransientModifier(new AttributeModifier(ResourceLocation.withDefaultNamespace("magi_base"),
                0.2, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        charge.addTransientModifier(new AttributeModifier(ResourceLocation.withDefaultNamespace("magi_total"),
                0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        close(helper, MagicPerformanceHelper.getEffectiveSpellPropertyWithDivide(player, mixed, 1, 23.4F,
                AttributeInit.CHARGE_SPEED), 10, "External operations must apply after calibrated staff base");
        close(helper, MagiEngineersStaffCalibration.base(mixed, AttributeInit.CHARGE_SPEED, player.registryAccess()),
                1, "Tooltip base must exclude external attribute modifiers");
        close(helper, MagicPerformanceHelper.getEffectiveSpellPropertyWithDivide(player, new ItemStack(Items.STICK),
                1, 23.4F, AttributeInit.CHARGE_SPEED), 10, "Native spell calculations must remain unchanged");
        helper.succeed();
    }

    private static void assertPerformance(GameTestHelper helper, FakePlayer player, ItemStack stack,
                                          double charge, double cooldown, double launch, double efficiency) {
        var charged = SpellInit.FULGENZA.get();
        var instant = SpellInit.AETHERIX.get();
        var continuous = SpellInit.ESFOUNTIA.get();
        close(helper, MagicPerformanceHelper.getEffectiveCost(player, stack, instant),
                instant.getConfig().cost() / efficiency, "Calibration must affect actual spell cost");
        close(helper, MagicPerformanceHelper.getEffectiveContinuousCost(player, stack, continuous),
                continuous.getConfig().costPerTick().orElseThrow() / efficiency,
                "Calibration must affect continuous spell cost");
        close(helper, MagicPerformanceHelper.getEffectiveChargeTime(player, stack, charged),
                charged.getConfig().chargeTime().orElseThrow() / charge, "Calibration must affect charge duration");
        helper.assertTrue(MagicPerformanceHelper.getEffectiveCooldown(player, stack, instant)
                        == (int) (float) (instant.getConfig().cooldown() / cooldown),
                "Calibration must affect cooldown duration with upstream integer rounding");
        close(helper, MagicPerformanceHelper.getEffectiveProjectileSpeed(player, stack, instant),
                instant.getConfig().properties().get(SpellPropertyInit.PROJECTILE_SPEED) * launch,
                "Calibration must affect projectile launch speed");
    }

    static void casting(GameTestHelper helper) {
        var item = (MagiEngineersStaff) ItemRegistry.MAGI_ENGINEERS_STAFF.get();
        for (var hand : InteractionHand.values()) {
            var player = player(helper);
            player.setPos(helper.absoluteVec(new Vec3(2, 2, 2)));
            var tuned = new ItemStack(item);
            for (int slot = 0; slot < 3; slot++) {
                item.trySetCalibrationAdjustment(tuned, slot, new ItemStack(ItemInit.HOLLOW_CRYSTAL.get()));
            }
            var other = new ItemStack(item);
            item.trySetCalibrationAdjustment(other, 0, new ItemStack(ItemInit.HIGH_PURITY_FLUORITE.get()));
            player.setItemInHand(hand, tuned);
            player.setItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND, other);
            var book = new ItemStack(ItemInit.ARCANE_ENGINEERING_COMPENDIUM.get());
            book.set(ComponentInit.SPELL_COMPONENT, new SpellComponent(List.of(SpellInit.AETHERIX.get()), 0));
            CuriosApi.getCuriosInventory(player).orElseThrow().setEquippedCurio("threadbound", 0, book);
            var mana = Objects.requireNonNull(player.getCapability(ManaCapabilities.MANA_CAPABLE_ENTITY));
            mana.setMana(mana.getMaxMana());
            long before = mana.getMana();
            item.use(helper.getLevel(), player, hand);
            var shots = helper.getLevel().getEntitiesOfClass(AetherixEntity.class, player.getBoundingBox().inflate(2),
                    entity -> entity.getOwner() == player);
            try {
                helper.assertTrue(shots.size() == 1 && Math.abs(before - mana.getMana() - 35714) <= 1,
                        "Either hand must cast exactly once using its own calibrated mana efficiency");
                close(helper, shots.getFirst().getDeltaMovement().length(), 2.7,
                        "Projectile must use source launch 1.8 without combining the other staff");
            } finally {
                shots.forEach(AetherixEntity::discard);
            }
        }
        helper.succeed();
    }

    private static void close(GameTestHelper helper, double actual, double expected, String message) {
        helper.assertTrue(Math.abs(actual - expected) < 0.001, message + ": expected " + expected + ", got " + actual);
    }
}
