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
package org.solace.game.entity;

/**
 * Represents walking and running directions.
 * @author Graham Edgecombe
 *
 */
public class Sprites {
	
	/**
	 * The walking direction.
	 */
	private int primary = -1;
	
	/**
	 * The running direction.
	 */
	private int secondary = -1;
	
	/**
	 * Gets the primary sprite.
	 * @return The primary sprite.
	 */
	public int getPrimarySprite() {
		return primary;
	}
	
	/**
	 * Gets the secondary sprite.
	 * @return The secondary sprite.
	 */
	public int getSecondarySprite() {
		return secondary;
	}
	
	/**
	 * Sets the sprites.
	 * @param primary The primary sprite.
	 * @param secondary The secondary sprite.
	 */
	public void setSprites(int primary, int secondary) {
		this.primary = primary;
		this.secondary = secondary;
	}

}
