package com.festina.gameserver.communitybbs.Manager;

import com.festina.gameserver.datatables.*;
import com.festina.gameserver.serverpackets.*;
import javolution.lang.TextBuilder;
import com.festina.Config;
import com.festina.gameserver.model.*;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import java.util.logging.Logger;

public class CustomBBSManager extends BaseBBSManager {

    private static CustomBBSManager _instance;

    public static void init() {
        _instance = new CustomBBSManager();
    }

    public static CustomBBSManager getInstance() {
        return _instance;
    }

    protected static Logger _log = Logger.getLogger(Config.class.getName());

    @Override
    public void parsecmd(String command, L2PcInstance player) {
        TextBuilder tb = new TextBuilder("");
//        tb.append(getPwHtm("menu")); // TODO Separate header munu and content
        if (command.equalsIgnoreCase("_pwhome")) {
            String content = getPwHtm("menu");
            if (content == null) {
                content = "<html><body><br><br><center>404 :File Not found: '" + PWHTML + "menu.htm' </center></body></html>";
            }
            separateAndSend(content + "</body></html>", player);
            return;
        } else if (command.startsWith("_bbspwhtm")) {
            String htm = command.substring(10).trim();
            String content = getPwHtm(htm);
            if (content == null) {
                content = "<html><body><br><br><center>Page: " + htm + ".htm not found.</center></body></html>";
            }
            tb.append(content + "</body></html>");
        } else if (command.startsWith("_bbsmultisell")) {
            String[] tmp = command.substring(14).split(" ");
//            L2Multisell.getInstance().SeparateAndSend(Integer.parseInt(tmp[1]), player, false, 0);
            player.sendPacket(new MultiSellList(Integer.parseInt(tmp[1]), false, player));
            String content = getPwHtm(tmp[0]);
            if (content == null) {
                content = "<html><body><br><br><center>Page: " + tmp[0] + ".htm not found.</center></body></html>";
            }
            tb.append(content + "</body></html>");
        } else if (command.startsWith("_bbsbuff")) {
            String[] tmp = command.substring(9).split(" ");
            try {
//                int buff_id = Integer.parseInt(tmp[1]);

                if (tmp.length == 6) {
                    int coin_id = Integer.parseInt(tmp[3]);
                    int coin_cnt = Integer.parseInt(tmp[4]);
                    player.destroyItemByItemId("Buffer", coin_id, coin_cnt, player, true);
                }
                // Something like allowed buff check ... don`t know
//                if (CustomServerData.getInstance().isWhiteBuff(buff_id)) {
//                    player.stopSkillEffects(buff_id);
//                    SkillTable.getInstance().getInfo(buff_id, Integer.parseInt(tmp[2])).getEffects(player, player);
//                }
            } catch (Exception ignored) {
                //
            }

            String content = getPwHtm(tmp[0]);
            if (content == null) {
                content = "<html><body><br><br><center>Page: " + tmp[0] + ".htm not found.</center></body></html>";
            }

            tb.append(content + "</body></html>");
        } else if (command.startsWith("_bbsteleto")) {
            String[] tmp = command.substring(11).trim().split("_");
//            int type = Integer.parseInt(tmp[0]);
            int x = Integer.parseInt(tmp[1]);
            int y = Integer.parseInt(tmp[2]);
            int z = Integer.parseInt(tmp[3]);

            tb.append("<br><br>&nbsp;&nbsp;Happy travels!</body></html>");
            player.teleToLocation(x, y, z);
        } else if (command.startsWith("_bbsbDop")) {
            switch (Integer.parseInt(command.substring(8).trim())) {
                case 1:
                    player.stopAllEffects(); // Remove buffs
                    break;
                case 2:
                    player.setCurrentHpMp(player.getMaxHp(), player.getMaxMp()); // Heal
                    player.setCurrentCp(player.getMaxCp());
                    break;
                case 3:
                    player.doRebuff(true);
                    break;
                case 4:
                    player.doFullBuff(1);
                    break;
                case 5:
                    player.doFullBuff(2);
                    break;
            }
            String content = getPwHtm("1");
            if (content == null) {
                content = "<html><body><br><br><center>Page: 1.htm not found.</center></body></html>";
            }
            tb.append(content);
        } else if (command.startsWith("_bbsprofileBuff")) {
            player.doBuffProfile(Integer.parseInt(command.substring(15).trim()));
            String content = getPwHtm("index");
            if (content == null) {
                content = "<html><body><br><br><center>Page: 40001.htm not found.</center></body></html>";
            }
            tb.append(content + "</body></html>");
        } else if (command.startsWith("_bbssprofileBuff")) {
            player.saveBuffProfile(Integer.parseInt(command.substring(16).trim()));
            String content = getPwHtm("40001-4");
            if (content == null) {
                content = "<html><body><br><br><center>Page: 40001-4.htm not found.</center></body></html>";
            }
            tb.append(content + "</body></html>");
        } else if (command.equalsIgnoreCase("_bbssell")) {
            player.sendPacket(new SellList(player));
            return;
        } else if (command.startsWith("_bbsnobless")) {
            if (NobleTable.isNoble(player.getObjectId())) {
                tb.append("You are already nobles");
                separateAndSend(tb.toString(), player);
                tb.reset();
                tb = null;
                return;
            }

            if (Config.SNOBLE_COIN > 0) {
                L2ItemInstance coins = player.getInventory().getItemByItemId(Config.SNOBLE_COIN);
                if (coins == null || coins.getCount() < Config.SNOBLE_PRICE) {
                    tb.append("Price for noble: " + Config.SNOBLE_PRICE + " " + Config.SNOBLE_COIN_NAME + ".");
                    separateAndSend(tb.toString(), player);
                    tb.reset();
                    tb = null;
                    return;
                }

                player.destroyItemByItemId("Donate Shop", Config.SNOBLE_COIN, Config.SNOBLE_PRICE, player, true);
            }

            NobleTable.setnoble(player.getObjectId());
            NobleTable.getNobleSkill(player);
            player.addItem("rewardNoble", 7694, 1, player, true);
            player.sendPacket(new PlaySound("ItemSound.quest_finish"));

            tb.append("Congratulations with nobles!");
        }
        separateAndSend(tb.toString(), player);
        tb.reset();
        tb = null;
    }

    @Override
    public void parsewrite(String ar1, String ar2, String ar3, String ar4, String ar5, L2PcInstance player) {
        // TODO Auto-generated method stub
    }
}
