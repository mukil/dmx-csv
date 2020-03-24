export default ({store, axios: http}) => ({

    init () {
        store.dispatch("registerUploadHandler", {
            mimeType: "CSV", // mimeType or file name ending in UPPERCASE, Fixme: multiple values, e.g. PNG;JPEG;JPG;
            action: "/csv/import",
            selected: function(file, fileList) {
                console.log("[CSV] upload dialog change selected for upload", fileList)
            },
            success: function(response, file, fileList) {
                console.log("[CSV] file uploaded successfully", response)
            },
            error: function(error, file, fileList) {
                console.log("[CSV] file upload error", error)
            }
        })
    },

    contextCommands: {
        topic: topic => {
          if (topic.typeUri === 'dmx.files.file') {
            let isCsvFile = (topic.value.indexOf('.csv') != -1) // Fixme: Do the right thing.
            if (isCsvFile) {
              return [{
                label: 'Import CSV',
                handler: id => {
                  // http.post(`/csv/import/dmx.bookmarks.bookmark/${id}`)
                  http.post(`/csv/import/${id}`)
                  .then(function (response) {
                    console.log("Import Status", response.data);
                    // this.$notify.error()
                    // Vue.prototype.$notify
                  })
                  .catch(function (error) {
                    console.error("Import Error", error);
                  })
                }
              }]
            }
          }
        }
    }

})
