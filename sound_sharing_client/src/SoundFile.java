import java.util.ArrayList;

public class SoundFile {
    private int id, owner_id, size;
    String name, description, duration, format, type, date_added, path;

    public SoundFile(int id, int owner_id, String name, String description, String duration, int size,
                     String format, String type, String date_added) {
        this.id = id;
        this.owner_id = owner_id;
        this.name = name;
        this.description = description;
        this.duration = duration;
        this.size = size;
        this.format = format;
        this.type = type;
        this.date_added = date_added;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getOwner_id() {
        return owner_id;
    }

    public void setOwner_id(int owner_id) {
        this.owner_id = owner_id;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDate_added() {
        return date_added;
    }

    public void setDate_added(String date_added) {
        this.date_added = date_added;
    }

    public String getCompleteName()
    {
        return name + "." + format;
    }

    public String getPath() { return path; }

    public void setPath(String path) { this.path = path; }

    public ArrayList<String> getArrayOfElements()
    {
        ArrayList<String> elements = new ArrayList<>();

        elements.add(String.valueOf(id));
        elements.add(String.valueOf(owner_id));
        elements.add(name);
        elements.add(description);
        elements.add(duration);
        elements.add(String.valueOf(size));
        elements.add(format);
        elements.add(type);
        elements.add(date_added);

        return elements;
    }
}
