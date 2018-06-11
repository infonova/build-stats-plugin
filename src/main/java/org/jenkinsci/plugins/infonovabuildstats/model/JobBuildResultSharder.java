package org.jenkinsci.plugins.infonovabuildstats.model;

import jenkins.model.Jenkins;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.infonovabuildstats.utils.CollectionsUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class assigns the build results to monthly files according to their job start date.
 *
 */
public class JobBuildResultSharder {

    private static final Logger LOGGER = Logger.getLogger(JobBuildResultSharder.class.getName());

    /**
     * Format if saved file
     */
    private static final String JOB_RESULT_FILENAME_SDF = "'agentStatistics-'YYYY-MM-dd'.xml'";

    /**
     * Path, from jenkins_home, to infonova-build-stats folder
     */
    private static final String IBS_ROOT_PATH = "infonova-build-stats";

    /**
     * Hand-off queue from the event callback of
     * {@link org.jenkinsci.plugins.infonovabuildstats.business.InfonovaBuildStatsPluginSaver.BeforeSavePluginCallback}
     * to
     * the
     * thread that's adding the records. Access needs to be synchronized.
     */
    private final List<AgentStatistic> queuedResultsToAdd = Collections
        .synchronizedList(new ArrayList<AgentStatistic>());

    public JobBuildResultSharder() {
        this(null, new ArrayList<AgentStatistic>());
    }

    public JobBuildResultSharder(JobBuildResultSharder sharder, List<AgentStatistic> agentStatistics) {
        //TODO Remove toogled out line
    	//this.persistedDailyResults = Collections.synchronizedMap(toJobResultFilenameMap(agentStatistics));
        if (sharder != null) {
            this.queueResultsToAdd(sharder.queuedResultsToAdd);
        }
    }

    /**
     * Transforming given AgentStatistic list into a map of type [filename of monthly job result file => list of job
     * results]
     */
    private static Map<String, List<AgentStatistic>> toJobResultFilenameMap(List<AgentStatistic> results) {
        // Sharding job build results depending on monthly rolling files
        Map<String, List<AgentStatistic>> byDayJobResults = new HashMap<String, List<AgentStatistic>>();
        for (AgentStatistic r : results) {
            Calendar completedDate = Calendar.getInstance();
            completedDate.setTime(r.getOfflineDate());

            String targetFilename = new SimpleDateFormat(JOB_RESULT_FILENAME_SDF).format(completedDate.getTime());

            if (!byDayJobResults.containsKey(targetFilename)) {
                LOGGER.log(Level.FINER, "Filename (" + targetFilename + ") not contained, create new arrayList.");
                byDayJobResults.put(targetFilename, new ArrayList<AgentStatistic>());
            }
            byDayJobResults.get(targetFilename).add(r);
        }

        return byDayJobResults;
    }

    public void queueResultsToAdd(List<AgentStatistic> results) {
        queuedResultsToAdd.addAll(results);
    }

    public List<AgentStatistic> getJobBuildResults() {
        return Collections.unmodifiableList(queuedResultsToAdd);
    }

    public boolean pendingChanges() {
        return !queuedResultsToAdd.isEmpty();
    }

    public void queueResultToAdd(AgentStatistic result) {
        queuedResultsToAdd.add(result);
    }

    /**
     * Main method for writing build results to file which is called from
     * {@link org.jenkinsci.plugins.infonovabuildstats.xstream.InfonovaBuildStatsXStreamConverter#marshal(Object, com.thoughtworks.xstream.io.HierarchicalStreamWriter, com.thoughtworks.xstream.converters.MarshallingContext)
     * InfonovaBuildStatsXStreamConverter.marshal()}
     */
    public void applyQueuedResultsInFiles() {
        LOGGER.log(Level.FINER, "Starting persisting queueResultsToAdd.");
        // atomically move all the queued stuff into a local list
        List<AgentStatistic> resultsToAdd;

        synchronized (queuedResultsToAdd) {
            resultsToAdd = new ArrayList<AgentStatistic>(queuedResultsToAdd);

            LOGGER.log(Level.FINER, "Size of queuedResultsToAdd: " + resultsToAdd.size());

            queuedResultsToAdd.clear();
        }

        if (resultsToAdd.isEmpty()) {
            LOGGER.log(Level.INFO, "No changes detected in job results update queue!");
            return;
        }

        File jobResultsRoot = getJobResultFolder();

        LOGGER.log(Level.FINER, "Try to write changes to folder: " + jobResultsRoot.toString());

        if (!jobResultsRoot.exists()) {
            try {
                LOGGER.log(Level.FINER, "Root not exists try to create.");

                FileUtils.forceMkdir(jobResultsRoot);

            } catch (IOException e) {

                LOGGER.log(Level.WARNING, "Not possible to create rootfolder: " + jobResultsRoot);

                throw new IllegalStateException("Can't create job results root directory : "
                    + jobResultsRoot.getAbsolutePath(), e);
            }
        }

        Map<String, List<AgentStatistic>> persistedDailyResults = toJobResultFilenameMap(resultsToAdd);

        List<String> updatedFilenamesList = new ArrayList<String>(persistedDailyResults.keySet());

        Collection<String> updatedFilenames = CollectionsUtil.toSet(updatedFilenamesList);

        long start = System.currentTimeMillis();

        for (String filename : updatedFilenames) {
            String jobResultFilepath = jobResultsRoot + File.separator + filename;

            LOGGER.log(Level.FINE, "Writing jobResults to file: " + jobResultFilepath);

            String encoding = StringUtils.defaultIfBlank(System.getProperty("file.encoding"), "UTF-8");
            try (OutputStreamWriter fw = new OutputStreamWriter(new FileOutputStream(jobResultFilepath), encoding)) {
                // If file exists we want to append, if not exists this will work automatically
                List<AgentStatistic> daily = persistedDailyResults.get(filename);

                Iterator iter = daily.iterator();

                while (iter.hasNext()) {
                    Jenkins.XSTREAM.toXML(iter.next(), fw);
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Unable to serialize job results into " + jobResultFilepath, e);
                throw new IllegalStateException("Unable to serialize job results into " + jobResultFilepath, e);
            }
        }

        LOGGER.log(Level.FINE, "Persisting took: " + (System.currentTimeMillis() - start));

        LOGGER.log(Level.FINER, "Finished persisting queueResultsToAdd.");
    }

    private static File getJobResultFolder() {
        return new File(Jenkins.getInstance().getRootDir().getAbsolutePath() + File.separator + IBS_ROOT_PATH);
    }
}
