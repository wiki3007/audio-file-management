// NOTE: get rid of all the sendCommandTo functions in the main loop once the GUI has a manual option send them out
// or don't, do whatever
// Sometimes multiple tasks of same host will have the "Working" status in the database. They just got caught in the middle of being updated when their data was sent out.

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.sql.*;
import java.util.ArrayList;

import java.util.concurrent.*;

/**
 * Main server thread, mainframe of the entire program
 */
public class ServerThread implements Callable<String> {
    /**
     * Array of remote hosts that have ever registered under the server
     */
    //private ArrayList<RemoteHostMasterThread> remoteHost = new ArrayList<>();
    /**
     * Array of remote hosts converted to Future class by ExecutorService
     */
    //private ArrayList<Future<String>> remoteHostFuture = new ArrayList<>();
    /**
     * Array of server threads responsible for communicating with remote hosts over the network
     */
    private ArrayList<ServerComThread> serverCom = new ArrayList<>();
    /**
     * Array of communications server threads converted to Future class by ExecutorService
     */
    //private ArrayList<Future<String>> serverComFuture = new ArrayList<>();
    //private Future<ServerAwait> serverAwaitFuture = null;
    private ServerAwait serverAwait = null;
    private Future<ServerComThread> serverAwaitFuture;
    /**
     * Port for receiving network communications
     */
    //private int port = 51707;
    private int port = 53529;
    /**
     * Server Socket to handle network communications
     */
    private ServerSocket serverSocket = new ServerSocket(port);
    /**
     * Server address
     */
    InetAddress serverAddress = InetAddress.getLocalHost();
    /**
     * ExecutorService with a cached thread pool
     */
    private ExecutorService exec = Executors.newCachedThreadPool();
    /**
     * Keep server alive, change this to false if you want the server to quit
     */
    public boolean keepAlive = true;

    ExecutorService awaitExec = Executors.newFixedThreadPool(1);

    /**
     * ID to be used for identification of ServerComThread and RemoteHostMasterThread, same value for both if they're the ones talking with each other
     */
    public static int id = 0;

    /**
     * Database connectivity
     */
    private DBConnection database = new DBConnection();

    public ServerThread() throws IOException, SQLException, ClassNotFoundException {
        serverSocket.setSoTimeout(1000);
        //System.out.println("Awaiting connection on: " + serverSocket.getInetAddress() + ":" + serverSocket.getLocalPort());
    }

    /**
     * Adds and immediately starts a new remote host
     */
    public void addNewRemoteHost() throws IOException, ExecutionException, InterruptedException {
        //RemoteHostMasterThread tempThread = new RemoteHostMasterThread(serverAddress, port, id);
        //remoteHost.add(tempThread);
        //remoteHostFuture.add(exec.submit(tempThread));
        /*
        if (id != 0) // for every host other than first wait for the SoTimeout before moving on with the program
        {
            Socket socket = null;
            try
            {
                socket = serverSocket.accept();
                System.out.println("New connection made");
                socket.setSoTimeout(1000);
                ServerComThread tempServerCom = new ServerComThread(socket, id);
                serverCom.add(tempServerCom);
                exec.submit(tempServerCom);
                //serverComFuture.add(exec.submit(tempServerCom));
                System.out.println("ServerThread added new host " + id);
                id++;
            }
            catch (SocketTimeoutException noNewConnections)
            {
                System.out.println("No new connections made\nPlease connect to: " + serverAddress);
            }
        }
        else // indefinitely wait first host connection
        {
            do {
                Socket socket = null;
                try
                {
                    socket = serverSocket.accept();
                    System.out.println("New connection made");
                    socket.setSoTimeout(1000);
                    ServerComThread tempServerCom = new ServerComThread(socket, id);
                    serverCom.add(tempServerCom);
                    exec.submit(tempServerCom);
                    //serverComFuture.add(exec.submit(tempServerCom));
                    System.out.println("ServerThread added new host " + id);
                    id++;
                    break;
                }
                catch (SocketTimeoutException noNewConnections)
                {
                    System.out.println("Awaiting first host connection\nPlease connect to: " + serverAddress);
                }
            } while (true);
        }

         */

        // If not currently waiting for new host, start
        //System.out.println("ATTEMPTING NEW CONNECTION");
        if (serverAwait == null)
        {
            //System.out.println("test");
            serverAwait = new ServerAwait(serverSocket);
            serverAwaitFuture = awaitExec.submit(serverAwait);
        }
        // If no new connections formed, start waiting again
        else if (serverAwait.status.equals("NoNewConnections"))
        {
            //System.out.println("test2");
            serverAwait = new ServerAwait(serverSocket);
            serverAwaitFuture = awaitExec.submit(serverAwait);
        }
        else if (serverAwaitFuture.isDone())
        {
            //System.out.println("test3");
            ServerComThread tempCom = serverAwaitFuture.get();
            serverCom.add(tempCom);
            exec.submit(tempCom);

            serverAwait = new ServerAwait(serverSocket);
            serverAwaitFuture = awaitExec.submit(serverAwait);
            System.out.println("New host with ID " + tempCom.talksWith + " connected");

        }
        else
        {
            //System.out.println("Awaiting host connection\nPlease connect to: " + serverAddress);
        }
    }

    /**
     * Cancels remote host with given ID. Does nothing if the host is already canceled or done
     * @param id ID of remote host to cancel
     * @param interruptIfRunning Whether to cancel host immediately or wait until host ends naturally
     */
    /*
    public void cancelHost(int id, boolean interruptIfRunning)
    {
        if (id < remoteHost.size())
        {
            Future<String> host = remoteHostFuture.get(id);
            if (host.isCancelled()) return;
            else if (host.isDone()) return;
            host.cancel(interruptIfRunning);
        }
    }

     */

    /**
     * Remote host getter
     * @return ArrayList of remote hosts
     */
    /*
    public ArrayList<RemoteHostMasterThread> getRemoteHost()
    {
        return remoteHost;
    }

     */

    /**
     * Remote host Future getter
     * @return ArrayList of Future class remote hosts
     */
    /*
    public ArrayList<Future<String>> getRemoteHostFuture()
    {
        return remoteHostFuture;
    }

     */

    /**
     * @deprecated Use {@link #sendCommandTo(ServerComThread, String)} instead, offers better reliability if order of communication threads changes for whatever reason.
     * Sends given command to Server Communication Thread responsible for remote host of given ID
     * @param hostId ID of remote host to give command to
     * @param command Command to give
     */
    @Deprecated
    public void sendCommandTo(int hostId, String command)
    {
        if (!serverCom.get(hostId).assumeDead)
        {
            serverCom.get(hostId).receiveCommands(command);
            System.out.println("ServerThread sent command " + command + " to " + hostId);
        }
    }

    /**
     * Sends given command to Server Communication Thread responsible for remote host of given ID
     * @param com communication thread that talks with the host to send the command to
     * @param command Command to give
     */
    public void sendCommandTo(ServerComThread com, String command)
    {
        if (!com.assumeDead)
        {
            com.receiveCommands(command);
            //System.out.println("ServerThread sent command " + command + " to " + com.talksWith);
        }
    }


    @Override
    public String call() throws Exception {
        // DROP previous DB
        //database.execUpdate("DROP DATABASE " + database.getDbname());

        // USE or CREATE database
        if (database.execUpdate("USE " + database.getDbname()) != -1) // if database exists, good
        {
            System.out.println("Database \"" + database.getDbname() + "\" successfully selected");
        }
        else // time to create it
        {
            System.out.println("No database \"" + database.getDbname() + "\" found, creating...");
            // create db
            if (database.execUpdate("CREATE DATABASE " + database.getDbname()) != -1)
            {
                System.out.println("Database \""+ database.getDbname() + "\" created");
            }
            else
            {
                System.out.println("Error creating database \"" + database.getDbname() + "\"");
            }

            // select it
            if (database.execUpdate("USE " + database.getDbname()) != -1)
            {
                System.out.println("Database \"" + database.getDbname() + "\" successfully selected");
            }
            else
            {
                System.out.println("Unrecoverable error selecting database \"" + database.getDbname() + "\", contact system administrator");
                return "UNRECOVERABLE DATABASE ERROR";
            }

            // create account table
            if (database.execUpdate("CREATE TABLE `account` (\n" +
                    "  `id` int(6) UNSIGNED NOT NULL,\n" +
                    "  `name` varchar(32) NOT NULL,\n" +
                    "  `password` varchar(80) NOT NULL,\n" +
                    "  `type` enum('admin','standard','guest') NOT NULL DEFAULT 'standard'\n" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;") != -1)
            {
                System.out.println("Table \"account\" created");
            }
            else
            {
                System.out.println("Error creating table \"account\", contact system administrator");
                //execUpdate(statement, "DROP DATABASE " + dbname);
                return "TABLE CREATION ERROR";
            }

            // insert base server and admin accounts
            if (database.execUpdate("INSERT INTO `account` (`id`, `name`, `password`, `type`) VALUES\n" +
                    "(1, 'server', 'chj54897hf9ui45uibnrf7bhr6iumje5k90v345j98mi', 'admin'),\n" +
                    "(2, 'admin', 'admin', 'admin');") != -1)
            {
                System.out.println("Accounts \"server\" and \"admin\" created");
            }
            else
            {
                System.out.println("Error creating accounts \"server\" or \"admin\", contact system administrator");
                return "TABLE CREATION ERROR";
            }


            // create file table
            if (database.execUpdate("CREATE TABLE `file` (\n" +
                    "  `id` int(6) UNSIGNED NOT NULL,\n" +
                    "  `owner_id` int(6) UNSIGNED NOT NULL,\n" +
                    "  `name` varchar(32) NOT NULL,\n" +
                    "  `description` varchar(500) DEFAULT NULL,\n" +
                    "  `path` varchar(80) NOT NULL,\n" +
                    "  `duration` varchar(10) DEFAULT NULL,\n" +
                    "  `size` int(10) DEFAULT NULL,\n" +
                    "  `format` enum('flac','mp3','ogg','raw','wav','wma','webm') NOT NULL,\n" +
                    "  `type` enum('public','private') NOT NULL,\n" +
                    "  `date_added` timestamp NOT NULL DEFAULT current_timestamp()\n" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;") != -1)
            {
                System.out.println("Table \"file\" created");
            }
            else
            {
                System.out.println("Error creating table \"file\", contact system administrator");
                //execUpdate(statement, "DROP DATABASE " + dbname);
                return "TABLE CREATION ERROR";
            }

            // create file_sharing table
            if (database.execUpdate("CREATE TABLE `file_sharing` (\n" +
                    "  `file_id` int(6) UNSIGNED NOT NULL,\n" +
                    "  `account_id` int(6) UNSIGNED NOT NULL\n" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;") != -1)
            {
                System.out.println("Table \"file_sharing\" created");
            }
            else
            {
                System.out.println("Error creating table \"file_sharing\", contact system administrator");
                //execUpdate(statement, "DROP DATABASE " + dbname);
                return "TABLE CREATION ERROR";
            }

            // create list_main table
            if (database.execUpdate("CREATE TABLE `list_main` (\n" +
                    "  `id` int(6) UNSIGNED NOT NULL,\n" +
                    "  `owner_id` int(6) UNSIGNED NOT NULL,\n" +
                    "  `name` varchar(80) NOT NULL,\n" +
                    "  `description` varchar(500) DEFAULT NULL,\n" +
                    "  `type` enum('private','public') NOT NULL\n" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;") != -1)
            {
                System.out.println("Table \"list_main\" created");
            }
            else
            {
                System.out.println("Error creating table \"list_main\", contact system administrator");
                //execUpdate(statement, "DROP DATABASE " + dbname);
                return "TABLE CREATION ERROR";
            }

            // create list_contents table
            if (database.execUpdate("CREATE TABLE `list_contents` (\n" +
                    "  `list_id` int(6) UNSIGNED NOT NULL,\n" +
                    "  `file_id` int(6) UNSIGNED NOT NULL\n" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;") != -1)
            {
                System.out.println("Table \"list_contents\" created");
            }
            else
            {
                System.out.println("Error creating table \"list_contents\", contact system administrator");
                //execUpdate(statement, "DROP DATABASE " + dbname);
                return "TABLE CREATION ERROR";
            }

            // create list_sharing table
            if (database.execUpdate("CREATE TABLE `list_sharing` (\n" +
                    "  `list_id` int(6) UNSIGNED NOT NULL,\n" +
                    "  `account_id` int(6) UNSIGNED NOT NULL\n" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;") != -1)
            {
                System.out.println("Table \"list_sharing\" created");
            }
            else
            {
                System.out.println("Error creating table \"list_sharing\", contact system administrator");
                //execUpdate(statement, "DROP DATABASE " + dbname);
                return "TABLE CREATION ERROR";
            }

            // create indexes on account
            if (database.execUpdate("ALTER TABLE `account`\n" +
                    "  ADD PRIMARY KEY (`id`),\n" +
                    "  ADD UNIQUE KEY `name` (`name`);") != -1)
            {
                System.out.println("Indexes on \"account\" created");
            }
            else
            {
                System.out.println("Error creating indexes on \"account\", contact system administrator");
                //execUpdate(statement, "DROP DATABASE " + dbname);
                return "TABLE CREATION ERROR";
            }

            // create indexes on file
            if (database.execUpdate("ALTER TABLE `file`\n" +
                    "  ADD PRIMARY KEY (`id`),\n" +
                    "  ADD KEY `owner_id` (`owner_id`),\n" +
                    "  ADD KEY `name` (`name`);") != -1)
            {
                System.out.println("Indexes on \"file\" created");
            }
            else
            {
                System.out.println("Error creating indexes on \"file\", contact system administrator");
                //execUpdate(statement, "DROP DATABASE " + dbname);
                return "TABLE CREATION ERROR";
            }

            // create indexes on file_sharing
            if (database.execUpdate("ALTER TABLE `file_sharing`\n" +
                    "  ADD PRIMARY KEY (`file_id`,`account_id`),\n" +
                    "  ADD KEY `account_id` (`account_id`);") != -1)
            {
                System.out.println("Indexes on \"file_sharing\" created");
            }
            else
            {
                System.out.println("Error creating indexes on \"file_sharing\", contact system administrator");
                //execUpdate(statement, "DROP DATABASE " + dbname);
                return "TABLE CREATION ERROR";
            }

            // create indexes on list_contents
            if (database.execUpdate("ALTER TABLE `list_contents`\n" +
                    "  ADD PRIMARY KEY (`list_id`,`file_id`),\n" +
                    "  ADD KEY `file_id` (`file_id`);") != -1)
            {
                System.out.println("Indexes on \"list_contents\" created");
            }
            else
            {
                System.out.println("Error creating indexes on \"list_contents\", contact system administrator");
                //execUpdate(statement, "DROP DATABASE " + dbname);
                return "TABLE CREATION ERROR";
            }

            // create indexes on list_main
            if (database.execUpdate("ALTER TABLE `list_main`\n" +
                    "  ADD PRIMARY KEY (`id`),\n" +
                    "  ADD KEY `owner_id` (`owner_id`);") != -1)
            {
                System.out.println("Indexes on \"list_main\" created");
            }
            else
            {
                System.out.println("Error creating indexes on \"list_main\", contact system administrator");
                //execUpdate(statement, "DROP DATABASE " + dbname);
                return "TABLE CREATION ERROR";
            }

            // create indexes on list_sharing
            if (database.execUpdate("ALTER TABLE `list_sharing`\n" +
                    "  ADD PRIMARY KEY (`list_id`,`account_id`),\n" +
                    "  ADD KEY `account_id` (`account_id`);") != -1)
            {
                System.out.println("Indexes on \"list_sharing\" created");
            }
            else
            {
                System.out.println("Error creating indexes on \"list_sharing\", contact system administrator");
                //execUpdate(statement, "DROP DATABASE " + dbname);
                return "TABLE CREATION ERROR";
            }

            // create auto_increment on account
            if (database.execUpdate("ALTER TABLE `account`\n" +
                    "  MODIFY `id` int(6) UNSIGNED NOT NULL AUTO_INCREMENT;") != -1)
            {
                System.out.println("Auto increment on \"account\" created");
            }
            else
            {
                System.out.println("Error creating auto increment on \"account\", contact system administrator");
                //execUpdate(statement, "DROP DATABASE " + dbname);
                return "TABLE CREATION ERROR";
            }

            // create auto_increment on file
            if (database.execUpdate("ALTER TABLE `file`\n" +
                    "  MODIFY `id` int(6) UNSIGNED NOT NULL AUTO_INCREMENT;") != -1)
            {
                System.out.println("Auto increment on \"file\" created");
            }
            else
            {
                System.out.println("Error creating auto increment on \"file\", contact system administrator");
                //execUpdate(statement, "DROP DATABASE " + dbname);
                return "TABLE CREATION ERROR";
            }

            // create auto_increment on list_main
            if (database.execUpdate("ALTER TABLE `list_main`\n" +
                    "  MODIFY `id` int(6) UNSIGNED NOT NULL AUTO_INCREMENT;") != -1)
            {
                System.out.println("Auto increment on \"list_main\" created");
            }
            else
            {
                System.out.println("Error creating auto increment on \"list_main\", contact system administrator");
                //execUpdate(statement, "DROP DATABASE " + dbname);
                return "TABLE CREATION ERROR";
            }

            // create constraints on file
            if (database.execUpdate("ALTER TABLE `file`\n" +
                    "  ADD CONSTRAINT `file_ibfk_1` FOREIGN KEY (`owner_id`) REFERENCES `account` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;") != -1)
            {
                System.out.println("Constraints on \"file\" created");
            }
            else
            {
                System.out.println("Error creating constraints on \"file\", contact system administrator");
                //execUpdate(statement, "DROP DATABASE " + dbname);
                return "TABLE CREATION ERROR";
            }

            // create constraints on file_sharing
            if (database.execUpdate("ALTER TABLE `file_sharing`\n" +
                    "  ADD CONSTRAINT `file_sharing_ibfk_1` FOREIGN KEY (`file_id`) REFERENCES `file` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,\n" +
                    "  ADD CONSTRAINT `file_sharing_ibfk_2` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;") != -1)
            {
                System.out.println("Constraints on \"file_sharing\" created");
            }
            else
            {
                System.out.println("Error creating constraints on \"file_sharing\", contact system administrator");
                //execUpdate(statement, "DROP DATABASE " + dbname);
                return "TABLE CREATION ERROR";
            }

            // create constraints on list_contents
            if (database.execUpdate("ALTER TABLE `list_contents`\n" +
                    "  ADD CONSTRAINT `list_contents_ibfk_1` FOREIGN KEY (`file_id`) REFERENCES `file` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,\n" +
                    "  ADD CONSTRAINT `list_contents_ibfk_2` FOREIGN KEY (`list_id`) REFERENCES `list_main` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;") != -1)
            {
                System.out.println("Constraints on \"list_contents\" created");
            }
            else
            {
                System.out.println("Error creating constraints on \"list_contents\", contact system administrator");
                //execUpdate(statement, "DROP DATABASE " + dbname);
                return "TABLE CREATION ERROR";
            }

            // create constraints on list_main
            if (database.execUpdate("ALTER TABLE `list_main`\n" +
                    "  ADD CONSTRAINT `list_main_ibfk_1` FOREIGN KEY (`owner_id`) REFERENCES `account` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;") != -1)
            {
                System.out.println("Constraints on \"list_main\" created");
            }
            else
            {
                System.out.println("Error creating constraints on \"list_main\", contact system administrator");
                //execUpdate(statement, "DROP DATABASE " + dbname);
                return "TABLE CREATION ERROR";
            }

            // create constraints on list_sharing
            if (database.execUpdate("ALTER TABLE `list_sharing`\n" +
                    "  ADD CONSTRAINT `list_sharing_ibfk_1` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,\n" +
                    "  ADD CONSTRAINT `list_sharing_ibfk_2` FOREIGN KEY (`list_id`) REFERENCES `list_main` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;") != -1)
            {
                System.out.println("Constraints on \"list_sharing\" created");
            }
            else
            {
                System.out.println("Error creating constraints on \"list_sharing\", contact system administrator");
                //execUpdate(statement, "DROP DATABASE " + dbname);
                return "TABLE CREATION ERROR";
            }
        }

        int delayCounter = 1;
        while (keepAlive)
        {
            addNewRemoteHost();
            delayCounter++;
            //addNewRemoteHost();

            /*
            if (serverCom.isEmpty())
            {
                addNewRemoteHost();
                sendCommandTo(0, "HOST_START_TASK");
                sendCommandTo(0, "7");

                sendCommandTo(0, "HOST_START_TASK");
                sendCommandTo(0, "8");

                sendCommandTo(0, "HOST_START_TASK");
                sendCommandTo(0, "1");

                sendCommandTo(0, "HOST_START_TASK");
                sendCommandTo(0, "3");

                sendCommandTo(0, "HOST_CANCEL_TASK");
                sendCommandTo(0, "2");
                sendCommandTo(0, "TRUE");

                sendCommandTo(0, "HOST_START_TASK");
                sendCommandTo(0, "3");
            }

             */

            // can't have sleep here either, else it just instaquits
            // nevermind, it just decides to fix itself. cool. really cool.
            // I don't know why, but this can't be at the beginning or end of the loop, so instead it has to just awkwardly sit in the middle here
            Thread.sleep(1000);
            //System.out.println("SERVER MAIN LOOP");
            //System.out.println(serverCom);

            //sendCommandTo(serverCom.get(0).talksWith, "HOST_START_TASK");
            //sendCommandTo(serverCom.get(0).talksWith, String.valueOf(delayCounter));

            //sendCommandTo(serverCom.get(1).talksWith, "HOST_START_TASK");
            //sendCommandTo(serverCom.get(1).talksWith, String.valueOf(delayCounter));
            //System.out.println("test");
            //addNewRemoteHost();
            /*
            for (ServerComThread com : serverCom)
            {
                //System.out.println(com);
                // ping host to check if he's still alive
                if (delayCounter == 5)
                {
                    System.out.println("Pinging hosts");
                    sendCommandTo(com, "PING");
                }

                // update database
                sendCommandTo(com, "HOST_RETURN_ALL_TASKS");
                ArrayList<ArrayList<String>> taskDummyArray = com.getSerializationArrayAll();
                //System.out.println("TASK DUMMY ARRAY " + taskDummyArray);
                //if (taskDummyArray.isEmpty()) break;
                //System.out.println("TASK DUMMY ARRAY " + taskDummyArray);
                for (ArrayList<String> task : taskDummyArray)
                {
                    //System.out.println("SQL_COMMAND " + "SELECT * FROM tasks WHERE hostId = " + task.get(0) + " AND taskId = " + task.get(1));
                    ResultSet set = searchDatabase("SELECT * FROM tasks WHERE hostId = " + task.get(0) + " AND taskId = " + task.get(1));
                    //ResultSet set = searchDatabase("SELECT * FROM tasks");
                    //System.out.println(set);
                    //if (!set.next()) break;
                    int hostId = -1;
                    int taskId = -1;
                    int priority = -1;
                    String status = "";
                    String result = "";
                    long serverTimeTaken = 0;
                    long clientTimeTaken = 0;
                    while (set.next()) // even though there's only one result, this has to be in a while loop, because ???, there's only one result anyway, but it's just weird
                    {
                        hostId = set.getInt("hostId");
                        taskId = set.getInt("taskId");
                        priority = set.getInt("priority");
                        status = set.getString("status");
                        result = set.getString("result");
                        serverTimeTaken = set.getLong("serverExecutionTime");
                        clientTimeTaken = set.getLong("clientExecutionTime");
                        //System.out.println(hostId + " " + taskId + " " + priority + " " + status + " " + result);
                    }

                    if (hostId != Integer.parseInt(task.get(0)) && taskId != Integer.parseInt(task.get(1))) // if no conflict with primary keys
                    {
                        //System.out.println("TASK " + taskId + " IS INSERT");
                        if (execUpdate("INSERT INTO `tasks` VALUES(" + task.get(0) + ", " +  task.get(1) + ", " + task.get(2) + ", \"" + task.get(3) +  "\", \"" + task.get(4) + "\", " + task.get(5) + ", " + task.get(6) + ")") != -1);
                        {
                            //System.out.println("RECORD " + hostId + " " + taskId + " " + priority + " " + status + " " + result + " INSERTED");
                        }
                    }
                    else if (priority != Integer.parseInt(task.get(2)) || !status.equals(task.get(3)) || !result.equals(task.get(4))) // if row is different, then update it, executionTimes only change when taks is done, so ignore it in the checks
                    {
                        if (execUpdate("UPDATE `tasks` SET priority = " + task.get(2) + ", status = \"" + task.get(3) + "\", result = \"" + task.get(4) + "\", serverExecutionTime = " + task.get(5) + ", clientExecutionTime = " + task.get(6) + " WHERE hostId = " +  task.get(0) + " AND taskId = " + task.get(1)) != -1)
                        {
                            //System.out.println("RECORD " + hostId + " " + taskId + " " + priority + " " + status + " " + result + " UPDATED");
                        }
                    }
                }
                //keepAlive = false;
            }*/
            //Thread.currentThread().wait(100); // this and sleep make somehow permanently brick the thread. I'm tired of this, so enjoy the spam if you ever put a print in here
            //break;
            if (delayCounter == 5) delayCounter = 0;
        }
        for (ServerComThread com : serverCom)
        {
            sendCommandTo(com, "EXIT_THREAD");
            sendCommandTo(com, "HOST_EXIT_THREAD");
        }
        System.out.println("SERVERTHREADEND");
        exec.shutdownNow();
        return null;
    }
}
