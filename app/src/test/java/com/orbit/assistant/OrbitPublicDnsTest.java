package com.orbit.assistant;

import org.junit.Test;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.*;

public class OrbitPublicDnsTest {
    private static InetAddress ip(String value) throws UnknownHostException { return InetAddress.getByName(value); }

    @Test public void acceptsPublicV4AndV6() throws Exception {
        OrbitPublicDns dns = new OrbitPublicDns(host -> Arrays.asList(ip("8.8.8.8"), ip("2001:4860:4860::8888")));
        List<InetAddress> answers = dns.lookup("example.com");
        assertEquals(2, answers.size());
    }

    @Test public void rejectsPrivateIpv4() throws Exception {
        OrbitPublicDns dns = new OrbitPublicDns(host -> Collections.singletonList(ip("192.168.1.5")));
        try { dns.lookup("example.com"); fail("private address must be rejected"); }
        catch (UnknownHostException expected) {}
    }

    @Test public void rejectsLoopbackIpv6() throws Exception {
        OrbitPublicDns dns = new OrbitPublicDns(host -> Collections.singletonList(ip("::1")));
        try { dns.lookup("example.com"); fail("loopback address must be rejected"); }
        catch (UnknownHostException expected) {}
    }

    @Test public void rejectsMixedPublicAndPrivateDnsAnswers() throws Exception {
        OrbitPublicDns dns = new OrbitPublicDns(host -> Arrays.asList(ip("1.1.1.1"), ip("10.0.0.8")));
        try { dns.lookup("example.com"); fail("mixed answer must be rejected"); }
        catch (UnknownHostException expected) {}
    }

    @Test public void rejectsEmptyAnswerSet() throws Exception {
        OrbitPublicDns dns = new OrbitPublicDns(host -> Collections.emptyList());
        try { dns.lookup("example.com"); fail("empty answers must be rejected"); }
        catch (UnknownHostException expected) {}
    }
}
