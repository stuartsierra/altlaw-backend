package org.altlaw.extract;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.WordUtils;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import org.altlaw.util.DummyEntityResolver;
import org.altlaw.util.RunProgramOnFile;

public class PROHTMLToText {
    private static final byte versionID = 1;

    public static final String filter(final String input) {
        if (input == null) { return null; }

        try {
            PROHTMLToText me = new PROHTMLToText(new org.ccil.cowan.tagsoup.Parser());
            me.parse(new StringReader(input));
            return me.toString();
        } catch (Exception e) {
            return null;
        }
    }

    public static final String filterWithTagSoup(final String input) {
        return filter(input);
    }

    public static void main(final String[] args) {
        if (args.length != 1) {
            System.err.println("\nUsage: java PROHTMLToText <file>\n" +
                               "Converts public.resource.org XHTML file to plain text.\n");
            System.exit(1);
        }

        try {
            String data = FileUtils.readFileToString(new File(args[0]),
                                                     "UTF-8");
            System.out.println(PROHTMLToText.filter(data));
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private StringBuilder output = new StringBuilder();

    private XMLReader reader;

    public PROHTMLToText() { ; }

    private PROHTMLToText(XMLReader xmlreader) {
        this.reader = xmlreader;
    }

    public String toString() {
        return output.toString();
    }

    private void parse(Reader input) throws IOException {
        try {
            if (reader == null) {
                reader = XMLReaderFactory.createXMLReader();
            }
            reader.setContentHandler(new MyHandler());
            reader.setEntityResolver(new DummyEntityResolver());
            InputSource source = new InputSource(input);
            reader.parse(source);
        } catch (SAXException e) {
            throw new IOException(e);
        } finally {
            input.close();
        }
    }

    private static final Pattern NEWLINES =
        Pattern.compile(" *\\r?\\n *");

    private class MyHandler extends DefaultHandler
        implements ContentHandler {

        private int paraCount;
        private StringBuilder buffer;

        public void characters(char[] ch, int start, int length) {
            if (buffer != null) {
                buffer.append(ch, start, length);
            }
        }

        public void startElement(String uri, String localName,
                                 String qName, Attributes atts) {
            if (localName.equals("p")) {
                buffer = new StringBuilder();
            } else if (buffer != null && localName.equals("br")) {
                buffer.append("\n");
            }
        }        
        
        public void endElement(String uri, String localName,
                               String qName)
            throws SAXException {

            if (buffer != null && localName.equals("p")) {
                /* Add line break if past 1st paragraph. */
                if (paraCount > 0) {
                    output.append("\n\n");
                }

                /* Rewrap string to 70 characters wide. */
                String buf = buffer.toString().trim();
                buf = NEWLINES.matcher(buf).replaceAll(" ");
                buf = WordUtils.wrap(buf, 70);
                
                output.append(buf);
                paraCount++;
                buffer = null;
            }
        }
    }
}