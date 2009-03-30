package org.altlaw.util;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import java.io.StringReader;

/** SAX XML entity resolver that always returns an empty InputSource.
 * This effectively prevents the XML parser from downloading external
 * entities such as DTD's from the web.  It also breaks all external
 * entities such as XHTML special characters, or any custom entities
 * you may have defined.  The built-in XML entities such as
 * <code>amp</code> and <code>lt</code> will still work, as will
 * numeric character entities.
 *
 * <p>Use it like this:
 *
 * <pre>
 * XMLReader reader = XMLReaderFactory.createXMLReader();
 * reader.setEntityResolver(new DummyEntityResolver());
 * reader.setContentHandler(new YourContentHandler());
 * reader.parse(your_xml_source);
 * </pre>
 *
 * @author Stuart Sierra (stuartsierra.com)
 * @see <a href="http://www.ibm.com/developerworks/library/x-tipent.html">Using an entity resolver</a>, IBM developerWorks.
 * @see <a href="http://www.w3.org/blog/systeam/2008/02/08/w3c_s_excessive_dtd_traffic">W3C's Excessive DTD Traffic</a>, W3C Systems Team Blog.
 *
 */
public class DummyEntityResolver implements EntityResolver {
    public InputSource resolveEntity(String publicID, String systemID)
        throws SAXException {
        
        return new InputSource(new StringReader(""));
    }
}