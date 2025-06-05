/*
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
package com.festina.gameserver.script;

import com.festina.gameserver.Announcements;
import com.festina.gameserver.CharNameTable;
import com.festina.gameserver.CharTemplateTable;
import com.festina.gameserver.ClanTable;
import com.festina.gameserver.GameTimeController;
import com.festina.gameserver.ItemTable;
import com.festina.gameserver.LevelUpData;
import com.festina.gameserver.MapRegionTable;
import com.festina.gameserver.NpcTable;
import com.festina.gameserver.RecipeController;
import com.festina.gameserver.SkillTable;
import com.festina.gameserver.SkillTreeTable;
import com.festina.gameserver.SpawnTable;
import com.festina.gameserver.TeleportLocationTable;
import com.festina.gameserver.idfactory.IdFactory;
import com.festina.gameserver.model.L2World;

/**
 * @author Luis Arias
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public interface EngineInterface
{
    //*  keep the references of Singletons to prevent garbage collection
    public CharNameTable _charNametable = CharNameTable.getInstance();

    public IdFactory _idFactory = IdFactory.getInstance();
    public ItemTable _itemTable = ItemTable.getInstance();
    
    public SkillTable _skillTable = SkillTable.getInstance();
    
    public RecipeController _recipeController = RecipeController.getInstance();
    
    public SkillTreeTable _skillTreeTable = SkillTreeTable.getInstance();
    public CharTemplateTable _charTemplates = CharTemplateTable.getInstance();
    public ClanTable _clanTable = ClanTable.getInstance();

    public NpcTable _npcTable = NpcTable.getInstance();
    
    public TeleportLocationTable _teleTable = TeleportLocationTable.getInstance();
    public LevelUpData _levelUpData = LevelUpData.getInstance();
    public L2World _world = L2World.getInstance();   
    public SpawnTable _spawnTable = SpawnTable.getInstance();
    public GameTimeController _gameTimeController = GameTimeController.getInstance();
    public Announcements _announcements = Announcements.getInstance();
    public MapRegionTable _mapRegions = MapRegionTable.getInstance();
    
    
    
    //public ArrayList getAllPlayers();
    //public Player getPlayer(String characterName);
    public void addQuestDrop(int npcID, int itemID, int min, int max, int chance, String questID, String[] states);
    public void addEventDrop(int[] items, int[] count, double chance, DateRange range);
    public void onPlayerLogin(String[] message, DateRange range);

}
