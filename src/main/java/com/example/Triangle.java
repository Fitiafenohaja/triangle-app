package com.example;

public class Triangle {

    public boolean estValide(int a, int b, int c) {
        return a > 0 && b > 0 && c > 0 &&
               a + b > c &&
               a + c > b &&
               b + c > a;
    }

}
