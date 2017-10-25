/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package connectionsgraph;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Random;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 *
 * @author Max
 */
public class Implementation {

    private String RUN_PROVISION_FIRST = "Please call \"provision\" first.";
    private String UNKNOWN_COMMAND = "Unknown command.";
    private String WRONG_PARAMETERS = "Wrong number of parameters.";
    private String PATH_NOT_FOUND = "Path not found.";
    private String DIRECTORY_NOT_EMPTY = "Directory not empty.";
    private String NOT_A_DIRECTORY = "The given path is not a directory.";
    private String OK = "OK";

    private HashSet firstDegreeSet = null;
    private HashSet secondDegreeSet = null;
    private Hashtable<Integer, List> friendsMap = null;
    private boolean provisioned = false;

    /**
     * Infinite loop in which we accept connections and respond to requests
     */
    public void run() throws IOException {
        try (ServerSocket listener = new ServerSocket(9090)) {
            while (true) {
                try (Socket socket = listener.accept()) {
                    while (!socket.isClosed()) {
                        BufferedReader reader = new BufferedReader(
                                new InputStreamReader(socket.getInputStream()));
                        String line = reader.readLine();
                        BufferedWriter writer = new BufferedWriter(
                                new OutputStreamWriter(socket.getOutputStream()));
                        respond(line, writer);
                        if (!socket.isClosed()) {
                            writer.flush();
                        }
                    }
                } catch (SocketException ex) {
                    // Ignore socket exceptions
                }
            }
        }
    }

    /**
     * Populates a directory according to the specified parameters
     *
     * @param path The path used to store the data
     * @param nUsers The number of users to generate
     * @param avg The average number of friends each user has
     * @param std The stdandard deviation of the friends distribution
     */
    private void populate(Path path, long nUsers, int avg, float std)
            throws FileNotFoundException, IOException, URISyntaxException {

        if (Files.notExists(path)) {
            throw new FileNotFoundException(PATH_NOT_FOUND);
        } else if (!Files.isDirectory(path)) {
            throw new IOException(NOT_A_DIRECTORY);
        }

        Stream<Path> list = Files.list(path);
        if (list.count() > 0) {
            throw new IOException(DIRECTORY_NOT_EMPTY);
        }

        Random rnd = new Random();
        HashSet friends = new HashSet();
        for (long userId = 0; userId < nUsers; ++userId) {
            friends.clear();
            Path userPath = Paths.get(new URI(
                    path.toUri().toString() + "/" + Long.toString(userId)));
            Files.createDirectory(userPath);

            // Sample number of friends from a Gaussian distribution 
            // with mean = avg and standard deviation = std
            int nFriends = (int) Math.floor(rnd.nextGaussian() * std + avg);

            // Add friends to hashset
            for (int i = 0; i < nFriends; ++i) {
                // Note: this is not quite the appropriate way, but random enough
                long uid = Long.remainderUnsigned(rnd.nextLong(), nUsers);
                if (!friends.add(uid)) {
                    // Friend was already there, retry
                    --i;
                } else {
                    // Create file
                    Path friendPath = Paths.get(new URI(
                            userPath.toUri().toString() + "/" + Long.toString(uid)));
                    Files.createFile(friendPath);
                }
            }
        }
    }

    /**
     * Reads data from a directory and populates in memory data structures
     *
     * @param path The path used to store the data
     * @param nUsers The number of users to generate
     * @param avg The average number of friends each user has
     * @param std The stdandard deviation of the friends distribution
     */
    private void provision(Path path)
            throws FileNotFoundException, IOException {
        if (Files.notExists(path)) {
            throw new FileNotFoundException(PATH_NOT_FOUND);
        } else if (!Files.isDirectory(path)) {
            throw new IOException(NOT_A_DIRECTORY);
        }

        firstDegreeSet = new HashSet();
        secondDegreeSet = new HashSet();
        friendsMap = new Hashtable<Integer, List>();

        File[] files = path.toFile().listFiles();
        for (int i = 0; i < files.length; ++i) {
            File file = files[i];
            if (file.isDirectory()) {
                int uid1 = Integer.parseInt(file.getName());
                List friends = new ArrayList();
                File[] innerFiles = file.listFiles();

                for (int j = 0; j < innerFiles.length; ++j) {
                    File innferFile = innerFiles[j];
                    if (innferFile.isFile()) {
                        int uid2 = Integer.parseInt(innferFile.getName());
                        friends.add(uid2);
                        long relationship = (long) uid1 << 32 | (long) uid2;
                        firstDegreeSet.add(relationship);
                    }
                }
                friendsMap.put(uid1, friends);
            }

        }
        friendsMap.keySet().forEach((Integer uid1) -> {
            friendsMap.get(uid1).forEach((Object obj) -> {
                Integer uid2 = (Integer) obj;
                friendsMap.get(uid2).forEach((Object obj2) -> {
                    Integer uid3 = (Integer) obj2;
                    long relationship = (long) uid1 << 32 | (long) uid3;
                    secondDegreeSet.add(relationship);
                });
            });
        });

        provisioned = true;
    }

    /**
     * Returns the friends for a given user id
     *
     * @param uid The id of the user of whom to get the friends list
     *
     * @return A List of int user ids
     *
     * @throws Exception
     */
    private List getFriends(int uid)
            throws Exception {
        if (!provisioned) {
            throw new Exception(RUN_PROVISION_FIRST);
        }
        return friendsMap.get(uid);
    }

    /**
     * Returns true if the users are directly friends, false o.w.
     *
     * @param uid First user id to use for lookup
     * @param uid2 Second user id to use for lookup
     *
     * @throws Exception
     */
    private boolean areFirstDegree(int uid, int uid2)
            throws Exception {
        if (!provisioned) {
            throw new Exception(RUN_PROVISION_FIRST);
        }
        boolean result = false;
        long relationship = (long) uid << 32 | (long) uid2;
        if (firstDegreeSet.contains(relationship)) {
            result = true;
        }
        return result;
    }

    /**
     * Returns true if the users are 2nd degree connected (friend of friend)
     *
     * @param uid First user id to use for lookup
     * @param uid2 Second user id to use for lookup
     *
     * @throws Exception
     */
    private boolean areSecondDegree(int uid, int uid2)
            throws Exception {
        if (!provisioned) {
            throw new Exception(RUN_PROVISION_FIRST);
        }
        boolean result = false;
        long relationship = (long) uid << 32 | (long) uid2;
        if (secondDegreeSet.contains(relationship)) {
            result = true;
        }
        return result;
    }

    /**
     * Returns true if the users are 3rd degree connected (friend of friend of
     * friend)
     *
     * @param uid First user id to use for lookup
     * @param uid2 Second user id to use for lookup
     *
     * @throws Exception
     */
    private boolean areThirdDegree(int uid, int uid2)
            throws Exception {
        if (!provisioned) {
            throw new Exception(RUN_PROVISION_FIRST);
        }
        boolean result = false;
        List friends = friendsMap.get(uid);
        Iterator iter = friends.iterator();
        try {
            while (true) {
                Object o = iter.next();
                Integer friendId = (Integer) o;
                long secondDegreeRelationship = (long) friendId << 32 | (long) uid2;
                if (secondDegreeSet.contains(secondDegreeRelationship)) {
                    result = true;
                    break;
                }
            }
        } catch (NoSuchElementException ex) {
            // End of iteration
        }

        return result;
    }

    /**
     * Parses the given command line
     *
     * @param commandLine Command line
     *
     * @return <? extends Command> object or throws
     *
     * @throws Exception
     */
    private Command parseCommand(String commandLine)
            throws Exception {
        Command result = null;

        String[] split = commandLine.split("\\s");

        Path path;
        long nUsers;
        int avg;
        float std;
        int uid, uid2;

        Pattern commandPattern = Pattern.compile("^(quit|populate|provision|getFriends|areFirstDegree|areSecondDegree|areThirdDegree)([(](.*)[)])$");
        Matcher m = commandPattern.matcher(commandLine);
        boolean matches = m.matches();

        if (matches) {
            String command = m.group(1);
            String params = m.group(3);
            String[] paramArray = params.split(",");
            switch (command) {
                case "quit":
                    result = new QuitCommand();
                    break;
                case "populate":
                    if (paramArray.length < 4 || paramArray.length > 4) {
                        throw new Exception(WRONG_PARAMETERS);
                    }
                    path = Paths.get(paramArray[0].trim());
                    nUsers = Long.parseLong(paramArray[1].trim());
                    avg = Integer.parseInt(paramArray[2].trim());
                    std = Float.parseFloat(paramArray[3].trim());
                    result = new PopulateCommand(path, nUsers, avg, std);
                    break;
                case "provision":
                    if (paramArray.length < 1 || paramArray.length > 1) {
                        throw new Exception(WRONG_PARAMETERS);
                    }
                    path = Paths.get(paramArray[0].trim());
                    result = new ProvisionCommand(path);
                    break;
                case "getFriends":
                    if (paramArray.length < 1 || paramArray.length > 1) {
                        throw new Exception(WRONG_PARAMETERS);
                    }
                    uid = Integer.parseInt(paramArray[0].trim());
                    result = new GetFriendsCommand(uid);
                    break;
                case "areFirstDegree":
                    if (paramArray.length < 2 || paramArray.length > 2) {
                        throw new Exception(WRONG_PARAMETERS);
                    }
                    uid = Integer.parseInt(paramArray[0].trim());
                    uid2 = Integer.parseInt(paramArray[1].trim());
                    result = new AreFirstDegreeCommand(uid, uid2);
                    break;
                case "areSecondDegree":
                    if (paramArray.length < 2 || paramArray.length > 2) {
                        throw new Exception(WRONG_PARAMETERS);
                    }
                    uid = Integer.parseInt(paramArray[0].trim());
                    uid2 = Integer.parseInt(paramArray[1].trim());
                    result = new AreSecondDegreeCommand(uid, uid2);
                    break;
                case "areThirdDegree":
                    if (paramArray.length < 2 || paramArray.length > 2) {
                        throw new Exception(WRONG_PARAMETERS);
                    }
                    uid = Integer.parseInt(paramArray[0].trim());
                    uid2 = Integer.parseInt(paramArray[1].trim());
                    result = new AreThirdDegreeCommand(uid, uid2);
                    break;
            }
        }

        return result;
    }

    /**
     * Responds to the given command, outputs to <b>writer</b>
     *
     * @param command line
     * @param writer A BufferedWriter
     *
     * @throws IOException
     */
    private void respond(String command, BufferedWriter writer)
            throws IOException {
        try {
            Command cmd = parseCommand(command.trim());

            if (cmd != null) {
                switch (cmd.type) {
                    case QUIT:
                        writer.close();
                        break;
                    case POPULATE:
                        populate((Path) cmd.get("path"), (long) cmd.get("nUsers"),
                                (int) cmd.get("avg"), (float) cmd.get("std"));
                        writer.write(OK);
                        writer.newLine();
                        break;
                    case PROVISION:
                        provision((Path) cmd.get("path"));
                        writer.write(OK);
                        writer.newLine();
                        break;
                    case GET_FRIENDS:
                        List friends = getFriends((int) cmd.get("uid"));

                        // Could probably use Stream.map, but it's really that
                        // simple...
                        int size = friends.size();
                        for (int i = 0; i < size; ++i) {
                            writer.write(Integer.toString((int) friends.get(i)));
                            if (i < size - 1) {
                                writer.write(", ");
                            }
                        }

                        writer.newLine();
                        writer.write(OK);
                        writer.newLine();
                        break;
                    case ARE_FIRST_DEGREE:
                        if (areFirstDegree((int) cmd.get("uid"), (int) cmd.get("uid2"))) {
                            writer.write("True");
                            writer.newLine();
                        } else {
                            writer.write("False");
                            writer.newLine();
                        }
                        break;
                    case ARE_SECOND_DEGREE:
                        if (areSecondDegree((int) cmd.get("uid"), (int) cmd.get("uid2"))) {
                            writer.write("True");
                            writer.newLine();
                        } else {
                            writer.write("False");
                            writer.newLine();
                        }
                        break;
                    case ARE_THIRD_DEGREE:
                        if (areThirdDegree((int) cmd.get("uid"), (int) cmd.get("uid2"))) {
                            writer.write("True");
                            writer.newLine();
                        } else {
                            writer.write("False");
                            writer.newLine();
                        }
                        break;
                    default:
                        writer.write(UNKNOWN_COMMAND);
                        writer.newLine();
                        break;
                }
            } else {
                writer.write(UNKNOWN_COMMAND);
                writer.newLine();
            }
        } catch (Exception ex) {
            try {
                writer.write("Exception: " + ex.getMessage());
                writer.newLine();
            } catch (IOException ex2) {
                // Ignore Exceptions while writing error message to client
            }
        }
    }
}
