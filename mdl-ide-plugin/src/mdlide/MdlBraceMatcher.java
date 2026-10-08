package mdlide;

import com.intellij.lang.BracePair;
import com.intellij.lang.PairedBraceMatcher;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

public final class MdlBraceMatcher implements PairedBraceMatcher {
    private static final BracePair[] PAIRS = {
            new BracePair(MdlTokenTypes.LBRACE, MdlTokenTypes.RBRACE, true),
            new BracePair(MdlTokenTypes.LPAREN, MdlTokenTypes.RPAREN, false),
    };

    @Override
    public BracePair @NotNull [] getPairs() {
        return PAIRS;
    }

    @Override
    public boolean isPairedBracesAllowedBeforeType(
            @NotNull IElementType lbraceType, IElementType contextType) {
        return true;
    }

    @Override
    public int getCodeConstructStart(@NotNull PsiFile file, int offset) {
        return -1;
    }
}
