package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.item.curios.AffinityData;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.enchantment.Enchantments;
import jp.aquafactory.apprenticecodex.item.WeaponImbueCooldownHelper;
import jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmulet;
import jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletAutoCastEvent;
import jp.aquafactory.apprenticecodex.item.curios.quickcastscrollcartridge.QuickcastCartridgeCasting;
import jp.aquafactory.apprenticecodex.item.curios.quickcastscrollcartridge.QuickcastScrollCartridge;
import jp.aquafactory.apprenticecodex.item.curios.satellitefollowcastamulet.SatelliteFollowcastAmulet;
import jp.aquafactory.apprenticecodex.item.curios.satellitefollowcastamulet.SatelliteFollowcastAmuletCastEvent;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerContinuousCastManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class AutomaticScrollTranscendenceGameTests {
    private AutomaticScrollTranscendenceGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void anvilAndStoredScrolls(GameTestHelper helper) {
        var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(helper,
                new BlockPos(0, 2, 0), "automatic_scroll_anvil");
        var enchantment = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.TRANSCENDENCE);
        var spell = SpellRegistry.MAGIC_MISSILE_SPELL.get();
        try {
            for (var item : List.of(ItemRegistry.QUICKCAST_SCROLL_CARTRIDGE.get(),
                    ItemRegistry.AUTOCAST_AMULET.get(), ItemRegistry.SATELLITE_FOLLOWCAST_AMULET.get())) {
                var menu = new AnvilMenu(0, player.getInventory());
                var book = new ItemStack(Items.ENCHANTED_BOOK);
                book.enchant(enchantment, 1);
                menu.getSlot(0).set(new ItemStack(item));
                menu.getSlot(1).set(book);
                menu.createResult();
                helper.assertTrue(menu.getSlot(2).getItem().getEnchantmentLevel(enchantment) == 1,
                        "Anvil must accept Transcendence for " + item);
                for (int level : new int[]{0, 1, 3, 10}) {
                    var stack = new ItemStack(item);
                    if (level > 0) stack.enchant(enchantment, level);
                    var scroll = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get());
                    ISpellContainer.createScrollContainer(spell, spell.getMaxLevel(), scroll);
                    if (item instanceof QuickcastScrollCartridge) QuickcastScrollCartridge.setCalibrationScroll(stack, 0, scroll);
                    else if (item instanceof AutocastAmulet) AutocastAmulet.setCalibrationScroll(stack, 0, scroll);
                    else SatelliteFollowcastAmulet.setCalibrationScroll(stack, 0, scroll);
                    var snapshot = stack.copy();
                    for (int repeat = 0; repeat < 2; repeat++) {
                        var data = item instanceof QuickcastScrollCartridge
                                ? QuickcastScrollCartridge.getResolvedSelectedSpellData(stack)
                                : item instanceof AutocastAmulet ? AutocastAmulet.getResolvedSpellDataAt(stack, 0)
                                : SatelliteFollowcastAmulet.getResolvedSpellDataAt(stack, 0);
                        helper.assertTrue(data.getLevel() == spell.getMaxLevel() + (level > 0 ? 1 : 0),
                                "Every positive enchantment level must grant exactly one level above the cap");
                    }
                    helper.assertTrue(ItemStack.isSameItemSameComponents(snapshot, stack),
                            "Resolving spells must preserve stored scrolls and enchantment levels");
                    var restored = ItemStack.parseOptional(player.registryAccess(), (CompoundTag) stack.saveOptional(player.registryAccess()));
                    var stored = item instanceof QuickcastScrollCartridge ? QuickcastScrollCartridge.getCalibrationScroll(restored, 0)
                            : item instanceof AutocastAmulet ? AutocastAmulet.getCalibrationScroll(restored, 0)
                            : SatelliteFollowcastAmulet.getCalibrationScroll(restored, 0);
                    helper.assertTrue(ItemStack.isSameItemSameComponents(scroll, stored),
                            "Reloaded and extracted scrolls must retain their original level");
                }
            }
        } finally {
            player.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void cartridgeAndAutocastUseResolvedLevels(GameTestHelper helper) {
        var spell = SpellRegistry.MAGIC_MISSILE_SPELL.get();
        for (int route = 0; route < 4; route++) {
            var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(helper,
                    new BlockPos(0, 2, 0), "automatic_scroll_cast_" + route);
            var magic = MagicData.getPlayerMagicData(player);
            magic.setMana(10000);
            magic.getSyncedData().learnSpell(spell, false);
            var curios = CuriosApi.getCuriosInventory(player).orElseThrow();
            var ring = new ItemStack(ItemRegistry.ENCHANTED_CIRCLET.get());
            AffinityData.setAffinityData(ring, spell, 2);
            curios.setEquippedCurio("head", 0, ring);
            var stack = new ItemStack(route == 3 ? ItemRegistry.AUTOCAST_AMULET.get() : ItemRegistry.QUICKCAST_SCROLL_CARTRIDGE.get());
            stack.enchant(player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.TRANSCENDENCE), 3);
            var scroll = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get());
            ISpellContainer.createScrollContainer(spell, 1, scroll);
            try {
                if (route == 3) {
                    AutocastAmulet.setCalibrationScroll(stack, 0, scroll);
                    curios.setEquippedCurio("necklace", 0, stack);
                    player.tickCount = 20;
                    AutocastAmuletAutoCastEvent.onPlayerTick(new PlayerTickEvent.Post(player));
                } else {
                    QuickcastScrollCartridge.setCalibrationScroll(stack, 0, scroll);
                    curios.setEquippedCurio("back", 0, stack);
                    var selection = new SpellSelectionManager(player).getSpellsForSlot(QuickcastCartridgeCasting.SLOT).getFirst();
                    helper.assertTrue(selection.spellData.getLevel() == 2
                                    && QuickcastScrollCartridge.getScrollTooltipData(stack).selectedSpell().getLevel() == 2,
                            "Wheel and tooltip must include only the internal bonus");
                    magic.getPlayerCooldowns().addCooldown(spell, 100, 50);
                    boolean started = route == 0 ? QuickcastCartridgeCasting.initiate(player)
                            : route == 1 ? Utils.serverSideInitiateQuickCast(player, selection.globalIndex)
                            : Utils.serverSideInitiateCast(player);
                    helper.assertTrue(started, "All cartridge routes must bypass cooldown using a charge");
                }
                helper.assertTrue(magic.isCasting() && magic.getCastingSpellLevel() == 4,
                        "Actual casting level must include Transcendence and Affinity exactly once");
            } finally {
                Utils.serverSideCancelCast(player);
                QuickcastCartridgeCasting.validate(player);
                player.discard();
            }
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void satelliteReservesManaAtResolvedLevel(GameTestHelper helper) {
        for (var spell : List.of(SpellRegistry.MAGIC_MISSILE_SPELL.get(), SpellRegistry.FIREBALL_SPELL.get(),
                SpellRegistry.FIRE_BREATH_SPELL.get())) {
            var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(helper,
                    new BlockPos(0, 2, 0), "satellite_resolved_mana");
            player.getAttribute(AttributeRegistry.MAX_MANA).setBaseValue(10000);
            var magic = MagicData.getPlayerMagicData(player);
            magic.getSyncedData().learnSpell(spell, false);
            var stack = new ItemStack(ItemRegistry.SATELLITE_FOLLOWCAST_AMULET.get());
            stack.enchant(player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.TRANSCENDENCE), 3);
            var scroll = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get());
            ISpellContainer.createScrollContainer(spell, 1, scroll);
            SatelliteFollowcastAmulet.setCalibrationScroll(stack, 0, scroll);
            SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(stack, 0,
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get()));
            var curios = CuriosApi.getCuriosInventory(player).orElseThrow();
            curios.setEquippedCurio("necklace", 0, stack);
            var ring = new ItemStack(ItemRegistry.ENCHANTED_CIRCLET.get());
            AffinityData.setAffinityData(ring, spell, 2);
            curios.setEquippedCurio("head", 0, ring);
            var trigger = jp.aquafactory.apprenticecodex.registry.SpellRegistry.MAGE_LIGHT.get();
            var event = new SpellOnCastEvent(player, trigger.getSpellId(), 1, 30, trigger.getSchoolType(), CastSource.SPELLBOOK);
            try {
                int cost = spell.getManaCost(4);
                magic.setMana(cost + 29);
                SatelliteFollowcastAmuletCastEvent.onSpellCast(event);
                helper.assertTrue(magic.getMana() == cost + 29 && !magic.getPlayerCooldowns().isOnCooldown(spell),
                        "Followcast must reserve original mana at its resolved spell level: " + spell.getSpellId()
                                + " expected=" + (cost + 29) + " actual=" + magic.getMana()
                                + " resolved=" + spell.getLevelFor(2, player));
                magic.setMana(cost + 30);
                SatelliteFollowcastAmuletCastEvent.onSpellCast(event);
                helper.assertTrue(magic.getMana() == 30,
                        "Remote execution must consume mana for the boosted level exactly once");
                if (spell.getCastType() == CastType.CONTINUOUS) {
                    helper.assertTrue(SatelliteFollowcastAmuletCastEvent.hasActiveContinuousFollowcastForGameTest(
                                    helper.getLevel(), player, "necklace", 0, 0),
                            "Continuous followcast must start with the resolved mana cost");
                } else {
                    var cooldown = magic.getPlayerCooldowns().getSpellCooldowns().get(spell.getSpellId());
                    int expected = WeaponImbueCooldownHelper.getEffectiveSpellCooldown(spell, player, CastSource.SWORD)
                            + (spell.getCastType() == CastType.LONG ? spell.getEffectiveCastTime(4, player) : 0);
                    helper.assertTrue(cooldown != null && cooldown.getSpellCooldown() == expected,
                            "Followcast cooldown must use resolved cast time without adding Affinity again");
                }
            } finally {
                RemoteOwnerContinuousCastManager.clearOwner(player, true);
                player.discard();
            }
        }
        helper.succeed();
    }
}
