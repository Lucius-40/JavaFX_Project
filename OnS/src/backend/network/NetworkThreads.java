package backend.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.util.concurrent.BlockingQueue;

class NetworkReader extends Thread{
    private NetworkClient client;
    private ObjectInputStream ois;
    private boolean running = false;

    public NetworkReader(NetworkClient networkClient, ObjectInputStream ois) {
        this.client = networkClient;
        this.ois = ois;
        this.setName("NetworkClient-Reader");
        this.setDaemon(true);
    }

    @Override
    public void run(){
        this.running = true;
        System.out.println("Reader thread started");

        try {
            Object object;
            while (running && client.isConnected() && (object = this.ois.readObject()) != null) {
                if(object.getClass().getName().endsWith("Message")){
                    try {
                        java.lang.reflect.Method getTypedMethod = object.getClass().getMethod("getType");
                        java.lang.reflect.Method getDataMethod = object.getClass().getMethod("getData");

                        String type = (String) getTypedMethod.invoke(object);
                        Object data = getDataMethod.invoke(object);

                        Message clientMsg = new Message(type, data);
                        // System.out.println("DEBUG: Received message: type=" + type + ", data=" + data);
                        client.processServerMessage(clientMsg);
                    } catch (Exception e) {
                        System.out.println("Error processing server message: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            if(running && client.isConnected()){
                System.out.println("from the server: " + e.getMessage());
                client.disconnect();
            }
        }

        System.out.println("Network reader thread stopped");
    }

    public void stopReading() {
        this.running = false;
        this.interrupt();
    }
}

class NetworkWriter extends Thread{
    private NetworkClient client;
    private ObjectOutputStream oos;
    private boolean running = false;
    private BlockingQueue<Message> outgoingMessages;

    public NetworkWriter(NetworkClient networkClient, ObjectOutputStream oos, BlockingQueue<Message> messages) {
        this.client = networkClient;
        this.oos = oos;
        this.outgoingMessages = messages;
        this.setName("Network-Writer");
        this.setDaemon(true);
    }

    @Override
    public void run(){
        this.running = true;
        System.out.println("Writer thread initiated");

        try {
            while (running && this.client.isConnected()) {
                Message messageToSend = outgoingMessages.take();

                if(running && client.isConnected()){
                    // System.out.println("DEBUG: Sending message: type=" + messageToSend.getType() + ", data=" + messageToSend.getData());
                    System.out.println("Sending to server of message: " + messageToSend.getType() + " and of data: " + messageToSend.getData());
                    oos.writeObject(messageToSend);
                    oos.flush();
                    oos.reset(); 
                    System.out.println("Message sent to the server: " + messageToSend.getData());
                }
            }
        } catch (InterruptedException e) {
            System.out.println("NetworkWriter thread interrupted");
            Thread.currentThread().interrupt();
        } catch (IOException e){
            if (running && client.isConnected()) {
                System.out.println("Error sending messages to the server: " + e.getMessage());
                e.printStackTrace();  
                this.client.disconnect();
            }
        }
    }

    public void stopWriting() {
        this.running = false;
        this.interrupt();
    }
}
