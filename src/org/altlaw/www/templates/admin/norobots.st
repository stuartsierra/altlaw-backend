$admin/admin_layout(

    content_body={
<h1>Docid No Robots</h1>
<form method="post">
<p><label for="docid">Add new norobots Docid</label>
<input name="docid" type="text" size="10" />
<p><input type="submit" value="Submit" /></p>
</p></form>
<h2>Current norobots:</h2>
<p>$norobots; separator=", "$</p>
})$