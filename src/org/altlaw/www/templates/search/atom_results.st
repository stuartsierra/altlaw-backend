<?xml version="1.0" encoding="utf-8"?>
<feed xmlns="http://www.w3.org/2005/Atom" xmlns:opensearch="http://a9.com/-/spec/opensearch/1.1/">
  <title>AltLaw Search: $query$</title>
  <link href="http://www.altlaw.org/"/>
  <updated>$timestamp$</updated>
  <author><name>AltLaw.org</name></author>
  <id>$atom_id$</id>
  <opensearch:totalResults>$total_hits$</opensearch:totalResults>
  <opensearch:startIndex>$start_hit$</opensearch:startIndex>
  <opensearch:itemsPerPage>$hits_per_page$</opensearch:itemsPerPage>
  <opensearch:Query role="request" searchTerms="$query$" startPage="$page$" />
  <link rel="alternate" href="$html_results_url$" type="text/html"/>
  <link rel="self" href="$atom_results_url$" type="application/atom+xml"/>
  <link rel="first" href="$atom_first_page_url$" type="application/atom+xml"/>
  <link rel="previous" href="$atom_prev_page_url$" type="application/atom+xml"/>
  <link rel="next" href="$atom_next_page_url$" type="application/atom+xml"/>
  <link rel="last" href="$atom_last_page_url$" type="application/atom+xml"/>
  <link rel="search" type="application/opensearchdescription+xml" href="http://www.altlaw.org.com/opensearch/cases.xml"/>
  $hits:search/atom_hit()$
</feed>
