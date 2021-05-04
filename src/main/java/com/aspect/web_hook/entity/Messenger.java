package com.aspect.web_hook.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Messenger {
    private long id;
    @JsonProperty("contact_status")
    private String contactStatus;
    @JsonProperty("conversation_status")
    private String conversationStatus;
    private String message;
    @JsonProperty("last_received_message")
    private String lastReceivedMessage;
    @JsonProperty("last_received_messages")
    private List<String> lastReceivedMessages;
    @JsonProperty("campaign_instance")
    private String campaignInstance;
    private Placeholders placeholders = new Placeholders();


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getContactStatus() {
        return contactStatus;
    }

    public void setContactStatus(String contactStatus) {
        this.contactStatus = contactStatus;
    }

    public String getConversationStatus() {
        return conversationStatus;
    }

    public void setConversationStatus(String conversationStatus) {
        this.conversationStatus = conversationStatus;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getLastReceivedMessage() {
        return lastReceivedMessage;
    }

    public void setLastReceivedMessage(String lastReceivedMessage) {
        this.lastReceivedMessage = lastReceivedMessage;
    }

    public List<String> getLastReceivedMessages() {
        return lastReceivedMessages;
    }

    public void setLastReceivedMessages(List<String> lastReceivedMessages) {
        this.lastReceivedMessages = lastReceivedMessages;
    }

    public String getCampaignInstance() {
        return campaignInstance;
    }

    public void setCampaignInstance(String campaignInstance) {
        this.campaignInstance = campaignInstance;
    }

    public Placeholders getPlaceholders() {
        return placeholders;
    }

    public void setPlaceholders(Placeholders placeholders) {
        this.placeholders = placeholders;
    }
}
