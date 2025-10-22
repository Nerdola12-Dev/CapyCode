package org.nerdola.capycode.libraries;

import java.util.Scanner;

public class OutputLibrary {
    public static void print(String value) {
        System.out.print(value);
    }

    public static void println(String value) {
        System.out.println(value);
    }

    public static String input(String prompt) {
        System.out.print(prompt);
        Scanner sc = new Scanner(System.in);
        return sc.nextLine();
    }
}
