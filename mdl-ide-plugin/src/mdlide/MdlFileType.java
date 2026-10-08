package mdlide;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileTypes.LanguageFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * File type for Keysight IC-CAP model definition (MDIF) decks.
 */
public final class MdlFileType extends LanguageFileType {
    public static final MdlFileType INSTANCE = new MdlFileType();

    private MdlFileType() {
        super(MdlLanguage.INSTANCE);
    }

    @Override
    public @NotNull String getName() {
        return "MDL";
    }

    @Override
    public @NotNull String getDescription() {
        return "IC-CAP model definition (.mdl)";
    }

    @Override
    public @NotNull String getDefaultExtension() {
        return "mdl";
    }

    @Override
    public @Nullable Icon getIcon() {
        return AllIcons.FileTypes.Text;
    }
}
