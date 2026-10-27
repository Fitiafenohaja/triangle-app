package com.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TriangleTest {

    @Test
    void triangleValide() {

        Triangle t = new Triangle();

        assertTrue(
            t.estValide(3,4,5)
        );
    }

    @Test
    void triangleInvalide() {

        Triangle t = new Triangle();

        assertFalse(
            t.estValide(1,2,5)
        );
    }

    @Test
    void coteNegatifInvalide() {

        Triangle t = new Triangle();

        assertFalse(
            t.estValide(-1,5,5)
        );
    }
}
