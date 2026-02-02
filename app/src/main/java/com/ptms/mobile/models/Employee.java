package com.ptms.mobile.models;

/**
 * Modèle Employé
 */
public class Employee {
    private int id;
    private String code;
    private String firstname;
    private String lastname;
    private String email;
    private String department;
    private String position;
    private int employeeStatus; // Type d'employé: 2=MANAGER, 4=EMPLOYEE (INT depuis v2.0)
    private int type; // Type utilisateur: 1=ADMIN, 2=MANAGER, 3=ACCOUNTANT, 4=EMPLOYEE, 5=VIEWER
    private String gender;
    private int status;
    private String avatar;
    private String dateCreated;
    private String dateUpdated;

    // Constructeurs
    public Employee() {}

    public Employee(int id, String firstname, String lastname, String email) {
        this.id = id;
        this.firstname = firstname;
        this.lastname = lastname;
        this.email = email;
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getFirstname() { return firstname; }
    public void setFirstname(String firstname) { this.firstname = firstname; }

    public String getLastname() { return lastname; }
    public void setLastname(String lastname) { this.lastname = lastname; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public int getEmployeeStatus() { return employeeStatus; }
    public void setEmployeeStatus(int employeeStatus) { this.employeeStatus = employeeStatus; }

    public int getType() { return type; }
    public void setType(int type) {
        this.type = type;
        // Synchroniser employeeStatus avec type pour compatibilité
        if (this.employeeStatus == 0) {
            this.employeeStatus = type;
        }
    }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public String getDateCreated() { return dateCreated; }
    public void setDateCreated(String dateCreated) { this.dateCreated = dateCreated; }

    public String getDateUpdated() { return dateUpdated; }
    public void setDateUpdated(String dateUpdated) { this.dateUpdated = dateUpdated; }

    // Méthodes utilitaires
    public String getFullName() {
        if (firstname == null && lastname == null) {
            return "Utilisateur";
        } else if (firstname == null) {
            return lastname;
        } else if (lastname == null) {
            return firstname;
        } else {
            return firstname + " " + lastname;
        }
    }

    public boolean isActive() {
        return status == 1;
    }

    // Méthode pour obtenir le statut employé formaté (compatible v2.0)
    public String getEmployeeStatusText() {
        // Utiliser le champ type en priorité, puis employeeStatus
        int userType = (type != 0) ? type : employeeStatus;

        if (userType == 0) {
            return "Non défini";
        }

        // Types v2.0: 1=ADMIN, 2=MANAGER, 3=ACCOUNTANT, 4=EMPLOYEE, 5=VIEWER
        switch (userType) {
            case 1:
                return "Administrateur";
            case 2:
                return "Gestionnaire";
            case 3:
                return "Comptable";
            case 4:
                return "Employé";
            case 5:
                return "Observateur";
            default:
                return "Type " + userType;
        }
    }

    // Méthode pour obtenir la couleur du statut (compatible v2.0)
    public int getEmployeeStatusColor(android.content.Context context) {
        // Utiliser le champ type en priorité, puis employeeStatus
        int userType = (type != 0) ? type : employeeStatus;

        if (userType == 0) {
            return context.getResources().getColor(android.R.color.darker_gray);
        }

        // Types v2.0: 1=ADMIN, 2=MANAGER, 3=ACCOUNTANT, 4=EMPLOYEE, 5=VIEWER
        switch (userType) {
            case 1: // ADMIN
                return context.getResources().getColor(android.R.color.holo_red_dark);
            case 2: // MANAGER
                return context.getResources().getColor(android.R.color.holo_orange_dark);
            case 3: // ACCOUNTANT
                return context.getResources().getColor(android.R.color.holo_blue_dark);
            case 4: // EMPLOYEE
                return context.getResources().getColor(android.R.color.holo_green_dark);
            case 5: // VIEWER
                return context.getResources().getColor(android.R.color.holo_purple);
            default:
                return context.getResources().getColor(android.R.color.darker_gray);
        }
    }

    // Méthodes utilitaires pour vérifier le type
    public boolean isAdmin() {
        int userType = (type != 0) ? type : employeeStatus;
        return userType == 1;
    }

    public boolean isManager() {
        int userType = (type != 0) ? type : employeeStatus;
        return userType == 2;
    }

    public boolean isAccountant() {
        int userType = (type != 0) ? type : employeeStatus;
        return userType == 3;
    }

    public boolean isEmployee() {
        int userType = (type != 0) ? type : employeeStatus;
        return userType == 4;
    }

    public boolean isViewer() {
        int userType = (type != 0) ? type : employeeStatus;
        return userType == 5;
    }
}






