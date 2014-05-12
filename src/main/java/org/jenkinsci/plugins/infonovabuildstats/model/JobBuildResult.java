package org.jenkinsci.plugins.infonovabuildstats.model;

import java.util.Calendar;
import java.util.Comparator;


public class JobBuildResult {

    public static final String MASTER_NODE_NAME = "master";

    private String buildId;

    private BuildResult result;

    private String jobName;

    private int buildNumber;

    /* Date when job was started */
    private Calendar buildStartDate;

    /* Date when job was executed */
    private Calendar buildExecuteDate;

    /* Date when job was completed */
    private Calendar buildCompletedDate;

    /* build overall duration */
    private long overallDuration = -1;

    /* build execution duration */
    private long executionDuration = -1;

    private String nodeLabel;

    private String nodeName;

    private String userName = null;

    public JobBuildResult(String _buildId, BuildResult _result, String _jobName, int _buildNumber,
            Calendar _buildStartDate, Calendar _buildCompletedDate, Calendar _buildExecuteDate, long _overallDuration,
            long _executionDuration, String _nodeLabel, String _nodeName, String _userName) {

        this.buildId = _buildId;
        this.result = _result;
        this.jobName = _jobName;
        this.buildNumber = _buildNumber;

        this.buildStartDate = (Calendar)_buildStartDate.clone();
        this.buildExecuteDate = (Calendar)_buildExecuteDate.clone();
        this.buildCompletedDate = (Calendar)_buildCompletedDate.clone();

        this.overallDuration = _overallDuration;
        this.executionDuration = _executionDuration;

        this.nodeLabel = _nodeLabel;
        setNodeName(_nodeName);
        this.userName = _userName;
    }

    public Calendar getBuildStartDate() {
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

    public static class ChronologicalComparator implements Comparator<JobBuildResult> {

        public int compare(JobBuildResult jbr1, JobBuildResult jbr2) {
            return jbr1.buildStartDate.compareTo(jbr2.buildStartDate);
        }
    }
}
