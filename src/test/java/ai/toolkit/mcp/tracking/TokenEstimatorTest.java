package ai.toolkit.mcp.tracking;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenEstimatorTest {

    @Test
    void estimateEmptyReturnsMinimumOne() {
        assertThat(TokenEstimator.estimate("")).isEqualTo(1L);
    }

    @Test
    void estimateHelloReturnsOne() {
        assertThat(TokenEstimator.estimate("hello")).isEqualTo(1L);
    }

    @Test
    void estimateFourHundredCharsReturnsOneHundred() {
        assertThat(TokenEstimator.estimate("a".repeat(400))).isEqualTo(100L);
    }

    @Test
    void estimateNullThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> TokenEstimator.estimate(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be null");
    }
}
