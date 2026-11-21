package jp.aquafactory.apprenticecodex.gametest.equipment.magiengineersstaff;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.magitech.MagiEngineersStaffCompat;
import jp.aquafactory.apprenticecodex.item.magiengineersstaff.MagiEngineersStaff;
import jp.aquafactory.apprenticecodex.network.packet.SyncMagiEngineersStaffCastPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncMagiEngineersStaffCastPacket.Phase;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.stln.magitech.content.entity.magicentity.aetherix.AetherixEntity;
import net.stln.magitech.content.entity.status.AttributeInit;
import net.stln.magitech.content.item.ItemInit;
import net.stln.magitech.content.item.component.ComponentInit;
import net.stln.magitech.content.item.component.PartMaterialComponent;
import net.stln.magitech.content.item.component.SpellComponent;
import net.stln.magitech.core.api.mana.ManaCapabilities;
import net.stln.magitech.feature.element.Element;
import net.stln.magitech.feature.magic.MagicPerformanceHelper;
import net.stln.magitech.feature.magic.cooldown.CooldownHelper;
import net.stln.magitech.feature.magic.spell.SpellInit;
import net.stln.magitech.feature.tool.material.MaterialInit;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

final class MagiEngineersStaffOffhandScenarios {
    private MagiEngineersStaffOffhandScenarios() {}

    static void nativeOffhandAttributes(GameTestHelper helper) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "magi_mixed_hands"));
        var staff = new ItemStack(ItemRegistry.MAGI_ENGINEERS_STAFF.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, staff);
        Objects.requireNonNull(player.getAttribute(AttributeInit.SPELL_POWER)).addTransientModifier(
                new AttributeModifier(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "test_external_power"),
                        0.25, AttributeModifier.Operation.ADD_VALUE));
        Objects.requireNonNull(player.getAttribute(AttributeInit.LAUNCH)).addTransientModifier(
                new AttributeModifier(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "test_external_launch"),
                        0.2, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        player.doTick();
        var baseline = spellPerformance(player, staff);
        helper.assertTrue(Math.abs(baseline.damage() - 12.5) < 0.001 && Math.abs(baseline.speed() - 2.7) < 0.001,
                "External spell power and launch modifiers must apply to the main-hand staff");

        var nativeStaff = new ItemStack(ItemInit.STAFF.get());
        nativeStaff.set(ComponentInit.PART_MATERIAL_COMPONENT,
                new PartMaterialComponent(MaterialInit.SAMPLE, MaterialInit.SAMPLE, MaterialInit.SAMPLE, MaterialInit.SAMPLE));
        player.setItemInHand(InteractionHand.OFF_HAND, nativeStaff);
        // 組み立てたばかりの杖は耐久値の初期化後に装備補正が生成される。
        nativeStaff.inventoryTick(helper.getLevel(), player, 40, false);
        nativeStaff.inventoryTick(helper.getLevel(), player, 40, false);
        player.doTick();
        helper.assertTrue(Math.abs(player.getAttributeValue(AttributeInit.SPELL_POWER) - 2.25) < 0.001,
                "Native offhand spell power must remain active on the player");
        var mixed = spellPerformance(player, staff);
        helper.assertTrue(baseline.equals(mixed),
                "The offhand caster must not change the main-hand staff's spell values. Expected " + baseline + ", got " + mixed);

        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.doTick();
        helper.assertTrue(Math.abs(spellPerformance(player, nativeStaff).damage() - 22.5) < 0.001,
                "The native staff must still use its own offhand power exactly once when it is the only staff");
        helper.succeed();
    }

    private static SpellPerformance spellPerformance(FakePlayer player, ItemStack staff) {
        var shot = SpellInit.AETHERIX.get();
        return new SpellPerformance(MagicPerformanceHelper.getOutgoingMagicDamage(player, staff, 25, 10, Element.EMBER),
                MagicPerformanceHelper.getEffectiveCost(player, staff, shot),
                MagicPerformanceHelper.getEffectiveChargeTime(player, staff, SpellInit.FULGENZA.get()),
                MagicPerformanceHelper.getEffectiveCooldown(player, staff, shot),
                MagicPerformanceHelper.getEffectiveProjectileSpeed(player, staff, shot));
    }

    private record SpellPerformance(float damage, float cost, float charge, int cooldown, float speed) {}

    static void priority(GameTestHelper helper) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "magi_priority"));
        player.setPos(helper.absolutePos(new BlockPos(2, 2, 2)).getCenter());
        var stack = new ItemStack(ItemRegistry.MAGI_ENGINEERS_STAFF.get());
        var item = (MagiEngineersStaff) stack.getItem();
        player.setItemInHand(InteractionHand.OFF_HAND, stack);
        var threadbound = new ItemStack(ItemInit.ARCANE_ENGINEERING_COMPENDIUM.get());
        var spell = SpellInit.AETHERIX.get();
        threadbound.set(ComponentInit.SPELL_COMPONENT, new SpellComponent(List.of(spell), 0));
        CuriosApi.getCuriosInventory(player).orElseThrow().setEquippedCurio("threadbound", 0, threadbound);
        var mana = Objects.requireNonNull(player.getCapability(ManaCapabilities.MANA_CAPABLE_ENTITY));
        for (var nativeItem : List.of(ItemInit.WAND.get(), ItemInit.STAFF.get())) {
            var nativeStack = new ItemStack(nativeItem);
            player.setItemInHand(InteractionHand.MAIN_HAND, nativeStack);
            mana.setMana(mana.getMaxMana());
            long before = mana.getMana();
            helper.assertTrue(!item.use(helper.getLevel(), player, InteractionHand.OFF_HAND).getResult().consumesAction()
                            && mana.getMana() == before && !player.isUsingItem(),
                    "Native main-hand caster must reject offhand staff without spending mana");
            helper.assertTrue(MagiEngineersStaffCompat.displayWand(player) == nativeStack,
                    "HUD must use the native main-hand caster");
            nativeStack.set(ComponentInit.BROKEN_COMPONENT, true);
            helper.assertTrue(!item.use(helper.getLevel(), player, InteractionHand.OFF_HAND).getResult().consumesAction(),
                    "Broken native caster must not fall back to the offhand staff");
            nativeStack.set(ComponentInit.BROKEN_COMPONENT, false);
            mana.setMana(0);
            helper.assertTrue(!item.use(helper.getLevel(), player, InteractionHand.OFF_HAND).getResult().consumesAction(),
                    "Insufficient native mana must not allow offhand fallback");
            mana.setMana(mana.getMaxMana());
            CooldownHelper.updateCooldown(player, spell, nativeStack);
            helper.assertTrue(!item.use(helper.getLevel(), player, InteractionHand.OFF_HAND).getResult().consumesAction(),
                    "Native cooldown must not allow offhand fallback");
        }
        // 別Playerでクールダウンを分離し、拒否だけでなく通常副手詠唱の成立まで確認する。
        var caster = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "magi_offhand_shot"));
        caster.setPos(player.position());
        caster.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        caster.setItemInHand(InteractionHand.OFF_HAND, stack);
        CuriosApi.getCuriosInventory(caster).orElseThrow().setEquippedCurio("threadbound", 0, threadbound.copy());
        var casterMana = Objects.requireNonNull(caster.getCapability(ManaCapabilities.MANA_CAPABLE_ENTITY));
        casterMana.setMana(casterMana.getMaxMana());
        helper.assertTrue(MagiEngineersStaffCompat.displayWand(caster) == stack,
                "Sword and offhand staff must display offhand spell values");
        item.use(helper.getLevel(), caster, InteractionHand.OFF_HAND);
        var shots = helper.getLevel().getEntitiesOfClass(AetherixEntity.class, caster.getBoundingBox().inflate(2),
                entity -> entity.getOwner() == caster);
        try {
            helper.assertTrue(shots.size() == 1, "Offhand use must spawn exactly one actual spell projectile");
            var shot = shots.getFirst();
            helper.assertTrue(ItemStack.isSameItemSameComponents(shot.getWand(), stack)
                            && Math.abs(shot.getDeltaMovement().length()
                            - MagicPerformanceHelper.getEffectiveProjectileSpeed(caster, stack, spell)) < 0.001,
                    "Actual offhand projectile must retain its source staff and converted launch speed");
        } finally {
            shots.forEach(AetherixEntity::discard);
        }
        helper.succeed();
    }

    static void packet(GameTestHelper helper) {
        var stack = new ItemStack(ItemRegistry.MAGI_ENGINEERS_STAFF.get());
        var spellId = ResourceLocation.fromNamespaceAndPath("magitech", "fulgenza");
        for (var hand : InteractionHand.values()) {
            for (var phase : Phase.values()) {
                var packet = new SyncMagiEngineersStaffCastPacket(42, spellId, stack.copy(), hand, phase, 37);
                var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess(), ConnectionType.NEOFORGE);
                try {
                    SyncMagiEngineersStaffCastPacket.STREAM_CODEC.encode(buffer, packet);
                    var decoded = SyncMagiEngineersStaffCastPacket.STREAM_CODEC.decode(buffer);
                    helper.assertTrue(decoded.entityId() == 42 && decoded.spellId().equals(spellId)
                                    && decoded.hand() == hand && decoded.phase() == phase && decoded.chargeTicks() == 37
                                    && ItemStack.matches(decoded.stack(), stack) && decoded.stack() != stack,
                            "Staff sync must preserve explicit hand, phase, spell and a decoded source snapshot");
                } finally {
                    buffer.release();
                }
            }
        }
        helper.succeed();
    }

    static void lootEnchantments(GameTestHelper helper) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "magi_loot"));
        var stack = new ItemStack(ItemRegistry.MAGI_ENGINEERS_STAFF.get());
        var registry = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var looting = registry.getOrThrow(Enchantments.LOOTING);
        stack.enchant(looting, 3);
        for (var hand : InteractionHand.values()) {
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            player.setItemInHand(hand, stack);
            helper.assertTrue(EnchantmentHelper.getEnchantmentLevel(looting, player)
                            == (hand == InteractionHand.MAIN_HAND ? 3 : 0),
                    "Looting must retain the gauntlets' intentional main-hand-only limitation");
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, stack.copy());
        helper.assertTrue(EnchantmentHelper.getEnchantmentLevel(looting, player) == 3,
                "Two staves must not add both Looting levels");
        if (ModList.get().isLoaded("malum")) {
            var plunder = helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                    .getHolder(ResourceLocation.fromNamespaceAndPath("malum", "spirit_plunder")).orElseThrow();
            var spoils = BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.fromNamespaceAndPath("malum", "spirit_spoils")).orElseThrow();
            var enchanted = new ItemStack(ItemRegistry.MAGI_ENGINEERS_STAFF.get());
            enchanted.enchant(plunder, 2);
            double base = spoils.value().getDefaultValue();
            for (var hand : InteractionHand.values()) {
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
                player.setItemInHand(hand, enchanted.copy());
                player.doTick();
                helper.assertTrue(Math.abs(player.getAttributeValue(spoils) - base
                                - (hand == InteractionHand.MAIN_HAND ? 2 : 0)) < 0.001,
                        "Spirit Plunder must only supply spirit spoils from the main hand");
            }
            player.setItemInHand(InteractionHand.MAIN_HAND, enchanted.copy());
            player.doTick();
            helper.assertTrue(Math.abs(player.getAttributeValue(spoils) - base - 2) < 0.001,
                    "Two staves must not double Spirit Plunder");
        }
        helper.succeed();
    }
}
