<dl class='boolean_search_doc'>
  <dt><var>part1</var> AND <var>part2</var></dt>
  <dd>
    Document must match both <var>part1</var> and <var>part2</var>.
  </dd>

  <dt><var>part1</var> OR <var>part2</var></dt>
  <dd>Document must match either <var>part1</var> or <var>part2</var>, or both.</dd>

  <dt>NOT <var>part</var></dt>
  <dd>Document must not match <var>part</var>.</dd>

  <dt>(<var>term1</var> AND <var>term2</var>) OR (<var>term3</var> AND <var>term4</var>)</dt>
  <dd>Use parentheses to group terms.</dd>

  <dt>"<var>multi-word phrase</var>"</dt>
  <dd>Document must contain the exact phrase.</dd>

  <dt>"<var>word1 word2 word3</var>"~20</dt>
  <dd>Document must contain <var>word1</var>, <var>word2</var>, and <var>word3</var> within 20 words of each other (proximity search). </dd>

  <dt>court:<var>name</var></dt>
  <dd>Document must be from the court <var>name</var>.  Abbreviations work, such as "1st" for the First Circuit Court of Appeals.</dd>

  <dt>date:<var>yyyy</var>-<var>mm</var>-<var>dd</var></dt>
  <dd>Document must match the exact date <var>yyyy</var>-<var>mm</var>-<var>dd</var>.</dd>

  <dt>date:[<var>yyyy</var>-<var>mm</var>-<var>dd</var> TO <var>yyyy</var>-<var>mm</var>-<var>dd</var>]</dt>
  <dd>Document must be within the range of dates, inclusive.</dd>

  <dt>name:<var>part</var></dt>
  <dd>The title of the case must match <var>part</var>.</dd>
</dl>
