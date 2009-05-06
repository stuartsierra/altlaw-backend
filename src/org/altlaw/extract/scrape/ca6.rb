class Ca6
  include Expect

  def accept_host
    'www.ca6.uscourts.gov'
  end

  def accept?(download)
    download.request_uri == OPINIONS_URL
  end

  def request
    yesterday = (Date.today - 1).strftime('%m/%d/%Y')
    today = Date.today.strftime('%m/%d/%Y')
    [DownloadRequest.new(OPINIONS_URL,
                         {'FROMDATE'=>yesterday,
                           'TODATE'=>today})]
  end

  def parse(download, receiver)
    html = download.response_body_as('US-ASCII')
    Hpricot(html).search('table') do |table|
      search_table(table, receiver)
    end
  end

  private

  BASE_URL = 'http://www.ca6.uscourts.gov'
  OPINIONS_URL = BASE_URL + '/cgi-bin/opinions.pl'

  def search_table(tbl, receiver)
    rows = tbl.search('tr')
    first_row = rows.shift

    # Check table headings to make sure nothing's changed.
    cells = first_row.search('th')
    match(cells[0].inner_text, 'Opinion')
    match(cells[1].inner_text, 'DocketSheet')
    match(cells[2].inner_text, 'Pub Date')
    match(cells[3].inner_text, 'Short Title/District')

    # Scrape table contents.  Because of weird HTML, the <td>s are
    # not nested within the <tr>s.
    tds = tbl.search('td')
    rows.length.times do
      cells = [tds.shift, tds.shift, tds.shift, tds.shift]
      entry = Document.new
      entry.court = 'http://id.altlaw.org/courts/us/fed/app/6'
      pdf_link = BASE_URL + cells[0].at('a')['href']
      entry.add_link('application/pdf', pdf_link)
      next unless entry.precedential = parse_filename(pdf_link)
      entry.dockets << cells[1].inner_text.strip
      entry.date = parse_date(cells[2].inner_text)
      entry.name, entry.appeal_from = parse_title(cells[3])
      receiver << entry
    end
  end

  def parse_title(elem)
    district = elem.at('font').inner_text.strip
    elem.search('font').remove
    elem.search('br').remove
    title = elem.inner_html.gsub('&nbsp;','').strip
    return [title, district]
  end

  def parse_filename(str)
    m = match(str, /([npa])-\d+\.pdf$/)
    return m[1] =~ /[pa]/
  end

  def parse_date(str)
    m = match(str, %r{^(\d{4})/(\d{2})/(\d{2})$})
    year,month,day = m[1], m[2], m[3]
    Date.civil(year.to_i, month.to_i, day.to_i)
  end
end
