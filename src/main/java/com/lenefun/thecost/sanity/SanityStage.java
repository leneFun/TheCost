package com.lenefun.thecost.sanity;

public enum SanityStage {
	LUCID("Lucid", "message.thecost.sanity.stage.lucid", 60, SanityData.MAX_VALUE),
	FRACTURED("Fractured", "message.thecost.sanity.stage.fractured", 20, 59),
	ASCENDED("Ascended", "message.thecost.sanity.stage.ascended", SanityData.MIN_VALUE, 19);

	private final String displayName;
	private final String transitionTranslationKey;
	private final int minimum;
	private final int maximum;

	SanityStage(String displayName, String transitionTranslationKey, int minimum, int maximum) {
		this.displayName = displayName;
		this.transitionTranslationKey = transitionTranslationKey;
		this.minimum = minimum;
		this.maximum = maximum;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getTransitionTranslationKey() {
		return transitionTranslationKey;
	}

	public int getMinimum() {
		return minimum;
	}

	public int getMaximum() {
		return maximum;
	}

	public boolean contains(int sanity) {
		return sanity >= minimum && sanity <= maximum;
	}

	public static SanityStage fromValue(int sanity) {
		int clampedSanity = SanityData.clamp(sanity);

		for (SanityStage stage : values()) {
			if (stage.contains(clampedSanity)) {
				return stage;
			}
		}

		throw new IllegalStateException("No sanity stage for value " + clampedSanity);
	}
}
