package ai.toolkit.mcp.tool;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class SimpleUnitTest {
    
    @Test
    public void testSimpleAssertion() {
        assertThat(true).isTrue();
        assertThat("hello").contains("ell");
    }
}
