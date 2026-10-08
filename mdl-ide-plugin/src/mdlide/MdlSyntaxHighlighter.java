package mdlide;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public final class MdlSyntaxHighlighter extends SyntaxHighlighterBase {
    private static final Map<IElementType, TextAttributesKey> KEYS = new HashMap<>();
    private static final TextAttributesKey[] KEYWORD_KEYS =
            pack(DefaultLanguageHighlighterColors.KEYWORD);
    private static final TextAttributesKey[] NUMBER_KEYS =
            pack(DefaultLanguageHighlighterColors.NUMBER);
    private static final TextAttributesKey[] STRING_KEYS =
            pack(DefaultLanguageHighlighterColors.STRING);
    private static final TextAttributesKey[] IDENTIFIER_KEYS =
            pack(DefaultLanguageHighlighterColors.IDENTIFIER);
    private static final TextAttributesKey[] VARREF_KEYS =
            pack(DefaultLanguageHighlighterColors.CONSTANT);
    private static final TextAttributesKey[] PREPROCESSOR_KEYS =
            pack(DefaultLanguageHighlighterColors.METADATA);
    private static final TextAttributesKey[] NETLIST_KEYS =
            pack(DefaultLanguageHighlighterColors.FUNCTION_CALL);
    private static final TextAttributesKey[] BRACE_KEYS =
            pack(DefaultLanguageHighlighterColors.BRACES);
    private static final TextAttributesKey[] PAREN_KEYS =
            pack(DefaultLanguageHighlighterColors.PARENTHESES);
    private static final TextAttributesKey[] OPERATION_KEYS =
            pack(DefaultLanguageHighlighterColors.OPERATION_SIGN);
    private static final TextAttributesKey[] COMMENT_KEYS =
            pack(DefaultLanguageHighlighterColors.LINE_COMMENT);
    private static final TextAttributesKey[] BRACKET_KEYS =
            pack(DefaultLanguageHighlighterColors.BRACKETS);

    static {
        KEYS.put(MdlTokenTypes.KEYWORD, DefaultLanguageHighlighterColors.KEYWORD);
        KEYS.put(MdlTokenTypes.LINK_KIND, DefaultLanguageHighlighterColors.KEYWORD);
        KEYS.put(MdlTokenTypes.NUMBER, DefaultLanguageHighlighterColors.NUMBER);
        KEYS.put(MdlTokenTypes.STRING, DefaultLanguageHighlighterColors.STRING);
        KEYS.put(MdlTokenTypes.IDENTIFIER, DefaultLanguageHighlighterColors.IDENTIFIER);
        KEYS.put(MdlTokenTypes.VARREF, DefaultLanguageHighlighterColors.CONSTANT);
        KEYS.put(MdlTokenTypes.PREPROCESSOR, DefaultLanguageHighlighterColors.METADATA);
        KEYS.put(MdlTokenTypes.NETLIST_KEYWORD, DefaultLanguageHighlighterColors.FUNCTION_CALL);
        KEYS.put(MdlTokenTypes.NETLIST_FUNC, DefaultLanguageHighlighterColors.FUNCTION_CALL);
        KEYS.put(MdlTokenTypes.NETLIST_COMPONENT, DefaultLanguageHighlighterColors.FUNCTION_CALL);
        KEYS.put(MdlTokenTypes.PEL_KEYWORD, DefaultLanguageHighlighterColors.KEYWORD);
        KEYS.put(MdlTokenTypes.PEL_CONST, DefaultLanguageHighlighterColors.CONSTANT);
        KEYS.put(MdlTokenTypes.COMMENT, DefaultLanguageHighlighterColors.LINE_COMMENT);
        KEYS.put(MdlTokenTypes.LBRACE, DefaultLanguageHighlighterColors.BRACES);
        KEYS.put(MdlTokenTypes.RBRACE, DefaultLanguageHighlighterColors.BRACES);
        KEYS.put(MdlTokenTypes.LPAREN, DefaultLanguageHighlighterColors.PARENTHESES);
        KEYS.put(MdlTokenTypes.RPAREN, DefaultLanguageHighlighterColors.PARENTHESES);
        KEYS.put(MdlTokenTypes.BRACKET, DefaultLanguageHighlighterColors.BRACKETS);
        KEYS.put(MdlTokenTypes.OPERATION, DefaultLanguageHighlighterColors.OPERATION_SIGN);
    }

    @Override
    public @NotNull Lexer getHighlightingLexer() {
        return new MdlLexer();
    }

    @Override
    public TextAttributesKey @NotNull [] getTokenHighlights(IElementType tokenType) {
        if (tokenType == MdlTokenTypes.KEYWORD || tokenType == MdlTokenTypes.LINK_KIND) {
            return KEYWORD_KEYS;
        }
        if (tokenType == MdlTokenTypes.NUMBER) {
            return NUMBER_KEYS;
        }
        if (tokenType == MdlTokenTypes.STRING) {
            return STRING_KEYS;
        }
        if (tokenType == MdlTokenTypes.IDENTIFIER) {
            return IDENTIFIER_KEYS;
        }
        if (tokenType == MdlTokenTypes.VARREF) {
            return VARREF_KEYS;
        }
        if (tokenType == MdlTokenTypes.PREPROCESSOR) {
            return PREPROCESSOR_KEYS;
        }
        if (tokenType == MdlTokenTypes.NETLIST_KEYWORD
                || tokenType == MdlTokenTypes.NETLIST_FUNC
                || tokenType == MdlTokenTypes.NETLIST_COMPONENT) {
            return NETLIST_KEYS;
        }
        if (tokenType == MdlTokenTypes.PEL_KEYWORD) {
            return KEYWORD_KEYS;
        }
        if (tokenType == MdlTokenTypes.PEL_CONST) {
            return VARREF_KEYS;
        }
        if (tokenType == MdlTokenTypes.COMMENT) {
            return COMMENT_KEYS;
        }
        if (tokenType == MdlTokenTypes.BRACKET) {
            return BRACKET_KEYS;
        }
        if (tokenType == MdlTokenTypes.LBRACE || tokenType == MdlTokenTypes.RBRACE) {
            return BRACE_KEYS;
        }
        if (tokenType == MdlTokenTypes.LPAREN || tokenType == MdlTokenTypes.RPAREN) {
            return PAREN_KEYS;
        }
        if (tokenType == MdlTokenTypes.OPERATION) {
            return OPERATION_KEYS;
        }
        return EMPTY;
    }
}
