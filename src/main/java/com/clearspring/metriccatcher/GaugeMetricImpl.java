package com.clearspring.metriccatcher;

import com.yammer.metrics.core.GaugeMetric;

public class GaugeMetricImpl extends GaugeMetric<Long> {
    long value;

    public GaugeMetricImpl() {
        value = 0;
    }

    @Override
    public Long value() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }
}