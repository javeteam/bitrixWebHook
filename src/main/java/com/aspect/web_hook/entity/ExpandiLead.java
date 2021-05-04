package com.aspect.web_hook.entity;

public class ExpandiLead {
    private Contact contact = new Contact();
    private Messenger messenger = new Messenger();

    public Contact getContact() {
        return contact;
    }

    public void setContact(Contact contact) {
        this.contact = contact;
    }

    public Messenger getMessenger() {
        return messenger;
    }

    public void setMessenger(Messenger messenger) {
        this.messenger = messenger;
    }
}
