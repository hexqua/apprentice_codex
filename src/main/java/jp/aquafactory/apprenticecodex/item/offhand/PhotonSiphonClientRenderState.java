package jp.aquafactory.apprenticecodex.item.offhand;

import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class PhotonSiphonClientRenderState {
    private static final float COMBAT_RING_APPEAR_TICKS = 5.0F;
    private static final float COMBAT_RING_DISAPPEAR_TICKS = 20.0F;
    private static final float COMBAT_RING_ROTATION_PERIOD_TICKS = 20.0F;
    private static final float MANA_RING_FAST_PERIOD_TICKS = 20.0F;
    private static final float MANA_RING_SLOW_PERIOD_TICKS = 200.0F;
    private static final RingRenderState HIDDEN_RING =
            new RingRenderState(false, 1.0F, 1.0F, 1.0F, 0.0F, 0.0F, 0.0F);
    private static final EnumMap<InteractionHand, CombatRingAnimationState> COMBAT_RING_STATES =
            new EnumMap<>(InteractionHand.class);
    private static final EnumMap<InteractionHand, ManaRingAnimationState> MANA_RING_STATES =
            new EnumMap<>(InteractionHand.class);
    private static boolean syncedCombatState;

    static {
        for (var hand : InteractionHand.values()) {
            COMBAT_RING_STATES.put(hand, new CombatRingAnimationState());
            MANA_RING_STATES.put(hand, new ManaRingAnimationState());
        }
    }

    private PhotonSiphonClientRenderState() {
    }

    public static void setSyncedCombatState(boolean inCombat) {
        syncedCombatState = inCombat;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        var player = minecraft.player;
        if (level == null || player == null) {
            resetManaRingStates();
            syncedCombatState = false;
            return;
        }

        updateManaRingState(player, InteractionHand.MAIN_HAND);
        updateManaRingState(player, InteractionHand.OFF_HAND);
    }

    public static RingRenderState resolveCombatRing(
            @Nullable ItemStack stack,
            @Nullable ItemDisplayContext perspective,
            float partialTick
    ) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof PhotonSiphon)) {
            return HIDDEN_RING;
        }

        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        var player = minecraft.player;
        if (level == null || player == null) {
            return HIDDEN_RING;
        }

        var hand = resolveRenderedHand(player, perspective);
        if (hand == null || !matchesRenderingStack(player.getItemInHand(hand), stack)) {
            return HIDDEN_RING;
        }

        var spellData = resolveSelectedSpellData(player);
        if (spellData == SpellData.EMPTY) {
            resetCombatRingState(hand);
            return HIDDEN_RING;
        }

        var schoolType = spellData.getSpell().getSchoolType();
        if (schoolType == null) {
            resetCombatRingState(hand);
            return HIDDEN_RING;
        }

        var animationState = COMBAT_RING_STATES.get(hand);
        if (animationState == null) {
            return HIDDEN_RING;
        }

        float scale = animationState.resolveScale(stack, syncedCombatState, level.getGameTime(), partialTick);
        if (scale <= 0.0F) {
            return HIDDEN_RING;
        }

        int color = MagicTools.resolveSchoolTintColor(schoolType);
        float time = level.getGameTime() + partialTick;
        float rotY = -time * Mth.TWO_PI / COMBAT_RING_ROTATION_PERIOD_TICKS;
        return new RingRenderState(
                true,
                ((color >> 16) & 0xFF) / 255.0F,
                ((color >> 8) & 0xFF) / 255.0F,
                (color & 0xFF) / 255.0F,
                1.0F,
                scale,
                rotY
        );
    }

    public static RingRenderState resolveManaRing(
            @Nullable ItemStack stack,
            @Nullable ItemDisplayContext perspective,
            float partialTick
    ) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof PhotonSiphon)) {
            return HIDDEN_RING;
        }

        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (minecraft.level == null || player == null || isStaticPerspective(perspective)
                || !isRenderedHeldStack(player, stack, perspective)) {
            return HIDDEN_RING;
        }

        var hand = resolveRenderedHand(player, perspective);
        if (hand == null) {
            return HIDDEN_RING;
        }

        var state = MANA_RING_STATES.get(hand);
        if (state == null || !state.visible() || !state.matches(stack)) {
            return HIDDEN_RING;
        }

        float colorStrength = 1.0F - state.manaRatio();
        return new RingRenderState(true, 0.0F, colorStrength, colorStrength, 1.0F, 1.0F, state.resolveAngle(partialTick));
    }

    private static void updateManaRingState(Player player, InteractionHand hand) {
        var state = MANA_RING_STATES.get(hand);
        if (state == null) {
            return;
        }

        var stack = player.getItemInHand(hand);
        if (stack.isEmpty() || !(stack.getItem() instanceof PhotonSiphon)) {
            state.reset();
            return;
        }

        var maxMana = (float) player.getAttributeValue(AttributeRegistry.MAX_MANA.get());
        if (maxMana <= 0.0F) {
            state.reset();
            return;
        }

        float manaRatio = Mth.clamp(ClientMagicData.getPlayerMana() / maxMana, 0.0F, 1.0F);
        if (manaRatio >= 0.999F) {
            state.reset();
            return;
        }

        state.advance(stack, manaRatio);
    }

    private static SpellData resolveSelectedSpellData(Player player) {
        SpellSelectionManager manager = ClientMagicData.getSpellSelectionManager();
        if (manager == null) {
            manager = new SpellSelectionManager(player);
        }

        var spellData = manager.getSelectedSpellData();
        return spellData == null ? SpellData.EMPTY : spellData;
    }

    private static void resetCombatRingState(InteractionHand hand) {
        var animationState = COMBAT_RING_STATES.get(hand);
        if (animationState != null) {
            animationState.reset();
        }
    }

    private static void resetManaRingStates() {
        for (var state : MANA_RING_STATES.values()) {
            state.reset();
        }
    }

    private static boolean isRenderedHeldStack(
            Player player,
            @Nullable ItemStack stack,
            @Nullable ItemDisplayContext perspective
    ) {
        var hand = resolveRenderedHand(player, perspective);
        if (hand != null) {
            return matchesRenderingStack(player.getItemInHand(hand), stack);
        }

        return matchesRenderingStack(player.getMainHandItem(), stack)
                || matchesRenderingStack(player.getOffhandItem(), stack);
    }

    private static boolean isStaticPerspective(@Nullable ItemDisplayContext perspective) {
        return perspective == ItemDisplayContext.GUI
                || perspective == ItemDisplayContext.GROUND
                || perspective == ItemDisplayContext.FIXED;
    }

    @Nullable
    private static InteractionHand resolveRenderedHand(Player player, @Nullable ItemDisplayContext perspective) {
        if (perspective == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || perspective == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
            return resolveHandByArm(player, HumanoidArm.RIGHT);
        }
        if (perspective == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || perspective == ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
            return resolveHandByArm(player, HumanoidArm.LEFT);
        }
        return null;
    }

    private static InteractionHand resolveHandByArm(Player player, HumanoidArm arm) {
        return player.getMainArm() == arm ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
    }

    private static boolean matchesRenderingStack(ItemStack expectedStack, @Nullable ItemStack renderingStack) {
        return renderingStack != null
                && !expectedStack.isEmpty()
                && ItemStack.isSameItemSameTags(expectedStack, renderingStack);
    }

    private static float easeOutSine(float progress) {
        return Mth.sin(Mth.clamp(progress, 0.0F, 1.0F) * Mth.HALF_PI);
    }

    public record RingRenderState(
            boolean visible,
            float red,
            float green,
            float blue,
            float alpha,
            float scale,
            float rotY
    ) {
    }

    private static final class CombatRingAnimationState {
        private ItemStack trackedStack = ItemStack.EMPTY;
        private boolean targetCombat;
        private long transitionStartGameTime;
        private float transitionStartScale;

        private float resolveScale(ItemStack stack, boolean combat, long gameTime, float partialTick) {
            if (!matches(stack)) {
                trackedStack = stack.copy();
                targetCombat = false;
                transitionStartGameTime = gameTime;
                transitionStartScale = 0.0F;
            }

            if (targetCombat != combat) {
                transitionStartScale = resolveCurrentScale(gameTime, 0.0F);
                targetCombat = combat;
                transitionStartGameTime = gameTime;
            }

            return resolveCurrentScale(gameTime, partialTick);
        }

        private float resolveCurrentScale(long gameTime, float partialTick) {
            float elapsed = Math.max(0.0F, gameTime - transitionStartGameTime + partialTick);
            if (targetCombat) {
                float progress = Mth.clamp(elapsed / COMBAT_RING_APPEAR_TICKS, 0.0F, 1.0F);
                return Mth.lerp(progress, transitionStartScale, 1.0F);
            }

            float progress = Mth.clamp(elapsed / COMBAT_RING_DISAPPEAR_TICKS, 0.0F, 1.0F);
            return Mth.lerp(easeOutSine(progress), transitionStartScale, 0.0F);
        }

        private boolean matches(ItemStack stack) {
            return !trackedStack.isEmpty() && ItemStack.isSameItemSameTags(trackedStack, stack);
        }

        private void reset() {
            trackedStack = ItemStack.EMPTY;
            targetCombat = false;
            transitionStartGameTime = 0L;
            transitionStartScale = 0.0F;
        }
    }

    private static final class ManaRingAnimationState {
        private ItemStack trackedStack = ItemStack.EMPTY;
        private boolean visible;
        private float manaRatio;
        private float previousAngle;
        private float currentAngle;
        private float rotationSpeed;

        private void advance(ItemStack stack, float manaRatio) {
            if (!matches(stack)) {
                trackedStack = stack.copy();
                visible = true;
                this.manaRatio = manaRatio;
                previousAngle = 0.0F;
                currentAngle = 0.0F;
                rotationSpeed = resolveTargetRotationSpeed(manaRatio);
                return;
            }

            visible = true;
            this.manaRatio = manaRatio;
            previousAngle = currentAngle;
            float targetSpeed = resolveTargetRotationSpeed(manaRatio);
            rotationSpeed = Mth.lerp(0.18F, rotationSpeed, targetSpeed);
            currentAngle = Mth.positiveModulo(currentAngle + rotationSpeed, Mth.TWO_PI);
        }

        private float resolveAngle(float partialTick) {
            float delta = currentAngle - previousAngle;
            if (delta < -Mth.PI) {
                delta += Mth.TWO_PI;
            } else if (delta > Mth.PI) {
                delta -= Mth.TWO_PI;
            }

            return Mth.positiveModulo(previousAngle + delta * Mth.clamp(partialTick, 0.0F, 1.0F), Mth.TWO_PI);
        }

        private float manaRatio() {
            return manaRatio;
        }

        private boolean visible() {
            return visible;
        }

        private boolean matches(ItemStack stack) {
            return !trackedStack.isEmpty() && ItemStack.isSameItemSameTags(trackedStack, stack);
        }

        private void reset() {
            trackedStack = ItemStack.EMPTY;
            visible = false;
            manaRatio = 1.0F;
            previousAngle = 0.0F;
            currentAngle = 0.0F;
            rotationSpeed = 0.0F;
        }

        private static float resolveTargetRotationSpeed(float manaRatio) {
            float periodTicks = Mth.lerp(manaRatio, MANA_RING_FAST_PERIOD_TICKS, MANA_RING_SLOW_PERIOD_TICKS);
            return Mth.TWO_PI / periodTicks;
        }
    }
}
