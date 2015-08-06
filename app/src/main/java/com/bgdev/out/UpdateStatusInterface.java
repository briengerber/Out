package com.bgdev.out;


import com.bgdev.out.backend.statusUpRecordApi.model.StatusUpRecord;

import java.util.List;


public interface UpdateStatusInterface {
    void update(List<StatusUpRecord> list);
    void updateFromScroll(List<StatusUpRecord> list);
}
