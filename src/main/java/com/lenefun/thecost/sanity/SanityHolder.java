package com.lenefun.thecost.sanity;

public interface SanityHolder {
	int thecost$getSanity();

	void thecost$setSanity(int sanity);

	void thecost$setSanitySilently(int sanity);

	default SanityStage thecost$getSanityStage() {
		return SanityStage.fromValue(thecost$getSanity());
	}
}
