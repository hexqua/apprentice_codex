package jp.aquafactory.apprenticecodex.gametest.equipment.magiengineersstaff;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import jp.aquafactory.apprenticecodex.compat.magitech.MagiEngineersStaffCompat;
import jp.aquafactory.apprenticecodex.item.magiengineersstaff.MagiEngineersStaff;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.stln.magitech.content.entity.status.AttributeInit;
import net.stln.magitech.content.item.ItemInit;
import net.stln.magitech.content.item.component.ComponentInit;
import net.stln.magitech.content.item.component.SpellComponent;
import net.stln.magitech.content.network.SpellCastPayLoadHandler;
import net.stln.magitech.content.network.SpellCastPayload;
import net.stln.magitech.content.network.SpellEndPayLoadHandler;
import net.stln.magitech.content.network.SpellEndPayload;
import net.stln.magitech.core.api.mana.ManaCapabilities;
import net.stln.magitech.data.DataAttachmentInit;
import net.stln.magitech.feature.element.Element;
import net.stln.magitech.feature.magic.charge.ChargeData;
import net.stln.magitech.feature.magic.MagicPerformanceHelper;
import net.stln.magitech.feature.magic.spell.SpellInit;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

final class MagiEngineersStaffScenarios {
    private MagiEngineersStaffScenarios() {
    }

    private static FakePlayer player(GameTestHelper helper) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "magi_compat_test"));
        player.setPos(helper.absoluteVec(new Vec3(2, 2, 2)));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemRegistry.MAGI_ENGINEERS_STAFF.get()));
        return player;
    }

    static void conversion(GameTestHelper helper) {
        var player = player(helper);
        var item = (MagiEngineersStaff) player.getMainHandItem().getItem();
        var stack = player.getMainHandItem();
        var mana = Objects.requireNonNull(player.getCapability(ManaCapabilities.MANA_CAPABLE_ENTITY));
        var data = MagicData.getPlayerMagicData(player);
        mana.setMana(0);
        data.setMana(9.5F);
        player.tickCount = 10;
        item.inventoryTick(stack, helper.getLevel(), player, 0, true);
        helper.assertTrue(data.getMana() == 9.5F && mana.getMana() == 0, "Fractional mana below ten must remain untouched");
        data.setMana(10);
        player.tickCount = 9;
        item.inventoryTick(stack, helper.getLevel(), player, 0, true);
        helper.assertTrue(data.getMana() == 10 && mana.getMana() == 0, "Conversion must respect its tick interval");
        player.tickCount = 10;
        item.inventoryTick(stack, helper.getLevel(), player, 0, true);
        helper.assertTrue(data.getMana() == 0 && mana.getMana() == 4000, "Exactly ten mana must restore four kJ");
        mana.setMana(mana.getMaxMana() - 500);
        data.setMana(25.5F);
        item.inventoryTick(stack, helper.getLevel(), player, 0, true);
        helper.assertTrue(mana.getMana() == mana.getMaxMana() && data.getMana() == 15.5F,
                "Partial capacity must still cost ten mana and clamp recovery");
        item.inventoryTick(stack, helper.getLevel(), player, 0, true);
        helper.assertTrue(data.getMana() == 15.5F, "Full Magitech mana must stop conversion");
        mana.setMana(0);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.setItemInHand(InteractionHand.OFF_HAND, stack);
        item.inventoryTick(stack, helper.getLevel(), player, 0, false);
        helper.assertTrue(data.getMana() == 15.5F && mana.getMana() == 0, "Offhand storage must not convert mana");
        helper.succeed();
    }

    static void power(GameTestHelper helper) {
        var player = player(helper);
        Objects.requireNonNull(player.getAttribute(AttributeRegistry.SPELL_POWER)).setBaseValue(1.5);
        Objects.requireNonNull(player.getAttribute(AttributeRegistry.ENDER_SPELL_POWER)).setBaseValue(2);
        Objects.requireNonNull(player.getAttribute(AttributeInit.SPELL_POWER)).setBaseValue(1.3);
        Objects.requireNonNull(player.getAttribute(AttributeInit.LAUNCH)).setBaseValue(1.7);
        var stack = player.getMainHandItem();
        float converted = MagicPerformanceHelper.getOutgoingMagicDamage(player, stack, 25, 3, Element.TREMOR);
        float nativeDamage = MagicPerformanceHelper.getOutgoingMagicDamage(player, new ItemStack(Items.STICK), 25, 3, Element.TREMOR);
        helper.assertTrue(Math.abs(converted - 8.4F) < 0.001 && Math.abs(nativeDamage - 3.9F) < 0.001,
                "Iron power and external Magitech bonuses must combine only for the new staff");
        helper.assertTrue(Math.abs(MagicPerformanceHelper.getEffectiveProjectileSpeed(player, stack, SpellInit.AETHERIX.get()) - 3.3F) < 0.001,
                "Launch power must preserve external bonuses over the staff base of 1.5");
        helper.assertTrue(MagiEngineersStaffCompat.highestElement(player) == Element.TREMOR,
                "Highest school must use the fixed element mapping");
        Objects.requireNonNull(player.getAttribute(AttributeRegistry.FIRE_SPELL_POWER)).setBaseValue(2);
        helper.assertTrue(MagiEngineersStaffCompat.highestElement(player) == Element.EMBER,
                "School ties must keep the earlier element");
        Objects.requireNonNull(player.getAttribute(AttributeRegistry.ELDRITCH_SPELL_POWER)).setBaseValue(3);
        helper.assertTrue(MagiEngineersStaffCompat.schoolPower(Element.MANA, player) == 3
                        && MagiEngineersStaffCompat.schoolPower(Element.LOGOS, player) == 3,
                "Mana must include Eldritch even though Logos is hidden from detail rows");
        helper.succeed();
    }

    static void equalIdCasters(GameTestHelper helper) {
        var serverCaster = player(helper);
        var mirrorCaster = player(helper);
        // 統合serverの両論理側を模し、Entity.equalsは同じでも別インスタンスを使う。
        mirrorCaster.setId(serverCaster.getId());
        helper.assertTrue(serverCaster != mirrorCaster && serverCaster.equals(mirrorCaster),
                "Fixture must reproduce equal entity IDs across distinct caster instances");
        var item = (MagiEngineersStaff) serverCaster.getMainHandItem().getItem();
        for (var caster : List.of(serverCaster, mirrorCaster)) {
            var mana = Objects.requireNonNull(caster.getCapability(ManaCapabilities.MANA_CAPABLE_ENTITY));
            mana.setMana(mana.getMaxMana());
            var threadbound = new ItemStack(ItemInit.ARCANE_ENGINEERING_COMPENDIUM.get());
            threadbound.set(ComponentInit.SPELL_COMPONENT, new SpellComponent(List.of(SpellInit.FULGENZA.get()), 0));
            CuriosApi.getCuriosInventory(caster).orElseThrow().setEquippedCurio("threadbound", 0, threadbound);
        }
        try {
            item.use(helper.getLevel(), serverCaster, InteractionHand.MAIN_HAND);
            item.use(helper.getLevel(), mirrorCaster, InteractionHand.MAIN_HAND);
            var serverMana = Objects.requireNonNull(serverCaster.getCapability(ManaCapabilities.MANA_CAPABLE_ENTITY));
            var mirrorMana = Objects.requireNonNull(mirrorCaster.getCapability(ManaCapabilities.MANA_CAPABLE_ENTITY));
            long serverBefore = serverMana.getMana();
            long mirrorBefore = mirrorMana.getMana();
            serverCaster.setData(DataAttachmentInit.SPELL_CHARGE, ChargeData.empty());
            item.onUseTick(helper.getLevel(), serverCaster, serverCaster.getMainHandItem(), 71920);
            helper.assertTrue(!serverCaster.isUsingItem() && serverMana.getMana() == serverBefore - 85000,
                    "Equal-ID mirror state must not overwrite the server cast or block charge completion");
            helper.assertTrue(mirrorCaster.isUsingItem() && mirrorMana.getMana() == mirrorBefore,
                    "Finishing one caster must not remove the equal-ID mirror cast");
            mirrorCaster.setData(DataAttachmentInit.SPELL_CHARGE, ChargeData.empty());
            item.onUseTick(helper.getLevel(), mirrorCaster, mirrorCaster.getMainHandItem(), 71920);
            helper.assertTrue(!mirrorCaster.isUsingItem() && mirrorMana.getMana() == mirrorBefore - 85000,
                    "Equal-ID mirror cast must finish independently");
        } finally {
            MagiEngineersStaffCompat.cancel(serverCaster);
            MagiEngineersStaffCompat.cancel(mirrorCaster);
        }
        helper.succeed();
    }

    static void casting(GameTestHelper helper) {
        var player = player(helper);
        var mana = Objects.requireNonNull(player.getCapability(ManaCapabilities.MANA_CAPABLE_ENTITY));
        mana.setMana(mana.getMaxMana());
        var threadbound = new ItemStack(ItemInit.ARCANE_ENGINEERING_COMPENDIUM.get());
        threadbound.set(ComponentInit.SPELL_COMPONENT, new SpellComponent(List.of(SpellInit.AETHERIX.get(), SpellInit.FULGENZA.get()), 0));
        CuriosApi.getCuriosInventory(player).orElseThrow().setEquippedCurio("threadbound", 0, threadbound);
        var stack = player.getMainHandItem();
        var item = (MagiEngineersStaff) stack.getItem();
        long before = mana.getMana();
        // C2S別経路はcontextや送信者を使う前に拒否し、server側useへ一本化する。
        SpellCastPayLoadHandler.handleDataOnMainC2S(new SpellCastPayload(SpellInit.AETHERIX.get(), Optional.of(stack), player.getId()), null);
        SpellEndPayLoadHandler.handleDataOnMainC2S(new SpellEndPayload(SpellInit.AETHERIX.get(), Optional.of(stack), player.getId()), null);
        helper.assertTrue(mana.getMana() == before, "Client-supplied staff payloads must not cast or consume mana");
        item.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(mana.getMana() == before - 25000 && !stack.isDamaged(),
                "Instant casting must consume Magitech mana exactly once without damaging the staff");
        mana.setMana(mana.getMaxMana());
        threadbound.set(ComponentInit.SPELL_COMPONENT, new SpellComponent(List.of(SpellInit.FULGENZA.get(), SpellInit.AETHERIX.get()), 0));
        item.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(player.isUsingItem() && player.getData(DataAttachmentInit.SPELL_CHARGE).charge().remaining() > 0,
                "Charged casting must start using the staff and track charge");
        threadbound.set(ComponentInit.SPELL_COMPONENT, new SpellComponent(List.of(SpellInit.FULGENZA.get(), SpellInit.AETHERIX.get()), 1));
        before = mana.getMana();
        item.releaseUsing(stack, helper.getLevel(), player, 71999);
        helper.assertTrue(mana.getMana() == before && !player.isUsingItem()
                        && player.getData(DataAttachmentInit.SPELL_CHARGE).charge().remaining() == 0,
                "Early release after selection changes must cancel the original charge without a second spell");
        threadbound.set(ComponentInit.SPELL_COMPONENT, new SpellComponent(List.of(SpellInit.FULGENZA.get()), 0));
        item.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        player.setData(DataAttachmentInit.SPELL_CHARGE, ChargeData.empty());
        before = mana.getMana();
        item.onUseTick(helper.getLevel(), player, stack, 71920);
        helper.assertTrue(!player.isUsingItem() && mana.getMana() == before - 85000,
                "Completed charge must finish once and consume the selected spell cost");
        mana.setMana(mana.getMaxMana());
        threadbound.set(ComponentInit.SPELL_COMPONENT, new SpellComponent(List.of(SpellInit.ESFOUNTIA.get()), 0));
        before = mana.getMana();
        item.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        item.onUseTick(helper.getLevel(), player, stack, 71999);
        item.releaseUsing(stack, helper.getLevel(), player, 71999);
        helper.assertTrue(mana.getMana() == before - 5500 && !player.isUsingItem(),
                "Continuous spell must pay its initial and per-tick costs without a second end cost");
        helper.succeed();
    }
}
