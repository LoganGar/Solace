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
package org.solace.event.listener;

import mint.event.EventHandler;
import mint.event.EventListener;
import org.solace.event.events.SpecialAttackEvent;
import org.solace.game.content.combat.specials.SpecialAttackManager;
import org.solace.game.item.container.impl.Equipment;

public class SpecialAttackListener implements EventListener {

    @EventHandler
    public void executeSpecial(SpecialAttackEvent event) {
        SpecialAttackManager.handleSpecial(event.getPlayer(), event.getPlayer().getEquipment().getItemBySlot(Equipment.WEAPON_SLOT));
    }

}
