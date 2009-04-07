require 'base64'
require 'rubygems'
require 'json'

class Download
  attr_accessor(:request_uri, :request_form_fields,
                :request_method, :request_http_version,
                :request_headers, :response_status_code,
                :response_http_version, :response_status_message,
                :response_headers, :response_body_base64)

  def self.from_json(json_string)
    d = self.new
    JSON.parse(json_string).each_pair do |key,value|
      d.send("#{key}=".to_sym, value)
    end
    d
  end

  def self.load_all_from_json(content)
    downloads = []
    content.each_line do |line|
      downloads << self.from_json(line)
    end
    downloads
  end

  def to_json(*a)
    hash = {}
    [:request_uri, :request_form_fields,
     :request_method, :request_http_version,
     :request_headers, :response_status_code,
     :response_http_version, :response_status_message,
     :response_headers, :response_body_base64].each do |key|
      hash[key] = self.send(key)
    end
    hash.to_json(*a)
  end

  # Get the response body as a Java byte array.
  def response_body_bytes
    org.apache.commons.codec.binary.Base64.
      decodeBase64(@response_body_base64.to_java_bytes)
  end

  # Get the response body as a String, interpreting the bytes in the
  # response in the given character set.
  def response_body_as(charset)
    java.lang.String.new(self.response_body_bytes(), charset).toString()
  end
end
