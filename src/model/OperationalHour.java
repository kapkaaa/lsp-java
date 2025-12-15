package model;

import java.sql.Timestamp;

/**
 * Model class untuk tabel operational_hours
 */
public class OperationalHour {
    private int id;
    private String serviceType;
    private String day;
    private String openTime;
    private String closeTime;
    private String status;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    
    // Constructor
    public OperationalHour() {}
    
    public OperationalHour(String serviceType, String day, String openTime, 
                          String closeTime, String status) {
        this.serviceType = serviceType;
        this.day = day;
        this.openTime = openTime;
        this.closeTime = closeTime;
        this.status = status;
    }
    
    // Getters & Setters
    public int getId() { 
        return id; 
    }
    
    public void setId(int id) { 
        this.id = id; 
    }
    
    public String getServiceType() { 
        return serviceType; 
    }
    
    public void setServiceType(String serviceType) { 
        this.serviceType = serviceType; 
    }
    
    public String getDay() { 
        return day; 
    }
    
    public void setDay(String day) { 
        this.day = day; 
    }
    
    public String getOpenTime() { 
        return openTime; 
    }
    
    public void setOpenTime(String openTime) { 
        this.openTime = openTime; 
    }
    
    public String getCloseTime() { 
        return closeTime; 
    }
    
    public void setCloseTime(String closeTime) { 
        this.closeTime = closeTime; 
    }
    
    public String getStatus() { 
        return status; 
    }
    
    public void setStatus(String status) { 
        this.status = status; 
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
        return day + " - " + status;
    }
}