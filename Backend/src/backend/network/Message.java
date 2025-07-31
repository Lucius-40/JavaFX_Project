package backend.network;

import java.io.Serializable;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String type;
    private Object data;  
    private long timestamp;

    public Message(String type, Object data) {
        this.type = type;
        this.data = data; 
        this.timestamp = System.currentTimeMillis();
    }

    public String getType() {
        return this.type;
    } 

    public Object getData() {
        return this.data; 
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    @Override
    public String toString() {
        return "Message{type='" + type + "', data=" + data + ", timestamp=" + timestamp + "}";
    }
}