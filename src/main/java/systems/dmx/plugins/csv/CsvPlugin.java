package systems.dmx.plugins.csv;

import au.com.bytecode.opencsv.CSVReader;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import javax.ws.rs.Consumes;
import systems.dmx.core.CompDef;
import static systems.dmx.core.Constants.*;
import systems.dmx.core.Topic;
import systems.dmx.core.TopicType;
import systems.dmx.core.model.ChildTopicsModel;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.osgi.PluginActivator;
import systems.dmx.core.service.Inject;
import systems.dmx.core.service.Transactional;
import systems.dmx.core.storage.spi.DMXTransaction;
import systems.dmx.files.FilesService;
import systems.dmx.files.StoredFile;
import systems.dmx.files.UploadedFile;

/**
 * TODO: Update
 * A plugin to map lines of a .CSV file (TAB-separated) to instances of a simple Topic Type.
 * Where simple means that the Topic Type Definition must not have another _Composite_ as child (levels of depth
 * supported not more than one) but  can have many simple child Topic Types (e.g. Text, Number, Boolean, HTML).<br/><br/>
 *
 * Note 1: Updates of topics by URI only are currently not supported by the new DMX 5.0-beta-7<br/><br/>
 *
 * Note 2: During the import process, simple child topics of datatype "text, number and boolean value will be matched by value
 * and Topic Type URI.
 *
 * @author Malte Rei&szlig;ig (<a href="mailto:malte@mikromedia.de">Email</a>, Danny Graf, 2012-2020
 * @version 1.0.0-SNAPSHOT
 */
@Path("csv")
@Produces(MediaType.APPLICATION_JSON)
public class CsvPlugin extends PluginActivator {

    private static Logger log = Logger.getLogger(CsvPlugin.class.getName());

    public static final char SEPARATOR = '|';
    public static final String SEPARATOR_MANY = ",";

    @Inject
    private FilesService files;

    @POST
    @Path("/import")
    @Consumes("multipart/form-data")
    @Transactional
    public Topic uploadCSV(UploadedFile file) {
        String operation = "Uploaded CSV " + file + " SUCCESSFULLY";
        try {
            log.info(operation);
            StoredFile storedFile = files.storeFile(file, "/");
            log.info("File stored. Fetching File Topic for HTTP Response.");
            return dmx.getTopic(storedFile.getFileTopicId());
        } catch (Exception e) {
            throw new RuntimeException(operation + " failed", e);
        }
    }

    @POST
    @Path("import/{fileId}")
    public ImportStatus importCsv(@PathParam("fileId") long fileId) {
        try {

            Topic configuredType = getConfiguredTopicType(fileId);
            TopicType topicType = dmx.getTopicType(configuredType.getUri());
            String typeUri = "";
            if (topicType == null) {
                return new ImportStatus(false, "please associate a Topic Type to the file topic for importing (via File Import association)", null);
            } else {
                typeUri = topicType.getUri();
                log.info("CSV Import configured for \"" + topicType.getSimpleValue() + "\" (" + typeUri + ")");
            }
            List<String[]> lines = readCsvFile(fileId);
            if (lines.size() < 2) {
                return new ImportStatus(false, "please upload a valid CSV, see README", null);
            }

            // read in typeUris of childTopics
            List<String> childTypeUris = Arrays.asList(lines.get(0));
            String uriPrefix = childTypeUris.get(0);
            
            // 
            Map<String, Long> instancesOfUri = getTopicsByTypeWithURIPrefix(typeUri, uriPrefix);
            // Map<String, Map<String, Long>> aggrIdsByTypeUriAndValue = getPossibleAggrChilds(typeUri, childTypeUris);

            // status informations
            int created = 0, deleted = 0, updated = 0;

            // persist each row
            for (int r = 1; r < lines.size(); r++) {
                String[] row = lines.get(r);
                String topicUri = uriPrefix + "." + row[0];

                // create a fresh model
                TopicModel model = mf.newTopicModel(typeUri);
                model.setUri(topicUri);

                // map all columns to composite value
                ChildTopicsModel value = mf.newChildTopicsModel();
                for (int c = 1; c < row.length; c++) {
                    String childTypeUri = childTypeUris.get(c);
                    String childValue = row[c];
                    if (!isMany(topicType, childTypeUri)) {
                        value.set(childTypeUri, childValue);
                    } else {
                        // Fixme: deletion of all former (many) child values?
                        String[] values = childValue.split(SEPARATOR_MANY);
                        for (int i=0; i < values.length; i++) {
                            value.add(childTypeUri, values[i]);
                        }
                    }
                }
                model.setChildTopics(value);

                // create or update a topic
                // this needs to be done in single transactions so referencing aggrated topics by value works
                // when they come all in one .csv file
                Long topicId = instancesOfUri.get(topicUri);
                Topic object = null;
                DMXTransaction tx = dmx.beginTx();
                if (topicId == null) { // create
                    object = dmx.createTopic(model);
                    dmx.createAssoc(mf.newAssocModel(ASSOCIATION,
                            mf.newTopicPlayerModel(object.getId(), CHILD),
                            mf.newTopicPlayerModel(fileId, PARENT)));
                    created++;
                    tx.success();
                } else { // update topic and remove from map (in java memory)
                    model.setId(topicId);
                    instancesOfUri.remove(topicUri);
                    dmx.updateTopic(model);
                    updated++;
                    tx.success();
                }
                tx.finish();
            }

            // delete the remaining instances
            DMXTransaction tx = dmx.beginTx();
            for (String topicUri : instancesOfUri.keySet()) {
                Long topicId = instancesOfUri.get(topicUri);
                dmx.deleteTopic(topicId);
                deleted++;
            }
            tx.success();
            tx.finish();

            List<String> status = new ArrayList<String>();
            status.add("created: " + created);
            status.add("updated: " + updated);
            status.add("deleted: " + deleted);

            return new ImportStatus(true, "SUCCESS", status);
        } catch (IOException e) {
            throw new RuntimeException(e) ;
        }
    }

    private boolean isMany(TopicType topicType, String childTypeUri) {
        CompDef model = topicType.getCompDef(childTypeUri);
        if (model.getChildCardinalityUri().equals("dmx.core.many")) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * get all possible aggregation instances and hash them by typeUri and value
     * 
     * @param typeUri
     * @param childTypeUris
     * @return
     */
    private Map<String, Map<String, Long>> getPossibleAggrChilds(String typeUri, List<String> childTypeUris) {
        TopicType topicType = dmx.getTopicType(typeUri);
        Map<String, Map<String, Long>> aggrIdsByTypeUriAndValue = new HashMap<String, Map<String, Long>>();
        for (CompDef associationDefinition : topicType.getCompDefs()) {
            if (associationDefinition.getTypeUri().equals("dmx.core.composition_def")) {
                String childTypeUri = associationDefinition.getChildTypeUri();
                log.info("childTypeUri: " + childTypeUri);
                if (childTypeUris.contains(childTypeUri)) {
                    log.info("childTypeUris contains: " + getTopicIdsByValue(childTypeUri));
                    aggrIdsByTypeUriAndValue.put(childTypeUri, getTopicIdsByValue(childTypeUri));
                }
            }
        }
        return aggrIdsByTypeUriAndValue;
    }

    /**
     * get all existing instance topics and hash them by value
     * 
     * @param childTypeUri
     * @return instance topics hashed by value
     */
    private Map<String, Long> getTopicIdsByValue(String childTypeUri) {
        Map<String, Long> idsByValue = new HashMap<String, Long>();
        for (Topic instance : dmx.getTopicsByType(childTypeUri)) {
            idsByValue.put(instance.getSimpleValue().toString(), instance.getId());
        }
        return idsByValue;
    }

    /**
     * get all existing instance topics and hash them by URI
     * 
     * @param typeUri
     * @return instance topics hashed by URI
     */
    private Map<String, Long> getTopicsByTypeWithURIPrefix(String typeUri, String uriPrefix) {
        Map<String, Long> idsByUri = new HashMap<String, Long>();
        for (Topic topic : dmx.getTopicsByType(typeUri)) {
            String topicUri = topic.getUri();
            if (topicUri != null && !topicUri.isEmpty()) {
                log.fine("DEBUG: add topicURI \"" + topicUri + "\" to cache map");
                if (topicUri.startsWith(uriPrefix)) idsByUri.put(topicUri, topic.getId());
            } else {
                log.fine("DEBUG: NOT add topicURI \"" + topicUri + "\" to cache map");
            }
        }
        return idsByUri;
    }

    /**
     * read and validate CSV file
     * 
     * @param fileId
     * @return parsed CSV rows with trimmed column array
     * 
     * @throws FileNotFoundException
     * @throws IOException
     */
    private List<String[]> readCsvFile(long fileId) throws IOException {
        String fileName = files.getFile(fileId).getAbsolutePath();
        log.info("read CSV " + fileName);
        CSVReader csvReader = new CSVReader(new FileReader(fileName), SEPARATOR);
        List<String[]> lines = csvReader.readAll();
        csvReader.close();
        // trim all columns
        for (String[] row : lines) {
            for (int col = 0; col < row.length; col++) {
                row[col] = row[col].trim();
            }
        }
        return lines;
    }

    private Topic getConfiguredTopicType(long fileId) {
        Topic csvFile = dmx.getTopic(fileId);
        return csvFile.getRelatedTopic("dmx.csv.file_import", null, null, null);
    }

}
