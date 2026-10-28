package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import io.redspace.irons_artifice.data.RecoilState;
import io.redspace.irons_artifice.data.ShotComponents;
import io.redspace.irons_artifice.gun.Guns;
import io.redspace.irons_artifice.item.GunItem;
import io.redspace.irons_artifice.item.GunplayManager;
import io.redspace.irons_artifice.item.MagazineContents;
import io.redspace.irons_artifice.menu.GunContainer;
import io.redspace.irons_artifice.utils.RecoilHelper;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import java.util.ArrayList;
import java.util.UUID;
import jp.aquafactory.apprenticecodex.compat.ironsartifice.ManaStabilizerModuleEvents;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.item.ManaStabilizerModuleServerConfig.Values;
import jp.aquafactory.apprenticecodex.config.item.ManaStabilizerModuleServerConfig;
import jp.aquafactory.apprenticecodex.item.modifier.manastabilizermodule.ManaStabilizerModuleModifier;
import jp.aquafactory.apprenticecodex.item.modifier.manastabilizermodule.ManaStabilizerRecovery;
import jp.aquafactory.apprenticecodex.mixin.ManaStabilizerMixinPlugin;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

final class ManaStabilizerModuleGameTestScenarios {
    private ManaStabilizerModuleGameTestScenarios() {}
    private static FakePlayer player(GameTestHelper h) {
        var player = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "stabilizer_test"));
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        player.setPos(h.absoluteVec(new Vec3(1, 30, 1)));
        h.getLevel().addFreshEntity(player);
        return player;
    }
    private static ItemStack gun(FakePlayer player, int count) {
        var stack = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("irons_artifice", "clockwork_rifle")));
        var container = new GunContainer(stack);
        for (int i = 0; i < count; i++) container.setItem(i, ItemRegistry.MANA_STABILIZER_MODULE.get().getDefaultInstance());
        container.setChanged();
        MagazineContents.set(stack, new MagazineContents(10));
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return stack;
    }
    private static void close(GameTestHelper h, double actual, double expected, String message) {
        h.assertTrue(Math.abs(actual - expected) < 0.001, message + ": expected=" + expected + " actual=" + actual);
    }
    static void lostInputExpires(GameTestHelper h) {
        var player = player(h);
        gun(player, 1);
        ManaStabilizerModuleEvents.input(player, true, false, 0);
        h.assertTrue(ManaStabilizerModuleEvents.active(player), "ADS must start without a recovery hook");
        h.runAfterDelay(10, () -> {
            try {
                ManaStabilizerModuleEvents.onTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
                h.assertTrue(!ManaStabilizerModuleEvents.active(player), "ADS must expire after ten ticks without input");
                close(h, player.getAttributeValue(Attributes.MOVEMENT_SPEED), player.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue(), "Expired ADS must remove movement penalty");
                h.succeed();
            } finally { player.discard(); }
        });
    }
    static void adsAndPriority(GameTestHelper h) throws Exception {
        var player = player(h);
        try (var ignored = ApprenticeCodexServerConfig.overrideManaStabilizerModuleForGameTest(ManaStabilizerModuleServerConfig.DEFAULTS)) {
            for (int n = 1; n <= 3; n++) {
                var stack = gun(player, n);
                var gun = (GunItem) stack.getItem();
                var base = GunplayManager.compose(player, gun.getGun(), stack);
                ManaStabilizerModuleEvents.input(player, true, true, n);
                var ads = GunplayManager.compose(player, gun.getGun(), stack);
                close(h, ads.value(ShotComponents.CAMERA_RECOIL_MULTIPLIER), Math.pow(0.75, n), "ADS recoil must stack multiplicatively");
                close(h, ads.value(ShotComponents.SPREAD), base.value(ShotComponents.SPREAD) - 0.5 * n, "ADS spread must retain its negative sign");
                close(h, player.getAttributeValue(Attributes.MOVEMENT_SPEED) / player.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue(), 0.7, "Movement must not stack");
                stack.getOrCreateTag().putBoolean(GunItem.GUN_SPYGLASS_TAG_KEY, true);
                h.assertTrue(!ManaStabilizerModuleEvents.active(player), "Scope must suppress stabilization");
                stack.removeTagKey(GunItem.GUN_SPYGLASS_TAG_KEY);
                var container = new GunContainer(stack);
                container.setItem(n, new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("irons_artifice", "bayonet_attachment_modifier"))));
                container.setChanged();
                h.assertTrue(!ManaStabilizerModuleEvents.eligible(player), "Bayonet must suppress stabilization");
                ManaStabilizerModuleEvents.input(player, false, false, n);
                close(h, player.getAttributeValue(Attributes.MOVEMENT_SPEED), player.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue(), "ADS release must remove slowdown");
            }
            gun(player, 1);
            ManaStabilizerModuleEvents.input(player, true, true, 4);
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            ManaStabilizerModuleEvents.onTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
            h.assertTrue(!ManaStabilizerModuleEvents.active(player), "Switching items must clear ADS");
        } finally { player.discard(); }
        h.succeed();
    }
    static void paymentAndFireOrdering(GameTestHelper h) throws Exception {
        var player = player(h);
        try (var ignored = ApprenticeCodexServerConfig.overrideManaStabilizerModuleForGameTest(ManaStabilizerModuleServerConfig.DEFAULTS)) {
            gun(player, 2);
            var magic = MagicData.getPlayerMagicData(player);
            long now = h.getLevel().getGameTime();
            for (boolean hook : new boolean[]{false, true}) {
                magic.setMana(1);
                RecoilState.set(player, new RecoilState(3, 4, now));
                ManaStabilizerModuleEvents.input(player, true, hook, 0);
                ManaStabilizerModuleEvents.onTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
                close(h, magic.getMana(), 1, "Insufficient mana and disabled hooks must not charge");
                close(h, RecoilState.current(player, h.getLevel().getGameTime()).pitch(), 3, "Unpaid recovery must retain normal recoil");
            }
            magic.setMana(2);
            ManaStabilizerModuleEvents.onTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
            close(h, magic.getMana(), 0, "Two modules must charge two mana per assisted tick");
            close(h, RecoilState.current(player, h.getLevel().getGameTime()).pitch(), 2.1, "Recovery must scale with count");
            RecoilState.set(player, new RecoilState(0.3f, 0.4f, now));
            ManaStabilizerModuleEvents.onTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
            close(h, RecoilState.current(player, h.getLevel().getGameTime()).pitch(), 0, "Final recovery must be free");
            var stack = gun(player, 1);
            var container = new GunContainer(stack);
            container.setItem(1, ItemRegistry.COMMENCE_MAGI_MODULE.get().getDefaultInstance());
            container.setChanged();
            magic.setMana(30);
            ManaStabilizerModuleEvents.input(player, true, true, 0);
            h.assertTrue(GunplayManager.tryFire(player, new Vec3(0, 0, 1)).fired(), "Existing firing cost must be affordable before assistance");
            close(h, magic.getMana(), 0, "Existing firing cost must be charged first");
            var recoil = RecoilState.current(player, h.getLevel().getGameTime());
            ManaStabilizerModuleEvents.onTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
            close(h, RecoilState.current(player, h.getLevel().getGameTime()).pitch(), recoil.pitch(), "Assistance must not run when firing exhausted mana");
            h.assertTrue(!GunplayManager.tryFire(player, new Vec3(0, 0, 1)).fired(), "Immediate repeated shot must fail its firing gate");
            close(h, magic.getMana(), 0, "Failed shots must not add mana costs");
            close(h, RecoilState.current(player, h.getLevel().getGameTime()).pitch(), recoil.pitch(), "Failed shots must not add recoil");
        } finally { player.discard(); }
        h.succeed();
    }
    static void clockworkRecoveryAndTooltip(GameTestHelper h) throws Exception {
        // server上でclientクラスをロードせず、配布jarのhook構造が有効なことを検証する。
        try (var input = ManaStabilizerModuleGameTestScenarios.class.getClassLoader().getResourceAsStream(
                "io/redspace/irons_artifice/client/RecoilManager.class")) {
            h.assertTrue(input != null, "Client recoil bytecode must be available");
            var node = new ClassNode();
            new ClassReader(input).accept(node, 0);
            h.assertTrue(ManaStabilizerMixinPlugin.compatible(node), "Supported Iron's Arms must enable the recovery hook");
        }
        var player = player(h);
        try (var ignored = ApprenticeCodexServerConfig.overrideManaStabilizerModuleForGameTest(ManaStabilizerModuleServerConfig.DEFAULTS)) {
            var stack = gun(player, 1);
            ManaStabilizerModuleEvents.input(player, true, true, 0);
            var profile = GunplayManager.compose(player, Guns.CLOCKWORK_RIFLE, stack);
            for (int index = 0; index < 10; index++) {
                MagazineContents.set(stack, new MagazineContents(10 - index));
                profile = GunplayManager.compose(player, Guns.CLOCKWORK_RIFLE, stack);
                Vec2 recoil = RecoilHelper.calculateCameraRecoil(profile);
                for (int tick = 0; tick < 4; tick++) recoil = ManaStabilizerRecovery.recover(recoil.x * 0.75f, recoil.y * 0.75f, 0.75, true);
                h.assertTrue(recoil.lengthSquared() == 0, "Every clockwork round must recover within four ticks");
                Vec2 impulse = RecoilHelper.calculateCameraRecoil(profile);
                Vec2 rapid = ManaStabilizerRecovery.recover(impulse.x * 0.75f, impulse.y * 0.75f, 0.75, true);
                h.assertTrue(rapid.lengthSquared() > 0, "One-tick firing intervals may retain recoil");
            }
            var lines = new ArrayList<Component>();
            new ManaStabilizerModuleModifier().getDescriptionText(lines::add);
            h.assertTrue(lines.size() == 6, "All six description lines must always be present");
            h.assertTrue(lines.get(0).getStyle().getColor().getValue() == ChatFormatting.AQUA.getColor(), "First line must be aqua");
            var recoilText = (TranslatableContents) lines.get(2).getContents();
            var spreadText = (TranslatableContents) lines.get(3).getContents();
            var movementText = (TranslatableContents) lines.get(4).getContents();
            h.assertTrue(recoilText.getArgs()[0].equals("75") && spreadText.getArgs()[0].equals("-0.5") && movementText.getArgs()[0].equals("70"), "Tooltip values must use the requested units and signs");
            try (var override = ApprenticeCodexServerConfig.overrideManaStabilizerModuleForGameTest(new Values(0.8, 0.5, -1, 0, 0))) {
                ManaStabilizerModuleEvents.input(player, true, true, 0);
                close(h, player.getAttributeValue(Attributes.MOVEMENT_SPEED) / player.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue(), 0.8, "Config changes must update movement");
                var changed = GunplayManager.compose(player, Guns.CLOCKWORK_RIFLE, stack);
                close(h, changed.value(ShotComponents.CAMERA_RECOIL_MULTIPLIER), 0.5, "Config changes must update recoil");
            }
        } finally { player.discard(); }
        h.succeed();
    }
}
