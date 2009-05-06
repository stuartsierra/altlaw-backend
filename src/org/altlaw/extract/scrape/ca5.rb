class Ca5
  include Expect

  def accept_host
    'www.ca5.uscourts.gov'
  end

  def accept?(download)
    download.request_uri == OPINIONS_URL
  end

  def request
    [DownloadRequest.new(OPINIONS_URL)]
  end

  def parse(download, receiver)
    html = download.response_body_as('US-ASCII')
    doc = Hpricot(html)
    published_cases = search_table(doc.at('table#tblPublished'), true)
    unpublished_cases = search_table(doc.at('table#tblUnpublished'), false)

    published_cases.each {|c| receiver << c}
    unpublished_cases.each {|c| receiver << c}
  end

  private

  BASE_URL = 'http://www.ca5.uscourts.gov/'
  OPINIONS_URL = BASE_URL + 'Opinions.aspx'

  def search_table(tbl, is_published)
    rows = tbl.search('tr')
    first_row = rows.shift

    # Check table headings to make sure nothing's changed.
    cells = first_row.search('td')
    match(cells[0].inner_text, 'Docket')
    match(cells[1].inner_text, 'Last Updated')
    match(cells[2].inner_text, 'Title')

    # Scrape table contents
    rows.collect do |row|
      cells = row.search('td')
      entry = Document.new
      entry.precedential = is_published
      entry.dockets << cells[0].inner_text
      url = BASE_URL + cells[0].at('a')['href'].gsub("\\",'%5C')
      entry.add_link('application/pdf', url)
      entry.date = parse_date(cells[1].inner_text)
      entry.name = cells[2].inner_text
      entry.court = 'http://id.altlaw.org/courts/us/fed/app/5'
      entry
    end
  end

  def parse_date(str)
    m = match(str, %r{^(\d{1,2})/(\d{1,2})/(\d{4})$})
    month, day, year = m[1], m[2], m[3]
    Date.civil(year.to_i, month.to_i, day.to_i)
  end
end
