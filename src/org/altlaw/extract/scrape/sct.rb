class Sct
  include Expect

  def accept_host
    'www.supremecourtus.gov'
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

  BASE_URL = 'http://www.supremecourtus.gov'
  OPINIONS_URL = BASE_URL + '/opinions/08slipopinion.html'
  PDFS_BASE_URL = BASE_URL + '/opinions/'

  JUSTICES = {
"A" => "Associate Justice Samuel A. Alito, Jr.",
"AS" => "Associate Justice Antonin Scalia",
"B" => "Associate Justice Stephen G. Breyer",
"D" => "Decree in Original Case",
"DS" => "Associate Justice David H. Souter",
"G" => "Associate Justice Ruth Bader Ginsburg",
"JS" => "Associate Justice John Paul Stevens",
"K" => "Associate Justice Anthony M. Kennedy",
"O" => "Associate Justice Sandra Day O'Connor",
"PC" => "Unsigned Per Curiam Opinion",
"R" => "Chief Justice John G. Roberts, Jr.",
"T" => "Associate Justice Clarence Thomas"
  }

  def parse_page(html, receiver)
    doc = Hpricot(html)
    unless table = doc.at("table[@cellpadding='2']")
      throw Exception.new("Failed to match main table XPath")
    end

    rows = table.search('tr')
    first_row = rows.shift
    cells = first_row.search('th')
    match(cells[0].inner_text, /R-/)
    match(cells[1].inner_text, /Date/)
    match(cells[2].inner_text, /Docket/)
    match(cells[3].inner_text, /Name/)
    match(cells[4].inner_text, /J\./)
    match(cells[5].inner_text, /Pt\./)

    rows.each {|r| parse_case(r, receiver)}
  end

  private

  def parse_case(row, receiver)
    cells = row.search('td')
    return if cells.length.zero?
    entry = Document.new
    entry.court = 'http://id.altlaw.org/courts/us/fed/supreme'
    entry.sequence_number = cells[0].inner_text.strip
    entry.date = parse_date(cells[1].inner_text.strip)
    entry.dockets << cells[2].inner_text.strip
    entry.name = cells[3].inner_text.strip
    url = PDFS_BASE_URL + cells[3].at('a').attributes['href']
    entry.add_link('application/pdf', url)
    entry.opinion_by = JUSTICES[cells[4].inner_text.strip]
    entry.reporter_part = cells[5].inner_text.strip
    receiver << entry
  end

  def parse_date(str)
    m = match(str, %r{(\d{1,2})/(\d{1,2})/(\d{2})})
    month, day, year = m[1], m[2], "20#{m[3]}"
    Date.civil(year.to_i, month.to_i, day.to_i)
  end
end
