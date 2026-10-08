package mdlide.test;

import mdlide.MdlCompletionContributor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Command-line check of what the completion engine suggests at given
 * "line:col" positions (1-based) of an .mdl file. Not part of the plugin jar.
 */
public class CompletionDump {
    public static void main(String[] args) throws Exception {
        String path = args[0];
        String[] text = new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8).split("\n", -1);
        for (int i = 1; i < args.length; i++) {
            String[] parts = args[i].split(":");
            int line = Integer.parseInt(parts[0]) - 1;
            int col = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            int offset = 0;
            for (int l = 0; l < line && l < text.length; l++) {
                offset += text[l].length() + 1;
            }
            offset += col;
            int caret = Math.min(offset, offset);
            StringBuilder sb = new StringBuilder();
            for (String c : MdlCompletionContributor.candidates(String.join("\n", text), caret)) {
                sb.append(c).append(' ');
            }
            System.out.printf("line %d:%d (%s) -> %s%n",
                    line + 1, col, excerpt(text, line), sb.toString().trim());
        }
    }

    private static String excerpt(String[] text, int line) {
        if (line < 0 || line >= text.length) {
            return "?";
        }
        return text[line].trim();
    }
}
