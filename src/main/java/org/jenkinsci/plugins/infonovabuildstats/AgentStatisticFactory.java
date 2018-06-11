package org.jenkinsci.plugins.infonovabuildstats;

import hudson.model.Computer;
import hudson.model.Node;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.infonovabuildstats.model.AgentStatistic;
import org.jenkinsci.plugins.mesos.JenkinsScheduler;
import org.jenkinsci.plugins.mesos.MesosSlave;
import org.jenkinsci.plugins.mesos.scheduling.JenkinsSlave;
import org.jenkinsci.plugins.mesos.scheduling.Result;

import javax.annotation.CheckForNull;
import java.util.Date;


/**
 * Class produces {@link AgentStatistic} from given
 * AbstractBuild.
 */
public class AgentStatisticFactory {

    public static final AgentStatisticFactory INSTANCE = new AgentStatisticFactory();
    private final Jenkins jenkins;

    /**
     * @see hudson.security.ACL#SYSTEM
     */

    public AgentStatisticFactory() {
        jenkins = Jenkins.getInstance();
    }

    /**
     * This method extracts all neccesery information out of an {@link Computer} and creates a new
     * {@link AgentStatistic} out of them.
     *
     * @param node - The computer from which AgentStatistic is produced.
     * @return AgentStatistic - Produced AgentStatistic
     */
    @CheckForNull
    public AgentStatistic createAgentStatisticFrom(Node node) {

        String lastJobName = "N/A";
        String agentName = node.getDisplayName();
        String agentLabel = extractNodeLabels(node);
        String mesosAgent = "N/A";
        String framework = "N/A";

        int memory = 0;
        double cpus = 0.0;
        String principal = "";


        if (node instanceof MesosSlave) {
            MesosSlave mesosSlave = (MesosSlave) node;
            lastJobName = mesosSlave.getLinkedItem();

            principal = mesosSlave.getCloud().getPrincipal();

            JenkinsScheduler jenkinsScheduler = (JenkinsScheduler) mesosSlave.getMesosInstance().getScheduler();
            if (jenkinsScheduler != null) {
                Result result = jenkinsScheduler.getResult(agentName);

                if (result != null) {
                    JenkinsSlave jenkinsSlave = result.getSlave();
                    if (jenkinsSlave != null) {
                        cpus = jenkinsSlave.getCpus();
                        memory = (int)jenkinsSlave.getMem();
                        mesosAgent = result.getSlave().getHostname();
                    }
                }
            }
            framework = mesosSlave.getCloud().getFrameworkName();
        }

        //node online date
        Date onlineDate = new Date();
        return AgentStatistic.createOnOnlineAgentStatistic(agentName, agentLabel, lastJobName, onlineDate,
                mesosAgent, framework,
                principal, jenkins.getRootUrl(), memory, cpus);
    }

    /**
     * @param node The Node from which nodeLabel is extracted
     * @return String label of node without label "jobEnvProperties")
     */
    public static String extractNodeLabels(Node node) {
        if (node == null) {
            return "";
        } else {
            // first delete jobEnvProperties Label
            String label = node.getLabelString().replaceAll("jobEnvProperties", "");
            // second remove all spaces
            return label.replaceAll("\\s", "");
        }
    }
}
