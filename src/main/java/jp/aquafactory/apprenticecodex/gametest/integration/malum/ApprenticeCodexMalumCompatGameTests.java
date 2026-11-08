package jp.aquafactory.apprenticecodex.gametest.integration.malum;

import net.minecraft.resources.ResourceLocation;

import static jp.aquafactory.apprenticecodex.gametest.integration.malum.MalumLegacyGameTests.MALUM_MOD_ID;

final class ApprenticeCodexMalumCompatGameTests {
    private ApprenticeCodexMalumCompatGameTests() {}

    static final ResourceLocation MALUM_REPLENISHING =
            ResourceLocation.fromNamespaceAndPath(MALUM_MOD_ID, "replenishing");
}
