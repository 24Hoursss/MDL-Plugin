package mdlide;

import com.intellij.lang.Language;
import org.jetbrains.annotations.NotNull;

public final class MdlLanguage extends Language {
    public static final MdlLanguage INSTANCE = new MdlLanguage();

    private MdlLanguage() {
        super("MDL");
    }

    @Override
    public @NotNull String getDisplayName() {
        return "IC-CAP MDL";
    }
}
