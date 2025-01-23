import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Callable;


public class ServerComThread implements Callable<String> {
    /**
     * Reader used to read messages coming over the network
     */
    BufferedReader receiveMsg;
    /**
     * Writer used to send messages over the network
     */
    PrintWriter sendMsg;
    /**
     * Socket to facilitate communicating over the network
     */
    Socket socket;
    /**
     * List of commands sent to remote host
     */
    public ArrayList<String> commandsList = new ArrayList<>();
    /**
     * Index of next-to-send command
     */
    int commandsListIndex = 0;
    /**
     * List of responses sent from remote host
     */
    private ArrayList<String> responseList = new ArrayList<>();
    /**
     * Index of next-to-read-response
     */
    int responsesListIndex = 0;
    /**
     * Contains the data of sound file
     */
    byte[] lastFileData;
    /**
     * ID of the remote host that this thread communicates with, double as id of com thread
     */
    int talksWith = -1;
    /**
     * ID of account that this thread is communicating with
     */
    int accountId = -1;
    /**
     * If host responded back to a ping
     */
    private boolean pingAck = true;
    /**
     * If host doesn't respond to a ping after sending one, tick up the counter
     */
    private int pingDelay = 0;
    /**
     * If pingDelay reaches a certain value, assume host is dead
     */
    public boolean assumeDead = false;
    /**
     * Database connectivity
     */
    private DBConnection database = new DBConnection();
    int testCounter = 0;
    ServerComThread(Socket socket, int id) throws IOException, SQLException {
        this.socket = socket;
        socket.setSoTimeout(1000);
        this.talksWith = id;
        receiveMsg = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        sendMsg = new PrintWriter(socket.getOutputStream(), true);
        sendMsg.println(talksWith);
    }

    /**
     * Receive command to send to remote host
     * @param command String of what to send to remote host
     */
    public void receiveCommands(String command)
    {
        commandsList.add(command);
    }

    /**
     * Receive commands to send to remote host
     * @param commands Array of commands to send to remote host
     */
    public void receiveCommands(ArrayList<String> commands)
    {
        //System.out.println(commands);
        commandsList.addAll(commands);
    }

    /**
     * Exchanges commands
     * @throws InterruptedException If thread interrupted while exchanging
     */
    private void readResponses() throws InterruptedException, IOException {
        String response = "";
        try
        {
            while (true) // keep reading until .readLine throws a timeout
            {
                response = receiveMsg.readLine();
                responseList.add(response);
            }
        }
        catch (SocketTimeoutException waitTooLong)
        {
            // means no more messages to read, so just go on with your life
            //System.out.println(waitTooLong);
        }
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
     * @return True, IDK
     * @throws IOException
     */
    boolean writeFileData(String name) throws IOException {
        File file = new File("./sound_files/" + name);
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
     * Creates a single sound file from an earlier executed DB statement
     * @param resultSet Result from seaerchDatabase of DBConnection object
     * @return An object of SoundFile class
     */
    private SoundFile makeFileFromResultSet(ResultSet resultSet) throws SQLException {
        resultSet.next();
        int id = resultSet.getInt("id");
        int owner_id = resultSet.getInt("owner_id");
        String name = resultSet.getString("name");
        String description = resultSet.getString("description");
        String duration = resultSet.getString("duration");
        int size = resultSet.getInt("size");
        String format = resultSet.getString("format");
        String type = resultSet.getString("type");
        String date_added = resultSet.getString("date_added");
        return new SoundFile(id, owner_id, name, description, duration, size, format, type, date_added);
    }

    /**
     * Creates an ArrayList of sound files from an earlier executed DB statement.
     * Use this over the single one as this has a better built-in validity check
     * @param resultSet Result from searchDatabase of DBConnection object
     * @return ArrayList of SoundFile objects
     * @throws SQLException If SQL fucks up, or you
     */
    private ArrayList<SoundFile> makeFileListFromResultSet(ResultSet resultSet) throws SQLException {
        ArrayList<SoundFile> list = new ArrayList<>();
        while (resultSet.next())
        {
            int id = resultSet.getInt("id");
            int owner_id = resultSet.getInt("owner_id");
            String name = resultSet.getString("name");
            String description = resultSet.getString("description");
            String duration = resultSet.getString("duration");
            int size = resultSet.getInt("size");
            String format = resultSet.getString("format");
            String type = resultSet.getString("type");
            String date_added = resultSet.getString("date_added");
            list.add(new SoundFile(id, owner_id, name, description, duration, size, format, type, date_added));
        }
        return list;
    }

    /**
     * Creates a single sound list from an earlier executed DB statement
     * @param resultSet Result from seaerchDatabase of DBConnection object
     * @return An object of SoundList class, with all the associated files
     */
    private SoundList makeListFromResultSet(ResultSet resultSet) throws SQLException {
        resultSet.next();
        int id = resultSet.getInt("id");
        int owner_id = resultSet.getInt("owner_id");
        String name = resultSet.getString("name");
        String description = resultSet.getString("description");
        String type = resultSet.getString("type");
        SoundList soundList = new SoundList(id, owner_id, name, description, type);

        ResultSet filesSet = database.searchDatabase("SELECT `id`, `owner_id`, `name`, `description`, `duration`, `size`, `format`, `type`, `date_added`\n" +
                "FROM `file` INNER JOIN `file_sharing`\n" +
                "ON `file`.`owner_id` = `file_sharing`.`account_id`;");
        ArrayList<SoundFile> files = makeFileListFromResultSet(filesSet);
        soundList.setFiles(files);

        return soundList;
    }

    /**
     * Creates an ArrayList of sound lists from an earlier executed DB statement.
     * Use this over the single one as this has a better built-in validity check
     * @param resultSet Result from searchDatabase of DBConnection object
     * @return ArrayList of SoundList objects
     * @throws SQLException If SQL fucks up, or you. I'm not checking, but I'm judging
     */
    private ArrayList<SoundList> makeListListFromResultSet(ResultSet resultSet) throws SQLException {
        ArrayList<SoundList> list = new ArrayList<>();
        while (resultSet.next())
        {
            int id = resultSet.getInt("id");
            int owner_id = resultSet.getInt("owner_id");
            String name = resultSet.getString("name");
            String description = resultSet.getString("description");
            String type = resultSet.getString("type");
            SoundList soundList = new SoundList(id, owner_id, name, description, type);

            ResultSet filesSet = database.searchDatabase("SELECT `id`, `owner_id`, `name`, `description`, `duration`, `size`, `format`, `type`, `date_added`\n" +
                    "FROM `file` INNER JOIN `file_sharing`\n" +
                    "ON `file`.`owner_id` = `file_sharing`.`account_id`;");
            ArrayList<SoundFile> files = makeFileListFromResultSet(filesSet);
            soundList.setFiles(files);

            list.add(soundList);
        }
        return list;
    }

    /**
     * Main working environment of the server communication thread
     * @return String of I don't even know what frankly
     * @throws Exception Way too much stuff
     */
    @Override
    public String call() throws Exception {
        // get the ID of host this thread is talking with
        //talksWith = Integer.parseInt(receiveMsg.readLine());
        //System.out.println(talksWith);

        boolean keepAlive = true;
        while (keepAlive)
        {

            //System.out.println("SERVERCOM THREAD CHECKIN " + talksWith);
            try
            {
                readResponses();
            }
            catch (IOException io) // if trying to read from a closed socket
            {
                System.out.println("Host ID " + talksWith + " socket reading error PING ACK " + pingDelay++);
                Thread.sleep(1000); // just so it emulates the normal time of 'operations'
            }
            //System.out.println("PING ACK " + pingDelay);
            //System.out.println(responseList);

            //System.out.println( responsesListIndex + " / " + responseList.size());
            while (responsesListIndex < responseList.size())
            {
                String response = responseList.get(responsesListIndex);
                responsesListIndex++;
                //System.out.println("SERVER PARSING RESPONSE " + response);
                String rawArray;
                boolean componentAlreadyExists;
                switch (response)
                {
                    case "RECEIVE_FILES":
                        ResultSet filesSetRcv = database.searchDatabase("SELECT `id`, `owner_id`, `name`, `description`, `duration`, `size`, `format`, `type`, `date_added`\n" +
                                "FROM `file` INNER JOIN `file_sharing`\n" +
                                "ON `file`.`owner_id` = `file_sharing`.`account_id`\n" +
                                "WHERE `owner_id` = " + accountId + ";");
                        ArrayList<SoundFile> filesRcv = makeFileListFromResultSet(filesSetRcv);
                        sendMsg.println(filesRcv.size()); // amount
                        for (SoundFile file : filesRcv)
                        {
                            ArrayList<String> elements = file.getArrayOfElements();
                            for (String element : elements)
                            {
                                sendMsg.println(element);
                            }
                        }
                        break;
                    case "RECEIVE_FILES_PUBLIC":
                        ResultSet filesSetRcvPub = database.searchDatabase("SELECT `id`, `owner_id`, `name`, `description`, `duration`, `size`, `format`, `type`, `date_added`\n" +
                                "FROM `file` INNER JOIN `file_sharing`\n" +
                                "ON `file`.`owner_id` = `file_sharing`.`account_id`\n" +
                                "WHERE `owner_id` = " + accountId + "\n" +
                                "AND `type` = \"public\";");
                        ArrayList<SoundFile> filesRcvPub = makeFileListFromResultSet(filesSetRcvPub);
                        sendMsg.println(filesRcvPub.size()); // amount
                        for (SoundFile file : filesRcvPub)
                        {
                            ArrayList<String> elements = file.getArrayOfElements();
                            for (String element : elements)
                            {
                                sendMsg.println(element);
                            }
                        }
                        break;
                    case "RECEIVE_LISTS":
                        ResultSet listsSetRcv = database.searchDatabase("SELECT `id`, `owner_id`, `name`, `description`, `type`\n" +
                                "FROM `list_main` INNER JOIN `list_sharing`\n" +
                                "ON `list_main`.`owner_id` = `list_sharing`.`account_id`\n" +
                                "WHERE `owner_id` = " + accountId + ";");
                        ArrayList<SoundList> listsRcv = makeListListFromResultSet(listsSetRcv);
                        sendMsg.println(listsRcv.size());
                        for (SoundList list: listsRcv)
                        {
                            ArrayList<String> elements = list.getArrayOfElements();
                            for (String element : elements)
                            {
                                sendMsg.println(element);
                            }
                            for (SoundFile file : list.getFiles())
                            {
                                elements = file.getArrayOfElements();
                                for (String element : elements)
                                {
                                    sendMsg.println(element);
                                }
                            }
                        }
                        break;
                    case "RECEIVE_LISTS_PUBLIC":
                        ResultSet listsSetRcvPub = database.searchDatabase("SELECT `id`, `owner_id`, `name`, `description`, `type`\n" +
                                "FROM `list_main` INNER JOIN `list_sharing`\n" +
                                "ON `list_main`.`owner_id` = `list_sharing`.`account_id`\n" +
                                "WHERE `owner_id` = " + accountId + "\n" +
                                "AND `type` = \"public\";");
                        ArrayList<SoundList> listsRcvPub = makeListListFromResultSet(listsSetRcvPub);
                        sendMsg.println(listsRcvPub.size());
                        for (SoundList list: listsRcvPub)
                        {
                            ArrayList<String> elements = list.getArrayOfElements();
                            for (String element : elements)
                            {
                                sendMsg.println(element);
                            }
                            for (SoundFile file : list.getFiles())
                            {
                                elements = file.getArrayOfElements();
                                for (String element : elements)
                                {
                                    sendMsg.println(element);
                                }
                            }
                        }
                        break;
                    case "LOGIN_REQUEST":
                        String loginLog = responseList.get(responsesListIndex++);
                        String passwordLog = responseList.get(responsesListIndex++);
                        if (loginLog.equalsIgnoreCase("guest"))
                        {
                            accountId = -1;
                            sendMsg.println("LOGIN_CORRECT");
                            sendMsg.println("-1");
                            sendMsg.println("guest");
                            break;
                        }
                        System.out.println("login: " + loginLog);
                        System.out.println("pass: " + passwordLog);
                        ResultSet loginSetLog = database.searchDatabase("SELECT *\n" +
                                "FROM `account`\n" +
                                "WHERE `name` = \"" + loginLog + "\";");
                        if (!loginSetLog.next())
                        {
                            sendMsg.println("LOGIN_ERROR");
                            break;
                        }
                        if (!passwordLog.equals(loginSetLog.getString("password")))
                        {
                            sendMsg.println("PASSWORD_ERROR");
                            break;
                        }
                        else
                        {
                            sendMsg.println("LOGIN_CORRECT");
                            sendMsg.println(loginSetLog.getString("id"));
                            sendMsg.println(loginSetLog.getString("type"));
                            System.out.println("found id: " + loginSetLog.getString("id"));
                            accountId = loginSetLog.getInt("id");
                            System.out.println("found type: " + loginSetLog.getString("type"));
                        }
                        break;
                    case "REGISTER_REQUEST":
                        String loginReg = responseList.get(responsesListIndex++);
                        String passwordReg = responseList.get(responsesListIndex++);
                        System.out.println("register test");
                        if (loginReg.equalsIgnoreCase("guest"))
                        {
                            sendMsg.println("REGISTER_ERROR");
                            break;
                        }
                        ResultSet loginSetReg = database.searchDatabase("SELECT *\n" +
                                "FROM `account`\n" +
                                "WHERE `name` = \"" + loginReg + "\";");
                        if (!loginSetReg.next())
                        {
                            if (database.execUpdate("INSERT INTO `account` (`name`, `password`, `type`)\n" +
                                    "VALUES (\"" + loginReg + "\", \"" + passwordReg + "\", \"standard\");") != -1)
                            {
                                sendMsg.println("REGISTER_CORRECT");
                                loginSetReg = database.searchDatabase("SELECT *\n" +
                                        "FROM `account`\n" +
                                        "WHERE `name` = \"" + loginReg + "\";");
                                sendMsg.println(loginSetReg.getString("id"));
                                sendMsg.println(loginSetReg.getString("type"));
                            }
                            else
                            {
                                sendMsg.println("REGISTER_ERROR");
                            }

                        }
                        else
                        {
                            sendMsg.println("REGISTER_ERROR");
                        }
                        break;
                    case "REGISTER_LIST":
                        int listAccId = Integer.parseInt(responseList.get(responsesListIndex++));
                        String listNameReg = responseList.get(responsesListIndex++);
                        String listDescReg = responseList.get(responsesListIndex++);
                        String listTypeReg = responseList.get(responsesListIndex++);

                        if (listAccId == 0)
                        {
                            ResultSet regListAdmChkSet = database.searchDatabase("SELECT `type`\n" +
                                    "FROM `account`\n" +
                                    "WHERE `id` = " + accountId + ";");
                            regListAdmChkSet.next();
                            if (!regListAdmChkSet.getString("type").equals("admin"))
                            {
                                sendMsg.println("REGISTER_LIST_ERROR");
                                break;
                            }
                        }

                        if (database.execUpdate("INSERT INTO `list_main`(`owner_id`, `name`, `description`, `type`)\n" +
                                "VALUES (\"" + listAccId + "\", \"" + listNameReg + "\", \"" + listDescReg + "\", \"" + listTypeReg + "\");") != -1)
                        {
                            ResultSet regListSet = database.searchDatabase("SELECT `id`\n" +
                                    "FROM `list_main`\n" +
                                    "WHERE `owner_id` = \"" + listAccId + "\"\n" +
                                    "AND `name` = \"" + listNameReg + "\"\n" +
                                    "AND `description` = \"" + listDescReg + "\"\n" +
                                    "AND `type` = \"" + listTypeReg + "\";");
                            regListSet.next();
                            int soundAmountRegList = Integer.parseInt(responseList.get(responsesListIndex++));
                            for (int i=0; i<soundAmountRegList; i++)
                            {
                                database.execUpdate("INSERT INTO `list_contents`(`list_id`, `file_id`)\n" +
                                        "VALUES (" + regListSet.getInt("id") + ", " + Integer.parseInt(responseList.get(responsesListIndex++)) + ");");
                            }
                            sendMsg.println("REGISTER_LIST_CORRECT");
                            sendMsg.println(regListSet.getInt("id"));
                        }
                        else
                        {
                            sendMsg.println("REGISTER_LIST_ERROR");
                        }
                        break;
                    case "REGISTER_FILE":
                        int fileAccountIdReg = Integer.parseInt(responseList.get(responsesListIndex++));
                        String fileNameReg = responseList.get(responsesListIndex++);
                        String fileDescReg = responseList.get(responsesListIndex++);
                        String fileDurReg = responseList.get(responsesListIndex++);
                        int fileSizeReg = Integer.parseInt(responseList.get(responsesListIndex++));
                        String fileFormReg = responseList.get(responsesListIndex++);
                        String fileTypeReg = responseList.get(responsesListIndex++);
                        String fileDateReg = responseList.get(responsesListIndex++);
                        System.out.println("fileAccountIdReg: " + fileAccountIdReg);
                        System.out.println("fileNameReg: " + fileNameReg);
                        System.out.println("fileDescReg: " + fileDescReg);
                        System.out.println("fileDurReg: " + fileDurReg);
                        System.out.println("fileSizeReg: " + fileSizeReg);
                        System.out.println("fileFormReg: " + fileFormReg);
                        System.out.println("fileTypeReg: " + fileTypeReg);
                        System.out.println("fileDateReg: " + fileDateReg);

                        String command = responseList.get(responsesListIndex++);
                        if (command.equals("SENDING_FILE_DATA"))
                        {
                            String downloadName = responseList.get(responsesListIndex++);
                            int downloadSize = Integer.parseInt(responseList.get(responsesListIndex++));

                            System.out.println("Receiving file: " + downloadName);
                            //System.out.println("fileLen: " + downloadSize);
                            //System.out.println("test 0");
                            String bufferString = responseList.get(responsesListIndex++);
                            String[] convString = bufferString.substring(1, bufferString.length()-1).split(",");
                            lastFileData = new byte[convString.length];
                            for (int i=0; i<lastFileData.length; i++)
                            {
                                lastFileData[i] = Byte.parseByte(convString[i].trim());
                            }
                            //System.out.println("test 1");
                            //System.out.println(lastFileData.length);
                            //System.out.println("test 2");
                        }
                        else
                        {
                            sendMsg.println("REGISTER_FILE_ERROR");
                            break;
                        }

                        String filePathReg = "./sound_files/" + fileNameReg + "." + fileFormReg;
                        //System.out.println("ok");
                        if (fileDateReg.equalsIgnoreCase("null"))
                        {
                            if (database.execUpdate("INSERT INTO `file`(`owner_id`, `name`, `description`, `path`, `duration`, `size`, `format`, `type`)\n" +
                                    "VALUES (" + fileAccountIdReg + ", \"" + fileNameReg + "\", \"" + fileDescReg + "\", \"" + fileNameReg + "\", \"" + fileDurReg + "\", " + fileSizeReg + ", \"" + fileFormReg + "\", \"" + fileTypeReg + "\");") != -1)
                            {
                                writeFileData(fileNameReg + "." + fileFormReg);
                                sendMsg.println("REGISTER_FILE_CORRECT");
                                ResultSet fileRegSet = database.searchDatabase("SELECT `id`\n" +
                                        "FROM `file`\n" +
                                        "WHERE `owner_id` = " + fileAccountIdReg + "\n" +
                                        "AND `name` = \"" + fileNameReg + "\";");
                                fileRegSet.next();
                                sendMsg.println(fileRegSet.getInt("id"));
                            }
                            else
                            {
                                sendMsg.println("REGISTER_FILE_ERROR");
                            }
                        }
                        else
                        {
                            if (database.execUpdate("INSERT INTO `file`(`owner_id`, `name`, `description`, `path`, `duration`, `size`, `format`, `type`, `date_added`)\n" +
                                    "VALUES (" + fileAccountIdReg + ", \"" + fileNameReg + "\", \"" + fileDescReg + "\", \"" + filePathReg + "\", \"" + fileDurReg + "\", " + fileSizeReg + ", \"" + fileFormReg + "\", \"" + fileTypeReg + "\", \"" + fileDateReg + "\");") != -1)
                            {
                                sendMsg.println("REGISTER_FILE_CORRECT");
                                ResultSet fileRegSet = database.searchDatabase("SELECT `id`\n" +
                                        "FROM `file`\n" +
                                        "WHERE `owner_id` = " + fileAccountIdReg + "\n" +
                                        "AND `name` = \"" + fileNameReg + "\";");
                                fileRegSet.next();
                                sendMsg.println(fileRegSet.getInt("id"));
                            }
                            else
                            {
                                sendMsg.println("REGISTER_FILE_ERROR");
                            }
                        }
                        break;
                    case "FILE_DELETE":
                        int fileIdDel = Integer.parseInt(responseList.get(responsesListIndex++));
                        ResultSet fileDelSet = database.searchDatabase("SELECT `owner_id`, `path`\n" +
                                "FROM `file`\n" +
                                "WHERE `id` = " + fileIdDel + ";");
                        if (fileDelSet.next())
                        {
                            if (fileDelSet.getInt("owner_id") == accountId)
                            {
                                if (database.execUpdate("DELETE FROM `file`\n" +
                                        "WHERE `id` = " + fileIdDel + ";") != -1)
                                {
                                    File file = new File(fileDelSet.getString("path"));
                                    if (file.exists()) file.delete();
                                    sendMsg.println("FILE_DELETE_APPROVED");
                                }
                                else
                                {
                                    sendMsg.println("FILE_DELETE_ERROR");
                                }
                            }
                            else
                            {
                                sendMsg.println("FILE_DELETE_ERROR");
                            }
                        }
                        else
                        {
                            sendMsg.println("FILE_DELETE_ERROR");
                        }
                        break;
                    case "FILE_SHARE_REQUEST":
                        int fileIdShare = Integer.parseInt(responseList.get(responsesListIndex++));
                        int accIdShareFile = Integer.parseInt(responseList.get(responsesListIndex++));

                        ResultSet fileShareSet = database.searchDatabase("SELECT `owner_id`\n" +
                                "FROM `file`\n" +
                                "WHERE `id` = " + fileIdShare + ";");
                        if (fileShareSet.next())
                        {
                            if (fileShareSet.getInt("owner_id") != accountId)
                            {
                                break;
                            }
                        }
                        else
                        {
                            break;
                        }

                        fileShareSet = database.searchDatabase("SELECT *\n" +
                                "FROM `file_sharing`\n" +
                                "WHERE `file_id` = " + fileIdShare + "\n" +
                                "AND `account_id` = " + accIdShareFile + ";");
                        if (fileShareSet.next())
                        {
                            break;
                        }

                        database.execUpdate("INSERT INTO `file_sharing`(`file_id`, `account_id`)\n" +
                                "VALUES (" + fileIdShare + ", " + accIdShareFile + ";");
                        break;
                    case "LIST_DELETE":
                        int listIdDel = Integer.parseInt(responseList.get(responsesListIndex++));
                        ResultSet listDelSet = database.searchDatabase("SELECT `owner_id`\n" +
                                "FROM `list_main`\n" +
                                "WHERE `id` = " + listIdDel + ";");
                        if (listDelSet.next())
                        {
                            if (listDelSet.getInt("owner_id") == accountId)
                            {
                                if (database.execUpdate("DELETE FROM `list_main`\n" +
                                        "WHERE `id` = " + listIdDel + ";") != -1)
                                {
                                    sendMsg.println("LIST_DELETE_APPROVED");
                                }
                                else
                                {
                                    sendMsg.println("LIST_DELETE_ERROR");
                                }
                            }
                            else
                            {
                                sendMsg.println("LIST_DELETE_ERROR");
                            }
                        }
                        else
                        {
                            sendMsg.println("LIST_DELETE_ERROR");
                        }
                        break;
                    case "LIST_SHARE_REQUEST":
                        int listIdShare = Integer.parseInt(responseList.get(responsesListIndex++));
                        int accIdShareList = Integer.parseInt(responseList.get(responsesListIndex++));

                        ResultSet listShareSet = database.searchDatabase("SELECT `owner_id`\n" +
                                "FROM `list_main`\n" +
                                "WHERE `id` = " + listIdShare + ";");
                        if (listShareSet.next())
                        {
                            if (listShareSet.getInt("owner_id") != accountId)
                            {
                                break;
                            }
                        }
                        else
                        {
                            break;
                        }

                        listShareSet = database.searchDatabase("SELECT *\n" +
                                "FROM `file_sharing`\n" +
                                "WHERE `file_id` = " + listIdShare + "\n" +
                                "AND `account_id` = " + accIdShareList + ";");
                        if (listShareSet.next())
                        {
                            break;
                        }

                        database.execUpdate("INSERT INTO `list_sharing`(`list_id`, `account_id`)\n" +
                                "VALUES (" + listIdShare + ", " + accIdShareList + ";");
                        break;
                    case "ADD_FILE_TO_LIST":
                        int listIdListAdd = Integer.parseInt(responseList.get(responsesListIndex++));
                        int fileIdListAdd = Integer.parseInt(responseList.get(responsesListIndex++));

                        ResultSet listListAddSet = database.searchDatabase("SELECT `owner_id`\n" +
                                "FROM `list_main`\n" +
                                "WHERE `id` = " + listIdListAdd + ";");
                        if (listListAddSet.next())
                        {
                            if (listListAddSet.getInt("owner_id") != accountId)
                            {
                                sendMsg.println("ADD_FILE_TO_LIST_ERROR");
                                break;
                            }
                        }
                        else
                        {
                            sendMsg.println("ADD_FILE_TO_LIST_ERROR");
                            break;
                        }

                        listListAddSet = database.searchDatabase("SELECT *\n" +
                                "FROM `list_contents`\n" +
                                "WHERE `list_id` = " + listIdListAdd + "\n" +
                                "AND `file_id` = " + fileIdListAdd + ";");
                        if (listListAddSet.next())
                        {
                            sendMsg.println("ADD_FILE_TO_LIST_ERROR");
                            break;
                        }

                        if (database.execUpdate("INSERT INTO `list_contents`(`list_id`, `file_id`)\n" +
                                "VALUES (" + listIdListAdd + ", " + fileIdListAdd + ";") != -1)
                        {
                            sendMsg.println("ADD_FILE_TO_LIST_APPROVED");
                        }
                        else
                        {
                            sendMsg.println("ADD_FILE_TO_LIST_ERROR");
                        }
                        break;
                    case "REMOVE_FILE_FROM_LIST":
                        int listIdListRem = Integer.parseInt(responseList.get(responsesListIndex++));
                        int fileIdListRem = Integer.parseInt(responseList.get(responsesListIndex++));

                        ResultSet listListRemSet = database.searchDatabase("SELECT `owner_id`\n" +
                                "FROM `list_main`\n" +
                                "WHERE `id` = " + listIdListRem + ";");
                        if (listListRemSet.next())
                        {
                            if (listListRemSet.getInt("owner_id") != accountId)
                            {
                                sendMsg.println("REMOVE_FILE_TO_LIST_ERROR");
                                break;
                            }
                        }
                        else
                        {
                            sendMsg.println("REMOVE_FILE_TO_LIST_ERROR");
                            break;
                        }

                        listListRemSet = database.searchDatabase("SELECT *\n" +
                                "FROM `list_contents`\n" +
                                "WHERE `list_id` = " + listIdListRem + "\n" +
                                "AND `file_id` = " + fileIdListRem + ";");
                        if (listListRemSet.next())
                        {
                            sendMsg.println("REMOVE_FILE_TO_LIST_ERROR");
                            break;
                        }

                        if (database.execUpdate("DELETE FROM `list_contents`\n" +
                                "WHERE `list_id` = " + listIdListRem + "\n" +
                                "AND `file_id`= " + fileIdListRem + ";") != -1)
                        {
                            sendMsg.println("REMOVE_FILE_TO_LIST_APPROVED");
                        }
                        else
                        {
                            sendMsg.println("REMOVE_FILE_TO_LIST_ERROR");
                        }
                        break;
                    case "FILE_UNSHARE_REQUEST":
                        int fileIdShareRem = Integer.parseInt(responseList.get(responsesListIndex++));
                        int accIdShareFileRem = Integer.parseInt(responseList.get(responsesListIndex++));

                        ResultSet fileShareRemSet = database.searchDatabase("SELECT `owner_id`\n" +
                                "FROM `file`\n" +
                                "WHERE `id` = " + fileIdShareRem + ";");
                        if (fileShareRemSet.next())
                        {
                            if (fileShareRemSet.getInt("owner_id") != accountId)
                            {
                                sendMsg.println("FILE_UNSHARE_ERROR");
                                break;
                            }
                        }
                        else
                        {
                            sendMsg.println("FILE_UNSHARE_ERROR");
                            break;
                        }

                        fileShareSet = database.searchDatabase("SELECT *\n" +
                                "FROM `file_sharing`\n" +
                                "WHERE `file_id` = " + fileIdShareRem + "\n" +
                                "AND `account_id` = " + accIdShareFileRem + ";");
                        if (!fileShareSet.next())
                        {
                            sendMsg.println("FILE_UNSHARE_ERROR");
                            break;
                        }

                        if (database.execUpdate("DELETE FROM `file_sharing`\n" +
                                "WHERE `file_id` = " + fileIdShareRem + "\n" +
                                "AND `account_id` = " + accIdShareFileRem + ";") != -1)
                        {
                            sendMsg.println("FILE_UNSHARE_APPROVED");
                        }
                        else
                        {
                            sendMsg.println("FILE_UNSHARE_ERROR");
                        }
                        break;
                    case "FILE_UNSHARE_ALL_REQUEST":
                        int fileIdShareRemAll = Integer.parseInt(responseList.get(responsesListIndex++));

                        ResultSet fileShareRemAllSet = database.searchDatabase("SELECT `owner_id`\n" +
                                "FROM `file`\n" +
                                "WHERE `id` = " + fileIdShareRemAll + ";");
                        if (fileShareRemAllSet.next())
                        {
                            if (fileShareRemAllSet.getInt("owner_id") != accountId)
                            {
                                sendMsg.println("FILE_UNSHARE_ALL_ERROR");
                                break;
                            }
                        }
                        else
                        {
                            sendMsg.println("FILE_UNSHARE_ALL_ERROR");
                            break;
                        }

                        fileShareSet = database.searchDatabase("SELECT *\n" +
                                "FROM `file_sharing`\n" +
                                "WHERE `file_id` = " + fileIdShareRemAll + ";");
                        if (!fileShareSet.next())
                        {
                            sendMsg.println("FILE_UNSHARE_ALL_ERROR");
                            break;
                        }

                        if (database.execUpdate("DELETE FROM `file_sharing`\n" +
                                "WHERE `file_id` = " + fileIdShareRemAll + ";") != -1)
                        {
                            sendMsg.println("FILE_UNSHARE_ALL_APPROVED");
                        }
                        else
                        {
                            sendMsg.println("FILE_UNSHARE_ALL_ERROR");
                        }
                        break;
                    case "BROWSE_FILES_REQUEST":
                        int browseUserFileId = Integer.parseInt(responseList.get(responsesListIndex++));
                        ResultSet browseUserFileSet = database.searchDatabase("SELECT `type`\n" +
                                "FROM `account`\n" +
                                "WHERE `id` = " + accountId + ";");
                        browseUserFileSet.next();
                        if (!browseUserFileSet.getString("type").equals("admin"))
                        {
                            sendMsg.println(0);
                            break;
                        }

                        browseUserFileSet = database.searchDatabase("SELECT *\n" +
                                "FROM `file`\n" +
                                "WHERE `owner_id` = " + browseUserFileId + ";");
                        int counter = 0;
                        while (browseUserFileSet.next())
                        {
                            counter++;
                        }
                        sendMsg.println(counter);
                        browseUserFileSet.beforeFirst();
                        while (browseUserFileSet.next())
                        {
                            sendMsg.println(browseUserFileSet.getInt("id"));
                            sendMsg.println(browseUserFileSet.getInt("owner_id"));
                            sendMsg.println(browseUserFileSet.getString("name"));
                            sendMsg.println(browseUserFileSet.getString("description"));
                            sendMsg.println(browseUserFileSet.getString("duration"));
                            sendMsg.println(browseUserFileSet.getInt("size"));
                            sendMsg.println(browseUserFileSet.getString("format"));
                            sendMsg.println(browseUserFileSet.getString("type"));
                            sendMsg.println(browseUserFileSet.getString("date_added"));
                        }
                        break;
                    case "FILE_DELETE_USER":
                        int delUserFileId = Integer.parseInt(responseList.get(responsesListIndex++));
                        int delFileFileId = Integer.parseInt(responseList.get(responsesListIndex++));

                        ResultSet delUserFileSet = database.searchDatabase("SELECT `type`\n" +
                                "FROM `account`\n" +
                                "WHERE `id` = " + accountId + ";");
                        delUserFileSet.next();
                        if (!delUserFileSet.getString("type").equals("admin"))
                        {
                            sendMsg.println("FILE_DELETE_USER_ERROR");
                            break;
                        }

                        delUserFileSet = database.searchDatabase("SELECT *\n" +
                                "FROM`file`\n" +
                                "WHERE `owner_id` = " + delUserFileId + "\n" +
                                "AND `id` = " + delFileFileId + ";");
                        if (!delUserFileSet.next())
                        {
                            sendMsg.println("FILE_DELETE_USER_ERROR");
                            break;
                        }
                        else
                        {
                            if ((database.execUpdate("DELETE FROM `file`\n" +
                                    "WHERE `id` = " + delFileFileId + ";")) != -1)
                            {
                                File file = new File(delUserFileSet.getString("path"));
                                if (file.exists()) file.delete();
                                sendMsg.println("FILE_DELETE_USER_APPROVED");
                            }
                            else
                            {
                                sendMsg.println("FILE_DELETE_USER_ERROR");
                            }
                        }
                        break;
                    case "BROWSE_LISTS_REQUEST":
                        int browseUserListId = Integer.parseInt(responseList.get(responsesListIndex++));
                        ResultSet browseUserListSet = database.searchDatabase("SELECT `type`\n" +
                                "FROM `account`\n" +
                                "WHERE `id` = " + accountId + ";");
                        browseUserListSet.next();
                        if (!browseUserListSet.getString("type").equals("admin"))
                        {
                            sendMsg.println(0);
                            break;
                        }

                        browseUserListSet = database.searchDatabase("SELECT *\n" +
                                "FROM `list_main`\n" +
                                "WHERE `owner_id` = " + browseUserListId + ";");
                        int counterLists = 0;
                        while (browseUserListSet.next())
                        {
                            counterLists++;
                        }
                        sendMsg.println(counterLists);
                        browseUserListSet.beforeFirst();
                        while (browseUserListSet.next())
                        {
                            sendMsg.println(browseUserListSet.getInt("id"));
                            sendMsg.println(browseUserListSet.getInt("owner_id"));
                            sendMsg.println(browseUserListSet.getString("name"));
                            sendMsg.println(browseUserListSet.getString("description"));
                            sendMsg.println(browseUserListSet.getString("type"));
                            ResultSet browseUserListInnerSet = database.searchDatabase("SELECT *\n" +
                                    "FROM `file` INNER JOIN `list_contents`\n" +
                                    "ON `file`.`id` = `list_contents`.`file_id`\n" +
                                    "WHERE `list_contents`.`list_id` = " + browseUserListSet.getInt("id"));
                            int counterFiles = 0;
                            while (browseUserListInnerSet.next())
                            {
                                counterFiles++;
                            }
                            sendMsg.println(counterFiles);
                            browseUserListInnerSet.beforeFirst();
                            while (browseUserListInnerSet.next())
                            {
                                sendMsg.println(browseUserListInnerSet.getInt("id"));
                                sendMsg.println(browseUserListInnerSet.getInt("owner_id"));
                                sendMsg.println(browseUserListInnerSet.getString("name"));
                                sendMsg.println(browseUserListInnerSet.getString("description"));
                                sendMsg.println(browseUserListInnerSet.getString("duration"));
                                sendMsg.println(browseUserListInnerSet.getInt("size"));
                                sendMsg.println(browseUserListInnerSet.getString("format"));
                                sendMsg.println(browseUserListInnerSet.getString("type"));
                                sendMsg.println(browseUserListInnerSet.getString("date_added"));
                            }
                        }
                        break;
                    case "LIST_DELETE_USER":
                        int listIdDelUser = Integer.parseInt(responseList.get(responsesListIndex++));
                        int userIdDelUser = Integer.parseInt(responseList.get(responsesListIndex++));

                        ResultSet delUserListSet = database.searchDatabase("SELECT `type`\n" +
                                "FROM `account`\n" +
                                "WHERE `id` = " + accountId + ";");
                        delUserListSet.next();
                        if (!delUserListSet.getString("type").equals("admin"))
                        {
                            sendMsg.println(0);
                            break;
                        }

                        delUserListSet = database.searchDatabase("SELECT `owner_id`\n" +
                                "FROM `list_main`\n" +
                                "WHERE `id` = " + listIdDelUser + ";");
                        if (delUserListSet.next())
                        {
                            if (delUserListSet.getInt("owner_id") == userIdDelUser)
                            {
                                if (database.execUpdate("DELETE FROM `list_main`\n" +
                                        "WHERE `id` = " + listIdDelUser + ";") != -1)
                                {
                                    sendMsg.println("LIST_DELETE_APPROVED");
                                }
                                else
                                {
                                    sendMsg.println("LIST_DELETE_ERROR");
                                }
                            }
                            else
                            {
                                sendMsg.println("LIST_DELETE_ERROR");
                            }
                        }
                        else
                        {
                            sendMsg.println("LIST_DELETE_ERROR");
                        }
                        break;
                    case "LISTEN_SOUND_REQUEST":
                        int soundIdListen = Integer.parseInt(responseList.get(responsesListIndex++));

                        ResultSet listenSet = database.searchDatabase("SELECT *\n" +
                                "FROM `file` INNER JOIN `file_sharing`\n" +
                                "ON `file`.`id` = `file_sharing`.`file_id`\n" +
                                "WHERE (`file`.`owner_id` = " + accountId + "\n" +
                                "OR `file_sharing`.`account_id` = " + accountId + ")\n" +
                                "AND `file`.`id` = " + soundIdListen + ";");
                        if (!listenSet.next())
                        {
                            sendMsg.println("LISTEN_SOUND_ERROR");
                            break;
                        }

                        sendFileData(listenSet.getString("path"));
                        sendMsg.println("LISTEN_SOUND_APPROVED");
                        break;
                    case "CHANGE_USER_TYPE":
                        String userIdUserType = responseList.get(responsesListIndex++);
                        String userIdTypeType = responseList.get(responsesListIndex++);

                        ResultSet chngUserTypeSet = database.searchDatabase("SELECT `type`\n" +
                                "FROM `account`\n" +
                                "WHERE `id` = " + accountId + ";");
                        chngUserTypeSet.next();
                        if (!chngUserTypeSet.getString("type").equals("admin"))
                        {
                            break;
                        }

                        chngUserTypeSet = database.searchDatabase("SELECT `type`\n" +
                                "FROM `account`\n" +
                                "WHERE `id` = " + userIdUserType + ";");
                        if (chngUserTypeSet.next())
                        {
                            database.execUpdate("UPDATE `account` SET\n" +
                                    "`type` = \"" + userIdTypeType + "\"\n" +
                                    "WHERE `id` = " + userIdUserType);
                        }
                        else
                        {
                            break;
                        }
                        break;
                    case "PING":
                        pingAck = true;
                        pingDelay = 0;
                        break;
                    case "HOST_EXITING":
                        keepAlive = false;
                        break;
                }

            }

            if (!keepAlive) break;

            while (commandsListIndex < commandsList.size())
            {
                //System.out.println("read command " + commandsList.get(commandsListIndex));
                if (commandsList.get(commandsListIndex).equals("PING"))
                {
                    pingAck = false;
                }
                sendMsg.println(commandsList.get(commandsListIndex));
                commandsListIndex++;
            }


            if (!pingAck) pingDelay++;
            if (pingDelay >= 3)
            {
                assumeDead = true;
                keepAlive = false;
                System.out.println("HOST " + talksWith + " is dead");
            }
        }
        System.out.println("COMTHREADEND");
        assumeDead = true;
        return null;
    }
}

