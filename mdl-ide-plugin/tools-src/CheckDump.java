package mdlide.test;

import mdlide.MdlCheck;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Command-line deck checker: prints MdlCheck issues as "line:col severity
 * message". Not part of the plugin jar.
 */
public class CheckDump {
    public static void main(String[] args) throws Exception {
        String path = args[0];
        String text = new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
        List<MdlCheck.Issue> issues = MdlCheck.check(text);
        int lines = 1;
        StringBuilder sb = new StringBuilder(text);
        for (int i = 0; i < issues.size(); i++) {
            MdlCheck.Issue issue = issues.get(i);
            int line = 1, col = 1;
            for (int j = 0; j < issue.start && j < text.length(); j++) {
                if (text.charAt(j) == '\n') {
                    line++;
                    col = 1;
                } else {
                    col++;
                }
            }
            System.out.printf("%d:%d %-7s %s%n", line, col,
                    issue.error ? "ERROR" : "WARN", issue.message);
        }
        System.out.println("issues=" + issues.size()
                + " errors=" + issues.stream().filter(i -> i.error).count());
    }
}
