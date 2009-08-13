#!/usr/bin/ruby

require 'rubygems'
require 'hpricot'
require 'date'
require 'pp'
require 'fileutils'

require 'org/altlaw/extract/scrape/document'
require 'org/altlaw/extract/scrape/download'
require 'org/altlaw/extract/scrape/download_request'
require 'org/altlaw/extract/scrape/expect'

class Run
  def run(args)
    puts "AltLaw.org web scraper tester."
    if args.length.zero?
      exit_with_usage()
    end

    classname = args.shift
    command = args.shift

    case command
    when "fetch"
      fetch(load_scraper(classname))
    when "scrape"
      scrape(load_scraper(classname))
    when "loop"
      run_loop(classname)
    else
      puts "Invalid command: #{command}"
      puts "Run without arguments for usage instructions."
      exit(1)
    end
  end


  private

  def load_scraper(classname)
    class_file_name = underscore(classname) + '.rb'
    class_file_path = "src/org/altlaw/extract/scrape/#{class_file_name}"
    if File.exists?(class_file_path)
      load("src/org/altlaw/extract/scrape/#{class_file_name}")
      return Kernel.const_get(classname).new
    else
      puts "ERROR: the file #{class_file_name} does not exist"
      puts "in the directory src/org/altlaw/extract/scrape/"
      exit(1)
    end
  end

  def run_loop(classname)
    while true
      scraper = load_scraper(classname)
      puts ">>> Starting with #{classname}"
      begin
        fetch(scraper)
        scrape(scraper)
      rescue Exception => e
        puts ">>> ERROR"
        puts e.inspect
        puts e.stacktrace[0..5]
      end

      puts ">>> Done with #{classname}"
      puts ">>> Modify your scraper code as needed."
      puts ">>> Press ENTER to run again; Ctrl-C to quit."
      readline
    end
  end

  def fetch(scraper)
    require 'net/https'
    require 'uri'

    classname = scraper.class.name
    puts "Removing old files for #{classname}."
    FileUtils.rm_f("#{classname}-*.html")
    FileUtils.rm_f("#{classname}-*.meta")
    FileUtils.rm_f("#{classname}-*.out")

    requests = scraper.request()

    unless requests.kind_of?(Array)
      requests = [requests]
    end

    i = 0
    requests.each do |request|
      response = http_download(request)
      write_response(request, response, scraper.class.name, i)
      i += 1
    end
  end

  def http_download(request)
    uri = URI.parse(request.request_uri)
    http = Net::HTTP.new(uri.host, uri.port)
    http.use_ssl = true if uri.scheme == 'https'
    form = request.request_form_fields

    request_path = uri.path + (uri.query ? "?#{uri.query}" : "")

    if form.nil?
      http.start { http.request_get(request_path) }
    else
      req = Net::HTTP::Post.new(request_path)
      req.form_data = form
      http.start do |h|
        h.request(req)
      end
    end
  end

  def write_response(request, response, classname, i)
    file = "#{classname}-#{i}.html"
    puts "Saving web page to #{file}"
    File.open(file, 'w') do |io|
      io.write(response.body)
    end

    file = "#{classname}-#{i}.meta"
    puts "Saving metadata to #{file}"
    File.open(file, 'w') do |io|
      hash = request.to_hash
      if request.request_form_fields.nil?
        hash[:request_method] = "GET"
      else
        hash[:request_method] = "POST"
      end
      io.puts(hash.inspect)
    end
  end


  def scrape(scraper)
    classname = scraper.class.name
    html_files = Dir.glob("#{classname}-*.html")

    i = 0
    html_files.each do |html_file|
      puts "Running scraper #{classname} on file #{html_file}"

      meta_file = html_file.sub('.html', '.meta')
      html = File.read(html_file)
      meta = eval(File.read(meta_file))
      documents = run_scraper(scraper, html, meta)

      write_documents(classname, documents, i)
      i += 1
    end
  end

  def run_scraper(scraper, html, meta)
    classname = scraper.class.name
    download = Download.from_map(meta)
    download.response_body_bytes = html.to_java_bytes
    documents = []
    scraper.parse(download, documents)
    documents
  end

  def write_documents(classname, documents, i)
    filename = "#{classname}-#{i}.out";
    puts "Saving scraper output to #{filename}"
    File.open(filename, 'w') do |io|
      documents.each do |doc|
        hash = doc.to_hash
        hash[:date] = hash[:date].to_s
        PP.pp(hash, io)
        io.puts ""
      end
    end
  end


  def exit_with_usage
    puts <<EOF
Usage: script/scrape.sh classname command

Where classname is the name of your Ruby scraper class
placed in src/org/altlaw/extract/scrape/

And command is either "fetch" or "scrape"

"fetch" will download the HTML file and save it.

"scrape" will attempt to scrape the downloaded files.

"loop" will run fetch, then scrape, then pause while you
make any needed changes, then repeat.
EOF
    Kernel.exit(1)
  end


  # from vendor/rails/activesupport/lib/active_support/inflector.rb, line 175
  def underscore(camel_cased_word)
    camel_cased_word.to_s.gsub(/::/, '/').
      gsub(/([A-Z]+)([A-Z][a-z])/,'\1_\2').
      gsub(/([a-z\d])([A-Z])/,'\1_\2').
      tr("-", "_").
      downcase
  end
end


if $0 == __FILE__
  Run.new.run(ARGV)
end
