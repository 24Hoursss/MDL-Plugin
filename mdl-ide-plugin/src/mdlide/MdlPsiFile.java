package mdlide;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import org.jetbrains.annotations.NotNull;

public final class MdlPsiFile extends PsiFileBase {
    public MdlPsiFile(@NotNull FileViewProvider viewProvider) {
        super(viewProvider, MdlLanguage.INSTANCE);
    }

    @Override
    public @NotNull FileType getFileType() {
        return MdlFileType.INSTANCE;
    }

    @Override
    public String toString() {
        return "MDL file";
    }
}
