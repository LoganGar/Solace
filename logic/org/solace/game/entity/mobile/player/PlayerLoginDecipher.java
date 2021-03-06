/*
 * This file is part of Solace Framework.
 * Solace is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * Solace is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Solace. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.solace.game.entity.mobile.player;

import java.io.IOException;
import java.nio.ByteBuffer;
import net.burtlebutle.bob.rand.isaac.ISAAC;
import org.solace.event.impl.PlayerLoadService;
import org.solace.network.NIODecoder;
import org.solace.network.RSChannelContext;
import org.solace.network.packet.PacketBuilder;
import org.solace.network.packet.RSPacketDecoder;
import org.solace.util.Constants;
import org.solace.util.ProtocolUtils;
import org.solace.game.Game;

/**
 * RuneScape login procedure decoder.
 * 
 * @author Faris
 * @author Klept0
 */
public class PlayerLoginDecipher implements NIODecoder {

	private PlayerLoginDecipher.State state = PlayerLoginDecipher.State.READ_USERNAME_HASH;
	private ByteBuffer buffer = ByteBuffer.allocateDirect(126);

	@Override
	public void decode(RSChannelContext channelContext) throws IOException {
		channelContext.channel().read(buffer);
		buffer.flip();
		switch (state) {

		case READ_USERNAME_HASH:
			/*
			 * Check if buffer has enough readable data.
			 */
			if (buffer.remaining() < 2) {
				buffer.compact();
				break;
			}

			/*
			 * Login packet opcode.
			 */
			int loginType = buffer.get() & 0xFF;
			if (loginType != 14) {
				System.out.println("Invalid login type.");
				channelContext.channel().close();
				break;
			}

			/*
			 * Name hash which is probably used to select proper login server.
			 */
			@SuppressWarnings("unused")
			int nameHash = buffer.get() & 0xFF;

			/*
			 * Write the response after first state.
			 */
			PacketBuilder out = PacketBuilder.allocate(17);
			out.putBytes(0, 17);
			out.sendTo(channelContext.channel());

			/*
			 * Switch to the next login procedure state.
			 */
			state = PlayerLoginDecipher.State.READ_LOGIN_HEADER;
			buffer.compact();
			break;

		case READ_LOGIN_HEADER:
			/*
			 * Check if buffer has enough readable data.
			 */
			if (buffer.remaining() < 2) {
				buffer.compact();
				break;
			}

			/*
			 * Login request. 16 means that it's normal connection and 18 means
			 * that it's reconnection.
			 */
			int loginRequest = buffer.get() & 0xFF;
			if (loginRequest != 16 && loginRequest != 18) {
				System.out.println("Invalid login request.");
				channelContext.channel().close();
				break;
			}

			/*
			 * Login payload size.
			 */
			loginPacketLength = buffer.get() & 0xFF;

			/*
			 * Switching to the last login state.
			 */
			state = PlayerLoginDecipher.State.READ_LOGIN_PAYLOAD;

		case READ_LOGIN_PAYLOAD:
			/*
			 * Check if buffer has enough readable data.
			 */
			if (buffer.remaining() < loginPacketLength) {
				buffer.flip();
				buffer.compact();
				break;
			}

			/*
			 * Opcode of the last login state.
			 */
			@SuppressWarnings("unused")
			int loginOpcode = buffer.get() & 0xFF;
			/*
			 * Version of the client, in this case 317.
			 */
			int clientVersion = buffer.getShort();
			if (clientVersion != 317) {
				//System.out.println("Invalid Client revision.");
				//channelContext.channel().close();
				//break;
			}

			/*
			 * Client memory version, indicates if client is on low or high
			 * detail mode.
			 */
			@SuppressWarnings("unused")
			int memoryVersion = buffer.get() & 0xFF;

			/*
			 * Skipping the RSA packet.
			 */
			for (int i = 0; i < 9; i++) {
				buffer.getInt();
			}

			/*
			 * The actual payload size, just another indicator to check if it's
			 * correct login packet.
			 */
			int expectedPayloadSize = buffer.get() & 0xFF;
			if (expectedPayloadSize != loginPacketLength - 41) {
				System.out.println("Invalid payload size.");
				channelContext.channel().close();
				break;
			}

			/*
			 * The RSA packet opcode.
			 */
			int rsaOpcode = buffer.get() & 0xFF;
			if (rsaOpcode != 10) {
				System.out.println("Invalid RSA operation code.");
				channelContext.channel().close();
				break;
			}

			/*
			 * Skipping the ISAAC seeds as we are not using it.
			 */
			long clientSeed = buffer.getLong();
			long serverSeed = buffer.getLong();

			int sessionSeed[] = new int[4];
			sessionSeed[0] = (int) (clientSeed >> 32);
			sessionSeed[1] = (int) clientSeed;
			sessionSeed[2] = (int) (serverSeed >> 32);
			sessionSeed[3] = (int) serverSeed;
			channelContext.decryption(new ISAAC(sessionSeed));

			for (int i = 0; i < 4; i++) {
				sessionSeed[i] += 50;
			}
			channelContext.encryption(new ISAAC(sessionSeed));

			/*
			 * The user id.
			 */
			@SuppressWarnings("unused")
			int userId = buffer.getInt();

			/*
			 * The user identify.
			 */
			String username = ProtocolUtils.formatString(ProtocolUtils
					.getRSString(buffer));
			String password = ProtocolUtils.getRSString(buffer);

			if (username.isEmpty() || password.isEmpty()) {
				channelContext.channel().close();
				return;
			}
			/*
			 * Create the player object for this channel.
			 */
			Player player = new Player(username, password, channelContext);
			boolean loaded = new PlayerLoadService(player).load();

			/*
			 * Generate response opcode.
			 */
			int response = 2;

			if (!loaded) {
				System.out.println("IGNORE FOR NOW, NO LOADING AVAILABLE");
				/*
				 * Invalid username or password.
				 */
				response = 3;
			} else {
				channelContext.player(player);
				Game.getSingleton().register(player);
			}

			if (Game.getPlayerRepository().size() >= Constants.SERVER_MAX_PLAYERS) {
				response = 10;
			}

			/*
			 * Write the login procedure response.
			 */
			out = PacketBuilder.allocate(3);
			out.putByte(response);
			out.putByte(player.getAuthentication().getPlayerRights());
			out.putByte(0);
			out.sendTo(channelContext.channel());

			/*
			 * Since login procedure is finished, switching to packet decoder.
			 */
			channelContext.decoder(new RSPacketDecoder());

			/*
			 * And finally sending the initialization packet to the client.
			 */
			player.getPacketDispatcher().sendInitPacket();
			break;
		}
	}

	/*
	 * We use this to store login packet length to check if all data has
	 * arrived.
	 */
	private int loginPacketLength;

	/**
	 * Login procedure states.
	 */
	private enum State {
		READ_USERNAME_HASH, READ_LOGIN_HEADER, READ_LOGIN_PAYLOAD
	}

}