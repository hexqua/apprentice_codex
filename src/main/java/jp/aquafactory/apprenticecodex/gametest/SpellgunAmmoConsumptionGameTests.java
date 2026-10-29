package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle.FullautoRapidcastSpellrifleCastContext;
import jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle.FullautoRapidcastSpellrifleCastEvent;
import jp.aquafactory.apprenticecodex.item.multipurposestaffrifle.MultipurposeStaffrifleCastContext;
import jp.aquafactory.apprenticecodex.item.multipurposestaffrifle.MultipurposeStaffrifleCastEvent;
import jp.aquafactory.apprenticecodex.item.spellgun.SpellGunCastEvent;
import jp.aquafactory.apprenticecodex.item.spellgun.SpellgunAmmoConsumption;
import jp.aquafactory.apprenticecodex.registry.ApprenticeAttributeRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellgunAmmoConsumptionGameTests extends ApprenticeCodexGameTestScenarios {
    private SpellgunAmmoConsumptionGameTests() {
    }

    @GameTest(template = "gametest/basic_floor")
    public static void attributeOnlySavingsPreserveSpellgunAndStaffrifleRounds(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "ammo_gun_routes");
        var attribute = player.getAttribute(ApprenticeAttributeRegistry.SPELLGUN_AMMO_CONSERVATION.get());
        attribute.addTransientModifier(modifier("free_ammo", -1));
        var spell = SpellRegistry.MAGIC_MISSILE_SPELL.get();
        var gun = new ItemStack(ItemRegistry.IRON_SPELLCASTER_GUN.get());
        player.getInventory().add(new ItemStack(ItemRegistry.RAPID_SPELLCASTER_ROUND.get()));
        MagicData.getPlayerMagicData(player).setPlayerCastingItem(gun);
        SpellGunCastEvent.onSpellCast(new SpellOnCastEvent(player, spell.getSpellId(), 1,
                spell.getManaCost(1), spell.getSchoolType(), CastSource.SWORD));
        helper.assertTrue(player.getInventory().countItem(ItemRegistry.RAPID_SPELLCASTER_ROUND.get()) == 1,
                "Spellgun must preserve its round with an attribute and no hood");
        var staffrifle = new ItemStack(ItemRegistry.MULTIPURPOSE_STAFFRIFLE.get());
        player.getInventory().add(new ItemStack(ItemRegistry.MULTI_PURPOSE_SPELL_ROUND.get()));
        MagicData.getPlayerMagicData(player).setPlayerCastingItem(staffrifle);
        try (var ignored = MultipurposeStaffrifleCastContext.open(player.getUUID(), staffrifle, spell, false)) {
            var event = new SpellOnCastEvent(player, spell.getSpellId(), 1,
                    spell.getManaCost(1), spell.getSchoolType(), CastSource.SWORD);
            MultipurposeStaffrifleCastEvent.onSpellCast(event);
            helper.assertTrue(player.getInventory().countItem(ItemRegistry.MULTI_PURPOSE_SPELL_ROUND.get()) == 1
                            && event.getManaCost() == 0,
                    "Staffrifle must preserve its round with an attribute and retain its native mana bypass");
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to close staffrifle cast context", exception);
        }
        helper.assertTrue(player.getInventory().countItem(ItemRegistry.EMPTY_MULTI_PURPOSE_SPELL_CASING.get()) == 0,
                "Skipped consumption must not generate an empty casing");
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void ammoModifiersMultiplyAndHoodRefreshPreservesOtherSources(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "ammo_attribute");
        var attribute = player.getAttribute(ApprenticeAttributeRegistry.SPELLGUN_AMMO_CONSERVATION.get());
        helper.assertTrue(attribute != null, "Players must have the new ammo attribute without saved data");
        assertChance(helper, player, 1);
        helper.assertFalse(SpellgunAmmoConsumption.shouldSkip(player), "Default consumption must never be skipped");
        try (var ignored = ApprenticeCodexServerConfig.useMagiAgentSuitAmmoConfigOverrideForGameTest(0.5, true)) {
            player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_HOOD.get()));
            assertChance(helper, player, 0.5);
            attribute.addTransientModifier(modifier("first_ring", -0.5));
            assertChance(helper, player, 0.25);
            attribute.addTransientModifier(modifier("second_ring", -0.5));
            assertChance(helper, player, 0.125);
            try (var changed = ApprenticeCodexServerConfig.useMagiAgentSuitAmmoConfigOverrideForGameTest(0.2, true)) {
                assertChance(helper, player, 0.2);
                assertChance(helper, player, 0.2);
            }
            assertChance(helper, player, 0.125);
            player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            assertChance(helper, player, 0.25);
            attribute.removeModifier(id("first_ring"));
            attribute.removeModifier(id("second_ring"));
            assertChance(helper, player, 1);
            attribute.addTransientModifier(modifier("free_ammo", -1));
            helper.assertTrue(SpellgunAmmoConsumption.shouldSkip(player), "Zero consume chance must always skip");
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void legacyHoodAndPlayerDataGainTheAttributeWithoutItemMigration(GameTestHelper helper) {
        var oldPlayer = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "legacy_ammo");
        var hood = new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_HOOD.get());
        hood.setHoverName(Component.literal("Legacy hood"));
        var custom = new CompoundTag();
        custom.putString("MagiAgentSuitCalibration", "legacy_fixture");
        hood.getOrCreateTag().merge(custom);
        var lookup = helper.getLevel().registryAccess();
        var savedHood = ItemStack.of(hood.save(new CompoundTag()));
        oldPlayer.setItemSlot(EquipmentSlot.HEAD, savedHood);
        var savedPlayer = new CompoundTag();
        oldPlayer.saveWithoutId(savedPlayer);
        // 旧版には存在しないAttributeを含まないplayerデータから復元する。
        savedPlayer.remove("Attributes");
        var restored = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "restored_ammo");
        restored.load(savedPlayer);
        try (var ignored = ApprenticeCodexServerConfig.useMagiAgentSuitAmmoConfigOverrideForGameTest(0.3, true)) {
            SpellgunAmmoConsumption.onLogin(new PlayerEvent.PlayerLoggedInEvent(restored));
            assertChance(helper, restored, 0.7);
            var actualHood = restored.getItemBySlot(EquipmentSlot.HEAD);
            helper.assertTrue(actualHood.getHoverName().getString().equals("Legacy hood")
                            && actualHood.getOrCreateTag().getString("MagiAgentSuitCalibration").equals("legacy_fixture"),
                    "Updating the hood must preserve its name and calibration data");
            SpellgunAmmoConsumption.onRespawn(new PlayerEvent.PlayerRespawnEvent(restored, false));
            assertChance(helper, restored, 0.7);
            restored.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            assertChance(helper, restored, 1);
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void nonHoodAmmoAttributeUsesTheSharedRifleManaSetting(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "ammo_mana");
        var gun = new ItemStack(ItemRegistry.FULLAUTO_RAPIDCAST_SPELLRIFLE.get());
        MagicData.getPlayerMagicData(player).setPlayerCastingItem(gun);
        player.getInventory().add(new ItemStack(ItemRegistry.FULLAUTO_SPELL_CASTING_ROUND.get()));
        player.getAttribute(ApprenticeAttributeRegistry.SPELLGUN_AMMO_CONSERVATION.get())
                .addTransientModifier(modifier("free_ammo", -1));
        var spell = SpellRegistry.MAGIC_MISSILE_SPELL.get();
        for (var skipMana : new boolean[]{false, true}) {
            try (var ignored = ApprenticeCodexServerConfig.useMagiAgentSuitAmmoConfigOverrideForGameTest(0, skipMana);
                 var context = FullautoRapidcastSpellrifleCastContext.open(player.getUUID(), gun, spell, false)) {
                var event = new SpellOnCastEvent(player, spell.getSpellId(), 1, spell.getManaCost(1), spell.getSchoolType(), CastSource.SWORD);
                FullautoRapidcastSpellrifleCastEvent.onSpellCast(event);
                helper.assertTrue(event.getManaCost() == (skipMana ? 0 : spell.getManaCost(1)),
                        "Non-hood attribute sources must follow the same mana exemption setting");
                helper.assertTrue(player.getInventory().countItem(ItemRegistry.FULLAUTO_SPELL_CASTING_ROUND.get()) == 1,
                        "Attribute-only savings must preserve ammunition without a hood");
            } catch (Exception exception) {
                throw new IllegalStateException("Failed to close rifle cast context", exception);
            }
        }
        helper.succeed();
    }

    private static AttributeModifier modifier(String name, double amount) {
        return new AttributeModifier(id(name), name, amount, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    private static UUID id(String path) {
        return UUID.nameUUIDFromBytes(("apprenticecodex:gametest_" + path).getBytes(StandardCharsets.UTF_8));
    }

    private static void assertChance(GameTestHelper helper, ServerPlayer player, double expected) {
        var actual = SpellgunAmmoConsumption.consumeChance(player);
        helper.assertTrue(Math.abs(actual - expected) < 1.0E-8,
                "Ammo consume chance mismatch: expected=" + expected + " actual=" + actual);
    }
}
