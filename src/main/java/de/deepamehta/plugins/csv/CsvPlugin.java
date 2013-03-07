package de.deepamehta.plugins.csv;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;

import au.com.bytecode.opencsv.CSVReader;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.model.CompositeValueModel;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.PluginService;
import de.deepamehta.core.service.annotation.ConsumesService;
import de.deepamehta.core.storage.spi.DeepaMehtaTransaction;
import de.deepamehta.plugins.files.ResourceInfo;
import de.deepamehta.plugins.files.service.FilesService;

@Path("csv")
public class CsvPlugin extends PluginActivator {

    private static Logger log = Logger.getLogger(CsvPlugin.class.getName());

    public static final String FOLDER = "csv";

    public static final char SEPARATOR = '|';

    private boolean isInitialized;

    private FilesService fileService;

    @POST
    @Path("import/{uri}/{file}")
    public ImportStatus importCsv(//
            @PathParam("uri") String typeUri,//
            @PathParam("file") long fileId,//
            @HeaderParam("Cookie") ClientState cookie) {

        DeepaMehtaTransaction tx = dms.beginTx();
        try {
            // status informations
            int created = 0, deleted = 0, updated = 0;

            // read and validate CSV file
            String fileName = fileService.getFile(fileId).getAbsolutePath();
            log.info("import CSV " + fileName);
            CSVReader csvReader = new CSVReader(new FileReader(fileName), SEPARATOR);
            List<String[]> lines = csvReader.readAll();
            csvReader.close();
            if (lines.size() < 2) {
                return new ImportStatus(false, "please upload a valid CSV, see README", null);
            }

            // get all existing instances and hash them by URI
            HashMap<String, Long> topicsByUri = new HashMap<String, Long>();
            Iterator<RelatedTopic> topics = dms.getTopics(typeUri, false, 0, cookie).getIterator();
            while (topics.hasNext()) {
                RelatedTopic topic = (RelatedTopic) topics.next();
                String topicUri = topic.getUri();
                if (topicUri != null && topicUri.trim().isEmpty() == false) {
                    topicsByUri.put(topicUri.trim(), topic.getId());
                }
            }

            // get header and child type URIs
            String[] childTypeUris = lines.get(0);
            String uriPrefix = childTypeUris[0].trim();
            for (int h = 1; h < childTypeUris.length; h++) {
                childTypeUris[h] = childTypeUris[h].trim();
            }

            // persist each row
            for (int r = 1; r < lines.size(); r++) {
                String[] row = lines.get(r);

                // create a fresh model and map all columns to composite value
                TopicModel model = new TopicModel(typeUri);
                CompositeValueModel value = new CompositeValueModel();
                for (int c = 1; c < row.length; c++) {
                    value.put(childTypeUris[c], row[c].trim());
                }
                model.setCompositeValue(value);

                // create or update a topic
                String topicUri = uriPrefix + "." + row[0].trim();
                model.setUri(topicUri);
                Long topicId = topicsByUri.get(topicUri);
                if (topicId == null) { // create
                    dms.createTopic(model, cookie);
                    created++;
                } else { // update topic and remove from map
                    model.setId(topicId);
                    topicsByUri.remove(topicUri);
                    dms.updateTopic(model, cookie);
                    updated++;
                }
                log.info(model.toJSON().toString());
            }

            // delete the remaining instances
            for (String topicUri : topicsByUri.keySet()) {
                Long topicId = topicsByUri.get(topicUri);
                dms.deleteTopic(topicId, cookie);
                deleted++;
            }

            List<String> infos = new ArrayList<String>();
            infos.add("created: " + created);
            infos.add("updated: " + updated);
            infos.add("deleted: " + deleted);

            tx.success();
            return new ImportStatus(true, "SUCCESS", infos);
        } catch (Exception e) {
            throw new WebApplicationException(e);
        } finally {
            tx.finish();
        }
    }

    /**
     * Initialize.
     */
    @Override
    public void init() {
        isInitialized = true;
        configureIfReady();
    }

    @Override
    @ConsumesService("de.deepamehta.plugins.files.service.FilesService")
    public void serviceArrived(PluginService service) {
        if (service instanceof FilesService) {
            fileService = (FilesService) service;
        }
        configureIfReady();
    }

    @Override
    public void serviceGone(PluginService service) {
        if (service == fileService) {
            fileService = null;
        }
    }

    private void configureIfReady() {
        if (isInitialized && fileService != null) {
            createCsvDirectory();
        }
    }

    private void createCsvDirectory() {
        // TODO move the initialization to migration "0"
        try {
            ResourceInfo resourceInfo = fileService.getResourceInfo(FOLDER);
            String kind = resourceInfo.toJSON().getString("kind");
            if (kind.equals("directory") == false) {
                String repoPath = System.getProperty("dm4.filerepo.path");
                throw new IllegalStateException("CSV storage directory " + //
                        repoPath + File.separator + FOLDER + " can not be used");
            }
        } catch (WebApplicationException e) { // !exists
            // catch fileService info request error => create directory
            if (e.getResponse().getStatus() != 404) {
                throw e;
            } else {
                log.info("create CSV directory");
                fileService.createFolder(FOLDER, "/");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
