package frontend.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;

import java.net.URL;
import java.util.ResourceBundle;

public class ProductsInputController implements Initializable {
    
    @FXML
    private ChoiceBox<String> categoryChoiceBox;
    
    @FXML
    private ComboBox<String> productComboBox;
    
    @FXML
    private Button submitButton;
    
    @FXML
    private Button clearButton;
    
    @FXML
    private Label resultLabel;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize ChoiceBox with categories
        ObservableList<String> categories = FXCollections.observableArrayList(
            "Electronics", 
            "Clothing", 
            "Books", 
            "Home & Garden", 
            "Sports"
        );
        categoryChoiceBox.setItems(categories);
        categoryChoiceBox.setValue("Electronics"); // Set default value
        
        // Initialize ComboBox with products
        ObservableList<String> products = FXCollections.observableArrayList(
            "Laptop", 
            "Smartphone", 
            "Tablet", 
            "Headphones", 
            "Camera"
        );
        productComboBox.setItems(products);
        productComboBox.setPromptText("Choose a product...");
        
        // Listen for changes in category selection
        categoryChoiceBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            updateProductList(newValue);
        });
    }
    
    private void updateProductList(String category) {
        ObservableList<String> products = FXCollections.observableArrayList();
        
        switch (category) {
            case "Electronics":
                products.addAll("Laptop", "Smartphone", "Tablet", "Headphones", "Camera");
                break;
            case "Clothing":
                products.addAll("T-Shirt", "Jeans", "Dress", "Shoes", "Hat");
                break;
            case "Books":
                products.addAll("Fiction", "Non-Fiction", "Science", "History", "Biography");
                break;
            case "Home & Garden":
                products.addAll("Furniture", "Plants", "Tools", "Decorations", "Appliances");
                break;
            case "Sports":
                products.addAll("Football", "Basketball", "Tennis Racket", "Running Shoes", "Gym Equipment");
                break;
        }
        
        productComboBox.setItems(products);
        productComboBox.getSelectionModel().clearSelection();
        productComboBox.setPromptText("Choose a product...");
    }
    
    @FXML
    private void handleSubmit(ActionEvent event) {
        String selectedCategory = categoryChoiceBox.getValue();
        String selectedProduct = productComboBox.getValue();
        
        if (selectedCategory != null && selectedProduct != null) {
            resultLabel.setText("Selected: " + selectedCategory + " - " + selectedProduct);
            System.out.println("Category: " + selectedCategory);
            System.out.println("Product: " + selectedProduct);
        } else {
            resultLabel.setText("Please select both category and product!");
        }
    }
    
    @FXML
    private void handleClear(ActionEvent event) {
        categoryChoiceBox.getSelectionModel().clearSelection();
        productComboBox.getSelectionModel().clearSelection();
        resultLabel.setText("Selected: ");
    }
}