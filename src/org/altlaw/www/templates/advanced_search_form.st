<form action="/v1/search/advanced" id="advanced_search_form" method="get">
  <p>Find cases with:</p>

  <table>
    <tr>
      <th><strong>All</strong> of the words: </th>
      <td><input class="text" id="advanced_all" name="advanced[all]" size="40" type="text" /></td>
    </tr>

    <tr class="alt">
      <th><strong>Any</strong> of the words: </th>
      <td><input class="text" id="advanced_any" name="advanced[any]" size="40" type="text" /></td>
    </tr>

    <tr>
      <th><strong>None</strong> of the words: </th>
      <td><input class="text" id="advanced_none" name="advanced[none]" size="40" type="text" /></td>
    </tr>

    <tr class="alt">
      <th>The <strong>exact phrase</strong>: </th>
      <td><input class="text" id="advanced_phrase" name="advanced[phrase]" size="40" type="text" /></td>
    </tr>

    <tr>
      <td colspan="2">
        <div>These words: <input class="text" id="advanced_near" name="advanced[near]" size="30" type="text" /></div>
        <div>within <input class="short" id="advanced_distance" name="advanced[distance]" size="4" type="text" /> words of each other.</div>
      </td>
    </tr>

    <tr class="alt">
      <th>These words in the <strong>title</strong>:</th>
      <td><input class="text" id="advanced_title" name="advanced[title]" size="40" type="text" /></td>
    </tr>

    <tr>
      <td colspan="2">
        <div id="within_courts">Within these courts:</div>

        <div class="checkboxes">
          <label><input checked="checked" id="advanced_supreme" name="advanced[supreme]" type="checkbox" value="1" /><input name="advanced[supreme]" type="hidden" value="0" /> U.S. Supreme Court</label>
        </div>
        <div class="checkboxes">
          <label><input checked="checked" id="advanced_ca1" name="advanced[ca1]" type="checkbox" value="1" /><input name="advanced[ca1]" type="hidden" value="0" />&nbsp;1st&nbsp;Circuit</label>
          <label><input checked="checked" id="advanced_ca2" name="advanced[ca2]" type="checkbox" value="1" /><input name="advanced[ca2]" type="hidden" value="0" />&nbsp;2nd&nbsp;Circuit</label>

          <label><input checked="checked" id="advanced_ca3" name="advanced[ca3]" type="checkbox" value="1" /><input name="advanced[ca3]" type="hidden" value="0" />&nbsp;3nd&nbsp;Circuit</label>
          <label><input checked="checked" id="advanced_ca4" name="advanced[ca4]" type="checkbox" value="1" /><input name="advanced[ca4]" type="hidden" value="0" />&nbsp;4th&nbsp;Circuit</label>
          <label><input checked="checked" id="advanced_ca5" name="advanced[ca5]" type="checkbox" value="1" /><input name="advanced[ca5]" type="hidden" value="0" />&nbsp;5th&nbsp;Circuit</label>
          <label><input checked="checked" id="advanced_ca6" name="advanced[ca6]" type="checkbox" value="1" /><input name="advanced[ca6]" type="hidden" value="0" />&nbsp;6th&nbsp;Circuit</label>
          <label><input checked="checked" id="advanced_ca7" name="advanced[ca7]" type="checkbox" value="1" /><input name="advanced[ca7]" type="hidden" value="0" />&nbsp;7th&nbsp;Circuit</label>

          <label><input checked="checked" id="advanced_ca8" name="advanced[ca8]" type="checkbox" value="1" /><input name="advanced[ca8]" type="hidden" value="0" />&nbsp;8th&nbsp;Circuit</label>
          <label><input checked="checked" id="advanced_ca9" name="advanced[ca9]" type="checkbox" value="1" /><input name="advanced[ca9]" type="hidden" value="0" />&nbsp;9th&nbsp;Circuit</label>
          <label><input checked="checked" id="advanced_ca10" name="advanced[ca10]" type="checkbox" value="1" /><input name="advanced[ca10]" type="hidden" value="0" />&nbsp;10th&nbsp;Circuit</label>
          <label><input checked="checked" id="advanced_ca11" name="advanced[ca11]" type="checkbox" value="1" /><input name="advanced[ca11]" type="hidden" value="0" />&nbsp;11th&nbsp;Circuit</label>
          <label><input checked="checked" id="advanced_cafc" name="advanced[cafc]" type="checkbox" value="1" /><input name="advanced[cafc]" type="hidden" value="0" />&nbsp;Federal&nbsp;Circuit</label>

          <label><input checked="checked" id="advanced_cadc" name="advanced[cadc]" type="checkbox" value="1" /><input name="advanced[cadc]" type="hidden" value="0" />&nbsp;D.C.&nbsp;Circuit</label>
        </div>
      </td>
    </tr>

    <tr class="alt">
      <td colspan="2">Between the dates <input class="medium" id="advanced_date_start" name="advanced[date_start]" size="15" type="text" />
      and <input class="medium" id="advanced_date_end" name="advanced[date_end]" size="15" type="text" /></td>
    </tr>
  </table>

  <p><input name="commit" type="submit" value="search cases" /></p>
</form>
