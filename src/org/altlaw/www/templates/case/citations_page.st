$default_layout(
    html_title={$name$ - Altlaw},
    page_class={doctype_$doctype$},
    norobots=true,
    content_head=case/content_head(),
    sidebar=case/sidebar(),
    content_body=case/citations_content_body()
)$