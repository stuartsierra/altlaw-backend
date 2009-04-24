require 'rubygems'
require 'hpricot'
require 'date'

require 'org/altlaw/extract/scrape/download'
require 'org/altlaw/extract/scrape/expect'
require 'org/altlaw/extract/scrape/ca1'

class ScraperHandler

  SCRAPER_CLASSES = [Ca1]

  def initialize
    @scrapers = {}
    SCRAPER_CLASSES.each do |klass|
      scraper = klass.new
      host = scraper.accept_host
      @scrapers[host] ||= []
      @scrapers[host] << scraper
    end
  end

  def parse_all_from_file(filename)
    File.open(filename, 'r') {|io| parse_all_from_stream(io)}
  end

  def parse_all_from_stream(stream)
    parse_all(Download.load_all_from_json(stream.read))
  end

  def parse_all(downloads)
    downloads.each {|d| parse(d)}
    self
  end

  def parse(download)
    host = URI.parse(download.request_uri).host
    if candidates = @scrapers[host]
      if scraper = candidates.find {|c| c.accept?(download)}
        parse_with(scraper, download)
      else
        self << { :exception => "No scraper accepted #{download.request_uri}" }
      end
    else
      self << { :exception =>  "No scrapers for #{host}"}
    end
    self
  end

  def <<(x)
    puts x.to_json
  end

  def parse_with(scraper, download)
    scraper.parse(download, self)
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
    self << err
  end
end


if $0 == __FILE__
  me = ScraperHandler.new
  $*.each do |arg|
    me.parse_all_from_file(arg)
  end
end
