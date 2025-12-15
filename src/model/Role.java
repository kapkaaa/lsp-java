package model;

import java.sql.Timestamp;

/**
 * Model class untuk tabel roles
 */
public class Role {
    private int id;
    private String name;
    private String information;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    
    // Constructor
    public Role() {}
    
    public Role(int id, String name) {
        this.id = id;
        this.name = name;
    }
    
    // Getters & Setters
    public int getId() { 
        return id; 
    }
    
    public void setId(int id) { 
        this.id = id; 
    }
    
    public String getName() { 
        return name; 
    }
    
    public void setName(String name) { 
        this.name = name; 
    }
    
    public String getInformation() { 
        return information; 
    }
    
    public void setInformation(String information) { 
        this.information = information; 
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
    
    @Override
    public String toString() {
        return name;
    }
}