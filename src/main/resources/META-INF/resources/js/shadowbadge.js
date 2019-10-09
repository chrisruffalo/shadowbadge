function rowClicked(value) {
    location.href = value;
}

function unlcaimModal(event, modalId, spanId, linkId, badgeName, unclaimHref) {
    // do not propagate to row click
    localEvent = event || window.event;
    localEvent.stopPropagation();

    // set badge id
    $(spanId).text(badgeName);
    $(linkId).attr("href", unclaimHref);

    // show modal
    $(modalId).modal('show');
}

function showHideCustomQr() {
    var dropdownValue = $('#qrTypeSelect option:selected').val();

    if ("CUSTOM" === dropdownValue) {
        $('#customQRFormGroup').removeClass("d-none")
    } else {
        $('#customQRFormGroup').addClass("d-none")
    }
}

function openQRCamera(node) {
    var reader = new FileReader();
    reader.onload = function() {
        // load src into img
        $("#qr").attr('src', reader.result);

        node.value = "";

        const codeReader = new ZXing.BrowserQRCodeReader();

        try {
            result = codeReader.decodeFromImage('qr').then((result) => {
                if (result !== null && result.text !== null) {
                    $('#inputCustomQR').val(result.text);
                }
            });
        } catch (err) {
            console.error(err);
        }
    };
    reader.readAsDataURL(node.files[0]);
}