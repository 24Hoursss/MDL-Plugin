package mdlide;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * Runs {@link MdlCheck} on the file and paints the results in the editor
 * (red squiggles for errors, yellow for warnings). Both soft, like a
 * spellchecker: an unbalanced brace still opens the file.
 */
public final class MdlAnnotator implements Annotator {
    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (!(element instanceof MdlPsiFile)) {
            return;
        }
        String text = element.getText();
        if (text == null) {
            return;
        }
        for (MdlCheck.Issue issue : MdlCheck.check(text)) {
            int end = Math.min(issue.start + issue.len, text.length());
            holder.newAnnotation(issue.error ? HighlightSeverity.ERROR
                            : HighlightSeverity.WARNING, issue.message)
                    .range(new TextRange(issue.start, end))
                    .create();
        }
    }

    @Override
    public boolean isDumbAware() {
        return true;
    }
}
