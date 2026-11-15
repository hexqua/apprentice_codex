package jp.aquafactory.apprenticecodex.gametest.equipment.magiengineerslegexoskeleton;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.compat.magitech.ExoskeletonTooltip;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.curios.magiengineerslegexoskeleton.ExoskeletonAction;
import jp.aquafactory.apprenticecodex.item.curios.magiengineerslegexoskeleton.ExoskeletonConfigState;
import jp.aquafactory.apprenticecodex.item.curios.magiengineerslegexoskeleton.ExoskeletonMovement;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.stln.magitech.content.item.ItemInit;
import net.stln.magitech.content.item.armor.FlamglideStriderItem;
import net.stln.magitech.feature.element.Element;

import java.util.ArrayList;

import static jp.aquafactory.apprenticecodex.gametest.support.CurioTestSupport.equipCurio;
import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;

final class ExoskeletonMagitechScenarios {
    private ExoskeletonMagitechScenarios() {}

    static void movement(GameTestHelper h) {
        var player = createEquipmentTestPlayer(h, new BlockPos(1, 2, 1), "exo_move");
        var magic = MagicData.getPlayerMagicData(player);
        try (var ignored = ApprenticeCodexServerConfig.useExoskeletonConfigOverrideForGameTest(15, 20)) {
            magic.setMana(50);
            h.assertFalse(ExoskeletonMovement.accept(player, ExoskeletonAction.LIFTER), "Unequipped movement must be rejected");
            equipCurio(player, "feet", new ItemStack(ItemRegistry.MAGI_ENGINEERS_LEG_EXOSKELETON.get()));
            // serverの接地・ダッシュと通知の差を許容する操作契約を確認する。
            player.setOnGround(true);
            player.setSprinting(false);
            player.setDeltaMovement(0.3, -1, 0.4);
            h.assertTrue(ExoskeletonMovement.accept(player, ExoskeletonAction.LIFTER), "Client airborne lifter must work despite server ground state");
            h.assertTrue(player.getDeltaMovement().equals(new Vec3(0.3, 0.8, 0.4)) && magic.getMana() == 35,
                    "First lifter must use upstream motion and spend mana");
            h.assertTrue(ExoskeletonMovement.accept(player, ExoskeletonAction.LIFTER) && magic.getMana() == 20,
                    "Every lifter must spend mana exactly once without a cooldown");
            h.assertTrue(ExoskeletonMovement.accept(player, ExoskeletonAction.AIR_STRIDER) && magic.getMana() == 0,
                    "Client sprint action must accept exact mana despite server sprint state");
            h.assertTrue(Math.abs(player.getDeltaMovement().y - 0.4) < 1.0e-6 && player.fallDistance == -5,
                    "Air strider must apply velocity and fall distance corrections");
            var motion = player.getDeltaMovement();
            h.assertFalse(ExoskeletonMovement.accept(player, ExoskeletonAction.LIFTER), "Insufficient mana must reject server action");
            h.assertTrue(player.getDeltaMovement().equals(motion) && magic.getMana() == 0,
                    "Rejected action must preserve motion and mana");
            magic.setMana(100);
            player.getAbilities().mayfly = true;
            h.assertFalse(ExoskeletonMovement.accept(player, ExoskeletonAction.LIFTER), "Flight ability must disable exoskeleton movement");
            h.assertTrue(magic.getMana() == 100, "Disabled movement must not spend mana");
        }
        h.succeed();
    }

    static void cooldownAndConfig(GameTestHelper h) {
        var player = createEquipmentTestPlayer(h, new BlockPos(1, 2, 1), "exo_cooldown");
        equipCurio(player, "feet", new ItemStack(ItemRegistry.MAGI_ENGINEERS_LEG_EXOSKELETON.get()));
        var magic = MagicData.getPlayerMagicData(player);
        try (var ignored = ApprenticeCodexServerConfig.useExoskeletonConfigOverrideForGameTest(7, 9)) {
            magic.setMana(30);
            h.assertTrue(ExoskeletonMovement.accept(player, ExoskeletonAction.GROUND_STRIDER) && magic.getMana() == 21,
                    "Ground strider must spend the configured mana cost");
            h.assertFalse(ExoskeletonMovement.accept(player, ExoskeletonAction.AIR_STRIDER), "Ground cooldown must also block airborne strider");
            h.assertTrue(ExoskeletonMovement.accept(player, ExoskeletonAction.LIFTER) && magic.getMana() == 14,
                    "Strider cooldown must not block lifter or ignore configured cost");
            equipCurio(player, "feet", ItemStack.EMPTY);
            equipCurio(player, "feet", new ItemStack(ItemRegistry.MAGI_ENGINEERS_LEG_EXOSKELETON.get()));
            for (int i = 0; i < 11; i++) player.getCooldowns().tick();
            h.assertFalse(ExoskeletonMovement.accept(player, ExoskeletonAction.AIR_STRIDER), "Strider must remain blocked through eleven ticks after re-equipping");
            player.getCooldowns().tick();
            h.assertTrue(ExoskeletonMovement.accept(player, ExoskeletonAction.AIR_STRIDER) && magic.getMana() == 5,
                    "Strider must reactivate after twelve ticks using configured cost");
            FlamglideStriderItem.longJump(player, 0, new ItemStack(ItemInit.FLAMGLIDE_STRIDER.get()));
            for (int i = 0; i < 12; i++) player.getCooldowns().tick();
            h.assertTrue(player.getCooldowns().isOnCooldown(ItemInit.FLAMGLIDE_STRIDER.get()), "Native strider must retain its longer cooldown");
            for (int i = 0; i < 8; i++) player.getCooldowns().tick();
            h.assertFalse(player.getCooldowns().isOnCooldown(ItemInit.FLAMGLIDE_STRIDER.get()), "Native strider must finish after twenty ticks");
        }
        try (var ignored = ApprenticeCodexServerConfig.useExoskeletonConfigOverrideForGameTest(0, 0)) {
            magic.setMana(0);
            h.assertTrue(ExoskeletonMovement.accept(player, ExoskeletonAction.LIFTER)
                            && ExoskeletonMovement.accept(player, ExoskeletonAction.AIR_STRIDER),
                    "Zero-cost configuration must allow both functions without mana");
        }
        h.succeed();
    }

    static void tooltip(GameTestHelper h) {
        int previousLifter = ExoskeletonConfigState.lifterManaCost();
        int previousStrider = ExoskeletonConfigState.striderManaCost();
        try {
            ExoskeletonConfigState.set(7, 9);
            var lines = new ArrayList<Component>();
            ExoskeletonTooltip.append(lines, Component.literal("L"), Component.literal("S"));
            var lifter = (TranslatableContents) lines.get(0).getSiblings().getFirst().getContents();
            var cost = (TranslatableContents) lines.get(1).getSiblings().getFirst().getContents();
            var strider = (TranslatableContents) lines.get(2).getSiblings().getFirst().getContents();
            h.assertTrue(((Component) lifter.getArgs()[0]).getString().equals("L")
                            && ((Component) strider.getArgs()[0]).getString().equals("S"), "Tooltip must preserve distinct key bindings");
            h.assertTrue(((Component) lifter.getArgs()[1]).getStyle().getColor().getValue() == (Element.HOLLOW.getTextColor().getRGB() & 0xFFFFFF)
                            && ((Component) strider.getArgs()[1]).getStyle().getColor().getValue() == (Element.EMBER.getTextColor().getRGB() & 0xFFFFFF),
                    "Item names must use the staff elemental colors");
            h.assertTrue(((Component) cost.getArgs()[0]).getString().equals("7")
                            && ((Component) cost.getArgs()[0]).getStyle().getColor().getValue() == ChatFormatting.AQUA.getColor(),
                    "Tooltip must display synchronized mana cost in aqua");
        } finally {
            ExoskeletonConfigState.set(previousLifter, previousStrider);
        }
        h.succeed();
    }
}
