package jp.aquafactory.apprenticecodex.gametest.equipment.mithrilfreecaststaff;

import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.WeaponImbueCooldownHelper;
import jp.aquafactory.apprenticecodex.item.mithrilfreecaststaff.MithrilFreecastStaffCastContext;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.support.CurioTestSupport.equipRingCurio;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class MithrilEquipmentCooldownGameTests {
    private MithrilEquipmentCooldownGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void equipmentDiscountsApplyBeforeLongCastTimeExactlyOnce(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var fixture = new MithrilCastFixture(helper)) {
                var player = fixture.player;
                var magicData = fixture.magicData;
                var staff = fixture.staff;
                equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));
                var harvestMoon = SpellRegistry.HARVEST_MOON.get();
                magicData.setPlayerCastingItem(staff.copy());
                try (var ignored = MithrilFreecastStaffCastContext.open(
                        player.getUUID(),
                        staff,
                        harvestMoon,
                        CastSource.SPELLBOOK
                )) {
                    var cooldownEvent = new SpellCooldownAddedEvent.Pre(
                            MagicManager.getEffectiveSpellCooldown(harvestMoon, player, CastSource.SWORD),
                            harvestMoon,
                            player,
                            CastSource.SWORD
                    );
                    NeoForge.EVENT_BUS.post(cooldownEvent);
                    var expectedCooldown = expectedCooldown(
                            player,
                            harvestMoon,
                            CastSource.SPELLBOOK
                    );
                    helper.assertTrue(cooldownEvent.getEffectiveCooldown() == expectedCooldown,
                            "Mithril Freecast Staff should keep CraftsmansDelight on the selected SPELLBOOK cooldown but got "
                                    + cooldownEvent.getEffectiveCooldown() + " / expected " + expectedCooldown);
                }

                player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_BOOTS.get()));
                var thermalProcess = SpellRegistry.THERMAL_PROCESS.get();
                magicData.setPlayerCastingItem(staff.copy());
                try (var ignored = MithrilFreecastStaffCastContext.open(
                        player.getUUID(),
                        staff,
                        thermalProcess,
                        CastSource.SPELLBOOK
                )) {
                    var cooldownEvent = new SpellCooldownAddedEvent.Pre(
                            MagicManager.getEffectiveSpellCooldown(thermalProcess, player, CastSource.SWORD),
                            thermalProcess,
                            player,
                            CastSource.SWORD
                    );
                    NeoForge.EVENT_BUS.post(cooldownEvent);
                    var expectedCooldown = expectedCooldown(
                            player,
                            thermalProcess,
                            CastSource.SPELLBOOK
                    );
                    helper.assertTrue(cooldownEvent.getEffectiveCooldown() == expectedCooldown,
                            "Mithril Freecast Staff should keep Thermal Process on the selected SPELLBOOK cooldown with Magi boots and CraftsmansDelight but got "
                                    + cooldownEvent.getEffectiveCooldown() + " / expected " + expectedCooldown);
                }

                var artisanSmash = SpellRegistry.ARTISAN_SMASH.get();
                magicData.setPlayerCastingItem(staff.copy());
                try (var ignored = MithrilFreecastStaffCastContext.open(
                        player.getUUID(),
                        staff,
                        artisanSmash,
                        CastSource.SPELLBOOK
                )) {
                    var cooldownEvent = new SpellCooldownAddedEvent.Pre(
                            MagicManager.getEffectiveSpellCooldown(artisanSmash, player, CastSource.SWORD),
                            artisanSmash,
                            player,
                            CastSource.SWORD
                    );
                    NeoForge.EVENT_BUS.post(cooldownEvent);
                    var expectedCooldown = expectedCooldown(
                            player,
                            artisanSmash,
                            CastSource.SPELLBOOK
                    );
                    helper.assertTrue(cooldownEvent.getEffectiveCooldown() == expectedCooldown,
                            "Mithril Freecast Staff should not re-reduce Magi boots cooldown after adding long cast time but got "
                                    + cooldownEvent.getEffectiveCooldown() + " / expected " + expectedCooldown);
                }

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
