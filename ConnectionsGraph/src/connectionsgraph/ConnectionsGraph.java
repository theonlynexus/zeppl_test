/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package connectionsgraph;

import java.io.IOException;

/**
 *
 * @author Max
 */
public class ConnectionsGraph {

    /**
     * @param args the command line arguments
     * @throws java.lang.Exception
     */
    public static void main(String[] args) throws Exception {
        Implementation implementation = new Implementation();
        try {
            implementation.run();
        } catch (IOException ex) {
            throw new Exception("Exception handler not yet implemented");
        }
    }
}
