package mdlide;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deck-level sanity checks, mirroring the iccap-mdl-edit agent's verify rules:
 * <ul>
 *   <li>structural column-0 braces must balance (unbalanced '{'/'}' is an
 *       error, exactly like mdl_edit.py's parse)</li>
 *   <li>parentheses / brackets / inline braces must pair (strings excluded)</li>
 *   <li>strings must close on the same line (Python docstrings excluded)</li>
 *   <li>{@code param} and its {@code fullprecision} row must both exist</li>
 *   <li>an Optimize row with {@code Use=Yes} needs numeric Min/Max/Stored with
 *       Min &lt;= Stored &lt;= Max, and a name that does not start with '!'</li>
 * </ul>
 * Pure text logic (no IDEA services): unit-testable off the IDE.
 */
public final class MdlCheck {

    public static final class Issue {
        public final boolean error;
        public final int start;
        public final int len;
        public final String message;

        Issue(boolean error, int start, int len, String message) {
            this.error = error;
            this.start = start;
            this.len = len;
            this.message = message;
        }
    }

    private static final int MAX_ISSUES = 200;
    private static final Pattern ELEMENT_ROW =
            Pattern.compile("element\\s+(\\d+)\\s+\"([^\"]+)\"\\s+\"([^\"]*)\"");

    private MdlCheck() {
    }

    public static List<Issue> check(@NotNull String text) {
        List<Issue> issues = new ArrayList<>();
        checkTokens(text, issues);
        checkTables(text, issues);
        return issues;
    }

    /** Structural col-0 braces, inline pair balance, string closure. */
    private static void checkTokens(String text, List<Issue> issues) {
        Deque<int[]> structure = new ArrayDeque<>(); // {offset, len} of a col-0 '{'
        Deque<int[]> inline = new ArrayDeque<>();    // {offset, closeChar} of '(', '[', '{'
        MdlLexer lexer = new MdlLexer();
        lexer.start(text, 0, text.length(), 0);
        while (lexer.getTokenType() != null) {
            IElementType type = lexer.getTokenType();
            int start = lexer.getTokenStart();
            int end = lexer.getTokenEnd();
            if (type == MdlTokenTypes.LBRACE || type == MdlTokenTypes.RBRACE) {
                boolean columnZero = start == 0 || text.charAt(start - 1) == '\n';
                if (type == MdlTokenTypes.LBRACE) {
                    if (columnZero) {
                        structure.push(new int[]{start, end - start});
                    } else {
                        inline.push(new int[]{start, '}'});
                    }
                } else if (columnZero) {
                    if (structure.isEmpty()) {
                        add(issues, true, start, end - start,
                                "Unmatched '}' (no column-0 '{' to close)");
                    } else {
                        structure.pop();
                    }
                } else {
                    matchOrReport(inline, '}', start, end - start, issues);
                }
            } else if (type == MdlTokenTypes.LPAREN) {
                inline.push(new int[]{start, ')'});
            } else if (type == MdlTokenTypes.RPAREN) {
                matchOrReport(inline, ')', start, end - start, issues);
            } else if (type == MdlTokenTypes.BRACKET) {
                if (text.charAt(start) == '[') {
                    inline.push(new int[]{start, ']'});
                } else {
                    matchOrReport(inline, ']', start, end - start, issues);
                }
            } else if (type == MdlTokenTypes.STRING) {
                if (!MdlLexer.stateInPython(lexer.getState())) {
                    String token = text.substring(start, end);
                    if (token.length() < 2 || token.charAt(token.length() - 1) != '"') {
                        add(issues, true, start, 1,
                                "Unterminated string (missing closing \")");
                    }
                }
            }
            lexer.advance();
        }
        while (!structure.isEmpty()) {
            int[] open = structure.pop();
            add(issues, true, open[0], open[1], "Unclosed '{' (block never closed)");
        }
        while (!inline.isEmpty()) {
            int[] open = inline.pop();
            add(issues, true, open[0], 1,
                    "Unclosed '" + text.charAt(open[0]) + "' (missing '" + (char) open[1] + "')");
        }
    }

    private static void matchOrReport(Deque<int[]> stack, char closeChar,
                                      int offset, int len, List<Issue> issues) {
        if (stack.isEmpty()) {
            add(issues, true, offset, len, "Unmatched '" + closeChar + "'");
            return;
        }
        int[] open = stack.peek();
        if ((char) open[1] != closeChar) {
            add(issues, true, offset, len,
                    "Unmatched '" + closeChar + "' (expected '" + (char) open[1] + "')");
        } else {
            stack.pop();
        }
    }

    /** param/fullprecision pairing + Optimize Use=Yes bounds. */
    private static void checkTables(String text, List<Issue> issues) {
        List<int[]> ranges = lineRanges(text);
        Map<String, Integer> params = new HashMap<>();   // name -> line index
        Map<String, Integer> fullprec = new HashMap<>();
        Deque<int[]> stack = new ArrayDeque<>();         // {openLine, headerLine}
        int lastHeader = -1;
        for (int li = 0; li < ranges.size(); li++) {
            String line = text.substring(ranges.get(li)[0], ranges.get(li)[1]);
            String trimmed = line.trim();
            if (trimmed.equals("{") && line.startsWith("{")) {
                stack.push(new int[]{li, lastHeader});
                continue;
            }
            if (trimmed.equals("}") && line.startsWith("}")) {
                int[] open = stack.poll();
                if (open != null && open[1] >= 0) {
                    String header = text.substring(ranges.get(open[1])[0],
                            ranges.get(open[1])[1]).trim();
                    if (header.equals("OPTIMEDIT")) {
                        checkOptimize(text, ranges, open[0] + 1, li, issues);
                    }
                }
                continue;
            }
            lastHeader = li;
            String name = quotedName(line, "param");
            if (name != null) {
                params.put(name, li);
                continue;
            }
            name = quotedName(line, "fullprecision");
            if (name != null) {
                fullprec.put(name, li);
            }
        }
        for (Map.Entry<String, Integer> e : params.entrySet()) {
            if (!fullprec.containsKey(e.getKey())) {
                add(issues, false, ranges.get(e.getValue())[0], 1,
                        "param " + e.getKey() + " has no matching fullprecision row");
            }
        }
        for (Map.Entry<String, Integer> e : fullprec.entrySet()) {
            if (!params.containsKey(e.getKey())) {
                add(issues, false, ranges.get(e.getValue())[0], 1,
                        "fullprecision " + e.getKey() + " has no matching param row");
            }
        }
    }

    private static List<int[]> lineRanges(String text) {
        List<int[]> ranges = new ArrayList<>();
        int start = 0;
        while (start <= text.length()) {
            int end = text.indexOf('\n', start);
            if (end < 0) {
                end = text.length();
            }
            ranges.add(new int[]{start, end});
            if (end >= text.length()) {
                break;
            }
            start = end + 1;
        }
        return ranges;
    }

    /** "param NAME \"...\"" / "fullprecision NAME \"...\"" -> the NAME. */
    @Nullable
    private static String quotedName(String line, String kind) {
        if (!line.startsWith(kind + " ")) {
            return null;
        }
        String rest = line.substring(kind.length() + 1).trim();
        int q = rest.indexOf('"');
        if (q <= 0) {
            return null;
        }
        return rest.substring(0, q).trim();
    }

    private static void checkOptimize(String text, List<int[]> ranges,
                                      int fromLine, int toLineExclusive, List<Issue> issues) {
        Map<Integer, Map<String, String>> rows = new HashMap<>();
        Map<Integer, Integer> firstLine = new HashMap<>();
        for (int li = fromLine; li < toLineExclusive; li++) {
            String line = text.substring(ranges.get(li)[0], ranges.get(li)[1]);
            Matcher m = ELEMENT_ROW.matcher(line.trim());
            if (m.matches()) {
                int idx = Integer.parseInt(m.group(1));
                rows.computeIfAbsent(idx, k -> new HashMap<>()).put(m.group(2), m.group(3));
                firstLine.putIfAbsent(idx, li);
            }
        }
        for (Map.Entry<Integer, Map<String, String>> e : rows.entrySet()) {
            Map<String, String> row = e.getValue();
            String use = row.getOrDefault("Use", "No").trim();
            if (!("Yes".equalsIgnoreCase(use) || use.equalsIgnoreCase("true"))) {
                continue;
            }
            int li = firstLine.get(e.getKey());
            int offset = ranges.get(li)[0];
            String name = row.getOrDefault("Name", "").trim();
            if (name.isEmpty()) {
                continue; // sentinel row
            }
            if (name.startsWith("!")) {
                add(issues, true, offset, 1,
                        "Use=Yes: parameter name '" + name + "' is unresolved (! prefix)");
                continue;
            }
            Double min = parseValue(row.getOrDefault("Min", ""));
            Double max = parseValue(row.getOrDefault("Max", ""));
            Double stored = parseValue(row.getOrDefault("Stored", ""));
            if (min == null || max == null || stored == null) {
                add(issues, true, offset, 1,
                        "Use=Yes: row '" + name + "' needs numeric Min, Max and Stored");
            } else if (stored < min || stored > max) {
                add(issues, true, offset, 1,
                        "Use=Yes: Stored " + row.get("Stored").trim()
                                + " outside Min " + row.get("Min").trim()
                                + " .. Max " + row.get("Max").trim());
            }
        }
    }

    /** Engineering value with suffix: 170m, 7n, 2.375u, 1MEG, 5e-05. */
    @Nullable
    private static Double parseValue(String raw) {
        String s = raw.trim();
        if (s.isEmpty()) {
            return null;
        }
        int i = s.length() - 1;
        while (i >= 0 && isLetter(s.charAt(i))) {
            i--;
        }
        String num = s.substring(0, i + 1).trim();
        String unit = s.substring(i + 1);
        if (num.isEmpty()) {
            return null;
        }
        double base;
        try {
            base = Double.parseDouble(num);
        } catch (NumberFormatException ex) {
            return null;
        }
        Double mult = unitMult(unit);
        return mult == null ? null : base * mult;
    }

    private static Double unitMult(String unit) {
        switch (unit) {
            case "": return 1.0;
            case "a": return 1e-18;
            case "f": return 1e-15;
            case "p": return 1e-12;
            case "n": return 1e-9;
            case "u": return 1e-6;
            case "m": return 1e-3;
            case "k":
            case "K": return 1e3;
            case "M":
            case "MEG": return 1e6;
            case "g":
            case "G": return 1e9;
            case "t":
            case "T": return 1e12;
            default: return null;
        }
    }

    private static boolean isLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private static void add(List<Issue> issues, boolean error, int start, int len, String message) {
        if (issues.size() < MAX_ISSUES) {
            issues.add(new Issue(error, start, len, message));
        }
    }
}
