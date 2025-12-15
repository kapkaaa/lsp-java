package model;

import java.sql.Timestamp;

/**
 * Model class untuk tabel product_photos
 */
public class ProductPhoto {
    private int id;
    private int productId;
    private String photoUrl;
    private boolean isPrimary;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    
    // Constructor
    public ProductPhoto() {}
    
    public ProductPhoto(int productId, String photoUrl, boolean isPrimary) {
        this.productId = productId;
        this.photoUrl = photoUrl;
        this.isPrimary = isPrimary;
    }
    
    // Getters & Setters
    public int getId() { 
        return id; 
    }
    
    public void setId(int id) { 
        this.id = id; 
    }
    
    public int getProductId() { 
        return productId; 
    }
    
    public void setProductId(int productId) { 
        this.productId = productId; 
    }
    
    public String getPhotoUrl() { 
        return photoUrl; 
    }
    
    public void setPhotoUrl(String photoUrl) { 
        this.photoUrl = photoUrl; 
    }
    
    public boolean isPrimary() { 
        return isPrimary; 
    }
    
    public void setPrimary(boolean primary) { 
        isPrimary = primary; 
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
}