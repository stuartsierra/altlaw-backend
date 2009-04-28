$default_layout(
    html_title={$name$ - Altlaw},
    page_class={doctype_$doctype$},
    content_head=case/content_head(),
    sidebar=case/sidebar(),
    content_body={$html$}
)$