package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData;
import io.redspace.ironsspellbooks.config.ServerConfigs;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.WeaponImbueCooldownHelper;
import jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle.FullautoCooldownPolicy;
import jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle.FullautoRapidcastSpellrifle;
import jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle.FullautoRapidcastSpellrifleCastContext;
import jp.aquafactory.apprenticecodex.item.spellgun.SpellgunRecastCooldown;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.spell.thermalprocess.ThermalProcess;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.Objects;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellgunCooldownRulesGameTests extends ApprenticeCodexGameTestScenarios {
    @GameTest(template = "gametest/basic_floor")
    public static void fullautoThresholdAndFloorAreIndependentOfEffectiveCooldown(GameTestHelper helper) {
        // 条件値と結果値を大きく離し、旧実装の実効CD判定への逆戻りを検出する。
        for (int base : new int[]{99, 100}) {
            helper.assertTrue(FullautoCooldownPolicy.calculate(base, 2000, 400, 100, 200, 20) == 0,
                    "Unmodified cooldown at or below the threshold must bypass even with a long cast");
        }
        helper.assertTrue(FullautoCooldownPolicy.calculate(101, 60, 0, 100, 20, 10) == 40,
                "Reducing effective cooldown below the threshold must not enable bypass");
        helper.assertTrue(FullautoCooldownPolicy.calculate(101, 3, 2, 100, 200, 20) == 5,
                "Reduction must not extend a combined cooldown below the floor");
        helper.assertTrue(FullautoCooldownPolicy.calculate(101, 20, 0, 100, 200, 20) == 20,
                "Cooldown equal to the floor must remain at the floor");
        helper.assertTrue(FullautoCooldownPolicy.calculate(101, 0, 50, 100, 20, 10) == 30,
                "A zero weapon multiplier must not remove the cast time surcharge");
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void fullautoEquipmentOrderMatchesPreview(GameTestHelper helper) throws Exception {
        var spell = new LongEquipmentSpell();
        var stack = new ItemStack(ItemRegistry.FULLAUTO_RAPIDCAST_SPELLRIFLE.get());
        ForgeConfigSpec.IntValue threshold = ApprenticeCodexServerConfig.SPEC.getValues()
                .get("Items.FullautoRapidcastSpellrifle.cooldownBypassThresholdTicks");
        int previousThreshold = threshold.get();
        double previousSword = ServerConfigs.SWORDS_CD_MULTIPLIER.get();
        try {
            threshold.set(0);
            for (int equipment = 0; equipment < 4; equipment++) {
                var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "gun_cd_equipment_" + equipment);
                var magic = MagicData.getPlayerMagicData(player);
                magic.setSyncedData(new SyncedSpellData(player));
                magic.initiateCast(spell, 1, 0, CastSource.SWORD, "mainhand");
                magic.setPlayerCastingItem(stack);
                if ((equipment & 1) != 0) player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_BOOTS.get()));
                if ((equipment & 2) != 0) equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));
                for (double sword : new double[]{0, 0.5, 1}) {
                    ServerConfigs.SWORDS_CD_MULTIPLIER.set(sword);
                    for (double reduction : new double[]{1, 1.5}) {
                        Objects.requireNonNull(player.getAttribute(AttributeRegistry.COOLDOWN_REDUCTION.get())).setBaseValue(reduction);
                        int castTime = spell.getCastType() == CastType.LONG ? spell.getEffectiveCastTime(1, player) : 0;
                        int effective = WeaponImbueCooldownHelper.getEffectiveSpellCooldown(spell, player, CastSource.SWORD);
                        var event = new SpellCooldownAddedEvent.Pre(
                                MagicManager.getEffectiveSpellCooldown(spell, player, CastSource.SWORD), spell, player, CastSource.SWORD);
                        // LONGが利用可能な発射状態を再現し、イベント優先度も含めて通す。
                        ((FullautoRapidcastSpellrifle) stack.getItem()).trySetCalibrationAdjustment(stack, 0,
                                new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get()), helper.getLevel().registryAccess());
                        try (var ignored = FullautoRapidcastSpellrifleCastContext.open(player.getUUID(), stack, spell, false)) {
                            MinecraftForge.EVENT_BUS.post(event);
                        }
                        int expected = FullautoCooldownPolicy.resolve(spell.getSpellCooldown(), effective, castTime);
                        helper.assertTrue(event.getEffectiveCooldown() == expected,
                                "Fullauto event must match preview after equipment reduction: " + event.getEffectiveCooldown() + " != " + expected);
                    }
                }
                magic.resetCastingState();
            }
        } finally {
            threshold.set(previousThreshold);
            ServerConfigs.SWORDS_CD_MULTIPLIER.set(previousSword);
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void longSpellgunAddsCastTimeAfterEquipmentReduction(GameTestHelper helper) {
        var spell = new LongEquipmentSpell();
        helper.assertTrue(spell.getCastType() == CastType.LONG, "Fixture must have a cast time");
        for (var item : new Item[]{ItemRegistry.DIAMOND_SPELLCASTER_GUN.get(), ItemRegistry.MALIGNANT_SPELLCASTER_GUN.get()}) {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "gun_equipment_surcharge");
            var magic = MagicData.getPlayerMagicData(player);
            magic.setSyncedData(new SyncedSpellData(player));
            magic.initiateCast(spell, 1, 0, CastSource.SWORD, "mainhand");
            magic.setPlayerCastingItem(new ItemStack(item));
            player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_BOOTS.get()));
            equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));
            int effective = WeaponImbueCooldownHelper.getEffectiveSpellCooldown(spell, player, CastSource.SWORD);
            int castTime = spell.getEffectiveCastTime(1, player);
            var event = new SpellCooldownAddedEvent.Pre(
                    MagicManager.getEffectiveSpellCooldown(spell, player, CastSource.SWORD), spell, player, CastSource.SWORD);
            MinecraftForge.EVENT_BUS.post(event);
            helper.assertTrue(event.getEffectiveCooldown() == effective + castTime,
                    "Equipment reduction must not absorb the instant cast surcharge");
            magic.resetCastingState();
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void gunRecastCooldownSurvivesSaveAndWeaponSwitch(GameTestHelper helper) throws Exception {
        var spell = SpellRegistry.FIREBALL_SPELL.get();
        for (var item : new Item[]{ItemRegistry.FULLAUTO_RAPIDCAST_SPELLRIFLE.get(),
                ItemRegistry.DIAMOND_SPELLCASTER_GUN.get(), ItemRegistry.MALIGNANT_SPELLCASTER_GUN.get()}) {
            for (var result : new RecastResult[]{RecastResult.USED_ALL_RECASTS, RecastResult.TIMEOUT}) {
                var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "gun_cd_recast");
                var magic = MagicData.getPlayerMagicData(player);
                magic.setSyncedData(new SyncedSpellData(player));
                var stack = new ItemStack(item);
                magic.initiateCast(spell, 1, 0, CastSource.SWORD, "mainhand");
                magic.setPlayerCastingItem(stack);
                int initialTime = spell.getEffectiveCastTime(1, player);
                var recast = new RecastInstance(spell.getSpellId(), 1, 2, 100, CastSource.SWORD, null);
                try (var ignored = FullautoRapidcastSpellrifleCastContext.open(player.getUUID(), stack, spell, false)) {
                    helper.assertTrue(magic.getPlayerRecasts().addRecast(recast, magic), "Initial recast must be registered");
                }
                var policy = ((SpellgunRecastCooldown.Holder) recast).apprenticecodex$getCooldown();
                helper.assertTrue(policy != null && policy.castTime() == initialTime,
                        "Recast must capture the first cast time");
                Objects.requireNonNull(policy);
                // 再ログインと同じNBT往復後、別の武器と詠唱速度で完了させる。
                var restored = new RecastInstance();
                restored.deserializeNBT(recast.serializeNBT());
                magic.getPlayerRecasts().forceAddRecast(restored);
                magic.resetCastingState();
                magic.setPlayerCastingItem(new ItemStack(ItemRegistry.IRON_SPELLCASTER_GUN.get()));
                Objects.requireNonNull(player.getAttribute(AttributeRegistry.CAST_TIME_REDUCTION.get())).setBaseValue(1.5);
                int effective = MagicManager.getEffectiveSpellCooldown(spell, player, CastSource.SWORD);
                magic.getPlayerRecasts().removeRecast(restored, result);
                int expected = policy.fullauto() ? FullautoCooldownPolicy.resolve(spell.getSpellCooldown(), effective, initialTime)
                        : effective + initialTime;
                var cooldown = magic.getPlayerCooldowns().getSpellCooldowns().get(spell.getSpellId());
                helper.assertTrue(cooldown != null && cooldown.getCooldownRemaining() == expected,
                        "Restored recast cooldown must retain its initial weapon policy and cast time");
                // 完了スコープが漏れていれば、通常の魔法書CDにも武器の加算が混入する。
                magic.setPlayerCastingItem(ItemStack.EMPTY);
                var control = new SpellCooldownAddedEvent.Pre(200, spell, player, CastSource.SPELLBOOK);
                MinecraftForge.EVENT_BUS.post(control);
                helper.assertTrue(control.getEffectiveCooldown() == 200, "Recast completion must not leak into later casts");
            }
        }
        helper.succeed();
    }

    // 両装備の対象であるThermal Processを借り、CDと短いLONG加算の順序だけを検証する。
    // 実在のThermal ProcessはCONTINUOUSであり、銃へ注入できる仕様には変更しない。
    private static final class LongEquipmentSpell extends ThermalProcess {
        private LongEquipmentSpell() { castTime = 40; }

        @Override
        public CastType getCastType() { return CastType.LONG; }
    }
}
