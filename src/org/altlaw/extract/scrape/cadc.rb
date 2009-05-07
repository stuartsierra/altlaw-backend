class Cadc
  include Expect

  def accept_host
    'www.cadc.uscourts.gov'
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

  BASE_URL = 'http://www.cadc.uscourts.gov'
  OPINIONS_URL = BASE_URL + '/bin/opinions/allopinions.asp'

  def parse_page(html, receiver)
    m = match(html, /<TITLE>Opinions Released ([^<]+)<\/TITLE>/)
    date = parse_date(m[1])
    html = m.post_match
    m = match(html, /<\/TABLE>/)
    html = m.pre_match
    html.scan(%r{<TR><TD ALIGN='LEFT' VALIGN=TOP WIDTH='40%'><A HREF='([^']+)'>([^<]+)</A></TD><TD ALIGN='LEFT' VALIGN='TOP'>([^<]+)</TD></TR>}) do |match|
      # '
      url, filename, title = match
      entry = Document.new
      entry.court = 'http://id.altlaw.org/courts/us/fed/app/dc'
      entry.precedential = true ## CADC only puts published opinions on the web
      entry.date = date
      entry.name = title
      entry.dockets << parse_filename(filename)
      entry.add_link('application/pdf', url)
      receiver << entry
    end
  end

  def parse_date(str)
    Date.parse(str)
  end

  def parse_filename(str)
    m = match(str, /(\d+-\d+)-\d+\.pdf/)
    m[1]
  end
end
