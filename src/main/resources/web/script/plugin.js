dm4c.add_plugin('dm4.csv.plugin', function() {

    // upload and import the file
    function importCsv() {

        dm4c.get_plugin('de.deepamehta.files').open_upload_dialog('/files/csv', 
                function(file) {

            var status = dm4c.restc.request('POST', '/csv/import/' +
                    dm4c.selected_object.uri + '/' + file.topic_id)
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

            dm4c.ui.dialog({
                id: 'csvImportStatus',
                title: 'CSV Import Status Report',
                content: $('<div>').append($message).append($infos)
            }).open()

        })
    }

    // configure menu and type commands
    dm4c.add_listener('topic_commands', function(topic) {

        var commands = []

        if (topic.type_uri === 'dm4.core.topic_type') {
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
