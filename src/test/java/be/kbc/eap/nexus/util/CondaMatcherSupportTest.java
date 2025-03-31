package be.kbc.eap.nexus.util;

import com.google.common.base.Predicate;
import org.junit.Test;
import org.sonatype.goodies.testsupport.TestSupport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

public class CondaMatcherSupportTest extends TestSupport {

    @Test
    public void equalsWithHashes() {
        Predicate<String> pred = CondaMatcherSupport.withHashes("/some-path.txt"::equals);

        assertThat(pred.apply("/some-path.txt"), equalTo(true));
        assertThat(pred.apply("/some-path.txt.sha1"), equalTo(true));
        assertThat(pred.apply("/some-path.txt.md5"), equalTo(true));
        assertThat(pred.apply("/some-path.txt.sha256"), equalTo(true));
        assertThat(pred.apply("/some-path.txt.sha512"), equalTo(true));

        assertThat(pred.apply("some-path.txt"), equalTo(false));
        assertThat(pred.apply("/some-path.txt.crc"), equalTo(false));
        assertThat(pred.apply("/some-path.tx"), equalTo(false));
        assertThat(pred.apply("/other-path.txt"), equalTo(false));
    }

    @Test
    public void endsWithWithHashes() {
        Predicate<String> pred = CondaMatcherSupport.withHashes((String input) -> input.endsWith("/some-path.txt"));

        assertThat(pred.apply("/some/prefix/some-path.txt"), equalTo(true));
        assertThat(pred.apply("/some-path.txt.sha1"), equalTo(true));
        assertThat(pred.apply("some/prefix/some-path.txt.md5"), equalTo(true));
        assertThat(pred.apply("/some-path.txt.sha256"), equalTo(true));
        assertThat(pred.apply("/some-path.txt.sha512"), equalTo(true));

        assertThat(pred.apply("some-path.txt"), equalTo(false));
        assertThat(pred.apply("/some-path.txt.crc"), equalTo(false));
        assertThat(pred.apply("/some-path.tx"), equalTo(false));
        assertThat(pred.apply("/other-path.txt"), equalTo(false));
    }

}
