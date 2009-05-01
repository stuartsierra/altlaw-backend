require 'rubygems'
require 'hpricot'
require 'date'

require 'org/altlaw/extract/scrape/document'
require 'org/altlaw/extract/scrape/download'
require 'org/altlaw/extract/scrape/download_request'
require 'org/altlaw/extract/scrape/expect'
require 'org/altlaw/extract/scrape/ca1'
require 'org/altlaw/extract/scrape/ca10'

class Hash
  def to_hash
    self
  end
end

class ScraperHandler

  SCRAPER_CLASSES = [Ca1, Ca10]

  def initialize
    @scrapers = {}
    SCRAPER_CLASSES.each do |klass|
      scraper = klass.new
      host = scraper.accept_host
      @scrapers[host] ||= []
      @scrapers[host] << scraper
    end
  end

  def all_requests
    requests = []
    @scrapers.each do |host, scrapers|
      scrapers.each do |scraper|
        if req = scraper.request
          requests << req.to_hash
        end
      end
    end
    requests
  end

  def parse_all_from_file(filename)
    File.open(filename, 'r') {|io| parse_all_from_stream(io)}
  end

  def parse_all_from_stream(stream)
    parse_all(Download.load_all_from_json(stream.read))
  end

  def parse_all(downloads)
    results = []
    downloads.each {|d| results.concat(parse(d))}
    results
  end

  def parse(download)
    results = []
    host = URI.parse(download.request_uri).host
    if candidates = @scrapers[host]
      if scraper = candidates.find {|c| c.accept?(download)}
        results.concat(parse_with(scraper, download))
      else
        results << { :exception => "No scraper accepted #{download.request_uri}" }
      end
    else
      results << { :exception =>  "No scrapers for #{host}"}
    end
    results.map {|r| r.to_hash}
  end

  def parse_with(scraper, download)
    results = []
    scraper.parse(download, results)
    results
  rescue Exception => e
    err = {
      :exception => e.inspect,
      :stacktrace => e.backtrace.join("\n"),
      :scraper_name => scraper.class.name,
      :download => download
    }
    if e.kind_of?(Expect::ExpectationFailed)
      err[:actual] = e.actual
      err[:expected] = e.expected
    end
    results << err
  end
end


if $0 == __FILE__
  me = ScraperHandler.new
  $*.each do |arg|
    me.parse_all_from_file(arg).each do |document|
      puts document.to_json
    end
  end
end
