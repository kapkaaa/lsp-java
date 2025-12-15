package model;

import java.sql.Timestamp;
import java.util.List;

/**
 * Model class untuk tabel transactions (Transaksi offline/kasir)
 */
public class Transaction {
    private int id;
    private int userId;
    private String transactionCode;
    private double total;
    private String paymentMethod;
    private String transactionStatus;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    
    // Relational objects
    private User user;
    private List<TransactionDetail> details;
    
    // Constructor
    public Transaction() {}
    
    public Transaction(String transactionCode, int userId, double total, String paymentMethod) {
        this.transactionCode = transactionCode;
        this.userId = userId;
        this.total = total;
        this.paymentMethod = paymentMethod;
    }
    
    // Getters & Setters
    public int getId() { 
        return id; 
    }
    
    public void setId(int id) { 
        this.id = id; 
    }
    
    public int getUserId() { 
        return userId; 
    }
    
    public void setUserId(int userId) { 
        this.userId = userId; 
    }
    
    public String getTransactionCode() { 
        return transactionCode; 
    }
    
    public void setTransactionCode(String transactionCode) { 
        this.transactionCode = transactionCode; 
    }
    
    public double getTotal() { 
        return total; 
    }
    
    public void setTotal(double total) { 
        this.total = total; 
    }
    
    public String getPaymentMethod() { 
        return paymentMethod; 
    }
    
    public void setPaymentMethod(String paymentMethod) { 
        this.paymentMethod = paymentMethod; 
    }
    
    public String getTransactionStatus() { 
        return transactionStatus; 
    }
    
    public void setTransactionStatus(String transactionStatus) { 
        this.transactionStatus = transactionStatus; 
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
    
    public User getUser() { 
        return user; 
    }
    
    public void setUser(User user) { 
        this.user = user; 
    }
    
    public List<TransactionDetail> getDetails() { 
        return details; 
    }
    
    public void setDetails(List<TransactionDetail> details) { 
        this.details = details; 
    }
    
    @Override
    public String toString() {
        return transactionCode;
    }
}