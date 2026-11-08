package jp.aquafactory.apprenticecodex.gametest.spell.summonweapon.bound;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.compat.Curios;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.gametest.spell.summonweapon.support.SummonWeaponTestSupport;
import jp.aquafactory.apprenticecodex.gametest.spell.targeting.support.TargetingTestFixture;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.boundbow.BoundBow;
import jp.aquafactory.apprenticecodex.spell.boundbow.BoundBowManager;
import jp.aquafactory.apprenticecodex.spell.boundsword.BoundSword;
import jp.aquafactory.apprenticecodex.spell.boundsword.BoundSwordManager;
import jp.aquafactory.apprenticecodex.spell.edgedancer.EdgeDancer;
import jp.aquafactory.apprenticecodex.spell.edgedancer.EdgeDancerManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;
import java.util.UUID;

final class BoundWeaponTestSupport {
    static final String TEMPLATE = "gametest/bound_weapons";
    static final ResourceLocation SUMMON_DAMAGE_TEST_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "bound_sword_summon_damage_test");

    private BoundWeaponTestSupport() {
    }

    static FakePlayer createPlayer(GameTestHelper helper, String name) {
        var scope = TargetingTestFixture.of(helper);
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
        player.setPos(helper.absoluteVec(new Vec3(48.5, 6, 48.5)));
        player.setNoGravity(true);
        SummonWeaponTestSupport.registerPlayer(helper, player);
        // 自然な返却・解除のassertionを終えた後で、失敗時も残った召喚状態だけを終了する。
        scope.onClose(() -> EdgeDancerManager.deactivate(player, true));
        scope.onClose(() -> BoundBowManager.deactivate(player, true));
        scope.onClose(() -> BoundSwordManager.deactivate(player, true));
        return player;
    }

    static List<AbstractArrow> ownedArrows(GameTestHelper helper, FakePlayer player) {
        var scope = TargetingTestFixture.of(helper);
        var arrows = scope.entities(AbstractArrow.class).stream().filter(arrow -> arrow.getOwner() == player).toList();
        arrows.forEach(scope::assertVisible);
        return arrows;
    }

    static int countArrows(GameTestHelper helper, FakePlayer player) {
        return ownedArrows(helper, player).size();
    }

    static AbstractArrow getSingleArrow(GameTestHelper helper, FakePlayer player) {
        var arrows = ownedArrows(helper, player);
        helper.assertTrue(arrows.size() == 1, "Expected exactly one owned Bound Bow arrow: count=" + arrows.size());
        return arrows.getFirst();
    }

    static MagicData resolveMagicData(GameTestHelper helper, FakePlayer player) {
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Bound Bow test could not resolve player magic data");
        return magicData;
    }

    static void equipGreaterConjurersTalisman(FakePlayer player) {
        var curiosInventory = CuriosApi.getCuriosInventory(player)
                .orElseThrow(() -> new IllegalStateException("Missing curios inventory for bound weapon Greater Conjurer's Talisman test"));
        curiosInventory.setEquippedCurio(Curios.NECKLACE_SLOT, 0,
                new ItemStack(ItemRegistry.GREATER_CONJURERS_TALISMAN.get()));
    }

    static int getPowerLevel(GameTestHelper helper, ItemStack stack) {
        var power = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.POWER);
        return EnchantmentHelper.getEnchantmentsForCrafting(stack).getLevel(power);
    }

    static BoundBow boundBow() {
        return (BoundBow) SpellRegistry.BOUND_BOW.get();
    }

    static BoundSword boundSword() {
        return (BoundSword) SpellRegistry.BOUND_SWORD.get();
    }

    static double resolveMainhandAttackDamage(ItemStack stack) {
        return stack.getAttributeModifiers().modifiers()
                .stream()
                .filter(entry -> entry.slot().equals(EquipmentSlotGroup.MAINHAND)
                        && entry.attribute().equals(Attributes.ATTACK_DAMAGE))
                .mapToDouble(entry -> entry.modifier().amount())
                .sum();
    }

    static EdgeDancer edgeDancer() {
        return (EdgeDancer) SpellRegistry.EDGE_DANCER.get();
    }
}
