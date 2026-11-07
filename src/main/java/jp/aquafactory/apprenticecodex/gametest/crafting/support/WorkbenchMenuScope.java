package jp.aquafactory.apprenticecodex.gametest.crafting.support;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** 同期テスト専用playerのメニューと入力を、途中失敗でも解放する。 */
public final class WorkbenchMenuScope implements AutoCloseable {
    private final List<Entry> entries = new ArrayList<>();

    public <T extends AbstractContainerMenu> T track(T menu, Player player) {
        entries.add(new Entry(menu, player));
        return menu;
    }

    @Override
    public void close() {
        Throwable failure = null;
        for (var index = entries.size() - 1; index >= 0; --index) {
            var entry = entries.get(index);
            try {
                // 入力返却を通す際に満杯のテストinventoryからworldへdropさせない。
                entry.player().getInventory().clearContent();
                entry.menu().removed(entry.player());
            } catch (RuntimeException | Error thrown) {
                if (failure == null) {
                    failure = thrown;
                } else {
                    failure.addSuppressed(thrown);
                }
            } finally {
                entry.player().getInventory().clearContent();
            }
        }
        entries.clear();
        if (failure instanceof RuntimeException exception) {
            throw exception;
        }
        if (failure instanceof Error error) {
            throw error;
        }
    }

    private record Entry(AbstractContainerMenu menu, Player player) {}
}
