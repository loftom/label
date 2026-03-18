package com.medlabel.dto;

import java.util.Map;

public class StatsResponse {
    private int total;
    private Map<String, Integer> byLabel;
    private Map<String, Integer> byAnnotator;

    public StatsResponse(int total, Map<String, Integer> byLabel, Map<String, Integer> byAnnotator) {
        this.total = total;
        this.byLabel = byLabel;
        this.byAnnotator = byAnnotator;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public Map<String, Integer> getByLabel() {
        return byLabel;
    }

    public void setByLabel(Map<String, Integer> byLabel) {
        this.byLabel = byLabel;
    }

    public Map<String, Integer> getByAnnotator() {
        return byAnnotator;
    }

    public void setByAnnotator(Map<String, Integer> byAnnotator) {
        this.byAnnotator = byAnnotator;
    }
}
