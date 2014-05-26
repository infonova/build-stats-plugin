package org.jenkinsci.plugins.infonovabuildstats.model;

import org.jenkinsci.plugins.infonovabuildstats.Messages;

/**
 * Class holds the different build results and links them to generated messages file.
 * 
 */
public enum BuildResult {

    SUCCESS((short)1) {

        @Override
        public String getLabel() {
            return Messages.Build_Results_Statuses_SUCCESS();
        }
    },
    FAILURE((short)2) {

        @Override
        public String getLabel() {
            return Messages.Build_Results_Statuses_FAILURES();
        }
    },
    UNSTABLE((short)4) {

        @Override
        public String getLabel() {
            return Messages.Build_Results_Statuses_UNSTABLES();
        }
    },
    ABORTED((short)8) {

        @Override
        public String getLabel() {
            return Messages.Build_Results_Statuses_ABORTED();
        }
    },
    NOT_BUILD((short)16) {

        @Override
        public String getLabel() {
            return Messages.Build_Results_Statuses_NOT_BUILD();
        }
    };

    public transient short code;

    private BuildResult(short _code) {
        this.code = _code;
    }

    public abstract String getLabel();

}
