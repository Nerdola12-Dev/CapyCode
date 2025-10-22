package org.nerdola.capycode.util;

import java.nio.file.*;
import java.io.*;

public class FileUtils {
    public static String readFile(String path) throws IOException {
        return Files.readString(Path.of(path));
    }

    public static void writeFile(String path, String content) throws IOException {
        Files.writeString(Path.of(path), content);
    }
}
