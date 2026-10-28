package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import io.redspace.irons_artifice.api.AmmoEvent;
import io.redspace.irons_artifice.data.ShotComponents;
import io.redspace.irons_artifice.entity.Bullet;
import io.redspace.irons_artifice.item.FireOutcome;
import io.redspace.irons_artifice.item.GunItem;
import io.redspace.irons_artifice.item.GunplayManager;
import io.redspace.irons_artifice.item.MagazineContents;
import io.redspace.irons_artifice.menu.GunContainer;
import java.util.UUID;
import java.util.function.Consumer;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.registry.ApprenticeAttributeRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.eventbus.api.EventPriority;

final class MagiAgentSuitArtificeGameTestScenarios {
    private MagiAgentSuitArtificeGameTestScenarios() {}

    static void hoodComposesWithModifiers(GameTestHelper h) {
        var player = player(h);
        var gun = gun();
        try (var ignored = ApprenticeCodexServerConfig.useMagiAgentSuitAmmoConfigOverrideForGameTest(0.5, false)) {
            assertChance(h, player, gun, 1);
            player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
            assertChance(h, player, gun, 1);
            player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_HOOD.get()));
            assertChance(h, player, gun, 0.5);
            // 同じ銃を再構成しても、前回の補正を蓄積しない。
            assertChance(h, player, gun, 0.5);
            new GunContainer(gun).setItem(0, artifice("enchanted_bullet_modifier"));
            assertChance(h, player, gun, 0.4375);
            try (var zero = ApprenticeCodexServerConfig.useMagiAgentSuitAmmoConfigOverrideForGameTest(0, false)) {
                assertChance(h, player, gun, 0.875);
            }
            assertChance(h, player, gun, 0.4375);
            player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            assertChance(h, player, gun, 0.875);
            player.getAttribute(ApprenticeAttributeRegistry.SPELLGUN_AMMO_CONSERVATION.get()).addTransientModifier(
                    new AttributeModifier(UUID.fromString("43c53b3b-7428-409c-b870-6cf8ef08a857"), "gametest_artifice_ring",
                            -0.5, AttributeModifier.Operation.MULTIPLY_TOTAL));
            assertChance(h, player, gun, 0.4375);
            player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_HOOD.get()));
            assertChance(h, player, gun, 0.21875);
        } finally { player.discard(); }
        h.succeed();
    }

    static void nativeRefundPreservesMagazine(GameTestHelper h) {
        var player = player(h);
        var gun = gun();
        player.setItemInHand(InteractionHand.MAIN_HAND, gun);
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_HOOD.get()));
        int[] refunded = {0};
        Consumer<AmmoEvent.Consume> observer = event -> {
            if (event.getEntity() == player && event.isCanceled()) refunded[0]++;
        };
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, true, AmmoEvent.Consume.class, observer);
        try (var ignored = ApprenticeCodexServerConfig.useMagiAgentSuitAmmoConfigOverrideForGameTest(1, false)) {
            h.assertTrue(GunplayManager.tryFire(player, new Vec3(0, 0, 1)) == FireOutcome.EMPTY_MAGAZINE,
                    "Hood must not allow firing an empty magazine");
            MagazineContents.set(gun, new MagazineContents(1));
            // 上流の消費抽選を実行しつつ、乱数由来のテスト不安定化を避ける。
            player.getRandom().setSeed(12345L);
            h.assertTrue(GunplayManager.tryFire(player, new Vec3(0, 0, 1)).fired(), "Loaded gun must fire");
            h.assertTrue(MagazineContents.get(gun).count() == 1, "Native refund must preserve magazine ammo");
            h.assertTrue(refunded[0] == 1, "Native consumption event must be canceled once");
            h.assertTrue(!h.getLevel().getEntitiesOfClass(Bullet.class, AABB.ofSize(player.position(), 40, 40, 40),
                    bullet -> bullet.getOwner() == player).isEmpty(), "Refunded shot must still spawn a bullet");
            h.assertTrue(player.lastActionbar != null
                    && player.lastActionbar.getContents() instanceof TranslatableContents message
                    && message.getKey().equals("irons_artifice.tooltip.refunded_ammo"),
                    "Hood must use the native ammo refund notification without an Enchanted Bullet modifier");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(observer);
            h.getLevel().getEntitiesOfClass(Bullet.class, AABB.ofSize(player.position(), 40, 40, 40),
                    bullet -> bullet.getOwner() == player).forEach(Bullet::discard);
            player.discard();
        }
        h.succeed();
    }

    private static void assertChance(GameTestHelper h, FakePlayer player, ItemStack gun, double expected) {
        double actual = GunplayManager.compose(player, ((GunItem) gun.getItem()).getGun(), gun)
                .value(ShotComponents.AMMO_CONSUME_CHANCE);
        h.assertTrue(Math.abs(actual - expected) < 0.000001,
                "Ammo consume chance mismatch: expected=" + expected + " actual=" + actual);
    }

    private static ItemStack artifice(String path) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("irons_artifice", path)));
    }

    private static ItemStack gun() {
        var gun = artifice("flintlock");
        MagazineContents.set(gun, new MagazineContents(0));
        return gun;
    }

    private static TestPlayer player(GameTestHelper h) {
        var player = new TestPlayer(h.getLevel());
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        player.setPos(h.absoluteVec(new Vec3(1, 30, 1)));
        h.getLevel().addFreshEntity(player);
        return player;
    }

    private static final class TestPlayer extends FakePlayer {
        private Component lastActionbar;

        private TestPlayer(ServerLevel level) {
            super(level, new GameProfile(UUID.randomUUID(), "hood_artifice_test"));
        }

        @Override
        public void displayClientMessage(Component message, boolean actionbar) {
            if (actionbar) lastActionbar = message;
        }
    }
}
