class Ca7
  include Expect

  def accept_host
    'www.ca7.uscourts.gov'
  end

  def accept?(download)
    download.request_uri == OPINIONS_URL
  end

  def request
    [DownloadRequest.new(OPINIONS_URL,
                         {'Submit'=>'Today',
                           'dtype'=>'Opinion'}),
     DownloadRequest.new(OPINIONS_URL,
                         {'Submit'=>'Today',
                           'dtype'=>'Nonprecedential Disposition'})]
  end

  def parse(download, receiver)
    case type = download.request_form_fields['dtype']
    when 'Opinion'
    when 'Nonprecedential Disposition'
    else
      raise "Ca7Feed: cannot parse database <#{type}>"
    end

    html = download.response_body_as('US-ASCII')
    tables = Hpricot(html).search('table')
    search_table(tables[2], receiver)
  end

  private

  BASE_URL = 'http://www.ca7.uscourts.gov'
  OPINIONS_URL = BASE_URL + '/fdocs/docs.fwx'

  def search_table(tbl, receiver)
    rows = tbl.search('tr')
    first_row = rows.shift

    # Check table headings to make sure nothing's changed.
    cells = first_row.search('th')
    match(cells[0].inner_text, 'Case #')
    match(cells[1].inner_text, 'Caption')
    match(cells[2].inner_text, 'Case Type')
    match(cells[3].inner_text, 'Uploaded')
    match(cells[4].inner_text, 'Description')
    if cells[5]
      match(cells[5].inner_text, 'Judge')
    end

    # Second row is empty
    match(rows.shift.inner_html, %r{(<TD></TD>)+}i)

    rows.each do |row|
      entry = Document.new
      entry.court = 'http://id.altlaw.org/courts/us/fed/app/7'
      cells = row.search('td')
      entry.dockets << cells[0].inner_text.strip
      entry.name = cells[1].inner_text.strip
      entry.subject = cells[2].inner_text.strip
      entry.date = parse_date(cells[3].inner_text.strip)
      url = BASE_URL + cells[4].at('a')['href']
      entry.add_link('application/pdf', url)
      entry.precedential = (cells[4].inner_text.strip == 'Opinion')
      if cells[5]
        entry.author = cells[5].inner_text.strip
      end
      receiver << entry
    end
  end

  def parse_date(str)
    m = match(str, %r{^(\d{2})/(\d{2})/(\d{4})$})
    month,day,year = m[1], m[2], m[3]
    Date.civil(year.to_i, month.to_i, day.to_i)
  end
end
