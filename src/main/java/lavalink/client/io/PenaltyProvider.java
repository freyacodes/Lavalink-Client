/*
 * Copyright (c) Freya Arbjerg. Licensed under the MIT license
 */

package lavalink.client.io;

public interface PenaltyProvider {

    /**
     * This method allows for adding custom penalties to {@link LavalinkSocket nodes}, making it possible to
     * change how the node selection system works on a per-guild per-node basis.
     * By using the provided {@link LavalinkLoadBalancer.Penalties Penalties} class you can fetch default penalties like CPU or Players.
     *
     * @param penalties - Instance of {@link LavalinkLoadBalancer.Penalties Penalties} class representing the node to check.
     * @return total penalty to add to this node.
     */
    int getPenalty(LavalinkLoadBalancer.Penalties penalties);

}
