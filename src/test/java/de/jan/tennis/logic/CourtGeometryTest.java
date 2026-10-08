package de.jan.tennis.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CourtGeometryTest {
	@Test
	void linesAreIn() {
		assertTrue(CourtGeometry.inSinglesHalf(12.5, 0, 1), "Grundlinie ist im Feld");
		assertTrue(CourtGeometry.inSinglesHalf(5, 4.5, 1), "Seitenlinie ist im Feld");
		assertFalse(CourtGeometry.inSinglesHalf(12.6, 0, 1));
		assertFalse(CourtGeometry.inSinglesHalf(5, 4.6, 1));
		assertFalse(CourtGeometry.inSinglesHalf(-5, 0, 1), "Eigene Hälfte zählt nicht");
	}

	@Test
	void serveGoesDiagonal() {
		// Aufschläger auf Seite -1, Einstandsseite: steht bei v > 0 (rechts), Ziel ist v < 0 auf Seite +1
		assertEquals(1, CourtGeometry.serverHalfSign(-1, true));
		assertEquals(-1, CourtGeometry.targetBoxSign(-1, true));
		assertTrue(CourtGeometry.inServiceBox(5, -2, 1, -1));
		assertTrue(CourtGeometry.inServiceBox(7.5, 0.4, 1, -1), "T-Linie und Mittellinie sind im Feld");
		assertFalse(CourtGeometry.inServiceBox(8, -2, 1, -1), "Hinter der T-Linie ist Fehler");
		assertFalse(CourtGeometry.inServiceBox(5, 2, 1, -1), "Falsches Feld");
		// Auf der anderen Seite ist rechts gespiegelt
		assertEquals(-1, CourtGeometry.serverHalfSign(1, true));
	}

	@Test
	void servePosition() {
		assertTrue(CourtGeometry.validServePosition(-13, 2, -1, true));
		assertFalse(CourtGeometry.validServePosition(-11, 2, -1, true), "Im Feld ist Fußfehler");
		assertFalse(CourtGeometry.validServePosition(-13, -2, -1, true), "Falsche Seite");
		assertTrue(CourtGeometry.validServePosition(-13, -2, -1, false), "Vorteilseite links");
	}
}
