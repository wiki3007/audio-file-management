import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.*;

/**
 * Remote Host Master Thread functions as a slave to Main Server.
 * Receives tasks from Main Server and executes them
 */
public class RemoteHostMasterThread implements Callable<String> {
    /**
     * ID of Remote Host Master Thread for identification by Main Server
     */
    private int sessionId = -1;
    public Account account = null;
    /**
     * ArrayList of tasks to execute converted to Future class
     */
    private ArrayList<Future<String>> taskArrayFuture = new ArrayList<>();
    /**
     * Executor to start new tasks
     */
    ExecutorService exec = Executors.newCachedThreadPool();
    /**
     * Socket to handle network communications
     */
    Socket socket;
    /**
     * Reader used to read messages coming over the network
     */
    BufferedReader receiveMsg;
    /**
     * Writer used to send messages over the network
     */
    PrintWriter sendMsg;
    /**
     * List of commands received from main server
     */
    ArrayList<String> commandsList = new ArrayList<>();
    /**
     * Index of next-to-execute command
     */
    int commandsListIndex = 0;
    /**
     * Contains the data of sound file
     */
    byte[] lastFileData;

    private ArrayList<SoundFile> listOfSoundFiles = new ArrayList<>();
    private ArrayList<SoundList> listOfSoundLists = new ArrayList<>();

    /**
     * All temporary lists have negative indexes
     */
    private int tempListIndex = -1;


    /**
     * Creates a new remote host master thread object.
     * Responsible for network communications and handling the threads that later have their results returned to the server
     * @param serverAddress Address of the server the remote host communicates with
     * @param port Network port over which remote host communicates with
     * @throws IOException If stuff breaks
     */
    public RemoteHostMasterThread(InetAddress serverAddress, int port) throws IOException {

        this.socket = new Socket(serverAddress, port);
        socket.setSoTimeout(1000);
        receiveMsg = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        sendMsg = new PrintWriter(socket.getOutputStream(), true);

        System.out.println("test");
        while (sessionId == -1) // must get the ID before proceeding
        {
            try
            {
                sessionId = Integer.parseInt(receiveMsg.readLine());
                System.out.println("Assigned host ID: " + sessionId);
            }
            catch (SocketTimeoutException waitTooLong)
            {
                // just try again lmao
            }
        }
        //socket.setSoTimeout(60000);
    }

    /**
     * Exchanges commands
     */
    private void readCommands() throws IOException {
        String command = "";
        try
        {
            while (true) // keep reading until .readLine throws a timeout
            {
                command = receiveMsg.readLine();
                commandsList.add(command);
            }
        }
        catch (SocketTimeoutException waitTooLong)
        {
            // means no more messages to read, so just go on with your life
            System.out.println(waitTooLong);
        }
    }

    private String readMsg()
    {
        String command = "";
        try
        {
            command = receiveMsg.readLine();
        }
        catch (IOException waitTooLong)
        {
            System.out.println(waitTooLong);
        }
        return command;
    }

    /**
     * Sends the raw data of file with given path
     * @param path Path of file to send
     * @return True if file sent, false otherwise
     * @throws IOException
     */
    boolean sendFileData(String path) throws IOException {
        System.out.println("Begin sending file...");
        if (path.endsWith(".flac") || path.endsWith(".mp3") || path.endsWith(".ogg") || path.endsWith(".wav") || path.endsWith(".wma") || path.endsWith(".webm"))
        {
            File file = new File(path);
            System.out.println("sendFile path: " + path);
            if (!file.exists()) return false;
            FileInputStream fileInputStream = new FileInputStream(file);
            sendMsg.println("SENDING_FILE_DATA");
            sendMsg.println(file.getName());
            sendMsg.println(file.length());

            byte[] buffer = fileInputStream.readNBytes((int) file.length());
            String bufferString = Arrays.toString(buffer);
            sendMsg.println(bufferString);
            System.out.println("sendFile len: " + file.length());
            System.out.println("buffer size: " + buffer.length);
            System.out.println("file test true");
            return true;
        }
        return false;
    }

    /**
     * Writes the raw data of file from the buffer
     * @param name Name of file to save
     * @param download whether the file is set to download or temporary
     * @return True, IDK
     * @throws IOException
     */
    boolean writeFileData(String name, boolean download) throws IOException {
        File file;
        if (download)
        {
            file = new File("./downloads/" + name);
        }
        else
        {
            file = new File("./tmp/" + name);
            file.deleteOnExit();
        }

        if (file.exists())
        {
            file.delete();
        }
        file.createNewFile();
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        fileOutputStream.write(lastFileData);
        return true;
    }

    /**
     * Flushes all contents of commandsList and sets index to 0
     */
    private void flushCommandList()
    {
        commandsList = new ArrayList<>();
        commandsListIndex = 0;
    }

    /**
     * Receives all applicable files from the server and puts them in listOfFiles array, or updates if already exists
     */
    void receiveFiles(){
        if (account.getType().equals("guest")) sendMsg.println("RECEIVE_FILES_PUBLIC");
        else sendMsg.println("RECEIVE_FILES");

        int amount = Integer.parseInt(readMsg());
        for (int i=0; i<amount; i++)
        {
            SoundFile temp = createFileFromCommandsList();
            boolean fileFound = false;
            for (int j = 0; j< listOfSoundFiles.size(); j++)
            {
                if (listOfSoundFiles.get(j).getId() == temp.getId())
                {
                    fileFound = true;
                    listOfSoundFiles.set(j, temp);
                    break;
                }
            }
            if (!fileFound) listOfSoundFiles.add(temp);
        }
    }

    /**
     * Receives all applicable lists from the server and puts them in listOfLists array, or updates if already exist
     */
    void receiveLists(){
        if (account.getType().equals("guest")) sendMsg.println("RECEIVE_LISTS_PUBLIC");
        sendMsg.println("RECEIVE_LISTS");

        do
        {
            int listAmount = Integer.parseInt(readMsg());
            for (int i=0; i<listAmount; i++)
            {
                int listId = Integer.parseInt(readMsg());
                int listOwnerId = Integer.parseInt(readMsg());
                String listName = readMsg();
                String listDescription = readMsg();
                String listType = readMsg();
                SoundList soundListTemp = new SoundList(listId, listOwnerId, listName, listDescription, listType);

                int fileAmount = Integer.parseInt(readMsg());
                ArrayList<SoundFile> tempSoundFiles = new ArrayList<>();
                for (int j=0; j<fileAmount; j++)
                {
                    SoundFile temp = createFileFromCommandsList();
                    boolean fileFound = false;
                    for (int k = 0; k< listOfSoundFiles.size(); k++)
                    {
                        if (listOfSoundFiles.get(k).getId() == temp.getId())
                        {
                            fileFound = true;
                            listOfSoundFiles.set(k, temp);
                            break;
                        }
                    }
                    if (!fileFound) listOfSoundFiles.add(temp);
                    tempSoundFiles.add(temp);
                }
                soundListTemp.setFiles(tempSoundFiles);

                boolean listFound = false;
                for (int j = 0; j< listOfSoundLists.size(); j++)
                {
                    if (listOfSoundLists.get(j).getId() == soundListTemp.getId())
                    {
                        listFound = true;
                        listOfSoundLists.set(j, soundListTemp);
                        break;
                    }
                }
                if (!listFound) listOfSoundLists.add(soundListTemp);
            }

        } while (commandsListIndex < commandsList.size());
    }

    /**
     * Gets an object of File class with given id, only those belonging to this user
     * @param id id of file to get
     * @return Object of File class
     */
    SoundFile getOwnFile(int id)
    {
        if (account.getType().equals("guest"))
        {
            System.out.println("PERMISSION DENIED FOR GUEST ACCOUNT");
            return null;
        }
        for (int i = 0; i< listOfSoundFiles.size(); i++)
        {
            if (listOfSoundFiles.get(i).getId() == id)
            {
                return listOfSoundFiles.get(i);
            }
        }
        return null;
    }

    /**
     * Gets an ArrayList of File class objects with owner_id of this account, only those belonging to this user
     * @return ArrayList of File class objects
     */
    ArrayList<SoundFile> getOwnFileArray()
    {
        if (account.getType().equals("guest"))
        {
            System.out.println("PERMISSION DENIED FOR GUEST ACCOUNT");
            return null;
        }
        ArrayList<SoundFile> array = new ArrayList<>();

        for (int i = 0; i< listOfSoundFiles.size(); i++)
        {
            if (listOfSoundFiles.get(i).getOwner_id() == account.getId())
            {
                array.add(listOfSoundFiles.get(i));
            }
        }

        return array;
    }

    /**
     * Gets an ArrayList of File class objects with given name, duplicate names allowed, only those belonging to this user
     * @param name Name of File to get
     * @return ArrayList of File class objects
     */
    ArrayList<SoundFile> getOwnFileArray(String name)
    {
        if (account.getType().equals("guest"))
        {
            System.out.println("PERMISSION DENIED FOR GUEST ACCOUNT");
            return null;
        }
        ArrayList<SoundFile> array = new ArrayList<>();

        for (int i = 0; i< listOfSoundFiles.size(); i++)
        {
            if (listOfSoundFiles.get(i).getName().equals(name))
            {
                array.add(listOfSoundFiles.get(i));
            }
        }

        return array;
    }

    /**
     * Gets an ArrayList of File class objects with type public, only those that don't belong to this user.
     *  If you want an array of public files owned by this user, filter through the GetOwnFileArray results
     * @return ArrayList of File class objects
     */
    ArrayList<SoundFile> getPublicFileArray()
    {
        ArrayList<SoundFile> array = new ArrayList<>();

        for (int i = 0; i< listOfSoundFiles.size(); i++)
        {
            if (listOfSoundFiles.get(i).getOwner_id() != account.getId() && listOfSoundFiles.get(i).getType().equals("public"))
            {
                array.add(listOfSoundFiles.get(i));
            }
        }
        return array;
    }

    /**
     * Gets an object of List class with given id, only those belonging to this user
     * @param id id of list to get
     * @return Object of List class
     */
    SoundList getOwnList(int id)
    {
        if (account.getType().equals("guest"))
        {
            System.out.println("PERMISSION DENIED FOR GUEST ACCOUNT");
            return null;
        }
        for (int i = 0; i< listOfSoundLists.size(); i++)
        {
            if (listOfSoundLists.get(i).getId() == id)
            {
                return listOfSoundLists.get(i);
            }
        }
        return null;
    }

    /**
     * Gets an ArrayList of List class objects with owner_id of this account, only those belonging to this user
     * @return ArrayList of List class objects
     */
    ArrayList<SoundList> getOwnListArray()
    {
        ArrayList<SoundList> array = new ArrayList<>();

        for (int i = 0; i< listOfSoundLists.size(); i++)
        {
            if (listOfSoundLists.get(i).getOwner_id() == account.getId())
            {
                array.add(listOfSoundLists.get(i));
            }
        }

        return array;
    }

    /**
     * Gets an ArrayList of List class objects with given name, duplicate names allowed, only those belonging to this user
     * @param name Name of list to get
     * @return ArrayList of List class objects
     */
    ArrayList<SoundList> getOwnListArray(String name)
    {
        ArrayList<SoundList> array = new ArrayList<>();

        for (int i = 0; i< listOfSoundLists.size(); i++)
        {
            if (listOfSoundLists.get(i).getName().equals(name))
            {
                array.add(listOfSoundLists.get(i));
            }
        }

        return array;
    }

    /**
     * Gets an ArrayList of List class objects with type public, only those that don't belong to this user.
     *  If you want an array of public files owned by this user, filter through the GetOwnListArray results
     * @return ArrayList of List class objects
     */
    ArrayList<SoundList> getPublicListArray()
    {
        ArrayList<SoundList> array = new ArrayList<>();

        for (int i = 0; i< listOfSoundLists.size(); i++)
        {
            if (listOfSoundLists.get(i).getOwner_id() != account.getId() && listOfSoundLists.get(i).getType().equals("public"))
            {
                array.add(listOfSoundLists.get(i));
            }
        }
        return array;
    }

    /**
     * Reads the commandList buffer to create a File object
     * @return File class object
     */
    SoundFile createFileFromCommandsList(){
        /*
        int id = Integer.parseInt(commandsList.get(commandsListIndex++));
        int owner_id = Integer.parseInt(commandsList.get(commandsListIndex++));
        String name = commandsList.get(commandsListIndex++);
        String description = commandsList.get(commandsListIndex++);
        String duration = commandsList.get(commandsListIndex++);
        int size = Integer.parseInt(commandsList.get(commandsListIndex++));
        String format = commandsList.get(commandsListIndex++);
        String type = commandsList.get(commandsListIndex++);
        String date_added = commandsList.get(commandsListIndex++);
         */
        int id = Integer.parseInt(readMsg());
        int owner_id = Integer.parseInt(readMsg());
        String name = readMsg();
        String description = readMsg();
        String duration = readMsg();
        int size = Integer.parseInt(readMsg());
        String format = readMsg();
        String type = readMsg();
        String date_added = readMsg();
        return new SoundFile(id, owner_id, name, description, duration, size, format, type, date_added);
    }

    /**
     * Manual procedure that asks the user for manual input until correct login.
     * Procedure that must be completed before user is allowed to move onto the rest of the app
     *  Asks for login and password. If login is "guest" (case-insensitive), then password is autofilled as "guest"
     *  If server successfully matches the given login and password, then it returns account values
     *  If login doesn't exist, then asks reask and resend both login and password.
     *  If password is incorrect for given login, reask for password.
     * @return false if communication error with server, true if account properly assigned
     */
    boolean loginProcedureManual(){
        Scanner in = new Scanner(System.in);
        System.out.print("Login: ");
        String login = in.next();
        String password = "guest";
        if (!login.equalsIgnoreCase("guest"))
        {
            System.out.print("\nPassword: ");
            password = in.next();
        }
        System.out.println();
        sendMsg.println("LOGIN_REQUEST");
        sendMsg.println(login);
        sendMsg.println(password);

        do
        {
            String confirmation = readMsg();
            if (confirmation.equals("LOGIN_CORRECT"))
            {
                int loginId = Integer.parseInt(readMsg());
                String loginType = readMsg();
                account = new Account(loginId, login, loginType);
                break;
            }
            else if (confirmation.equals("PASSWORD_ERROR"))
            {
                System.out.println("Password: ");
                password = in.next();
                commandsListIndex = commandsList.size();
                sendMsg.println("LOGIN_REQUEST");
                sendMsg.println(login);
                sendMsg.println(password);
            }
            else if (confirmation.equals("LOGIN_ERROR"))
            {
                System.out.println("Login: ");
                login = in.next();
                password = "guest";
                if (!login.equalsIgnoreCase("guest"))
                {
                    System.out.println("Password: ");
                    password = in.next();
                }
                commandsListIndex = commandsList.size();
                sendMsg.println("LOGIN_REQUEST");
                sendMsg.println(login);
                sendMsg.println(password);
                }
        } while(account == null);

        return true;
    }

    /**
     * Argument procedure that uses given arguments once
     * Procedure that must be completed before user is allowed to move onto the rest of the app
     *  Asks for login and password. If login is "guest" (case-insensitive), then password is autofilled as "guest"
     *  If server successfully matches the given login and password, then it gives account values and returns true
     *  If login doesn't exist, then return false
     *  If password is incorrect for given login, then return false
     * @param login Name of account to log into
     * @param password Password of said account
     * @return false if communication error with server or unsuccessful log in, true if account properly assigned
     */
    boolean loginProcedureArg(String login, String password){
        sendMsg.println("LOGIN_REQUEST");
        sendMsg.println(login);
        sendMsg.println(password);
        do
        {
            String confirmation = readMsg();
            System.out.println(confirmation);
            if (confirmation.equals("LOGIN_CORRECT"))
            {
                int loginId = Integer.parseInt(readMsg());
                String loginType = readMsg();
                account = new Account(loginId, login, loginType);
                return true;
            }
            else if (confirmation.equals("PASSWORD_ERROR"))
            {
                return false;
            }
            else if (confirmation.equals("LOGIN_ERROR"))
            {
                return false;
            }
        } while (account == null);

        return true;
    }

    /**
     * Manual procedure that asks the user for manual input until correct registration.
     * Registers user into the server
     *  Asks for login and password, login can't be "guest" (case-insensitive), password can't be empty
     *  If server successfully registers, then it gives account values
     *  If login exists, then asks reask and resend both login and password.
     * @return false if communication error with server, true if account properly assigned
     */
    boolean registerProcedureManual(){
        Scanner in = new Scanner(System.in);
        String login, password;
        do
        {
            System.out.print("Login: ");
            login = in.next();
        } while (login.equalsIgnoreCase("guest"));

        do
        {
            System.out.print("\nPassword: ");
            password = in.next();
        } while (password.isEmpty());
        System.out.println();
        sendMsg.println("REGISTER_REQUEST");
        sendMsg.println(login);
        sendMsg.println(password);

        do
        {
            String confirmation = readMsg();
            if (confirmation.equals("REGISTER_CORRECT"))
            {
                int loginId = Integer.parseInt(readMsg());
                String loginType = readMsg();
                account = new Account(loginId, login, loginType);
                break;
            }
            else if (confirmation.equals("REGISTER_ERROR"))
            {
                System.out.println("Password: ");
                password = in.next();
                commandsListIndex = commandsList.size();
                sendMsg.println("REGISTER_REQUEST");
                sendMsg.println(login);
                sendMsg.println(password);
            }
        } while(account == null);

        return true;
    }

    /**
     * Argument procedure that uses given arguments once
     * Registers user into the server
     *  Uses given login and password, login can't be "guest" (case-insensitive), password can't be empty
     *  If server successfully registers, then it gives account values
     *  If login exists, then asks reask and resend both login and password.
     * @return false if communication error with server, true if account properly assigned
     */
    boolean registerProcedureArgs(String login, String password){
        sendMsg.println("REGISTER_REQUEST");
        sendMsg.println(login);
        sendMsg.println(password);
        do
        {
            String confirmation = readMsg();
            if (confirmation.equals("REGISTER_CORRECT"))
            {
                int loginId = Integer.parseInt(readMsg());
                String loginType = readMsg();
                account = new Account(loginId, login, loginType);
                return true;
            }
            else if (confirmation.equals("REGISTER_ERROR"))
            {
                return false;
            }
        } while(account == null);

        return true;
    }

    /**
     * Creates a temporary list, mainly for guests, but I guess you can do it for other users too?
     *  All lists with negative indexes are classified as temporary, as well as a custom type that won't go into the database, even if you tried, enum FTW
     * @param name Name of temporary list
     * @param description Description of temporary list
     * @return A List class object with negative ID and "temporary" type
     */
    SoundList makeTempList(String name, String description)
    {
        return new SoundList(tempListIndex--, account.getId(), name, description, "temporary");
    }

    /**
     * Creates a list and registers it with the server
     * @param name Name of list
     * @param description Description of list
     * @param type Type of list, "public" or "private"
     * @param soundFiles ArrayList of File objects to add to the list
     * @return A new list
     * @throws IOException if error communicationg with server
     * @throws InterruptedException idk
     */
    SoundList addList(String name, String description, String type, ArrayList<SoundFile> soundFiles){
        if (account.getType().equals("guest"))
        {
            System.out.println("PERMISSION DENIED");
            return null;
        }
        sendMsg.println("REGISTER_LIST");
        //sendMsg.println(account.getId());
        sendMsg.println(name);
        sendMsg.println(description);
        sendMsg.println(type);
        sendMsg.println(soundFiles.size());
        for (int i = 0; i< soundFiles.size(); i++)
        {
            sendMsg.println(soundFiles.get(i).getId());
        }

        SoundList soundList = null;
        String command = readMsg();
        if (command.equals("REGISTER_LIST_CORRECT"))
        {
            int listId = Integer.parseInt(readMsg());
            soundList = new SoundList(listId, account.getId(), name, description, type);
            soundList.setFiles(soundFiles);
            listOfSoundLists.add(soundList);
            return soundList;
        }
        else return null;
    }

    /**
     * Registers a file with the server, and if successful, adds it locally
     * Auto-fills date_added as "null" string
     * @param name Name of file
     * @param description Description of file
     * @param duration Play duration of file
     * @param size Size of file in KB
     * @param format Format of file
     * @param type Type of file, "private" or "public"
     * @param path Path to file to send its data to server
     * @return The newly added file or null if encountered error
     */
    SoundFile addFile(String name, String description, String duration, int size, String format, String type, String path) throws IOException {
        return addFile(name, description, duration, size, format, type, "null", path);
    }
    /**
     * Registers a file with the server, and if successful, adds it locally
     * @param name Name of file
     * @param description Description of filde
     * @param duration Play duration of file
     * @param size Size of file in KB
     * @param format Format of file
     * @param type Type of file, "private" or "public"
     * @param date_added Date added of file, DB autofills it as current_timestamp, give null unless you have a plan
     * @param path Path to file to send its data to server
     * @return The newly added file or null if encountered error
     */
    SoundFile addFile(String name, String description, String duration, int size, String format, String type, String date_added, String path) throws IOException {
        if (account.getType().equals("guest"))
        {
            System.out.println("PERMISSION DENIED");
            return null;
        }

        name = name.substring(0, name.length() - format.length() - 1);
        System.out.println("subed name: " + name);
        for (int i=0; i<path.length(); i++)
        {
            char test = path.charAt(i);
            path = path.substring(i+1);
            if (test == '/') break;
        }
        path = path.replace("%20", " ");
        //System.out.println("path: " + path);

        sendMsg.println("REGISTER_FILE");
        //sendMsg.println(account.getId());
        sendMsg.println(name);
        sendMsg.println(description);
        sendMsg.println(duration);
        sendMsg.println(size);
        sendMsg.println(format);
        sendMsg.println(type);
        sendMsg.println(date_added);
        sendFileData(path);

        String command = readMsg();
        if (command.equals("REGISTER_FILE_CORRECT"))
        {
            int fileId = Integer.parseInt(readMsg());
            SoundFile soundFile = new SoundFile(fileId, account.getId(), name, description, duration, size, format, type, date_added);
            listOfSoundFiles.add(soundFile);
            return soundFile;
        }
        else return null;
    }

    /**
     * Reports deletion to server, server either approves or not
     * @param soundFile File to delete
     * @return true if file successfully deleted, false if not
     */
    boolean deleteFile (SoundFile soundFile){
        if (account.getType().equals("guest"))
        {
            System.out.println("PERMISSION DENIED");
            return false;
        }

        if (soundFile.getOwner_id() != account.getId()) return false;
        sendMsg.println("FILE_DELETE");
        sendMsg.println(soundFile.getId());

        String command = readMsg();
        if (command.equals("FILE_DELETE_APPROVED"))
        {
            for (SoundList soundList : listOfSoundLists)
            {
                soundList.getFiles().remove(soundFile);
            }
            listOfSoundFiles.remove(soundFile);
            soundFile = null;
            return true;
        }
        else return false;
    }

    /**
     * Shares a given file with a user of given ID. Sharing public files is allowed, in case they go private
     * @param soundFile File to share
     * @param shareWith ID of user to share with
     * @return True if share request sent (doesn't mean approved), False otherwise
     */
    boolean shareFile (SoundFile soundFile, int shareWith)
    {
        if (account.getType().equals("guest"))
        {
            System.out.println("PERMISSION DENIED");
            return false;
        }

        if (soundFile.getOwner_id() != account.getId()) return false;

        sendMsg.println("FILE_SHARE_REQUEST");
        sendMsg.println(soundFile.getId());
        sendMsg.println(shareWith);

        return true;
    }

    /**
     * Deletes a given list, both from user and server
     * @param soundList List to delete
     * @return True if server approved delete and user deleted, False otherwise
     */
    boolean deleteList(SoundList soundList){
        if (account.getType().equals("guest"))
        {
            System.out.println("PERMISSION DENIED");
            return false;
        }

        if (soundList.getOwner_id() != account.getId()) return false;
        sendMsg.println("LIST_DELETE");
        sendMsg.println(soundList.getId());

        String command = readMsg();
        if (command.equals("LIST_DELETE_APPROVED"))
        {
            listOfSoundLists.remove(soundList);
            soundList = null;
            return true;
        }
        else return false;
    }

    /**
     * Adds a given file to the given list
     * @param soundList List to add file to
     * @param soundFile File to add to list
     * @return True if server agrees, false otherwise
     */
    boolean addFileToList(SoundList soundList, SoundFile soundFile){
        if (account.getType().equals("guest"))
        {
            System.out.println("PERMISSION DENIED");
            return false;
        }
        if (soundList.getOwner_id() != account.getId()) return false;
        if (soundFile.getOwner_id() != account.getId()) return false;

        sendMsg.println("ADD_FILE_TO_LIST");
        sendMsg.println(soundList.getId());
        sendMsg.println(soundFile.getId());

        String command = readMsg();
        if (command.equals("ADD_FILE_TO_LIST_APPROVED"))
        {
            return true;
        }
        else return false;
    }

    /**
     * Removes a given file from the given list
     * @param soundList List to remove file to
     * @param soundFile File to remove to list
     * @return True if server agrees, false otherwise
     */
    boolean removeFileFromList(SoundList soundList, SoundFile soundFile){
        if (account.getType().equals("guest"))
        {
            System.out.println("PERMISSION DENIED");
            return false;
        }
        if (soundList.getOwner_id() != account.getId()) return false;
        if (soundFile.getOwner_id() != account.getId()) return false;

        sendMsg.println("REMOVE_FILE_FROM_LIST");
        sendMsg.println(soundList.getId());
        sendMsg.println(soundFile.getId());

        String command = readMsg();
        if (command.equals("REMOVE_FILE_FROM_LIST_APPROVED"))
        {
            return true;
        }
        else return false;
    }

    /**
     * Unshares a file with a user
     * @param soundFile File to unshare
     * @param who ID of user to unshare
     * @return True if server agrees, false otherwise
     */
    boolean unshareFileFromUser(SoundFile soundFile, int who){
        if (account.getType().equals("guest"))
        {
            System.out.println("PERMISSION DENIED");
            return false;
        }
        if (soundFile.getOwner_id() != account.getId()) return false;

        sendMsg.println("FILE_UNSHARE_REQUEST");
        sendMsg.println(soundFile.getId());
        sendMsg.println(who);

        String command = readMsg();
        if (command.equals("FILE_UNSHARE_APPROVED"))
        {
            return true;
        }
        else return false;
    }

    boolean unshareFileFromAll(SoundFile soundFile){
        if (account.getType().equals("guest"))
        {
            System.out.println("PERMISSION DENIED");
            return false;
        }
        if (soundFile.getOwner_id() != account.getId()) return false;

        sendMsg.println("FILE_UNSHARE_ALL_REQUEST");
        sendMsg.println(soundFile.getId());

        String command = readMsg();
        if (command.equals("FILE_UNSHARE_ALL_APPROVED"))
        {
            return true;
        }
        else return false;
    }

    /**
     * Shares a given list with a user of given ID. Sharing public list is allowed, in case they go private
     * @param soundList List to share
     * @param shareWith ID of user to share with
     * @return True if share request sent (doesn't mean approved), False otherwise
     */
    boolean shareList (SoundList soundList, int shareWith)
    {
        if (account.getType().equals("guest"))
        {
            System.out.println("PERMISSION DENIED");
            return false;
        }
        if (soundList.getOwner_id() != account.getId()) return false;

        sendMsg.println("LIST_SHARE_REQUEST");
        sendMsg.println(shareWith);

        return true;
    }

    /**
     * Registers a file with the server, and if successful, adds it locally,
     *  Works same as addFile() function, but is only available to admin accounts,
     *  owner_id is always 0 (server) and type is always "public"
     * @param name Name of file
     * @param description Description of filde
     * @param duration Play duration of file
     * @param size Size of file in KB
     * @param format Format of file
     * @param date_added Date added of file, DB autofills it as current_timestamp, give null unless you have a plan
     * @param path Path to file to send its data to server
     * @return The newly added file or null if encountered error
     */
    SoundFile addFileAsServer(String name, String description, String duration, int size, String format, String date_added, String path) throws IOException {
        if (!account.getType().equals("admin"))
        {
            System.out.println("PERMISSION DENIED");
            return null;
        }

        sendMsg.println("REGISTER_FILE");
        sendMsg.println(0);
        sendMsg.println(name);
        sendMsg.println(description);
        sendMsg.println(duration);
        sendMsg.println(size);
        sendMsg.println(format);
        sendMsg.println("public");
        sendMsg.println(date_added);
        sendFileData(path);

        String command = readMsg();
        if (command.equals("REGISTER_FILE_CORRECT"))
        {
            int fileId = Integer.parseInt(commandsList.get(commandsListIndex++));
            SoundFile soundFile = new SoundFile(fileId, 0, name, description, duration, size, format, "public", date_added);
            listOfSoundFiles.add(soundFile);
            return soundFile;
        }
        else return null;
    }

    /**
     * Gets an ArrayList of all files which give user is the owner of
     * @param userId ID of user to serach
     * @return ArrayList of File class objects belonging to given user
     */
    ArrayList<SoundFile> browseUserFiles(int userId){
        if (!account.getType().equals("admin"))
        {
            System.out.println("PERMISSION DENIED");
            return null;
        }
        ArrayList<SoundFile> soundFiles = new ArrayList<>();

        sendMsg.println("BROWSE_FILES_REQUEST");
        sendMsg.println(userId);

        int amount = Integer.parseInt(readMsg());
        for (int i=0; i<amount; i++)
        {
            SoundFile soundFile = createFileFromCommandsList();
            soundFiles.add(soundFile);
        }
        return soundFiles;
    }

    /**
     * Reports deletion to server, server either approves or not, admin only function
     * @param soundFile File to delete, get a list of files belonging to user via browserUserFiles()
     * @param userId ID of user to delete the file from
     * @return True if server approved deletion, False otherwise
     */
    boolean deleteUserFile(SoundFile soundFile, int userId){
        if (!account.getType().equals("admin"))
        {
            System.out.println("PERMISSION DENIED");
            return false;
        }

        if (soundFile.getOwner_id() != userId) return false;
        sendMsg.println("FILE_DELETE_USER");
        sendMsg.println(userId);
        sendMsg.println(soundFile.getId());

        String command = readMsg();
        if (command.equals("FILE_DELETE_USER_APPROVED"))
        {
            soundFile = null;
            return true;
        }
        else return false;
    }

    /**
     * Registers a list with the server, and if successful, adds it locally,
     *  Works same as addList() function, but is only available to admin accounts,
     *  owner_id is always 0 (server) and type is always "public"
     * @param name Name of list
     * @param description Description of list
     * @param soundFiles List of files to add to list
     * @return The newly added list or null if encountered error
     */
    SoundList addListAsServer(String name, String description, ArrayList<SoundFile> soundFiles)
    {
        if (account.getType().equals("guest"))
        {
            System.out.println("PERMISSION DENIED");
            return null;
        }
        sendMsg.println("REGISTER_LIST");
        sendMsg.println(0);
        sendMsg.println(name);
        sendMsg.println(description);
        sendMsg.println("public");
        sendMsg.println(soundFiles.size());
        for (int i = 0; i< soundFiles.size(); i++)
        {
            sendMsg.println(soundFiles.get(i).getId());
        }

        SoundList soundList = null;

        String command = readMsg();
        if (command.equals("REGISTER_LIST_CORRECT"))
        {
            int listId = Integer.parseInt(readMsg());
            soundList = new SoundList(listId, 0, name, description, "public");
            soundList.setFiles(soundFiles);
            listOfSoundLists.add(soundList);
            return soundList;
        }
        else return null;
    }

    /**
     * Gets an ArrayList of all lists which given user is the owner of
     * @param userId ID of user to serach
     * @return ArrayList of List class objects belonging to given user
     */
    ArrayList<SoundList> browseUserLists(int userId){
        if (!account.getType().equals("admin"))
        {
            System.out.println("PERMISSION DENIED");
            return null;
        }
        ArrayList<SoundList> soundLists = new ArrayList<>();

        sendMsg.println("BROWSE_LISTS_REQUEST");
        sendMsg.println(userId);


        int listsAmount = Integer.parseInt(commandsList.get(commandsListIndex++));

        for (int i=0; i<listsAmount; i++)
        {
            int listId = Integer.parseInt(readMsg());
            int listOwnerId = Integer.parseInt(readMsg());
            String listName = readMsg();
            String listDescription = readMsg();
            String listType = readMsg();
            SoundList soundListTemp = new SoundList(listId, listOwnerId, listName, listDescription, listType);

            int fileAmount = Integer.parseInt(commandsList.get(commandsListIndex++));
            ArrayList<SoundFile> tempSoundFiles = new ArrayList<>();
            for (int j=0; j<fileAmount; j++)
            {
                SoundFile temp = createFileFromCommandsList();
                tempSoundFiles.add(temp);
            }
            soundListTemp.setFiles(tempSoundFiles);
            soundLists.add(soundListTemp);
        }
        return soundLists;
    }

    /**
     * Reports deletion to server, server either approves or not, admin only function
     * @param soundList List to delete, get a list of lists belonging to user via browserUserLists()
     * @param userId ID of user to delete the file from
     * @return True if server approved deletion, False otherwise
     */
    boolean deleteUserList(SoundList soundList, int userId){
        if (!account.getType().equals("admin"))
        {
            System.out.println("PERMISSION DENIED");
            return false;
        }

        if (soundList.getOwner_id() != userId) return false;
        sendMsg.println("LIST_DELETE_USER");
        sendMsg.println(userId);
        sendMsg.println(soundList.getId());

        String command = readMsg();
        if (command.equals("LIST_DELETE_USER_APPROVED"))
        {
            soundList = null;
            return true;
        }
        else return false;
    }

    /**
     * Gets the contents of a given file from server.
     *  If download is set to false, that means that the user wants to play the file.
     *  In that case, create a hidden file in the tmp folder and delete it as soon as playback is finished.
     *  If download is set to true, the means that the user wants to download the file.
     *  In that case, create a visible file in the downloads folder, but don't play it.
     * @param soundFile
     * @param download
     * @throws IOException
     */
    void getSoundFile(SoundFile soundFile, boolean download) throws IOException {
        sendMsg.println("LISTEN_SOUND_REQUEST");
        sendMsg.println(soundFile.getId());

        String command = readMsg();
        if (command.equals("LISTEN_SOUND_APPROVED"))
        {
            if (!download)
            {
                String path = "./tmp/" + soundFile.getName() + "." + soundFile.getFormat();
                soundFile.setPath(path);
                writeFileData(path, false);
                // thing to play sound here
            }
            else
            {
                if (account.getType().equals("guest"))
                {
                    System.out.println("PERMISSION DENIED");
                    return;
                }

                String path = "./downloads/" + soundFile.getName() + "." + soundFile.getFormat();
                soundFile.setPath(path);
                writeFileData(path, true);
                // thing to play sound here
            }
        }
    }

    /**
     * Changes user type
     * @param userId Id of user to change
     * @param type Type to change to
     * @return True if request send (doesn't check if approved), False if not
     */
    boolean changeUserType (int userId, String type)
    {
        if (!account.getType().equals("admin"))
        {
            System.out.println("PERMISSION DENIED");
            return false;
        }

        sendMsg.println("CHANGE_USER_TYPE");
        sendMsg.println(userId);
        sendMsg.println(type);

        return true;
    }

    /**
     * Main loop of the Remote Host Master
     * @return
     * @throws Exception
     */
    @Override
    public String call() throws Exception {
        // notify whichever server thread that's listening who he's talking with
        //sendMsg.println(hostId);

        boolean keepAlive = true;
        int delayCounter = 0;
        while (keepAlive)
        {
            //System.out.println("TEATTE");

            // read all pending commands
            /*
            try
            {
                readCommands();
            }
            catch (IOException socketError)
            {
                System.out.println("Can't communicate with server, please restart connection or contact system administrator if error persists");
                return null;
            }
            */

            //System.out.println("HOST AFTER READING COMMANDS (" + commandsListIndex + " / " + commandsList.size() + ")");

            // process all the still unfinished commands
            while (commandsListIndex < commandsList.size())
            {
                // process commands
                System.out.println("host parsing command (" + commandsListIndex + " / " + (commandsList.size()-1) + ") " + commandsList.get(commandsListIndex));
                String command = commandsList.get(commandsListIndex);
                commandsListIndex++;
                ArrayList<String> components;
                switch (command)
                {
                    case "EXIT_THREAD": // 1 string in buffer
                    case "HOST_EXIT_THREAD": // 1 string in buffer
                        sendMsg.println("HOST_EXITING");
                        exec.shutdownNow();
                        keepAlive = false;
                        break;
                    case "HOST_ADD_FILES": // Adds or updates file, based on its ID
                        int filesAmount = Integer.parseInt(commandsList.get(commandsListIndex++));
                        for (int i=0; i<filesAmount; i++)
                        {
                            SoundFile temp = createFileFromCommandsList();
                            boolean fileFound = false;
                            for (SoundFile soundFile : listOfSoundFiles)
                            {
                                if (soundFile.getId() == temp.getId())
                                {
                                    listOfSoundFiles.set(listOfSoundFiles.indexOf(soundFile), temp);
                                    fileFound = true;
                                    break;
                                }
                            }
                            if (!fileFound)
                            {
                                listOfSoundFiles.add(temp);
                            }
                        }
                        break;
                    case "HOST_ADD_LIST": // Adds or updates a list, as well as all the files that go into it
                        int id = Integer.parseInt(commandsList.get(commandsListIndex++));
                        int owner_id = Integer.parseInt(commandsList.get(commandsListIndex++));
                        String name = commandsList.get(commandsListIndex++);
                        String description = commandsList.get(commandsListIndex++);
                        String type = commandsList.get(commandsListIndex++);
                        SoundList tempSoundList = new SoundList(id, owner_id, name, description, type);
                        int filePerList = Integer.parseInt(commandsList.get(commandsListIndex++));
                        boolean listFound = false;
                        for (int i = 0; i< listOfSoundLists.size(); i++)
                        {
                            if (listOfSoundLists.get(i).getId() == id)
                            {
                                listOfSoundLists.set(i, tempSoundList);
                                listFound = true;
                                break;
                            }
                        }
                        if (!listFound)
                        {
                            listOfSoundLists.add(tempSoundList);
                        }

                        for (int i=0; i<filePerList; i++)
                        {
                            SoundFile temp = createFileFromCommandsList();
                            boolean fileFound = false;
                            for (int j = 0; i< listOfSoundFiles.size(); i++)
                            {
                                if (listOfSoundFiles.get(j).getId() == temp.getId())
                                {
                                    listOfSoundFiles.set(j, temp);
                                    tempSoundList.addFile(temp);
                                    fileFound = true;
                                    break;
                                }
                            }
                            if (!fileFound)
                            {
                                listOfSoundFiles.add(temp);

                            }
                        }
                        break;
                    case "PING":
                        sendMsg.println("PING");
                        break;
                    default:
                        break;
                }
                //serialOut.close();
                //System.out.println("HOST DONE PARSING");
            }

            // check if thread is done

            /*
            for (int i=currentWorkingThread; i<taskArray.size(); i++)
            {
                if (!taskArray.get(i).getStatus().equals("Done"))
                {
                    currentWorkingThread = i;
                    taskArrayFuture.get(i).notify();
                    break;
                }
            }

             */
            //System.out.println("HOST END OF LOOP");
        }
        System.out.println("HOSTTHREADEND");
        exec.shutdown();
        return null;
    }
}
