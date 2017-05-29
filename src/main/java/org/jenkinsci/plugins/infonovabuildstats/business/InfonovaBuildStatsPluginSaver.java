package org.jenkinsci.plugins.infonovabuildstats.business;

import com.thoughtworks.xstream.converters.basic.DateConverter;
import hudson.BulkChange;
import hudson.util.DaemonThreadFactory;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.infonovabuildstats.InfonovaBuildStatsPlugin;
import org.jenkinsci.plugins.infonovabuildstats.model.AgentStatistic;
import org.jenkinsci.plugins.infonovabuildstats.xstream.InfonovaBuildStatsXStreamConverter;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class which is used for saving plugin state and builds.
 * Contains also the initialization of the XSTREAM config (used to write build stats to xml file)
 * and the registration of the XSTREAM converter see private method initializeXStream().
 *
 */
public class InfonovaBuildStatsPluginSaver {

    private static final Logger LOGGER = Logger.getLogger(InfonovaBuildStatsPluginSaver.class.getName());

    private InfonovaBuildStatsPlugin plugin;

    /**
     * Use of a size 1 thread pool frees us from worring about
     * accidental thread death.
     */
    final ExecutorService writer = Executors.newFixedThreadPool(1, new DaemonThreadFactory());

    public InfonovaBuildStatsPluginSaver(InfonovaBuildStatsPlugin plugin) {
        this.plugin = plugin;

        // call to initialization of XSTREAM
        this.initializeXStream();
    }

    /**
     *
     */
    private void initializeXStream() {
        // registers the InfonovaBuildStatsXStreamConverter with XSTREAM
        Jenkins.XSTREAM.registerConverter(new InfonovaBuildStatsXStreamConverter());

        /*For the correct format of our dates we have to use a LocalConverter for the date fields*/
        DateConverter dateConverter = new DateConverter(null, "yyyy-MM-dd HH:mm:ss.SSS", null,
        		Locale.getDefault(), TimeZone.getDefault(), true);
        Jenkins.XSTREAM.registerLocalConverter(AgentStatistic.class,
        		"onlineDate", dateConverter);
        Jenkins.XSTREAM.registerLocalConverter(AgentStatistic.class,
        		"offlineDate", dateConverter);

        // XStream compacting aliases...
        Jenkins.XSTREAM.alias(InfonovaBuildStatsXStreamConverter.JOB_BUILD_RESULT_CLASS_ALIAS, AgentStatistic.class);
        Jenkins.XSTREAM.alias("agent-list", java.util.List.class);

        Jenkins.XSTREAM.aliasField("lastJobName", AgentStatistic.class, "lastJobName");
        Jenkins.XSTREAM.aliasField("agentLabel", AgentStatistic.class, "agentLabel");
        Jenkins.XSTREAM.aliasField("agentName", AgentStatistic.class, "agentName");
        Jenkins.XSTREAM.aliasField("onlineDate", AgentStatistic.class, "onlineDate");
        Jenkins.XSTREAM.aliasField("offlineDate", AgentStatistic.class, "offlineDate");
        Jenkins.XSTREAM.aliasField("onlineTimeMillis", AgentStatistic.class, "onlineTimeMillis");
        Jenkins.XSTREAM.aliasField("mesosAgent", AgentStatistic.class, "mesosAgent");
        Jenkins.XSTREAM.aliasField("memory", AgentStatistic.class, "memory");
        Jenkins.XSTREAM.aliasField("cpus", AgentStatistic.class, "cpus");
        Jenkins.XSTREAM.aliasField("principal", AgentStatistic.class, "principal");
        Jenkins.XSTREAM.aliasField("framework", AgentStatistic.class, "framework");
        Jenkins.XSTREAM.aliasField("project", AgentStatistic.class, "project");
        Jenkins.XSTREAM.aliasField("jenkinsUrl", AgentStatistic.class, "jenkinsUrl");
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

        LOGGER.log(Level.FINER, "Infonova build stats - updatePlugin!");

        writer.submit(new Runnable() {

            public void run() {

                // Persist everything
                try {

                    if (!plugin.getJobBuildResultsSharder().pendingChanges()) {

                        LOGGER.log(Level.FINER, "No change detected in update queue no update required !");

                        return;
                    }

                    LOGGER.log(Level.FINER, "We have changes, save plugin");

                    if (BulkChange.contains(plugin)) {
                        LOGGER.log(Level.FINER, "...but bulkChange contains plugins...");
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
