package org.nerdola.capycode.compiler;

import java.io.*;
import java.util.*;

public class BytecodeWriter {
    public static void write(String filename, List<Token> tokens) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filename))) {
            for (Token token : tokens) {
                bw.write(formatToken(token));
                bw.newLine();
            }
        }
        System.out.println("[Compiler] Wrote bytecode file: " + filename);
    }

    private static String formatToken(Token token) {
        String escapedValue = escapeString(token.value);
        return token.type + "('" + escapedValue + "')";
    }

    private static String escapeString(String str) {
        return str
            .replace("\\", "\\\\")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
            .replace("\"", "\\\"");
    }
    
}
