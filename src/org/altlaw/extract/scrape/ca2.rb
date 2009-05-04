class Ca2
  include Expect

  def accept_host
    'www.ca2.uscourts.gov'
  end

  def accept?(download)
    download.request_uri == OPINIONS_URL and
      (download.request_form_fields['IW_DATABASE'] == 'OPN' or
       download.request_form_fields['IW_DATABASE'] == 'SUM')
  end

  def request
    date = (Date.today - 1).strftime("%Y%m%d")
    req1 = DownloadRequest.new(OPINIONS_URL,
                               {'IW_DATABASE' => 'OPN',
                                 'IW_SORT' => 'DATE',
                                 'IW_BATCHSIZE' => '50',
                                 'IW_FIELD_TEXT' => '*',
                                 'IW_FILTER_DATE_AFTER' => date})

    req2 = DownloadRequest.new(OPINIONS_URL,
                               {'IW_DATABASE' => 'SUM',
                                 'IW_SORT' => 'DATE',
                                 'IW_BATCHSIZE' => '50',
                                 'IW_FIELD_TEXT' => '*',
                                 'IW_FILTER_DATE_AFTER' => date})
    [req1, req2]
  end

  def parse(download, receiver)
    case db = download.request_form_fields['IW_DATABASE']
    when 'SUM'
      parse_page(download, false, receiver)
    when 'OPN'
      parse_page(download, true, receiver)
    else
      raise Exception.new("Unrecognized IW_DATABASE field in request.")
    end
  end

  private

  BASE_URL = 'http://www.ca2.uscourts.gov'
  OPINIONS_URL = BASE_URL + '/decisions'

  def parse_page(download, is_precedent, receiver)
    html = download.response_body_as('US-ASCII')
    return nil if html =~ /Your search did not find any matching documents/
    return nil if html =~ /500 Internal Server Error/

    doc = Hpricot(html)
    tables = doc.search('table')

    # First table
    tables.shift # nested table
    match(tables.shift.inner_text, /Search results/)

    # Second table
    tables.shift # nested table
    documents = tables.shift.inner_text
    match(documents, /Documents 1 to (\d+) of (\d+)/)
    documents =~ /Documents 1 to (\d+) of (\d+)/
    match($1, $2)
    doc_count = $1.to_i

    # Third table
    cells = tables.shift.search('td')
    match(cells[0].inner_text, 'Docket #')
    match(cells[1].inner_text, 'Caption')
    match(cells[2].inner_text, 'Date Posted')
    match(cells[3].inner_text, 'Type')

    doc_count.times do
      cells = tables.shift.search('td')
      entry = Document.new
      entry.precedential = is_precedent
      url = BASE_URL + cells[0].at('a')['href']

      # Ca2 appends a fragment string like
      #  "#xml=http://www.ca2.uscourts.gov:8080/isysquery/irl685c/1/hilite"
      # to every PDF URL.
      # This fragment string changes each time the page is requested.
      # Removing the fragment string ensures the URLs are valid and consistent.
      url.sub!(/#.+$/, '')

      entry.add_link('application/pdf', url)
      entry.dockets << cells[0].inner_text.strip
      entry.name = cells[1].inner_text
      entry.date = parse_date(cells[2].inner_text)
      entry.court = 'http://id.altlaw.org/courts/us/fed/app/2'
      receiver << entry
    end
  end

  def parse_date(str)
    m = match(str, %r{^(\d{1,2})-(\d{1,2})-(\d{4})$})
    month, day, year = m[1], m[2], m[3]
    Date.civil(year.to_i, month.to_i, day.to_i)
  end
end
