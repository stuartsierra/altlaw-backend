$default_layout(
    html_title={Boolean Search - AltLaw},
    content_head={<h1>Boolean Search</h1>},
    content_body={
        $search/boolean_search_form()$
        <h2>Boolean Search Instructions</h2>
        $search/boolean_search_doc()$
    }
)$