package org.altlaw.extract;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

public class PROBodyExtractor {
    private static final byte versionID = 1;

    private static final String[] START_TAGS = {
        "<div class=\"prelims\">",
        "<div class=\"num\"",
        "<p class=\"indent\">"
    };

    private static final String FOOTER = "<div id=\"footer\">";

    public static final String filter(String input) {
        if (input == null) { return null; }

        int start = findStart(input);
        int end = input.indexOf(FOOTER);

        if (start < 0 || end < 0) {
            return null;
        } else {
            return "<div class=\"prohtml\">\n" +
                input.substring(start, end).trim() +
                "\n</div>";
        }
    }

    private static final int findStart(String input) {
        for (String tag : START_TAGS) {
            int index = input.indexOf(tag);
            if (index > 0) {
                return index;
            }
        }
        return -1;
    }

    /** Main function for testing at command line. */
    public static final void main(final String[] args) {
        if (args.length != 1) {
            System.err.println("\nUsage: java PROBodyExtractor <file>\n" +
                               "Extracts body of XHTML file from public.resource.org.\n");
            System.exit(1);
        }

        try {
            String data = FileUtils.readFileToString(new File(args[0]),
                                                     "UTF-8");
            System.out.println(PROBodyExtractor.filter(data));
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
