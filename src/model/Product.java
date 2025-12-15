package model;

import java.sql.Timestamp;
import java.util.List;

/**
 * Model class untuk tabel products
 */
public class Product {
    private int id;
    private int brandId;
    private int typeId;
    private int sizeId;
    private int colorId;
    private String name;
    private double sellingPrice;
    private double costPrice;
    private int stock;
    private String status;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    
    // Relational objects
    private Brand brand;
    private ProductType type;
    private Size size;
    private Color color;
    private List<ProductPhoto> photos;
    
    // Constructor
    public Product() {}
    
    // Getters & Setters
    public int getId() { 
        return id; 
    }
    
    public void setId(int id) { 
        this.id = id; 
    }
    
    public int getBrandId() { 
        return brandId; 
    }
    
    public void setBrandId(int brandId) { 
        this.brandId = brandId; 
    }
    
    public int getTypeId() { 
        return typeId; 
    }
    
    public void setTypeId(int typeId) { 
        this.typeId = typeId; 
    }
    
    public int getSizeId() { 
        return sizeId; 
    }
    
    public void setSizeId(int sizeId) { 
        this.sizeId = sizeId; 
    }
    
    public int getColorId() { 
        return colorId; 
    }
    
    public void setColorId(int colorId) { 
        this.colorId = colorId; 
    }
    
    public String getName() { 
        return name; 
    }
    
    public void setName(String name) { 
        this.name = name; 
    }
    
    public double getSellingPrice() { 
        return sellingPrice; 
    }
    
    public void setSellingPrice(double sellingPrice) { 
        this.sellingPrice = sellingPrice; 
    }
    
    public double getCostPrice() { 
        return costPrice; 
    }
    
    public void setCostPrice(double costPrice) { 
        this.costPrice = costPrice; 
    }
    
    public int getStock() { 
        return stock; 
    }
    
    public void setStock(int stock) { 
        this.stock = stock; 
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
    
    public Brand getBrand() { 
        return brand; 
    }
    
    public void setBrand(Brand brand) { 
        this.brand = brand; 
    }
    
    public ProductType getType() { 
        return type; 
    }
    
    public void setType(ProductType type) { 
        this.type = type; 
    }
    
    public Size getSize() { 
        return size; 
    }
    
    public void setSize(Size size) { 
        this.size = size; 
    }
    
    public Color getColor() { 
        return color; 
    }
    
    public void setColor(Color color) { 
        this.color = color; 
    }
    
    public List<ProductPhoto> getPhotos() { 
        return photos; 
    }
    
    public void setPhotos(List<ProductPhoto> photos) { 
        this.photos = photos; 
    }
    
    @Override
    public String toString() {
        return name;
    }
}