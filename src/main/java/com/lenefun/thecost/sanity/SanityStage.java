package com.lenefun.thecost.sanity;

public enum SanityStage {
	LUCID("Lucid", 60, SanityData.MAX_VALUE),
	FRACTURED("Fractured", 20, 59),
	ASCENDED("Ascended", SanityData.MIN_VALUE, 19);

	private final String displayName;
	private final int minimum;
	private final int maximum;

	SanityStage(String displayName, int minimum, int maximum) {
		this.displayName = displayName;
		this.minimum = minimum;
		this.maximum = maximum;
	}

	public String getDisplayName() {
		return displayName;
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
