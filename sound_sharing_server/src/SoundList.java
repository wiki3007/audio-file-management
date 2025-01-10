import java.util.ArrayList;

public class SoundList {
    private int id, owner_id;
    private String name, description, type;
    ArrayList<SoundFile> soundFiles = new ArrayList<>();

    public SoundList(int id, int owner_id, String name, String description, String type) {
        this.id = id;
        this.owner_id = owner_id;
        this.name = name;
        this.description = description;
        this.type = type;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ArrayList<SoundFile> getFiles() {
        return soundFiles;
    }

    public void setFiles(ArrayList<SoundFile> soundFiles) {
        this.soundFiles = soundFiles;
    }

    public String addFile(SoundFile soundFile)
    {
        for (SoundFile temp: soundFiles)
        {
            if (soundFile.getId() == temp.getId())
            {
                temp = soundFile;
                return "FILE_REPLACED";
            }
        }
        soundFiles.add(soundFile);
        return "FILE_ADDED";
    }

    public ArrayList<String> getArrayOfElements()
    {
        ArrayList<String> elements = new ArrayList<>();

        elements.add(String.valueOf(id));
        elements.add(String.valueOf(owner_id));
        elements.add(name);
        elements.add(description);
        elements.add(type);

        return elements;
    }

}
