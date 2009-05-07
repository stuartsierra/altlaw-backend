class Ca9
  include Expect

  def accept_host
    'www.ca9.uscourts.gov'
  end

  def accept?(download)
    download.request_uri == OPINIONS_URL or
      download.request_uri =~ SUBPAGE_REGEX
  end

  def request
    [DownloadRequest.new(OPINIONS_URL)]
  end

  def parse(download, receiver)
    if download.request_uri == OPINIONS_URL
      xml = download.response_body_as('US-ASCII')
      parse_xml_feed(xml, receiver)
    elsif download.request_uri =~ SUBPAGE_REGEX
      html = download.response_body_as('US-ASCII')
      parse_html_opinion(html, receiver)
    else
      throw Exception.new("Unrecognized URI: " + download.request_uri)
    end
  end

  private

  BASE_URL = 'http://www.ca9.uscourts.gov'
  OPINIONS_URL = BASE_URL + '/feed/opinions.xml'

  SUBPAGE_REGEX = %r{^http://www\.ca9\.uscourts\.gov/opinions/view_subpage\.php\?pk_id=\d+$}

  OPINION_TYPES = {
    'o'=>'order',
    'oa'=>'order amended',
    'oad'=>'order dissent',
    'oop'=>'order & opinion',
    'o2'=>'second opinion',
    'eb'=>'en banc',
    'ebm'=>'eb memorandum',
    'ebo'=>'eb order',
    'cd'=>'concurrence/dissent',
    'ad'=>'amended dissent',
    's'=>'supplemental opinion',
    'sd'=>'supplemental dissent',
    'ao'=>'amended opinion',
    'co'=>'corrected opinion',
    'app'=>'appendix',
    'o2'=>'second opinion'
  }

  def parse_xml_feed(xml, receiver)
    doc = Hpricot.XML(xml)
    doc.search('item').each do |item|
      entry = Document.new
      url = item.attributes['rdf:about']
      entry.add_link('text/html', url)
      title = item.at('title').inner_text
      m = match(title, /^ Case: (.*), (\d{2}-\d{5})$/)
      entry.name = m[1]
      entry.dockets << m[2]
      entry.date = parse_feed_date(item.at('description').inner_text)
      entry.court = 'http://id.altlaw.org/courts/us/fed/app/9'
      receiver << entry
    end
  end

  def parse_html_opinion(html, receiver)
    doc = Hpricot(html)
    entry = Document.new
    entry.court = 'http://id.altlaw.org/courts/us/fed/app/9'

    titlestring = doc.at('head/title').inner_text
    return if titlestring =~ /NO OPINIONS FILED TODAY/
    titlestring.sub!('Opinion for', '')
    titleparts = titlestring.split(',')
    entry.dockets << titleparts.pop.strip
    entry.name = titleparts.join(',').strip

    unless datatable = doc.at('div#bd div#yui-main div.yui-b table[2]')
      raise Exception.new("CSS selector for data table failed.")
    end
    rows = datatable.search('tr')

    match(rows[0].at('td[1]').inner_text, 'Case Number:')
    entry.dockets << rows[0].at('td[2]').inner_text

    match(rows[1].at('td[1]').inner_text, 'Immediate Filing:')
    entry.immediate_filing = (rows[1].at('td[2]').inner_text == 'yes')

    match(rows[2].at('td[1]').inner_text, 'Case Type:')
    entry.subject = rows[2].at('td[2]').inner_text

    match(rows[3].at('td[1]').inner_text, 'Case Code:')
    entry.opinion_type = OPINION_TYPES[rows[3].at('td[2]').inner_text]

    unless iframe = doc.at("iframe#view_opinion")
      raise Exception.new('Missing IFRAME')
    end
    url = BASE_URL + iframe.attributes['src']
    entry.add_link('application/pdf', url)
    entry.date = parse_url_date(url)

    receiver << entry
  end

  def parse_feed_date(str)
    m = match(str, %r{^Date filed: (\d{2})/(\d{2})/(\d{4})$})
    month, day, year = m[1], m[2], m[3]
    Date.civil(year.to_i, month.to_i, day.to_i)
  end

  def parse_url_date(str)
    m = match(str, %r{/datastore/opinions/(\d{4})/(\d{2})/(\d{2})/})
    year,month,day = m[1], m[2], m[3]
    Date.civil(year.to_i, month.to_i, day.to_i)
  end
end
