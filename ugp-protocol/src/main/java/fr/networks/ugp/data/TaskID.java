package fr.networks.ugp.data;

import java.net.SocketAddress;

public record TaskID(long id, SocketAddress socketAddress) { }
