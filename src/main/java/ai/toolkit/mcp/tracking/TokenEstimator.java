package ai.toolkit.mcp.tracking;

/**
 * Character-based token estimate utility.
 * <p>
 * This is an approximation only: {@code 1 token ≈ 4 characters}.
 * It is not an exact tokenizer count from an LLM provider and must never
 * be presented as precise token usage.
 */
public final class TokenEstimator {

    private TokenEstimator() {
    }

    /**
     * Estimates token count from character length using 1 token ≈ 4 chars.
     * Returns at least 1 for any non-null input (including empty strings).
     *
     * @param text text to estimate; must not be null
     * @return estimated token count (minimum 1)
     * @throws IllegalArgumentException if text is null
     */
    public static long estimate(String text) {
        if (text == null) {
            throw new IllegalArgumentException("text must not be null");
        }
        return Math.max(1, text.length() / 4);
    }
}
