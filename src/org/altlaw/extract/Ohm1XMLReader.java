package org.altlaw.extract;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.altlaw.util.DateUtils;
import org.altlaw.util.DummyEntityResolver;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

public class Ohm1XMLReader {

    private Map<String, String> data;

    private static final Pattern R1 = Pattern.compile("\\w{3}");
    private static final Pattern R2 = Pattern.compile("\\w+");
    private static final Pattern R3 = Pattern.compile("^[-/0-9]+ (.+)");
    private static final Pattern R4 = Pattern.compile("^.*UNITED STATES COURT OF APPEALS FOR THE THIRD CIRCUIT[ _]*");
    private static final Pattern R5 = Pattern.compile("[a-zA-Z.:]*[-0-9\\/. _]*");
    private static final Pattern R6 = Pattern.compile("vs?\\.");
    private static final Pattern R7 = Pattern.compile("^.*vs?\\. ");
    private static final Pattern R8 = Pattern.compile("\\s+");
    private static final Pattern R9 = Pattern.compile("[^A-Z]*$");

    private static final Map<String, String> COURT_DIRS = new HashMap<String, String>(14);
    static {
        COURT_DIRS.put("/cadc/", "http://id.altlaw.org/courts/us/fed/app/dc");
        COURT_DIRS.put("/cafc/", "http://id.altlaw.org/courts/us/fed/app/fed");
        COURT_DIRS.put("/cafc-gtown/", "http://id.altlaw.org/courts/us/fed/app/fed");
        COURT_DIRS.put("/sct/", "http://id.altlaw.org/courts/us/fed/supreme");
        COURT_DIRS.put("/ca1/", "http://id.altlaw.org/courts/us/fed/app/1");
        COURT_DIRS.put("/ca2/", "http://id.altlaw.org/courts/us/fed/app/2");
        COURT_DIRS.put("/ca3/", "http://id.altlaw.org/courts/us/fed/app/3");
        COURT_DIRS.put("/ca4/", "http://id.altlaw.org/courts/us/fed/app/4");
        COURT_DIRS.put("/ca5/", "http://id.altlaw.org/courts/us/fed/app/5");
        COURT_DIRS.put("/ca6/", "http://id.altlaw.org/courts/us/fed/app/6");
        COURT_DIRS.put("/ca7/", "http://id.altlaw.org/courts/us/fed/app/7");
        COURT_DIRS.put("/ca8/", "http://id.altlaw.org/courts/us/fed/app/8");
        COURT_DIRS.put("/ca9/", "http://id.altlaw.org/courts/us/fed/app/9");
        COURT_DIRS.put("/ca10/", "http://id.altlaw.org/courts/us/fed/app/10");
        COURT_DIRS.put("/ca11/", "http://id.altlaw.org/courts/us/fed/app/11");
    }

    private static final Pattern DIR_REGEX = Pattern.compile("/(cadc|cafc|cafc-gtown|sct|ca\\d{1,2})/");

    public String getName() {
        String s;
        if ((s = data.get("title")) != null &&
            R1.matcher(s).find()) {
            return s;
        } else if ((s = data.get("datetimetitle")) != null &&
                   R2.matcher(s).find()) {
            Matcher m = R3.matcher(s);
            if (m.find()) {
                return m.group(1);
            }
        } else if ((s = data.get("dumbabstract")) != null &&
                   R2.matcher(s).find()) {
            return getTitleFromDumbAbstract(s);
        }
        return null;
    }

    public List<String> getDockets() {
        List<String> d = new ArrayList<String>(4);
        String s;
        if ((s = data.get("docket")) != null) {
            d.add(s);
        }
        if ((s = data.get("docket1")) != null) {
            d.add(s);
        }
        if ((s = data.get("docket2")) != null) {
            d.add(s);
        }
        if ((s = data.get("docket3")) != null) {
            d.add(s);
        }
        if ((s = data.get("docket4")) != null) {
            d.add(s);
        }
        return d;
    }

    private static final Map<String, Integer> MONTHS = new HashMap<String, Integer>(12);
    static {
        MONTHS.put("jan", 1); MONTHS.put("feb", 2); MONTHS.put("mar", 3);
        MONTHS.put("apr", 4); MONTHS.put("may", 5); MONTHS.put("jun", 6);
        MONTHS.put("jul", 7); MONTHS.put("aug", 8); MONTHS.put("sep", 9);
        MONTHS.put("oct", 10); MONTHS.put("nov", 11); MONTHS.put("dec", 12);
    }

    private static final Pattern D1 = Pattern.compile(".*(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]* (\\d{1,2}), (\\d{4}).*",
                                                      Pattern.CASE_INSENSITIVE);
    private static final Pattern D2 = Pattern.compile("(\\d{4})/(\\d{1,2})/(\\d{1,2})\\D?.*");
    private static final Pattern D3 = Pattern.compile(".*?(\\d{1,2})[/-](\\d{1,2})[/-](\\d{4})(?:\\D.+)?");
    private static final Pattern D4 = Pattern.compile("(\\d{1,2})[/-](\\d{1,2})[/-](\\d{2})(?:\\D.+)?");
    private static final Pattern D5 = Pattern.compile(".*?(\\d{1,2})[- ](Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*[- ](\\d{4}).*",
                                                      Pattern.CASE_INSENSITIVE);

    private static final int THIS_YEAR_DIGITS;
    static {
        Calendar c = new GregorianCalendar();
        THIS_YEAR_DIGITS = c.get(Calendar.YEAR) % 100;
    }

    public String getDate() {
        String s;
        int year = 0, month = 0, day = 0;
        Matcher m;
        if ((s = data.get("released")) != null) {
            if ((m = D2.matcher(s)).matches()) {
                year = Integer.parseInt(m.group(1));
                month = Integer.parseInt(m.group(2));
                day = Integer.parseInt(m.group(3));
            } else if ((m = D3.matcher(s)).matches()) {
                month = Integer.parseInt(m.group(1));
                day = Integer.parseInt(m.group(2));
                year = Integer.parseInt(m.group(3));
            } else if ((m = D4.matcher(s)).matches()) {
                month = Integer.parseInt(m.group(1));
                day = Integer.parseInt(m.group(2));
                year = Integer.parseInt(m.group(3));
                year += (year <= THIS_YEAR_DIGITS ? 2000 : 1900);
            } else if ((m = D5.matcher(s)).matches()) {
                day = Integer.parseInt(m.group(1));
                month = MONTHS.get(m.group(2).toLowerCase());
                year = Integer.parseInt(m.group(3));
            }
        } else if ((s = data.get("dumbabstract")) != null &&
                   (m = D1.matcher(s)).matches()) {
            month = MONTHS.get(m.group(1).toLowerCase());
            day = Integer.parseInt(m.group(2));
            year = Integer.parseInt(m.group(3));
        } else if ((s = data.get("datetimetitle")) != null &&
                   (m = D4.matcher(s)).matches()) {
            month = Integer.parseInt(m.group(1));
            day = Integer.parseInt(m.group(2));
            year = Integer.parseInt(m.group(3));
            year += (year <= THIS_YEAR_DIGITS ? 2000 : 1900);
        } else if ((s = data.get("datetimesize")) != null &&
                   (m = D5.matcher(s)).matches()) {
            day = Integer.parseInt(m.group(1));
            month = MONTHS.get(m.group(2).toLowerCase());
            year = Integer.parseInt(m.group(3));
        } else if ((s = data.get("uploaded")) != null &&
                   (m = D4.matcher(s)).matches()) {
            month = Integer.parseInt(m.group(1));
            day = Integer.parseInt(m.group(2));
            year = Integer.parseInt(m.group(3));
            year += (year <= THIS_YEAR_DIGITS ? 2000 : 1900);
        } else if ((s = data.get("uploadeddatetime")) != null &&
                   (m = D3.matcher(s)).matches()) {
            month = Integer.parseInt(m.group(1));
            day = Integer.parseInt(m.group(2));
            year = Integer.parseInt(m.group(3));
        } else if ((s = data.get("lastupdated")) != null &&
                   (m = D3.matcher(s)).matches()) {
            month = Integer.parseInt(m.group(1));
            day = Integer.parseInt(m.group(2));
            year = Integer.parseInt(m.group(3));
        } else if ((s = data.get("date")) != null &&
                   (m = D3.matcher(s)).matches()) {
            month = Integer.parseInt(m.group(1));
            day = Integer.parseInt(m.group(2));
            year = Integer.parseInt(m.group(3));
        } else if ((s = data.get("date")) != null &&
                   (m = D4.matcher(s)).matches()) {
            month = Integer.parseInt(m.group(1));
            day = Integer.parseInt(m.group(2));
            year = Integer.parseInt(m.group(3));
            year += (year <= THIS_YEAR_DIGITS ? 2000 : 1900);
        }




        if (year != 0) {
            /* GregorianCalendar months are zero-indexed. */
            Calendar c = new GregorianCalendar(year, month - 1, day);
            return DateUtils.sqlDateString(c);
        }

        /* Fall back on date file was downloaded. */
        s = data.get("filecreated");
        try {
            Date d = DateUtils.parseUnixDate(s);
            return DateUtils.sqlDateString(d);
        } catch (ParseException e) {
            System.err.println("ParseException");
            return null;
        }
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("\nUsage: java org.altlaw.Ohm1XMLReader <xml file>\n" +
                               "Prints a JSON representation of the file metadata.\n" +
                               "If file is - read from standard input.");
            System.exit(1);
        }
        try {
            Ohm1XMLReader me = new Ohm1XMLReader();
            Reader reader;
            if (args[0].equals("-")) {
                reader = new BufferedReader(new InputStreamReader(System.in));
            } else {
                reader = new FileReader(args[0]);
            }
            me.parse(reader);
            // System.out.println(ThriftUtils.toSimpleJSON(me.toCaseData(), 4));
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private String getTitleFromDumbAbstract(String s) {
        s = R4.matcher(s).replaceFirst("");
        s = R5.matcher(s).replaceFirst("");
        Matcher m = R6.matcher(s);
        String name, rest;
        if (m.find()) {
            name = s.substring(0, m.start()) + "v.";
            rest = R7.matcher(s).replaceFirst("");
        } else {
            name = "";
            rest = s;
        }

        String[] words = R8.split(rest);
        for (String word : words) {
            if (!word.equals(word.toUpperCase())) { break; }
            name = name + " " + word;
        }

        name = R9.matcher(name).replaceFirst("");
        return name.trim();
    }

    public void parse(Reader reader) throws IOException {
        data = new HashMap<String, String>();
        try {
            XMLReader parser = new org.ccil.cowan.tagsoup.Parser();
            parser.setContentHandler(new MyHandler());
            parser.setEntityResolver(new DummyEntityResolver());
            parser.parse(new InputSource(reader));
        } catch (SAXException e) {
            throw new IOException(e);
        } finally {
            reader.close();
        }
    }

    private class MyHandler extends DefaultHandler implements ContentHandler {

        private String currentTag;
        private StringBuilder buffer;

        public void characters(char[] ch, int start, int length) {
            if (buffer != null) {
                buffer.append(ch, start, length);
            }
        }

        public void startElement(String uri, String localName,
                                 String qName, Attributes atts) {
            if (!localName.equals("case")) {
                currentTag = localName.toLowerCase();
                buffer = new StringBuilder();
            }
        }        
        
        public void endElement(String uri, String localName,
                               String qName)
            throws SAXException {

            if (buffer != null) {
                data.put(currentTag, buffer.toString());
            }
            buffer = null;
        }
    }
}
