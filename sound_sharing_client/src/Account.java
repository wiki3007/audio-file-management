public class Account {
    private int id;
    private String name;
    /**
     * Type is divided into 3 levels: admin, standard and guest.
     *  Guest has very limited access to functions, mainly to just browse publicly available files.
     *  Standard can upload and download files as well as maintain his own lists.
     *  Admin can access other peoples private files and change their account types
     */
    private String type;

    public Account(int id, String name, String type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
