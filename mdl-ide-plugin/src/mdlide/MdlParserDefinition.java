package mdlide;

import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * Flat syntax tree: every non-whitespace token is a child of the file element,
 * with no block grammar. Folding and brace matching operate on the document
 * text / leaf tokens rather than on parser-produced blocks.
 */
public final class MdlParserDefinition implements ParserDefinition {
    private static final IFileElementType FILE = new IFileElementType("MDL_FILE", MdlLanguage.INSTANCE);

    @Override
    public @NotNull Lexer createLexer(Project project) {
        return new MdlLexer();
    }

    @Override
    public @NotNull PsiParser createParser(Project project) {
        return new PsiParser() {
            @Override
            public @NotNull ASTNode parse(@NotNull IElementType root, @NotNull PsiBuilder builder) {
                PsiBuilder.Marker file = builder.mark();
                while (!builder.eof()) {
                    builder.advanceLexer();
                }
                file.done(root);
                return builder.getTreeBuilt();
            }
        };
    }

    @Override
    public @NotNull IFileElementType getFileNodeType() {
        return FILE;
    }

    @Override
    public @NotNull TokenSet getWhitespaceTokens() {
        return TokenSet.create(TokenType.WHITE_SPACE);
    }

    @Override
    public @NotNull TokenSet getCommentTokens() {
        return TokenSet.EMPTY; // MDL has no comment syntax
    }

    @Override
    public @NotNull TokenSet getStringLiteralElements() {
        return TokenSet.create(MdlTokenTypes.STRING);
    }

    @Override
    public @NotNull PsiElement createElement(@NotNull ASTNode node) {
        return new ASTWrapperPsiElement(node);
    }

    @Override
    public @NotNull PsiFile createFile(@NotNull FileViewProvider viewProvider) {
        return new MdlPsiFile(viewProvider);
    }
}
