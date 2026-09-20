package jp.aquafactory.apprenticecodex.compat.epicfight;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.spellreaperscythe.ScytheThrowManager;
import jp.aquafactory.apprenticecodex.item.spellreaperscythe.SpellReaperScythe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.IEventBus;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.WeaponCapability;
import yesman.epicfight.world.capabilities.item.WeaponTypeReloadListener;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

public final class EpicFightSpellReaperScytheCompat {
    public static EpicFightSpellReapingSkill SPELL_REAPING;
    private static final ResourceLocation TYPE = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "spell_reaper_scythe");

    private EpicFightSpellReaperScytheCompat() {}

    public static void register(IEventBus bus) {
        bus.addListener(EpicFightSpellReaperScytheCompat::onSkillBuild);
        bus.addListener(EpicFightSpellReaperScytheCompat::onWeaponPreset);
    }

    private static void onSkillBuild(yesman.epicfight.api.forgeevent.SkillBuildEvent event) {
        SPELL_REAPING = (EpicFightSpellReapingSkill) event.createRegistryWorker(ApprenticeCodex.MODID)
                .build("spell_reaping", EpicFightSpellReapingSkill::new, EpicFightSpellReapingSkill.builder());
    }

    private static void onWeaponPreset(yesman.epicfight.api.forgeevent.WeaponCapabilityPresetRegistryEvent event) {
        event.getTypeEntry().put(TYPE, item -> {
            var builder = (WeaponCapability.Builder) WeaponTypeReloadListener.getOrThrow("epicfight:greatsword").apply(item);
            builder.constructor(EpicFightSpellReaperScytheCapability::new);
            // 1.20.1 では Moveset ではなく capability builder にインネイトを登録する。
            builder.innateSkill(CapabilityItem.Styles.TWO_HAND, stack -> SPELL_REAPING);
            return builder;
        });
    }

    public static void tick(ServerPlayer player) {
        java.util.Optional.ofNullable(EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class)).ifPresent(patch -> {
            if (!patch.isEpicFightMode()) ScytheThrowManager.recall(player);
            SPELL_REAPING.validateHolding(patch);
        });
    }

    public static void clear(ServerPlayer player) {
        java.util.Optional.ofNullable(EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class))
                .ifPresent(patch -> SPELL_REAPING.abort(patch));
    }

    public static void onAcceptedAttack(ServerPlayerPatch patch) {
        if (patch.isEpicFightMode() && patch.getOriginal().getMainHandItem().getItem() instanceof SpellReaperScythe) {
            SPELL_REAPING.abort(patch);
            ScytheThrowManager.recall(patch.getOriginal());
        }
    }
}
