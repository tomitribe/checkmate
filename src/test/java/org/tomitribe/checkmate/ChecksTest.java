/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tomitribe.checkmate;

import org.junit.Test;
import org.tomitribe.util.Archive;
import org.tomitribe.util.PrintString;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.tomitribe.checkmate.Checks.WhenFalse.WARN;

public class ChecksTest {

    @Test
    public void checkFormatting() {
        final PrintString out = new PrintString();
        final Checks.ChecksImpl checks = new Checks.ChecksImpl(out, 50);

        checks.check("File exists").pass();
        checks.check("File is directory").fail();
        checks.check("File is directory").fail("with message");
        checks.check("File is executable").error("NullPointerException: null");
        checks.check("File is empty").warn("FileNotFoundException: no such file");
        checks.check("File is symlink").skip();
        checks.check("File is fun").pass();

        assertEquals("" +
                "File exists. . . . . . . . . . . . . . . . . . . PASS\n" +
                "File is directory. . . . . . . . . . . . . . . . FAIL\n" +
                "File is directory. . . . . . . . . . . . . . . . FAIL  with message\n" +
                "File is executable . . . . . . . . . . . . . . . ERROR  NullPointerException: null\n" +
                "File is empty. . . . . . . . . . . . . . . . . . WARN  FileNotFoundException: no such file\n" +
                "File is symlink. . . . . . . . . . . . . . . . . SKIP\n" +
                "File is fun. . . . . . . . . . . . . . . . . . . PASS\n", out.toString());
    }

    /**
     *
     * Here we use checks.object() so we can execute a few checks on the same
     * object.
     *
     * Once there is a failure in a series, we skip the remaining checks.
     */
    @Test
    public void checkObject() {

        final File dir = Archive.archive()
                .add("java-1.8/bin/java", "foo")
                .asDir();

        final PrintString out = new PrintString();
        final Checks.ChecksImpl checks = new Checks.ChecksImpl(out, 50);

        final AtomicInteger called = new AtomicInteger();
        checks.object("JAVA_HOME", dir)
                .check("exists", File::exists)
                .check("is directory", file -> false)
                .check("can write", file -> called.incrementAndGet() > 0)
                .check("can can read", file -> called.incrementAndGet() > 0)
        ;

        assertEquals("" +
                "JAVA_HOME exists . . . . . . . . . . . . . . . . PASS\n" +
                "JAVA_HOME is directory . . . . . . . . . . . . . FAIL\n" +
                "JAVA_HOME can write. . . . . . . . . . . . . . . SKIP\n" +
                "JAVA_HOME can can read . . . . . . . . . . . . . SKIP\n", out.toString());

        assertEquals(0, called.get());
    }

    /**
     *
     * Here we use checks.object() so we can execute a few checks on the same
     * object.
     *
     * Once there is a failure in a series, we skip the remaining checks.
     */
    @Test
    public void checkObjectCheckObject() {

        final File dir = Archive.archive()
                .add("java-1.8/bin/java", "foo")
                .asDir();

        final PrintString out = new PrintString();
        final Checks.ChecksImpl checks = new Checks.ChecksImpl(out, 50);

        final AtomicInteger called = new AtomicInteger();
        checks.object("JAVA_HOME", dir)
                .check("exists", File::exists)
                .check("is directory", File::isDirectory)
                .map("bin directory", file -> new File("bin"))
                .check("exists", File::exists)
                .check("is file", File::isFile)
                .check("can read", file -> called.incrementAndGet() > 0)
        ;

        assertEquals("" +
                "JAVA_HOME exists . . . . . . . . . . . . . . . . PASS\n" +
                "JAVA_HOME is directory . . . . . . . . . . . . . PASS\n" +
                "JAVA_HOME bin directory exists . . . . . . . . . FAIL\n" +
                "JAVA_HOME bin directory is file. . . . . . . . . SKIP\n" +
                "JAVA_HOME bin directory can read . . . . . . . . SKIP\n", out.toString());

        assertEquals(0, called.get());
    }

    /**
     * The first check fails so the remaining checks in the series are skipped
     *
     * We don't need to wory about that potential NullPointerException when we
     * call new File(map.get("JAVA_HOME"))
     */
    @Test
    public void checkObjectSkip() {

        final PrintString out = new PrintString();
        final Checks.ChecksImpl checks = new Checks.ChecksImpl(out, 50);

        final Map<String, String> map = new HashMap<>();

        checks.check("JAVA_HOME is specified", () -> map.containsKey("JAVA_HOME"))
                .object("JAVA_HOME", () -> new File(map.get("JAVA_HOME")))
                .check("is directory", File::isDirectory)
                .check("contains bin directory", file -> new File(file, "bin").exists())
                .check("contains java executable", file -> new File(file, "bin/java").exists());
    }

    /**
     * Our map() execution is ingored because we've already failed
     */
    @Test
    public void checkObjectMapIgnored() {

        final File dir = Archive.archive()
                .add("java-1.8/bin/java", "foo")
                .asDir();

        final PrintString out = new PrintString();
        final Checks.ChecksImpl checks = new Checks.ChecksImpl(out, 50);

        final AtomicInteger called = new AtomicInteger();
        checks.object("JAVA_HOME", dir)
                .check("exists", File::exists)
                .check("is directory", file -> false)
                .map("bin directory", file -> new File("bin"))
                .check("exists", File::exists)
                .check("is file", File::isFile)
                .check("can read", file -> called.incrementAndGet() > 0)
        ;

        assertEquals("" +
                "JAVA_HOME exists . . . . . . . . . . . . . . . . PASS\n" +
                "JAVA_HOME is directory . . . . . . . . . . . . . FAIL\n", out.toString());

        assertEquals(0, called.get());
    }

    /**
     * Check throws exception
     */
    @Test
    public void checkError() {

        final PrintString out = new PrintString();
        final Checks.ChecksImpl checks = new Checks.ChecksImpl(out, 50);

        checks.check("JAVA_HOME is full", () -> {
            throw new RuntimeException("Whoops");
        });

        assertEquals("JAVA_HOME is full. . . . . . . . . . . . . . . . ERROR  RuntimeException: Whoops\n", out.toString());
    }

    /**
     * A check can issue a warning instead of a failure.  The result still returns false
     * and subsequent checks are still not called.  If you need to call a warning and
     * continue doing checks, they need to be separated as follows:
     *
     * keystoreChecks = checks.object("keystoreFile", Paths.get(""));
     *
     * keystoreChecks.check("is defined", path -> false, WARN);
     * keystoreChecks.check("is readable", function)
     *                 .check("is writable", function);
     */
    @Test
    public void checkWarn() {
        final PrintString out = new PrintString();
        final Checks.ChecksImpl checks = new Checks.ChecksImpl(out, 50);

        final AtomicInteger calls = new AtomicInteger();
        final Function<Path, Boolean> function = path -> calls.incrementAndGet() > -1;

        final boolean result = checks.object("keystoreFile", Paths.get(""))
                .check("is defined", path -> false, WARN)
                .check("is readable", function)
                .check("is writable", function)
                .map("keystoreFile length", path -> path.toFile().length())
                .check("is too high", length -> length > 1000000)
                .result();

        assertFalse(result);
        assertEquals("" +
                "keystoreFile is defined. . . . . . . . . . . . . WARN\n" +
                "keystoreFile is readable . . . . . . . . . . . . SKIP\n" +
                "keystoreFile is writable . . . . . . . . . . . . SKIP\n", out.toString());
    }

    @Test
    public void getOrThrowSuccess() {
        final PrintString out = new PrintString();
        final Checks.ChecksImpl checks = new Checks.ChecksImpl(out, 50);

        final String expected = "magic string";
        final String actual = checks.object("keystoreFile", expected)
                .check("is defined", s -> true)
                .getOrThrow(() -> new IllegalStateException("the magic is gone"));


        assertEquals(expected, actual);
        assertSame(expected, actual);
        assertEquals("keystoreFile is defined. . . . . . . . . . . . . PASS\n", out.toString());
    }

    @Test
    public void getOrThrowFail() {
        final PrintString out = new PrintString();
        final Checks.ChecksImpl checks = new Checks.ChecksImpl(out, 50);

        try {
            final String actual = checks.object("keystoreFile", "magic string")
                    .check("is defined", s -> false)
                    .getOrThrow(() -> new IllegalStateException("the magic is gone"));

            fail("Expected exception");
        } catch (IllegalStateException e) {
            assertEquals("the magic is gone", e.getMessage());
            assertEquals("keystoreFile is defined. . . . . . . . . . . . . FAIL\n", out.toString());
        }
    }


    @Test
    public void getOrThrowWarn() {
        final PrintString out = new PrintString();
        final Checks.ChecksImpl checks = new Checks.ChecksImpl(out, 50);

        try {
            final String actual = checks.object("keystoreFile", "magic string")
                    .check("is defined", s -> false, WARN)
                    .getOrThrow(() -> new IllegalStateException("the magic is gone"));

            fail("Expected exception");
        } catch (IllegalStateException e) {
            assertEquals("the magic is gone", e.getMessage());
            assertEquals("keystoreFile is defined. . . . . . . . . . . . . WARN\n", out.toString());
        }
    }

    @Test
    public void orThrowSuccess() {
        final PrintString out = new PrintString();
        final Checks.ChecksImpl checks = new Checks.ChecksImpl(out, 50);

        final String expected = "magic string";
        checks.check("keystoreFile is defined", () -> true)
                .orThrow(() -> new IllegalStateException("the magic is gone"));

        assertEquals("keystoreFile is defined. . . . . . . . . . . . . PASS\n", out.toString());
    }

    @Test
    public void orThrowFail() {
        final PrintString out = new PrintString();
        final Checks.ChecksImpl checks = new Checks.ChecksImpl(out, 50);

        try {
            checks.check("keystoreFile is defined", () -> false)
                    .orThrow(() -> new IllegalStateException("the magic is gone"));

            fail("Expected exception");
        } catch (IllegalStateException e) {
            assertEquals("the magic is gone", e.getMessage());
            assertEquals("keystoreFile is defined. . . . . . . . . . . . . FAIL\n", out.toString());
        }
    }


    @Test
    public void orThrowWarn() {
        final PrintString out = new PrintString();
        final Checks.ChecksImpl checks = new Checks.ChecksImpl(out, 50);

        try {
            checks.check("keystoreFile is defined", () -> false, WARN)
                    .orThrow(() -> new IllegalStateException("the magic is gone"));

            fail("Expected exception");
        } catch (IllegalStateException e) {
            assertEquals("the magic is gone", e.getMessage());
            assertEquals("keystoreFile is defined. . . . . . . . . . . . . WARN\n", out.toString());
        }
    }

    @Test
    public void orAfterSuccess() {
        final PrintString out = new PrintString();
        final Checks.ChecksImpl checks = new Checks.ChecksImpl(out, 50);

        final AtomicReference<String> actual = new AtomicReference<>();

        checks.object("favorite color", "orange")
                .check("is defined", s -> true)
                .or("default color", "green")
                .check("evaluated", s1 -> actual.compareAndSet(null, s1));

        assertEquals("orange", actual.get());
        assertEquals("" +
                "favorite color is defined. . . . . . . . . . . . PASS\n" +
                "favorite color evaluated . . . . . . . . . . . . PASS\n", out.toString());
    }

    @Test
    public void orAfterFail() {
        final PrintString out = new PrintString();
        final Checks.ChecksImpl checks = new Checks.ChecksImpl(out, 50);

        final AtomicReference<String> actual = new AtomicReference<>();

        checks.object("favorite color", "orange")
                .check("is defined", s -> false)
                .or("default color", "green")
                .check("evaluated", s1 -> actual.compareAndSet(null, s1));

        assertEquals("green", actual.get());
        assertEquals("" +
                "favorite color is defined. . . . . . . . . . . . FAIL\n" +
                "default color evaluated. . . . . . . . . . . . . PASS\n", out.toString());
    }

    @Test
    public void orAfterWarn() {
        final PrintString out = new PrintString();
        final Checks.ChecksImpl checks = new Checks.ChecksImpl(out, 50);

        final AtomicReference<String> actual = new AtomicReference<>();

        checks.object("favorite color", "orange")
                .check("is defined", s -> false, WARN)
                .or("default color", "green")
                .check("evaluated", s1 -> actual.compareAndSet(null, s1));

        assertEquals("green", actual.get());
        assertEquals("" +
                "favorite color is defined. . . . . . . . . . . . WARN\n" +
                "default color evaluated. . . . . . . . . . . . . PASS\n", out.toString());
    }
}
