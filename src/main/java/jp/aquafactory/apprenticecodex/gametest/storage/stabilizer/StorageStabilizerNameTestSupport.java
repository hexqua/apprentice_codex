package jp.aquafactory.apprenticecodex.gametest.storage.stabilizer;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;

import static jp.aquafactory.apprenticecodex.gametest.support.TooltipTestSupport.assertTranslatableKey;

public final class StorageStabilizerNameTestSupport {
    private StorageStabilizerNameTestSupport() {}

    public static void assertStorageStabilizerDisplayName(
            GameTestHelper helper,
            ItemStack stack,
            AbstractSpell expectedSpell,
            String message
    ) {
        var displayName = stack.getHoverName();
        helper.assertTrue(displayName.getContents() instanceof TranslatableContents,
                message + " (display name was not translatable: " + displayName + ")");
        if (!(displayName.getContents() instanceof TranslatableContents contents)) {
            return;
        }

        helper.assertTrue("item.apprenticecodex.storage_stabilizer.with_spell".equals(contents.getKey()),
                message + " (unexpected translation key: " + contents.getKey() + ")");
        var args = contents.getArgs();
        helper.assertTrue(args.length == 2,
                message + " (unexpected argument count: " + args.length + ")");
        if (args.length != 2 || !(args[0] instanceof Component baseName) || !(args[1] instanceof Component spellName)) {
            return;
        }

        assertTranslatableKey(helper, baseName, "item.apprenticecodex.storage_stabilizer",
                message + " (unexpected base item name)");
        helper.assertTrue(expectedSpell.getDisplayName(null).getString().equals(spellName.getString()),
                message + " (unexpected spell name: " + spellName.getString() + ")");
    }
}
