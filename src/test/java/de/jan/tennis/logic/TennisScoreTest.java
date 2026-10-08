package de.jan.tennis.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TennisScoreTest {
	private static void winGame(TennisScore s, int p) {
		for (int i = 0; i < 4; i++) {
			s.pointWonBy(p);
		}
	}

	@Test
	void countsPointsLikeTennis() {
		TennisScore s = new TennisScore(1, 0);
		assertEquals("0:0", s.gameText("A", "B"));
		s.pointWonBy(0);
		assertEquals("15:0", s.gameText("A", "B"));
		s.pointWonBy(0);
		s.pointWonBy(1);
		assertEquals("30:15", s.gameText("A", "B"));
		s.pointWonBy(0);
		assertEquals("40:15", s.gameText("A", "B"));
		assertEquals(TennisScore.Result.GAME, s.pointWonBy(0));
		assertEquals(1, s.games(0));
		assertEquals(1, s.server(), "Aufschlag wechselt nach jedem Spiel");
	}

	@Test
	void deuceAndAdvantage() {
		TennisScore s = new TennisScore(1, 0);
		for (int i = 0; i < 3; i++) {
			s.pointWonBy(0);
			s.pointWonBy(1);
		}
		assertEquals("Einstand", s.gameText("A", "B"));
		s.pointWonBy(1);
		assertEquals("Vorteil B", s.gameText("A", "B"));
		s.pointWonBy(0);
		assertEquals("Einstand", s.gameText("A", "B"));
		s.pointWonBy(0);
		assertEquals(TennisScore.Result.GAME, s.pointWonBy(0));
		assertEquals(1, s.games(0));
	}

	@Test
	void serveSideAlternates() {
		TennisScore s = new TennisScore(1, 0);
		assertTrue(s.deuceCourt());
		s.pointWonBy(0);
		assertFalse(s.deuceCourt());
		s.pointWonBy(1);
		assertTrue(s.deuceCourt());
	}

	@Test
	void setNeedsTwoGamesLead() {
		TennisScore s = new TennisScore(1, 0);
		for (int i = 0; i < 5; i++) {
			winGame(s, 0);
			winGame(s, 1);
		}
		winGame(s, 0);
		assertEquals(6, s.games(0));
		assertEquals(-1, s.winner());
		winGame(s, 0);
		assertEquals(0, s.winner());
		assertEquals("7:5", s.setsText());
	}

	@Test
	void tiebreakAtSixAll() {
		TennisScore s = new TennisScore(2, 0);
		for (int i = 0; i < 6; i++) {
			winGame(s, 0);
			winGame(s, 1);
		}
		assertTrue(s.isTiebreak());
		int first = s.server();
		assertEquals(0, first, "Wer im Spiel nach 6:6 dran ist, schlägt zuerst auf");
		s.pointWonBy(0);
		assertEquals(1, s.server());
		s.pointWonBy(0);
		assertEquals(1, s.server());
		s.pointWonBy(0);
		assertEquals(0, s.server());
		for (int i = 0; i < 3; i++) {
			s.pointWonBy(0);
		}
		assertEquals("Satzball", s.pressureLabel());
		assertEquals(TennisScore.Result.SET, s.pointWonBy(0));
		assertEquals(1, s.sets(0));
		assertEquals(1, s.server(), "Im nächsten Satz beginnt der andere Spieler");
		assertEquals("7:6 0:0", s.setsText());
	}

	@Test
	void changeEndsAfterOddGames() {
		TennisScore s = new TennisScore(1, 0);
		winGame(s, 0);
		assertTrue(s.changeEnds());
		winGame(s, 1);
		assertFalse(s.changeEnds());
		winGame(s, 1);
		assertTrue(s.changeEnds());
	}

	@Test
	void breakAndMatchPoint() {
		TennisScore s = new TennisScore(1, 0);
		s.pointWonBy(1);
		s.pointWonBy(1);
		s.pointWonBy(1);
		assertEquals("Breakball", s.pressureLabel());
		TennisScore m = new TennisScore(1, 0);
		for (int i = 0; i < 5; i++) {
			winGame(m, 0);
		}
		for (int i = 0; i < 3; i++) {
			m.pointWonBy(0);
		}
		assertEquals("Matchball", m.pressureLabel());
	}
}
