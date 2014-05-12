package org.jenkinsci.plugins.infonovabuildstats.business;

import hudson.BulkChange;
import hudson.util.DaemonThreadFactory;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import jenkins.model.Jenkins;

import org.jenkinsci.plugins.infonovabuildstats.InfonovaBuildStatsPlugin;
import org.jenkinsci.plugins.infonovabuildstats.model.JobBuildResult;
import org.jenkinsci.plugins.infonovabuildstats.xstream.InfonovaBuildStatsXStreamConverter;


public class InfonovaBuildStatsPluginSaver {

    private static final Logger LOGGER = Logger.getLogger(InfonovaBuildStatsPluginSaver.class.getName());

    private InfonovaBuildStatsPlugin plugin;

    /**
     * See
     * {@link #updatePlugin(hudson.plugins.global_build_stats.business.GlobalBuildStatsPluginSaver.BeforeSavePluginCallback)}
     * Use of a size 1 thread pool frees us from worring about accidental thread death.
     */
    final ExecutorService writer = Executors.newFixedThreadPool(1, new DaemonThreadFactory());

    public InfonovaBuildStatsPluginSaver(InfonovaBuildStatsPlugin plugin) {
        this.plugin = plugin;

        this.initializeXStream();
    }

    private void initializeXStream() {
        Jenkins.XSTREAM.registerConverter(new InfonovaBuildStatsXStreamConverter());

        // XStream compacting aliases...
        Jenkins.XSTREAM.alias(InfonovaBuildStatsXStreamConverter.JOB_BUILD_RESULT_CLASS_ALIAS, JobBuildResult.class);

        Jenkins.XSTREAM.aliasField("name", JobBuildResult.class, "jobName");
        Jenkins.XSTREAM.aliasField("number", JobBuildResult.class, "buildNumber");
        Jenkins.XSTREAM.aliasField("id", JobBuildResult.class, "buildId");
        Jenkins.XSTREAM.aliasField("result", JobBuildResult.class, "result");
        Jenkins.XSTREAM.aliasField("startDate", JobBuildResult.class, "buildStartDate");
        Jenkins.XSTREAM.aliasField("executionDate", JobBuildResult.class, "buildExecuteDate");
        Jenkins.XSTREAM.aliasField("completionDate", JobBuildResult.class, "buildCompletedDate");
        Jenkins.XSTREAM.aliasField("overallDuration", JobBuildResult.class, "overallDuration");
        Jenkins.XSTREAM.aliasField("executionDuration", JobBuildResult.class, "executionDuration");
        Jenkins.XSTREAM.aliasField("nodeLabel", JobBuildResult.class, "nodeLabel");
        Jenkins.XSTREAM.aliasField("nodeName", JobBuildResult.class, "nodeName");
        Jenkins.XSTREAM.aliasField("userName", JobBuildResult.class, "userName");
    }

    public static abstract class BeforeSavePluginCallback {

        public abstract void changePluginStateBeforeSavingIt(InfonovaBuildStatsPlugin plugin);

        public void afterPluginSaved() {
        }
    }

    public void reloadPlugin() {
        try {
            this.plugin.load();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        } catch (NullPointerException x) {
            File f = plugin.getConfigXmlFile();
            File bak = new File(f.getParentFile(), f.getName() + ".bak");
            if (!f.renameTo(bak)) {
                LOGGER.log(Level.WARNING, "failed to rename {0} to {1}", new Object[] { f, bak });
            }
            LOGGER.log(Level.WARNING, "JENKINS-17248 load failure; saving problematic file to " + bak, x);
        }
    }

    /**
     * Single entry point to persist information on GlobalBuildStatsPlugin
     * As the number of builds grow, the time it takes to execute "plugin.save()" become
     * non-trivial, up to the order of minutes or more. So to prevent this from blocking executor threads
     * that execute this callback, we use {@linkplain #writer a separate thread} to asynchronously persist
     * them to the disk.
     * 
     * @param callback
     */
    public void updatePlugin(BeforeSavePluginCallback callback) {

        callback.changePluginStateBeforeSavingIt(plugin);

        LOGGER.log(Level.INFO, "Infonova build stats - updatePlugin!");

        writer.submit(new Runnable() {

            public void run() {

                // Persist everything
                try {

                    if (!plugin.getJobBuildResultsSharder().pendingChanges()) {

                        LOGGER.log(Level.INFO, "No change detected in update queue no update required !");

                        return;
                    }

                    LOGGER.log(Level.INFO, "We have changes, save plugin");

                    if (BulkChange.contains(plugin)) {
                        LOGGER.log(Level.INFO, "...but bulkChange contains plugins...");
                    }

                    plugin.save();

                    LOGGER.log(Level.FINER, "Changes applied and file saved !");
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Failed to persist global build stat records", e);
                }
            }
        });
    }
}
