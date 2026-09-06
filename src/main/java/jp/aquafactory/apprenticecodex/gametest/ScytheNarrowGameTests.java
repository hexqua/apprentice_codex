package jp.aquafactory.apprenticecodex.gametest;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.malum.MalumSpellReaperScytheBridge;
import jp.aquafactory.apprenticecodex.item.spellreaperscythe.ScytheThrowEntity;
import jp.aquafactory.apprenticecodex.item.spellreaperscythe.ScytheThrowManager;
import jp.aquafactory.apprenticecodex.network.packet.ScytheRecallEffectPacket;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.utility.RaycastTools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ScytheNarrowGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";

    static void equip(FakePlayer player, String id) {
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("malum", id));
        CuriosApi.getCuriosInventory(player).orElseThrow().setEquippedCurio("necklace", 0, new ItemStack(item));
    }

    static void unequip(FakePlayer player) {
        CuriosApi.getCuriosInventory(player).orElseThrow().setEquippedCurio("necklace", 0, ItemStack.EMPTY);
    }

    @GameTest(template = TEMPLATE)
    public static void movingPlateUsesOnlyYawAndFindsFirstContact(GameTestHelper h) {
        for (float yaw : new float[]{0, 45, 90}) {
            var plate = ScytheThrowEntity.narrowBox(Vec3.ZERO, yaw);
            var forward = plate.forward();
            var right = new Vec3(forward.z, 0, -forward.x);
            var near = new AABB(-0.01, -0.01, -0.01, 0.01, 0.01, 0.01);
            var path = forward.scale(20);
            var contact = RaycastTools.firstMovingHorizontalBoxContact(near.move(forward.scale(10)), plate, path);
            h.assertTrue(contact.isPresent() && contact.getAsDouble() > 0.4 && contact.getAsDouble() < 0.5,
                    "Fast plate must contact between endpoints at yaw " + yaw);
            h.assertFalse(RaycastTools.firstMovingHorizontalBoxContact(near.move(forward.scale(10).add(right.scale(0.2))), plate, path).isPresent(),
                    "Thin side must miss at yaw " + yaw);
            h.assertTrue(RaycastTools.firstMovingHorizontalBoxContact(near.move(0, 10, 0), plate, new Vec3(0, 20, 0)).isPresent(),
                    "Upward travel must preserve a finite yaw-only plate");
            h.assertTrue(RaycastTools.firstMovingHorizontalBoxContact(near.move(0, -10, 0), plate, new Vec3(0, -20, 0)).isPresent(),
                    "Downward travel must preserve a finite yaw-only plate");
            h.assertFalse(RaycastTools.firstMovingHorizontalBoxContact(near.move(forward.scale(10).add(0, 1.52, 0)), plate, path).isPresent(),
                    "Plate height must remain three blocks");
        }
        var diagonal = ScytheThrowEntity.narrowBox(Vec3.ZERO, 45);
        h.assertFalse(RaycastTools.firstMovingHorizontalBoxContact(new AABB(0.8, 0, 0.8, 0.9, 0.1, 0.9), diagonal, Vec3.ZERO).isPresent(),
                "Diagonal broad-phase corner must not intersect the plate");
        h.assertTrue(RaycastTools.firstMovingHorizontalBoxContact(new AABB(0.05, 0, 0, 0.1, 0.1, 0.1),
                ScytheThrowEntity.narrowBox(Vec3.ZERO, 0), Vec3.ZERO).isPresent(), "Exact plate boundary must count as contact");
        h.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void narrowUsesThirtyTwoBlocksAndCapturesEquipment(GameTestHelper h) {
        if (!MalumSpellReaperScytheBridge.isAvailable()) { h.succeed(); return; }
        var p = ScytheReboundGameTests.player(h, 100);
        ScytheReboundGameTests.enchant(h, p.getMainHandItem(), "rebound", 1);
        equip(p, "necklace_of_the_narrow_edge");
        var origin = p.getEyePosition();
        p.setYRot(45);
        ScytheReboundGameTests.use(h, p);
        var entity = ScytheThrowManager.active(p);
        h.assertTrue(entity != null && entity.isNarrow(), "Equipped Narrow Edge must select the narrow mode");
        try {
            unequip(p);
            p.setYRot(90);
            for (int i = 0; i < 20; i++) entity.tick();
            h.assertTrue(entity.isRemoved() && entity.isNarrow() && entity.getThrowYaw() == 45,
                    "Removing equipment or turning must not change an active throw");
            h.assertTrue(Math.abs(entity.position().distanceTo(origin) - 32) < 0.001,
                    "Narrow must reach thirty-two blocks without the old twenty-four-block recall");
        } finally { entity.discard(); }
        equip(p, "necklace_of_the_hidden_blade");
        h.assertFalse(MalumSpellReaperScytheBridge.hasNarrowEdge(p), "Hidden Blade must not activate Narrow Edge");
        h.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void narrowRecallsAtFirstEnemyWithoutDoubleHit(GameTestHelper h) {
        if (!MalumSpellReaperScytheBridge.isAvailable()) { h.succeed(); return; }
        var p = ScytheReboundGameTests.player(h, 100);
        p.setXRot(0);
        equip(p, "necklace_of_the_narrow_edge");
        ScytheReboundGameTests.enchant(h, p.getMainHandItem(), "rebound", 1);
        // 遠い敵を先に登録し、Entity列挙順に依存せず手前で折り返すことを確認する。
        var far = h.spawn(EntityType.HUSK, new BlockPos(2, 20, 6));
        var near = h.spawn(EntityType.HUSK, new BlockPos(2, 20, 5));
        for (var target : new net.minecraft.world.entity.monster.Husk[]{far, near}) {
            target.setNoAi(true); target.setNoGravity(true);
            target.getAttribute(Attributes.ARMOR).setBaseValue(0);
            target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
            target.setHealth(100);
        }
        ScytheReboundGameTests.use(h, p);
        var entity = ScytheThrowManager.active(p);
        try {
            entity.tick();
            h.assertTrue(entity.isRemoved() && !entity.isHovering(), "First contact must recall in the same tick");
            h.assertTrue(Math.abs(near.getHealth() - 87) < 0.01,
                    "Narrow must apply the captured 1.3 multiplier once; health=" + near.getHealth());
            h.assertTrue(far.getHealth() == 100, "Enemies beyond the first contact must not be hit");
            h.assertTrue(entity.getZ() < near.getZ(), "Recall must originate at contact, not at the next tick endpoint");
        } finally { entity.discard(); }
        h.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void narrowDoesNotChangeNormalThrow(GameTestHelper h) {
        if (!MalumSpellReaperScytheBridge.isAvailable()) { h.succeed(); return; }
        var p = ScytheReboundGameTests.player(h, 200);
        equip(p, "necklace_of_the_narrow_edge");
        ScytheReboundGameTests.use(h, p);
        h.assertTrue(p.isUsingItem() && ScytheThrowManager.active(p) == null, "Narrow alone must still charge");
        h.runAfterDelay(10, () -> {
            p.releaseUsingItem();
            var entity = ScytheThrowManager.active(p);
            h.assertTrue(entity != null && entity.getMode() == ScytheThrowEntity.Mode.NORMAL, "Unenchanted Narrow must use normal mode");
            try {
                for (int i = 0; i < 8; i++) entity.tick();
                h.assertTrue(entity.isHovering() && Math.abs(entity.position().distanceTo(p.getEyePosition()) - 2.5) < 0.001,
                        "Normal Narrow throw must keep charge range and hovering");
            } finally { entity.discard(); }
            h.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void narrowRecallPacketPreservesPose(GameTestHelper h) {
        var packet = new ScytheRecallEffectPacket(Vec3.ZERO, new Vec3(0, 32, 0), 0x123456, true, 45);
        var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), h.getLevel().registryAccess());
        try {
            ScytheRecallEffectPacket.STREAM_CODEC.encode(buffer, packet);
            h.assertTrue(ScytheRecallEffectPacket.STREAM_CODEC.decode(buffer).equals(packet), "Recall packet must preserve vertical mode and yaw");
        } finally { buffer.release(); }
        h.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void canceledImpactContinuesButInvulnerableContactRecalls(GameTestHelper h) {
        if (!MalumSpellReaperScytheBridge.isAvailable()) { h.succeed(); return; }
        var p = ScytheReboundGameTests.player(h, 200);
        p.setXRot(0);
        equip(p, "necklace_of_the_narrow_edge");
        ScytheReboundGameTests.enchant(h, p.getMainHandItem(), "rebound", 1);
        var target = h.spawn(EntityType.HUSK, new BlockPos(2, 20, 5));
        target.setNoAi(true); target.setNoGravity(true);
        target.getAttribute(Attributes.ARMOR).setBaseValue(0);
        java.util.function.Consumer<net.neoforged.neoforge.event.entity.ProjectileImpactEvent> listener = event -> {
            if (event.getProjectile() instanceof ScytheThrowEntity scythe && scythe.getOwner() == p) event.setCanceled(true);
        };
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(listener);
        try {
            ScytheReboundGameTests.use(h, p);
            var entity = ScytheThrowManager.active(p);
            try {
                entity.tick();
                h.assertFalse(entity.isRemoved(), "Canceled impact must not trigger Narrow recall");
                h.assertTrue(target.getHealth() == target.getMaxHealth(), "Canceled impact must not deal damage");
            } finally { entity.discard(); }
        } finally { net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(listener); }
        target.invulnerableTime = 20;
        ((jp.aquafactory.apprenticecodex.mixin.LivingEntityDamageMemoryAccessor) target).apprenticecodex$setLastHurt(100);
        ScytheReboundGameTests.use(h, p);
        var entity = ScytheThrowManager.active(p);
        try {
            entity.tick();
            h.assertTrue(entity.isRemoved() && target.getHealth() == target.getMaxHealth(),
                    "Valid but invulnerable contact must recall without bypassing damage immunity");
        } finally { entity.discard(); }
        h.succeed();
    }
}
