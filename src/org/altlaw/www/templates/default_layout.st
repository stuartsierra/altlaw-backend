<div id="container" class="$page_class$">
  <div id="site_head">
    <p id="site_logo">
      <a href="/"><img src="/images/logo1.png" width="172" height="76" alt="AltLaw, the free legal search engine" /></a>
    </p>
    <div id="site_search">
      <form action="/v1/search" class="simple_search" method="get">
        <p>Enter a case name, citation, or key words and phrases:</p>
        <p>
          <input id="q" name="q" type="text" size="30" />
          <input class="button" name="command" type="submit" value="search cases" />
        </p>
      </form>
    </div>
    <div id="site_links">
      <ul>
        <li><a href="/v1/about">About This Site</a></li>
        <li><a href="/v1/about/coverage">Case Coverage</a></li>
        <li><a href="/v1/cases">Browse All Cases</a></li>
        <li><a href="/v1/search/advanced">Advanced Search</a></li>
        <li><a href="/v1/search/boolean">Boolean Search</a></li>
      </ul>
    </div>
  </div>
  <div id="site_body">
    <div id="content">
      <div id="content_head">
        $content_head$
      </div>
      <div id="content_body">
        $content_body$
      </div>
    </div>
    <div id="sidebar">
      $sidebar$
    </div>
  </div>
  <div id="site_foot">
    $site_foot$
    <p>Unless otherwise noted, all cases and statutes on this site are in the public domain.</p>
    <p>DISCLAIMER: The people behind AltLaw are not your lawyers, and nothing on this site should be considered legal advice. We also make no representations or promises about the completeness, accuracy, or timeliness of any of the content on the site. AltLaw is a beta-release project, and we recommend that before relying on our site, you double-check your results with another legal research resource.</p>
  </div>
</div>
