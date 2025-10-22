package org.nerdola.capycode.compiler;

import java.util.*;

public class SemanticChecker {

    public static void check(List<Token> tokens) {
        Set<String> declaredVars = new HashSet<>();
        int i = 0;

        while (i < tokens.size()) {
            Token token = tokens.get(i);

            if (token.type == TokenType.VAR) {
                if (i + 3 >= tokens.size()) {
                    Logger.fatal("Incomplete variable declaration: expected format 'var name = value'", token.line, token.column);
                }

                Token name = tokens.get(i + 1);
                Token equal = tokens.get(i + 2);

                if (name.type != TokenType.IDENTIFIER || equal.type != TokenType.EQUAL) {
                    Logger.fatal("Invalid syntax in variable declaration", name.line, name.column);
                }

                if (declaredVars.contains(name.value)) {
                    Logger.fatal("Variable '" + name.value + "' is already declared", name.line, name.column);
                }

                declaredVars.add(name.value);
                i += 4; // Avança mais tokens assumindo declaração completa (var nome = valor)
                continue;
            } else if (token.type == TokenType.IDENTIFIER) {
                if (i + 1 < tokens.size() && tokens.get(i + 1).type == TokenType.EQUAL) {
                    String varName = token.value;
                    if (!declaredVars.contains(varName)) {
                        Logger.fatal("Variable '" + varName + "' is not declared", token.line, token.column);
                    }
                }
            }
            i++;
        }
        Logger.info("Semantic analysis completed successfully", 0, 0);
    }
}
