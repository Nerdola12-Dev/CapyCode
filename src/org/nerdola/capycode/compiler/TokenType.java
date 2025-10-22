package org.nerdola.capycode.compiler;

public enum TokenType {
    // Palavras-chave
    VAR, PRINT,
    IF, ELSE, ELSEIF, FOR, WHILE,

    // Operadores e símbolos
    PLUS, MINUS, STAR, SLASH, PERCENT,
    EQUAL, SEMICOLON, DOT,
    LPAREN, RPAREN,
    
    // Importes
    USING,
    
    // Tipos de dados
    IDENTIFIER,
    NUMBER,     // para inteiros
    FLOAT,      // números com ponto
    BOOLEAN,    // true / false
    CHAR,       // ex: 'a'
    STRING,     // ex: "abc"

    // Fim do arquivo
    EOF
}
