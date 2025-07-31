package backend.network;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import backend.models.Inventory;
import backend.models.Product;
import backend.models.UserManager;


public class ShopServer {
    // File watcher thread for products.txt
    private Thread fileWatcherThread;

    private void startFileWatcher() {
        fileWatcherThread = new Thread(() -> {
            try {
                java.nio.file.Path dir = java.nio.file.Paths.get("Backend/data");
                java.nio.file.WatchService watchService = java.nio.file.FileSystems.getDefault().newWatchService();
                dir.register(watchService, java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY);

                while (running) {
                    java.nio.file.WatchKey key = watchService.take();
                    for (java.nio.file.WatchEvent<?> event : key.pollEvents()) {
                        java.nio.file.Path changed = (java.nio.file.Path) event.context();
                        if (changed.toString().equals("products.txt")) {
                            System.out.println("products.txt modified, reloading inventory...");
                            refreshInventoryFromFile();
                            broadcastInventoryUpdateMessage();
                        }
                    }
                    key.reset();
                }
            } catch (Exception e) {
                System.err.println("File watcher error: " + e.getMessage());
            }
        });
        fileWatcherThread.setDaemon(true);
        fileWatcherThread.start();
    }
    private static final int PORT = 8888;
    private ServerSocket serverSocket;
    private boolean running = false;
    private ExecutorService threadpool;
    private List<ClientHandler> conncectedClients = Collections.synchronizedList(new ArrayList<>());

    private List<Product> serverInventory;

    // Public method to refresh inventory from file
    public void refreshInventoryFromFile() {
        try {
            // Reload inventory from file
            backend.models.Inventory.loadFromFile();
            this.serverInventory = new ArrayList<>(backend.models.Inventory.getAllProducts());
            System.out.println(" Server inventory refreshed from file: " + this.serverInventory.size() + " products");
        } catch (Exception e) {
            System.err.println(" Error refreshing inventory from file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public ShopServer(){
        threadpool = Executors.newCachedThreadPool();
        loadInventory();
    }

    private void loadInventory() {
        try {
            String absolutePath = new File("Backend/data/products.txt").getAbsolutePath();
            String descriptionPath = new File("Backend/data/descriptions.txt").getAbsolutePath();
            String usersPath = new File("Backend/data/users.txt").getAbsolutePath();

            Inventory.setProductsFilePath(absolutePath);
            Inventory.setDescriptionFilePath(descriptionPath);
            UserManager.setUsersFilePath(usersPath);
            
            System.out.println("Products file path: " + absolutePath);
            System.out.println("Users file path: " + usersPath);

            Inventory.loadFromFile();
            UserManager.loadUsers();
            
            serverInventory = new ArrayList<>(Inventory.getAllProducts());
            System.out.println("Products loaded: " + serverInventory.size());
            System.out.println("Users loaded: " + UserManager.getUserCount());
            
        } catch (Exception e) {
            System.out.println("Failed to load data files");
            e.printStackTrace();
        }
    }

    public void start(){
        try {
            this.serverSocket = new ServerSocket(PORT);
            this.running = true;
            System.out.println("Shop server running on port: " + ShopServer.PORT);

            // Start file watcher thread
            startFileWatcher();

            while (running) {
                try {
                    Socket clientSocket = this.serverSocket.accept();
                    System.out.println("New Client connected: " + clientSocket.getInetAddress().getHostAddress());
                    String ID = clientSocket.getInetAddress().getHostAddress();

                    ClientHandler handler = new ClientHandler(clientSocket, this, ID);
                    conncectedClients.add(handler);

                    this.threadpool.execute(handler);
                } catch (IOException e) {
                    if(running){
                        System.err.println("Error accepting client connection: " + e.getMessage());
                        // e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void stop(){
        this.running = false;
        for(ClientHandler client: this.conncectedClients){
            client.close();
        }

        conncectedClients.clear();

        if(threadpool != null){
            this.threadpool.shutdown();
            try {
                if(!threadpool.awaitTermination(5, TimeUnit.SECONDS)){
                    this.threadpool.shutdownNow();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                this.threadpool.shutdownNow();
            }
        }

        if(this.serverSocket != null && !serverSocket.isClosed()){
            try {
                this.serverSocket.close();
                System.out.println("Server stopped");
            } catch (Exception e) {
                System.err.println("Error closing server: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public List<Product> getInventory(){
        return this.serverInventory;
    }

    public synchronized boolean updateProductStock(String productId, int quantity) {
        for (Product product : this.serverInventory) {
            if (product.getId().equals(productId)) {
                int oldStock = product.getStockQuantity();
                int newStock = oldStock - quantity;
                
                // Collision detection: Check if stock would go negative
                if (newStock < 0) {
                    System.out.println(" COLLISION DETECTED: Insufficient stock for product " + productId);
                    System.out.println("   Requested: " + quantity + ", Available: " + oldStock);
                    return false; // Purchase cannot be completed
                }
                
                // Update stock
                product.setStockQuantity(newStock);
                System.out.println(" Stock updated for product " + productId + " from " + oldStock + " to " + newStock);
                
                // Immediately save to file to persist changes
                try {
                    backend.models.Inventory.saveToFile();
                    System.out.println(" Inventory saved to file after stock update");
                } catch (Exception e) {
                    System.err.println(" Error saving inventory to file: " + e.getMessage());
                }
                
                return true; // Update successful
            }
        }
        
        System.out.println(" Product not found: " + productId);
        return false; // Product not found
    }



    
    public void broadcastInventoryUpdateMessage() {
        List<Product> inventory = getInventory();
        Message updateMsg = new Message("INVENTORY_UPDATE", inventory);
        
        List<ClientHandler> clientsToRemove = new ArrayList<>();
        int successfulBroadcasts = 0;
        
        synchronized (conncectedClients) {
            System.out.println("Broadcasting inventory update to " + conncectedClients.size() + " clients...");
            
            for (ClientHandler client : conncectedClients) {
                try {
                    if (client.isRunning()) {
                        client.sendMessage(updateMsg);
                        successfulBroadcasts++;
                    } else {
                        clientsToRemove.add(client);
                    }
                } catch (Exception e) {
                    System.err.println("Error broadcasting to client " + client.getID() + ": " + e.getMessage());
                    clientsToRemove.add(client);
                }
            }
            
            // Clean up disconnected clients
            for (ClientHandler client : clientsToRemove) {
                conncectedClients.remove(client);
                System.out.println("🗑️ Removed disconnected client: " + client.getID());
            }
        }
        
        System.out.println("Inventory update broadcasted to " + successfulBroadcasts + " clients");
        System.out.println("Current inventory: " + inventory.size() + " products");
    }

    public void removeClient(ClientHandler client){
        conncectedClients.remove(client);
        System.out.println("Client disconnected: " + client.getID() + ". Remaining clients: " + conncectedClients.size());
    }

    public int getClientCount() {
        return conncectedClients.size();
    }

    // Also make the conncectedClients list accessible if needed
    public List<ClientHandler> getConnectedClients() {
        return new ArrayList<>(conncectedClients);
    }

    // Method to get real-time server statistics
    public Map<String, Object> getServerStats() {
        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("connectedClients", conncectedClients.size());
        stats.put("inventorySize", serverInventory.size());
        stats.put("serverRunning", running);
        return stats;
    }

    public static void main(String[] args) {
        ShopServer server = new ShopServer();
        server.start();
    }
}
