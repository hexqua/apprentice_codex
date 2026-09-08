package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.entity.spells.shield.ShieldEntity;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.antimanaarrow.AntiManaArrowEntity;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import jp.aquafactory.apprenticecodex.item.curios.protectionspellsupporter.ProtectionSpellSupporter;
import jp.aquafactory.apprenticecodex.item.curios.protectionspellsupporter.SupportedShieldEntity;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.Map;
import java.util.UUID;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public class SupportedShieldGameTests extends ApprenticeCodexGameTestScenarios {
    private static final String TEMPLATE = "gametest/basic_floor";

    @GameTest(template = TEMPLATE, batch = "apprenticecodex.supported_shield")
    public static void generatedShieldKeepsBaseProperties(GameTestHelper helper) {
        var player = createAssistWingsRider(helper, new BlockPos(1, 10, 1), "shield_generation");
        var spell = SpellRegistry.SHIELD_SPELL.get();
        var data = MagicData.getPlayerMagicData(player);
        player.setYRot(0);
        player.setXRot(0);
        spell.onCast(helper.getLevel(), 3, player, CastSource.SPELLBOOK, data);
        var box = player.getBoundingBox().inflate(6);
        var normal = helper.getLevel().getEntitiesOfClass(ShieldEntity.class, box).getFirst();
        float health = normal.getHealth();
        var position = normal.position();
        normal.discard();
        equip(player, true);
        spell.onCast(helper.getLevel(), 3, player, CastSource.SPELLBOOK, data);
        var shield = helper.getLevel().getEntitiesOfClass(SupportedShieldEntity.class, box).getFirst();
        helper.assertTrue(shield.getHealth() == health && shield.position().equals(position), "Supported shield must preserve health and placement");
        helper.assertTrue(shield.getParts().length == normal.getParts().length, "Supported shield must preserve multipart geometry");
        helper.assertTrue(shield.getName().equals(normal.getName()), "Supported shield must use the original translated name");
        helper.assertTrue(ProtectionSpellSupporter.isManaCostDiscountTargetSpell(spell.getSpellId()), "Shield must be a discount target");
        helper.assertTrue(ProtectionSpellSupporter.applyManaCostDiscount(40, player) == 20, "Shield discount must halve mana");
        var tag = new CompoundTag();
        shield.saveWithoutId(tag);
        var restored = new SupportedShieldEntity(EntityRegistry.SUPPORTED_SHIELD.get(), helper.getLevel());
        restored.load(tag);
        try (var online = online(player)) {
            helper.assertTrue(restored.getActiveOwner() == player, "Owner UUID must survive serialization");
        }
        var baseline = new ShieldEntity(helper.getLevel(), health);
        for (int tick = 0; tick < 401; tick++) {
            baseline.tick();
            shield.tick();
            helper.assertTrue(baseline.isRemoved() == shield.isRemoved(), "Supported shield lifetime must match the original");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "apprenticecodex.supported_shield")
    public static void passageUsesEquipmentAndServerTeams(GameTestHelper helper) {
        var owner = createAssistWingsRider(helper, new BlockPos(1, 10, 1), "shield_owner");
        var ally = createAssistWingsRider(helper, new BlockPos(1, 10, 1), "shield_ally");
        var server = helper.getLevel().getServer();
        boolean pvp = server.isPvpAllowed();
        var scoreboard = server.getScoreboard();
        var team = scoreboard.addPlayerTeam("shield_" + owner.getUUID().toString().substring(0, 8));
        var shield = shield(helper, owner);
        var start = shield.position().add(0, -0.5, -1);
        try (var online = online(owner)) {
            server.setPvpAllowed(true);
            equip(owner, true);
            helper.assertTrue(shield.allowsPassage(owner), "Equipped owner must pass");
            helper.assertFalse(shield.allowsPassage(ally), "Enemy must not pass in PvP");
            owner.setPos(start);
            owner.move(MoverType.PLAYER, new Vec3(0, 0, 2));
            helper.assertTrue(owner.getZ() > shield.getZ(), "Owner must physically cross the shield");
            ally.setPos(start);
            ally.move(MoverType.PLAYER, new Vec3(0, 0, 2));
            helper.assertTrue(ally.getZ() < shield.getZ(), "Enemy movement must collide with shield parts");
            scoreboard.addPlayerToTeam(owner.getScoreboardName(), team);
            scoreboard.addPlayerToTeam(ally.getScoreboardName(), team);
            team.setAllowFriendlyFire(false);
            helper.assertTrue(shield.allowsPassage(ally), "Protected teammate must pass");
            ally.setPos(start);
            ally.move(MoverType.PLAYER, new Vec3(0, 0, 2));
            helper.assertTrue(ally.getZ() > shield.getZ(), "Protected teammate must physically cross");
            team.setAllowFriendlyFire(true);
            helper.assertFalse(shield.allowsPassage(ally), "Friendly-fire teammate must follow combat policy");
            server.setPvpAllowed(false);
            helper.assertTrue(shield.allowsPassage(ally), "PvP-disabled players must be protected");
            equip(owner, false);
            helper.assertFalse(shield.allowsPassage(owner), "Unequipping must revoke owner passage");
            equip(owner, true);
            helper.assertTrue(shield.allowsPassage(owner), "Re-equipping must restore passage");
        } finally {
            server.setPvpAllowed(pvp);
            scoreboard.removePlayerTeam(team);
            shield.discard();
        }
        helper.assertFalse(shield.allowsPassage(owner), "Offline owner must not grant passage");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "apprenticecodex.supported_shield")
    public static void movementPacketsCannotGrantPassage(GameTestHelper helper) {
        var owner = createAssistWingsRider(helper, new BlockPos(1, 10, 1), "shield_packet_owner");
        var mover = createAssistWingsRider(helper, new BlockPos(1, 10, 1), "shield_packet_mover");
        var shield = shield(helper, owner);
        var start = shield.position().add(0, -0.5, -1);
        // 装備許可のない相手がクライアント側だけで衝突を無視する入力を再現する。
        mover.setPos(start);
        mover.connection.resetPosition();
        mover.connection.handleMovePlayer(new ServerboundMovePlayerPacket.Pos(start.x, start.y, start.z + 0.8, false));
        helper.assertTrue(mover.position().distanceToSqr(start) < 0.001, "Packet entering shield must be corrected");
        var other = createAssistWingsRider(helper, new BlockPos(1, 10, 1), "shield_packet_cross");
        other.setPos(start);
        other.connection.resetPosition();
        other.connection.handleMovePlayer(new ServerboundMovePlayerPacket.Pos(start.x, start.y, start.z + 2, false));
        helper.assertTrue(other.position().distanceToSqr(start) < 0.001, "Packet crossing shield must be corrected");
        try (var online = online(owner)) {
            equip(owner, true);
            owner.setPos(start);
            // 受理された移動はチャンク追跡も更新するため、実際のlevelへ登録する。
            helper.getLevel().addNewPlayer(owner);
            owner.connection.resetPosition();
            owner.connection.handleMovePlayer(new ServerboundMovePlayerPacket.Pos(start.x, start.y, start.z + 2, false));
            helper.assertTrue(owner.getZ() > shield.getZ(), "Authorized owner's movement packet must cross the shield");
        } finally {
            owner.discard();
        }
        shield.discard();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "apprenticecodex.supported_shield")
    public static void ownerDamageAndAntiMagicRemainDistinct(GameTestHelper helper) {
        var owner = createAssistWingsRider(helper, new BlockPos(1, 10, 1), "shield_damage_owner");
        var enemy = createAssistWingsRider(helper, new BlockPos(1, 10, 1), "shield_damage_enemy");
        var shield = shield(helper, owner);
        try (var online = online(owner)) {
            equip(owner, true);
            shield.takeDamage(helper.getLevel().damageSources().playerAttack(owner), 4, shield.position());
            helper.assertTrue(shield.getHealth() == 40, "Owner melee must not consume shield health");
            var arrow = new Arrow(helper.getLevel(), owner, new ItemStack(Items.ARROW), new ItemStack(Items.BOW));
            shield.takeDamage(helper.getLevel().damageSources().arrow(arrow, owner), 4, shield.position());
            helper.assertTrue(shield.getHealth() == 40, "Owner projectile must not consume shield health");
            shield.takeDamage(helper.getLevel().damageSources().playerAttack(enemy), 4, shield.position());
            shield.takeDamage(helper.getLevel().damageSources().generic(), 4, shield.position());
            helper.assertTrue(shield.getHealth() == 32, "Enemy and unknown damage must consume shield health");
            equip(owner, false);
            shield.takeDamage(helper.getLevel().damageSources().playerAttack(owner), 4, shield.position());
            helper.assertTrue(shield.getHealth() == 28, "Unequipped owner must damage shield normally");
            equip(owner, true);
            var anti = new AntiManaArrowEntity(helper.getLevel(), owner, new ItemStack(ItemRegistry.ANTI_MANA_ARROW.get()), new ItemStack(Items.BOW));
            try {
                var hit = AntiManaArrowEntity.class.getDeclaredMethod("onHitEntity", EntityHitResult.class);
                hit.setAccessible(true);
                hit.invoke(anti, new EntityHitResult(shield.getParts()[12]));
            } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
            helper.assertTrue(shield.isRemoved(), "Owner Anti Mana Arrow must destroy supported shield");
            var counterTarget = shield(helper, owner);
            owner.setPos(counterTarget.position().add(0, -owner.getEyeHeight(), -3));
            owner.setYRot(0);
            owner.setXRot(0);
            SpellRegistry.COUNTERSPELL_SPELL.get().onCast(helper.getLevel(), 1, owner, CastSource.SPELLBOOK, MagicData.getPlayerMagicData(owner));
            helper.assertTrue(counterTarget.isRemoved(), "Owner Counterspell must destroy supported shield");
        } finally { shield.discard(); }
        helper.succeed();
    }

    private static SupportedShieldEntity shield(GameTestHelper helper, ServerPlayer owner) {
        var shield = new SupportedShieldEntity(EntityRegistry.SUPPORTED_SHIELD.get(), helper.getLevel());
        shield.setOwner(owner);
        shield.setHealth(40);
        shield.setPos(helper.absoluteVec(new Vec3(2, 11, 3)));
        helper.getLevel().addFreshEntity(shield);
        shield.tick();
        return shield;
    }

    private static void equip(ServerPlayer player, boolean equipped) {
        CuriosApi.getCuriosInventory(player).orElseThrow().setEquippedCurio(CuriosSlotConstants.BELT, 0,
                equipped ? new ItemStack(ItemRegistry.PROTECTION_SPELL_SUPPORTER.get()) : ItemStack.EMPTY);
    }

    @SuppressWarnings("unchecked")
    private static OnlinePlayer online(ServerPlayer player) {
        try {
            // ログイン処理で他のGameTestのworld tick条件を変えず、オンライン解決だけを登録する。
            var field = PlayerList.class.getDeclaredField("playersByUUID");
            field.setAccessible(true);
            var map = (Map<UUID, ServerPlayer>) field.get(player.server.getPlayerList());
            map.put(player.getUUID(), player);
            return new OnlinePlayer(map, player.getUUID());
        } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }

    private record OnlinePlayer(Map<UUID, ServerPlayer> players, UUID uuid) implements AutoCloseable {
        @Override public void close() { players.remove(uuid); }
    }
}
