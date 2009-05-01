class Ca1
  include Expect

  def accept_host
    "www.ca1.uscourts.gov"
  end

  def accept?(download)
    download.request_uri == OPINIONS_URL
  end

  def parse(download, receiver)
    doc = Hpricot(download.response_body_as('US-ASCII'))
    tables = doc.search('table')
    search_table(tables[1], receiver)
  end

  def request
    DownloadRequest.new(OPINIONS_URL)
  end

  private

  BASE_URL = 'http://www.ca1.uscourts.gov'
  PDF_BASE = BASE_URL + '/pdf.opinions/'
  WP_BASE = BASE_URL + '/wp.opinions/'
  OPINIONS_URL = BASE_URL + '/cgi-bin/newopn.pl'

  def search_table(tbl, receiver)
    rows = tbl.search('tr')
    first_row = rows.shift

    # Check table headings to make sure nothing's changed.
    cells = first_row.search('th')
    match(cells[0].inner_text, 'Date of Issuance')
    match(cells[1].inner_text, 'Click forOpinion')
    match(cells[2].inner_text, 'Click forDocketSheet')
    match(cells[3].inner_text, 'Short Title/District')

    # Scrape table contents.  Because of weird HTML, the <td>s are
    # not nested within the <tr>s.
    tds = tbl.search('td')
    rows.length.times do
      cells = [tds.shift, tds.shift, tds.shift, tds.shift]
      entry = Document.new
      entry.date = parse_date(cells[0].inner_text.strip)
      filename = cells[1].at('a').inner_text.strip
      case parse_filename(filename)
      when :published
        entry.precedential = true
      when :unpublished
        entry.precedential = false
      when :errata
        entry.doctype = :errata
      end
      entry.add_link('text/html', BASE_URL + cells[1].at('a')['href'])
      entry.add_link('application/pdf', PDF_BASE + filename.sub('.01A', '-01A.pdf'))
      entry.add_link('application/vnd.wordperfect', WP_BASE + filename)
      entry.dockets << cells[2].inner_text.strip
      name, appeal = parse_title(cells[3])
      entry.name = name
      entry.appeal_from = appeal
      entry.court = "http://id.altlaw.org/courts/us/fed/app/1"
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
    m = match(str, /^\d{2}-\d{4}([PUE])\d*\.01A$/)
    case m[1]
    when 'P' then :published
    when 'U' then :unpublished
    when 'E' then :errata
    end
  end

  def parse_date(str)
    m = match(str, %r{^(\d{1,2})/(\d{1,2})/(\d{4})$})
    month, day, year = m[1], m[2], m[3]
    Date.civil(year.to_i, month.to_i, day.to_i)
  end
end
