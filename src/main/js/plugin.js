export default ({dm5, store, axios: http, Vue}) => ({

  init () {
    store.dispatch("registerUploadHandler", {
      mimeType: "CSV", // mimeType or file name ending in UPPERCASE, Fixme: multiple values, e.g. PNG;JPEG;JPG;
      action: "/csv/import",
      selected: function(file, fileList) {
        console.log("[CSV] upload dialog change selected for upload", fileList)
      },
      success: function(response, file, fileList) {
        this.$store.dispatch("revealTopicById", response.id)
        this.$notify({
          title: 'CSV File Uploaded', type: 'success'
        })
        this.$store.dispatch("closeUploadDialog")
      },
      error: function(error, file, fileList) {
        console.log("[CSV] file upload error", error)
        this.$notify.error({
          title: 'CSV File Upload Failed', message: 'Error: ' + JSON.stringify(error)
        })
        this.$store.dispatch("closeUploadDialog")
      }
    })
  },

  contextCommands: {
    topic: topic => {
      if (topic.typeUri === 'dmx.files.file') {
        // 1) Check configuration
        let hasTargetTypeConfigured = true
        dm5.restClient.getTopicRelatedTopics(topic.id, {
          assocTypeUri: "dmx.csv.file_import", othersTopicTypeUri: "dmx.core.topic_type"})
                .then(response => {
                  hasTargetTypeConfigured = (response.length > 0) ? true : false
                })
        // 2) Check if file topic ends on .CSV
        let isCsvFile = (topic.value.indexOf('.csv') !== -1) // Fixme: Do the right thing.
        if (isCsvFile) {
          return [{
            label: 'Import CSV',
            handler: id => {
              // 3.2) Execute if configuration is _potentially_ correct
              if (hasTargetTypeConfigured) {
                http.post(`/csv/import/${id}`)
                .then(function (response) {
                  console.log("Import Status", response.data)
                  Vue.prototype.$notify({
                    title: "CSV Import Successful",
                    message: JSON.stringify(response.data.infos),
                    type: "success"
                  })
                })
                .catch(function (error) {
                  console.error("Import Error", error);
                })
              } else {
                // 3.2) Notify about mis-configuration before executing the import
                Vue.prototype.$notify({
                  title: "Import operation needs to know the <i>Topic Type</>",
                  dangerouslyUseHTMLString: true, duration: 10000,
                  message: "You must relate a <i>Topic Type</i> to <i>"+topic.value+"</i> before importing data. "
                  + "Create a <i>File Import</i> association to the <i>Topic Type</i> you want to import data to.",
                  type: "error"
                })
              }
            }
          }]
        }
      }
    }
  }

})
