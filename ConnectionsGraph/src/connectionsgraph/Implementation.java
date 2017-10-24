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
//import net.yadan.banana.map.HashMap;
//import net.yadan.banana.map.IHashMap;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.security.KeyException;
import java.util.ArrayList;
import java.util.Random;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

/**
 *
 * @author Max
 */
public class Implementation {

    private enum COMMAND_TYPE {
        PROVISION,
        READ_PROVISION,
        GET_FRIENDS,
        ARE_FIRST_DEGREE,
        ARE_SECOND_DEGREE,
        ARE_THIRD_DEGREE
    }

    class Command {

        COMMAND_TYPE type;
        HashMap parameters;

        Command(COMMAND_TYPE type, HashMap parameters) {
            this.type = type;
            this.parameters = parameters;
        }

        public Object get(Object key) throws NullPointerException, KeyException {
            if (parameters != null) {
                return parameters.get(key);
            }
            throw new NullPointerException("Command.parameters is null.");
        }
    }

    class ProvisionCommand extends Command {

        private ProvisionCommand(COMMAND_TYPE type, HashMap paramters) {
            super(type, paramters);
        }

        ProvisionCommand(Path path, long nUsers, int avg, float std) {
            super(COMMAND_TYPE.PROVISION, null);
            parameters = new HashMap(4);
            parameters.put("path", path);
            parameters.put("nUsers", nUsers);
            parameters.put("avg", avg);
            parameters.put("std", std);
        }
    }

    class ReadProvisionCommand extends Command {

        private ReadProvisionCommand(COMMAND_TYPE type, HashMap paramters) {
            super(type, paramters);
        }

        ReadProvisionCommand(Path path) {
            super(COMMAND_TYPE.READ_PROVISION, null);
            parameters = new HashMap(1);
            parameters.put("path", path);
        }
    }

    class GetFriendsCommand extends Command {

        private GetFriendsCommand(COMMAND_TYPE type, HashMap paramters) {
            super(type, paramters);
        }

        GetFriendsCommand(int uid) {
            super(COMMAND_TYPE.GET_FRIENDS, null);
            parameters = new HashMap(1);
            parameters.put("uid", uid);
        }
    }

    class AreFirstDegreeCommand extends Command {

        private AreFirstDegreeCommand(COMMAND_TYPE type, HashMap paramters) {
            super(type, paramters);
        }

        AreFirstDegreeCommand(int uid, int uid2) {
            super(COMMAND_TYPE.ARE_FIRST_DEGREE, null);
            parameters = new HashMap(1);
            parameters.put("uid", uid);
            parameters.put("uid2", uid2);
        }
    }

    class AreSecondDegreeCommand extends Command {

        private AreSecondDegreeCommand(COMMAND_TYPE type, HashMap paramters) {
            super(type, paramters);
        }

        AreSecondDegreeCommand(int uid, int uid2) {
            super(COMMAND_TYPE.ARE_SECOND_DEGREE, null);
            parameters = new HashMap(1);
            parameters.put("uid", uid);
            parameters.put("uid2", uid2);
        }
    }

    class AreThirdDegreeCommand extends Command {

        private AreThirdDegreeCommand(COMMAND_TYPE type, HashMap paramters) {
            super(type, paramters);
        }

        AreThirdDegreeCommand(int uid, int uid2) {
            super(COMMAND_TYPE.ARE_THIRD_DEGREE, null);
            parameters = new HashMap(1);
            parameters.put("uid", uid);
            parameters.put("uid2", uid2);
        }
    }

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
                        writer.flush();
                    }
                }
            }
        }
    }

    /**
     * @param path The path used to store the data
     * @param nUsers The number of users to generate
     * @param avg The average number of friends each user has
     * @param std The stdandard deviation of the user distribution
     */
    private void provision(Path path, long nUsers, int avg, float std)
            throws FileNotFoundException, IOException, URISyntaxException {

        if (Files.notExists(path)) {
            throw new FileNotFoundException("Storage directory not found.");
        } else if (!Files.isDirectory(path)) {
            throw new IOException("The given storage path is not a directory.");
        }

        Stream<Path> list = Files.list(path);
        if (list.count() > 0) {
            throw new IOException("Storage diretory is not empty.");
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

    private HashSet firstDegreeSet = null;
    private HashSet secondDegreeSet = null;
    private Hashtable<Integer, List> friendsMap = null;
    private boolean provisioned = false;

    private void readProvision(Path path)
            throws FileNotFoundException, IOException {
        if (Files.notExists(path)) {
            throw new FileNotFoundException("Storage directory not found.");
        } else if (!Files.isDirectory(path)) {
            throw new IOException("The given storage path is not a directory.");
        }

        firstDegreeSet = new HashSet();
        secondDegreeSet = new HashSet();
        friendsMap = new Hashtable<Integer, List>();
        
        File[] files = path.toFile().listFiles();
        for(int i=0; i<files.length; ++i){
            File file = files[i];
            if (file.isDirectory()) {
                int uid1 = Integer.parseInt(file.getName());
                List friends = new ArrayList();                
                File[] innerFiles = file.listFiles();
                
                for(int j=0; j<innerFiles.length; ++j){
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

    private List getFriends(int uid)
            throws Exception {
        if (!provisioned) {
            throw new Exception("Please run readProvision first.");
        }
        return friendsMap.get(uid);
    }

    private boolean areFirstDegree(int uid, int uid2)
            throws Exception {
        if (!provisioned) {
            throw new Exception("Please run readProvision first.");
        }
        boolean result = false;
        long relationship = (long) uid << 32 | (long) uid2;
        if (firstDegreeSet.contains(relationship)) {
            result = true;
        }
        return result;
    }

    private boolean areSecondDegree(int uid, int uid2)
            throws Exception {
        if (!provisioned) {
            throw new Exception("Please run readProvision first.");
        }
        boolean result = false;
        long relationship = (long) uid << 32 | (long) uid2;
        if (secondDegreeSet.contains(relationship)) {
            result = true;
        }
        return result;
    }

    private boolean areThirdDegree(int uid, int uid2)
            throws Exception {
        if (!provisioned) {
            throw new Exception("Please run readProvision first.");
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

    private Command parseCommand(String command)
            throws Exception {
        Command result = null;

        String[] split = command.split("\\s");

        Path path;
        long nUsers;
        int avg;
        float std;
        int uid, uid2;

        if (split.length > 0) {
            String cmdSlice = split[0];
            switch (cmdSlice) {
                case "provision":
                    if (split.length < 5 || split.length > 5) {
                        throw new Exception("Wrong number of parameters.");
                    }
                    path = Paths.get(split[1]);
                    nUsers = Long.parseLong(split[2]);
                    avg = Integer.parseInt(split[3]);
                    std = Float.parseFloat(split[4]);
                    result = new ProvisionCommand(path, nUsers, avg, std);
                    break;
                case "readProvision":
                    if (split.length < 2 || split.length > 2) {
                        throw new Exception("Wrong number of parameters.");
                    }
                    path = Paths.get(split[1]);
                    result = new ReadProvisionCommand(path);
                    break;
                case "getFriends":
                    if (split.length < 2 || split.length > 2) {
                        throw new Exception("Wrong number of parameters.");
                    }
                    uid = Integer.parseInt(split[1]);
                    result = new GetFriendsCommand(uid);
                    break;
                case "areFirstDegree":
                    if (split.length < 3 || split.length > 3) {
                        throw new Exception("Wrong number of parameters.");
                    }
                    uid = Integer.parseInt(split[1]);
                    uid2 = Integer.parseInt(split[2]);
                    result = new AreFirstDegreeCommand(uid, uid2);
                    break;
                case "areSecondDegree":
                    if (split.length < 3 || split.length > 3) {
                        throw new Exception("Wrong number of parameters.");
                    }
                    uid = Integer.parseInt(split[1]);
                    uid2 = Integer.parseInt(split[2]);
                    result = new AreSecondDegreeCommand(uid, uid2);
                    break;
                case "areThirdDegree":
                    if (split.length < 3 || split.length > 3) {
                        throw new Exception("Wrong number of parameters.");
                    }
                    uid = Integer.parseInt(split[1]);
                    uid2 = Integer.parseInt(split[2]);
                    result = new AreThirdDegreeCommand(uid, uid2);
                    break;
            }
        }

        return result;
    }

    private void respond(String command, BufferedWriter writer)
            throws IOException {
        try {
            Command cmd = parseCommand(command);

            if (cmd != null) {
                switch (cmd.type) {
                    case PROVISION:
                        provision((Path) cmd.get("path"), (long) cmd.get("nUsers"),
                                (int) cmd.get("avg"), (float) cmd.get("std"));
                        writer.write("OK");
                        writer.newLine();
                        break;
                    case READ_PROVISION:
                        readProvision((Path) cmd.get("path"));
                        writer.write("OK");
                        writer.newLine();
                        break;
                    case GET_FRIENDS:
                        List friends = getFriends((int)cmd.get("uid"));
                        int size = friends.size();
                        for(int i=0; i<size; ++i){
                            writer.write(Integer.toString((int)friends.get(i)));
                            if( i<size-1 ){
                                writer.write(", ");
                            }
                        }
                        writer.newLine();
                        writer.write("OK");
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
                        writer.write("Unknown command.");
                        writer.newLine();
                        break;
                }
            } else {
                writer.write("Unknown command.");
                writer.newLine();
            }
        } catch (Exception ex) {
//            Logger.getLogger(Implementation.class.getName()).log(Level.SEVERE, null, ex);
            try {
                writer.write("Exception: "+ex.getMessage());
                writer.newLine();
            } catch (IOException ex2) {
                // Ignore Exceptions while writing error message to client
            }
        }
    }
}
