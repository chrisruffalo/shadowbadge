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
        $('#customQrFormRow').removeClass("d-none")
    } else {
        $('#customQrFormRow').addClass("d-none")
    }
}

function readFromServlet(node) {
    console.log("reading qr from servlet...")
    $("#inputCustomQR").prop("readonly", true);
    $("#qrUpload").prop("disabled", true);
    $("#qr-btn").addClass("btn-qr-disabled");
    $("#qr-btn").removeClass("btn-qr");

    const data = new FormData();
    data.append('qr', node.files[0])

    const config = { headers: { 'Content-Type': 'multipart/form-data' } };
    axios.post('/qr/detect', data, config)
    .then((response) => {
        if (response !== null && response.status === 200 && response.data !== null && response.data.length > 0) {
            console.log("servlet response: " + response.data)
            $('#inputCustomQR').val(response.data);
        }
    })
    .catch((error) => {
        console.log(error);
    })
    .finally(() => {
        $("#inputCustomQR").prop("readonly", false);
        $("#qrUpload").prop("disabled", false);
        $("#qr-btn").removeClass("btn-qr-disabled");
        $("#qr-btn").addClass("btn-qr");

        // clear files
        node.value = "";
    });
}