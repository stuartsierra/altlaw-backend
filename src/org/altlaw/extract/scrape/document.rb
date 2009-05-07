class Document

  FIELDS = [:docid, :doctype, :name, :date, :files, :dockets,
  :citations, :court, :html, :text, :size, :incites, :outcites,
  :norobots, :removed, :precedential, :appeal_from, :subject, :author,
  :abstract, :decided_by, :opinion_type, :links, :appeal_from_docket,
  :description, :immediate_filing, :reporter_part, :sequence_number,
  :opinion_by]

  attr_accessor(*FIELDS)

  def initialize
    @links = {}
    @dockets = []
    @citations = []
  end

  def add_link(mime_type, url)
    @links[mime_type] ||= []
    @links[mime_type] << url
  end

  def self.from_json(json_string)
    d = self.new
    JSON.parse(json_string).each_pair do |key,value|
      d.send("#{key}=".to_sym, value)
    end
    d
  end

  def to_hash
    hash = {}
    FIELDS.each do |key|
      value = self.send(key)
      if value
        hash[key] = self.send(key)
      end
    end
    hash
  end

  def to_json(*a)
    self.to_hash.to_json(*a)
  end
end
