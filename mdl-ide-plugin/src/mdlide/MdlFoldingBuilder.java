package mdlide;

import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.FoldingBuilderEx;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * Folds column-0 brace blocks. The fold region starts at the header line
 * (the nearest non-blank line above the opening brace, e.g. the LINK line)
 * and ends at the closing brace line, so a folded block reads like its
 * table/transform header. Computed from the document text, not from PSI.
 */
public final class MdlFoldingBuilder extends FoldingBuilderEx {
    private static final String FALLBACK_PLACEHOLDER = "{...}";

    @Override
    public FoldingDescriptor @NotNull [] buildFoldRegions(
            @NotNull PsiElement element, @NotNull Document document, boolean quick) {
        return compute(document, element);
    }

    @Override
    public FoldingDescriptor @NotNull [] buildFoldRegions(
            @NotNull ASTNode node, @NotNull Document document) {
        return compute(document, node.getPsi());
    }

    @Override
    public @Nullable String getPlaceholderText(@NotNull ASTNode node, @NotNull TextRange range) {
        return null;
    }

    @Override
    public @Nullable String getPlaceholderText(@NotNull ASTNode node) {
        return null;
    }

    @Override
    public boolean isCollapsedByDefault(@NotNull ASTNode node) {
        return true;
    }

    private FoldingDescriptor @NotNull [] compute(@NotNull Document document, @NotNull PsiElement file) {
        CharSequence text = document.getImmutableCharSequence();
        int length = text.length();
        List<FoldingDescriptor> folds = new ArrayList<>();
        Deque<OpenBlock> stack = new ArrayDeque<>();
        int lastHeaderStart = -1;
        int lastHeaderEnd = -1;
        int lineStart = 0;
        while (lineStart <= length) {
            int lineEnd = indexOfNewline(text, lineStart, length);
            if (lineEnd < 0) {
                lineEnd = length;
            }
            int contentEnd = lineEnd;
            if (contentEnd > lineStart && text.charAt(contentEnd - 1) == '\r') {
                contentEnd--;
            }
            int ce = contentEnd;
            while (ce > lineStart && isSpaceOrTab(text.charAt(ce - 1))) {
                ce--;
            }
            if (ce > lineStart) {
                char first = text.charAt(lineStart);
                if (first == '{' && isOnlyBrace(text, lineStart, ce)) {
                    stack.push(new OpenBlock(lineStart, lastHeaderStart, lastHeaderEnd));
                } else if (first == '}' && isOnlyBrace(text, lineStart, ce)) {
                    OpenBlock open = stack.poll();
                    if (open != null) {
                        int start = open.headerStart >= 0 ? open.headerStart : open.start;
                        if (!(start == 0 && ce >= length - 1)) {
                            // skip the pair that spans the whole file: folding would
                            // collapse the deck to a single header
                            String placeholder = (open.headerStart >= 0
                                    ? text.subSequence(open.headerStart, open.headerEnd).toString()
                                    : FALLBACK_PLACEHOLDER).trim();
                            // Every block opens collapsed: a deck shows as an
                            // outline of headers and expands on demand.
                            folds.add(new FoldingDescriptor(
                                    file.getNode(), new TextRange(start, ce), null,
                                    placeholder, Boolean.TRUE, Collections.emptySet()));
                        }
                    }
                } else {
                    lastHeaderStart = lineStart;
                    lastHeaderEnd = ce;
                }
            }
            if (lineEnd >= length) {
                break;
            }
            lineStart = lineEnd + 1;
        }
        return folds.toArray(FoldingDescriptor.EMPTY);
    }

    /** The line contains only the brace itself (trailing spaces already trimmed). */
    private boolean isOnlyBrace(CharSequence text, int start, int end) {
        return end - start == 1;
    }

    private static boolean isSpaceOrTab(char c) {
        return c == ' ' || c == '\t';
    }

    private static int indexOfNewline(CharSequence text, int from, int limit) {
        for (int i = from; i < limit; i++) {
            if (text.charAt(i) == '\n') {
                return i;
            }
        }
        return -1;
    }

    private static final class OpenBlock {
        final int start;         // offset of the opening brace
        final int headerStart;   // offset of the header line, or -1
        final int headerEnd;     // end offset of the header line (exclusive of newline)

        OpenBlock(int start, int headerStart, int headerEnd) {
            this.start = start;
            this.headerStart = headerStart;
            this.headerEnd = headerEnd;
        }
    }
}
