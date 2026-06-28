package jp.aquafactory.apprenticecodex.compat.epicfight;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.spellchargedgreatsword.SpellchargedGreatsword;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import yesman.epicfight.api.collider.Collider;
import yesman.epicfight.api.collider.MultiOBBCollider;
import yesman.epicfight.api.forgeevent.WeaponCapabilityPresetRegistryEvent;
import yesman.epicfight.gameasset.EpicFightSkills;
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

    private EpicFightSpellchargedGreatswordCompat() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(EpicFightSpellchargedGreatswordCompat::onWeaponCapabilityPresetRegistry);
    }

    private static void onWeaponCapabilityPresetRegistry(WeaponCapabilityPresetRegistryEvent event) {
        event.getTypeEntry().put(
                WEAPON_TYPE_ID,
                item -> EpicFightSpellchargedGreatswordCompat.buildCapability(item, GREATSWORD_PRESET_ID)
        );
    }

    private static CapabilityItem.Builder buildCapability(Item item, ResourceLocation basePresetId) {
        var builder = (WeaponCapability.Builder) WeaponTypeReloadListener.getOrThrow(basePresetId.toString()).apply(item);

        builder.passiveSkill(EpicFightSkills.SWORD_MASTER);
        // 1.20.1 の Epic Fight では MoveSet.addInnateSkill が無いため builder 経由で登録する。
        // 1.21.1 へ移植する際は TWO_HAND の MoveSet 側へ移す。
        builder.innateSkill(CapabilityItem.Styles.TWO_HAND, stack -> EpicFightSkills.SWEEPING_EDGE);

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
                    return capability.getInnateSkill(playerPatch, stack) == EpicFightSkills.SWEEPING_EDGE
                            && capability.getPassiveSkill() == EpicFightSkills.SWORD_MASTER;
                })
                .orElse(false);
    }

    public static boolean setSweepingEdgeCharge(ServerPlayer player, int stack) {
        var skillContainer = getSweepingEdgeContainer(player);
        if (skillContainer == null) {
            return false;
        }

        skillContainer.setSkill(EpicFightSkills.SWEEPING_EDGE);
        skillContainer.setStack(stack);
        return true;
    }

    public static int getSweepingEdgeCharge(ServerPlayer player) {
        var skillContainer = getSweepingEdgeContainer(player);
        return skillContainer == null ? 0 : skillContainer.getStack();
    }

    public static int getSweepingEdgeMaxStack() {
        return EpicFightSkills.SWEEPING_EDGE.getMaxStack();
    }

    private static SkillContainer getSweepingEdgeContainer(ServerPlayer player) {
        return EpicFightCapabilities.getUnparameterizedEntityPatch(player, ServerPlayerPatch.class)
                .map(playerPatch -> playerPatch.getSkill(SkillSlots.WEAPON_INNATE))
                .orElse(null);
    }

    private static void refillSweepingEdgeCharge(ServerPlayerPatch playerpatch) {
        var skillContainer = playerpatch.getSkill(SkillSlots.WEAPON_INNATE);
        if (skillContainer == null || !skillContainer.hasSkill(EpicFightSkills.SWEEPING_EDGE)) {
            return;
        }

        var maxStack = EpicFightSkills.SWEEPING_EDGE.getMaxStack();
        if (maxStack <= 0 || skillContainer.getStack() >= maxStack) {
            return;
        }

        EpicFightSkills.SWEEPING_EDGE.setStackSynchronize(skillContainer, maxStack);
    }
}
