package ai.nervemind.ui.util;

import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignB;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignD;
import org.kordamp.ikonli.materialdesign2.MaterialDesignE;
import org.kordamp.ikonli.materialdesign2.MaterialDesignF;
import org.kordamp.ikonli.materialdesign2.MaterialDesignG;
import org.kordamp.ikonli.materialdesign2.MaterialDesignH;
import org.kordamp.ikonli.materialdesign2.MaterialDesignI;
import org.kordamp.ikonli.materialdesign2.MaterialDesignJ;
import org.kordamp.ikonli.materialdesign2.MaterialDesignK;
import org.kordamp.ikonli.materialdesign2.MaterialDesignL;
import org.kordamp.ikonli.materialdesign2.MaterialDesignM;
import org.kordamp.ikonli.materialdesign2.MaterialDesignN;
import org.kordamp.ikonli.materialdesign2.MaterialDesignO;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;
import org.kordamp.ikonli.materialdesign2.MaterialDesignQ;
import org.kordamp.ikonli.materialdesign2.MaterialDesignR;
import org.kordamp.ikonli.materialdesign2.MaterialDesignS;
import org.kordamp.ikonli.materialdesign2.MaterialDesignT;
import org.kordamp.ikonli.materialdesign2.MaterialDesignU;
import org.kordamp.ikonli.materialdesign2.MaterialDesignV;
import org.kordamp.ikonli.materialdesign2.MaterialDesignW;
import org.kordamp.ikonli.materialdesign2.MaterialDesignX;
import org.kordamp.ikonli.materialdesign2.MaterialDesignY;
import org.kordamp.ikonli.materialdesign2.MaterialDesignZ;

/**
 * Utility class for resolving icon names to Ikonli Ikon instances.
 * 
 * <p>
 * This class provides a centralized way to convert string icon names
 * (e.g., "FILE_EYE", "PUZZLE") to Material Design icon instances.
 * </p>
 */
public final class IconResolver {

    private static final Ikon DEFAULT_ICON = MaterialDesignP.PUZZLE;

    private IconResolver() {
        // Utility class - no instantiation
    }

    /**
     * Resolves an icon name to an Ikon instance.
     * 
     * <p>
     * The icon name should be in UPPER_SNAKE_CASE format matching
     * Material Design icon names (e.g., "FILE_EYE", "PLAY_CIRCLE").
     * </p>
     * 
     * @param iconName the icon name to resolve
     * @return the resolved Ikon, or a default puzzle icon if not found
     */
    public static Ikon resolve(String iconName) {
        if (iconName == null || iconName.isBlank()) {
            return DEFAULT_ICON;
        }

        String normalized = iconName.toUpperCase().replace("-", "_").replace(" ", "_");

        if (normalized.isEmpty()) {
            return DEFAULT_ICON;
        }

        char first = normalized.charAt(0);
        return switch (first) {
            case 'A' -> tryEnum(MaterialDesignA.class, normalized);
            case 'B' -> tryEnum(MaterialDesignB.class, normalized);
            case 'C' -> tryEnum(MaterialDesignC.class, normalized);
            case 'D' -> tryEnum(MaterialDesignD.class, normalized);
            case 'E' -> tryEnum(MaterialDesignE.class, normalized);
            case 'F' -> tryEnum(MaterialDesignF.class, normalized);
            case 'G' -> tryEnum(MaterialDesignG.class, normalized);
            case 'H' -> tryEnum(MaterialDesignH.class, normalized);
            case 'I' -> tryEnum(MaterialDesignI.class, normalized);
            case 'J' -> tryEnum(MaterialDesignJ.class, normalized);
            case 'K' -> tryEnum(MaterialDesignK.class, normalized);
            case 'L' -> tryEnum(MaterialDesignL.class, normalized);
            case 'M' -> tryEnum(MaterialDesignM.class, normalized);
            case 'N' -> tryEnum(MaterialDesignN.class, normalized);
            case 'O' -> tryEnum(MaterialDesignO.class, normalized);
            case 'P' -> tryEnum(MaterialDesignP.class, normalized);
            case 'Q' -> tryEnum(MaterialDesignQ.class, normalized);
            case 'R' -> tryEnum(MaterialDesignR.class, normalized);
            case 'S' -> tryEnum(MaterialDesignS.class, normalized);
            case 'T' -> tryEnum(MaterialDesignT.class, normalized);
            case 'U' -> tryEnum(MaterialDesignU.class, normalized);
            case 'V' -> tryEnum(MaterialDesignV.class, normalized);
            case 'W' -> tryEnum(MaterialDesignW.class, normalized);
            case 'X' -> tryEnum(MaterialDesignX.class, normalized);
            case 'Y' -> tryEnum(MaterialDesignY.class, normalized);
            case 'Z' -> tryEnum(MaterialDesignZ.class, normalized);
            default -> DEFAULT_ICON;
        };
    }

    /**
     * Tries to resolve an enum constant by name.
     */
    private static <E extends Enum<E> & Ikon> Ikon tryEnum(Class<E> enumClass, String name) {
        try {
            return Enum.valueOf(enumClass, name);
        } catch (IllegalArgumentException _) {
            return DEFAULT_ICON;
        }
    }

    /**
     * Gets the default icon used when resolution fails.
     * 
     * @return the default icon
     */
    public static Ikon getDefaultIcon() {
        return DEFAULT_ICON;
    }
}
