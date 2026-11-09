package jp.aquafactory.apprenticecodex.gametest.support;

import io.redspace.ironsspellbooks.compat.Curios;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import jp.aquafactory.apprenticecodex.item.curios.absorptionamplifyamulet.AbsorptionAmplifyAmulet;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;

public final class CurioTestSupport {
    private CurioTestSupport() {}

    static final TagKey<Item> CURIOS_BACK = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("curios", CuriosSlotConstants.BACK)
    );

    public static final TagKey<Item> CURIOS_CHARM = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("curios", CuriosSlotConstants.CHARM)
    );

    public static void equipCurio(FakePlayer player, String slotId, ItemStack stack) {
        var curiosInventory = CuriosApi.getCuriosInventory(player)
                .orElseThrow(() -> new IllegalStateException("Missing curios inventory for curio equip test"));
        curiosInventory.setEquippedCurio(slotId, 0, stack);
    }

    public static void assertManaShieldCharmEquipped(GameTestHelper helper, ServerPlayer player, String context) {
        var curiosInventory = CuriosApi.getCuriosInventory(player)
                .orElseThrow(() -> new IllegalStateException("Missing curios inventory for Mana Shield Charm " + context + " test"));
        helper.assertTrue(curiosInventory.isEquipped(ItemRegistry.MANA_SHIELD_CHARM.get()),
                "Mana Shield Charm should be recognized as equipped in Curios during " + context + " test");
        helper.assertTrue(curiosInventory.findFirstCurio(ItemRegistry.MANA_SHIELD_CHARM.get()).isPresent(),
                "Mana Shield Charm should be discoverable via findFirstCurio during " + context + " test");
    }

    public static LivingAttackEvent postLivingAttackEventForGameTest(
            ServerPlayer player,
            DamageSource source,
            float amount
    ) {
        var event = new LivingAttackEvent(player, source, amount);
        MinecraftForge.EVENT_BUS.post(event);
        return event;
    }

    public static void equipRingCurio(FakePlayer player, ItemStack ringStack) {
        equipCurio(player, Curios.RING_SLOT, ringStack);
    }

    public static void equipNecklaceCurio(FakePlayer player, ItemStack necklaceStack) {
        equipCurio(player, Curios.NECKLACE_SLOT, necklaceStack);
    }

    public static void tickEquippedAbsorptionAmplifyAmulet(FakePlayer player) {
        var slotResult = CuriosApi.getCuriosInventory(player)
                .resolve()
                .flatMap(inventory -> inventory.findFirstCurio(ItemRegistry.ABSORPTION_AMPLIFY_AMULET.get()))
                .orElseThrow(() -> new IllegalStateException("Missing equipped Absorption Amplify Amulet for GameTest"));
        if (slotResult.stack().getItem() instanceof AbsorptionAmplifyAmulet amulet) {
            amulet.curioTick(slotResult.slotContext(), slotResult.stack());
        }
    }

    public static void equipGreaterConjurersTalisman(FakePlayer player) {
        var curiosInventory = CuriosApi.getCuriosInventory(player)
                .orElseThrow(() -> new IllegalStateException("Missing curios inventory for Greater Conjurer's Talisman test"));
        curiosInventory.setEquippedCurio(Curios.NECKLACE_SLOT, 0,
                new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.GREATER_CONJURERS_TALISMAN.get()));
    }

    public static SlotContext createSpellbookSlotContext(GameTestHelper helper) {
        return new SlotContext(
                Curios.SPELLBOOK_SLOT,
                helper.spawn(EntityType.PIG, new BlockPos(0, 2, 0)),
                0,
                false,
                true
        );
    }
}
