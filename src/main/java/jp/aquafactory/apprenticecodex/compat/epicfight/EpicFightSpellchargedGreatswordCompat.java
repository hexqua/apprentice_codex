package jp.aquafactory.apprenticecodex.compat.epicfight;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.spellchargedgreatsword.SpellchargedGreatsword;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import yesman.epicfight.api.collider.Collider;
import yesman.epicfight.api.collider.MultiOBBCollider;
import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.api.event.types.registry.WeaponCapabilityPresetRegistryEvent;
import yesman.epicfight.api.ex_cap.data.Moveset;
import yesman.epicfight.api.ex_cap.managers.MovesetManager;
import yesman.epicfight.registry.entries.EpicFightSkills;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.WeaponCapability;
import yesman.epicfight.world.capabilities.item.WeaponTypeReloadListener;

// リフレクションで参照するため、IDE側の未使用検知を無効化.
@SuppressWarnings("unused")
public final class EpicFightSpellchargedGreatswordCompat {
    public static final String MOD_ID = EpicFightCompat.MOD_ID;
    public static final ResourceLocation WEAPON_TYPE_ID =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "spellcharged_greatsword");
    private static final ResourceLocation GREATSWORD_PRESET_ID =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "greatsword");
    private static final ResourceLocation GREATSWORD_TWO_HAND_MOVESET_ID =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "greatsword_2h");
    private static final ResourceLocation SPELLCHARGED_GREATSWORD_TWO_HAND_MOVESET_ID =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "spellcharged_greatsword_2h");

    private EpicFightSpellchargedGreatswordCompat() {
    }

    public static void register(IEventBus modEventBus) {
        MovesetManager.addMoveset(
                SPELLCHARGED_GREATSWORD_TWO_HAND_MOVESET_ID,
                Moveset.builder()
                        .parent(GREATSWORD_TWO_HAND_MOVESET_ID)
                        .setPassiveSkill(EpicFightSkills.SWORD_MASTER)
                        .addInnateSkill((stack, playerPatch) -> sweepingEdgeSkill())
        );
        EpicFightEventHooks.Registry.WEAPON_CAPABILITY_PRESET.registerEvent(
                EpicFightSpellchargedGreatswordCompat::onWeaponCapabilityPresetRegistry,
                "apprenticecodex:spellcharged_greatsword"
        );
    }

    private static void onWeaponCapabilityPresetRegistry(WeaponCapabilityPresetRegistryEvent event) {
        event.getTypeEntry().put(WEAPON_TYPE_ID, EpicFightSpellchargedGreatswordCompat::buildCapability);
    }

    private static CapabilityItem.Builder<?> buildCapability(Item item) {
        var greatswordFactory = WeaponTypeReloadListener.get(GREATSWORD_PRESET_ID);
        var baseBuilder = greatswordFactory != null ? greatswordFactory.apply(item) : null;
        var builder = baseBuilder instanceof WeaponCapability.Builder weaponBuilder
                ? weaponBuilder.copy()
                : WeaponCapability.builder();

        builder.addMoveset(
                CapabilityItem.Styles.TWO_HAND,
                SPELLCHARGED_GREATSWORD_TWO_HAND_MOVESET_ID
        );

        return builder;
    }

    public static Collider getOverchargedWeaponCollider(Item item) {
        return new MultiOBBCollider(3, 0.5D, 0.8D, 1.5D, 0.0D, 0.0D, -1.5D);
    }

    public static void tick(ServerPlayer player) {
        var stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof SpellchargedGreatsword)
                || !SpellchargedGreatsword.isOverchargeActive(stack, player.level().getGameTime())) {
            return;
        }

        EpicFightCapabilities.getUnparameterizedEntityPatch(player, ServerPlayerPatch.class)
                .ifPresent(EpicFightSpellchargedGreatswordCompat::refillSweepingEdgeCharge);
    }

    public static boolean hasExpectedSpellchargedGreatswordSkills(ServerPlayer player, ItemStack stack) {
        return EpicFightCapabilities.getUnparameterizedEntityPatch(player, ServerPlayerPatch.class)
                .map(playerPatch -> {
                    var capability = EpicFightCapabilities.getItemStackCapability(stack);
                    return capability.getInnateSkill(playerPatch, stack) == sweepingEdgeSkill()
                            && capability.getPassiveSkill(playerPatch) == swordMasterSkill();
                })
                .orElse(false);
    }

    public static boolean setSweepingEdgeCharge(ServerPlayer player, int stack) {
        var skillContainer = getSweepingEdgeContainer(player);
        if (skillContainer == null) {
            return false;
        }

        skillContainer.setSkill(sweepingEdgeSkill());
        skillContainer.setStack(stack);
        return true;
    }

    public static int getSweepingEdgeCharge(ServerPlayer player) {
        var skillContainer = getSweepingEdgeContainer(player);
        return skillContainer == null ? 0 : skillContainer.getStack();
    }

    public static int getSweepingEdgeMaxStack() {
        return sweepingEdgeSkill().getMaxStack();
    }

    private static SkillContainer getSweepingEdgeContainer(ServerPlayer player) {
        return EpicFightCapabilities.getUnparameterizedEntityPatch(player, ServerPlayerPatch.class)
                .map(playerPatch -> playerPatch.getSkill(SkillSlots.WEAPON_INNATE))
                .orElse(null);
    }

    private static void refillSweepingEdgeCharge(ServerPlayerPatch playerpatch) {
        var skillContainer = playerpatch.getSkill(SkillSlots.WEAPON_INNATE);
        var sweepingEdge = sweepingEdgeSkill();
        if (skillContainer == null || !skillContainer.hasSkill(sweepingEdge)) {
            return;
        }

        var maxStack = sweepingEdge.getMaxStack();
        if (maxStack <= 0 || skillContainer.getStack() >= maxStack) {
            return;
        }

        sweepingEdge.setStackSynchronize(skillContainer, maxStack);
    }

    private static Skill sweepingEdgeSkill() {
        return EpicFightSkills.SWEEPING_EDGE.get();
    }

    private static Skill swordMasterSkill() {
        return EpicFightSkills.SWORD_MASTER.get();
    }
}
