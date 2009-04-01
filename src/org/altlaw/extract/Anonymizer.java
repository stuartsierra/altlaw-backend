package org.altlaw.extract;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Replaces personal identification numbers with XXX. */
public class Anonymizer {

    private static final byte versionID = 1;
    private static final Pattern ssn =
        Pattern.compile("\\d{3}.\\d{2}.\\d{4}");
    private static final Pattern ein =
        Pattern.compile("\\d{2}.\\d{7}");
    private static final Pattern alienid =
        Pattern.compile("\\d{2}.\\d{3}.\\d{3}");

    /** Replaces SSN's, EIN's, and Alien ID's wxth XXX's.
     * Null-safe. */
    public static String filter(String input) {
        if (input == null) {
            return null;
        }
        input = ssn.matcher(input).replaceAll("XXX-XX-XXXX");
        input = ein.matcher(input).replaceAll("XX-XXXXXXX");
        input = alienid.matcher(input).replaceAll("XX-XXX-XXX");
        return input;
    }
}
