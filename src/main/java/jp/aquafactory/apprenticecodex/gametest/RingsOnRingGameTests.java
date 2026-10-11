package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import jp.aquafactory.apprenticecodex.registry.CreativeTabRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.common.event.CuriosEventHandler;
import top.theillusivec4.curios.common.inventory.CurioStacksHandler;

import java.util.LinkedHashMap;
import java.util.Objects;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class RingsOnRingGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";

    private RingsOnRingGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void ringsStackModifiersAndRemoveIndividually(GameTestHelper helper) {
        var player = createPlayer(helper, "rings_stack_test");
        var inventory = CuriosApi.getCuriosInventory(player).orElseThrow();
        var ringSlots = inventory.getStacksHandler("ring").orElseThrow();
        var mana = Objects.requireNonNull(player.getAttribute(AttributeRegistry.MAX_MANA),
                "Rings on Ring test requires max mana attribute");
        mana.setBaseValue(500);

        inventory.setEquippedCurio(CuriosSlotConstants.RINGS, 0, new ItemStack(ItemRegistry.RINGS_ON_RING.get()));
        tick(player);
        assertEffects(helper, player, 2, 450);
        inventory.setEquippedCurio(CuriosSlotConstants.RINGS, 1, new ItemStack(ItemRegistry.RINGS_ON_RING.get()));
        tick(player);
        assertEffects(helper, player, 3, 400);

        // 増設枠の収納物は、枠縮小時にCuriosが通常インベントリへ返すことを確認する。
        var silverRing = io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get();
        inventory.setEquippedCurio("ring", 2, new ItemStack(silverRing));
        tick(player);
        inventory.setEquippedCurio(CuriosSlotConstants.RINGS, 0, ItemStack.EMPTY);
        tick(player);
        assertEffects(helper, player, 2, 450);
        tick(player);
        int returnedCount = player.getInventory().items.stream()
                .filter(stack -> stack.is(silverRing)).mapToInt(ItemStack::getCount).sum();
        helper.assertTrue(returnedCount == 1, "Shrinking ring slots must return the extra ring exactly once");
        helper.assertTrue(ringSlots.getStacks().getStackInSlot(0).isEmpty()
                        && ringSlots.getStacks().getStackInSlot(1).isEmpty(),
                "Returned ring must not remain equipped");

        inventory.setEquippedCurio(CuriosSlotConstants.RINGS, 1, ItemStack.EMPTY);
        tick(player);
        assertEffects(helper, player, 1, 500);
        inventory.setEquippedCurio(CuriosSlotConstants.RINGS, 0, new ItemStack(ItemRegistry.RINGS_ON_RING.get()));
        tick(player);
        assertEffects(helper, player, 2, 450);
        tick(player);
        assertEffects(helper, player, 2, 450);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void ringsUseSeparateSlotAndIgnoreCosmetics(GameTestHelper helper) {
        var player = createPlayer(helper, "rings_slot_test");
        var stack = new ItemStack(ItemRegistry.RINGS_ON_RING.get());
        helper.assertTrue(stack.is(TagKey.create(Registries.ITEM,
                        ResourceLocation.fromNamespaceAndPath("curios", CuriosSlotConstants.RINGS))),
                "Rings on Ring must belong to the rings tag");
        helper.assertFalse(CuriosApi.isStackValid(new SlotContext("ring", player, 0, false, true), stack),
                "Rings on Ring must not fit the standard ring slot");
        var curio = CuriosApi.getCurio(stack).orElseThrow();
        var context = new SlotContext("ring", player, 0, false, true);
        helper.assertTrue(curio.getAttributeModifiers(context, CuriosApi.getSlotId(context)).isEmpty(),
                "Other slot types must not grant ring capacity or mana modifiers");
        var inventory = CuriosApi.getCuriosInventory(player).orElseThrow();
        inventory.getStacksHandler(CuriosSlotConstants.RINGS).orElseThrow().getCosmeticStacks().setStackInSlot(0, stack);
        double initialMana = player.getAttributeValue(AttributeRegistry.MAX_MANA);
        tick(player);
        assertEffects(helper, player, 1, initialMana);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void ringsRecipeAndCreativeTabRequireCataclysm(GameTestHelper helper) {
        boolean installed = ModList.get().isLoaded("cataclysm");
        var recipe = helper.getLevel().getRecipeManager().byKey(ItemRegistry.RINGS_ON_RING.getId());
        helper.assertTrue(recipe.isPresent() == installed, "Rings on Ring recipe must follow Cataclysm availability");
        var tab = CreativeTabRegistry.MAIN.get();
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(
                helper.getLevel().enabledFeatures(), true, helper.getLevel().registryAccess()));
        helper.assertTrue(tab.contains(new ItemStack(ItemRegistry.RINGS_ON_RING.get())) == installed,
                "Rings on Ring creative tab entry must follow Cataclysm availability");
        var advancement = helper.getLevel().getServer().getAdvancements().get(
                ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "recipes/combat/rings_on_ring"));
        helper.assertTrue((advancement != null) == installed,
                "Rings on Ring recipe advancement must follow Cataclysm availability");
        if (installed) {
            helper.assertTrue(CuriosApi.getPlayerSlots(helper.getLevel()).containsKey(CuriosSlotConstants.RINGS),
                    "Cataclysm must provide the rings slot");
            var input = NonNullList.withSize(9, ItemStack.EMPTY);
            input.set(0, new ItemStack(ItemRegistry.WISDOM_SHARD.get()));
            for (int index : new int[]{1, 3, 5, 7}) {
                input.set(index, new ItemStack(ItemRegistry.MANA_ENVELOPED_SILVER_CHUNK.get()));
            }
            input.set(4, new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get()));
            var crafting = CraftingInput.of(3, 3, input);
            var shaped = (ShapedRecipe) recipe.orElseThrow().value();
            helper.assertTrue(shaped.matches(crafting, helper.getLevel()), "Rings on Ring recipe must match the specified pattern");
            var result = shaped.assemble(crafting, helper.getLevel().registryAccess());
            helper.assertTrue(result.is(ItemRegistry.RINGS_ON_RING.get()) && result.getCount() == 1,
                    "Rings on Ring recipe must produce one item");
        }
        helper.succeed();
    }

    private static FakePlayer createPlayer(GameTestHelper helper, String name) {
        var player = BowGameTestSupport.createEquipmentTestPlayer(helper, new BlockPos(1, 2, 1), name);
        var inventory = CuriosApi.getCuriosInventory(player).orElseThrow();
        // MODなしでも実装を検証できるよう、テスト対象のプレイヤーだけにringsを用意する。
        var slots = new LinkedHashMap<>(inventory.getCurios());
        slots.clear();
        slots.put("ring", new CurioStacksHandler(inventory, "ring"));
        slots.put(CuriosSlotConstants.RINGS, new CurioStacksHandler(
                inventory, CuriosSlotConstants.RINGS, 2, true, true, true, ICurio.DropRule.DEFAULT));
        inventory.setCurios(slots);
        return player;
    }

    private static void tick(FakePlayer player) {
        new CuriosEventHandler().tick(new EntityTickEvent.Post(player));
    }

    private static void assertEffects(GameTestHelper helper, FakePlayer player, int slots, double mana) {
        helper.assertTrue(CuriosApi.getCuriosInventory(player).orElseThrow()
                        .getStacksHandler("ring").orElseThrow().getSlots() == slots,
                "Rings on Ring capacity differs from expected " + slots);
        helper.assertTrue(player.getAttributeValue(AttributeRegistry.MAX_MANA) == mana,
                "Rings on Ring max mana differs from expected " + mana);
    }
}
