package com.festina.gameserver.model.actor.instance;

import java.util.Random;
import java.util.concurrent.ScheduledFuture;

import com.festina.gameserver.SkillTable;
import com.festina.gameserver.ThreadPoolManager;
import com.festina.gameserver.model.L2Character;
import com.festina.gameserver.model.L2Object;
import com.festina.gameserver.model.L2Skill;
import com.festina.gameserver.serverpackets.MagicSkillUser;
import com.festina.gameserver.templates.L2NpcTemplate;

/**
 * @author Drunkard Zabb0x
 * Lets drink2code!
 */
public class L2XmassTreeInstance extends L2NpcInstance
{
    ScheduledFuture aiTask;

    class XmassAI implements Runnable
    {
        L2XmassTreeInstance _caster;

        protected XmassAI(L2XmassTreeInstance caster)
        {
            _caster = caster;
        }

        public void run()
        {
            Random r = new Random();
            for (L2PcInstance player : getKnownList().getKnownPlayers().values())
            {
                int i = r.nextInt(3);
                handleCast(player, (4262 + i));
            }
        }

        private boolean handleCast(L2PcInstance player, int skillId)
        {
            L2Skill skill = SkillTable.getInstance().getInfo(skillId, 1);

            if (player.getEffect(skill) == null)
            {
                setTarget(player);
                doCast(skill);

                MagicSkillUser msu = new MagicSkillUser(_caster, player, skill.getId(), 1,
                                                        skill.getSkillTime(), 0);
                broadcastPacket(msu);

                return true;
            }

            return false;
        }

    }

    public L2XmassTreeInstance(int objectId, L2NpcTemplate template)
    {
        super(objectId, template);
        aiTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new XmassAI(this), 3000,
                                                                            3000);
    }

    public void deleteMe()
    {
        if (aiTask != null) aiTask.cancel(true);

        super.deleteMe();
    }

    public int getDistanceToWatchObject(L2Object object)
    {
        return 900;
    }

    /* (non-Javadoc)
     * @see com.festina.gameserver.model.L2Object#isAttackable()
     */
    public boolean isAutoAttackable(@SuppressWarnings("unused")
    L2Character attacker)
    {
        return false;
    }

}
