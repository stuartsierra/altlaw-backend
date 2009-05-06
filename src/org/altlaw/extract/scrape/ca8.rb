class Ca8
  include Expect

  def accept_host
    'www.ca8.uscourts.gov'
  end

  def accept?(download)
    download.request_uri == OPINIONS_URL
  end

  def request
    [DownloadRequest.new(OPINIONS_URL)]
  end

  def parse(download, receiver)
    html = download.response_body_as('US-ASCII')
    search_html(html, receiver)
  end

  private

  OPINIONS_URL = 'http://www.ca8.uscourts.gov/cgi-bin/new/today2.pl'

  def search_html(html, receiver)
    chunks = html.split(/<font [^>]+>/)

    # check first chunk
    match(chunks.shift.split("\n")[1], /OPINIONS ARE POSTED DAILY/)

    # check second chunk
    match(chunks.shift.split("\n")[1], /The most recent opinions are for/)

    # Process cases
    chunks.each {|chunk| parse_chunk(chunk, receiver)}
  end

  def parse_chunk(html, receiver)
    details, abstract = html.split('<br>')
    abstract.sub!('</pre></body></html>', '')  # remove HTML footer
    lines = details.split("\n")

    link, filename, datestring, title = parse_first_line(lines.shift)
    entry = Document.new
    entry.abstract = abstract.strip
    entry.court = 'http://id.altlaw.org/courts/us/fed/app/8'
    entry.add_link('application/pdf', link)
    entry.name = title.strip
    entry.date = parse_date(datestring)
    entry.precedential = parse_filename(filename)

    # Read docket numbers
    line = lines.shift
    while line =~ /^\s+(U\.S\. Court of Appeals Case No:|and No:)\s+([0-9-]+)$/
      entry.dockets << $2
      line = lines.shift
    end

    # Read district court name
    match(line, /District|Commission|Board/)
    entry.appeal_from = line.strip

    # Read names of judges from remaining lines
    judges = lines.join(" ")
    if judges =~ /^\s+\[(PUBLISHED|UNPUBLISHED)\] \[([^\]]+)\]$/
      entry.decided_by = $2.strip.gsub(/\s+/, ' ')
    end

    receiver << entry
  end

  def parse_first_line(line)
    m = match(line, %r{<a href="([^"]+)">([^<]+)</a>\s+<b>(\d{2}/\d{2}/\d{4})\s+([^<]+)})
    # "
    [m[1],m[2],m[3],m[4]]
  end

  def parse_filename(filename)
    m = match(filename, /([UP])\.pdf$/)
    return (m[1] == 'P')
  end

  def parse_date(str)
    m = match(str, %r{^(\d{2})/(\d{2})/(\d{4})$})
    month,day,year = m[1], m[2], m[3]
    Date.civil(year.to_i, month.to_i, day.to_i)
  end
end
