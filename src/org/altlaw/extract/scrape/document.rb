class Document

  FIELDS = [:docid, :doctype, :name, :date, :files, :dockets,
  :citations, :court, :html, :text, :size, :incites, :outcites,
  :norobots, :removed, :precedential, :appeal_from, :subject, :author,
  :abstract, :decided_by, :opinion_type, :links]

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

  def to_json(*a)
    hash = {}
    FIELDS.each do |key|
      hash[key] = self.send(key)
    end
    hash.to_json(*a)
  end
end
