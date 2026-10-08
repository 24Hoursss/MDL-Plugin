package mdlide;

import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public final class MdlHighlighterFactory extends SyntaxHighlighterFactory {
    @Override
    public @NotNull SyntaxHighlighter getSyntaxHighlighter(
            Project project, VirtualFile virtualFile) {
        return new MdlSyntaxHighlighter();
    }
}
