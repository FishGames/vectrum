package io.github.fishgames.vectrum.core.digital;

import io.github.fishgames.vectrum.core.digital.CoderLinks.Coder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoderLinksTest {
    @Test
    void networksOnTheSameFrequencyAreCoupled() {
        List<Coder> coders = List.of(new Coder(1, 0, 10), new Coder(2, 0, 20), new Coder(3, 0, 30));

        assertEquals(List.of(coders.get(1), coders.get(2)), CoderLinks.partners(10, coders));
        assertEquals(List.of(coders.get(0), coders.get(2)), CoderLinks.partners(20, coders));
    }

    @Test
    void differentFrequenciesStayApart() {
        List<Coder> coders = List.of(new Coder(1, 0, 10), new Coder(2, 5, 20), new Coder(3, 0, 30));

        assertEquals(List.of(coders.get(2)), CoderLinks.partners(10, coders));
        assertTrue(CoderLinks.partners(20, coders).isEmpty());
    }

    @Test
    void aNetworkWithSeveralCodersUsesAllTheirFrequencies() {
        List<Coder> coders = List.of(new Coder(1, 1, 10), new Coder(2, 2, 10), new Coder(3, 1, 20), new Coder(4, 2, 30),
                new Coder(5, 3, 40));

        assertEquals(List.of(coders.get(2), coders.get(3)), CoderLinks.partners(10, coders));
    }

    @Test
    void eachPartnerNetworkAppearsOnceWithItsFirstCoder() {
        List<Coder> coders = List.of(new Coder(9, 0, 20), new Coder(4, 0, 20), new Coder(1, 0, 10));

        assertEquals(List.of(new Coder(4, 0, 20)), CoderLinks.partners(10, coders));
    }

    @Test
    void ownNetworkAndCodersWithoutNetworkAreIgnored() {
        List<Coder> coders = List.of(new Coder(1, 0, 10), new Coder(2, 0, 10), new Coder(3, 0, -1));

        assertTrue(CoderLinks.partners(10, coders).isEmpty());
    }

    @Test
    void noCoderInTheNetworkMeansNoPartners() {
        List<Coder> coders = List.of(new Coder(2, 0, 20), new Coder(3, 0, 30));

        assertTrue(CoderLinks.partners(99, coders).isEmpty());
    }
}
