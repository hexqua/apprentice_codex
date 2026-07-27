package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.config.SpellConfigManager;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.block.apprenticedesk.ApprenticeDeskMenu;
import jp.aquafactory.apprenticecodex.item.magicitem.WoodenWand;
import jp.aquafactory.apprenticecodex.item.magicitem.WoodenWandDurabilityEvent;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ApprenticeDeskReworkGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";
    private static final String DESK_SPELL_CONFIG_BATCH = "apprentice_desk_spell_config_isolated";

    private ApprenticeDeskReworkGameTests() {
    }

    @GameTest(template = TEMPLATE, batch = DESK_SPELL_CONFIG_BATCH)
    public static void apprenticeDeskCraftsWandAndKeepsFocus(GameTestHelper helper) {
        var previousSpellConfigManager = SpellConfigManager.INSTANCE;
        try {
            SpellConfigManager.INSTANCE = new SpellConfigManager();
            SpellConfigManager.INSTANCE.handleServerConfigUpdate();
            SpellConfigManager.onDatapackSync(new OnDatapackSyncEvent(
                    helper.getLevel().getServer().getPlayerList(),
                    null
            ));

            var deskPos = new BlockPos(1, 1, 1);
            helper.setBlock(deskPos, BlockRegistry.APPRENTICE_DESK.get());
            var player = createPlayer(helper, "apprentice_desk_focus_test");
            var menu = new ApprenticeDeskMenu(
                    0,
                    player.getInventory(),
                    ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(deskPos))
            );
            menu.container.setItem(
                    ApprenticeDeskMenu.INK_SLOT,
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.INK_LEGENDARY.get(), 2)
            );
            menu.container.setItem(ApprenticeDeskMenu.WAND_BASE_SLOT, new ItemStack(Items.STICK, 2));
            menu.container.setItem(
                    ApprenticeDeskMenu.FOCUS_SLOT,
                    new ItemStack(Items.BLAZE_ROD)
            );

            helper.assertTrue(menu.hasAllInputs(), "Apprentice Desk did not recognize all three inputs");
            helper.assertFalse(menu.getAvailableSpells().isEmpty(), "Fire focus exposed no craftable spells");
            var selectedSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIREBALL_SPELL.get();
            var selectedIndex = menu.getAvailableSpells().indexOf(selectedSpell);
            helper.assertTrue(selectedIndex >= 0, "Fire focus did not expose Fireball");
            MagicData.getPlayerMagicData(player).getSyncedData().learnSpell(selectedSpell, false);
            helper.assertTrue(menu.clickMenuButton(player, selectedIndex), "A learned fire spell could not be selected");

            var resultSlot = menu.getSlot(ApprenticeDeskMenu.RESULT_SLOT);
            var result = resultSlot.getItem();
            helper.assertTrue(result.is(ItemRegistry.WOODEN_WAND.get()), "Apprentice Desk did not create a wooden wand");
            var resultContainer = ISpellContainer.get(result);
            helper.assertTrue(resultContainer != null, "Crafted wooden wand has no spell container");
            helper.assertTrue(resultContainer != null && !resultContainer.isSpellWheel(),
                    "Crafted wooden wand unexpectedly contributes to the spell wheel");
            var expectedLevel = selectedSpell.getMinLevelForRarity(SpellRarity.LEGENDARY);
            helper.assertTrue(resultContainer != null
                            && resultContainer.getSpellAtIndex(0).getSpell() == selectedSpell
                            && resultContainer.getSpellAtIndex(0).getLevel() == expectedLevel,
                    "Crafted wooden wand has the wrong spell or ink-derived level");

            var taken = resultSlot.remove(1);
            resultSlot.onTake(player, taken);
            helper.assertTrue(menu.container.getItem(ApprenticeDeskMenu.INK_SLOT).getCount() == 1,
                    "Taking a wand did not consume exactly one ink");
            helper.assertTrue(menu.container.getItem(ApprenticeDeskMenu.WAND_BASE_SLOT).getCount() == 1,
                    "Taking a wand did not consume exactly one wand base");
            helper.assertTrue(menu.container.getItem(ApprenticeDeskMenu.FOCUS_SLOT).is(Items.BLAZE_ROD),
                    "Taking a wand consumed the school focus");

            var magicData = MagicData.getPlayerMagicData(player);
            var maxMana = player.getAttribute(
                    io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA.get()
            );
            helper.assertTrue(maxMana != null, "Wooden wand test could not resolve max mana");
            maxMana.setBaseValue(1000.0D);
            magicData.setMana(1000.0F);
            player.setItemInHand(InteractionHand.MAIN_HAND, taken);
            var castLevel = selectedSpell.getLevelFor(
                    WoodenWand.getImbuedSpell(taken).getLevel(),
                    player
            );
            var preparedCastResult = selectedSpell.canBeCastedBy(
                    castLevel,
                    CastSource.SWORD,
                    magicData,
                    player
            );
            helper.assertTrue(preparedCastResult.isSuccess(),
                    "The prepared player could not cast the wooden wand spell: learned="
                            + selectedSpell.isLearned(player)
                            + ", mana=" + magicData.getMana()
                            + ", cost=" + selectedSpell.getManaCost(castLevel)
                            + ", cooldown=" + magicData.getPlayerCooldowns().isOnCooldown(selectedSpell)
                            + ", message=" + (preparedCastResult.message == null
                            ? "none"
                            : preparedCastResult.message.getString()));
            helper.assertTrue(selectedSpell.checkPreCastConditions(helper.getLevel(), castLevel, player, magicData),
                    "The wooden wand spell failed its pre-cast conditions");
            var useResult = taken.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(useResult.getResult().consumesAction(),
                    "Right-clicking the wooden wand returned " + useResult.getResult());
            helper.assertTrue(magicData.isCasting(),
                    "Right-clicking the wooden wand consumed the action without starting its imbued spell");
            helper.assertTrue(magicData.getPlayerCastingItem() == taken,
                    "Wooden wand casting did not retain the wand as the casting item");
            magicData.resetCastingState();
            helper.succeed();
        } finally {
            SpellConfigManager.INSTANCE = previousSpellConfigManager;
        }
    }

    @GameTest(template = TEMPLATE)
    public static void apprenticeDeskAcceptsAndReturnsScrollFocus(GameTestHelper helper) {
        var deskPos = new BlockPos(1, 1, 1);
        helper.setBlock(deskPos, BlockRegistry.APPRENTICE_DESK.get());
        var player = createPlayer(helper, "apprentice_desk_scroll_test");
        var menu = new ApprenticeDeskMenu(
                0,
                player.getInventory(),
                ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(deskPos))
        );
        var fireball = io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIREBALL_SPELL.get();
        var scroll = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get());
        ISpellContainer.createScrollContainer(fireball, 1, scroll);

        menu.container.setItem(
                ApprenticeDeskMenu.INK_SLOT,
                new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.INK_LEGENDARY.get())
        );
        menu.container.setItem(ApprenticeDeskMenu.WAND_BASE_SLOT, new ItemStack(Items.STICK));
        menu.container.setItem(ApprenticeDeskMenu.FOCUS_SLOT, scroll);

        helper.assertTrue(menu.hasAllInputs(), "Spell scroll was not accepted as a school focus");
        helper.assertTrue(menu.getAvailableSpells().stream()
                        .allMatch(spell -> spell.getSchoolType() == fireball.getSchoolType()),
                "Spell scroll focus exposed a spell from another school");

        menu.removed(player);
        helper.assertTrue(menu.container.isEmpty(), "Closing the Apprentice Desk left transient input items behind");
        helper.assertTrue(player.getInventory().items.stream().anyMatch(stack ->
                        stack.is(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get())
                                && ISpellContainer.isSpellContainer(stack)
                                && ISpellContainer.get(stack).getSpellAtIndex(0).getSpell() == fireball),
                "Closing the Apprentice Desk did not return the focus scroll");
        helper.assertTrue(player.getInventory().contains(new ItemStack(Items.STICK)),
                "Closing the Apprentice Desk did not return the wand base");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void woodenWandUsesOwnSpellAndConsumesDurabilityOnCooldown(GameTestHelper helper) {
        var player = createPlayer(helper, "wooden_wand_durability_test");
        var wand = new ItemStack(ItemRegistry.WOODEN_WAND.get());
        var magicMissile = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
        var fireball = io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIREBALL_SPELL.get();
        var mutable = ISpellContainer.create(1, false, false).mutableCopy();
        mutable.addSpellAtIndex(magicMissile, 1, 0, true);
        ISpellContainer.set(wand, mutable.toImmutable());

        var wandItem = (WoodenWand) wand.getItem();
        helper.assertTrue(wandItem.getMaxDamage(wand) == WoodenWand.MAX_DAMAGE,
                "Wooden wand has the wrong durability");
        helper.assertTrue(wandItem.getEnchantmentValue(wand) == 15,
                "Wooden wand has the wrong enchantment value");
        helper.assertTrue(wandItem.canApplyAtEnchantingTable(wand, Enchantments.UNBREAKING)
                        && wandItem.canApplyAtEnchantingTable(wand, Enchantments.MENDING)
                        && wandItem.canApplyAtEnchantingTable(wand, EnchantmentRegistry.WISDOM.get())
                        && wandItem.canApplyAtEnchantingTable(wand, EnchantmentRegistry.PLUNDER.get()),
                "Wooden wand rejected an allowed enchantment");
        helper.assertFalse(wandItem.canApplyAtEnchantingTable(wand, Enchantments.SHARPNESS),
                "Wooden wand accepted a non-durability combat enchantment");
        helper.assertTrue(wandItem.isValidRepairItem(
                        wand,
                        new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_ESSENCE.get())
                ),
                "Arcane essence did not repair the wooden wand");
        helper.assertTrue(wand.getHoverName().getContents() instanceof TranslatableContents contents
                        && "item.apprenticecodex.wooden_wand.imbue_spell".equals(contents.getKey()),
                "Imbued wooden wand did not use the spell-name translation");

        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        var magicData = MagicData.getPlayerMagicData(player);
        magicData.setPlayerCastingItem(wand);
        WoodenWandDurabilityEvent.onSpellCooldownAdded(
                new SpellCooldownAddedEvent.Post(20, fireball, player, CastSource.SWORD)
        );
        helper.assertTrue(wand.getDamageValue() == 0,
                "An unrelated spell cooldown damaged the wooden wand");

        WoodenWandDurabilityEvent.onSpellCooldownAdded(
                new SpellCooldownAddedEvent.Post(20, magicMissile, player, CastSource.SWORD)
        );
        helper.assertTrue(wand.getDamageValue() == 1,
                "The wooden wand did not lose durability when its spell entered cooldown");
        helper.succeed();
    }

    private static FakePlayer createPlayer(GameTestHelper helper, String name) {
        return BowGameTestSupport.createEquipmentTestPlayer(helper, new BlockPos(1, 2, 1), name);
    }
}
