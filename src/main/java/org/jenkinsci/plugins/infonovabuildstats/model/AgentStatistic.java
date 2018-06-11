package org.jenkinsci.plugins.infonovabuildstats.model;

import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Date;

/**
 * Represants the job build results.
 * If class member is added don't forget to adapt XSTREAM initialization in
 * {@link org.jenkinsci.plugins.infonovabuildstats.business.InfonovaBuildStatsPluginSaver InfonovaBuildStatsPluginSaver}
 * private method "initializeXStream".
 */
public class AgentStatistic {

    public static final String MASTER_NODE_NAME = "master";

    final private String lastJobName;
    final private String agentLabel;
    final private String agentName;

    /* Date when agent has become online */
    final private Date onlineDate;

    /* Date when agent went offline */
    final private Date offlineDate;

    /* Agent online time in millis  */
    final private long onlineTimeMillis;

    final private String mesosAgent;
    final private int memory;
    final private double cpus;
    final private String principal;
    final private String framework;
    final private String project;
    final private String jenkinsUrl;

    public static AgentStatistic createOnOnlineAgentStatistic(String agentName, String agentLabel, String lastJobName,
                                                              Date onlineDate, String mesosAgent,
                                                              String framework, String principal, String jenkinsUrl,
                                                              int memory, double cpus) {
        return new AgentStatistic(agentName, agentLabel, lastJobName,
                onlineDate, null, mesosAgent,
                framework, principal, jenkinsUrl, memory, cpus);
    }

    public static AgentStatistic createOnOfflineAgentStatistic(AgentStatistic oldAgentStatistic, Date offlineDate) {
        return new AgentStatistic(oldAgentStatistic.getAgentName(),
                oldAgentStatistic.getAgentLabel(),
                oldAgentStatistic.getLastJobName(),
                oldAgentStatistic.getOnlineDate(),
                offlineDate,
                oldAgentStatistic.getMesosAgent(),
                oldAgentStatistic.getFramework(),
                oldAgentStatistic.getPrincipal(),
                oldAgentStatistic.getJenkinsUrl(),
                oldAgentStatistic.getMemory(),
                oldAgentStatistic.getCpus());
    }

    private AgentStatistic(String agentName, String agentLabel, String lastJobName,
                           Date onlineDate, Date offlineDate, String mesosAgent,
                           String framework, String principal, String jenkinsUrl,
                           int memory, double cpus) {
        this.lastJobName = lastJobName;

        this.onlineDate = onlineDate;
        this.offlineDate = offlineDate;

        this.agentLabel = agentLabel;
        this.agentName = StringUtils.defaultIfBlank(agentName, MASTER_NODE_NAME);

        this.cpus = cpus;
        this.memory = memory;

        this.framework = framework;
        this.principal = principal;
        this.mesosAgent = mesosAgent;

        this.project = StringUtils.defaultIfBlank(exctractProjectName(lastJobName), principal);

        this.jenkinsUrl = jenkinsUrl;

        this.onlineTimeMillis = calculateOnlineTimeMillis();
    }

    public String getLastJobName() {
        return lastJobName;
    }

    public String getAgentLabel() {
        return agentLabel;
    }

    public String getAgentName() {
        return agentName;
    }

    public Date getOnlineDate() {
        return onlineDate != null ? new Date(onlineDate.getTime()) : null;
    }

    public long getOnlineTimeMillis() {
        return onlineTimeMillis;
    }

    public String getMesosAgent() {
        return mesosAgent;
    }

    public int getMemory() {
        return memory;
    }

    public double getCpus() {
        return cpus;
    }

    public String getPrincipal() {
        return principal;
    }

    public String getFramework() {
        return framework;
    }

    public String getProject() {
        return project;
    }

    public String getJenkinsUrl() {
        return jenkinsUrl;
    }

    private String exctractProjectName(String lastJobName) {
        String projectName = "";
        if (!StringUtils.isBlank(lastJobName) && !StringUtils.equals("N/A", lastJobName)) {
            String[] splittedJobName = lastJobName.split("/");
            if (splittedJobName.length > 1) {
                projectName = splittedJobName[0]; //Foldername is is the project
            }
        }
        return projectName;
    }


    private long calculateOnlineTimeMillis() {
        if (offlineDate != null && onlineDate != null) {
            return offlineDate.getTime() - onlineDate.getTime();
        }
        return onlineTimeMillis;
    }

    public static class AntiChronologicalComparator extends ChronologicalComparator implements Serializable {



        public int compare(AgentStatistic jbr1, AgentStatistic jbr2) {
            return super.compare(jbr1, jbr2) * -1;
        }
    }

    /*
     * the value 0 if the argument Date is equal to this Date; a value less than 0 if this Date is before the Date
     * argument; and a value greater than 0 if this Date is after the Date argument.
     */
    public static class ChronologicalComparator implements Comparator<AgentStatistic>, Serializable {
        public int compare(AgentStatistic jbr1, AgentStatistic jbr2) {
            return jbr1.offlineDate.compareTo(jbr2.offlineDate);
        }
    }

    public Date getOfflineDate() {
        return offlineDate != null ? new Date(offlineDate.getTime()) : null;
    }
}
