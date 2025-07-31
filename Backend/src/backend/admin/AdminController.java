package backend.admin;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import javafx.geometry.Insets;


import backend.models.Product;
import backend.models.Inventory;
import backend.network.ShopServer;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.List;
import java.util.Optional;
import java.io.*;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.HashMap;

public class AdminController implements Initializable {
    
    @FXML private TableView<Product> productTable;
    @FXML private TableColumn<Product, String> productIdColumn;
    @FXML private TableColumn<Product, String> productNameColumn;
    @FXML private TableColumn<Product, String> categoryColumn;
    @FXML private TableColumn<Product, Double> priceColumn;
    @FXML private TableColumn<Product, Integer> stockColumn;
    @FXML private TableColumn<Product, Boolean> availableColumn;
    
    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private Button refreshButton;
    @FXML private Button updateStockButton;
    @FXML private Button addProductButton;
    
    // Order Management Tab
    @FXML private TextArea ordersTextArea;
    @FXML private Button refreshOrdersButton;
    @FXML private Label orderCountLabel;
    @FXML private Button proceedOrderButton;
    @FXML private Button removeOrderButton;
    @FXML private ComboBox<OrderInfo> orderSelector;
    
    // Server Status
    @FXML private Label serverStatusLabel;
    @FXML private Label connectedClientsLabel;
    @FXML private Button startServerButton;
    @FXML private Button stopServerButton;
    
    private ObservableList<Product> productList = FXCollections.observableArrayList();
    private ShopServer server;
    private Thread serverThread;
    private boolean serverRunning = false;
    
    // Store parsed orders for management
    private List<OrderInfo> parsedOrders = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupProductTable();
        setupCategoryFilter();
        loadProducts();
        loadOrders();
        updateServerStatus();
        
        // Set up auto-refresh every 30 seconds
        startAutoRefresh();
        
        // Initialize order management
        setupOrderManagement();
    }

    private void setupOrderManagement() {
        // Initially disable order action buttons
        proceedOrderButton.setDisable(true);
        removeOrderButton.setDisable(true);
        
        // Enable buttons when an order is selected
        orderSelector.setOnAction(e -> {
            boolean orderSelected = orderSelector.getValue() != null;
            proceedOrderButton.setDisable(!orderSelected);
            removeOrderButton.setDisable(!orderSelected);
        });
    }

    // Inner class to represent order information
    private static class OrderInfo {
        private String orderTime;
        private String customer;
        private String address;
        private String contact;
        private String postCode;
        private String username;
        private String items;
        private String total;
        private String fullOrderText;
        
        public OrderInfo(String orderTime, String customer, String fullOrderText) {
            this.orderTime = orderTime;
            this.customer = customer;
            this.fullOrderText = fullOrderText;
        }
        
        @Override
        public String toString() {
            return orderTime + " - " + customer;
        }
        
        // Getters
        public String getOrderTime() { return orderTime; }
        public String getCustomer() { return customer; }
        public String getFullOrderText() { return fullOrderText; }
        public void setAddress(String address) { this.address = address; }
        public void setContact(String contact) { this.contact = contact; }
        public void setPostCode(String postCode) { this.postCode = postCode; }
        public void setUsername(String username) { this.username = username; }
        public void setItems(String items) { this.items = items; }
        public void setTotal(String total) { this.total = total; }
    }

    private void setupProductTable() {
        productIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        productNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        stockColumn.setCellValueFactory(new PropertyValueFactory<>("stockQuantity"));
        availableColumn.setCellValueFactory(new PropertyValueFactory<>("available"));
        
        // Format price column
        priceColumn.setCellFactory(column -> new TableCell<Product, Double>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText("");
                } else {
                    setText(String.format("$%.2f", price));
                }
            }
        });
        
        // Color code stock levels
        stockColumn.setCellFactory(column -> new TableCell<Product, Integer>() {
            @Override
            protected void updateItem(Integer stock, boolean empty) {
                super.updateItem(stock, empty);
                if (empty || stock == null) {
                    setText("");
                    setStyle("");
                } else {
                    setText(stock.toString());
                    if (stock == 0) {
                        setStyle("-fx-background-color: #ffcccb;"); // Light red
                    } else if (stock < 50) {
                        setStyle("-fx-background-color: #fff3cd;"); // Light yellow
                    } else {
                        setStyle("-fx-background-color: #d4edda;"); // Light green
                    }
                }
            }
        });
        
        productTable.setItems(productList);
    }

    private void setupCategoryFilter() {
        categoryFilter.getItems().addAll("All", "Clothes", "Electronics", "Groceries", "Stationary", "Others", "Shoes");
        categoryFilter.setValue("All");
        
        categoryFilter.setOnAction(e -> filterProducts());
        searchField.textProperty().addListener((obs, oldText, newText) -> filterProducts());
    }

    @FXML
    private void loadProducts() {
        Platform.runLater(() -> {
            try {
                String currentDir = System.getProperty("user.dir");
                System.out.println("Current working directory: " + currentDir);
                
                String absolutePath = new File("Backend/data/products.txt").getAbsolutePath();
                String descriptionPath = new File("data/descriptions.txt").getAbsolutePath();
                String usersPath = new File("data/users.txt").getAbsolutePath();
                
                System.out.println("Trying to load products from: " + absolutePath);
                
                Inventory.setProductsFilePath(absolutePath);
                Inventory.setDescriptionFilePath(descriptionPath);
                
                Inventory.loadFromFile();
                List<Product> products = Inventory.getAllProducts();
                productList.clear();
                productList.addAll(products);
                filterProducts();
                System.out.println("✅ Loaded " + products.size() + " products");
            } catch (Exception e) {
                showAlert("Error", "Failed to load products: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void filterProducts() {
        String searchText = searchField.getText().toLowerCase();
        String selectedCategory = categoryFilter.getValue();
        
        ObservableList<Product> filteredList = FXCollections.observableArrayList();
        
        for (Product product : productList) {
            boolean matchesSearch = searchText.isEmpty() || 
                product.getName().toLowerCase().contains(searchText) ||
                product.getId().toLowerCase().contains(searchText);
                
            boolean matchesCategory = selectedCategory.equals("All") || 
                product.getCategory().equalsIgnoreCase(selectedCategory);
                
            if (matchesSearch && matchesCategory) {
                filteredList.add(product);
            }
        }
        
        productTable.setItems(filteredList);
    }

    @FXML
    private void updateStock() {
        Product selectedProduct = productTable.getSelectionModel().getSelectedItem();
        if (selectedProduct == null) {
            showAlert("No Selection", "Please select a product to update stock.");
            return;
        }
        
        Dialog<Integer> dialog = new Dialog<>();
        dialog.setTitle("Update Stock");
        dialog.setHeaderText("Update stock for: " + selectedProduct.getName());
        
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        
        Label currentStockLabel = new Label("Current Stock: " + selectedProduct.getStockQuantity());
        TextField newStockField = new TextField();
        newStockField.setPromptText("Enter new stock quantity");
        newStockField.setText(String.valueOf(selectedProduct.getStockQuantity()));
        
        content.getChildren().addAll(currentStockLabel, newStockField);
        dialog.getDialogPane().setContent(content);
        
        ButtonType updateButtonType = new ButtonType("Update", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(updateButtonType, ButtonType.CANCEL);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == updateButtonType) {
                try {
                    return Integer.parseInt(newStockField.getText());
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        });
        
        Optional<Integer> result = dialog.showAndWait();
        result.ifPresent(newStock -> {
            if (newStock >= 0) {
                updateProductStock(selectedProduct, newStock);
            } else {
                showAlert("Invalid Input", "Stock quantity cannot be negative.");
            }
        });
    }

    private void updateProductStock(Product product, int newStock) {
        try {
            // Update the product in memory
            product.setStockQuantity(newStock);
            Inventory.saveToFile();

            // Broadcast inventory update to all clients if server is running
            if (server != null && serverRunning) {
                server.refreshInventoryFromFile();
                server.broadcastInventoryUpdateMessage();
                System.out.println("🔄 Stock update broadcasted to all clients");
                System.out.println("📡 Updated " + product.getName() + " stock to " + newStock);
            }

            // Refresh the admin panel display
            productTable.refresh();
            showAlert("Success", String.format("Stock updated for %s\nNew stock: %d\nUpdate broadcasted to %d clients", product.getName(), newStock, server != null ? server.getClientCount() : 0));
            System.out.println("✅ Stock updated: " + product.getName() + " -> " + newStock);
        } catch (Exception e) {
            showAlert("Error", "Failed to update stock: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void addNewProduct() {
        Dialog<Map<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Add New Product");
        dialog.setHeaderText("Enter details for the new product");
        
        // Create the form with ScrollPane for better layout
        ScrollPane scrollPane = new ScrollPane();
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        scrollPane.setContent(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportHeight(400);
        
        TextField idField = new TextField();
        idField.setPromptText("Product ID (e.g., C021, E024, G017)");
        
        TextField nameField = new TextField();
        nameField.setPromptText("Product Name");
        
        ComboBox<String> categoryCombo = new ComboBox<>();
        categoryCombo.getItems().addAll("Clothes", "Electronics", "Groceries", "Stationary", "Others", "Shoes");
        categoryCombo.setPromptText("Select Category");
        
        TextField priceField = new TextField();
        priceField.setPromptText("Price (e.g., 29.99)");
        
        TextField stockField = new TextField();
        stockField.setPromptText("Initial Stock Quantity");
        
        // Add description field
        TextArea descriptionArea = new TextArea();
        descriptionArea.setPromptText("Enter product description (recommended)");
        descriptionArea.setPrefRowCount(3);
        descriptionArea.setWrapText(true);
        
        CheckBox availableCheck = new CheckBox("Available for purchase");
        availableCheck.setSelected(true);
        
        // Add labels and fields
        content.getChildren().addAll(
            new Label("Product ID:"), idField,
            new Label("Product Name:"), nameField,
            new Label("Category:"), categoryCombo,
            new Label("Price ($):"), priceField,
            new Label("Stock Quantity:"), stockField,
            new Label("Description:"), descriptionArea,
            availableCheck
        );
        
        dialog.getDialogPane().setContent(scrollPane);
        dialog.getDialogPane().setPrefWidth(500);
        
        ButtonType addButtonType = new ButtonType("Add Product", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);
        
        // Validation
        Button addButton = (Button) dialog.getDialogPane().lookupButton(addButtonType);
        addButton.setDisable(true);
        
        // Enable button only when all required fields are filled
        Runnable validation = () -> {
            boolean valid = !idField.getText().trim().isEmpty() &&
                           !nameField.getText().trim().isEmpty() &&
                           categoryCombo.getValue() != null &&
                           !priceField.getText().trim().isEmpty() &&
                           !stockField.getText().trim().isEmpty();
            addButton.setDisable(!valid);
        };
        
        idField.textProperty().addListener((obs, oldText, newText) -> validation.run());
        nameField.textProperty().addListener((obs, oldText, newText) -> validation.run());
        categoryCombo.valueProperty().addListener((obs, oldVal, newVal) -> validation.run());
        priceField.textProperty().addListener((obs, oldText, newText) -> validation.run());
        stockField.textProperty().addListener((obs, oldText, newText) -> validation.run());
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                Map<String, String> result = new HashMap<>();
                result.put("id", idField.getText().trim());
                result.put("name", nameField.getText().trim());
                result.put("category", categoryCombo.getValue());
                result.put("price", priceField.getText().trim());
                result.put("stock", stockField.getText().trim());
                result.put("available", String.valueOf(availableCheck.isSelected()));
                
                // Add description to the result
                String description = descriptionArea.getText().trim();
                if (description.isEmpty()) {
                    // Generate a basic description if none provided
                    description = generateDefaultDescription(nameField.getText().trim(), categoryCombo.getValue());
                }
                result.put("description", description);
                
                return result;
            }
            return null;
        });
        
        Optional<Map<String, String>> result = dialog.showAndWait();
        result.ifPresent(productData -> {
            try {
                // Validate price and stock are numbers
                double price = Double.parseDouble(productData.get("price"));
                int stock = Integer.parseInt(productData.get("stock"));
                
                if (price < 0) {
                    showAlert("Invalid Input", "Price cannot be negative.");
                    return;
                }
                if (stock < 0) {
                    showAlert("Invalid Input", "Stock quantity cannot be negative.");
                    return;
                }
                
                // Check if product ID already exists
                if (isProductIdExists(productData.get("id"))) {
                    showAlert("Duplicate ID", "A product with this ID already exists. Please choose a different ID.");
                    return;
                }
                
                createNewProduct(productData);
                
            } catch (NumberFormatException e) {
                showAlert("Invalid Input", "Please enter valid numbers for price and stock quantity.");
            }
        });
    }

    // Helper method to check if product ID exists
    private boolean isProductIdExists(String productId) {
        for (Product product : productList) {
            if (product.getId().equalsIgnoreCase(productId)) {
                return true;
            }
        }
        return false;
    }

    // Helper method to generate default descriptions
    private String generateDefaultDescription(String productName, String category) {
        switch (category.toLowerCase()) {
            case "clothes":
                return "Stylish " + productName.toLowerCase() + " made from quality materials. Comfortable fit and modern design perfect for everyday wear or special occasions.";
            case "electronics":
                return "High-quality " + productName.toLowerCase() + " with modern features and reliable performance. Perfect for both personal and professional use.";
            case "groceries":
                return "Fresh " + productName.toLowerCase() + " of premium quality. Carefully selected for freshness and nutritional value.";
            case "stationary":
                return "Essential " + productName.toLowerCase() + " for office, school, or home use. Durable construction and reliable performance for all your needs.";
            case "shoes":
                return "Comfortable " + productName.toLowerCase() + " with quality construction and stylish design. Perfect for daily wear with excellent comfort and support.";
            case "others":
            default:
                return "Quality " + productName.toLowerCase() + " designed for reliability and functionality. Great addition to your collection with excellent value for money.";
        }
    }

    // Helper method to generate image paths
    private String generateDefaultImagePath(String category, String productName) {
        // Generate a default image path based on category
        String cleanName = productName.replaceAll("[^a-zA-Z0-9\\s]", "").replaceAll("\\s+", "");
        
        switch (category) {
            case "Clothes":
                return "/images/ClothesImage/" + cleanName + ".jpeg";
            case "Electronics":
                return "/images/Electronics/" + cleanName + ".jpeg";
            case "Groceries":
                return "/images/Groceries/" + cleanName + ".jpeg";
            case "Stationary":
                return "/images/Stationary/" + cleanName + ".jpeg";
            case "Shoes":
                return "/images/Shoes/" + cleanName + ".jpeg";
            case "Others":
            default:
                return "/images/Others/" + cleanName + ".jpeg";
        }
    }

    // Method to create new product
    private void createNewProduct(Map<String, String> productData) {
        try {
            String id = productData.get("id");
            String name = productData.get("name");
            String category = productData.get("category");
            double price = Double.parseDouble(productData.get("price"));
            int stock = Integer.parseInt(productData.get("stock"));
            boolean available = Boolean.parseBoolean(productData.get("available"));
            String description = productData.get("description");

            // Generate default image path based on category
            String imagePath = generateDefaultImagePath(category, name);

            // Create product data array in the format expected by Product constructor
            String[] productArray = {
                id,
                name,
                category,
                String.valueOf(price),
                imagePath,
                String.valueOf(stock),
                String.valueOf(available)
            };

            Product newProduct = new Product(productArray, description);

            // Add to products.txt file
            appendProductToFile(newProduct);

            // Add description to descriptions.txt file
            appendDescriptionToFile(id, description);

            // Reload inventory and refresh display
            Inventory.loadFromFile();
            List<Product> products = Inventory.getAllProducts();
            productList.clear();
            productList.addAll(products);
            filterProducts();

            // Broadcast inventory update if server is running
            if (server != null && serverRunning) {
                server.refreshInventoryFromFile();
                server.broadcastInventoryUpdateMessage();
                System.out.println(" New product broadcasted to all clients");
                System.out.println(" Added new product: " + name + " (" + id + ")");
            }

            showAlert("Success", String.format("Product added successfully!\n\nID: %s\nName: %s\nCategory: %s\nPrice: $%.2f\nStock: %d\nBroadcasted to %d clients", id, name, category, price, stock, server != null ? server.getClientCount() : 0));
            System.out.println("New product added: " + name + " (" + id + ") with description");
        } catch (Exception e) {
            showAlert("Error", "Failed to add product: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Method to append product to file
    private void appendProductToFile(Product product) throws IOException {
        File productsFile = new File("data/products.txt");
        
        // Create the product line in the same format as existing products
        String productLine = String.format("%s|%s|%s|%.2f|%s|%d|%s",
            product.getId(),
            product.getName(),
            product.getCategory(),
            product.getPrice(),
            product.getImagePath(),
            product.getStockQuantity(),
            product.isAvailable()
        );
        
        try (FileWriter writer = new FileWriter(productsFile, true);
             BufferedWriter bufferedWriter = new BufferedWriter(writer)) {
            
            bufferedWriter.newLine(); // Add a new line before the new product
            bufferedWriter.write(productLine);
            
            System.out.println(" Product appended to products.txt: " + productLine);
        }
    }

    // Method to append description to file
    private void appendDescriptionToFile(String productId, String description) throws IOException {
        File descriptionsFile = new File("data/descriptions.txt");
        
        // Create the description line in the same format as existing descriptions
        String descriptionLine = productId + "|" + description;
        
        try (FileWriter writer = new FileWriter(descriptionsFile, true);
             BufferedWriter bufferedWriter = new BufferedWriter(writer)) {
            
            bufferedWriter.newLine(); // Add a new line before the new description
            bufferedWriter.write(descriptionLine);
            
            System.out.println(" Description appended to descriptions.txt: " + productId);
        }
    }

    @FXML
    private void loadOrders() {
        Platform.runLater(() -> {
            try {
                File ordersFile = new File("Backend/data/orders.txt");
                
                System.out.println("Trying to load orders from: " + ordersFile.getAbsolutePath());
                
                if (!ordersFile.exists()) {
                    ordersTextArea.setText("No orders file found at: " + ordersFile.getAbsolutePath());
                    orderCountLabel.setText("Orders: 0");
                    orderSelector.getItems().clear();
                    parsedOrders.clear();
                    System.out.println("Orders file does not exist: " + ordersFile.getAbsolutePath());
                    return;
                }
                
                StringBuilder content = new StringBuilder();
                parsedOrders.clear();
                orderSelector.getItems().clear();
                
                try (BufferedReader reader = new BufferedReader(new FileReader(ordersFile))) {
                    String line;
                    OrderInfo currentOrder = null;
                    StringBuilder orderText = new StringBuilder();
                    
                    while ((line = reader.readLine()) != null) {
                        content.append(line).append("\n");
                        
                        if (line.startsWith("Order Time:")) {
                            // Start of a new order - save previous order if exists
                            if (currentOrder != null) {
                                currentOrder.fullOrderText = orderText.toString();
                                parsedOrders.add(currentOrder);
                                orderSelector.getItems().add(currentOrder);
                            }
                            
                            String orderTime = line.substring(11).trim();
                            currentOrder = new OrderInfo(orderTime, "", "");
                            orderText = new StringBuilder();
                            orderText.append(line).append("\n");
                            
                        } else if (line.startsWith("Customer  :") && currentOrder != null) {
                            String customer = line.substring(11).trim();
                            currentOrder.customer = customer;
                            orderText.append(line).append("\n");
                            
                        } else if (line.startsWith("Address   :") && currentOrder != null) {
                            currentOrder.setAddress(line.substring(11).trim());
                            orderText.append(line).append("\n");
                            
                        } else if (line.startsWith("Contact   :") && currentOrder != null) {
                            currentOrder.setContact(line.substring(11).trim());
                            orderText.append(line).append("\n");
                            
                        } else if (line.startsWith("Post Code :") && currentOrder != null) {
                            currentOrder.setPostCode(line.substring(11).trim());
                            orderText.append(line).append("\n");
                            
                        } else if (line.startsWith("Username  :") && currentOrder != null) {
                            currentOrder.setUsername(line.substring(11).trim());
                            orderText.append(line).append("\n");
                            
                        } else if (line.startsWith("Total: $") && currentOrder != null) {
                            currentOrder.setTotal(line.substring(8).trim());
                            orderText.append(line).append("\n");
                            
                        } else {
                            // Add all other lines to the order text
                            orderText.append(line).append("\n");
                        }
                    }
                    
                    // Don't forget the last order
                    if (currentOrder != null) {
                        currentOrder.fullOrderText = orderText.toString();
                        parsedOrders.add(currentOrder);
                        orderSelector.getItems().add(currentOrder);
                    }
                }
                
                if (content.length() == 0) {
                    ordersTextArea.setText("Orders file is empty.");
                } else {
                    ordersTextArea.setText(content.toString());
                }
                
                orderCountLabel.setText("Orders: " + parsedOrders.size());
                ordersTextArea.setScrollTop(Double.MAX_VALUE);
                
                System.out.println(" Loaded " + parsedOrders.size() + " orders from: " + ordersFile.getAbsolutePath());
                
            } catch (IOException e) {
                String errorMsg = "Error loading orders: " + e.getMessage();
                ordersTextArea.setText(errorMsg);
                orderCountLabel.setText("Orders: Error");
                System.err.println(" " + errorMsg);
                e.printStackTrace();
            }
        });
    }

    @FXML
    private void proceedOrder() {
        OrderInfo selectedOrder = orderSelector.getValue();
        if (selectedOrder == null) {
            showAlert("No Selection", "Please select an order to proceed.");
            return;
        }
        
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Proceed Order");
        confirmAlert.setHeaderText("Proceed with this order?");
        confirmAlert.setContentText("Customer: " + selectedOrder.getCustomer() + "\nOrder Time: " + selectedOrder.getOrderTime());
        
        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // Add to proceeded.txt
                addToProceededFile(selectedOrder);
                
                // Remove from orders.txt
                removeOrderFromFile(selectedOrder);
                
                // Refresh the orders display
                loadOrders();
                
                showAlert("Success", "Order proceeded successfully!");
                
            } catch (Exception e) {
                showAlert("Error", "Failed to proceed order: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void removeOrder() {
        OrderInfo selectedOrder = orderSelector.getValue();
        if (selectedOrder == null) {
            showAlert("No Selection", "Please select an order to remove.");
            return;
        }
        
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Remove Order");
        confirmAlert.setHeaderText("Remove this order?");
        confirmAlert.setContentText("Customer: " + selectedOrder.getCustomer() + "\nOrder Time: " + selectedOrder.getOrderTime() + "\n\nThis action cannot be undone.");
        
        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // Add to removed.txt
                addToRemovedFile(selectedOrder);
                
                // Remove from orders.txt
                removeOrderFromFile(selectedOrder);
                
                // Refresh the orders display
                loadOrders();
                
                showAlert("Success", "Order removed successfully!");
                
            } catch (Exception e) {
                showAlert("Error", "Failed to remove order: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void addToProceededFile(OrderInfo order) throws IOException {
        File proceededFile = new File("c:/Lucius FIles/backup/Java-Project/Backend/data/proceeded.txt");
        proceededFile.getParentFile().mkdirs();
        
        try (FileWriter writer = new FileWriter(proceededFile, true);
             BufferedWriter bufferedWriter = new BufferedWriter(writer)) {
            
            // Add processing timestamp
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            
            bufferedWriter.write("=== ORDER PROCEEDED ===\n");
            bufferedWriter.write("Processed Time: " + now.format(formatter) + "\n");
            bufferedWriter.write(order.getFullOrderText());
            bufferedWriter.write("========================\n\n");
        }
        
        System.out.println("✅ Order added to proceeded.txt");
    }

    private void addToRemovedFile(OrderInfo order) throws IOException {
        File removedFile = new File("data/removed.txt");
        removedFile.getParentFile().mkdirs();
        
        try (FileWriter writer = new FileWriter(removedFile, true);
             BufferedWriter bufferedWriter = new BufferedWriter(writer)) {
            
            // Add removal timestamp
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            
            bufferedWriter.write("=== ORDER REMOVED ===\n");
            bufferedWriter.write("Removed Time: " + now.format(formatter) + "\n");
            bufferedWriter.write(order.getFullOrderText());
            bufferedWriter.write("=====================\n\n");
        }
        
        System.out.println(" Order added to removed.txt");
    }

    private void removeOrderFromFile(OrderInfo orderToRemove) throws IOException {
        File ordersFile = new File("data/orders.txt");
        File tempFile = new File("Backend/data/orders_temp.txt");
        
        try (BufferedReader reader = new BufferedReader(new FileReader(ordersFile));
             FileWriter writer = new FileWriter(tempFile);
             BufferedWriter bufferedWriter = new BufferedWriter(writer)) {
            
            String line;
            boolean skipOrder = false;
            
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Order Time:")) {
                    // Check if this is the order to remove
                    String orderTime = line.substring(11).trim();
                    skipOrder = orderTime.equals(orderToRemove.getOrderTime());
                }
                
                if (!skipOrder) {
                    bufferedWriter.write(line + "\n");
                }
                
                
                if (line.equals("----------------------------------------")) {
                    skipOrder = false;
                }
            }
        }
        
        // Replace original file with temp file
        if (!ordersFile.delete()) {
            throw new IOException("Could not delete original orders file");
        }
        if (!tempFile.renameTo(ordersFile)) {
            throw new IOException("Could not rename temp file");
        }
        
        System.out.println("Order removed from orders.txt");
    }

    @FXML
    private void startServer() {
        if (!serverRunning) {
            serverThread = new Thread(() -> {
                try {
                    server = new ShopServer();
                    serverRunning = true;
                    Platform.runLater(() -> updateServerStatus());
                    server.start();
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        showAlert("Server Error", "Failed to start server: " + e.getMessage());
                        serverRunning = false;
                        updateServerStatus();
                    });
                }
            });
            serverThread.setDaemon(true);
            serverThread.start();
        }
    }

    @FXML
    private void stopServer() {
        if (server != null && serverRunning) {
            server.stop();
            serverRunning = false;
            updateServerStatus();
        }
    }

    private void updateServerStatus() {
        Platform.runLater(() -> {
            if (serverRunning) {
                serverStatusLabel.setText(" Server Running");
                serverStatusLabel.setStyle("-fx-text-fill: green;");
                startServerButton.setDisable(true);
                stopServerButton.setDisable(false);
            } else {
                serverStatusLabel.setText(" Server Stopped");
                serverStatusLabel.setStyle("-fx-text-fill: red;");
                startServerButton.setDisable(false);
                stopServerButton.setDisable(true);
            }
            
            if (server != null) {
                connectedClientsLabel.setText("Connected Clients: Available when server running");
            } else {
                connectedClientsLabel.setText("Connected Clients: 0");
            }
        });
    }

    private void startAutoRefresh() {
        Thread refreshThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(30000);
                    Platform.runLater(() -> {
                        loadProducts();
                        loadOrders();
                    });
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        refreshThread.setDaemon(true);
        refreshThread.start();
    }

    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public void shutdown() {
        if (server != null && serverRunning) {
            server.stop();
        }
    }
}