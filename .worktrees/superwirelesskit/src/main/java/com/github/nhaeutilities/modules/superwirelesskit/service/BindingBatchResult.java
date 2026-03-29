package com.github.nhaeutilities.modules.superwirelesskit.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.github.nhaeutilities.modules.superwirelesskit.data.BindingRecord;

public final class BindingBatchResult {

    private final int successCount;
    private final List<BindingRecord> failedRecords;

    public BindingBatchResult(int successCount, List<BindingRecord> failedRecords) {
        this.successCount = successCount;
        this.failedRecords = Collections.unmodifiableList(new ArrayList<BindingRecord>(failedRecords));
    }

    public int getSuccessCount() {
        return successCount;
    }

    public int getFailureCount() {
        return failedRecords.size();
    }

    public List<BindingRecord> getFailedRecords() {
        return failedRecords;
    }
}
