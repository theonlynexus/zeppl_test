/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package connectionsgraph;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.stream.Stream;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author max
 */
public class ConnectionsGraphTest {

    private Implementation implementation = new Implementation();
    String workingDir = System.getProperty("user.dir");
    Path dir1 = Paths.get(workingDir, "tmp1");
    Path dir2 = Paths.get(workingDir, "tmp2");

    public ConnectionsGraphTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() throws IOException {
        if (Files.exists(dir1)) {
            recursiveDeleteDir(dir1.toAbsolutePath().toString());
        }
        if (Files.exists(dir2)) {
            recursiveDeleteDir(dir2.toAbsolutePath().toString());
        }

        // Test for equality when populating two directories with same seed
        Files.createDirectory(dir1);
        Files.createDirectory(dir2);
    }

    @After
    public void tearDown() throws IOException {
        recursiveDeleteDir(dir1.toAbsolutePath().toString());
        recursiveDeleteDir(dir2.toAbsolutePath().toString());
    }

    /**
     * Delete a file or a directory and its children.
     *
     * @param file The directory to delete.
     * @throws IOException Exception when problem occurs during deleting the
     * directory.
     */
    private void recursiveDeleteDir(String directoryName) throws IOException {

        Path directory = Paths.get(directoryName);
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path file,
                    BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                    throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
    
    /**
     * Test if first degree connections are computed correctly given a known
     * test case
     */
    @Test
    public void testSecondDegreeConnections() {
        try {
            implementation.setSeed(0);
            implementation.populate(dir1, 100, 1, 0);
            implementation.provision(dir1);
            
            Stream<Path> stream = Files.list(dir1);
            stream.forEach((Path p) -> {
                try {
                    int uid1 = Integer.parseInt(p.toFile().getName());
                    Files.list(p).forEach((Path f) -> {
                        try {
                            int uid2 = Integer.parseInt(f.toFile().getName());
                            assertEquals(implementation.areFirstDegree(uid1, uid2), true);
                            assertEquals(implementation.areFirstDegree(
                                    uid1, Integer.remainderUnsigned(uid2 + 1, 100)), false);
                            assertEquals(implementation.areFirstDegree(
                                    uid1, Integer.remainderUnsigned(uid2 + 10, 100)), false);
                            assertEquals(implementation.areFirstDegree(
                                    uid1, Integer.remainderUnsigned(uid2 + 25, 100)), false);
                        } catch (Exception ex) {
                            fail("testFirstDegreeConnections - Unexpected Exception: " + ex.toString());
                        }
                    });
                } catch (IOException ex) {
                    fail("testFirstDegreeConnections  Unexpected IOException: " + ex.toString());
                }
            });

        } catch (Exception ex) {
            fail("testFirstDegreeConnections - Unexpected Exception: " + ex.toString());
        }
    }

    /**
     * Test if first degree connections are computed correctly given a known
     * test case
     */
    @Test
    public void testConnections() {
        try {
            implementation.setSeed(0);
            implementation.populate(dir1, 100, 1, 0);
            implementation.provision(dir1);

            Stream<Path> stream = Files.list(dir1);
            stream.forEach((Path p) -> {
                try {
                    int uid1 = Integer.parseInt(p.toFile().getName());
                    Files.list(p).forEach((Path f) -> {
                        try {
                            int uid2 = Integer.parseInt(f.toFile().getName());
                            // UID1->UID2 == UID2
                            assertEquals(implementation.areFirstDegree(uid1, uid2), true);
                            // UID1->UID2->UID1 != UID2
                            assertEquals(implementation.areSecondDegree(uid1, uid2), false);
                            // UID1->UID2->UID1 == UID1
                            assertEquals(implementation.areSecondDegree(uid1, uid1), true);                            
                            // UID1->UID2->UID1->UID2 == UID2
                            assertEquals(implementation.areThirdDegree(uid1, uid2), true);
                            // UID1->UID2->UID1->UID2 == UID1
                            assertEquals(implementation.areThirdDegree(uid1, uid1), false);
                            assertEquals(implementation.areFirstDegree(
                                    uid1, Integer.remainderUnsigned(uid2 + 1, 100)), false);
                            assertEquals(implementation.areFirstDegree(
                                    uid1, Integer.remainderUnsigned(uid2 + 10, 100)), false);
                            assertEquals(implementation.areFirstDegree(
                                    uid1, Integer.remainderUnsigned(uid2 + 25, 100)), false);
                        } catch (Exception ex) {
                            fail("testFirstDegreeConnections - Unexpected Exception: " + ex.toString());
                        }
                    });
                } catch (IOException ex) {
                    fail("testFirstDegreeConnections  Unexpected IOException: " + ex.toString());
                }
            });

        } catch (Exception ex) {
            fail("testFirstDegreeConnections - Unexpected Exception: " + ex.toString());
        }
    }

    /**
     * Test population of two directories with different seed.
     */
    @Test
    public void testPopulateDifferentSeed() throws IOException {
        try {
            implementation.setSeed(0);
            implementation.populate(dir1, 100, 5, 0);
            implementation.setSeed(1);
            implementation.populate(dir2, 100, 5, 0);
            Iterator iter = Files.list(dir1).iterator();

            while (true) {
                Path p1 = (Path) iter.next();
                Path p2 = Paths.get(dir2.toAbsolutePath().toString(), p1.toFile().getName());
                if (!Files.exists(p2)) {
                    fail("User directories do not match");
                }
                if (Files.isDirectory(p1)) {
                    boolean matchingFriends = true;
                    Iterator innerIter = Files.list(p1).iterator();
                    try {
                        while (true) {
                            Path innerP1 = (Path) innerIter.next();
                            Path innerP2 = Paths.get(p2.toAbsolutePath().toString(), innerP1.toFile().getName());
                            if (!Files.exists(innerP2)) {
                                matchingFriends = false;
                                break;
                            }
                        }
                    } catch (NoSuchElementException innerEx) {

                    }
                    if (matchingFriends) {
                        fail("Friends files match, expected different friend lists.");
                    }
                }
            }
        } catch (NoSuchElementException ex) {

        } catch (Exception ex) {
            fail("testPopulateDifferentSeed - Unexpected Exception: " + ex.toString());
        }
    }

    /**
     * Test population of two directories with different seed.
     */
    @Test
    public void testPopulateSameSeed() throws IOException {

        try {
            implementation.setSeed(0);
            implementation.populate(dir1, 100, 5, 0);
            implementation.setSeed(0);
            implementation.populate(dir2, 100, 5, 0);
            Iterator iter = Files.list(dir1).iterator();
            while (true) {
                Path p1 = (Path) iter.next();
                Path p2 = Paths.get(dir2.toAbsolutePath().toString(), p1.toFile().getName());
                if (!Files.exists(p2)) {
                    fail("User directories do not match");
                }
                if (Files.isDirectory(p1)) {
                    Iterator innerIter = Files.list(p1).iterator();
                    try {
                        while (true) {
                            Path innerP1 = (Path) innerIter.next();
                            Path innerP2 = Paths.get(p2.toAbsolutePath().toString(), innerP1.toFile().getName());
                            if (!Files.exists(innerP2)) {
                                fail("Friend files do not match, expected same friends list");
                            }
                        }
                    } catch (NoSuchElementException innerEx) {

                    }

                }
            }

        } catch (NoSuchElementException ex) {

        } catch (Exception ex) {
            fail("testPopulateSameSeed - Unexpected Exception: " + ex.toString());
        }

    }

}
