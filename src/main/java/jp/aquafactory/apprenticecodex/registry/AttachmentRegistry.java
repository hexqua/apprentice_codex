package jp.aquafactory.apprenticecodex.registry;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.capability.companiontrunkinventory.CompanionTrunkInventory;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellData;
import jp.aquafactory.apprenticecodex.capability.endergrimoire.EnderGrimoireSpellbookData;
import jp.aquafactory.apprenticecodex.capability.personalinventory.PersonalInventory;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class AttachmentRegistry {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, ApprenticeCodex.MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PersonalInventory>> PERSONAL_INVENTORY =
            ATTACHMENT_TYPES.register("personal_inventory", () -> AttachmentType.builder(PersonalInventory::new)
                    .serialize(new IAttachmentSerializer<CompoundTag, PersonalInventory>() {
                        @Override
                        public PersonalInventory read(IAttachmentHolder holder, CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
                            var value = new PersonalInventory();
                            value.deserializeNBT(provider, tag);
                            return value;
                        }

                        @Override
                        public CompoundTag write(PersonalInventory attachment, net.minecraft.core.HolderLookup.Provider provider) {
                            return attachment.serializeNBT(provider);
                        }
                    })
                    .copyOnDeath()
                    .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<CodexSpellData>> SPELL_DATA =
            ATTACHMENT_TYPES.register("spell_data", () -> AttachmentType.builder(CodexSpellData::new)
                    .serialize(new IAttachmentSerializer<CompoundTag, CodexSpellData>() {
                        @Override
                        public CodexSpellData read(IAttachmentHolder holder, CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
                            var value = new CodexSpellData();
                            value.loadAll(tag);
                            return value;
                        }

                        @Override
                        public CompoundTag write(CodexSpellData attachment, net.minecraft.core.HolderLookup.Provider provider) {
                            return attachment.saveAll();
                        }
                    })
                    .copyOnDeath()
                    .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<CompanionTrunkInventory>> COMPANION_TRUNK_INVENTORY =
            ATTACHMENT_TYPES.register("companion_trunk_inventory", () -> AttachmentType.builder(CompanionTrunkInventory::new)
                    .serialize(new IAttachmentSerializer<CompoundTag, CompanionTrunkInventory>() {
                        @Override
                        public CompanionTrunkInventory read(IAttachmentHolder holder, CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
                            var value = new CompanionTrunkInventory();
                            value.deserializeNBT(provider, tag);
                            return value;
                        }

                        @Override
                        public CompoundTag write(CompanionTrunkInventory attachment, net.minecraft.core.HolderLookup.Provider provider) {
                            return attachment.serializeNBT(provider);
                        }
                    })
                    .copyOnDeath()
                    .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<EnderGrimoireSpellbookData>> ENDER_GRIMOIRE_SPELLBOOK =
            ATTACHMENT_TYPES.register("ender_grimoire_spellbook", () -> AttachmentType.builder(EnderGrimoireSpellbookData::new)
                    .serialize(new IAttachmentSerializer<CompoundTag, EnderGrimoireSpellbookData>() {
                        @Override
                        public EnderGrimoireSpellbookData read(IAttachmentHolder holder, CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
                            var value = new EnderGrimoireSpellbookData();
                            value.load(tag);
                            return value;
                        }

                        @Override
                        public CompoundTag write(EnderGrimoireSpellbookData attachment, net.minecraft.core.HolderLookup.Provider provider) {
                            return attachment.save();
                        }
                    })
                    .copyOnDeath()
                    .build());

    private AttachmentRegistry() {
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
