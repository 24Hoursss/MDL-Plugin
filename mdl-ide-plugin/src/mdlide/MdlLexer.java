package mdlide;

import com.intellij.lexer.Lexer;
import com.intellij.lexer.LexerPosition;
import com.intellij.lexer.LexerPositionImpl;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Line-oriented lexer for IC-CAP MDL decks.
 *
 * <p>Structural rule (same as the iccap-mdl-edit agent): braces count only at
 * column 0. The lexer tracks the brace depth to know which body region it is
 * in, and applies context-specific rules per the iccap-hpeesofsim /
 * iccap-pel skills:</p>
 * <ul>
 *   <li>circuitdeck body: ADS netlist keywords (define/model/end/global/...),
 *       VarEqn keywords (if/then/else/endif/equals), functions (file/fg/...),
 *       instances (TYPE:INAME), ';' comments</li>
 *   <li>BLKEDIT "Program Body": PEL keywords (case-insensitive, FOR/STEP/IF/
 *       WHILE/ICCAP_FUNC/...), constants (PI/2PI/J), '!' comments</li>
 *   <li>BLKEDIT "Macro Editor": Python source ('#' lines stay directives;
 *       keyword coloring is the VSCode embed's job)</li>
 * </ul>
 *
 * <p>State (bit packed so incremental re-lexing can resume mid-file):</p>
 * <pre>
 *   bit 0      atLineStart  (no non-whitespace token on the current line yet)
 *   bit 1      prevWasLink  (a LINE-start keyword "LINK" was the previous token)
 *   bit 2      pendingCirc  (saw "circuitdeck" at line start, waiting for its '{')
 *   bit 3      pendingBlk   (saw "BLKEDIT" at line start, waiting for its string)
 *   bit 4      pendingPel   (the BLKEDIT string was "Program Body")
 *   bit 5      pendingPy    (the BLKEDIT string was "Macro Editor")
 *   bits 6-7   ctx          0 none, 1 circuitdeck, 2 PEL, 3 Python
 *   bits 8-19  ctxDepth     brace depth of the context block
 *   bits 20-31 depth        open column-0 '{' count
 * </pre>
 */
public final class MdlLexer extends Lexer {
    private static final int AT_LINE_START = 1;
    private static final int PREV_WAS_LINK = 2;
    private static final int PENDING_CIRC = 4;
    private static final int PENDING_BLK = 8;
    private static final int PENDING_PEL = 1 << 4;
    private static final int PENDING_PY = 1 << 5;
    private static final int CTX_SHIFT = 6;
    private static final int CTX_MASK = 0x3;
    private static final int CTX_NONE = 0;
    private static final int CTX_CIRC = 1;
    private static final int CTX_PEL = 2;
    private static final int CTX_PY = 3;
    private static final int CTX_DEPTH_SHIFT = 8;
    private static final int DEPTH_SHIFT = 20;
    private static final int MASK = 0x0FFF;

    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
            "LINK", "list", "element", "param", "fullprecision", "View",
            "dataset", "datasize", "type", "member",
            "BLKEDIT", "TABLE", "HYPTABLE", "data", "circuitdeck", "OPTIMEDIT",
            "__iccapDataBlockEnd"));
    private static final Set<String> LINK_KINDS = new HashSet<>(Arrays.asList(
            "MODEL", "DUT", "DAT", "SWEEP", "OUT", "XFORM", "PLOT", "MACRO",
            "CIRC", "TCIRC", "PS", "DPS"));
    /** ADS / hpeesofsim netlist keywords (iccap-hpeesofsim ads/grammar.md). */
    private static final Set<String> NETLIST_KEYWORDS = new HashSet<>(Arrays.asList(
            "define", "model", "end", "subckt", "ends", "parameters", "include",
            "global", "globalnode", "ground", "Options", "ParamSweep",
            "if", "then", "else", "endif", "equals", "not", "and", "or"));
    /** ADS VarEqn / DAC functions. */
    private static final Set<String> NETLIST_FUNCS = new HashSet<>(Arrays.asList(
            "file", "list", "dep_data", "read_data", "access_all_data", "fg",
            "index", "abs", "sqrt", "exp", "log", "pow", "sin", "cos", "mag",
            "min", "max", "floor", "ceil", "signum"));
    /** PEL statements and built-in functions (iccap-pel SKILL.md). */
    private static final Set<String> PEL_KEYWORDS = new HashSet<>(Arrays.asList(
            "FOR", "STEP", "NEXT", "LOOP", "ENDLOOP", "WHILE", "ENDWHILE",
            "IF", "ELSEIF", "ELSE", "ENDIF", "EXIT", "RETURN", "RETURN_VALUE",
            "GLOBAL_VAR", "GET_INT", "GET_REAL", "GET_STRING", "LINPUT",
            "GET_DATASET", "ICCAP_FUNC", "USERC_num_of_points", "SIZEOF",
            "FLOOR", "ABS", "MAG", "SIZE", "REAL", "IMAG", "NOT", "AND", "OR",
            "SIN", "COS", "EXP", "LOG", "SQRT", "TAN", "ATAN", "POW", "MIN",
            "MAX", "MOD", "TWOPORT", "PI", "2PI", "J"));
    private static final Set<String> PEL_CONSTS = new HashSet<>(Arrays.asList(
            "PI", "2PI", "J"));

    private CharSequence myBuffer;
    private int myEnd;
    private int myOffset;      // current token start
    private int myTokenEnd;
    private IElementType myTokenType;
    private int myStateAtTokenStart;

    // decoded state, valid for the current token start at the beginning of scanToken()
    private boolean atLineStart;
    private boolean prevWasLink;
    private boolean pendingCirc;
    private boolean pendingBlk;
    private boolean pendingPel;
    private boolean pendingPy;
    private int ctx;           // CTX_*
    private int ctxDepth;
    private int depth;

    @Override
    public void start(@NotNull CharSequence buffer, int startOffset, int endOffset, int initialState) {
        myBuffer = buffer;
        myEnd = endOffset;
        myOffset = startOffset;
        decodeState(initialState);
        if (startOffset == 0) {
            atLineStart = true; // column 0 of the file is a line start
        }
        scanToken();
    }

    @Override
    public void advance() {
        myOffset = myTokenEnd;
        scanToken();
    }

    @Override
    public int getState() {
        return myStateAtTokenStart;
    }

    @Override
    public IElementType getTokenType() {
        return myTokenType;
    }

    @Override
    public int getTokenStart() {
        return myOffset;
    }

    @Override
    public int getTokenEnd() {
        return myTokenEnd;
    }

    @Override
    public @NotNull CharSequence getBufferSequence() {
        return myBuffer;
    }

    @Override
    public int getBufferEnd() {
        return myEnd;
    }

    @Override
    public @NotNull LexerPosition getCurrentPosition() {
        return new LexerPositionImpl(myOffset, myStateAtTokenStart);
    }

    @Override
    public void restore(@NotNull LexerPosition position) {
        start(myBuffer, position.getOffset(), myEnd, position.getState());
    }

    /** Whether the lexer state was inside a circuitdeck body. */
    public static boolean stateInCirc(int state) {
        return ((state >> CTX_SHIFT) & CTX_MASK) == CTX_CIRC;
    }

    /** Whether the lexer state was inside a BLKEDIT "Macro Editor" (Python) body. */
    public static boolean stateInPython(int state) {
        return ((state >> CTX_SHIFT) & CTX_MASK) == CTX_PY;
    }

    /** Whether the lexer state was at the first non-whitespace token of a line. */
    public static boolean stateAtLineStart(int state) {
        return (state & AT_LINE_START) != 0;
    }

    private void scanToken() {
        myStateAtTokenStart = encodeState();
        if (myOffset >= myEnd) {
            myTokenType = null;
            myTokenEnd = myEnd;
            return;
        }
        int p = myOffset;
        char c = myBuffer.charAt(p);
        if (isWhitespace(c)) {
            boolean sawNewline = false;
            while (p < myEnd && isWhitespace(c = myBuffer.charAt(p))) {
                if (c == '\n') {
                    sawNewline = true;
                }
                p++;
            }
            if (sawNewline) {
                atLineStart = true;
            }
            myTokenType = TokenType.WHITE_SPACE;
        } else if (c == '"') {
            int start = p + 1;
            p++;
            // MDL strings never span lines; an unterminated string ends at EOL
            while (p < myEnd && myBuffer.charAt(p) != '"' && myBuffer.charAt(p) != '\n') {
                p++;
            }
            if (pendingBlk) {
                String content = myBuffer.subSequence(start, Math.min(p, myEnd)).toString();
                if ("Program Body".equals(content)) {
                    pendingPel = true;
                } else if ("Macro Editor".equals(content)) {
                    pendingPy = true;
                }
                pendingBlk = false;
            }
            if (p < myEnd) {
                p++; // closing quote
            }
            atLineStart = false;
            prevWasLink = false;
            myTokenType = MdlTokenTypes.STRING;
        } else if (c >= '0' && c <= '9') {
            p = scanNumber(p);
            atLineStart = false;
            prevWasLink = false;
            myTokenType = MdlTokenTypes.NUMBER;
        } else if (c == '#') {
            p++;
            while (p < myEnd && isWordChar(myBuffer.charAt(p))) {
                p++;
            }
            atLineStart = false;
            prevWasLink = false;
            myTokenType = MdlTokenTypes.PREPROCESSOR;
        } else if (c == '$') {
            p++;
            while (p < myEnd && isWordChar(myBuffer.charAt(p))) {
                p++;
            }
            atLineStart = false;
            prevWasLink = false;
            myTokenType = MdlTokenTypes.VARREF;
        } else if (c == '!' && ctx == CTX_PEL) {
            p++;
            while (p < myEnd && myBuffer.charAt(p) != '\n') {
                p++;
            }
            atLineStart = false;
            prevWasLink = false;
            myTokenType = MdlTokenTypes.COMMENT;
        } else if (c == ';' && ctx == CTX_CIRC) {
            p++;
            while (p < myEnd && myBuffer.charAt(p) != '\n') {
                p++;
            }
            atLineStart = false;
            prevWasLink = false;
            myTokenType = MdlTokenTypes.COMMENT;
        } else if (isWordStart(c)) {
            int wordStart = p;
            while (p < myEnd && isWordChar(myBuffer.charAt(p))) {
                p++;
            }
            String word = myBuffer.subSequence(wordStart, p).toString();
            boolean atStart = atLineStart;
            if (atStart && "circuitdeck".equals(word)) {
                pendingCirc = true;
            }
            if (atStart && "BLKEDIT".equals(word)) {
                pendingBlk = true;
            }
            if (ctx == CTX_CIRC) {
                if (NETLIST_KEYWORDS.contains(word)) {
                    myTokenType = MdlTokenTypes.NETLIST_KEYWORD;
                } else if (p < myEnd && myBuffer.charAt(p) == ':' && !isKeywordWord(word)) {
                    myTokenType = MdlTokenTypes.NETLIST_COMPONENT;
                } else if (NETLIST_FUNCS.contains(word)) {
                    myTokenType = MdlTokenTypes.NETLIST_FUNC;
                } else {
                    myTokenType = MdlTokenTypes.IDENTIFIER;
                }
            } else if (ctx == CTX_PEL) {
                String upper = word.toUpperCase();
                if (PEL_CONSTS.contains(upper)) {
                    myTokenType = MdlTokenTypes.PEL_CONST;
                } else if (PEL_KEYWORDS.contains(upper)) {
                    myTokenType = MdlTokenTypes.PEL_KEYWORD;
                } else {
                    myTokenType = MdlTokenTypes.IDENTIFIER;
                }
            } else if (ctx == CTX_PY) {
                myTokenType = MdlTokenTypes.IDENTIFIER;
            } else if (atStart && ("LINK".equals(word) || KEYWORDS.contains(word))) {
                myTokenType = MdlTokenTypes.KEYWORD;
            } else if (prevWasLink && LINK_KINDS.contains(word)) {
                myTokenType = MdlTokenTypes.LINK_KIND;
            } else {
                myTokenType = MdlTokenTypes.IDENTIFIER;
            }
            if (pendingCirc && !atStart) {
                pendingCirc = false; // "circuitdeck" was not a line-start word after all
            }
            if (pendingBlk && !atStart && !"BLKEDIT".equals(word)) {
                pendingBlk = false; // BLKEDIT not at line start
            }
            atLineStart = false;
            prevWasLink = atStart && "LINK".equals(word);
        } else if (c == '{') {
            p++;
            boolean structural = atColumnZero();
            atLineStart = false;
            prevWasLink = false;
            if (structural) {
                depth++;
                if (pendingCirc) {
                    ctx = CTX_CIRC;
                    ctxDepth = depth;
                    pendingCirc = false;
                } else if (pendingPel) {
                    ctx = CTX_PEL;
                    ctxDepth = depth;
                    pendingPel = false;
                } else if (pendingPy) {
                    ctx = CTX_PY;
                    ctxDepth = depth;
                    pendingPy = false;
                }
            }
            myTokenType = MdlTokenTypes.LBRACE;
        } else if (c == '}') {
            p++;
            boolean structural = atColumnZero();
            atLineStart = false;
            prevWasLink = false;
            if (structural) {
                if (ctx != CTX_NONE && depth == ctxDepth) {
                    ctx = CTX_NONE;
                }
                if (depth > 0) {
                    depth--;
                }
            }
            myTokenType = MdlTokenTypes.RBRACE;
        } else if (c == '(') {
            p++;
            atLineStart = false;
            prevWasLink = false;
            myTokenType = MdlTokenTypes.LPAREN;
        } else if (c == ')') {
            p++;
            atLineStart = false;
            prevWasLink = false;
            myTokenType = MdlTokenTypes.RPAREN;
        } else if (c == '[' || c == ']') {
            p++;
            atLineStart = false;
            prevWasLink = false;
            myTokenType = MdlTokenTypes.BRACKET;
        } else if (c == '=' || c == ',' || c == ':' || c == '+' || c == '-'
                || c == '/' || c == '*' || c == '^' || c == '&' || c == '<' || c == '>') {
            p++;
            atLineStart = false;
            prevWasLink = false;
            myTokenType = MdlTokenTypes.OPERATION;
        } else {
            p++;
            atLineStart = false;
            prevWasLink = false;
            myTokenType = MdlTokenTypes.OTHER;
        }
        if (p <= myOffset) {
            // safety net: a lexer must never emit a zero-length token
            p = myOffset + 1;
            myTokenType = MdlTokenTypes.OTHER;
        }
        myTokenEnd = p;
    }

    private boolean isKeywordWord(String word) {
        return NETLIST_KEYWORDS.contains(word) || NETLIST_FUNCS.contains(word);
    }

    /**
     * Structural braces are lines that are exactly '{' or '}' at column 0, i.e.
     * the brace is the very first character of the line. An indented brace is
     * body text (Program/Python bodies).
     */
    private boolean atColumnZero() {
        return myOffset == 0 || myBuffer.charAt(myOffset - 1) == '\n';
    }

    private int scanNumber(int p) {
        while (p < myEnd && isDigit(myBuffer.charAt(p))) {
            p++;
        }
        if (p + 1 < myEnd && myBuffer.charAt(p) == '.' && isDigit(myBuffer.charAt(p + 1))) {
            p++;
            while (p < myEnd && isDigit(myBuffer.charAt(p))) {
                p++;
            }
        }
        if (p < myEnd && (myBuffer.charAt(p) == 'e' || myBuffer.charAt(p) == 'E')) {
            int q = p + 1;
            if (q < myEnd && (myBuffer.charAt(q) == '+' || myBuffer.charAt(q) == '-')) {
                q++;
            }
            if (q < myEnd && isDigit(myBuffer.charAt(q))) {
                while (q < myEnd && isDigit(myBuffer.charAt(q))) {
                    q++;
                }
                p = q;
            }
        }
        // engineering suffix: 170m, 9f, 90K, 75.00u
        if (p < myEnd && isLetter(myBuffer.charAt(p))) {
            p++;
        }
        return p;
    }

    private int encodeState() {
        int state = CTX_NONE;
        if (atLineStart) {
            state |= AT_LINE_START;
        }
        if (prevWasLink) {
            state |= PREV_WAS_LINK;
        }
        if (pendingCirc) {
            state |= PENDING_CIRC;
        }
        if (pendingBlk) {
            state |= PENDING_BLK;
        }
        if (pendingPel) {
            state |= PENDING_PEL;
        }
        if (pendingPy) {
            state |= PENDING_PY;
        }
        state |= (ctx & CTX_MASK) << CTX_SHIFT;
        state |= (ctxDepth & MASK) << CTX_DEPTH_SHIFT;
        state |= (depth & MASK) << DEPTH_SHIFT;
        return state;
    }

    private void decodeState(int state) {
        atLineStart = (state & AT_LINE_START) != 0;
        prevWasLink = (state & PREV_WAS_LINK) != 0;
        pendingCirc = (state & PENDING_CIRC) != 0;
        pendingBlk = (state & PENDING_BLK) != 0;
        pendingPel = (state & PENDING_PEL) != 0;
        pendingPy = (state & PENDING_PY) != 0;
        ctx = (state >> CTX_SHIFT) & CTX_MASK;
        ctxDepth = (state >> CTX_DEPTH_SHIFT) & MASK;
        depth = (state >> DEPTH_SHIFT) & MASK;
    }

    private static boolean isWhitespace(char c) {
        return c == ' ' || c == '\t' || c == '\r' || c == '\n' || c == '\f' || c == 0x0B;
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private static boolean isWordStart(char c) {
        return isLetter(c) || c == '_';
    }

    private static boolean isWordChar(char c) {
        return isLetter(c) || isDigit(c) || c == '_';
    }
}
