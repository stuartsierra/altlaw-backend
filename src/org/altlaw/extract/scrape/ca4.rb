class Ca4
  include Expect

  def accept_host
    'pacer.ca4.uscourts.gov'
  end

  def accept?(download)
    download.request_uri == OPINIONS_URL
  end

  def request
    [DownloadRequest.new(OPINIONS_URL)]
  end

  def parse(download, receiver)
    find_opinions(download, receiver)
  end

  private

  BASE_URL = 'http://pacer.ca4.uscourts.gov/'
  OPINIONS_URL = BASE_URL + 'opinions_today.htm'

  def find_opinions(download, receiver)
    html = download.response_body_as('US-ASCII')
    entry = nil
    lines = html.split("\n")
    while (line = lines.shift)
      if line =~ %r{^<a href="(opinion.pdf/[^.]+\.[UP]\.pdf)" target=[^>]+><b> ([^.]+\.[UP]) </b></a>[ .]+(\d{2}/\d{2}/\d{4}) (.+)$}
        path,docket,datestring,title = $1,$2,$3,$4
        entry = Document.new
        entry.court = 'http://id.altlaw.org/courts/us/fed/app/4'
        entry.name = title.strip
        entry.date = parse_date(datestring)
        entry.dockets << docket
        entry.add_link('application/pdf', BASE_URL + path)
        if docket =~ /\.P$/
          entry.precedential = true
        else
          entry.precedential = false
        end

        # subsequent lines should be
        match(lines.shift, '<br>')
        match(lines.shift, '<b>')

        line = lines.shift
        if line =~ /^ (Published|Unpublished) opinion/
          entry.description = line.strip
        else
          # title may continue on to next line:
          entry.name += ' ' + line.strip
        end

        # subsequent lines
        match(lines.shift, '<br>')
        entry.appeal_from = lines.shift.strip
        match(lines.shift, '<br>')
        line = lines.shift
        entry.subject = line.strip if line

        receiver << entry
      end
    end
  end

  def parse_date(str)
    m = match(str, %r{^(\d{1,2})/(\d{1,2})/(\d{4})$})
    month, day, year = m[1], m[2], m[3]
    Date.civil(year.to_i, month.to_i, day.to_i)
  end
end
