package jp.aquafactory.apprenticecodex.spell.remoteeye;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.RemoteEyeState;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class RemoteEyeClientController {
    private static final double BASE_MOVE_SPEED = 0.35;
    private static final double SPRINT_MOVE_SPEED = 0.70;
    private static final double CAMERA_WIDTH = 0.35;
    private static final double CAMERA_HEIGHT = 0.35;
    private static final double POSITION_EPSILON_SQ = 1.0e-6;
    private static final int CAMERA_ENTITY_ID = Integer.MIN_VALUE / 2;

    private static ArmorStand activeCamera;
    private static long activeUntilGameTime;

    private RemoteEyeClientController() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        var level = minecraft.level;
        if (player == null || level == null) {
            deactivateCamera(minecraft);
            return;
        }

        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            deactivateCamera(minecraft);
            return;
        }

        var state = spellData.get(CodexSpellStateTypeRegister.REMOTE_EYE_STATE);
        if (!isActive(level, state) || !player.isAlive()) {
            deactivateCamera(minecraft);
            return;
        }

        var camera = ensureCamera(player, state);
        if (minecraft.getCameraEntity() != camera) {
            minecraft.setCameraEntity(camera);
        }

        moveCamera(camera, player.level(), minecraft);
        anchorLocalPlayer(player, state);
    }

    @SubscribeEvent
    public static void onInteractionKeyMappingTriggered(InputEvent.InteractionKeyMappingTriggered event) {
        var minecraft = Minecraft.getInstance();
        if (!shouldRestrictGameplayInput(minecraft)) {
            return;
        }

        // 攻撃/使用/中クリックを別キーへ再割り当てしていても通らないようにしておく.
        event.setCanceled(true);
        event.setSwingHand(false);
    }

    @SubscribeEvent
    public static void onMouseButtonPre(InputEvent.MouseButton.Pre event) {
        if (!shouldRestrictGameplayInput(Minecraft.getInstance())) {
            return;
        }

        if (event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }

        // 視線移動にはマウスボタンは不要なので、RemoteEye 中は押下自体を通さない.
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onMouseScrolling(InputEvent.MouseScrollingEvent event) {
        if (!shouldRestrictGameplayInput(Minecraft.getInstance())) {
            return;
        }

        event.setCanceled(true);
    }

    private static boolean isActive(Level level, RemoteEyeState state) {
        return state.activeUntilGameTime > level.getGameTime();
    }

    public static boolean shouldRestrictGameplayInput(Minecraft minecraft) {
        if (minecraft.screen != null) {
            return false;
        }

        var player = minecraft.player;
        var level = minecraft.level;
        if (player == null || level == null) {
            return false;
        }

        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            return false;
        }

        return isActive(level, spellData.get(CodexSpellStateTypeRegister.REMOTE_EYE_STATE));
    }

    public static boolean isAllowedRemoteEyeKey(Minecraft minecraft, int keyCode, int scanCode) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            return true;
        }

        var options = minecraft.options;
        return options.keyUp.matches(keyCode, scanCode)
                || options.keyDown.matches(keyCode, scanCode)
                || options.keyLeft.matches(keyCode, scanCode)
                || options.keyRight.matches(keyCode, scanCode)
                || options.keyJump.matches(keyCode, scanCode)
                || options.keyShift.matches(keyCode, scanCode)
                || options.keySprint.matches(keyCode, scanCode);
    }

    private static ArmorStand ensureCamera(Player player, RemoteEyeState state) {
        var clientLevel = (ClientLevel) player.level();
        var existing = clientLevel.getEntity(CAMERA_ENTITY_ID);
        if (activeCamera != null && existing != activeCamera) {
            activeCamera = null;
        }

        if (activeCamera == null || !activeCamera.isAlive() || activeUntilGameTime != state.activeUntilGameTime) {
            if (existing != null) {
                removeCameraEntity(clientLevel);
            }

            // setCameraEntity の対象はクライアントワールドへ登録済みでないと挙動が不安定だったため、
            // ローカル専用の ArmorStand を視点実体として使う.
            var armorStand = EntityType.ARMOR_STAND.create(player.level());
            if (armorStand == null) {
                throw new IllegalStateException("RemoteEye camera entity could not be created");
            }
            activeCamera = armorStand;
            activeCamera.setId(CAMERA_ENTITY_ID);
            activeCamera.noPhysics = true;
            activeCamera.setNoGravity(true);
            activeCamera.setInvisible(true);
            var eyePosition = new Vec3(state.anchorX, state.anchorY + player.getEyeHeight(player.getPose()), state.anchorZ);
            activeCamera.setPos(eyePosition.x, eyePosition.y, eyePosition.z);
            activeCamera.setYRot(state.anchorYaw);
            activeCamera.setXRot(state.anchorPitch);
            activeCamera.setYHeadRot(state.anchorYaw);
            activeCamera.setYBodyRot(state.anchorYaw);
            activeCamera.yRotO = state.anchorYaw;
            activeCamera.xRotO = state.anchorPitch;
            activeCamera.xOld = eyePosition.x;
            activeCamera.yOld = eyePosition.y;
            activeCamera.zOld = eyePosition.z;
            activeUntilGameTime = state.activeUntilGameTime;
            clientLevel.addEntity(activeCamera);
        }

        return activeCamera;
    }

    private static void moveCamera(Entity camera, Level level, Minecraft minecraft) {
        var movementInput = gatherMovementInput(minecraft, camera);
        if (movementInput.lengthSqr() <= 1.0e-6) {
            return;
        }

        var speed = minecraft.options.keySprint.isDown() ? SPRINT_MOVE_SPEED : BASE_MOVE_SPEED;
        var desiredMovement = movementInput.normalize().scale(speed);
        var moved = collideWithBlocks(level, camera, camera.position(), desiredMovement);
        if (moved.lengthSqr() <= 1.0e-6) {
            return;
        }

        var newPosition = camera.position().add(moved);
        camera.setPos(newPosition.x, newPosition.y, newPosition.z);
        camera.xOld = newPosition.x;
        camera.yOld = newPosition.y;
        camera.zOld = newPosition.z;
    }

    private static Vec3 gatherMovementInput(Minecraft minecraft, Entity cameraEntity) {
        var forwardInput = 0.0;
        var strafeInput = 0.0;
        var verticalInput = 0.0;
        if (minecraft.options.keyUp.isDown()) {
            forwardInput += 1.0;
        }
        if (minecraft.options.keyDown.isDown()) {
            forwardInput -= 1.0;
        }
        if (minecraft.options.keyLeft.isDown()) {
            strafeInput -= 1.0;
        }
        if (minecraft.options.keyRight.isDown()) {
            strafeInput += 1.0;
        }
        if (minecraft.options.keyJump.isDown()) {
            verticalInput += 1.0;
        }
        if (minecraft.options.keyShift.isDown()) {
            verticalInput -= 1.0;
        }

        var flatForward = getFlatForward(cameraEntity.getYRot());
        var right = new Vec3(-flatForward.z, 0.0, flatForward.x);
        return flatForward.scale(forwardInput)
                .add(right.scale(strafeInput))
                .add(0.0, verticalInput, 0.0);
    }

    private static Vec3 getFlatForward(float yaw) {
        var yawRad = yaw * Mth.DEG_TO_RAD;
        var x = -Mth.sin(yawRad);
        var z = Mth.cos(yawRad);
        var forward = new Vec3(x, 0.0, z);
        if (forward.lengthSqr() <= 1.0e-6) {
            return new Vec3(0.0, 0.0, 1.0);
        }

        return forward.normalize();
    }

    private static Vec3 collideWithBlocks(Level level, Entity collisionContext, Vec3 currentPosition, Vec3 desiredMovement) {
        var currentBox = makeCollisionBox(currentPosition);
        var xMovement = tryAxisMove(level, collisionContext, currentBox, new Vec3(desiredMovement.x, 0.0, 0.0));
        currentBox = currentBox.move(xMovement);
        var yMovement = tryAxisMove(level, collisionContext, currentBox, new Vec3(0.0, desiredMovement.y, 0.0));
        currentBox = currentBox.move(yMovement);
        var zMovement = tryAxisMove(level, collisionContext, currentBox, new Vec3(0.0, 0.0, desiredMovement.z));
        return xMovement.add(yMovement).add(zMovement);
    }

    private static Vec3 tryAxisMove(Level level, Entity collisionContext, AABB currentBox, Vec3 movement) {
        if (movement.lengthSqr() <= 1.0e-6) {
            return Vec3.ZERO;
        }

        var movedBox = currentBox.move(movement);
        if (!hasBlockCollision(level, collisionContext, movedBox)) {
            return movement;
        }

        return Vec3.ZERO;
    }

    private static boolean hasBlockCollision(Level level, Entity collisionContext, AABB box) {
        return level.getBlockCollisions(collisionContext, box).iterator().hasNext();
    }

    private static AABB makeCollisionBox(Vec3 position) {
        var halfWidth = CAMERA_WIDTH / 2.0;
        return new AABB(
                position.x - halfWidth,
                position.y,
                position.z - halfWidth,
                position.x + halfWidth,
                position.y + CAMERA_HEIGHT,
                position.z + halfWidth
        );
    }

    public static boolean turnActiveCamera(double yRot, double xRot) {
        if (activeCamera == null) {
            return false;
        }

        // 視線入力は MouseHandlerMixin からここへだけ流し、本体回転は固定側へ任せる.
        activeCamera.turn(yRot, xRot);
        activeCamera.setXRot(Mth.clamp(activeCamera.getXRot(), -90.0f, 90.0f));
        activeCamera.setYHeadRot(activeCamera.getYRot());
        activeCamera.setYBodyRot(activeCamera.getYRot());
        activeCamera.yRotO = activeCamera.getYRot();
        activeCamera.xRotO = activeCamera.getXRot();
        return true;
    }

    private static void anchorLocalPlayer(Player player, RemoteEyeState state) {
        var anchor = new Vec3(state.anchorX, state.anchorY, state.anchorZ);
        if (player.position().distanceToSqr(anchor) > POSITION_EPSILON_SQ) {
            player.setPos(anchor.x, anchor.y, anchor.z);
        }

        player.xOld = anchor.x;
        player.yOld = anchor.y;
        player.zOld = anchor.z;
        player.setYRot(state.anchorYaw);
        player.setXRot(state.anchorPitch);
        player.setYHeadRot(state.anchorYaw);
        player.setYBodyRot(state.anchorYaw);
        player.yRotO = state.anchorYaw;
        player.xRotO = state.anchorPitch;
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0f;

        if (player instanceof LocalPlayer localPlayer) {
            localPlayer.input.forwardImpulse = 0.0f;
            localPlayer.input.leftImpulse = 0.0f;
            localPlayer.input.jumping = false;
            localPlayer.input.shiftKeyDown = false;
        }
    }

    private static void deactivateCamera(Minecraft minecraft) {
        if (minecraft.getCameraEntity() == activeCamera) {
            minecraft.setCameraEntity(minecraft.player);
        }

        var level = minecraft.level;
        if (level instanceof ClientLevel) {
            removeCameraEntity((ClientLevel) level);
        }

        activeCamera = null;
        activeUntilGameTime = 0L;
    }

    private static void removeCameraEntity(ClientLevel level) {
        var existing = level.getEntity(CAMERA_ENTITY_ID);
        if (existing != null) {
            level.removeEntity(CAMERA_ENTITY_ID, RemovalReason.DISCARDED);
        }
    }
}
