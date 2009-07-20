class ScraperTemplate
  include Expect  # always include this

  def accept_host
    # Return a String, like 'www.ca5.uscourts.gov', giving the domain
    # name of the web site that this scraper is supposed to handle.
  end

  def accept?(download)
    # download is a Download object
    #
    # Check the URL and any other information in the Download object
    # to make sure it's the right page for this scraper to parse.
    # If it is, return true.  Otherwise, return false.
  end

  def request
    # Create one or more DownloadRequest objects.  These represent
    # commands to the web crawler to download particular pages.
    #
    # For a GET request, just do DownloadRequest.new(url)
    #
    # For a WWW form POST request, do
    #
    #     DownloadRequest.new(url, {"field1" => "value1",
    #                               "field2" => "value2"})
    #
    # Return the DownloadRequest objects in an Array
  end

  def parse(download, receiver)
    # download is a Download object.
    # receiver is some object that can receive Documents.
    #
    # To get the HTML out of the Download, do this:
    #
    #   html = download.response_body_as('US-ASCII')
    #
    # Where you have to specify the encoding of the HTML.  For most
    # sites, US-ASCII will be sufficient; but if it's not you have to
    # check if it's UTF-8 or ISO-8859-1.
    #
    # Next, you'll scan the HTML for cases and create a Document
    # object for each one.
    #
    # Send each document you create to the receiver like this:
    #
    #   receiver << document
    #
  end
end
