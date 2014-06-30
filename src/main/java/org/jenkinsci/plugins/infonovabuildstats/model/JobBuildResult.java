package org.jenkinsci.plugins.infonovabuildstats.model;

import java.util.Comparator;
import java.util.Date;

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

    private long buildId;

    private int buildNumber;

    private BuildResult result;

    /* Date when job was started */
    private Date buildStartDate;

    /* Date when job was executed */
    private Date buildExecuteDate;

    /* Date when job was completed */
    private Date buildCompletedDate;

    /* build overall duration */
    private long duration = -1;

    private long queueDuration = -1;

    private String nodeLabel;

    private String nodeName;

    private String userName = null;

    public JobBuildResult(BuildResult _result, String _jobName, String _buildClass, int _buildNumber,
    		Date _buildStartDate, Date _buildCompletedDate, Date _buildExecuteDate, long _duration,
            long _queueDuration, String _nodeLabel, String _nodeName, String _userName) {

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

    	this.buildId = this.getGeneratedId();
    }

    public Date getBuildStartDate() {
        return this.buildStartDate;
    }

    public Date getBuildCompletedDate() {
        return this.buildCompletedDate;
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

            if (jbr1.buildCompletedDate.compareTo(jbr2.buildCompletedDate) == 0) {
                return 0;
            } else if (jbr1.buildCompletedDate.compareTo(jbr2.buildCompletedDate) > 0) {
                return 1;
            } else {// jbr1 < jbr2 (jbr1 is before jbr2)
                return -1;
            }
        }
    }

    /**
     * Generates an unique ID by adding hashCodes of buildCompletedDate, jobname and
     * the buildNumber of this jobBuildResult.
     *
     * @return hashCode The hashCode of this jobBuildResult
     */

    public long getGeneratedId(){

    	/*For the hashValue of the jobName and the jobCompletedDate we need to check if the
    	 * hashValue is positiv --> otherwise make it positiv by multiplying with "-1"*/
    	long jobNameHash = (this.jobName.hashCode() < 0)
    			? this.jobName.hashCode() * (-1)  : this.jobName.hashCode();

    	long jobCompletedDateHash = (this.buildCompletedDate.hashCode() < 0)
    			? this.buildCompletedDate.hashCode() * (-1)  : this.buildCompletedDate.hashCode();

    	return jobNameHash + jobCompletedDateHash + this.buildNumber;
    }
}
