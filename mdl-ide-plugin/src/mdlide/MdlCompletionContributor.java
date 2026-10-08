package mdlide;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiFile;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Typing completion for MDL decks. The context rules mirror the VSCode
 * extension: line-start keywords, LINK kinds after "LINK ", directives after
 * '#', parameter references after '$', netlist keywords inside circuitdeck.
 */
public final class MdlCompletionContributor extends CompletionContributor {
    private static final List<String> KEYWORDS = Arrays.asList(
            "LINK", "list", "element", "param", "fullprecision", "View",
            "dataset", "datasize", "type", "member",
            "BLKEDIT", "TABLE", "HYPTABLE", "data", "circuitdeck", "OPTIMEDIT",
            "__iccapDataBlockEnd");
    private static final List<String> LINK_KINDS = Arrays.asList(
            "MODEL", "DUT", "DAT", "SWEEP", "OUT", "XFORM", "PLOT", "MACRO",
            "CIRC", "TCIRC", "PS", "DPS");
    private static final List<String> DIRECTIVES = Arrays.asList("#load", "#echo", "#include", "#define");
    private static final List<String> VARREFS = Arrays.asList("$dpar", "$mpar", "$xpar", "$var");
    private static final List<String> NETLIST = Arrays.asList(
            "define", "model", "end", "subckt", "ends", "parameters", "include");

    private static final Pattern LINK_HEAD = Pattern.compile("\\s*LINK\\s+[A-Za-z0-9_]*");
    private static final Pattern DIRECTIVE_PREFIX = Pattern.compile(".*#[A-Za-z]*");
    private static final Pattern VARREF_PREFIX = Pattern.compile(".*\\$[A-Za-z]*");
    private static final Pattern LINE_START = Pattern.compile("\\s*[A-Za-z0-9_]*");

    public MdlCompletionContributor() {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), new CompletionProvider<>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters parameters,
                                          @NotNull ProcessingContext context,
                                          @NotNull CompletionResultSet result) {
                PsiFile file = parameters.getPosition().getContainingFile();
                String text = file.getText();
                if (text != null) {
                    for (String candidate : candidates(text, parameters.getOffset())) {
                        result.addElement(LookupElementBuilder.create(candidate)
                                .withTypeText("MDL keyword"));
                    }
                }
            }
        });
    }

    /** Pure computation, unit-testable: which keywords fit at the caret offset. */
    public static List<String> candidates(String text, int caret) {
        if (caret > text.length()) {
            caret = text.length();
        }
        int lineStart = text.lastIndexOf('\n', Math.max(0, caret - 1)) + 1;
        String before = text.substring(lineStart, caret);
        Set<String> out = new LinkedHashSet<>();
        if (LINK_HEAD.matcher(before).matches()) {
            out.addAll(LINK_KINDS);
        } else {
            if (DIRECTIVE_PREFIX.matcher(before).matches()) {
                out.addAll(DIRECTIVES);
            } else if (VARREF_PREFIX.matcher(before).matches()) {
                out.addAll(VARREFS);
            }
            if (LINE_START.matcher(before).matches()) {
                boolean inCirc = isInCircuitdeck(text, caret);
                if (inCirc) {
                    out.addAll(NETLIST);
                }
                out.addAll(KEYWORDS);
            }
        }
        return new ArrayList<>(out);
    }

    /** Is the caret inside a circuitdeck body (column-0 brace rule)? */
    private static boolean isInCircuitdeck(String text, int caret) {
        MdlLexer lexer = new MdlLexer();
        lexer.start(text, 0, text.length(), 0);
        while (lexer.getTokenType() != null && lexer.getTokenEnd() <= caret) {
            lexer.advance();
        }
        if (lexer.getTokenType() == null) {
            return false;
        }
        return MdlLexer.stateInCirc(lexer.getState());
    }
}
