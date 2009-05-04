class DownloadRequest
  FIELDS=[:request_uri, :request_form_fields]

  attr_accessor(*FIELDS)

  def initialize(uri, form_fields = nil)
    @request_uri = uri
    if form_fields
      form_fields.each do |name, value|
        add_form_field(name, value)
      end
    end
  end

  def add_form_field(name, value)
    @request_form_fields ||= {}
    @request_form_fields[name.to_s] = value.to_s
  end

  def to_hash
    hash = {}
    FIELDS.each do |key|
      value = self.send(key)
      if value
        hash[key] = value
      end
    end
    hash
  end
end
