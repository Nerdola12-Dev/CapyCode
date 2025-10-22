package org.nerdola.capycode.compiler;

import java.io.*;
import java.util.*;

public class Executor {

    public static boolean DEBUG = false;

    public static void run(String filename) throws IOException {
        List<String> lines = java.nio.file.Files.readAllLines(new File(filename).toPath());
        Map<String, TypedVariable> variables = new HashMap<>();
        Set<String> importedLibs = new HashSet<>();
        List<String> commandTokens = new ArrayList<>();
        int lineNumber = 0;

        for (String line : lines) {
            lineNumber++;
            line = line.trim();
            if (DEBUG) System.out.println("[DEBUG] Line " + lineNumber + ": " + line);

            if (line.equals("EOF('')")) break;

            if (line.startsWith("USING(")) {
                String libName = extractTokenValue(line);
                importedLibs.add(libName);

                if (DEBUG) System.out.println("[DEBUG] Imported library: " + libName);

                continue;
            }

            commandTokens.add(line);
            if (DEBUG) System.out.println("[DEBUG] Added token: " + line);

            if (line.startsWith("SEMICOLON(")) {
                if (DEBUG) System.out.println("[DEBUG] Executing command at line " + lineNumber + ": " + commandTokens);
                executeCommand(commandTokens, variables, importedLibs, lineNumber);
                commandTokens.clear();
            }
        }
    }

    private static void executeCommand(List<String> tokens,
                                       Map<String, TypedVariable> variables,
                                       Set<String> importedLibs,
                                       int lineNumber) {
        if (tokens.isEmpty()) return;
        String first = tokens.get(0);

        if (tokens.size() >= 6 && tokens.get(0).startsWith("VAR(")) {
            String type = extractTokenValue(tokens.get(1));
            String varName = extractTokenValue(tokens.get(2));

            if (variables.containsKey(varName)) {
                Logger.fatal("Variable '" + varName + "' já declarada", lineNumber, 0);
                return;
            }

            if (!tokens.get(3).startsWith("EQUAL(")) {
                Logger.fatal("Esperado '=' após declaração da variável '" + varName + "'", lineNumber, 0);
                return;
            }

            List<String> exprTokens = tokens.subList(4, tokens.size() - 1);
            Object value = evaluateExpression(exprTokens, variables, importedLibs, lineNumber);

            if (!isTypeCompatible(type, value)) {
                Logger.fatal("Tipo incompatível para variável '" + varName + "'", lineNumber, 0);
                return;
            }

            variables.put(varName, new TypedVariable(type, value.toString()));

            if (DEBUG) {
                System.out.println("[DEBUG] Declarada variável: " + varName + " tipo: " + type + " valor: " + value);
            }

            return;
        }

        try {
            if (DEBUG) System.out.println("[DEBUG] Executing tokens: " + tokens);

            if (first.startsWith("IDENTIFIER(") && tokens.size() >= 4 && tokens.get(1).startsWith("EQUAL(")) {
                String varName = extractTokenValue(tokens.get(0));
                TypedVariable existing = variables.get(varName);
                if (existing == null) {
                    Logger.fatal("Variable '" + varName + "' is not declared", lineNumber, 0);
                }

                if (DEBUG) System.out.println("[DEBUG] Assigning to variable '" + varName + "'");

                // ✅ Intercepta reatribuição do tipo: NOME = Output.input("...")(TIPO);
                if (tokens.size() >= 11 &&
                    tokens.get(2).startsWith("IDENTIFIER('Output')") &&
                    tokens.get(3).startsWith("DOT('.')") &&
                    tokens.get(4).startsWith("IDENTIFIER('input')") &&
                    tokens.get(5).startsWith("LPAREN(") &&
                    tokens.get(6).startsWith("STRING(") &&
                    tokens.get(7).startsWith("RPAREN(") &&
                    tokens.get(8).startsWith("LPAREN(") &&
                    tokens.get(9).startsWith("IDENTIFIER(") &&
                    tokens.get(10).startsWith("RPAREN(")) {

                    if (!importedLibs.contains("Output")) {
                        Logger.fatal("Library 'Output' not imported. Use `using Output;`", lineNumber, 0);
                    }

                    String prompt = extractTokenValue(tokens.get(6));
                    String expectedType = extractTokenValue(tokens.get(9)).toUpperCase();

                    System.out.print(prompt + " ");
                    Scanner scanner = new Scanner(System.in);
                    String inputValue = scanner.nextLine();

                    if (DEBUG) System.out.println("[DEBUG] Input received: " + inputValue);

                    Object value;
                    switch (expectedType) {
                        case "INT":
                            try {
                                value = Integer.parseInt(inputValue);
                            } catch (NumberFormatException e) {
                                Logger.fatal("Invalid integer input", lineNumber, 0);
                                return;
                            }
                            break;
                        case "STRING":
                            value = inputValue;
                            break;
                        default:
                            Logger.fatal("Unsupported input type: " + expectedType, lineNumber, 0);
                            return;
                    }

                    if (!isTypeCompatible(existing.type, value)) {
                        Logger.fatal("Type mismatch: variable '" + varName + "' is of type " + existing.type +
                                ", but tried to assign " + value.getClass().getSimpleName(), lineNumber, 0);
                    }

                    existing.value = value.toString();

                    if (DEBUG) System.out.println("[DEBUG] Updated variable: " + varName + " = " + existing.value);
                    return;
                }

                // Avaliação de expressão padrão
                List<String> exprTokens = tokens.subList(2, tokens.size() - 1);

                if (DEBUG) System.out.println("[DEBUG] Evaluating expression for assignment: " + exprTokens);

                Object result = evaluateExpression(exprTokens, variables, importedLibs, lineNumber);

                if (!isTypeCompatible(existing.type, result)) {
                    Logger.fatal("Type mismatch: variable '" + varName + "' is of type " + existing.type +
                            ", but tried to assign " + result.getClass().getSimpleName(), lineNumber, 0);
                }

                existing.value = result.toString();

                if (DEBUG) System.out.println("[DEBUG] Variable '" + varName + "' updated to: " + existing.value);
                return;
            }

            // Output.print / println
            if (first.startsWith("IDENTIFIER(")) {
                String identifier = extractTokenValue(tokens.get(0));
                if (identifier.equals("Output") && tokens.size() >= 5 && tokens.get(1).startsWith("DOT(")) {
                    if (!importedLibs.contains("Output")) {
                        Logger.fatal("Library 'Output' not imported. Use `using Output;`", lineNumber, 0);
                    }

                    String methodName = extractTokenValue(tokens.get(2));
                    String argToken = tokens.get(4);
                    String arg;

                    if (argToken.startsWith("IDENTIFIER(")) {
                        String varName = extractTokenValue(argToken);
                        TypedVariable var = variables.get(varName);
                        if (var == null) {
                            Logger.fatal("Variable '" + varName + "' not found", lineNumber, 0);
                        }
                        arg = var.value;
                    } else if (argToken.startsWith("STRING(")) {
                        arg = unescapeString(extractTokenValue(argToken));
                    } else {
                        arg = String.valueOf(evaluateExpression(Collections.singletonList(argToken), variables, importedLibs, lineNumber));
                    }

                    arg = interpolateString(arg, variables);

                    if (DEBUG) System.out.println("[DEBUG] Output." + methodName + " with argument: " + arg);

                    if (methodName.equals("print")) System.out.print(arg);
                    else if (methodName.equals("println")) System.out.println(arg);

                    return;
                }
            }

            // PRINT legacy
            if (first.startsWith("PRINT(")) {
                String valueToken = extractTokenValue(tokens.get(1));
                TypedVariable var = variables.get(valueToken);
                String value = var != null ? var.value : valueToken;
                value = interpolateString(value, variables);
                if (DEBUG) System.out.println("[DEBUG] PRINT with value: " + value);
                System.out.println(value);
                return;
            }

        } catch (RuntimeException ex) {
            Logger.fatal("Runtime error: " + ex.getMessage(), lineNumber, 0);
        }
    }

    private static String extractTokenValue(String tokenLine) {
        int start = tokenLine.indexOf("('") + 2;
        int end = tokenLine.indexOf("')", start);
        String value = tokenLine.substring(start, end);

        if (DEBUG) System.out.println("[DEBUG] Extracted token value: " + value + " from token: " + tokenLine);

        if (tokenLine.startsWith("STRING(")) {
            if (value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length() - 1);
            }
        }

        return value;
    }

    private static Object evaluateExpression(List<String> tokens, Map<String, TypedVariable> variables, Set<String> importedLibs, int lineNumber) {
        Stack<Object> values = new Stack<>();
        Stack<String> ops = new Stack<>();

        if (DEBUG) System.out.println("[DEBUG] Evaluating expression tokens: " + tokens);

        for (String token : tokens) {
            if (token.startsWith("NUMBER(")) {
                int v = Integer.parseInt(extractTokenValue(token));
                values.push(v);
                if (DEBUG) System.out.println("[DEBUG] Pushed number: " + v);
            } else if (token.startsWith("STRING(")) {
                String v = unescapeString(extractTokenValue(token));
                values.push(v);
                if (DEBUG) System.out.println("[DEBUG] Pushed string: " + v);
            } else if (token.startsWith("IDENTIFIER(")) {
                String name = extractTokenValue(token);

                if (name.equals("Output")) {
                    if (!importedLibs.contains("Output")) {
                        Logger.fatal("Library 'Output' not imported. Use `using Output;`", lineNumber, 0);
                    }
                    Logger.fatal("Invalid use of 'Output' as variable in expression", lineNumber, 0);
                }

                TypedVariable var = variables.get(name);
                if (var == null) {
                    Logger.fatal("Undefined variable: " + name, lineNumber, 0);
                }
                String val = var.value;
                try {
                    int vi = Integer.parseInt(val);
                    values.push(vi);
                } catch (NumberFormatException e) {
                    values.push(val);
                }
            } else if (isOperator(token)) {
                String op = getOperator(token);
                while (!ops.isEmpty() && precedence(ops.peek()) >= precedence(op)) {
                    applyOp(values, ops.pop());
                }
                ops.push(op);
            }
        }
        while (!ops.isEmpty()) {
            applyOp(values, ops.pop());
        }
        if (values.isEmpty()) {
            Logger.fatal("Empty expression", lineNumber, 0);
        }

        return values.pop();
    }

    private static boolean isOperator(String token) {
        return token.startsWith("PLUS(") || token.startsWith("MINUS(") ||
               token.startsWith("STAR(") || token.startsWith("SLASH(") ||
               token.startsWith("PERCENT(");
    }

    private static String getOperator(String token) {
        if (token.startsWith("PLUS(")) return "PLUS";
        if (token.startsWith("MINUS(")) return "MINUS";
        if (token.startsWith("STAR(")) return "STAR";
        if (token.startsWith("SLASH(")) return "SLASH";
        if (token.startsWith("PERCENT(")) return "PERCENT";
        throw new RuntimeException("Unknown operator token: " + token);
    }

    private static int precedence(String op) {
        return switch (op) {
            case "PLUS", "MINUS" -> 1;
            case "STAR", "SLASH", "PERCENT" -> 2;
            default -> 0;
        };
    }

    private static void applyOp(Stack<Object> values, String op) {
        Object b = values.pop();
        Object a = values.pop();
        if (op.equals("PLUS")) {
            if (a instanceof String || b instanceof String) {
                values.push(String.valueOf(a) + String.valueOf(b));
            } else {
                values.push(((Integer) a) + ((Integer) b));
            }
        } else {
            int ai = (Integer) a;
            int bi = (Integer) b;
            switch (op) {
                case "MINUS" -> values.push(ai - bi);
                case "STAR" -> values.push(ai * bi);
                case "SLASH" -> values.push(ai / bi);
                case "PERCENT" -> values.push(ai % bi);
                default -> throw new RuntimeException("Unknown operator: " + op);
            }
        }
    }

    private static String unescapeString(String s) {
        return s.replace("\\\\", "\\").replace("\\n", "\n")
                .replace("\\r", "\r").replace("\\t", "\t").replace("\\\"", "\"");
    }

    private static String interpolateString(String s, Map<String, TypedVariable> variables) {
        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '{') {
                int end = s.indexOf('}', i);
                if (end == -1) {
                    result.append(c);
                    i++;
                    continue;
                }
                String varName = s.substring(i + 1, end).trim();
                TypedVariable var = variables.get(varName);
                result.append(var != null && var.value != null ? var.value : "{" + varName + "}");
                i = end + 1;
            } else {
                result.append(c);
                i++;
            }
        }
        return result.toString();
    }

    private static boolean isTypeCompatible(String expectedType, Object value) {
        return switch (expectedType.toUpperCase()) {
            case "INT" -> value instanceof Integer;
            case "BYTE" -> value instanceof Byte || (value instanceof Integer i && i >= -128 && i <= 127);
            case "BOOLEAN" -> value instanceof Boolean;
            case "CHAR" -> value instanceof Character || (value instanceof String s && s.length() == 1);
            case "FLOAT" -> value instanceof Float || value instanceof Double;
            case "DOUBLE" -> value instanceof Double || value instanceof Float;
            case "STRING" -> value instanceof String;
            default -> false;
        };
    }


    private static class TypedVariable {
        public final String type;
        public String value;
        public TypedVariable(String type, String value) {
            this.type = type;
            this.value = value;
        }
    }
}
