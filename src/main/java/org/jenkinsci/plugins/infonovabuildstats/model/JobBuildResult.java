package org.jenkinsci.plugins.infonovabuildstats.model;

import java.util.Comparator;

/**
 * Represants the job build results.
 * If class member is added don't forget to adapt XSTREAM initialization in
 * {@link org.jenkinsci.plugins.infonovabuildstats.business.InfonovaBuildStatsPluginSaver InfonovaBuildStatsPluginSaver}
 * private method "initializeXStream".
 * 
 */
public class JobBuildResult {

    public static final String MASTER_NODE_NAME = "master";

    private String jobName;

    private String buildClass;

    private String buildId;

    private int buildNumber;

    private BuildResult result;

    /* Date when job was started */
    private long buildStartDate;

    /* Date when job was executed */
    private long buildExecuteDate;

    /* Date when job was completed */
    private long buildCompletedDate;

    /* build overall duration */
    private long duration = -1;

    private long queueDuration = -1;

    private String nodeLabel;

    private String nodeName;

    private String userName = null;

    public JobBuildResult(String _buildId, BuildResult _result, String _jobName, String _buildClass, int _buildNumber,
            long _buildStartDate, long _buildCompletedDate, long _buildExecuteDate, long _duration,
            long _queueDuration, String _nodeLabel, String _nodeName, String _userName) {

        this.buildId = _buildId;
        this.result = _result;
        this.jobName = _jobName;
        this.buildClass = _buildClass;
        this.buildNumber = _buildNumber;

        this.buildStartDate = _buildStartDate;
        this.buildExecuteDate = _buildExecuteDate;
        this.buildCompletedDate = _buildCompletedDate;

        this.duration = _duration;
        this.queueDuration = _queueDuration;

        this.nodeLabel = _nodeLabel;
        setNodeName(_nodeName);
        this.userName = _userName;
    }

    public long getBuildStartDate() {
        return this.buildStartDate;
    }

    public void setNodeName(String nodeName) {
        // @see {@link Node#getNodeName}
        if ("".equals(nodeName)) {
            this.nodeName = MASTER_NODE_NAME;
        } else {
            this.nodeName = nodeName;
        }
    }

    public static class AntiChronologicalComparator extends ChronologicalComparator {

        public int compare(JobBuildResult jbr1, JobBuildResult jbr2) {
            return super.compare(jbr1, jbr2) * -1;
        }
    }


    /*
     * the value 0 if the argument Date is equal to this Date; a value less than 0 if this Date is before the Date
     * argument; and a value greater than 0 if this Date is after the Date argument.
     */
    public static class ChronologicalComparator implements Comparator<JobBuildResult> {

        public int compare(JobBuildResult jbr1, JobBuildResult jbr2) {

            if (jbr1.buildStartDate == jbr2.buildStartDate) {
                return 0;
            } else if (jbr1.buildStartDate > jbr2.buildStartDate) {
                return 1;
            } else {
                return -1;
            }
        }
    }
}
