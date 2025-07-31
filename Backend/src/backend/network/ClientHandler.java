package backend.network;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import backend.models.Product;
import backend.models.User;
import backend.models.UserManager;
import backend.models.SessionManager;
import backend.models.SessionManager.UserSession;
 
public class ClientHandler implements Runnable {
    private String ID;
    private Socket clienSocket;
    private ShopServer server;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;
    private boolean running = false;

    private String sessionId = null;
    private User authenticatedUser = null;

    
    /**
     * @param clientSocket
     * @param shopServer
     * @param ID
     */
    public ClientHandler(Socket clientSocket, ShopServer shopServer, String ID) {
        this.clienSocket = clientSocket;
        this.server = shopServer;
        this.ID = ID;
    }
    
    @Override
    public void run(){
        try {
            oos = new ObjectOutputStream(clienSocket.getOutputStream());
            oos.flush();
            ois = new ObjectInputStream(clienSocket.getInputStream());
            this.running = true;

            System.out.println("Client "+ ID + " conncted successfully.");

            Object inputObject;
            while (running && (inputObject = ois.readObject()) != null) {
                // System.out.println("DEBUG: Received object: " + inputObject.getClass().getName());
                if (inputObject.getClass().getName().endsWith("Message")) {
                    try {
                        java.lang.reflect.Method getTypeMethod = inputObject.getClass().getMethod("getType");
                        java.lang.reflect.Method getDataMethod = inputObject.getClass().getMethod("getData");

                        String type = (String) getTypeMethod.invoke(inputObject);
                        Object data = getDataMethod.invoke(inputObject);

                        Message serverMsg = new Message(type, data);
                    
                        System.out.println("type=" + serverMsg.getType() + 
                                        ", data=" + serverMsg.getData());
                        
                        processRequest(serverMsg);
                    } catch (Exception e) {
                         System.out.println("Error processing message via reflection: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("Not a Message object: " + inputObject);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            if (running) {
                System.err.println("Error handling client: " + this.ID + " : " + e.getMessage());
                e.printStackTrace();  
            }
        } finally{
            this.close();
            server.removeClient(this);
        }
    }

    

    /**
     * @param message
     */
    private void processRequest(Message message) {
        System.out.println("Received message from: " + this.ID + ": " + message.getType());

        switch (message.getType()) {
            case "GET_INVENTORY":
                this.sendInventoryUpdate();
                break;
            case "PURCHASE":
                if(message.getData() instanceof Map){
                    this.processPurchase((Map<String, Integer>) message.getData());
                }
                break;
            case "PING":
                this.sendPong();
                break;
            case "LOGIN":
                if (message.getData() instanceof Map) {
                    processLogin((Map<String, String>) message.getData());
                }
                break;

            case "REGISTER":
                if (message.getData() instanceof Map) {
                    processRegister((Map<String, String>) message.getData());
                }
                break;

            case "LOGOUT":
                processLogout();
                break;

            case "GET_USER_DATA":
                if (message.getData() instanceof Map) {
                    processUserDataRequest((Map<String, String>) message.getData());
                }
                break;
        
            case "COMPLETE_ORDER":
                if (message.getData() instanceof Map) {
                    processCompleteOrder((Map<String, Object>) message.getData());
                }
                break;
                
            default:
            System.out.println("Unknown request from the client " + ID + ": " + message.getType());
                break;
        }
    }

   
    private void sendPong() {
        try {
            Message pong = new Message("PONG", "Server alive");
            oos.writeObject(pong);
            oos.flush();
        } catch (Exception e) {
            System.err.println("Error sending pong to the client " + this.ID + ": " + e.getMessage());
        }
    }

    private void processPurchase(Map<String, Integer> items) {
        if (!isAuthenticated()) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Authentication required for purchase");
            Message authError = new Message("AUTH_REQUIRED", response);
            sendMessage(authError);
            return;
        }
        
        // Pre-validate all items before any stock updates
        List<String> errors = new ArrayList<>();
        List<Product> itemsToUpdate = new ArrayList<>();
        Map<String, Integer> validatedItems = new HashMap<>();
        
        synchronized (server) { // Synchronize the entire validation and update process
            // First pass: Validate all items
            for (Map.Entry<String, Integer> entry : items.entrySet()) {
                String productId = entry.getKey();
                int quantity = entry.getValue();
                
                Product product = null;
                for (Product p : server.getInventory()) {
                    if (p.getId().equals(productId)) {
                        product = p;
                        break;
                    }
                }
                
                if (product == null) {
                    errors.add("Product not found: " + productId);
                } else if (product.getStockQuantity() < quantity) {
                    errors.add("Insufficient stock for " + product.getName() + 
                              ". Available: " + product.getStockQuantity() + 
                              ", Requested: " + quantity);
                } else {
                    itemsToUpdate.add(product);
                    validatedItems.put(productId, quantity);
                }
            }
            
            // If any validation failed, abort entire purchase
            if (!errors.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("errors", errors);
                Message errorResponse = new Message("PURCHASE_FAILED", response);
                sendMessage(errorResponse);
                return;
            }
            
            // Second pass: Update all stocks atomically
            List<String> updatedProducts = new ArrayList<>();
            boolean allUpdatesSuccessful = true;
            
            for (Map.Entry<String, Integer> entry : validatedItems.entrySet()) {
                String productId = entry.getKey();
                int quantity = entry.getValue();
                
                // Use the enhanced updateProductStock method with collision detection
                boolean updateSuccess = server.updateProductStock(productId, quantity);
                
                if (updateSuccess) {
                    updatedProducts.add(productId);
                    System.out.println(" Processed purchase: " + quantity + " units of product " + productId);
                } else {
                    // This should rarely happen due to pre-validation, but handle it
                    errors.add("Failed to update stock for product: " + productId);
                    allUpdatesSuccessful = false;
                    break;
                }
            }
            
            if (!allUpdatesSuccessful) {
                // Rollback would be complex, so we rely on pre-validation
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("errors", errors);
                Message errorResponse = new Message("PURCHASE_FAILED", response);
                sendMessage(errorResponse);
                return;
            }
            
            // Refresh server inventory and broadcast update
            server.refreshInventoryFromFile();
            server.broadcastInventoryUpdateMessage();
            
            // Log the complete order
            logCompleteOrderToFile(itemsToUpdate, validatedItems, extractCustomerInfo());
            
            // Send success response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Purchase processed successfully");
            response.put("updatedProducts", updatedProducts);
            response.put("totalItems", validatedItems.size());
            
            Message confirmation = new Message("PURCHASE_CONFIRMED", response);
            sendMessage(confirmation);
            
            System.out.println("Purchase confirmation sent to client " + this.ID);
        }
    }

    // Helper method to extract customer info for logging
    private Map<String, String> extractCustomerInfo() {
        Map<String, String> customerInfo = new HashMap<>();
        
        if (authenticatedUser != null) {
            customerInfo.put("name", authenticatedUser.getFullName());
            customerInfo.put("username", authenticatedUser.getUsername());
            // Add default values for missing info
            customerInfo.put("address", "Not provided");
            customerInfo.put("contact", "Not provided");
            customerInfo.put("postCode", "Not provided");
        } else {
            customerInfo.put("name", "Guest Customer");
            customerInfo.put("username", "");
            customerInfo.put("address", "Not provided");
            customerInfo.put("contact", "Not provided");
            customerInfo.put("postCode", "Not provided");
        }
        
        return customerInfo;
    }

    private void processLogin(Map<String, String> loginData) {
        String username = loginData.get("username");
        String password = loginData.get("password");
        
        User user = UserManager.loginUser(username, password);
        if (user != null) {
            this.sessionId = SessionManager.createSession(username);
            this.authenticatedUser = user;
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("sessionId", sessionId);
            response.put("username", user.getUsername());
            response.put("fullName", user.getFullName());
            
            Message loginResponse = new Message("LOGIN_SUCCESS", response);
            sendMessage(loginResponse);
        } else {
            Map<String, String> response = new HashMap<>();
            response.put("success", "false");
            response.put("error", "Invalid username or password");
            
            Message loginResponse = new Message("LOGIN_FAILED", response);
            sendMessage(loginResponse);
        }
    }

    private void processRegister(Map<String, String> registerData) {
        String username = registerData.get("username");
        String password = registerData.get("password");
        String email = registerData.get("email");
        String fullName = registerData.get("fullName");
        String address = registerData.get("address");
        String phone = registerData.get("phone");
        
        boolean success = UserManager.registerUser(username, password, email, fullName, address, phone);
        
        Map<String, Object> response = new HashMap<>();
        if (success) {
            response.put("success", true);
            response.put("message", "Registration successful");
            Message registerResponse = new Message("REGISTER_SUCCESS", response);
            sendMessage(registerResponse);
        } else {
            response.put("success", false);
            response.put("error", "Username or email already exists");
            Message registerResponse = new Message("REGISTER_FAILED", response);
            sendMessage(registerResponse);
        }
    }

    private void processLogout() {
        if (sessionId != null) {
            SessionManager.removeSession(sessionId);
            sessionId = null;
            authenticatedUser = null;
            
            Message logoutResponse = new Message("LOGOUT_SUCCESS", "Logged out successfully");
            sendMessage(logoutResponse);
        }
    }

    private void processUserDataRequest(Map<String, String> requestData) {
        String sessionId = requestData.get("sessionId");
        
        if (sessionId == null || sessionId.trim().isEmpty()) {
            Message errorResponse = new Message("USER_DATA_ERROR", "Invalid session ID");
            sendMessage(errorResponse);
            return;
        }
        
        UserSession session = SessionManager.getSession(sessionId);
        if (session == null) {
            Message errorResponse = new Message("USER_DATA_ERROR", "Session expired or invalid");
            sendMessage(errorResponse);
            return;
        }
        
        // Get user data from UserManager
        String username = session.getUsername();
        User user = UserManager.getUser(username); // You'll need to add this method to UserManager
        
        if (user != null) {
            Map<String, String> userData = new HashMap<>();
            userData.put("username", user.getUsername());
            userData.put("email", user.getEmail());
            userData.put("fullName", user.getFullName());
            userData.put("address", user.getAddress());
            userData.put("phone", user.getPhone());
            
            Message userDataResponse = new Message("USER_DATA_RESPONSE", userData);
            sendMessage(userDataResponse);
        } else {
            Message errorResponse = new Message("USER_DATA_ERROR", "User not found");
            sendMessage(errorResponse);
        }
    }
    
    private boolean isAuthenticated() {
        return sessionId != null && SessionManager.isValidSession(sessionId);
    }

    public void sendInventoryUpdate() {
        try {
            List<Product> inventory = server.getInventory();
            int chunkSize = 20;
            int totalProducts = inventory.size();

            Message countMessage = new Message("INVENTORY_COUNT", totalProducts);
            oos.writeObject(countMessage);
            oos.flush();

            for(int i=0; i<totalProducts; i+=chunkSize){
                int endIndex = Math.min(i+chunkSize, totalProducts);
                List<Product> chunk = new ArrayList<>();
                for(int j=i; j<endIndex; j++){
                    chunk.add(inventory.get(j));
                }
                Message chunkMessage = new Message("INVENTORY_CHUNK", chunk);
                oos.writeObject(chunkMessage);
                oos.flush();

                Thread.sleep(10);
            }

            Message completeMessage = new Message("INVENTORY_COMPLETE", null);

            oos.writeObject(completeMessage);
            oos.flush();
            System.out.println("Successfully sent the inventory info to the client " + this.ID + " in chunks");
        } catch (Exception e) {
            System.err.println("Error sending inventory information to the client " + this.ID + ": " + e.getMessage() );
        }
    }

    public void sendMessage(Message message){
        try {
            oos.reset();
            oos.writeObject(message);
            oos.flush();
        } catch (Exception e) {
            System.err.println("Error sending custom message to the client: " + this.ID + ": " + e.getMessage());
        }
    }

    
    public void close() {
        this.running = false;
        try {
            if(oos != null){
                oos.close();
            }

            if(ois != null){
                ois.close();
            }

            if(clienSocket != null && !clienSocket.isClosed()){
                clienSocket.close();
            }

            System.out.println("Successfully closed the client: " + this.ID);
        } catch (Exception e) {
            System.err.println("Error closing client " + this.ID + ": " + e.getMessage());
        }
    }

    public String getID(){
        return this.ID;
    }

    public boolean isRunning(){
        return this.running;
    }
    
    private void processCompleteOrder(Map<String, Object> orderData) {
        if (!isAuthenticated()) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Authentication required for purchase");
            Message authError = new Message("AUTH_REQUIRED", response);
            sendMessage(authError);
            return;
        }
        
        Map<String, String> customerInfo = (Map<String, String>) orderData.get("customerInfo");
        Map<String, Integer> items = (Map<String, Integer>) orderData.get("items");
        
        List<String> errors = new ArrayList<>();
        List<Product> itemsToUpdate = new ArrayList<>();
        Map<String, Integer> validatedItems = new HashMap<>();
        
        synchronized (server) { // Synchronize the entire process
            // Validate all items first
            for (Map.Entry<String, Integer> entry : items.entrySet()) {
                String productId = entry.getKey();
                int quantity = entry.getValue();
                
                Product product = null;
                for (Product p : server.getInventory()) {
                    if (p.getId().equals(productId)) {
                        product = p;
                        break;
                    }
                }
                
                if (product == null) {
                    errors.add("Product not found: " + productId);
                } else if (product.getStockQuantity() < quantity) {
                    errors.add("Insufficient stock for " + product.getName() + 
                              ". Available: " + product.getStockQuantity() + 
                              ", Requested: " + quantity);
                } else {
                    itemsToUpdate.add(product);
                    validatedItems.put(productId, quantity);
                }
            }
            
            if (!errors.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("errors", errors);
                Message errorResponse = new Message("PURCHASE_FAILED", response);
                sendMessage(errorResponse);
                return;
            }
            
            // Update all stocks atomically
            List<String> updatedProducts = new ArrayList<>();
            boolean allUpdatesSuccessful = true;
            
            for (Map.Entry<String, Integer> entry : validatedItems.entrySet()) {
                String productId = entry.getKey();
                int quantity = entry.getValue();
                
                boolean updateSuccess = server.updateProductStock(productId, quantity);
                if (updateSuccess) {
                    updatedProducts.add(productId);
                } else {
                    allUpdatesSuccessful = false;
                    break;
                }
            }
            
            if (!allUpdatesSuccessful) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("errors", List.of("Failed to process order due to stock conflicts"));
                Message errorResponse = new Message("PURCHASE_FAILED", response);
                sendMessage(errorResponse);
                return;
            }
            
            // Refresh and broadcast
            server.refreshInventoryFromFile();
            server.broadcastInventoryUpdateMessage();
            
            // Log the complete order with customer details
            logCompleteOrderToFile(itemsToUpdate, validatedItems, customerInfo);
            
            // Send success response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Order processed successfully");
            response.put("updatedProducts", updatedProducts);
            response.put("totalItems", validatedItems.size());
            
            Message confirmation = new Message("PURCHASE_CONFIRMED", response);
            sendMessage(confirmation);
            
            System.out.println(" Complete order confirmation sent to client " + this.ID);
        }
    }

    // Enhanced order logging with customer details
    private void logCompleteOrderToFile(List<Product> products, Map<String, Integer> quantities, Map<String, String> customerInfo) {
        try {
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String timestamp = now.format(formatter);
            
            // Get authenticated user info
            String customerName = customerInfo.get("name");
            String address = customerInfo.get("address");
            String contact = customerInfo.get("contact");
            String postCode = customerInfo.get("postCode");
            
            String username = "";
            if (authenticatedUser != null) {
                username = authenticatedUser.getUsername();
            }
            
            // Build order string with complete customer details
            StringBuilder sb = new StringBuilder();
            sb.append("Order Time: ").append(timestamp).append("\n");
            sb.append("Customer  : ").append(customerName).append("\n");
            sb.append("Address   : ").append(address).append("\n");
            sb.append("Contact   : ").append(contact).append("\n");
            sb.append("Post Code : ").append(postCode).append("\n");
            
            if (!username.isEmpty()) {
                sb.append("Username  : ").append(username).append("\n");
            }
            
            sb.append("Items     :\n");

            double total = 0.0;
            for (Product product : products) {
                Integer qty = quantities.get(product.getId());
                if (qty != null) {
                    double unitPrice = product.getPrice();
                    double subtotal = unitPrice * qty;
                    sb.append("- Product: ").append(product.getName())
                            .append(", Quantity: ").append(qty)
                            .append(", Unit Price: $").append(String.format("%.2f", unitPrice))
                            .append(", Subtotal: $").append(String.format("%.2f", subtotal)).append("\n");
                    total += subtotal;
                }
            }
            sb.append("Total: $").append(String.format("%.2f", total)).append("\n");
            sb.append("----------------------------------------\n\n");

            // Write to orders.txt file
            java.io.File ordersFile = new java.io.File("Backend/data/orders.txt");
            try (java.io.FileWriter writer = new java.io.FileWriter(ordersFile, true);
                 java.io.BufferedWriter bufferedWriter = new java.io.BufferedWriter(writer)) {
                bufferedWriter.write(sb.toString());
                System.out.println(" Complete order logged to orders.txt successfully");
            }
            
        } catch (java.io.IOException e) {
            System.err.println(" Error logging complete order to file: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
