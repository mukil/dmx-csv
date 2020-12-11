export default ({dmx, store, axios: http, Vue}) => ({

  init () {
    store.dispatch("registerUploadHandler", {
      mimeTypes:  ["application/vnd.ms-excel", "text/csv"], // 1. Win 10 FFox CSV 2. Other CSV
      action: "/csv/import",
      selected: function(file, fileList) {
        console.log("[CSV] upload dialog change selected for upload", fileList)
      },
      success: function(response, file, fileList) {
        this.$store.dispatch("revealTopicById", response.id)
        this.$notify({
          title: 'CSV File Uploaded', type: 'success',
          dangerouslyUseHTMLString: true, duration: 10000,
          message: "To configure the uploaded file's content for import, associate it via a <i>File Import</i> association to its respective Topic Type. "
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
      let isLoggedIn = (store.state.accesscontrol.username)
      if (isLoggedIn && topic.typeUri === 'dmx.files.file') {
        // 1) Check configuration
        let hasTargetTypeConfigured = true
        dmx.rpc.getTopicRelatedTopics(topic.id, {
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
                    dangerouslyUseHTMLString: true,
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
