# Add AltLaw to Your Search Toolbar

---

<p>Using <a href="http://www.mozilla.com/en-US/firefox/">Mozilla Firefox</a> or <a href="http://www.microsoft.com/windows/products/winfamily/ie/default.mspx">Internet Explorer 7</a>?  Add AltLaw to your search toolbar!</p>
<p>Just click one of the links below:</p>
<script type="text/javascript">
  var cases_xml = "http://altlaw.org/opensearch/cases.xml";
  var uscodes_xml = "http://altlaw.org/opensearch/uscode.xml";
  function add_link(url, body) {
    document.write('<p><a href="#" onclick="window.external.AddSearchProvider(' + "'" + url + "'" + ')">' + body + '</a></p>');
  }
  // if (window.external.AddSearchProvider) fails in IE7.
  if (typeof window.external.AddSearchProvider != 'undefined') {
    add_link(cases_xml, "Add AltLaw Case Search");
    //add_link(uscodes_xml, "Add AltLaw U.S. Code Search");
  } else {
    document.write("<p>Sorry, your web browser does not support to the search toolbar.</p>");
  }
</script>
<noscript>
  <p>Sorry, either you have JavaScript turned off, or your web browser does not support it.</p>
</noscript>
