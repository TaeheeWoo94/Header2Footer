package com.codestates.Header2Footer.helper.email;

public class MockEmailSendable implements EmailSendable {
    @Override
    public void send(String message) {
        System.out.println("sent mock email!");
    }
}