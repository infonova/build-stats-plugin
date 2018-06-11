package org.jenkinsci.plugins.infonovabuildstats;

import hudson.Extension;
import hudson.Plugin;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.model.listeners.ItemListener;
import hudson.slaves.ComputerListener;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.infonovabuildstats.business.InfonovaBuildStatsBusiness;
import org.jenkinsci.plugins.infonovabuildstats.model.AgentStatistic;
import org.jenkinsci.plugins.infonovabuildstats.model.JobBuildResultSharder;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Plugin for collecting agent online and offline statistics
 * 
 * */

public class InfonovaBuildStatsPlugin extends Plugin {

    private static final Logger LOGGER = Logger.getLogger(InfonovaBuildStatsPlugin.class.getName());

    transient private final InfonovaBuildStatsBusiness business = new InfonovaBuildStatsBusiness(this);

    private final JobBuildResultSharder jobBuildResultsSharder = new JobBuildResultSharder();

    /**
     * Highered visibility of load method
     */
    public void load() throws IOException {
        super.load();
    }

    /**
     * @return File the config file of plugin
     */

    public File getConfigXmlFile() {
        return getConfigXml().getFile();
    }

    /**
     * Used to work with the plugin singleton
     *
     * @return InfonovaBuildStatsPlugin - the actual loaded plugin from jenkins instance
     */

    public static InfonovaBuildStatsPlugin getInstance() {
        return Jenkins.get().getPlugin(InfonovaBuildStatsPlugin.class);
    }

    /**
     * @return InfonovaBuildStatsBusiness - the business layer of plugin
     */

    public static InfonovaBuildStatsBusiness getPluginBusiness() {
        return getInstance().business;
    }

    /**
     * @return An unmodifiable list of job build results
     */
    public List<AgentStatistic> getJobBuildResults() {
        return this.jobBuildResultsSharder.getJobBuildResults();
    }

    /**
     * @return JobBuildResultSharder
     */
    public JobBuildResultSharder getJobBuildResultsSharder() {
        return jobBuildResultsSharder;
    }

    /**
     * Inner class for loading the plugin, when jenkins instance is loaded
     */
    @Extension
    public static class InfonovaBuildStatsItemListener extends ItemListener {

        /**
         * Called from jenkins for (re-) loading plugin
         */
        @Override
        public void onLoaded() {
            super.onLoaded();

            LOGGER.log(Level.INFO, "InfonovaBuildStatsItemListener on Loaded - start");

            getPluginBusiness().reloadPlugin();

            LOGGER.log(Level.INFO, "InfonovaBuildStatsItemListener on Loaded - end");
        }
    }

    /**
     * Used to collect build stats data if job is completed.
     */
    @Extension
    public static class InfonovaBuildStatsComputerListener extends ComputerListener {

        @Inject
        private InfonovaBuildStatsConfig config;

        private final Map<String, AgentStatistic> agentStatisticMap = Collections.synchronizedMap(new HashMap<>());

        @Override
        public void onOnline(Computer c, TaskListener listener) {
            String displayName = c.getDisplayName();

            try {
                // Only collect job data if config is enabled
                if (config.isCollectBuildStats()) {
                    Node node = c.getNode();
                    if (node != null) {
                        AgentStatistic agentStatistic = AgentStatisticFactory.INSTANCE.createAgentStatisticFrom(node);
                        if (agentStatistic != null) {
                            agentStatisticMap.put(displayName, agentStatistic);
                        } else {
                            LOGGER.log(Level.WARNING, "Unable to create agentStatistic for " + displayName + ", factory returned a null object");
                        }
                    } else {
                        LOGGER.log(Level.WARNING, "Unable to collect node for " + displayName + ", node is null");
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error while creating AgentStatistic", e);
            }
        }

        @Override
        public void onOffline(Computer c) {
            String displayName = c.getDisplayName();

            try {
                // Only collect job data if config is enabled
                if (config.isCollectBuildStats()) {
                    AgentStatistic agentStatistic = agentStatisticMap.get(displayName);
                    if (agentStatistic != null) {
                        getPluginBusiness().onComputerOffline(AgentStatistic.createOnOfflineAgentStatistic(agentStatistic, new Date()));
                    } else {
                        LOGGER.log(Level.WARNING, "Unable to find node for " + displayName);
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error while processing offline computer " + displayName, e);
            } finally {
                agentStatisticMap.remove(displayName);
            }
        }
    }
}
