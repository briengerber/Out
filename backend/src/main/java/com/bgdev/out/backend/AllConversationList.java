package com.bgdev.out.backend;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Brien on 5/25/2015.
 */
public class AllConversationList {

    private List<IndConvListObject> messageList = new ArrayList<>();

    public AllConversationList(){

    }

    public List<IndConvListObject> getMessageList() {
        return messageList;
    }

    public void addToMessageList(Long id, List<Conversation.IndMessage> list){
        messageList.add(new IndConvListObject(id,list));
    }

    public static class IndConvListObject{
        private Long conversationid;
        private List<Conversation.IndMessage> listOfIndMessages;

        public IndConvListObject(Long convId,List<Conversation.IndMessage> list){
            conversationid = convId;
            listOfIndMessages=list;
        }

        public Long getConversationid() {
            return conversationid;
        }

        public List<Conversation.IndMessage> getListOfIndMessages() {
            return listOfIndMessages;
        }
    }

}
