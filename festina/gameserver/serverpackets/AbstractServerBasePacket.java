/*
 * $HeadURL: $
 *
 * $Author: $
 * $Date: $
 * $Revision: $
 *
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package com.festina.gameserver.serverpackets;

/**
 * This class makes runImpl() and writeImpl() abstract for custom classes outside of this package
 * 
 * @version $Revision: $ $Date: $
 * @author  galun
 */
public abstract class AbstractServerBasePacket extends ServerBasePacket
{

    /**
     * @see com.festina.gameserver.serverpackets.ServerBasePacket#runImpl()
     */
    abstract protected void runImpl();

    /**
     * @see com.festina.gameserver.serverpackets.ServerBasePacket#writeImpl()
     */
    abstract protected void writeImpl();

    /**
     * @see com.festina.gameserver.BasePacket#getType()
     */
    abstract public String getType();
}
