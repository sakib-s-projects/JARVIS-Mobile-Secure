package com.orbit.assistant;

import okhttp3.Dns;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Resolver vets every DNS answer; OkHttp connects to one of these validated IPs, not a second DNS lookup. */
final class OrbitPublicDns implements Dns {
    interface Resolver { List<InetAddress> lookup(String host) throws UnknownHostException; }
    private final Resolver resolver;

    OrbitPublicDns() { this(host -> java.util.Arrays.asList(InetAddress.getAllByName(host))); }
    OrbitPublicDns(Resolver resolver) { this.resolver = resolver; }

    @Override public List<InetAddress> lookup(String hostname) throws UnknownHostException {
        List<InetAddress> answers = resolver.lookup(hostname);
        if (answers == null || answers.isEmpty())
            throw new UnknownHostException("Host returned no DNS addresses.");
        for (InetAddress address : answers) {
            if (!OrbitExtension.isPublicAddress(address))
                throw new UnknownHostException("Host resolves to a non-public address.");
        }
        return Collections.unmodifiableList(new ArrayList<>(answers));
    }
}
