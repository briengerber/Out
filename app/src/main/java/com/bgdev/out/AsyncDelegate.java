package com.bgdev.out;

import com.bgdev.out.backend.conversationApi.model.IndMessage;

import java.util.List;

/**
 * Created by Brien on 5/9/2015.
 */
public interface AsyncDelegate {
    void asyncComplete(boolean success);
    void asyncCompleteMessage(boolean success, List<IndMessage> list);
}
