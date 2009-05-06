class Ca3
  include Expect

  def accept_host
    'www.ca3.uscourts.gov'
  end

  def accept?(download)
    download.request_uri == PRECEDENTIAL_URL or
      download.request_uri == NON_PRECEDENTIAL_URL
  end

  def request
    [DownloadRequest.new(PRECEDENTIAL_URL),
     DownloadRequest.new(NON_PRECEDENTIAL_URL)]
  end

  def parse(download, receiver)
    html = download.response_body_as('US-ASCII')
    doc = Hpricot(html)
    case heading = doc.at('h1').inner_text
    when "Precedential Opinions Filed Today"
      parse_page(download, true, receiver)
    when "Not Precedential Opinions Filed Today"
      parse_page(download, false, receiver)
    else
      raise "Ca3Feed: cannot parse page with H1 <#{heading}>"
    end
  end

  private

  PRECEDENTIAL_URL = 'http://www.ca3.uscourts.gov/recentop/week/recprec2day.htm'
  NON_PRECEDENTIAL_URL = 'http://www.ca3.uscourts.gov/recentop/week/recnon2day.htm'

  def parse_page(download, is_precedent, receiver)
    html = download.response_body_as('US-ASCII')
    lines = html.split("\n")
    entry = nil
    while line = lines.shift
      if line =~ %r{<BR>Filed (\d{2})/(\d{2})/(\d{2}), No\. ([^<]+)<BR>}
        month, day, year, docket = $1, $2, $3, $4
        entry = Document.new
        entry.court = 'http://id.altlaw.org/courts/us/fed/app/3'
        entry.date = Date.civil("20#{year}".to_i, month.to_i, day.to_i)
        entry.dockets << docket
        entry.precedential = is_precedent

        line = lines.shift
        if line =~ %r{<a href='([^']+)'>([^<]+)</a><BR>} #'
          entry.add_link('application/pdf', $1)
          entry.name = $2
        end

        line = lines.shift
        if line =~ /([^<]+)<BR>/
          entry.appeal_from = $1
          receiver << entry
        end
      end
    end
  end

  def parse_date(str)
    m = match(str, %r{^(\d{1,2})-(\d{1,2})-(\d{4})$})
    month, day, year = m[1], m[2], m[3]
    Date.civil(year.to_i, month.to_i, day.to_i)
  end
end
