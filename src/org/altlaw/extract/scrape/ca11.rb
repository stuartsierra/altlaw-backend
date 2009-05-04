class Ca11
  include Expect

  def accept_host
    'www.ca11.uscourts.gov'
  end

  def request
    [DownloadRequest.new(PUBLISHED_URL),
     DownloadRequest.new(UNPUBLISHED_URL)]
  end

  def accept?(download)
    download.request_uri == PUBLISHED_URL or
      download.request_uri == UNPUBLISHED_URL
  end

  def parse(download, receiver)
    published = if download.request_uri.to_s =~ %r{/opinions/} then true
                elsif download.request_uri.to_s =~ %r{/unpub/} then false
                end
    base_uri = download.request_uri.to_s.sub("todaysops.php", "")
    html = download.response_body_as('US-ASCII')
    parse_page(html, published, base_uri, receiver)
  end

  private

  BASE_URL = 'http://www.ca11.uscourts.gov'
  PUBLISHED_URL = BASE_URL + '/opinions/todaysops.php'
  UNPUBLISHED_URL = BASE_URL + '/unpub/todaysops.php'

  def parse_page(html, published, base_uri, receiver)
    doc = Hpricot(html)
    table = doc.at("/html/body/table[3]/tr/td[3]/center/table/tr/td/table")

    rows = table.search("tr")

    # First row title
    row = rows.shift
    match(row.inner_text, /Opinions Issued Today/)

    # 2nd row headings
    row = rows.shift
    cells = row.search("td")
    match(cells[0].inner_text, /Case Name:/)
    match(cells[1].inner_text, /Case #/)
    match(cells[2].inner_text, /Docket #/)
    match(cells[3].inner_text, /Date/)
    match(cells[4].inner_text, /Type/)
    match(cells[5].inner_text, /File/)

    # remaining rows: cases
    rows.each do |row|
      parse_case(row, published, base_uri, receiver)
    end
  end

  def parse_case(row, published, base_uri, receiver)
    cells = row.search("td")
    return if cells.length != 6
    entry = Document.new
    entry.court = 'http://id.altlaw.org/courts/us/fed/app/11'
    entry.precedential = published
    entry.name = cells[0].inner_text.strip
    entry.dockets << cells[1].inner_text.strip
    entry.appeal_from_docket = cells[2].inner_text.strip
    entry.date = parse_date(cells[3].inner_text.strip)
    entry.add_link('application/pdf', base_uri + cells[5].at("a").attributes["href"])
    receiver << entry
  end

  def parse_date(str)
    m = match(str, /(\d{2})-(\d{2})-(\d{4})/)
    month, day, year = m[1], m[2], m[3]
    Date.civil(year.to_i, month.to_i, day.to_i)
  end
end
