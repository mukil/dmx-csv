export default ({store, axios: http}) => {

    store.dispatch("registerUploadHandler", { props: {
            mimeType: "text/csv",
            action: "/csv-import/upload",
            selected: function(selected, list) {
                console.log("upload dialog change selected for upload")
            },
            success: function(response, file, fileList) {
                console.log("file uploaded successfully", response)
            },
            error: function(error) {
                console.log("file upload error")
            }
        }})
    console.log("[CSV] dispatched registerUploadHandler")

    return {
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
  }
}
