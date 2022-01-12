package com.hartwig.platinum.kubernetes;

import com.hartwig.platinum.config.NodePoolConfiguration;
import com.hartwig.platinum.config.SampleConfiguration;

public interface TargetNodePool {

    boolean isDefault();

    String name();

    String machineType();

    int numNodes();

    static TargetNodePool fromConfig(final NodePoolConfiguration configuration, final int samples) {
        return new TargetNodePool() {
            @Override
            public boolean isDefault() {
                return false;
            }

            @Override
            public String name() {
                return configuration.name();
            }

            @Override
            public String machineType() {
                return "e2-medium";
            }

            @Override
            public int numNodes() {
                return Math.max(1, samples / 10);
            }
        };
    }

    static TargetNodePool defaultPool() {
        return new TargetNodePool() {
            @Override
            public boolean isDefault() {
                return true;
            }

            @Override
            public String name() {
                throw new UnsupportedOperationException();
            }

            @Override
            public String machineType() {
                throw new UnsupportedOperationException();
            }

            @Override
            public int numNodes() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
