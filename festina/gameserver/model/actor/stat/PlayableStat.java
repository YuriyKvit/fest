package com.festina.gameserver.model.actor.stat;

import com.festina.gameserver.model.actor.instance.L2PlayableInstance;
import com.festina.gameserver.model.base.Experience;

public class PlayableStat extends CharStat
{
    // =========================================================
    // Data Field
    
    // =========================================================
    // Constructor
    public PlayableStat(L2PlayableInstance[] activeChar)
    {
        super(activeChar);
    }

    // =========================================================
    // Method - Public
    public boolean addExp(int value)
    {
		if ((getExp() + value) < 0 || getExp() == (getExpForLevel(Experience.MAX_LEVEL) - 1)) 
            return true;
        
		if (getExp() + value >= getExpForLevel(Experience.MAX_LEVEL)) 
 			value = getExpForLevel(Experience.MAX_LEVEL) - 1 - getExp();

        setExp(getExp() + value);

		int level = 0;
		for (level = 1; level <= Experience.MAX_LEVEL; level++)
        {
        	if (getExp() >= getExpForLevel(level)) continue;
        	level--;
        	break;
        }
        if (level != getLevel()) addLevel(level - getLevel());

        return true;
    }
    
    public boolean removeExp(int value)
    {
        if ((getExp() + value) < 0 || getExp() == (getExpForLevel(Experience.MAX_LEVEL) - 1)) 
            return true;
        
        if (getExp() + value >= getExpForLevel(Experience.MAX_LEVEL)) 
            value = getExpForLevel(Experience.MAX_LEVEL) - 1 - getExp();

        setExp(getExp() - value);

        int level = 0;
        for (level = 1; level <= Experience.MAX_LEVEL; level++)
        {
            if (getExp() >= getExpForLevel(level)) continue;
            level--;
            break;
        }
        if (level != getLevel()) addLevel(level - getLevel());

        return true;
    }

    public boolean addExpAndSp(int addToExp, int addToSp)
    {
    	boolean expAdded = false;
    	boolean spAdded = false;
    	if (addToExp > 0) expAdded = addExp(addToExp);
    	if (addToSp > 0) spAdded = addSp(addToSp);

    	return expAdded || spAdded;
    }
    
    public boolean removeExpAndSp(int removeExp, int removeSp)
    {
        boolean expRemoved = false;
        boolean spRemoved = false;
        if (removeExp > 0) expRemoved = removeExp(removeExp);
        if (removeSp > 0) spRemoved = removeSp(removeSp);

        return expRemoved || spRemoved;
    }
    
    public boolean addLevel(int value)
    {
		if (getLevel() + value > Experience.MAX_LEVEL - 1)
        {
			if (getLevel() < Experience.MAX_LEVEL - 1)
				value = Experience.MAX_LEVEL - 1 - getLevel();
        	else
        		return false;
        }

        boolean levelIncreased = (getLevel() + value > getLevel());
        value += getLevel();
        setLevel(value);

        // Sync up exp with current level
        if (getExp() >= getExpForLevel(getLevel() + 1) || getExpForLevel(getLevel()) > getExp()) setExp(getExpForLevel(getLevel()));

        if (!levelIncreased) return false;
        
        getActiveChar().getStatus().setCurrentHp(getActiveChar().getStat().getMaxHp());
        getActiveChar().getStatus().setCurrentMp(getActiveChar().getStat().getMaxMp());

        return true;
    }

    public boolean addSp(int value)
    {
    	int currentSp = getSp();
    	if (currentSp == Integer.MAX_VALUE)
    		return false;
        
    	if (currentSp > Integer.MAX_VALUE - value)
    		value = Integer.MAX_VALUE - currentSp;
    	
        setSp(currentSp + value);
        return true;
    }
    
    public boolean removeSp(int value)
    {
        setSp(getSp() - value);
        return true;
    }
    
    public int getExpForLevel(int level) { return level; }

    // =========================================================
    // Method - Private

    // =========================================================
    // Property - Public
    public L2PlayableInstance getActiveChar() { return (L2PlayableInstance)super.getActiveChar(); }
}
