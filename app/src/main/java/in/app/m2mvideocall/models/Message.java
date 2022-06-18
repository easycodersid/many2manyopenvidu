package in.app.m2mvideocall.models;

public class Message {
    private String message, typeOfMessage, from, messageType;

    public Message(String message, String typeOfMessage, String from, String messageType) {
        this.message = message;
        this.from = from;
        this.typeOfMessage = typeOfMessage;
        this.messageType = messageType;
    }

    public String getMessageType() {
        return messageType;
    }

    public String getTypeOfMessage() {
        return typeOfMessage;
    }

    public String getMessage() {
        return message;
    }

    public String getFrom() {
        return from;
    }
}
