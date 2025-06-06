package com.festina.gameserver.model.actor.stat;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

import com.festina.Config;
import com.festina.L2DatabaseFactory;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.gameserver.model.base.Experience;
import com.festina.gameserver.serverpackets.PledgeShowMemberListAdd;
import com.festina.gameserver.serverpackets.SocialAction;
import com.festina.gameserver.serverpackets.StatusUpdate;
import com.festina.gameserver.serverpackets.SystemMessage;
import com.festina.gameserver.serverpackets.UserInfo;

public class PcStat extends PlayableStat
{
	private static Logger _log = Logger.getLogger(L2PcInstance.class.getName());

    // =========================================================
    // Data Field

    private int _OldMaxHp;      // stats watch
    private int _OldMaxMp;      // stats watch
    
    // =========================================================
    // Constructor
    public PcStat(L2PcInstance[] activeChar)
    {
        super(activeChar);
    }

    // =========================================================
    // Method - Public
    public boolean addExp(int value)
    {
        // Set new karma
        if (getActiveChar().getKarma() > 0 && (getActiveChar().isGM() || !getActiveChar().isInsideZone(L2PcInstance.ZONE_PVP)))
        {
            int karmaLost = getActiveChar().calculateKarmaLost(value);
            if (karmaLost > 0) getActiveChar().setKarma(getActiveChar().getKarma() - karmaLost);
        }

		if (!super.addExp(value)) return false;
		
        StatusUpdate su = new StatusUpdate(getActiveChar().getObjectId());
        su.addAttribute(StatusUpdate.EXP, getExp());
        getActiveChar().sendPacket(su);

        return true;
    }

    /**
     * Add Experience and SP rewards to the L2PcInstance, remove its Karma (if necessary) and Launch increase level task.<BR><BR>
     *
     * <B><U> Actions </U> :</B><BR><BR>
     * <li>Remove Karma when the player kills L2MonsterInstance</li>
     * <li>Send a Server->Client packet StatusUpdate to the L2PcInstance</li>
     * <li>Send a Server->Client System Message to the L2PcInstance </li>
     * <li>If the L2PcInstance increases it's level, send a Server->Client packet SocialAction (broadcast) </li>
     * <li>If the L2PcInstance increases it's level, manage the increase level task (Max MP, Max MP, Recommandation, Expertise and beginner skills...) </li>
     * <li>If the L2PcInstance increases it's level, send a Server->Client packet UserInfo to the L2PcInstance </li><BR><BR>
     *
     * @param addToExp The Experience value to add
     * @param addToSp The SP value to add
     */
    public boolean addExpAndSp(int addToExp, int addToSp)
    {
    	if (!super.addExpAndSp(addToExp, addToSp)) return false;

        // Send a Server->Client System Message to the L2PcInstance
        SystemMessage sm = new SystemMessage(SystemMessage.YOU_EARNED_S1_EXP_AND_S2_SP);
        sm.addNumber(addToExp);
        sm.addNumber(addToSp);
        getActiveChar().sendPacket(sm);

        return true;
    }
    
    public boolean removeExpAndSp(int addToExp, int addToSp)
    {
        if (!super.removeExpAndSp(addToExp, addToSp)) return false;

        // Send a Server->Client System Message to the L2PcInstance
        //TODO: add right System msg
        SystemMessage sm = new SystemMessage(SystemMessage.YOU_EARNED_S1_EXP_AND_S2_SP);
        sm.addNumber(addToExp);
        sm.addNumber(addToSp);
        getActiveChar().sendPacket(sm);

        return true;
    }

    public final boolean addLevel(int value)
    {
		if (getLevel() + value > Experience.MAX_LEVEL - 1) return false;

        boolean levelIncreased = super.addLevel(value);
        
        if (levelIncreased)
        {
			/**
			 * If there are no characters on the server, the bonuses will be applied to the first character that becomes level 6
			 *   and end if this character reaches level 25 or above.
			 * If the first character that becomes level 6 is deleted, the rest of the characters may not receive the new character bonus
			 * If the first character to become level 6 loses a level, and the player makes another character level 6,
			 *   the bonus will be applied to only the first character to achieve level 6.
			 * If the character loses a level after reaching level 25, the character may not receive the bonus.
			 */
        	if (!Config.ALT_GAME_NEW_CHAR_ALWAYS_IS_NEWBIE)
        	{
				if (getActiveChar().getLevel() >= Experience.MIN_NEWBIE_LEVEL && getActiveChar().getLevel() < Experience.MAX_NEWBIE_LEVEL && !getActiveChar().isNewbie())
	        	{
	        		java.sql.Connection con = null;
					try
					{
						con = L2DatabaseFactory.getInstance().getConnection();
						PreparedStatement statement;
						
						statement = con.prepareStatement("SELECT value FROM account_data WHERE (account_name=?) AND (var='newbie_char')");
						statement.setString(1, getActiveChar().getAccountName());
						ResultSet rset = statement.executeQuery();
						
						if (!rset.next())
						{
							PreparedStatement statement1;
							statement1 = con.prepareStatement("INSERT INTO account_data (account_name, var, value) VALUES (?, 'newbie_char', ?)");
                            statement1.setString(1, getActiveChar().getAccountName());
							statement1.setInt(2, getActiveChar().getObjectId());
							statement1.executeUpdate();
							statement1.close();
                            
							getActiveChar().setNewbie(true);
							if (Config.DEBUG) _log.info("New newbie character: " + getActiveChar().getCharId());
						};
						rset.close();
						statement.close();
					}
					catch (SQLException e)
					{
						_log.warning("Could not check character for newbie: " + e);
					}
					finally
					{
						try { con.close(); } catch (Exception e) {}
					}
	        	};
	
	        	if (getActiveChar().getLevel() >= 25 && getActiveChar().isNewbie())
	        	{
	        		getActiveChar().setNewbie(false);
					if (Config.DEBUG) _log.info("Newbie character ended: " + getActiveChar().getCharId());
	        	};
        	};
        	
        	getActiveChar().setCurrentCp(getMaxCp());
            getActiveChar().broadcastPacket(new SocialAction(getActiveChar().getObjectId(), 15));
            getActiveChar().sendPacket(new SystemMessage(SystemMessage.YOU_INCREASED_YOUR_LEVEL));
        }

        getActiveChar().rewardSkills(); // Give Expertise skill of this level
        if (getActiveChar().getClan() != null)
        {
        	getActiveChar().getClan().updateClanMember(getActiveChar());
        	getActiveChar().getClan().broadcastToOtherOnlineMembers(new PledgeShowMemberListAdd(getActiveChar()), getActiveChar());
        }
        if (getActiveChar().isInParty()) getActiveChar().getParty().recalculatePartyLevel(); // Recalculate the party level

        StatusUpdate su = new StatusUpdate(getActiveChar().getObjectId());
        su.addAttribute(StatusUpdate.LEVEL, getLevel());
        su.addAttribute(StatusUpdate.MAX_CP, getMaxCp());
        su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
        su.addAttribute(StatusUpdate.MAX_MP, getMaxMp());
        getActiveChar().sendPacket(su);
            
        // Send a Server->Client packet UserInfo to the L2PcInstance
        getActiveChar().sendPacket(new UserInfo(getActiveChar()));

        return levelIncreased;
    }

    public boolean addSp(int value)
    {
        if (!super.addSp(value)) return false;

        StatusUpdate su = new StatusUpdate(getActiveChar().getObjectId());
        su.addAttribute(StatusUpdate.SP, getSp());
        getActiveChar().sendPacket(su);

        return true;
    }

    public final int getExpForLevel(int level) { return Experience.LEVEL[level]; }

    // =========================================================
    // Method - Private

    // =========================================================
    // Property - Public
    public final L2PcInstance getActiveChar() { return (L2PcInstance)super.getActiveChar(); }

    public final int getExp()
    {
        if (getActiveChar().isSubClassActive()) 
	        return getActiveChar().getSubClasses().get(getActiveChar().getClassIndex()).getExp();
        
        return super.getExp();
    }
    
    public final void setExp(int value)
    {
        if (getActiveChar().isSubClassActive())
            getActiveChar().getSubClasses().get(getActiveChar().getClassIndex()).setExp(value);
        else
            super.setExp(value);
    }

    public final int getLevel()
    {
        if (getActiveChar().isSubClassActive()) 
        	return getActiveChar().getSubClasses().get(getActiveChar().getClassIndex()).getLevel();
        	
        return super.getLevel();
    }
    public final void setLevel(int value)
    {
		if (value > Experience.MAX_LEVEL - 1) 
			value = Experience.MAX_LEVEL - 1;
        	
        if (getActiveChar().isSubClassActive())
            getActiveChar().getSubClasses().get(getActiveChar().getClassIndex()).setLevel(value);
        else
            super.setLevel(value);
        //getActiveChar().giveAvailableSkills();
    }

    public final int getMaxHp()
    {
        // Get the Max HP (base+modifier) of the L2PcInstance
        int val = super.getMaxHp();
        if (val != _OldMaxHp)
        {
            _OldMaxHp = val;

            // Launch a regen task if the new Max HP is higher than the old one
            if (getActiveChar().getStatus().getCurrentHp() != val) getActiveChar().getStatus().setCurrentHp(getActiveChar().getStatus().getCurrentHp()); // trigger start of regeneration
        }

        return val;
    }

    public final int getMaxMp()
    {
        // Get the Max MP (base+modifier) of the L2PcInstance
        int val = super.getMaxMp();
        
        if (val != _OldMaxMp)
        {
            _OldMaxMp = val;

            // Launch a regen task if the new Max MP is higher than the old one
            if (getActiveChar().getStatus().getCurrentMp() != val) 
            	getActiveChar().getStatus().setCurrentMp(getActiveChar().getStatus().getCurrentMp()); // trigger start of regeneration
        }

        return val;
    }

    public final int getSp()
    {
        if (getActiveChar().isSubClassActive()) 
        	return getActiveChar().getSubClasses().get(getActiveChar().getClassIndex()).getSp();
        	
        return super.getSp();
    }
    public final void setSp(int value)
    {
        if (getActiveChar().isSubClassActive())
            getActiveChar().getSubClasses().get(getActiveChar().getClassIndex()).setSp(value);
        else
            super.setSp(value);
    }
}
