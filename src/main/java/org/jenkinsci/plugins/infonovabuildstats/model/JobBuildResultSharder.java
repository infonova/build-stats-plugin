package org.jenkinsci.plugins.infonovabuildstats.model;

import hudson.model.Hudson;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import jenkins.model.Jenkins;

import org.apache.commons.io.FileUtils;
import org.jenkinsci.plugins.infonovabuildstats.utils.CollectionsUtil;


public class JobBuildResultSharder {

    private static final Logger LOGGER = Logger.getLogger(JobBuildResultSharder.class.getName());

    private static final SimpleDateFormat JOB_RESULT_FILENAME_SDF = new SimpleDateFormat("'jobResults-'yyyy-MM'.xml'");

    // Path, from jenkins_home, to infonova-build-stats folder
    private static final String IBS_ROOT_PATH = "infonova-build-stats";

    /**
     * Effective persisted results list
     */
    private final List<JobBuildResult> persistedResults;
    /**
     * Effective persisted results map
     * Note: persistedResults & persistedMonthlyResults are always coherent
     */
    private final Map<String, List<JobBuildResult>> persistedMonthlyResults;

    /**
     * Hand-off queue from the event callback of
     * {@link org.jenkinsci.plugins.infonovabuildstats.business.InfonovaBuildStatsPluginSaver.BeforeSavePluginCallback}
     * to
     * the
     * thread that's adding the records. Access needs to be synchronized.
     */
    private final List<JobBuildResult> queuedResultsToAdd = Collections
        .synchronizedList(new ArrayList<JobBuildResult>());

    public JobBuildResultSharder() {
        this(null, new ArrayList<JobBuildResult>());
    }

    public JobBuildResultSharder(JobBuildResultSharder sharder, List<JobBuildResult> jobBuildResults) {
        this.persistedResults = Collections.synchronizedList(jobBuildResults);
        this.persistedMonthlyResults = Collections.synchronizedMap(toJobResultFilenameMap(jobBuildResults));
        if (sharder != null) {
            this.queueResultsToAdd(sharder.queuedResultsToAdd);
        }
    }

    /**
     * Transforming given JobBuildResult list into a map of type [filename of daily job result file => list of job
     * results]
     */
    private static Map<String, List<JobBuildResult>> toJobResultFilenameMap(List<JobBuildResult> results) {
        // Sharding job build results depending on monthly rolling files
        Map<String, List<JobBuildResult>> byMonthJobResults = new HashMap<String, List<JobBuildResult>>();
        for (JobBuildResult r : results) {
            String targetFilename = JOB_RESULT_FILENAME_SDF.format(r.getBuildStartDate().getTime());
            if (!byMonthJobResults.containsKey(targetFilename)) {
                byMonthJobResults.put(targetFilename, new ArrayList<JobBuildResult>());
            }
            byMonthJobResults.get(targetFilename).add(r);
        }

        return byMonthJobResults;
    }

    public void queueResultsToAdd(List<JobBuildResult> results) {
        queuedResultsToAdd.addAll(results);
    }

    public List<JobBuildResult> getJobBuildResults() {
        List<JobBuildResult> aggregatedList = new ArrayList<JobBuildResult>(this.persistedResults);
        aggregatedList.addAll(queuedResultsToAdd);
        return Collections.unmodifiableList(aggregatedList);
    }

    public boolean pendingChanges() {
        return !queuedResultsToAdd.isEmpty();
    }

    public void queueResultToAdd(JobBuildResult result) {
        queuedResultsToAdd.add(result);
    }

    public void applyQueuedResultsInFiles() {
        LOGGER.info("Starting persisting queueResultsToAdd.");
        // atomically move all the queued stuff into a local list
        List<JobBuildResult> resultsToAdd;

        synchronized (queuedResultsToAdd) {
            resultsToAdd = new ArrayList<JobBuildResult>(queuedResultsToAdd);
            queuedResultsToAdd.clear();
        }

        if (resultsToAdd.isEmpty()) {
            LOGGER.info("No change detected in job results update queue!");
            return;
        }

        File jobResultsRoot = getJobResultFolder();

        LOGGER.log(Level.INFO, "Try to write changes to file: " + jobResultsRoot.toString());

        if (!jobResultsRoot.exists()) {
            try {
                LOGGER.log(Level.INFO, "Root not exists try to create.");

                FileUtils.forceMkdir(jobResultsRoot);
            } catch (IOException e) {

                LOGGER.log(Level.INFO, "Not possible to create rootfolder: " + jobResultsRoot);

                throw new IllegalStateException("Can't create job results root directory : "
                    + jobResultsRoot.getAbsolutePath(), e);
            }
        }

        // Persisting everything
        addPersistedJobResults(resultsToAdd);

        List<String> updatedFilenamesList = new ArrayList<String>(toJobResultFilenameMap(resultsToAdd).keySet());

        Collection<String> updatedFilenames = CollectionsUtil.toSet(updatedFilenamesList);

        for (String filename : updatedFilenames) {
            String jobResultFilepath = jobResultsRoot + File.separator + filename;

            LOGGER.info("Writing jobResults to file: " + jobResultFilepath);

            try {
                FileWriter fw = new FileWriter(jobResultFilepath);
                Jenkins.XSTREAM.toXML(persistedMonthlyResults.get(filename), fw);
                fw.close();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Unable to serialize job results into " + jobResultFilepath, e);
                throw new IllegalStateException("Unable to serialize job results into " + jobResultFilepath, e);
            }
        }
        LOGGER.info("Finished persisting queueResultsToAdd.");
    }

    private static File getJobResultFolder() {
        return new File(Jenkins.getInstance().getRootDir().getAbsolutePath() + File.separator + IBS_ROOT_PATH);
    }

    public static List<JobBuildResult> load() {
        List<JobBuildResult> jobBuildResults = new ArrayList<JobBuildResult>();
        File jobResultsRoot = getJobResultFolder();

        LOGGER.log(Level.INFO, "jobResultsRoot: " + jobResultsRoot + " exists: " + jobResultsRoot.exists());

        if (jobResultsRoot.exists()) {// if not exists, we have nothing to load
            for (File f : jobResultsRoot.listFiles()) {
                try {
                    FileReader fr = new FileReader(f);
                    List<JobBuildResult> jobResultsInFile = (List<JobBuildResult>)Hudson.XSTREAM.fromXML(fr);
                    jobBuildResults.addAll(jobResultsInFile);
                    fr.close();
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Unable to read job results in " + f.getAbsolutePath(), e);
                    throw new IllegalStateException("Unable to read job results in " + f.getAbsolutePath(), e);
                }
            }
        }
        Collections.sort(jobBuildResults, new JobBuildResult.AntiChronologicalComparator());
        return jobBuildResults;
    }

    private synchronized void addPersistedJobResults(List<JobBuildResult> results) {
        persistedResults.addAll(results);
        Map<String, List<JobBuildResult>> filenameMap = toJobResultFilenameMap(results);
        CollectionsUtil.mapMergeAdd(persistedMonthlyResults, filenameMap);
    }
}
