package model;

import java.sql.Timestamp;

/**
 * Model class untuk tabel users
 */
public class User {
    private int id;
    private int roleId;
    private String name;
    private String username;
    private String password;
    private String nik;
    private String address;
    private String city;
    private String phone;
    private String profilePhoto;
    private String status;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    
    // Relational object
    private Role role;
    
    // Constructor
    public User() {}
    
    public User(int id, String name, String username) {
        this.id = id;
        this.name = name;
        this.username = username;
    }
    
    // Getters & Setters
    public int getId() { 
        return id; 
    }
    
    public void setId(int id) { 
        this.id = id; 
    }
    
    public int getRoleId() { 
        return roleId; 
    }
    
    public void setRoleId(int roleId) { 
        this.roleId = roleId; 
    }
    
    public String getName() { 
        return name; 
    }
    
    public void setName(String name) { 
        this.name = name; 
    }
    
    public String getUsername() { 
        return username; 
    }
    
    public void setUsername(String username) { 
        this.username = username; 
    }
    
    public String getPassword() { 
        return password; 
    }
    
    public void setPassword(String password) { 
        this.password = password; 
    }
    
    public String getNik() { 
        return nik; 
    }
    
    public void setNik(String nik) { 
        this.nik = nik; 
    }
    
    public String getAddress() { 
        return address; 
    }
    
    public void setAddress(String address) { 
        this.address = address; 
    }
    
    public String getCity() { 
        return city; 
    }
    
    public void setCity(String city) { 
        this.city = city; 
    }
    
    public String getPhone() { 
        return phone; 
    }
    
    public void setPhone(String phone) { 
        this.phone = phone; 
    }
    
    public String getProfilePhoto() { 
        return profilePhoto; 
    }
    
    public void setProfilePhoto(String profilePhoto) { 
        this.profilePhoto = profilePhoto; 
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
    
    public Role getRole() { 
        return role; 
    }
    
    public void setRole(Role role) { 
        this.role = role; 
    }
    
    @Override
    public String toString() {
        return name + " (" + username + ")";
    }
}