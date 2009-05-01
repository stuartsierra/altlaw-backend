class Ca10
  include Expect

  def accept_host
    'www.ca10.uscourts.gov'
  end

  def accept?(download)
    download.request_uri == OPINIONS_URL
  end

  def request
    DownloadRequest.new(OPINIONS_URL)
  end

  def parse(download, receiver)
    parse_page(download.response_body_as("US-ASCII"), receiver)
  end

  private

  BASE_URL = 'http://www.ca10.uscourts.gov'
  OPINIONS_URL = BASE_URL + '/clerk/opinions.php'

  def parse_page(html, receiver)
    doc = Hpricot(html)
    td = doc.search('td').find {|e| e.inner_html =~ /^<span class="headline">Today/}
    published, unpublished = td.to_html.split("Unpublished")
    parse_section(published, receiver, true)
    parse_section(unpublished, receiver, false)
  end

  def parse_section(html, receiver, precedential)
    if html
      doc = Hpricot(html)
      doc.search("a").each do |a|
        title = a.attributes['title']
        if title != nil
          entry = Document.new
          entry.court = 'http://id.altlaw.org/courts/us/fed/app/10'
          entry.precedential = precedential
          entry.date = Date.today
          entry.dockets << a.inner_html.gsub(/<\/?[^>]*>/, "").gsub(/&nbsp;/,"")
          entry.add_link('application/pdf', BASE_URL + a.attributes["href"])
          entry.name = title
          receiver << entry
        end
      end
    end
  end

  def parse_date(str)
    m = Expect.match(str, %r{^(\d{1,2})-(\d{1,2})-(\d{4})$})
    month, day, year = m[1], m[2], m[3]
    Date.civil(year.to_i, month.to_i, day.to_i)
  end
end
