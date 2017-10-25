/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package connectionsgraph;

import java.nio.file.Path;
import java.security.KeyException;
import java.util.HashMap;

/**
 *
 * @author max
 */
class Command {
    
    enum COMMAND_TYPE {
        QUIT,
        POPULATE,
        PROVISION,
        GET_FRIENDS,
        ARE_FIRST_DEGREE,
        ARE_SECOND_DEGREE,
        ARE_THIRD_DEGREE
    }

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
    
    class QuitCommand extends Command {
        private QuitCommand(COMMAND_TYPE type, HashMap params) {
            super(type, params);
        }

        QuitCommand() {
            super(COMMAND_TYPE.QUIT, null);
        }
    }


    class PopulateCommand extends Command {

        private PopulateCommand(COMMAND_TYPE type, HashMap params) {
            super(type, params);
        }

        PopulateCommand(Path path, long nUsers, int avg, float std) {
            super(COMMAND_TYPE.POPULATE, null);
            parameters = new HashMap(4);
            parameters.put("path", path);
            parameters.put("nUsers", nUsers);
            parameters.put("avg", avg);
            parameters.put("std", std);
        }
    }

    class ProvisionCommand extends Command {

        private ProvisionCommand(COMMAND_TYPE type, HashMap params) {
            super(type, params);
        }

        ProvisionCommand(Path path) {
            super(COMMAND_TYPE.PROVISION, null);
            parameters = new HashMap(1);
            parameters.put("path", path);
        }
    }

    class GetFriendsCommand extends Command {

        private GetFriendsCommand(COMMAND_TYPE type, HashMap params) {
            super(type, params);
        }

        GetFriendsCommand(int uid) {
            super(COMMAND_TYPE.GET_FRIENDS, null);
            parameters = new HashMap(1);
            parameters.put("uid", uid);
        }
    }

    class AreFirstDegreeCommand extends Command {

        private AreFirstDegreeCommand(COMMAND_TYPE type, HashMap params) {
            super(type, params);
        }

        AreFirstDegreeCommand(int uid, int uid2) {
            super(COMMAND_TYPE.ARE_FIRST_DEGREE, null);
            parameters = new HashMap(1);
            parameters.put("uid", uid);
            parameters.put("uid2", uid2);
        }
    }

    class AreSecondDegreeCommand extends Command {

        private AreSecondDegreeCommand(COMMAND_TYPE type, HashMap params) {
            super(type, params);
        }

        AreSecondDegreeCommand(int uid, int uid2) {
            super(COMMAND_TYPE.ARE_SECOND_DEGREE, null);
            parameters = new HashMap(1);
            parameters.put("uid", uid);
            parameters.put("uid2", uid2);
        }
    }

    class AreThirdDegreeCommand extends Command {

        private AreThirdDegreeCommand(COMMAND_TYPE type, HashMap params) {
            super(type, params);
        }

        AreThirdDegreeCommand(int uid, int uid2) {
            super(COMMAND_TYPE.ARE_THIRD_DEGREE, null);
            parameters = new HashMap(1);
            parameters.put("uid", uid);
            parameters.put("uid2", uid2);
        }
    }