package jp.aquafactory.apprenticecodex.capability;

import jp.aquafactory.apprenticecodex.capability.personalinventory.PersonalInventory;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

public class Capabilities {
    public static Capability<PersonalInventory> PERSONAL_INVENTORY = CapabilityManager.get(new CapabilityToken<>() {});
}