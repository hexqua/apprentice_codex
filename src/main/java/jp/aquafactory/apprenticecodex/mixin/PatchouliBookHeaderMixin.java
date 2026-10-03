package jp.aquafactory.apprenticecodex.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vazkii.patchouli.client.book.gui.GuiBook;
import vazkii.patchouli.client.book.gui.GuiBookEntry;
import vazkii.patchouli.common.book.Book;

@Mixin(value = GuiBook.class, remap = false)
public abstract class PatchouliBookHeaderMixin extends Screen {
    @Unique
    private static final ResourceLocation APPRENTICES_MEMO_ID =
            ResourceLocation.fromNamespaceAndPath("apprenticecodex", "apprentices_memo");

    @Shadow
    @Final
    public Book book;

    protected PatchouliBookHeaderMixin(Component title) {
        super(title);
    }

    // 表示補正だけなので、対象メソッドへの注入ができなくても起動を継続する。
    @Inject(method = "drawCenteredStringNoShadow(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/util/FormattedCharSequence;III)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void apprenticecodex$fitFormattedHeader(GuiGraphics graphics, FormattedCharSequence text,
                                                   int x, int y, int color, CallbackInfo ci) {
        if (apprenticecodex$renderFittedHeader(graphics, text, x, y, color)) {
            ci.cancel();
        }
    }

    @Inject(method = "drawCenteredStringNoShadow(Lnet/minecraft/client/gui/GuiGraphics;Ljava/lang/String;III)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void apprenticecodex$fitStringHeader(GuiGraphics graphics, String text,
                                                int x, int y, int color, CallbackInfo ci) {
        if (apprenticecodex$renderFittedHeader(graphics, Component.literal(text).getVisualOrderText(), x, y, color)) {
            ci.cancel();
        }
    }

    @Unique
    private boolean apprenticecodex$renderFittedHeader(GuiGraphics graphics, FormattedCharSequence text,
                                                       int x, int y, int color) {
        // 中央揃え描画は目次やテンプレートにも使われるため、学徒手記の標準ページ見出しに限定する。
        if (!APPRENTICES_MEMO_ID.equals(book.id)
                || !((Object) this instanceof GuiBookEntry)
                || x != GuiBook.PAGE_WIDTH / 2) {
            return false;
        }

        var width = font.width(text);
        if (width <= GuiBook.PAGE_WIDTH) {
            return false;
        }

        // Patchouliの標準見出しは折り返さない。本文とレシピの位置を保ったまま正式名を全文収める。
        var scale = (float) GuiBook.PAGE_WIDTH / width;
        graphics.pose().pushPose();
        graphics.pose().translate(x - GuiBook.PAGE_WIDTH / 2F, y, 0);
        graphics.pose().scale(scale, scale, 1);
        graphics.drawString(font, text, 0, 0, color, false);
        graphics.pose().popPose();
        return true;
    }
}
