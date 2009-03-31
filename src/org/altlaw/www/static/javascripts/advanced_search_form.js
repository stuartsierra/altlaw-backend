function docheck_all_courts(checked) {
    $('#advanced_search_form input[type="checkbox"]').attr("checked", checked);
    return false;
}

function check_all_courts() { docheck_all_courts(true); }
function uncheck_all_courts() { docheck_all_courts(false); }

$(document).ready(function() {
        $("#within_courts").append('&nbsp;&nbsp;&nbsp;&nbsp;<a href="javascript:check_all_courts()">Check all</a>&nbsp;&nbsp;&nbsp;&nbsp;<a href="javascript:uncheck_all_courts()">Uncheck all</a>');
    });
