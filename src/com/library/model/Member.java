package com.library.model;

public class Member {
    private String id;
    private String name;
    private String email;
    private String membershipId;
    private String password;
    private String phone;
    private String role; // "MEMBER" or "LIBRARIAN"
    private String joinedDate;
    private boolean prefDueDateAlerts = true;
    private boolean prefNewArrivalAlerts = true;
    private boolean prefNewsletter = false;

    public Member() {}

    public Member(String id, String name, String email, String membershipId, String password, String phone, String role, String joinedDate) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.membershipId = membershipId;
        this.password = password;
        this.phone = phone;
        this.role = role;
        this.joinedDate = joinedDate;
        this.prefDueDateAlerts = true;
        this.prefNewArrivalAlerts = true;
        this.prefNewsletter = false;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getMembershipId() { return membershipId; }
    public void setMembershipId(String membershipId) { this.membershipId = membershipId; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getJoinedDate() { return joinedDate; }
    public void setJoinedDate(String joinedDate) { this.joinedDate = joinedDate; }

    public boolean isPrefDueDateAlerts() { return prefDueDateAlerts; }
    public void setPrefDueDateAlerts(boolean prefDueDateAlerts) { this.prefDueDateAlerts = prefDueDateAlerts; }

    public boolean isPrefNewArrivalAlerts() { return prefNewArrivalAlerts; }
    public void setPrefNewArrivalAlerts(boolean prefNewArrivalAlerts) { this.prefNewArrivalAlerts = prefNewArrivalAlerts; }

    public boolean isPrefNewsletter() { return prefNewsletter; }
    public void setPrefNewsletter(boolean prefNewsletter) { this.prefNewsletter = prefNewsletter; }
}
