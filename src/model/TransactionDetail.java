package model;

import java.sql.Timestamp;

/**
 * Model class untuk tabel transaction_details
 */
public class TransactionDetail {
    private int id;
    private int transactionId;
    private int productId;
    private int quantity;
    private double unitPrice;
    private double subtotal;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    
    // Relational object
    private Product product;
    
    // Constructor
    public TransactionDetail() {}
    
    public TransactionDetail(int transactionId, int productId, int quantity, 
                            double unitPrice, double subtotal) {
        this.transactionId = transactionId;
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.subtotal = subtotal;
    }
    
    // Getters & Setters
    public int getId() { 
        return id; 
    }
    
    public void setId(int id) { 
        this.id = id; 
    }
    
    public int getTransactionId() { 
        return transactionId; 
    }
    
    public void setTransactionId(int transactionId) { 
        this.transactionId = transactionId; 
    }
    
    public int getProductId() { 
        return productId; 
    }
    
    public void setProductId(int productId) { 
        this.productId = productId; 
    }
    
    public int getQuantity() { 
        return quantity; 
    }
    
    public void setQuantity(int quantity) { 
        this.quantity = quantity; 
    }
    
    public double getUnitPrice() { 
        return unitPrice; 
    }
    
    public void setUnitPrice(double unitPrice) { 
        this.unitPrice = unitPrice; 
    }
    
    public double getSubtotal() { 
        return subtotal; 
    }
    
    public void setSubtotal(double subtotal) { 
        this.subtotal = subtotal; 
    }
    
    public Timestamp getCreatedAt() { 
        return createdAt; 
    }
    
    public void setCreatedAt(Timestamp createdAt) { 
        this.createdAt = createdAt; 
    }
    
    public Timestamp getUpdatedAt() { 
        return updatedAt; 
    }
    
    public void setUpdatedAt(Timestamp updatedAt) { 
        this.updatedAt = updatedAt; 
    }
    
    public Product getProduct() { 
        return product; 
    }
    
    public void setProduct(Product product) { 
        this.product = product; 
    }
}