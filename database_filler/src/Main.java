import java.sql.SQLException;
import java.util.Date;
import java.util.Random;
import java.util.Scanner;

public class Main {
    static Random rng = new Random();
    static String randomizeString(double chance, int maxLen)
    {
        String res = "";
        do
        {
            res += Character.toString((char) rng.nextInt(65, 91));
        } while ((rng.nextDouble() < chance && res.length() < maxLen) || res.length() <= 5);

        return res.toString();
    }

    static int randomizeInt(int min, int max)
    {
        return rng.nextInt(min, max+1);
    }

    static String randomizeType(double adminChance)
    {
        if (rng.nextDouble() >= adminChance)
        {
            return "admin";
        }
        return "standard";
    }

    static String randomizeVisibility(double privateChance)
    {
        if (rng.nextDouble() < privateChance)
        {
            return "private";
        }
        return "public";
    }

    static String randomizeFormat()
    {
        double steppingChance = (double) 1 /7;
        double rolled = rng.nextDouble();

        if (rolled < steppingChance * 1) return "flac";
        else if (rolled < steppingChance * 2) return "mp3";
        else if (rolled < steppingChance * 3) return "ogg";
        else if (rolled < steppingChance * 4) return "raw";
        else if (rolled < steppingChance * 5) return "wav";
        else if (rolled < steppingChance * 6) return "wma";
        else return "webm";
    }

    public static void main(String[] args) throws SQLException {
        DBConnection database = new DBConnection();

        // DROP previous DB
        database.execUpdate("DROP DATABASE " + database.getDbname());

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
            }
        }

        int accountAmount = 100, fileAmount = 100, listAmount = 100, fileShareAmount = 100, listShareAmount = 100, listFillAmount = 100;
        Scanner in = new Scanner(System.in);

        System.out.println("\n----------------------\n");
        System.out.println("Amounts to fill in");
        System.out.print("Accounts: "); accountAmount = in.nextInt();
        System.out.print("Files: "); fileAmount = in.nextInt();
        System.out.print("Lists: "); listAmount = in.nextInt();
        System.out.print("File shares: "); fileShareAmount = in.nextInt();
        System.out.print("List contents: "); listFillAmount = in.nextInt();
        System.out.print("List shares: "); listShareAmount = in.nextInt();
        System.out.println("\n----------------------\n");

        for (int i=0; i<accountAmount; i++)
        {
            String name = randomizeString(0.95, 32);
            String password = randomizeString(0.8, 80);
            String type = randomizeType(0.01);
            database.execUpdate("INSERT INTO `account`\n" +
                    "VALUES (NULL, \"" + name + "\", \"" + password + "\", \"" + type + "\");");
        }
        System.out.println("Accounts added");

        for (int i=0; i<fileAmount; i++)
        {
            int ownerId = randomizeInt(1, accountAmount+2);
            String name = randomizeString(0.80, 32);
            String description = randomizeString(0.99, 500);
            String path = "./sound_files/" + name;
            String hour = "0" + String.valueOf(randomizeInt(0, 1));
            String minute = String.valueOf(randomizeInt(0, 59));
            if (Integer.parseInt(minute) < 10) minute = "0" + minute;
            String second = String.valueOf(randomizeInt(0, 59));
            if (Integer.parseInt(second) < 10) second = "0" + second;
            String duration = hour + ":" + minute + ":" + second;
            int size = randomizeInt(1, 999999999);
            String format = randomizeFormat();
            String type = randomizeVisibility(0.1);

            database.execUpdate("INSERT INTO `file`(`owner_id`, `name`, `description`, `path`, `duration`, `size`, `format`, `type`)\n" +
                    "VALUES (" + ownerId + ", \"" + name + "\", \"" + description + "\", \"" + path + "\", \"" + duration + "\", " + size + ", \"" + format + "\", \"" + type + "\");");
        }
        System.out.println("Files added");

        for (int i=0; i<listAmount; i++)
        {
            int ownerId = randomizeInt(1, accountAmount+2);
            String name = randomizeString(0.90, 80);
            String description = randomizeString(0.99, 500);
            String type = randomizeVisibility(0.75);

            database.execUpdate("INSERT INTO `list_main`(`owner_id`, `name`, `description`, `type`)\n" +
                    "VALUES (\"" + ownerId + "\", \"" + name + "\", \"" + description + "\", \"" + type + "\");");
        }
        System.out.println("Lists added");

        for (int i=0; i<fileShareAmount; i++)
        {
            database.execUpdate("INSERT INTO `file_sharing`(`file_id`, `account_id`)\n" +
                    "VALUES (" + randomizeInt(1, fileAmount) + ", " + randomizeInt(2, accountAmount+2) + ");");

        }
        System.out.println("File sharing added");

        for (int i=0; i<listFillAmount; i++)
        {
            database.execUpdate("INSERT INTO `list_contents`(`list_id`, `file_id`)\n" +
                    "VALUES (" + randomizeInt(1, listAmount) + ", " + randomizeInt(1, fileAmount) + ");");
        }
        System.out.println("List contents added");

        for (int i=0; i<listShareAmount; i++)
        {
            database.execUpdate("INSERT INTO `list_sharing`(`list_id`, `account_id`)\n" +
                    "VALUES (" + randomizeInt(1, listAmount) + ", " + randomizeInt(2, accountAmount+2) + ");");
        }
        System.out.println("List sharing added");
    }
}