package org.altlaw.extract;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory; 
import org.altlaw.util.DateUtils;
import org.altlaw.util.DummyEntityResolver;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

/** Parser/loader for XHTML case files from public.resource.org.
 *
 * @see http://bulk.resource.org/courts.gov/c/
 */
public class PROMetadataReader {

    private static final byte versionID = 3;

    public String getFilename() {
        return filename;
    }

    /** Sets the file name of the document being parsed, to be
     * included in log messages. */
    public void setFilename(String filename) {
        this.filename = filename;
    }

    /** Returns the title of this case, or null if not found. */
    public String getTitle() {
        return title;
    }

    public String getCourtId() {
        return courtId;
    }

    /** Returns the date this case was decided, as an SQL-style date
     * string (YYYY-MM-DD), or null if not found. */
    public String getDate() {
        return date;
    }

    /** Returns the F.2d, F.3d, or U.S. citation, or null if not
     * found. */
    public String getMainCitation() {
        return mainCitation;
    }

    /** Returns all citations. */
    public List<String> getCitations() {
        return citations;
    }

    /** Returns all docket numbers. The text "No." or "Docket" is
     * stripped off. */
    public List<String> getDockets() {
        return dockets;
    }

    /** Parses the XHTML stream and sets fields in this object.
     * Closes the input stream.  Call {@link setFileName} first to
     * include file name in log messages. */
    public void parse(Reader stream)
        throws IOException {

        try {
            XMLReader reader = XMLReaderFactory.createXMLReader();
            reader.setContentHandler(new MyHandler());
            reader.setEntityResolver(new DummyEntityResolver());
            InputSource source = new InputSource(stream);
            reader.parse(source);
        } catch (SAXException e) {
            if (e != FINISHED) {
                logWarn("SAXException: <" + e.toString() + ">");
                return;
            }
        } finally {
            stream.close();
        }
        date = DateUtils.sqlDateString(findBestDate());
        mainCitation = findMainCitation();
        checkSupremeCourt();
    }

    /** Clears / re-initializes all fields.  Must call before
     * re-using. */
    public void clear() {
        title = date = metadate = filename = court = prelims = mainCitation = null;
        courtId = null;
        citations = new ArrayList<String>();
        dockets = new ArrayList<String>();
        eventStrings = new ArrayList<String>();
    }

    /** Returns the case metadata as a Map.  Keys in the map are
     * filename, mainCitation, citations, date, court, title,
     * dockets, and doctype.  Values have type String or
     * List<String>. */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("mainCitation", mainCitation);
        map.put("citations", citations);
        map.put("mainDate", date);
        map.put("court", court);
        map.put("mainTitle", title);
        map.put("dockets", dockets);
        map.put("doctype", "case");
        return map;
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("\nUsage: java org.altlaw.PROMetadataReader <html file>\n" +
                               "Prints a JSON representation of the file metadata.\n" +
                               "If file is - read from standard input.");
            System.exit(1);
        }

        try {
            PROMetadataReader me = new PROMetadataReader();
            Reader reader;
            if (args[0].equals("-")) {
                me.setFilename(args[0]);
                reader = new BufferedReader(new InputStreamReader(System.in));
            } else {
                reader = new FileReader(args[0]);
            }
            me.parse(reader);
            // System.out.println(ThriftUtils.toSimpleJSON(me.toCaseData(), 4));
            System.exit(0);
        } catch (Exception e) {
            System.err.println(e);
            System.exit(1);
        }
    }
    
    /** Thrown when we've parsed enough of the document. */
    private static final SAXException FINISHED =
        new SAXException("Finished parsing.");
    
    private static final Pattern MAIN_CITE_REGEX =
        Pattern.compile("F\\.2d|F\\.3d|U\\.S\\.");
    private static final Pattern DATE_REGEX =
        Pattern.compile("(?i)(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z;:.,]* *\\d+[;:., ]*\\d+|\\d{1,2}[-/]\\d{1,2}[-/]\\d{2,4}");
    private static final Pattern TERM_REGEX =
        Pattern.compile("(?i)(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z;:.,]*[^0-9]*(\\d{4})");
    private static final Pattern DECIDED_REGEX =
        Pattern.compile("(?i)Decided:?\\s+(.+)");
    private static final Pattern DATE_STRIP_REGEX =
        Pattern.compile("[.;:]");
    private static final Pattern COMMA_SPLIT =
        Pattern.compile(", *|\\n+");
    private static final Pattern DOCKET_STRIP_REGEX =
        Pattern.compile("Docket|Nos?|\\.", Pattern.CASE_INSENSITIVE);
    private static final Pattern MINIMAL_DOCKET_REGEX =
        Pattern.compile(".*\\d+.*", Pattern.CASE_INSENSITIVE);
    private static final Pattern MINIMAL_CITE_REGEX =
        Pattern.compile("^\\d+ .*[a-z].* \\d+$", Pattern.CASE_INSENSITIVE);
    private static final Pattern COURT_REGEXES[] = {
        Pattern.compile("(?is)Supreme"), Pattern.compile("(?is)First"),
        Pattern.compile("(?is)Second"), Pattern.compile("(?is)Third"),
        Pattern.compile("(?is)Fourth"), Pattern.compile("(?is)Fifth"),
        Pattern.compile("(?is)Sixth"), Pattern.compile("(?is)Seventh"),
        Pattern.compile("(?is)Eighth"), Pattern.compile("(?is)Ninth"),
        Pattern.compile("(?is)Tenth"), Pattern.compile("(?is)Eleventh"),
        Pattern.compile("(?is)Armed Forces"), Pattern.compile("(?is)Federal Circuit"),
        Pattern.compile("(?is)District of Columbia"), Pattern.compile("(?is)Temporary Emergency Court of Appeals"),
        Pattern.compile("(?is)Emergency Court of Appeals"), Pattern.compile("(?is)Customs and Patent Appeals"),
        Pattern.compile("(?is)Court of Claims"), Pattern.compile("(?is)Federal Claims"),
        Pattern.compile("(?is)United States Claims Court")
    };

    /** Correct names corresponding to COURT_REGEXES. */
    private static final String[] COURT_IDS = {
        "http://id.altlaw.org/courts/us/fed/supreme",
        "http://id.altlaw.org/courts/us/fed/app/1",
        "http://id.altlaw.org/courts/us/fed/app/2",
        "http://id.altlaw.org/courts/us/fed/app/3",
        "http://id.altlaw.org/courts/us/fed/app/4",
        "http://id.altlaw.org/courts/us/fed/app/5",
        "http://id.altlaw.org/courts/us/fed/app/6",
        "http://id.altlaw.org/courts/us/fed/app/7",
        "http://id.altlaw.org/courts/us/fed/app/8",
        "http://id.altlaw.org/courts/us/fed/app/9",
        "http://id.altlaw.org/courts/us/fed/app/10",
        "http://id.altlaw.org/courts/us/fed/app/11",
        "http://id.altlaw.org/courts/us/fed/app/af",
        "http://id.altlaw.org/courts/us/fed/app/fed",
        "http://id.altlaw.org/courts/us/fed/app/dc",
        "http://id.altlaw.org/courts/us/fed/teca",
        "http://id.altlaw.org/courts/us/fed/app/eca",
        "http://id.altlaw.org/courts/us/fed/app/customs-patents",
        "http://id.altlaw.org/courts/us/fed/app/court-of-claims",
        "http://id.altlaw.org/courts/us/fed/app/claims",
        "http://id.altlaw.org/courts/us/fed/app/claims"
    };

    private String title, date, court, prelims, mainCitation,
        filename, metadate;
    private String courtId;
    private ArrayList<String> citations = new ArrayList<String>();
    private ArrayList<String> dockets = new ArrayList<String>();
    private ArrayList<String> eventStrings = new ArrayList<String>();
    private static Log log = LogFactory.getLog(PROMetadataReader.class);

    /** SAX2 XML receiver. */
    private class MyHandler extends DefaultHandler
        implements ContentHandler {

        private String state;
        private String currentElement;
        private StringBuilder buffer;

        public void characters(char[] ch, int start, int length) {
            if (state != null) {
                buffer.append(ch, start, length);
            }
        }

        public void startElement(String uri, String localName,
                                 String qName, Attributes atts) {
            String c = atts.getValue("class");
            if (c != null &&
                (c.equals("case_cite") || c.equals("parties") ||
                 c.equals("date") || c.equals("docket") ||
                 c.equals("court") || c.equals("prelims"))) {

                state = c;
                currentElement = localName;
                buffer = new StringBuilder();
            } else if (qName.equals("meta") &&
                       (c = atts.getValue("name")) != null &&
                       c.equals("date")) {
                metadate = atts.getValue("content");
            } else if (state != null) {
                if (localName.equals("br")) {
                    buffer.append('\n');
                } else {
                    buffer.append(' ');
                }
            }
        }

        public void endElement(String uri, String localName,
                               String qName)
            throws SAXException {
            
            if (currentElement != null &&
                localName.equals(currentElement)) {

                String content = DeUnderscore.filter(Anonymizer.filter(buffer.toString()));

                if (state.equals("case_cite")) {
                    citations.addAll(parseCites(content));
                } else if (state.equals("parties")) {
                    title = content;
                } else if (state.equals("date")) {
                    eventStrings.add(content);
                } else if (state.equals("docket")) {
                    dockets.addAll(parseDockets(content));
                } else if (state.equals("court")) {
                    courtId = parseCourt(content);
                } else if (state.equals("prelims")) {
                    prelims = content;
                    throw FINISHED;
                }

                currentElement = null;
                state = null;
            }
        }

        private List<String> parseCites(String in) {
            String[] splits = COMMA_SPLIT.split(in);
            List<String> output = new ArrayList<String>(splits.length);
            for (int i = 0; i < splits.length; i++) {
                if (MINIMAL_CITE_REGEX.matcher(splits[i]).matches()) {
                    output.add(splits[i]);
                }
            }
            return output;
        }

        private String parseCourt(String in) {
            /* Try to recognize court tag. */
            for (int i = 0; i < COURT_REGEXES.length; i++) {
                if (COURT_REGEXES[i].matcher(in).find()) {
                    return COURT_IDS[i];
                }
            }
            logWarn("Could not parse court from <" + in + ">");
            return null;
        }

        private List<String> parseDockets(String in) {
            String[] splits = COMMA_SPLIT.split(in);
            List<String> output = new ArrayList<String>(splits.length);
            for (int i = 0; i < splits.length; i++) {
                String docket = DOCKET_STRIP_REGEX.matcher(splits[i]).replaceAll("").trim();
                if (MINIMAL_DOCKET_REGEX.matcher(docket).matches()) {
                    output.add(docket);
                }
            }
            return output;
        }
    }

    private Calendar findBestDate() {
        /* First see if there was a META tag with the date. */
        if (metadate != null) {
            return DateUtils.parsePastDate(metadate.substring(0,10));
        }

        logWarn("No date META tag; going to headers.");
        String dateString = findDecidedDate();
        if (dateString != null) {
            return DateUtils.parsePastDate(dateString);
        } else {
            for (String s : eventStrings) {
                Matcher m = DATE_REGEX.matcher(s);
                if (m.find()) {
                    return DateUtils.parsePastDate(m.group());
                }
            }
        }
        
        /* For old Supreme Court cases, try searching for a vague date
         * like "January Term, 1854". */
        for (String s : eventStrings) {
            Matcher m = TERM_REGEX.matcher(s);
            if (m.find()) {
                return DateUtils.parsePastDate(m.group(1) + " 1, " + m.group(2));
            }
        }

        if (prelims != null) {
            logWarn("No date in headers; going to prelims.");

            /* Try to find "Decided ..." date in prelims. */
            Matcher m1 = DECIDED_REGEX.matcher(prelims);
            if (m1.find()) {
                Matcher m2 = DATE_REGEX.matcher(m1.group(1));
                if (m2.find()) {
                    return DateUtils.parsePastDate(m2.group());
                }
            }

            /* If all else fails, take first valid date in prelims. */
            m1 = DATE_REGEX.matcher(prelims);
            if (m1.find()) {
                return DateUtils.parsePastDate(m1.group());
            }
        }

        logWarn("Could not find date.");
        return null;
    }

    private String findDecidedDate() {
        for (String s : eventStrings) {
            Matcher m1 = DECIDED_REGEX.matcher(s);
            if (m1.find()) {
                Matcher m2 = DATE_REGEX.matcher(m1.group(1));
                if (m2.find()) {
                    return m2.group();
                }
            }
        }
        return null;
    }

    private String findMainCitation() {
        for (String s : citations) {
            if (MAIN_CITE_REGEX.matcher(s).find()) {
                return s;
            }
        }
        return null;
    }

    private void checkSupremeCourt() {
        /* Check for U.S. reporter citation. */
        if (courtId == null && mainCitation != null &&
            mainCitation.contains(" U.S. ")) {
            courtId = "http://id.altlaw.org/courts/us/fed/supreme";
        }
    }

    /** Logs a WARNING with message and the filename. */ 
    private void logWarn(String message) {
        if (filename != null) {
            message = filename + ": " + message;
        }
        log.warn(message);
    }
}
