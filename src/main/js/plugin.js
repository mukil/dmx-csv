export default ({store, axios: http}) => ({

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
            })
            .catch(function (error) {
              console.error("Import Error", error);
            });
          }
        }]  
        }
      }
    }
  }
})
