package com.festina.gameserver.datatables;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;

import com.festina.L2DatabaseFactory;
import com.festina.gameserver.ClanTable;
import com.festina.gameserver.SkillTable;
import com.festina.gameserver.model.L2Skill;
import com.festina.gameserver.model.actor.instance.L2PcInstance;

public class NobleTable {
	//TODO: ну нельзя же нагружать базу постоянно делая запросы на нубл...
	private static Logger _log = Logger.getLogger(ClanTable.class.getName());
	public static void setnoble(int chrid)
	{
			try{
			Connection con = L2DatabaseFactory.getInstance().getConnection();
            
            PreparedStatement statement = con.prepareStatement("UPDATE characters SET noble = 1 WHERE obj_Id = ?");
            statement.setInt(1, chrid);
            statement.execute();
            statement.close();
			}
		catch(Exception e)
		{
			_log.warning("Some error on setNoble: "+e);
		}
	}

	public static boolean isNoble(int chrid) {
		Connection con = null;
		int isNoble = 0;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement = con.prepareStatement("SELECT noble FROM characters WHERE obj_Id = ?");
            statement.setInt(1, chrid);
            ResultSet result = statement.executeQuery();
			while(result.next())
            {
                isNoble = result.getInt("noble");
            }
            result.close();
			statement.close();
		}
		catch (Exception e) {
			_log.warning("data error on clan item: " + e);
		} finally {
			try { con.close(); } catch (Exception e) {}
		}
		if(isNoble == 1)
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	public static void getNobleSkill(L2PcInstance player)
	{
		player.addSkill(SkillTable.getInstance().getInfo(1323, 1));
		player.addSkill(SkillTable.getInstance().getInfo(1324, 1));
		player.addSkill(SkillTable.getInstance().getInfo(1325, 1));
		player.addSkill(SkillTable.getInstance().getInfo(1326, 1));
		player.addSkill(SkillTable.getInstance().getInfo(1327, 1));
		player.addSkill(SkillTable.getInstance().getInfo(325, 1));
		player.addSkill(SkillTable.getInstance().getInfo(326, 1));
		player.addSkill(SkillTable.getInstance().getInfo(327, 1));
		
	}
	public static void remNobleSkill(L2PcInstance player)
	{
		player.removeSkill(SkillTable.getInstance().getInfo(1323, 1));
		player.removeSkill(SkillTable.getInstance().getInfo(1324, 1));
		player.removeSkill(SkillTable.getInstance().getInfo(1325, 1));
		player.removeSkill(SkillTable.getInstance().getInfo(1326, 1));
		player.removeSkill(SkillTable.getInstance().getInfo(1327, 1));
		player.removeSkill(SkillTable.getInstance().getInfo(325, 1));
		player.removeSkill(SkillTable.getInstance().getInfo(326, 1));
		player.removeSkill(SkillTable.getInstance().getInfo(327, 1));
		
	}
}
