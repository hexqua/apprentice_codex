package jp.aquafactory.apprenticecodex.gametest.equipment.spellgun;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData;
import java.util.UUID;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import jp.aquafactory.apprenticecodex.item.curios.spellcasterammopouch.SpellcasterAmmoPouch;
import jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle.FullautoRapidcastSpellrifle;
import jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle.FullautoRapidcastSpellrifleScrollStorage;
import jp.aquafactory.apprenticecodex.item.multipurposestaffrifle.MultipurposeStaffrifle;
import jp.aquafactory.apprenticecodex.item.multipurposestaffrifle.MultipurposeStaffrifleScrollStorage;
import jp.aquafactory.apprenticecodex.item.spellgun.SpellGunCastEvent;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.utility.SpellCalibrationImbueHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.support.CurioTestSupport.equipCurio;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class RifleAmmunitionGameTests {
    private RifleAmmunitionGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void multipurposeStaffrifleUsesDedicatedAmmoAndCasingReturnPolicy(GameTestHelper helper) {
        assertAmmunition(helper, false);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void fullautoRapidcastSpellrifleUsesDedicatedAmmoAndCasingReturnPolicy(GameTestHelper helper) {
        assertAmmunition(helper, true);
    }

    private static void assertAmmunition(GameTestHelper helper, boolean fullauto) {
        var casingRandom = new CasingRandom();
        var player = createPlayer(helper, casingRandom);
        var stack = new ItemStack(fullauto ? ItemRegistry.FULLAUTO_RAPIDCAST_SPELLRIFLE.get()
                : ItemRegistry.MULTIPURPOSE_STAFFRIFLE.get());
        var ammo = fullauto ? ItemRegistry.FULLAUTO_SPELL_CASTING_ROUND.get() : ItemRegistry.MULTI_PURPOSE_SPELL_ROUND.get();
        var casing = fullauto ? ItemRegistry.EMPTY_FULLAUTO_SPELL_CASTING_CASING.get() : ItemRegistry.EMPTY_MULTI_PURPOSE_SPELL_CASING.get();
        var wrongAmmo = fullauto ? ItemRegistry.MULTI_PURPOSE_SPELL_ROUND.get() : ItemRegistry.FULLAUTO_SPELL_CASTING_ROUND.get();
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        player.getInventory().add(new ItemStack(wrongAmmo, 3));
        var magic = MagicData.getPlayerMagicData(player);
        magic.setSyncedData(new SyncedSpellData(player));
        magic.setMana(1000);
        var spell = SpellRegistry.MAGIC_MISSILE_SPELL.get();
        var scroll = SpellCalibrationImbueHelper.createScroll(new SpellData(spell, 1));
        if (fullauto) FullautoRapidcastSpellrifleScrollStorage.set(stack, 0, scroll, helper.getLevel().registryAccess());
        else MultipurposeStaffrifleScrollStorage.set(stack, 0, scroll, helper.getLevel().registryAccess());
        float mana = magic.getMana();
        helper.assertFalse(fullauto ? ((FullautoRapidcastSpellrifle) stack.getItem()).tryTriggerSelectedSpell(player, false)
                : ((MultipurposeStaffrifle) stack.getItem()).tryTriggerSelectedSpell(player, false), "Wrong ammunition must reject actual cast");
        helper.assertTrue(count(player, wrongAmmo) == 3 && count(player, casing) == 0 && magic.getMana() == mana && !magic.isCasting(),
                "Rejected cast must preserve wrong ammo, mana and casting state without creating casings");

        player.getInventory().add(new ItemStack(ammo, 2));
        // 出目を直接制御するため、有限回の抽選や特定seedの偶然に依存しない。
        casingRandom.value = 0;
        helper.assertTrue(fullauto ? ((FullautoRapidcastSpellrifle) stack.getItem()).tryTriggerSelectedSpell(player, false)
                : ((MultipurposeStaffrifle) stack.getItem()).tryTriggerSelectedSpell(player, false), "Correct ammunition must allow actual cast");
        spell.castSpell(helper.getLevel(), magic.getCastingSpellLevel(), player, CastSource.SWORD, true);
        helper.assertTrue(count(player, ammo) == 1 && count(player, wrongAmmo) == 3, "Cast must consume exactly one dedicated round");
        helper.assertTrue(countInventory(player, casing) == 1, "Returned dedicated casing must reach inventory without a pouch");

        casingRandom.value = 0.5F;
        consume(player, stack, ammo);
        helper.assertTrue(count(player, ammo) == 0 && count(player, casing) == 1, "Non-returning draw must consume ammo without adding a casing");
        casingRandom.value = 0;
        consume(player, stack, ammo);
        helper.assertTrue(count(player, casing) == 1, "Missing ammunition must not create a casing even with a returning draw");

        // 同tick中の装備変更をCuriosの検索cache更新に依存させず、装備済みの別playerで確認する。
        var pouchPlayer = createPlayer(helper, casingRandom);
        equipCurio(pouchPlayer, CuriosSlotConstants.BELT, new ItemStack(ItemRegistry.SPELLCASTER_AMMO_POUCH.get()));
        helper.assertTrue(SpellcasterAmmoPouch.isEquippedBy(pouchPlayer), "Fixture must equip the ammo pouch");
        casingRandom.value = 0.5F;
        var pouchAmmo = new ItemStack(ammo, 2);
        helper.assertTrue(SpellcasterAmmoPouch.accepts(pouchAmmo), "Fixture round must be accepted by the pouch tag");
        boolean inserted = SpellcasterAmmoPouch.storeInAccessiblePouches(pouchPlayer, pouchAmmo);
        helper.assertTrue(inserted && pouchAmmo.isEmpty(), "Fixture must store ammunition in pouch: inserted="
                + inserted + ", remaining=" + pouchAmmo + ", stored=" + SpellcasterAmmoPouch.countAmmoInAccessiblePouches(pouchPlayer, ammo));
        consume(pouchPlayer, stack, ammo);
        helper.assertTrue(count(pouchPlayer, ammo) == 1 && SpellcasterAmmoPouch.countAmmoInAccessiblePouches(pouchPlayer, casing) == 1,
                "Pouch consumption must remove one round and return the dedicated casing to the pouch");
        helper.assertTrue(countInventory(pouchPlayer, casing) == 0, "Pouch return must not place the casing in inventory");
        helper.succeed();
    }

    private static FakePlayer createPlayer(GameTestHelper helper, CasingRandom casingRandom) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "rifle_ammunition")) {
            @Override public RandomSource getRandom() { return casingRandom; }
        };
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        player.setPos(helper.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(1, 2, 1))));
        return player;
    }

    private static void consume(FakePlayer player, ItemStack stack, Item ammo) {
        if (stack.getItem() instanceof FullautoRapidcastSpellrifle rifle) SpellGunCastEvent.consumeAmmo(player, player.getInventory(), ammo, rifle);
        else SpellGunCastEvent.consumeAmmo(player, player.getInventory(), ammo, (MultipurposeStaffrifle) stack.getItem());
    }

    private static int count(FakePlayer player, Item item) {
        return SpellGunCastEvent.countAvailableAmmo(player, player.getInventory(), item);
    }

    private static int countInventory(FakePlayer player, Item item) {
        return player.getInventory().items.stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum();
    }

    private static final class CasingRandom extends LegacyRandomSource {
        private float value;
        private CasingRandom() { super(0); }
        @Override public float nextFloat() { return value; }
    }
}
