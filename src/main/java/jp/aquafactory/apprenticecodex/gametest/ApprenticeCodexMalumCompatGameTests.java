package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.MagicHelper;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.mixin.LivingEntityAccessor;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.InvocationTargetException;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ApprenticeCodexMalumCompatGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";
    private static final String MALUM_MOD_ID = "malum";
    private static final String IRONS_SPELLS_COMPAT =
            "com.sammy.malum.compability.irons_spellbooks.IronsSpellsCompat";
    private static final String IRONS_SPELLS_LOADED_ONLY = IRONS_SPELLS_COMPAT + "$LoadedOnly";
    private static final ResourceLocation MALUM_REPLENISHING =
            ResourceLocation.fromNamespaceAndPath(MALUM_MOD_ID, "replenishing");

    private ApprenticeCodexMalumCompatGameTests() {
    }

    @GameTest(template = TEMPLATE, batch = "apprenticecodex.malum_compat")
    public static void malumManaweavingManaRecoveryUsesCurrentIronsSync(GameTestHelper helper) {
        if (!ModList.get().isLoaded(MALUM_MOD_ID)) {
            helper.succeed();
            return;
        }

        var player = ApprenticeCodexGameTestScenarios.createTrackedEquipmentTestPlayer(
                helper, new BlockPos(0, 2, 0), "malum_manaweaving_mana_recovery_test"
        );
        helper.runAtTickTime(1, () -> {
            var magicData = MagicData.getPlayerMagicData(player);
            magicData.setMana(0.0F);
            invokeMalumGenerateMana(player, 17.5F);
            helper.assertTrue(Math.abs(magicData.getMana() - 17.5F) < 0.001F,
                    "Malum mana recovery should add mana without calling the removed UpdateClient API");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = "apprenticecodex.malum_compat")
    public static void malumReplenishingUsesSpellSideEdgeOffhand(GameTestHelper helper) {
        if (!ModList.get().isLoaded(MALUM_MOD_ID)) {
            helper.succeed();
            return;
        }

        var player = ApprenticeCodexGameTestScenarios.createTrackedEquipmentTestPlayer(
                helper, new BlockPos(0, 2, 0), "malum_replenishing_spell_side_edge_test"
        );
        helper.runAtTickTime(25, () -> {
            var replenishing = ForgeRegistries.ENCHANTMENTS.getValue(MALUM_REPLENISHING);
            helper.assertTrue(replenishing != null, "Malum Replenishing is not registered");
            ((LivingEntityAccessor) player).apprenticecodex$setAttackStrengthTicker(20);
            helper.assertTrue(player.getAttackStrengthScale(0.0F) > 0.8F,
                    "Replenishing test requires a fully charged attack");
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemRegistry.SPELL_SIDE_EDGE.get().getDefaultInstance());
            var offhand = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
            offhand.enchant(replenishing, 2);
            player.setItemInHand(InteractionHand.OFF_HAND, offhand);

            var magicMissile = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
            var magicData = MagicData.getPlayerMagicData(player);
            MagicHelper.MAGIC_MANAGER.addCooldown(player, magicMissile, CastSource.SWORD);
            var cooldown = magicData.getPlayerCooldowns().getSpellCooldowns().get(magicMissile.getSpellId());
            helper.assertTrue(cooldown != null, "Replenishing test requires an active spell cooldown");
            var before = cooldown.getCooldownRemaining();
            invokeMalumReplenishing(player);
            helper.assertTrue(cooldown.getCooldownRemaining() == before - (int) (before * 0.05F),
                    "Spell Side Edge should apply offhand Replenishing II through Malum's cooldown recovery");

            magicData.getPlayerCooldowns().removeCooldown(magicMissile.getSpellId());
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            MagicHelper.MAGIC_MANAGER.addCooldown(player, magicMissile, CastSource.SWORD);
            cooldown = magicData.getPlayerCooldowns().getSpellCooldowns().get(magicMissile.getSpellId());
            helper.assertTrue(cooldown != null, "Replenishing control test requires an active spell cooldown");
            before = cooldown.getCooldownRemaining();
            invokeMalumReplenishing(player);
            helper.assertTrue(cooldown.getCooldownRemaining() == before,
                    "Spell Side Edge should not recover cooldowns without offhand Replenishing");
            helper.succeed();
        });
    }

    private static void invokeMalumReplenishing(ServerPlayer player) {
        try {
            var compatClass = Class.forName(IRONS_SPELLS_LOADED_ONLY);
            var event = new LivingHurtEvent(player, player.damageSources().playerAttack(player), 1.0F);
            compatClass.getMethod("triggerReplenishing", LivingHurtEvent.class).invoke(null, event);
        } catch (InvocationTargetException exception) {
            throw new AssertionError("Malum Replenishing threw an exception", exception.getCause());
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Unable to invoke Malum Replenishing compatibility API", exception);
        }
    }

    private static void invokeMalumGenerateMana(ServerPlayer player, float amount) {
        try {
            var compatClass = Class.forName(IRONS_SPELLS_COMPAT);
            compatClass.getMethod("generateMana", ServerPlayer.class, float.class).invoke(null, player, amount);
        } catch (InvocationTargetException exception) {
            throw new AssertionError("Malum mana recovery threw an exception", exception.getCause());
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Unable to invoke Malum mana recovery compatibility API", exception);
        }
    }
}
