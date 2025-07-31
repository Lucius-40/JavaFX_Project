package backend.network;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javafx.application.Platform;

import backend.models.Product;

public class NetworkService {
    private static NetworkService instance;
    private NetworkClient client;
    private boolean connected = false;
    
    //   CENTRALIZED INVENTORY STORAGE
    private static List<Product> globalInventory = new ArrayList<>();
    private static final Object inventoryLock = new Object();
    private static final List<Runnable> inventoryObservers = new ArrayList<>();

    private NetworkService(){
        this.client = NetworkClient.getInstance();
    }

    public static synchronized NetworkService getInstance(){
        if(instance == null){
            instance = new NetworkService();
        }
        return instance;
    }

    public CompletableFuture<Boolean> connectToServer(String host, int port) {
        return CompletableFuture.supplyAsync(() -> {
            System.out.println("Connecting to server at " + host + ":" + port + "...");
            this.connected = this.client.connect(host, port);
            if (connected) {
                // Always set the inventory update listener so global inventory stays in sync
                setInventoryUpdateListener(null); // null means only update global inventory and observers
                System.out.println("Connection successful, requesting inventory...");
                client.requestInventory();
            } else {
                System.out.println("Connection failed!");
            }
            return this.connected;
        });
    }

    //   UPDATED: Set up centralized inventory listener
    public void setInventoryUpdateListener(InventoryUpdateListener listener){
        if (this.client != null) {
            this.client.setUpdateListener(inventory -> {
                //   CRITICAL: First update global inventory and notify all observers
                updateGlobalInventoryAndNotify((List<Product>)inventory);
                
                //   Then call the direct listener (for backward compatibility)
                if (listener != null) {
                    Platform.runLater(() -> {
                        listener.onInventoryUpdated((List<Product>)inventory);
                    });
                }
            });
        }
    }

    public static void addInventoryObserver(Runnable observer) {
        synchronized(inventoryObservers) {
            if (!inventoryObservers.contains(observer)) {
                inventoryObservers.add(observer);
                // System.out.println("Added inventory observer. Total observers: " + inventoryObservers.size());
            }
        }
    }

    public static void removeInventoryObserver(Runnable observer) {
        synchronized(inventoryObservers) {
            inventoryObservers.remove(observer);
            // System.out.println("Removed inventory observer. Total observers: " + inventoryObservers.size());
        }
    }

    public static List<Product> getGlobalInventory() {
        synchronized(inventoryLock) {
            return new ArrayList<>(globalInventory);
        }
    }

    //   NEW: Centralized update method
    private void updateGlobalInventoryAndNotify(List<Product> newInventory) {
        synchronized(inventoryLock) {
            int oldSize = globalInventory.size();
            globalInventory.clear();
            globalInventory.addAll(newInventory);
            System.out.println("🔄 Global inventory updated: " + oldSize + " -> " + globalInventory.size() + " products");
        }

        Platform.runLater(() -> {
            synchronized(inventoryObservers) {
                System.out.println("📡 Notifying " + inventoryObservers.size() + " observers of inventory update");
                for (Runnable observer : inventoryObservers) {
                    try {
                        observer.run();
                        System.out.println(" Observer notified successfully");
                    } catch (Exception e) {
                        System.err.println(" Error notifying observer: " + e.getMessage());
                    }
                }
            }
        });
    }



    public interface InventoryUpdateListener {
        void onInventoryUpdated(List<Product> inventory);
    }

    public List<Product> getCachedInventory(){
        return getGlobalInventory(); //   Use centralized inventory
    }

    public void sendPurchase(Map<String, Integer> items){
        if(this.connected){
            this.client.sendPurchase(items);
        }
    }

    public void sendCompleteOrder(Map<String, Object> orderData) {
        if (isConnected()) {
            try {
                Message orderMessage = new Message("COMPLETE_ORDER", orderData);
                client.sendMessage(orderMessage);
            } catch (Exception e) {
                System.err.println("Error sending complete order: " + e.getMessage());
            }
        }
    }

    public boolean isConnected(){
        return this.connected && this.client != null && this.client.isConnected();
    }

    public void disconnect(){
        if(this.client != null){
            this.client.disconnect();
        }
        this.connected = false;
        
        //   Clear observers on disconnect
        synchronized(inventoryObservers) {
            inventoryObservers.clear();
        }
        synchronized(inventoryLock) {
            globalInventory.clear();
        }
    }

    //   Keep existing methods for authentication
    public void setAuthListener(NetworkClient.AuthListener listener) {
        if (this.client != null) {
            this.client.setAuthListener(listener);
        }
    }

    public void login(String username, String password) {
        if (this.connected && this.client != null) {
            this.client.login(username, password);
        }
    }

    public void register(String username, String password, String email, String fullName, String address, String phone) {
        if (this.connected) {
            this.client.register(username, password, email, fullName, address, phone);
        }
    }

    public void requestUserData(String sessionId) {
        if (this.connected && this.client != null) {
            this.client.requestUserData(sessionId);
        }
    }

    public void setUserDataListener(NetworkClient.UserDataListener listener) {
        if (this.client != null) {
            this.client.setUserDataListener(listener);
        }
    }

    public void setPurchaseListener(NetworkClient.PurchaseListener listener) {
        if (this.client != null) {
            this.client.setPurchaseListener(listener);
        }
    }

    public static void main(String[] args) {
        new NetworkService();
    }
}
