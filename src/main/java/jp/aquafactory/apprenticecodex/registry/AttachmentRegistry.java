package jp.aquafactory.apprenticecodex.registry;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.capability.companiontrunkinventory.CompanionTrunkInventory;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellData;
import jp.aquafactory.apprenticecodex.capability.endergrimoire.EnderGrimoireSpellbookData;
import jp.aquafactory.apprenticecodex.capability.personalinventory.PersonalInventory;
import jp.aquafactory.apprenticecodex.spell.bloodbrand.BloodBrandState;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.NotNull;

public final class AttachmentRegistry {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, ApprenticeCodex.MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PersonalInventory>> PERSONAL_INVENTORY =
            ATTACHMENT_TYPES.register("personal_inventory", () -> AttachmentType.builder(PersonalInventory::new)
                    .serialize(new IAttachmentSerializer<CompoundTag, PersonalInventory>() {
                        @Override
                        public @NotNull PersonalInventory read(@NotNull IAttachmentHolder holder, @NotNull CompoundTag tag, net.minecraft.core.HolderLookup.@NotNull Provider provider) {
                            var value = new PersonalInventory();
                            value.deserializeNBT(provider, tag);
                            return value;
                        }

                        @Override
                        public CompoundTag write(@NotNull PersonalInventory attachment, net.minecraft.core.HolderLookup.@NotNull Provider provider) {
                            return attachment.serializeNBT(provider);
                        }
                    })
                    .copyOnDeath()
                    .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<CodexSpellData>> SPELL_DATA =
            ATTACHMENT_TYPES.register("spell_data", () -> AttachmentType.builder(CodexSpellData::new)
                    .serialize(new IAttachmentSerializer<CompoundTag, CodexSpellData>() {
                        @Override
                        public @NotNull CodexSpellData read(@NotNull IAttachmentHolder holder, @NotNull CompoundTag tag, net.minecraft.core.HolderLookup.@NotNull Provider provider) {
                            var value = new CodexSpellData();
                            value.loadAll(tag);
                            return value;
                        }

                        @Override
                        public CompoundTag write(@NotNull CodexSpellData attachment, net.minecraft.core.HolderLookup.@NotNull Provider provider) {
                            return attachment.saveAll();
                        }
                    })
                    .copyOnDeath()
                    .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<CompanionTrunkInventory>> COMPANION_TRUNK_INVENTORY =
            ATTACHMENT_TYPES.register("companion_trunk_inventory", () -> AttachmentType.builder(CompanionTrunkInventory::new)
                    .serialize(new IAttachmentSerializer<CompoundTag, CompanionTrunkInventory>() {
                        @Override
                        public @NotNull CompanionTrunkInventory read(@NotNull IAttachmentHolder holder, @NotNull CompoundTag tag, net.minecraft.core.HolderLookup.@NotNull Provider provider) {
                            var value = new CompanionTrunkInventory();
                            value.deserializeNBT(provider, tag);
                            return value;
                        }

                        @Override
                        public CompoundTag write(@NotNull CompanionTrunkInventory attachment, net.minecraft.core.HolderLookup.@NotNull Provider provider) {
                            return attachment.serializeNBT(provider);
                        }
                    })
                    .copyOnDeath()
                    .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<EnderGrimoireSpellbookData>> ENDER_GRIMOIRE_SPELLBOOK =
            ATTACHMENT_TYPES.register("ender_grimoire_spellbook", () -> AttachmentType.builder(EnderGrimoireSpellbookData::new)
                    .serialize(new IAttachmentSerializer<CompoundTag, EnderGrimoireSpellbookData>() {
                        @Override
                        public @NotNull EnderGrimoireSpellbookData read(@NotNull IAttachmentHolder holder, @NotNull CompoundTag tag, net.minecraft.core.HolderLookup.@NotNull Provider provider) {
                            var value = new EnderGrimoireSpellbookData();
                            value.load(tag);
                            return value;
                        }

                        @Override
                        public CompoundTag write(@NotNull EnderGrimoireSpellbookData attachment, net.minecraft.core.HolderLookup.@NotNull Provider provider) {
                            return attachment.save();
                        }
                    })
                    .copyOnDeath()
                    .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<BloodBrandState>> BLOOD_BRAND_STATE =
            ATTACHMENT_TYPES.register("blood_brand_state", () -> AttachmentType.builder(BloodBrandState::empty)
                    .serialize(new IAttachmentSerializer<CompoundTag, BloodBrandState>() {
                        @Override
                        public @NotNull BloodBrandState read(@NotNull IAttachmentHolder holder, @NotNull CompoundTag tag, net.minecraft.core.HolderLookup.@NotNull Provider provider) {
                            return BloodBrandState.load(tag);
                        }

                        @Override
                        public CompoundTag write(@NotNull BloodBrandState attachment, net.minecraft.core.HolderLookup.@NotNull Provider provider) {
                            return attachment.save();
                        }
                    })
                    .build());

    private AttachmentRegistry() {
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
