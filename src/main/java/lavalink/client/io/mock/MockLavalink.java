/*
 * Written by TheMulti0, 2019
 * https://github.com/TheMulti0
 */

package lavalink.client.io.mock;

import edu.umd.cs.findbugs.annotations.NonNull;
import lavalink.client.io.Lavalink;

import java.net.URI;

public class MockLavalink extends Lavalink {
    public MockLavalink() {
        super("", 1);
    }

    @Override
    protected MockLink buildNewLink(String guildId) {
        return new MockLink(this, guildId);
    }

    // Custom implementations

    @Override
    public void addNode(@NonNull String name, @NonNull URI serverUri, @NonNull String password) {
    }

    @Override
    public void removeNode(int key) {
    }
}