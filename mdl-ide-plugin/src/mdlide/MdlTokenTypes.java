package mdlide;

import com.intellij.psi.tree.IElementType;

/**
 * Lexer token types for MDL decks. Block structure follows the same rule the
 * iccap-mdl-edit agent uses: structural braces are lines whose only character
 * is a brace at column 0; braces inside Program/Python bodies are body text.
 */
public final class MdlTokenTypes {
    private MdlTokenTypes() {
    }

    public static final IElementType KEYWORD = new IElementType("MDL_KEYWORD", MdlLanguage.INSTANCE);
    public static final IElementType LINK_KIND = new IElementType("MDL_LINK_KIND", MdlLanguage.INSTANCE);
    public static final IElementType LBRACE = new IElementType("MDL_LBRACE", MdlLanguage.INSTANCE);
    public static final IElementType RBRACE = new IElementType("MDL_RBRACE", MdlLanguage.INSTANCE);
    public static final IElementType LPAREN = new IElementType("MDL_LPAREN", MdlLanguage.INSTANCE);
    public static final IElementType RPAREN = new IElementType("MDL_RPAREN", MdlLanguage.INSTANCE);
    public static final IElementType BRACKET = new IElementType("MDL_BRACKET", MdlLanguage.INSTANCE);
    public static final IElementType STRING = new IElementType("MDL_STRING", MdlLanguage.INSTANCE);
    public static final IElementType NUMBER = new IElementType("MDL_NUMBER", MdlLanguage.INSTANCE);
    public static final IElementType IDENTIFIER = new IElementType("MDL_IDENTIFIER", MdlLanguage.INSTANCE);
    public static final IElementType OPERATION = new IElementType("MDL_OPERATION", MdlLanguage.INSTANCE);
    /** {@code #load}, {@code #echo}, {@code #include}, {@code #uselib}, ... directives. */
    public static final IElementType PREPROCESSOR = new IElementType("MDL_PREPROCESSOR", MdlLanguage.INSTANCE);
    /** {@code $dpar(...)}, {@code $mpar(...)}, {@code $xpar(...)}, {@code $var(...)} references. */
    public static final IElementType VARREF = new IElementType("MDL_VARREF", MdlLanguage.INSTANCE);
    /** Netlist keywords inside a circuitdeck body: define, model, end, global, Options, ... */
    public static final IElementType NETLIST_KEYWORD = new IElementType("MDL_NETLIST_KEYWORD", MdlLanguage.INSTANCE);
    /** Netlist function calls: file{}, dep_data(...), fg(...), min, max, ... */
    public static final IElementType NETLIST_FUNC = new IElementType("MDL_NETLIST_FUNC", MdlLanguage.INSTANCE);
    /** Instance prefix TYPE:INAME (C:Cpg, R:Rg, M:X1, ...). */
    public static final IElementType NETLIST_COMPONENT = new IElementType("MDL_NETLIST_COMPONENT", MdlLanguage.INSTANCE);
    /** PEL statement keywords inside a BLKEDIT "Program Body". */
    public static final IElementType PEL_KEYWORD = new IElementType("MDL_PEL_KEYWORD", MdlLanguage.INSTANCE);
    /** PEL built-in constants: PI, 2PI, J. */
    public static final IElementType PEL_CONST = new IElementType("MDL_PEL_CONST", MdlLanguage.INSTANCE);
    /** {@code ;} netlist comment or {@code !} PEL comment, to end of line. */
    public static final IElementType COMMENT = new IElementType("MDL_COMMENT", MdlLanguage.INSTANCE);
    public static final IElementType OTHER = new IElementType("MDL_OTHER", MdlLanguage.INSTANCE);
}
