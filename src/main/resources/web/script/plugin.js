
dm4c.add_plugin('dm4.csv.plugin', function() {
    
     function isLoggedIn() {
        var requestUri = '/accesscontrol/user'
        //
        var response = false
        $.ajax({
            type: "GET", url: requestUri,
            dataType: "text", processData: true, async: false,
            success: function(data, text_status, jq_xhr) {
                if (typeof data === "undefined") return false // this seems to match (new) response semantics
                if (data != "") response = true
            },
            error: function(jq_xhr, text_status, error_thrown) {
                console.warn("CSV Importer Plugin says: Not authenticated.")
                response = false
            }
        })
        return response
    }

    // upload and import the file
    function importCsv() {

        dm4c.get_plugin('de.deepamehta.files').open_upload_dialog('/files/csv', 
                function(file) {

            // Start to import the content of the just uploaded file
            var status = dm4c.restc.request('POST', '/csv/import/' +
                    dm4c.selected_object.uri + '/' + file['topic_id'])

            var $message = $('<span>').text(status.message)
            var $infos = $('<ul>')

            if (status.success) {
                $message.addClass('success')
                $.each(status.infos, function(i, info) {
                    $infos.append($('<li>').text(info))
                })
            } else {
                $message.addClass('error')
            }

            // The following lines throw "TypeError: undefined is not a function"
            // but at least (the importer process) seems to be unaffected.
            try {
                dm4c.ui.dialog({
                    "id"    : 'csvImportStatus',
                    "title" : 'CSV Import Status Report',
                    "content" : $('<div>').append($message).append($infos)
                }).open()
            } catch (ex_un) {
                console.warn("UI Dialog Exception:", ex_un)
            }

        })
    }

    // configure menu and type commands
    dm4c.add_listener('topic_commands', function(topic) {

        var commands = []
        
        if (isLoggedIn() && topic.type_uri === 'dm4.core.topic_type') {
            commands.push({
                is_separator : true,
                context : 'context-menu'
            })
            commands.push({
                label : 'Import CSV',
                handler : importCsv,
                context : [ 'context-menu', 'detail-panel-show' ]
            })
        }
        return commands
    })

})
