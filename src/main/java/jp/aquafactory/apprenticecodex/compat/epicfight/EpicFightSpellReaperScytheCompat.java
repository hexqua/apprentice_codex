package jp.aquafactory.apprenticecodex.compat.epicfight;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.spellreaperscythe.ScytheThrowManager;
import jp.aquafactory.apprenticecodex.item.spellreaperscythe.SpellReaperScythe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.api.ex_cap.data.Moveset;
import yesman.epicfight.api.ex_cap.managers.MovesetManager;
import yesman.epicfight.registry.EpicFightRegistries;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.WeaponCapability;
import yesman.epicfight.world.capabilities.item.WeaponTypeReloadListener;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

public final class EpicFightSpellReaperScytheCompat {
    private static final DeferredRegister<Skill> SKILLS = DeferredRegister.create(EpicFightRegistries.Keys.SKILL, ApprenticeCodex.MODID);
    public static final DeferredHolder<Skill, EpicFightSpellReapingSkill> SPELL_REAPING = SKILLS.register(
            "spell_reaping", key -> EpicFightSpellReapingSkill.builder().build(key));
    private static final ResourceLocation TYPE = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "spell_reaper_scythe");

    private EpicFightSpellReaperScytheCompat() {}

    public static void register(IEventBus bus) {
        SKILLS.register(bus);
        MovesetManager.addMoveset(TYPE, Moveset.builder()
                .parent(ResourceLocation.fromNamespaceAndPath("epicfight", "greatsword_2h"))
                .addInnateSkill((stack, patch) -> SPELL_REAPING.get()));
        EpicFightEventHooks.Registry.WEAPON_CAPABILITY_PRESET.registerEvent(event -> event.getTypeEntry().put(TYPE, item -> {
            var builder = ((WeaponCapability.Builder) WeaponTypeReloadListener.get(
                    ResourceLocation.fromNamespaceAndPath("epicfight", "greatsword")).apply(item)).copy();
            builder.constructor(EpicFightSpellReaperScytheCapability::new);
            builder.addMoveset(CapabilityItem.Styles.TWO_HAND, TYPE);
            return builder;
        }), "apprenticecodex:spell_reaper_scythe");
    }

    public static void tick(ServerPlayer player) {
        EpicFightCapabilities.getUnparameterizedEntityPatch(player, ServerPlayerPatch.class).ifPresent(patch -> {
            if (!patch.isEpicFightMode()) ScytheThrowManager.recall(player);
            SPELL_REAPING.get().validateHolding(patch);
        });
    }

    public static void clear(ServerPlayer player) {
        EpicFightCapabilities.getUnparameterizedEntityPatch(player, ServerPlayerPatch.class)
                .ifPresent(patch -> SPELL_REAPING.get().abort(patch));
    }

    public static void onAcceptedAttack(ServerPlayerPatch patch) {
        if (patch.isEpicFightMode() && patch.getOriginal().getMainHandItem().getItem() instanceof SpellReaperScythe) {
            SPELL_REAPING.get().abort(patch);
            ScytheThrowManager.recall(patch.getOriginal());
        }
    }
}
