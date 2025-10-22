package org.nerdola.capycode.compiler;

import java.util.*;

public class Lexer {
    private final String src;
    private int pos = 0;
    private int line = 1;
    private int column = 1;

    public Lexer(String src) {
        this.src = src;
    }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();

        while (pos < src.length()) {
            char c = src.charAt(pos);

            if (Character.isWhitespace(c)) {
                advancePosition(c);
                continue;
            }

            switch (c) {
                case '+': tokens.add(new Token(TokenType.PLUS, "+", line, column)); advancePosition(c); break;
                case '-': tokens.add(new Token(TokenType.MINUS, "-", line, column)); advancePosition(c); break;
                case '*': tokens.add(new Token(TokenType.STAR, "*", line, column)); advancePosition(c); break;
                case '/': tokens.add(new Token(TokenType.SLASH, "/", line, column)); advancePosition(c); break;
                case '=': tokens.add(new Token(TokenType.EQUAL, "=", line, column)); advancePosition(c); break;
                case ';': tokens.add(new Token(TokenType.SEMICOLON, ";", line, column)); advancePosition(c); break;
                case '.': tokens.add(new Token(TokenType.DOT, ".", line, column)); advancePosition(c); break;
                case '(': tokens.add(new Token(TokenType.LPAREN, "(", line, column)); advancePosition(c); break;
                case ')': tokens.add(new Token(TokenType.RPAREN, ")", line, column)); advancePosition(c); break;
                case '"':
                    tokens.add(new Token(TokenType.STRING, readString(), line, column));
                    break;
                case '\'':
                    tokens.add(new Token(TokenType.CHAR, readChar(), line, column));
                    break;
                default:
                    if (Character.isDigit(c)) {
                        String number = readNumber();
                        TokenType type = number.contains(".") ? TokenType.FLOAT : TokenType.NUMBER;
                        tokens.add(new Token(type, number, line, column));
                    } else if (Character.isLetter(c) || c == '_') {
                        String word = readWord();
                        int startLine = line;
                        int startColumn = column - word.length();

                        // Tratamento especial para USING(...)
                        if (word.equals("using")) {
                            skipWhitespace();

                            if (pos < src.length() && (Character.isLetter(src.charAt(pos)) || src.charAt(pos) == '_')) {
                                String nextIdent = readWord(); // pega o identificador após 'using'
                                tokens.add(new Token(TokenType.USING, nextIdent, startLine, startColumn));
                            } else {
                                tokens.add(new Token(TokenType.USING, word, startLine, startColumn));
                            }
                            continue;
                        }

                        // Tratamento especial para var (ajustado para separar o tipo)
                        else if (word.equals("var")) {
                            tokens.add(new Token(TokenType.VAR, word, startLine, startColumn));
                            skipWhitespace();

                            if (pos < src.length()) {
                                if (src.charAt(pos) == '(') {
                                    advancePosition('(');
                                    StringBuilder typeBuilder = new StringBuilder();
                                    while (pos < src.length() && src.charAt(pos) != ')') {
                                        char typeChar = src.charAt(pos);
                                        if (!Character.isLetter(typeChar)) {
                                            Logger.fatal("Invalid character in type declaration", line, column);
                                        }
                                        typeBuilder.append(typeChar);
                                        advancePosition(typeChar);
                                    }
                                    if (pos >= src.length() || src.charAt(pos) != ')') {
                                        Logger.fatal("Expected closing ')' in variable declaration", line, column);
                                    }
                                    advancePosition(')');
                                    String type = typeBuilder.toString().toUpperCase();
                                    tokens.add(new Token(TokenType.IDENTIFIER, type, line, column));
                                } else if (Character.isLetter(src.charAt(pos)) || src.charAt(pos) == '_') {
                                    String type = readWord().toUpperCase();
                                    tokens.add(new Token(TokenType.IDENTIFIER, type, line, column));
                                }
                            }
                            continue;
                        } else {
                            switch (word) {
                                case "true", "false" -> tokens.add(new Token(TokenType.BOOLEAN, word, startLine, startColumn));
                                case "print" -> tokens.add(new Token(TokenType.PRINT, word, startLine, startColumn));
                                case "if" -> tokens.add(new Token(TokenType.IF, word, startLine, startColumn));
                                case "else" -> tokens.add(new Token(TokenType.ELSE, word, startLine, startColumn));
                                case "elseif" -> tokens.add(new Token(TokenType.ELSEIF, word, startLine, startColumn));
                                case "for" -> tokens.add(new Token(TokenType.FOR, word, startLine, startColumn));
                                case "while" -> tokens.add(new Token(TokenType.WHILE, word, startLine, startColumn));
                                default -> tokens.add(new Token(TokenType.IDENTIFIER, word, startLine, startColumn));
                            }
                        }
                    } else {
                        Logger.warning("Unknown character: '" + c + "'", line, column);
                        advancePosition(c);
                    }
            }
        }

        tokens.add(new Token(TokenType.EOF, "", line, column));
        return tokens;
    }

    private void advancePosition(char c) {
        pos++;
        if (c == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
    }

    private char peekChar() {
        if (pos < src.length() - 1) {
            return src.charAt(pos + 1);
        }
        return '\0';
    }

    private char peekNextNonWhitespaceChar() {
        int tempPos = pos;
        while (tempPos < src.length()) {
            char c = src.charAt(tempPos);
            if (!Character.isWhitespace(c)) {
                return c;
            }
            tempPos++;
        }
        return '\0';
    }

    private String readNumber() {
        int startPos = pos;
        boolean hasDot = false;
        while (pos < src.length()) {
            char c = src.charAt(pos);
            if (Character.isDigit(c)) {
                advancePosition(c);
            } else if (c == '.' && !hasDot) {
                hasDot = true;
                advancePosition(c);
            } else {
                break;
            }
        }
        return src.substring(startPos, pos);
    }

    private String readWord() {
        int startPos = pos;
        while (pos < src.length() &&
               (Character.isLetterOrDigit(src.charAt(pos)) || src.charAt(pos) == '_')) {
            advancePosition(src.charAt(pos));
        }
        return src.substring(startPos, pos);
    }

    private String readString() {
        int startLine = line;
        int startColumn = column;
        pos++; // pula aspas
        column++;
        StringBuilder sb = new StringBuilder();
        while (pos < src.length()) {
            char c = src.charAt(pos);
            if (c == '"') {
                advancePosition(c);
                return sb.toString();
            }
            if (c == '\\') {
                pos++; column++;
                if (pos >= src.length()) {
                    Logger.fatal("Unterminated escape sequence in string literal", startLine, startColumn);
                }
                char next = src.charAt(pos);
                switch (next) {
                    case 'n' -> sb.append('\n');
                    case 't' -> sb.append('\t');
                    case 'r' -> sb.append('\r');
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    default -> {
                        Logger.warning("Unknown escape sequence \\" + next + " in string literal", line, column);
                        sb.append('\\').append(next);
                    }
                }
                advancePosition(next);
            } else {
                sb.append(c);
                advancePosition(c);
            }
        }
        Logger.fatal("Unterminated string literal", startLine, startColumn);
        return sb.toString();
    }

    private String readChar() {
        int startColumn = column;
        pos++; column++; // pula abertura '

        if (pos >= src.length() || src.charAt(pos) == '\'') {
            Logger.fatal("Empty character literal", line, startColumn);
        }

        char value = src.charAt(pos);
        advancePosition(value);

        if (pos >= src.length() || src.charAt(pos) != '\'') {
            Logger.fatal("Unterminated character literal", line, startColumn);
        }

        advancePosition('\''); // fecha aspas simples
        return String.valueOf(value);
    }

    private void skipWhitespace() {
        while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) {
            advancePosition(src.charAt(pos));
        }
    }

}
