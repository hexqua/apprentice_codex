package jp.aquafactory.apprenticecodex.spell.tamerspocket;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.ICastDataSerializable;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.TamersPocketState;
import jp.aquafactory.apprenticecodex.utility.RaycastTools;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class TamersPocket extends AbstractSpell {
    private static final int[] DEPLOY_Y_OFFSETS = {0, 1, -1, 2, -2};

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "tamers_pocket");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(SchoolRegistry.EVOCATION_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(4)
            .build();

    public TamersPocket() {
        baseSpellPower = 0;
        spellPowerPerLevel = 0;
        baseManaCost = 50;
        manaCostPerLevel = 0;
        castTime = 20;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        var distanceInfo = Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(getRange(), 1));

        // Inscribe 系 GUI では caster が null のプレビュー呼び出しがあるため、件数表示だけ隠す。
        if (caster == null) {
            return List.of(distanceInfo);
        }
        return List.of(
                distanceInfo,
                Component.translatable("ui.apprenticecodex.tamers_pocket.current_count", getCurrentPetCount(caster))
        );
    }

    private int getCurrentPetCount(@Nullable LivingEntity entity) {
        if (entity == null) {
            return 0;
        }
        var spellData = Capabilities.getSpellDataOrNull(entity);
        if (spellData == null) {
            return 0;
        }
        return spellData.get(CodexSpellStateTypeRegister.TAMERS_POCKET_STATE).getStoredPetCount();
    }

    private double getRange() {
        return 8;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellId;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return config;
    }

    @Override
    public CastType getCastType() {
        return CastType.LONG;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundRegistry.CLOUD_OF_REGEN_LOOP.get());
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.empty();
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.ANIMATION_CONTINUOUS_CAST;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return AnimationHolder.none();
    }

    @Override
    public ICastDataSerializable getEmptyCastData() {
        return new TamersPocketCastData();
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        if (!(entity instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        var castData = buildCastData(serverPlayer);
        if (castData == null || castData.mode == TamersPocketCastMode.NONE) {
            return false;
        }

        playerMagicData.setAdditionalCastData(castData);
        return true;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel serverLevel
                && entity instanceof ServerPlayer serverPlayer
                && playerMagicData.getAdditionalCastData() instanceof TamersPocketCastData castData) {
            switch (castData.mode) {
                case WITHDRAW_SINGLE, WITHDRAW_AREA -> withdrawLockedPets(serverPlayer, castData.lockedPetUuids);
                case DEPLOY -> deployStoredPets(serverLevel, serverPlayer, castData.lockedDeployPositions);
                case NONE -> {
                }
            }
        }

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onServerCastComplete(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, boolean cancelled) {
        if (playerMagicData != null) {
            if (playerMagicData.getAdditionalCastData() instanceof TamersPocketCastData castData) {
                castData.reset();
            }
            playerMagicData.setAdditionalCastData(null);
        }
        super.onServerCastComplete(level, spellLevel, entity, playerMagicData, cancelled);
    }

    private @Nullable TamersPocketCastData buildCastData(ServerPlayer player) {
        var lookTarget = findLookTarget(player);
        if (lookTarget != null) {
            var castData = new TamersPocketCastData();
            castData.mode = TamersPocketCastMode.WITHDRAW_SINGLE;
            castData.lockedPetUuids.add(lookTarget.getUUID());
            sendActionBar(player, Component.translatable("ui.apprenticecodex.tamers_pocket.withdraw_target", lookTarget.getName()));
            return castData;
        }

        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            return null;
        }

        var storedCount = spellData.get(CodexSpellStateTypeRegister.TAMERS_POCKET_STATE).getStoredPetCount();
        if (storedCount <= 0) {
            var nearbyPets = findNearbyOwnedPets(player.serverLevel(), player);
            if (nearbyPets.isEmpty()) {
                sendFailureActionBar(player, Component.translatable("ui.apprenticecodex.tamers_pocket.no_pets"));
                return null;
            }

            var castData = new TamersPocketCastData();
            castData.mode = TamersPocketCastMode.WITHDRAW_AREA;
            nearbyPets.stream()
                    .map(Entity::getUUID)
                    .forEach(castData.lockedPetUuids::add);
            sendActionBar(player, Component.translatable("ui.apprenticecodex.tamers_pocket.withdraw_area", castData.lockedPetUuids.size()));
            return castData;
        }

        var deployPositions = findDeployPositions(player, storedCount);
        if (deployPositions.isEmpty()) {
            sendFailureActionBar(player, Component.translatable("ui.apprenticecodex.tamers_pocket.no_safe_place"));
            return null;
        }

        var castData = new TamersPocketCastData();
        castData.mode = TamersPocketCastMode.DEPLOY;
        castData.lockedDeployPositions.addAll(deployPositions);
        sendActionBar(player, Component.translatable("ui.apprenticecodex.tamers_pocket.deploy", deployPositions.size()));
        return castData;
    }

    private @Nullable LivingEntity findLookTarget(ServerPlayer player) {
        var result = RaycastTools.raycastFromEye(player, getRange(), 0.5, target -> isOwnedPetBy(target, player));
        return result.hitEntity() instanceof LivingEntity living ? living : null;
    }

    private List<LivingEntity> findNearbyOwnedPets(ServerLevel level, ServerPlayer player) {
        return level.getEntitiesOfClass(
                        LivingEntity.class,
                        player.getBoundingBox().inflate(getRange()),
                        target -> target.isAlive() && isOwnedPetBy(target, player)
                ).stream()
                .sorted(Comparator.comparingDouble(target -> target.distanceToSqr(player)))
                .toList();
    }

    private void withdrawLockedPets(ServerPlayer player, List<UUID> lockedPetUuids) {
        if (lockedPetUuids.isEmpty()) {
            return;
        }

        var server = player.getServer();
        if (server == null) {
            return;
        }

        var capturedPets = new ArrayList<TamersPocketState.StoredPet>();
        for (var uuid : lockedPetUuids) {
            var target = findEntityByUuid(server, uuid);
            if (!(target instanceof LivingEntity pet) || !pet.isAlive() || !isOwnedPetBy(pet, player)) {
                continue;
            }

            var captured = capturePet(player, pet);
            capturedPets.add(captured);
            pet.discard();
        }

        if (capturedPets.isEmpty()) {
            return;
        }

        Capabilities.withSpellData(player, spellData -> spellData.edit(
                CodexSpellStateTypeRegister.TAMERS_POCKET_STATE,
                state -> capturedPets.forEach(state::addStoredPet)
        ));
        TamersPocketSync.syncToClient(player);
    }

    private TamersPocketState.@NotNull StoredPet capturePet(ServerPlayer owner, LivingEntity pet) {
        var entityTypeId = EntityType.getKey(pet.getType());

        // 格納前に騎乗関係を解いておくと、復元時に 1.20.1 固有の乗客参照が残りにくい。
        pet.ejectPassengers();
        pet.stopRiding();

        return new TamersPocketState.StoredPet(
                UUID.randomUUID(),
                entityTypeId.toString(),
                pet.saveWithoutId(new CompoundTag()),
                owner.serverLevel().getGameTime(),
                pet.hasCustomName() ? pet.getName().getString() : null
        );
    }

    private void deployStoredPets(ServerLevel level, ServerPlayer player, List<BlockPos> lockedDeployPositions) {
        if (lockedDeployPositions.isEmpty()) {
            return;
        }

        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            return;
        }

        var storedPets = spellData.get(CodexSpellStateTypeRegister.TAMERS_POCKET_STATE).getStoredPetsSnapshot();
        if (storedPets.isEmpty()) {
            return;
        }

        var removedEntryIds = new ArrayList<UUID>();
        var attemptCount = Math.min(storedPets.size(), lockedDeployPositions.size());
        for (int i = 0; i < attemptCount; ++i) {
            var storedPet = storedPets.get(i);
            var deployPos = lockedDeployPositions.get(i);
            if (deployStoredPet(level, player, storedPet, deployPos)) {
                removedEntryIds.add(storedPet.entryId());
            }
        }

        if (removedEntryIds.isEmpty()) {
            return;
        }

        spellData.edit(CodexSpellStateTypeRegister.TAMERS_POCKET_STATE, state -> state.removeStoredPets(removedEntryIds));
        TamersPocketSync.syncToClient(player);
    }

    private boolean deployStoredPet(ServerLevel level, ServerPlayer owner, TamersPocketState.StoredPet storedPet, BlockPos deployPos) {
        var deployCenter = getDeployCenter(level, deployPos);
        var entity = EntityType.loadEntityRecursive(storedPet.createSpawnTag(), level, loaded -> {
            loaded.moveTo(
                    deployCenter.x,
                    deployCenter.y,
                    deployCenter.z,
                    loaded.getYRot(),
                    loaded.getXRot()
            );
            return loaded;
        });

        if (!(entity instanceof LivingEntity pet)) {
            return false;
        }

        if (!ensureOwnedByCaster(pet, owner)) {
            return false;
        }

        if (!level.tryAddFreshEntityWithPassengers(pet)) {
            return false;
        }

        var healAmount = Math.max(0L, (level.getGameTime() - storedPet.storedGameTime()) / 1200L);
        if (healAmount > 0L) {
            pet.heal((float) healAmount);
        }

        return true;
    }

    private List<BlockPos> findDeployPositions(ServerPlayer player, int maxCount) {
        var level = player.serverLevel();
        var origin = player.blockPosition();
        var forward = getHorizontalLook(player);
        var range = Mth.ceil(getRange());
        var candidates = new ArrayList<DeployCandidate>();

        for (int x = -range; x <= range; ++x) {
            for (int z = -range; z <= range; ++z) {
                if (x == 0 && z == 0) {
                    continue;
                }
                var horizontalDistanceSqr = x * x + z * z;
                if (horizontalDistanceSqr > range * range) {
                    continue;
                }

                for (var yOffset : DEPLOY_Y_OFFSETS) {
                    var candidatePos = origin.offset(x, yOffset, z);
                    if (!isSafeDeployPosition(level, player, candidatePos)) {
                        continue;
                    }

                    var horizontal = new Vec3(x, 0.0, z).normalize();
                    var priority = horizontal.dot(forward) * 100.0 - horizontalDistanceSqr - Math.abs(yOffset) * 6.0;
                    candidates.add(new DeployCandidate(candidatePos.immutable(), priority));
                }
            }
        }

        candidates.sort((left, right) -> Double.compare(right.priority(), left.priority()));

        var selected = new ArrayList<BlockPos>();
        var reservedAreas = new ArrayList<AABB>();
        for (var candidate : candidates) {
            var reservedArea = new AABB(candidate.pos()).inflate(1.0);
            var overlaps = reservedAreas.stream().anyMatch(area -> area.intersects(reservedArea));
            if (overlaps) {
                continue;
            }

            selected.add(candidate.pos());
            reservedAreas.add(reservedArea);
            if (selected.size() >= maxCount) {
                break;
            }
        }

        return selected;
    }

    private boolean isSafeDeployPosition(ServerLevel level, ServerPlayer player, BlockPos feetPos) {
        if (!hasOpenAirVolume(level, feetPos)) {
            return false;
        }
        if (!hasSupport(level, feetPos)) {
            return false;
        }

        var occupiedArea = new AABB(feetPos).inflate(1.0);
        return level.getEntities(player, occupiedArea, Entity::isAlive).isEmpty();
    }

    private boolean hasOpenAirVolume(ServerLevel level, BlockPos feetPos) {
        for (int x = -1; x <= 1; ++x) {
            for (int y = 0; y <= 2; ++y) {
                for (int z = -1; z <= 1; ++z) {
                    if (!level.getBlockState(feetPos.offset(x, y, z)).isAir()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean hasSupport(ServerLevel level, BlockPos feetPos) {
        var belowPos = feetPos.below();
        if (level.getFluidState(belowPos).is(FluidTags.WATER)) {
            return true;
        }

        return !level.getBlockState(belowPos).getCollisionShape(level, belowPos).isEmpty();
    }

    private Vec3 getDeployCenter(ServerLevel level, BlockPos feetPos) {
        var belowPos = feetPos.below();
        var y = (double) feetPos.getY();
        if (!level.getFluidState(belowPos).is(FluidTags.WATER)) {
            var supportShape = level.getBlockState(belowPos).getCollisionShape(level, belowPos);
            y = belowPos.getY() + supportShape.max(net.minecraft.core.Direction.Axis.Y);
        }

        return new Vec3(feetPos.getX() + 0.5, y, feetPos.getZ() + 0.5);
    }

    private static boolean ensureOwnedByCaster(LivingEntity pet, ServerPlayer owner) {
        if (pet instanceof TamableAnimal tamable) {
            tamable.setOwnerUUID(owner.getUUID());
            tamable.setTame(true, true);
            return tamable.isOwnedBy(owner);
        }

        if (pet instanceof AbstractHorse horse) {
            horse.setOwnerUUID(owner.getUUID());
            horse.setTamed(true);
            return owner.getUUID().equals(horse.getOwnerUUID());
        }

        return false;
    }

    private static boolean isOwnedPetBy(Entity target, Player owner) {
        if (!(target instanceof LivingEntity living) || !living.isAlive()) {
            return false;
        }

        if (target instanceof TamableAnimal tamable) {
            return tamable.isTame() && tamable.isOwnedBy(owner);
        }

        if (target instanceof AbstractHorse horse) {
            return horse.isTamed() && owner.getUUID().equals(horse.getOwnerUUID());
        }

        return false;
    }

    private static @Nullable Entity findEntityByUuid(MinecraftServer server, UUID uuid) {
        for (var level : server.getAllLevels()) {
            var entity = level.getEntity(uuid);
            if (entity != null) {
                return entity;
            }
        }
        return null;
    }

    private static Vec3 getHorizontalLook(ServerPlayer player) {
        var look = player.getLookAngle();
        var horizontal = new Vec3(look.x, 0.0, look.z);
        if (horizontal.lengthSqr() > 1.0E-6) {
            return horizontal.normalize();
        }

        var facing = player.getDirection();
        var fallback = new Vec3(facing.getStepX(), 0.0, facing.getStepZ());
        return fallback.lengthSqr() > 1.0E-6 ? fallback.normalize() : new Vec3(0.0, 0.0, 1.0);
    }

    private static void sendActionBar(ServerPlayer player, Component message) {
        player.connection.send(new ClientboundSetActionBarTextPacket(message.copy().withStyle(ChatFormatting.GREEN)));
    }

    private static void sendFailureActionBar(ServerPlayer player, Component message) {
        player.connection.send(new ClientboundSetActionBarTextPacket(message.copy().withStyle(ChatFormatting.RED)));
    }

    private record DeployCandidate(BlockPos pos, double priority) {
    }

    private enum TamersPocketCastMode {
        NONE,
        WITHDRAW_SINGLE,
        WITHDRAW_AREA,
        DEPLOY
    }

    public static class TamersPocketCastData implements ICastDataSerializable {
        private TamersPocketCastMode mode = TamersPocketCastMode.NONE;
        private final List<UUID> lockedPetUuids = new ArrayList<>();
        private final List<BlockPos> lockedDeployPositions = new ArrayList<>();

        @Override
        public void writeToBuffer(FriendlyByteBuf friendlyByteBuf) {
            friendlyByteBuf.writeEnum(mode);
            friendlyByteBuf.writeVarInt(lockedPetUuids.size());
            for (var uuid : lockedPetUuids) {
                friendlyByteBuf.writeUUID(uuid);
            }
            friendlyByteBuf.writeVarInt(lockedDeployPositions.size());
            for (var pos : lockedDeployPositions) {
                friendlyByteBuf.writeBlockPos(pos);
            }
        }

        @Override
        public void readFromBuffer(FriendlyByteBuf friendlyByteBuf) {
            mode = friendlyByteBuf.readEnum(TamersPocketCastMode.class);
            lockedPetUuids.clear();
            var lockedPetUuidCount = friendlyByteBuf.readVarInt();
            for (int i = 0; i < lockedPetUuidCount; ++i) {
                lockedPetUuids.add(friendlyByteBuf.readUUID());
            }
            lockedDeployPositions.clear();
            var lockedDeployPositionCount = friendlyByteBuf.readVarInt();
            for (int i = 0; i < lockedDeployPositionCount; ++i) {
                lockedDeployPositions.add(friendlyByteBuf.readBlockPos());
            }
        }

        @Override
        public void reset() {
            mode = TamersPocketCastMode.NONE;
            lockedPetUuids.clear();
            lockedDeployPositions.clear();
        }

        @Override
        public CompoundTag serializeNBT(HolderLookup.Provider provider) {
            var tag = new CompoundTag();
            tag.putString("Mode", mode.name());

            var petList = new ListTag();
            for (var uuid : lockedPetUuids) {
                var petTag = new CompoundTag();
                petTag.putUUID("Uuid", uuid);
                petList.add(petTag);
            }
            tag.put("LockedPetUuids", petList);

            var posList = new ListTag();
            for (var pos : lockedDeployPositions) {
                var posTag = new CompoundTag();
                posTag.putInt("X", pos.getX());
                posTag.putInt("Y", pos.getY());
                posTag.putInt("Z", pos.getZ());
                posList.add(posTag);
            }
            tag.put("LockedDeployPositions", posList);
            return tag;
        }

        @Override
        public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
            mode = nbt.contains("Mode") ? TamersPocketCastMode.valueOf(nbt.getString("Mode")) : TamersPocketCastMode.NONE;
            lockedPetUuids.clear();
            var petList = nbt.getList("LockedPetUuids", Tag.TAG_COMPOUND);
            for (int i = 0; i < petList.size(); ++i) {
                var petTag = petList.getCompound(i);
                if (petTag.hasUUID("Uuid")) {
                    lockedPetUuids.add(petTag.getUUID("Uuid"));
                }
            }

            lockedDeployPositions.clear();
            var posList = nbt.getList("LockedDeployPositions", Tag.TAG_COMPOUND);
            for (int i = 0; i < posList.size(); ++i) {
                var posTag = posList.getCompound(i);
                if (posTag.contains("X", Tag.TAG_INT) && posTag.contains("Y", Tag.TAG_INT) && posTag.contains("Z", Tag.TAG_INT)) {
                    lockedDeployPositions.add(new BlockPos(posTag.getInt("X"), posTag.getInt("Y"), posTag.getInt("Z")));
                }
            }
        }
    }
}
