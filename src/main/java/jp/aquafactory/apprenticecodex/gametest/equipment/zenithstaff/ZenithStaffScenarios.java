package jp.aquafactory.apprenticecodex.gametest.equipment.zenithstaff;

import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.enchantment.Enchantments;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelight;
import jp.aquafactory.apprenticecodex.item.curios.protectionspellsupporter.ProtectionSpellSupporter;
import jp.aquafactory.apprenticecodex.item.zenithstaff.ZenithStaffManaCostEvent;
import jp.aquafactory.apprenticecodex.item.zenithstaff.ZenithStaffPowerHelper;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.divinepossession.DivinePossessionPowerHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayer;

import static jp.aquafactory.apprenticecodex.gametest.equipment.enchantment.EnchantmentTestSupport.createEnchantedBook;
import static jp.aquafactory.apprenticecodex.gametest.support.CurioTestSupport.equipCurio;
import static jp.aquafactory.apprenticecodex.gametest.support.CurioTestSupport.equipRingCurio;
import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;

public final class ZenithStaffScenarios {
    private ZenithStaffScenarios() {}

    public static final ResourceLocation ZENITH_STAFF_SCHOOL_POWER_TEST_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "zenith_staff_school_power_test");

    static void zenithStaffUsesStrongestSchoolPowerAndManaPenalty(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var zenithStaffId = BuiltInRegistries.ITEM.getKey(ItemRegistry.ZENITH_STAFF.get());
            helper.assertTrue(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "zenith_staff").equals(zenithStaffId),
                    "Zenith Staff is not registered with the expected id: " + zenithStaffId);
            var zenithStack = new ItemStack(ItemRegistry.ZENITH_STAFF.get());
            var synthesis = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(Enchantments.SYNTHESIS);
            helper.assertFalse(zenithStack.getItem().supportsEnchantment(zenithStack, synthesis),
                    "Zenith Staff should not accept Synthesis at the enchanting table");
            helper.assertFalse(zenithStack.getItem().isBookEnchantable(zenithStack, createEnchantedBook(synthesis)),
                    "Zenith Staff should not accept Synthesis from enchanted books");
            helper.assertFalse(synthesis.value().canEnchant(zenithStack),
                    "Zenith Staff should not be included in the Synthesis supported_items tag");

            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "zenith_staff_power_test");
            player.setItemInHand(InteractionHand.OFF_HAND, zenithStack);

            var firePowerAttribute = player.getAttribute(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(
                    AttributeRegistry.FIRE_SPELL_POWER.get()
            ));
            helper.assertTrue(firePowerAttribute != null, "Zenith Staff test player is missing fire spell power attribute");
            firePowerAttribute.addTransientModifier(new AttributeModifier(
                    ZENITH_STAFF_SCHOOL_POWER_TEST_MODIFIER_ID,
                    0.5D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            ));

            var fireSchool = SchoolRegistry.FIRE.get();
            var iceSchool = SchoolRegistry.ICE.get();
            var snapshot = ZenithStaffPowerHelper.resolvePowerSnapshot(player);
            helper.assertTrue(snapshot.hasSchoolBonus(), "Zenith Staff should detect the fire school bonus");
            helper.assertTrue(snapshot.isStrongest(fireSchool), "Zenith Staff should treat fire as the strongest school");
            helper.assertTrue(snapshot.bonusPercent() == 50,
                    "Zenith Staff should report +50% school bonus but got " + snapshot.bonusPercent());

            var expectedPower = fireSchool.getPowerFor(player);
            var resolvedIcePower = DivinePossessionPowerHelper.resolveSchoolPower(iceSchool, player);
            helper.assertTrue(Math.abs(resolvedIcePower - expectedPower) < 1.0e-9D,
                    "Zenith Staff should cast ice with fire's strongest school power");

            try (var ignored = ApprenticeCodexServerConfig.useZenithStaffManaCostMultiplierOverrideForGameTest(3.0D)) {
                var iceManaEvent = new SpellOnCastEvent(
                        player,
                        "apprenticecodex:zenith_staff_gametest_ice",
                        1,
                        10,
                        iceSchool,
                        CastSource.SPELLBOOK
                );
                ZenithStaffManaCostEvent.onSpellCast(iceManaEvent);
                helper.assertTrue(iceManaEvent.getManaCost() == 30,
                        "Zenith Staff should triple non-strongest school mana cost");

                var fireManaEvent = new SpellOnCastEvent(
                        player,
                        "apprenticecodex:zenith_staff_gametest_fire",
                        1,
                        10,
                        fireSchool,
                        CastSource.SPELLBOOK
                );
                ZenithStaffManaCostEvent.onSpellCast(fireManaEvent);
                helper.assertTrue(fireManaEvent.getManaCost() == 10,
                        "Zenith Staff should not increase strongest school mana cost");

                var iceSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.ICE_SPIKES_SPELL.get();
                var iceBaseManaCost = iceSpell.getManaCost(1);
                var iceRequiredManaCost = ZenithStaffManaCostEvent.applyZenithManaCostMultiplier(iceBaseManaCost);
                helper.assertTrue(iceRequiredManaCost > iceBaseManaCost,
                        "Zenith Staff pre-cast test needs increased mana cost");

                var magicData = MagicData.getPlayerMagicData(player);
                helper.assertTrue(magicData != null, "Zenith Staff pre-cast test could not resolve player mana data");
                magicData.setMana(Math.max(0, iceRequiredManaCost - 1));
                var insufficientPreCastEvent = new SpellPreCastEvent(
                        player,
                        iceSpell.getSpellId(),
                        1,
                        iceSchool,
                        CastSource.SPELLBOOK
                );
                ZenithStaffManaCostEvent.onSpellPreCast(insufficientPreCastEvent);
                helper.assertTrue(insufficientPreCastEvent.isCanceled(),
                        "Zenith Staff should cancel non-strongest school pre-cast when increased mana cost is unaffordable");

                magicData.setMana(iceRequiredManaCost);
                var affordablePreCastEvent = new SpellPreCastEvent(
                        player,
                        iceSpell.getSpellId(),
                        1,
                        iceSchool,
                        CastSource.SPELLBOOK
                );
                ZenithStaffManaCostEvent.onSpellPreCast(affordablePreCastEvent);
                helper.assertFalse(affordablePreCastEvent.isCanceled(),
                        "Zenith Staff should allow non-strongest school pre-cast when increased mana cost is affordable");

                var recastSpell = SpellRegistry.ARCHER_MULTIPLE.get();
                helper.assertTrue(ZenithStaffPowerHelper.shouldIncreaseManaCost(player, recastSpell.getSchoolType()),
                        "Zenith Staff recast pre-cast test needs mana gate to apply");
                magicData.getPlayerRecasts().addRecast(new RecastInstance(
                        recastSpell.getSpellId(),
                        1,
                        2,
                        100,
                        CastSource.SPELLBOOK,
                        null
                ), magicData);
                var recastRequiredManaCost = ZenithStaffManaCostEvent.applyZenithManaCostMultiplier(recastSpell.getManaCost(1));
                magicData.setMana(Math.max(0, recastRequiredManaCost - 1));
                var recastPreCastEvent = new SpellPreCastEvent(
                        player,
                        recastSpell.getSpellId(),
                        1,
                        recastSpell.getSchoolType(),
                        CastSource.SPELLBOOK
                );
                ZenithStaffManaCostEvent.onSpellPreCast(recastPreCastEvent);
                helper.assertFalse(recastPreCastEvent.isCanceled(),
                        "Zenith Staff should not cancel active recasts with its pre-cast mana gate");

                equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));
                var touchDigSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.TOUCH_DIG.get();
                assertZenithPreCastUsesDiscountedManaGate(
                        helper,
                        player,
                        magicData,
                        touchDigSpell,
                        CraftsmansDelight.applyManaCostDiscount(touchDigSpell.getManaCost(1), player),
                        "CraftsmansDelight Touch Dig"
                );

                equipCurio(player, CuriosSlotConstants.BELT, new ItemStack(ItemRegistry.PROTECTION_SPELL_SUPPORTER.get()));
                var mysticShieldSpell = SpellRegistry.MYSTIC_SHIELD.get();
                assertZenithPreCastUsesDiscountedManaGate(
                        helper,
                        player,
                        magicData,
                        mysticShieldSpell,
                        ProtectionSpellSupporter
                                .applyManaCostDiscount(mysticShieldSpell.getManaCost(1), player),
                        "ProtectionSpellSupporter Mystic Shield"
                );

                var fireSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIREBALL_SPELL.get();
                magicData.setMana(Math.max(0, ZenithStaffManaCostEvent.applyZenithManaCostMultiplier(fireSpell.getManaCost(1)) - 1));
                var strongestPreCastEvent = new SpellPreCastEvent(
                        player,
                        fireSpell.getSpellId(),
                        1,
                        fireSchool,
                        CastSource.SPELLBOOK
                );
                ZenithStaffManaCostEvent.onSpellPreCast(strongestPreCastEvent);
                helper.assertFalse(strongestPreCastEvent.isCanceled(),
                        "Zenith Staff should not cancel strongest school pre-cast with its mana multiplier gate");

                player.addEffect(new MobEffectInstance(EffectRegistry.DIVINE_POSSESSION, 100, 0));
                var divineManaEvent = new SpellOnCastEvent(
                        player,
                        "apprenticecodex:zenith_staff_gametest_divine",
                        1,
                        10,
                        iceSchool,
                        CastSource.SPELLBOOK
                );
                ZenithStaffManaCostEvent.onSpellCast(divineManaEvent);
                helper.assertTrue(divineManaEvent.getManaCost() == 10,
                        "Divine Possession should suppress Zenith Staff's mana cost increase");

                magicData.setMana(Math.max(0, iceRequiredManaCost - 1));
                var divinePreCastEvent = new SpellPreCastEvent(
                        player,
                        iceSpell.getSpellId(),
                        1,
                        iceSchool,
                        CastSource.SPELLBOOK
                );
                ZenithStaffManaCostEvent.onSpellPreCast(divinePreCastEvent);
                helper.assertFalse(divinePreCastEvent.isCanceled(),
                        "Divine Possession should suppress Zenith Staff's pre-cast mana gate");
            }
        });
    }

    static void assertZenithPreCastUsesDiscountedManaGate(
            GameTestHelper helper,
            FakePlayer player,
            MagicData magicData,
            AbstractSpell spell,
            int discountedManaCost,
            String context
    ) {
        helper.assertTrue(ZenithStaffPowerHelper.shouldIncreaseManaCost(player, spell.getSchoolType()),
                context + " test needs Zenith Staff mana gate to apply");
        var rawRequiredManaCost = ZenithStaffManaCostEvent.applyZenithManaCostMultiplier(spell.getManaCost(1));
        var discountedRequiredManaCost = ZenithStaffManaCostEvent.applyZenithManaCostMultiplier(discountedManaCost);
        helper.assertTrue(discountedRequiredManaCost < rawRequiredManaCost,
                context + " test needs a lower discounted Zenith mana cost");

        magicData.setMana(discountedRequiredManaCost);
        var affordablePreCastEvent = new SpellPreCastEvent(
                player,
                spell.getSpellId(),
                1,
                spell.getSchoolType(),
                CastSource.SPELLBOOK
        );
        ZenithStaffManaCostEvent.onSpellPreCast(affordablePreCastEvent);
        helper.assertFalse(affordablePreCastEvent.isCanceled(),
                "Zenith Staff should allow " + context + " pre-cast at discounted increased mana cost");

        magicData.setMana(Math.max(0, discountedRequiredManaCost - 1));
        var insufficientPreCastEvent = new SpellPreCastEvent(
                player,
                spell.getSpellId(),
                1,
                spell.getSchoolType(),
                CastSource.SPELLBOOK
        );
        ZenithStaffManaCostEvent.onSpellPreCast(insufficientPreCastEvent);
        helper.assertTrue(insufficientPreCastEvent.isCanceled(),
                "Zenith Staff should still cancel " + context + " pre-cast below discounted increased mana cost");
    }
}
