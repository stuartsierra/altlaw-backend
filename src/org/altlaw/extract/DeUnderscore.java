package org.altlaw.extract;

import java.util.regex.Pattern;

/** Text filter class to strip underscores from input.  Trims
 * whitespace from beginning and end of string after removing
 * underscores. */
public class DeUnderscore {
    private static final Pattern UNDERSCORES = Pattern.compile("_+");
    
    public static final String filter(String input) {
        return UNDERSCORES.matcher(input).replaceAll("").trim();
    }
}