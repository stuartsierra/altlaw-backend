module Expect
  class ExpectationFailed < Exception
    attr_accessor(:actual, :expected, :download, :stacktrace,
                  :input_line, :code_line)

    def initialize(actual, expected)
      @actual = actual
      @expected = expected
    end
  end

  def match(actual, expected)
    if expected.kind_of?(Regexp) and md = expected.match(actual)
      return md
    elsif actual == expected
      return true
    else
      raise ExpectationFailed.new(actual, expected)
    end
  end
end
