package de.jan.tennis.outfit;

import java.util.Locale;
import org.jspecify.annotations.Nullable;

/**
 * Tennis-Outfits. Eigene Figuren, die vom Stil berühmter Tennis-Legenden inspiriert sind,
 * aber keine echten Personen darstellen.
 */
public enum Outfit {
	ROTFUCHS("rotfuchs", "Der Rotfuchs", "Rotblonder Wuschelkopf, klassisches Weiß – Bumm-Bumm-Aufschlag"),
	EISMANN("eismann", "Der Eismann", "Lange blonde Haare, Stirnband, Nadelstreifen – eiskalt von der Grundlinie"),
	HITZKOPF("hitzkopf", "Der Hitzkopf", "Dunkle Locken, rotes Stirnband – Serve-and-Volley mit Temperament"),
	GRAEFIN("graefin", "Die Gräfin", "Blonder Pferdeschwanz, weißes Tenniskleid – gefürchtete Vorhand"),
	MAESTRO("maestro", "Der Maestro", "Weißes Bandana, Creme und Gold – elegant wie ein Ballett"),
	MATADOR("matador", "Der Matador", "Bandana, ärmelloses Shirt, Dreiviertelhose – Topspin ohne Ende"),
	KOENIGIN("koenigin", "Die Königin", "Lange Zöpfe, kräftiges Lila – Aufschläge wie Kanonenschüsse");

	public static final String NONE = "aus";

	public final String id;
	public final String displayName;
	public final String description;

	Outfit(String id, String displayName, String description) {
		this.id = id;
		this.displayName = displayName;
		this.description = description;
	}

	public static @Nullable Outfit byId(String id) {
		String lower = id.toLowerCase(Locale.ROOT);
		for (Outfit o : values()) {
			if (o.id.equals(lower)) {
				return o;
			}
		}
		return null;
	}
}
