package mdlide.test;

import com.intellij.lexer.Lexer;
import mdlide.MdlLexer;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Sanity checker: tokenizes an .mdl file with the plugin lexer and prints the
 * token stream plus brace balance. Not part of the plugin jar.
 */
public class LexerDump {
    public static void main(String[] args) throws Exception {
        String path = args[0];
        byte[] raw = Files.readAllBytes(Paths.get(path));
        String text = new String(raw, StandardCharsets.UTF_8);
        Lexer lexer = new MdlLexer();
        lexer.start(text, 0, text.length(), 0);
        int n = 0;
        int braces = 0;
        int netlist = 0;
        int keywords = 0;
        int varref = 0;
        int preproc = 0;
        int components = 0;
        int funcs = 0;
        int comments = 0;
        int pel = 0;
        int limit = args.length > 1 ? Integer.parseInt(args[1]) : 60;
        while (lexer.getTokenType() != null) {
            String t = lexer.getTokenText().replace("\n", "\\n").replace("\r", "");
            if ("{".equals(t)) {
                braces++;
            } else if ("}".equals(t)) {
                braces--;
            }
            if (lexer.getTokenType().toString().contains("NETLIST")) {
                netlist++;
            }
            if (lexer.getTokenType().toString().contains("COMPONENT")) {
                components++;
            }
            if (lexer.getTokenType().toString().contains("FUNC")) {
                funcs++;
            }
            if (lexer.getTokenType().toString().contains("COMMENT") && !lexer.getTokenType().toString().contains("PREPROCESSOR")) {
                comments++;
            }
            if (lexer.getTokenType().toString().contains("PEL")) {
                pel++;
            }
            if (lexer.getTokenType().toString().contains("KEYWORD")) {
                keywords++;
            }
            if (lexer.getTokenType().toString().contains("VARREF")) {
                varref++;
            }
            if (lexer.getTokenType().toString().contains("PREPROCESSOR")) {
                preproc++;
            }
            if (n < limit) {
                System.out.printf("%-22s %s%n", lexer.getTokenType().toString(), t);
            }
            n++;
            lexer.advance();
        }
        System.out.println("tokens=" + n + " braceBalance=" + braces
                + " netlist=" + netlist + " components=" + components
                + " funcs=" + funcs + " comments=" + comments
                + " pel=" + pel + " keywords=" + keywords
                + " varref=" + varref + " preproc=" + preproc);
    }
}
