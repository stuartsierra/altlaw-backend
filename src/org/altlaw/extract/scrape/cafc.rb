class Cafc
  include Expect

  def accept_host
    'www.cafc.uscourts.gov'
  end

  def accept?(download)
    download.request_uri == OPINIONS_URL
  end

  def request
    [DownloadRequest.new(OPINIONS_URL)]
  end

  def parse(download, receiver)
    html = download.response_body_as('US-ASCII')
    parse_page(html, receiver)
  end

  private

  BASE_URL = 'http://www.cafc.uscourts.gov'
  OPINIONS_URL = BASE_URL + '/dailylog.html'

  def parse_page(html, receiver)
    doc = Hpricot(html)
    unless table = doc.at("table[@summary='List of Federal Circuit Decisions']")
      raise Exception.new("CafcFeed: XPath for main table failed")
    end
    rows = table.search('tr')
    first_row = rows.shift

    cells = first_row.search('th')
    match(cells[0].inner_text, 'Date')
    match(cells[1].inner_text, 'Dkt. #')
    match(cells[2].inner_text, 'Origin')
    match(cells[3].inner_text, 'Case Name')
    match(cells[4].inner_text, 'P/N')

    rows.each {|r| parse_row(r, receiver)}
  end

  def parse_row(row, receiver)
    entry = Document.new
    entry.court = 'http://id.altlaw.org/courts/us/fed/app/fed'

    cells = row.search('td')
    return if cells[0].inner_text.nil? or cells[0].inner_text.empty?

    entry.date = parse_date(cells[0].inner_text)
    entry.dockets << cells[1].inner_text.sub('.pdf', '')
    entry.appeal_from = cells[2].inner_text

    entry.name = cells[3].at('a').inner_text
    link = cells[3].at('a').attributes['href']
    link.sub!(/^\./,'')
    url = BASE_URL + link
    entry.add_link('application/pdf', url)

    entry.precedential = (cells[4].inner_text == 'P')

    if cells[3].inner_text =~ /\[([A-Za-z]+)\]/
      entry.opinion_type = $1
    end

    receiver<< entry
  end

  def parse_date(str)
    m = match(str, %r{^(\d{4})/(\d{1,2})/(\d{1,2})$})
    year, month, day = m[1], m[2], m[3]
    Date.civil(year.to_i, month.to_i, day.to_i)
  end
end
