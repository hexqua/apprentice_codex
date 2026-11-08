package jp.aquafactory.apprenticecodex.gametest.equipment.mithrilfreecaststaff;

import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import io.redspace.ironsspellbooks.compat.Curios;
import io.redspace.ironsspellbooks.gui.overlays.SpellSelection;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.WeaponImbueCooldownHelper;
import jp.aquafactory.apprenticecodex.item.curios.archivistsgrimoire.ArchivistsGrimoire;
import jp.aquafactory.apprenticecodex.item.mithrilfreecaststaff.MithrilFreecastStaffCastContext;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.support.CurioTestSupport.equipCurio;
import static jp.aquafactory.apprenticecodex.gametest.support.SpellTestSupport.createSpellScroll;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class MithrilRecastCooldownGameTests {
    private MithrilRecastCooldownGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void boundBowRecastConsumesRetainedSourceAndCanBeCleared(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var fixture = new MithrilCastFixture(helper)) {
                var player = fixture.player;
                var magicData = fixture.magicData;
                var staff = fixture.staff;
                var staffItem = fixture.staffItem;
                var grimoire = new ItemStack(ItemRegistry.ARCHIVISTS_GRIMOIRE.get());
                ArchivistsGrimoire.setUpgradeCount(grimoire, 1);
                new ArchivistsGrimoire.ScrollInventory(grimoire, helper.getLevel().registryAccess())
                        .setStackInSlot(0, createSpellScroll(SpellRegistry.BOUND_BOW.get()));
                equipCurio(player, Curios.SPELLBOOK_SLOT, grimoire);
                magicData.getSyncedData().setSpellSelection(new SpellSelection(
                        Curios.SPELLBOOK_SLOT,
                        0
                ));
                magicData.getPlayerCooldowns().removeCooldown(SpellRegistry.BOUND_BOW.get().getSpellId());
                helper.assertTrue(staffItem.tryTriggerSpellOnSwing(player, InteractionHand.MAIN_HAND, true),
                        "Mithril Freecast Staff should immediately trigger silver-ring Bound Bow from the spellbook slot");
                var boundBowRecast = magicData.getPlayerRecasts().getRecastInstance(SpellRegistry.BOUND_BOW.get().getSpellId());
                helper.assertTrue(boundBowRecast != null,
                        "Mithril Freecast Staff silver-ring Bound Bow should create a recast before cooldown");
                magicData.getPlayerRecasts().removeRecast(boundBowRecast, RecastResult.USED_ALL_RECASTS);
                var expectedBoundBowCooldown = expectedCooldown(
                        player,
                        SpellRegistry.BOUND_BOW.get(),
                        CastSource.SPELLBOOK
                );
                var actualBoundBowCooldown = magicData.getPlayerCooldowns()
                        .getSpellCooldowns()
                        .get(SpellRegistry.BOUND_BOW.get().getSpellId());
                helper.assertTrue(
                        actualBoundBowCooldown != null
                                && actualBoundBowCooldown.getCooldownRemaining() == expectedBoundBowCooldown,
                        "Mithril Freecast Staff should keep the selected source cooldown for Bound Bow recast cooldown but got "
                                + (actualBoundBowCooldown == null ? "none" : actualBoundBowCooldown.getCooldownRemaining())
                                + " / expected " + expectedBoundBowCooldown
                );
                MithrilFreecastStaffCastContext.retainUntilCooldown(
                        player.getUUID(),
                        staff,
                        SpellRegistry.BOUND_BOW.get(),
                        CastSource.SPELLBOOK
                );
                magicData.setPlayerCastingItem(grimoire.copy());
                var retainedRecastCooldownEvent = new SpellCooldownAddedEvent.Pre(
                        MagicManager.getEffectiveSpellCooldown(SpellRegistry.BOUND_BOW.get(), player, CastSource.SWORD),
                        SpellRegistry.BOUND_BOW.get(),
                        player,
                        CastSource.SWORD
                );
                NeoForge.EVENT_BUS.post(retainedRecastCooldownEvent);
                helper.assertTrue(retainedRecastCooldownEvent.getEffectiveCooldown() == expectedBoundBowCooldown,
                        "Mithril Freecast Staff should consume retained recast cooldown source without relying on current casting item but got "
                                + retainedRecastCooldownEvent.getEffectiveCooldown()
                                + " / expected " + expectedBoundBowCooldown);
                helper.assertTrue(MithrilFreecastStaffCastContext.resolveCooldownSource(
                        player.getUUID(),
                        grimoire,
                        SpellRegistry.BOUND_BOW.get()
                ).isEmpty(), "Mithril Freecast Staff should clear resolved retained recast cooldown source");
                MithrilFreecastStaffCastContext.retainUntilCooldown(
                        player.getUUID(),
                        staff,
                        SpellRegistry.BOUND_BOW.get(),
                        CastSource.SPELLBOOK
                );
                MithrilFreecastStaffCastContext.clearPendingCooldownSource(
                        player.getUUID(),
                        grimoire,
                        SpellRegistry.BOUND_BOW.get()
                );
                helper.assertTrue(MithrilFreecastStaffCastContext.resolveCooldownSource(
                        player.getUUID(),
                        grimoire,
                        SpellRegistry.BOUND_BOW.get()
                ).isEmpty(), "Mithril Freecast Staff should clear retained pending cooldown source without relying on current casting item");

            }
        });
    }

    @GameTest(template = "gametest/basic_floor")
    public static void recastTimeoutClearsRetainedSource(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var fixture = new MithrilCastFixture(helper)) {
                var player = fixture.player;
                var timeoutPlayer = player;
                var timeoutMagicData = MagicData.getPlayerMagicData(timeoutPlayer);
                helper.assertTrue(timeoutMagicData != null,
                        "Mithril Freecast Staff timeout cleanup test could not resolve player magic data");
                var timeoutStaff = new ItemStack(ItemRegistry.MITHRIL_FREECAST_STAFF.get());
                MithrilFreecastStaffCastContext.retainUntilCooldown(
                        timeoutPlayer.getUUID(),
                        timeoutStaff,
                        SpellRegistry.BOUND_BOW.get(),
                        CastSource.SPELLBOOK
                );
                var timeoutBoundBowRecast = new RecastInstance(
                        SpellRegistry.BOUND_BOW.get().getSpellId(),
                        1,
                        2,
                        20,
                        CastSource.SWORD,
                        null
                );
                timeoutMagicData.getPlayerRecasts().forceAddRecast(timeoutBoundBowRecast);
                timeoutMagicData.getPlayerRecasts().removeRecast(timeoutBoundBowRecast, RecastResult.TIMEOUT);
                var swordBoundBowCooldown = MagicManager.getEffectiveSpellCooldown(SpellRegistry.BOUND_BOW.get(), timeoutPlayer, CastSource.SWORD);
                var timeoutExpectedBoundBowCooldown = expectedCooldown(
                        timeoutPlayer,
                        SpellRegistry.BOUND_BOW.get(),
                        CastSource.SPELLBOOK
                );
                helper.assertTrue(timeoutExpectedBoundBowCooldown != swordBoundBowCooldown,
                        "Mithril Freecast Staff timeout cleanup test needs SPELLBOOK and SWORD cooldowns to differ");
                timeoutMagicData.setPlayerCastingItem(new ItemStack(Items.STICK));
                var staleTimeoutCooldownEvent = new SpellCooldownAddedEvent.Pre(
                        swordBoundBowCooldown,
                        SpellRegistry.BOUND_BOW.get(),
                        timeoutPlayer,
                        CastSource.SWORD
                );
                NeoForge.EVENT_BUS.post(staleTimeoutCooldownEvent);
                helper.assertTrue(staleTimeoutCooldownEvent.getEffectiveCooldown() == swordBoundBowCooldown,
                        "Mithril Freecast Staff should clear retained source after recast timeout but got "
                                + staleTimeoutCooldownEvent.getEffectiveCooldown() + " / expected " + swordBoundBowCooldown);

            }
        });
    }

    private static int expectedCooldown(Player player, AbstractSpell spell,
                                        CastSource source) {
        // 選択元の基準値に詠唱時間を一度だけ加える。検証対象のstaff計算を期待値に呼び返さない。
        return WeaponImbueCooldownHelper.getEffectiveSpellCooldown(spell, player, source)
                + (spell.getCastType() == CastType.LONG
                ? spell.getEffectiveCastTime(1, player) : 0);
    }
}
