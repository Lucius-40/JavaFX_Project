package backend.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import backend.models.Product;

public class NetworkClient {
    private static NetworkClient instance;
    private Socket socket;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private boolean conneced = false;
    private List<Product> cachedInventory = new ArrayList<>();
    private InternalInventoryUpdateListener updateListener;
    
    private NetworkReader readerThread;
    private NetworkWriter writerThread;

    private BlockingQueue<Message> outgoingMessages = new LinkedBlockingQueue<Message>();

    private NetworkClient(){};

    public static synchronized NetworkClient getInstance(){
        if(instance == null){
            instance = new NetworkClient();
        }

        return instance;
    }

    public boolean connect(String host, int port){
        try {
            this.socket = new Socket(host, port);

            oos = new ObjectOutputStream(this.socket.getOutputStream());
            oos.flush();
            ois = new ObjectInputStream(this.socket.getInputStream());

            this.conneced = true;
            System.out.println("Successfully connected to the server at host: " + host + " and port: " + port);

            startThreads();
            return true;
        } catch (IOException e) {
            System.out.println("Failed to connect to the server at host: " + host + " and port: " + port + ". " + e.getMessage());
            return false;
        }
    }

    private void startThreads() {
        readerThread = new NetworkReader(this, ois);
        readerThread.start();
        
        writerThread = new NetworkWriter(this, oos, outgoingMessages);
        writerThread.start();
        
        System.out.println("Reader and writer thread started");
    }

    public void disconnect() {
        this.conneced = false;
        this.outgoingMessages.clear();

        if(readerThread != null){
            readerThread.stopReading();
        }

        if(writerThread != null){
            writerThread.stopWriting();
        }
        try {
            if(oos != null){
                oos.close();
            }
            if(ois != null){
                ois.close();
            }
            if(socket != null && !socket.isClosed()){
                socket.close();
            }
            System.out.println("Successfully disconnected the client");
        } catch (Exception e) {
            System.out.println("Error disconnecting the client");
            e.printStackTrace();
        }
    }

    //  Add this interface to NetworkClient
    public interface UserDataListener {
        void onUserDataReceived(Map<String, String> userData);
        void onUserDataError(String error);
    }

    //   Add this interface for authentication
    public interface AuthListener {
        void onLoginSuccess(Map<String, Object> userData);
        void onLoginFailed(Map<String, String> error);
        void onRegisterSuccess(Map<String, Object> response);
        void onRegisterFailed(Map<String, Object> error);
        void onAuthRequired();
    }

    //   Add interface for purchase responses
    public interface PurchaseListener {
        void onPurchaseSuccess(Map<String, Object> response);
        void onPurchaseFailure(Map<String, Object> error);
    }

    public void processServerMessage(Object inputObject) {
        try {
            Class<?> messageClass = inputObject.getClass();
            java.lang.reflect.Method getTypeMethod = messageClass.getMethod("getType");
            java.lang.reflect.Method getDataMethod = messageClass.getMethod("getData");
            
            String type = (String) getTypeMethod.invoke(inputObject);
            Object data = getDataMethod.invoke(inputObject);
            
            System.out.println("Received message: " + type + " with data type: " + 
                              (data != null ? data.getClass().getName() : "null"));
            
            
            switch (type) {
                case "INVENTORY_COUNT":
                    if(data instanceof Integer){
                        int totalProducts = (Integer) data;
                        System.out.println("Expecting: " + totalProducts + " products");
                        this.cachedInventory.clear(); 
                    }
                    break;
                    
                case "INVENTORY_CHUNK":
                    if(data instanceof List){
                        List<?> chunk = (List<?>) data;
                        
                        
                        for(Object serverProduct : chunk) {
                            Product clientProduct = convertServerProductToClient(serverProduct);
                            if(clientProduct != null) {
                                cachedInventory.add(clientProduct);
                                // System.out.println("-> Converted: " + clientProduct); 
                            } else {
                                System.out.println("Failed to convert server product: " + serverProduct);
                            }
                        }
                        
                        System.out.println("Received chunk: " + chunk.size() + " products. Total so far: " + cachedInventory.size());                   
                    }
                    break;
                    
                case "INVENTORY_COMPLETE":
                    System.out.println("Inventory loading complete. Total products: " + cachedInventory.size());

                    if (this.updateListener != null) {
                        updateListener.onInventoryUpdated(new ArrayList<>(cachedInventory));
                    }
                    break;
                    
                case "INVENTORY_UPDATE":
                    if (data instanceof List) {
                        updateInventory((List<?>) data);
                    }
                    break;
                    
                case "PURCHASE_CONFIRMED":
                    System.out.println("  Purchase confirmed successfully: " + data);
                    if (purchaseListener != null && data instanceof Map) {
                        purchaseListener.onPurchaseSuccess((Map<String, Object>) data);
                    }
                    break;
                
                case "PURCHASE_FAILED":
                    System.out.println("Purchase failed: " + data);
                    if (purchaseListener != null && data instanceof Map) {
                        purchaseListener.onPurchaseFailure((Map<String, Object>) data);
                    }
                    break;
                
                case "PONG":
                    System.out.println("Server responded to ping");
                    break;

                //   Add new cases for authentication and user data
                case "LOGIN_SUCCESS":
                    if (authListener != null && data instanceof Map) {
                        authListener.onLoginSuccess((Map<String, Object>) data);
                    }
                    break;
                    
                case "LOGIN_FAILED":
                    if (authListener != null && data instanceof Map) {
                        authListener.onLoginFailed((Map<String, String>) data);
                    }
                    break;
                    
                case "REGISTER_SUCCESS":
                    if (authListener != null && data instanceof Map) {
                        authListener.onRegisterSuccess((Map<String, Object>) data);
                    }
                    break;
                    
                case "REGISTER_FAILED":
                    if (authListener != null && data instanceof Map) {
                        authListener.onRegisterFailed((Map<String, Object>) data);
                    }
                    break;
                    
                case "USER_DATA_RESPONSE":
                    if (userDataListener != null && data instanceof Map) {
                        userDataListener.onUserDataReceived((Map<String, String>) data);
                    }
                    break;
                    
                case "USER_DATA_ERROR":
                    if (userDataListener != null && data instanceof String) {
                        userDataListener.onUserDataError((String) data);
                    }
                    break;
                    
                case "AUTH_REQUIRED":
                    if (authListener != null) {
                        authListener.onAuthRequired();
                    }
                    break;
            
                default:
                    System.out.println("Server message: " + data + " of type: " + type);
                    break;
            }
        } catch (Exception e) {
            System.err.println("Error processing server message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Product convertServerProductToClient(Object serverProduct) {
        if(serverProduct == null){
            System.out.println("Server product is null");
        }

        try {
            Class<?> serverClass = serverProduct.getClass();
            //  System.out.println("Converting server product of class: " + serverClass.getName());

            String id = (String) serverClass.getMethod("getId").invoke(serverProduct);
            String name = (String) serverClass.getMethod("getName").invoke(serverProduct);
            String category = (String) serverClass.getMethod("getCategory").invoke(serverProduct);
            double price = (double) serverClass.getMethod("getPrice").invoke(serverProduct);
            String imagePath = (String) serverClass.getMethod("getImagePath").invoke(serverProduct);
            int stockQuantity = (int) serverClass.getMethod("getStockQuantity").invoke(serverProduct);
            boolean isAvailable = (boolean) serverClass.getMethod("isAvailable").invoke(serverProduct);

            String description = "";
            try {
                description = (String) serverClass.getMethod("getDescription").invoke(serverProduct);

                if(description == null){
                    description = "No description available";
                }
            } catch (Exception e) {
                description = "No description available";
                System.out.println("Warning: Could not get description for product " + name);
            }

            String[] productData = {
                id, name, category, String.valueOf(price),
                imagePath, String.valueOf(stockQuantity), String.valueOf(isAvailable)
            };

            Product clientProduct = new Product(productData, description);
            // System.out.println("  Successfully converted: " + name + " (" + category + ")");
            return clientProduct;

        } catch (Exception e) {
            System.err.println("Error converting server product: " + e.getMessage());
            System.err.println("Server product class: " + serverProduct.getClass().getName());
            e.printStackTrace();
            return null;
        }
    }

    private void updateInventory(List<?> newInventory) {
        this.cachedInventory.clear();
        
        // Convert server products to client products
        for(Object serverProduct : newInventory) {
            Product clientProduct = convertServerProductToClient(serverProduct);
            if(clientProduct != null) {
                this.cachedInventory.add(clientProduct);
            }
        }

        System.out.println("Inventory updated. Now available size: " + cachedInventory.size());

        if(this.updateListener != null){
            updateListener.onInventoryUpdated(new ArrayList<>(cachedInventory));
        }
    }

    private void queueMessage(Message message){
        if(conneced){
            try {
                outgoingMessages.put(message);
            } catch (InterruptedException e) {
                System.out.println("Interrupted while queuing message: " + e.getMessage());
                Thread.currentThread().interrupt();
            }
        }else{
            System.out.println("Not connected to server - cannot queue message");
        }
    }

    public List<Product> requestInventory(){
        if(!conneced){
            System.out.println("Not connected to server");
            return new ArrayList<>();
        }

        Message request = new Message("GET_INVENTORY", null);
        queueMessage(request);
        return this.cachedInventory;
    }

    public void sendPurchase(Map<String, Integer> items){
        if(!this.conneced){
            System.out.println("Not connected to server");
            return;
        }

        Message message = new Message("PURCHASE", items);
        queueMessage(message);
        System.out.println("Purchase queued for sending to server");
    }

    public void sendPing(){
        if(!conneced){
            System.out.println("Server not connected");
            return;
        }

        Message message = new Message("PING", null);
        queueMessage(message);
        System.out.println("Ping queued for sending to server");
        
    }
    
    public void setUpdateListener(InventoryUpdateListener listener){
        // Bridge: Convert our internal List<?> to List<Product> for the interface
        this.updateListener = (inventory) -> {
            // Convert List<?> to List<Product>
            List<Product> productList = new ArrayList<>();
            for(Object obj : inventory) {
                if(obj instanceof Product) {
                    productList.add((Product) obj);
                }
            }
            // Call the listener with proper List<Product>
            if(listener != null) {
                listener.onInventoryUpdated(productList);
            }
        };
    }

    // private InternalInventoryUpdateListener updateListener;

    private interface InternalInventoryUpdateListener {
        void onInventoryUpdated(List<?> inventory);
    }

    private UserDataListener userDataListener;
    private AuthListener authListener;
    private PurchaseListener purchaseListener;

    public void setUserDataListener(UserDataListener listener) {
        this.userDataListener = listener;
    }

    public void setAuthListener(AuthListener listener) {
        this.authListener = listener;
    }

    public void setPurchaseListener(PurchaseListener listener) {
        this.purchaseListener = listener;
    }

    public void requestUserData(String sessionId) {
        if (!conneced) {
            if (userDataListener != null) {
                userDataListener.onUserDataError("Not connected to server");
            }
            return;
        }
        
        Map<String, String> request = new HashMap<>();
        request.put("sessionId", sessionId);
        
        Message message = new Message("GET_USER_DATA", request);
        queueMessage(message);
        System.out.println("User data request queued for session: " + sessionId);
    }

    public void login(String username, String password) {
        if (!conneced) {
            if (authListener != null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Not connected to server");
                authListener.onLoginFailed(error);
            }
            return;
        }
        
        Map<String, String> loginData = new HashMap<>();
        loginData.put("username", username);
        loginData.put("password", password);
        
        Message loginRequest = new Message("LOGIN", loginData);
        queueMessage(loginRequest);
        System.out.println("Login request queued for: " + username);
    }

    public void register(String username, String password, String email, String fullName, String address, String phone) {
        if (!conneced) {
            if (authListener != null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Not connected to server");
                authListener.onRegisterFailed(error);
            }
            return;
        }
        
        Map<String, String> registerData = new HashMap<>();
        registerData.put("username", username);
        registerData.put("password", password);
        registerData.put("email", email);
        registerData.put("fullName", fullName);
        registerData.put("address", address);
        registerData.put("phone", phone);
        
        Message registerRequest = new Message("REGISTER", registerData);
        queueMessage(registerRequest);
        System.out.println("Registration request queued for: " + username);
    }

    public boolean isConnected(){
        return this.conneced;
    }

    public List<Product> getCachedInventory(){
        return this.cachedInventory;
    }

    public int getOutgoingQueueSize(){
        return this.outgoingMessages.size();
    }

    public interface InventoryUpdateListener {
        void onInventoryUpdated(List<?> cachedInventory);        
    }

    public void sendMessage(Message message){
         if (isConnected() && oos != null) {
            try {
                oos.writeObject(message);
                oos.flush();
                System.out.println("Message sent: " + message.getType());
            } catch (IOException e) {
                System.err.println("Error sending message: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.err.println("Cannot send message - not connected to server");
        }
    }

    public static void main(String[] args) {
        NetworkClient client = NetworkClient.getInstance();
        
        client.setUpdateListener(inventory -> {
            System.out.println("Inventory updated! Total products: " + inventory.size());
        });
        
        boolean connected = client.connect("127.0.0.1", 8888);

        if (connected) {
            System.out.println("Client Connected to server");
            
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
          
            client.requestInventory();
            // System.out.println("should get the inventory");
            
            // Send ping
            client.sendPing();
            
            // Wait for responses
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            // Test purchase
            Map<String, Integer> testPurchase = new HashMap<>();
            testPurchase.put("C001", 1);
            client.sendPurchase(testPurchase);
            
            // Keep running for a bit longer
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            client.disconnect();
        }
    }

}
