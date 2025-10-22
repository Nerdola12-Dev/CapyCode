package org.nerdola.capycode;

import org.nerdola.capycode.compiler.*;
import org.nerdola.capycode.util.*;

import java.util.*;

public class CapyCode {
    public static void main(String[] args) throws Exception {
        //System.out.println("=== CapyCode Compiler 1.0 ===");

        if (args.length < 2) {
            System.out.println("Usage:");
            System.out.println("  CapyCode -c <file.cy>   // compile");
            System.out.println("  CapyCode -r <file.cyc>  // run");
            return;
        }

        String command = args[0];
        String filename = args[1];

        if (command.equals("-c")) {
            String source = FileUtils.readFile(filename);
            Lexer lexer = new Lexer(source);
            List<Token> tokens = lexer.tokenize();
            BytecodeWriter.write(filename.replace(".cy", ".cyc"), tokens);
            System.out.println("[Compiler] Compilation complete.");
        } else if (command.equals("-r")) {
            Executor.run(filename);
        } else {
            System.out.println("Unknown command: " + command);
        }
    }
}
