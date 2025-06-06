package com.festina.gameserver;
 
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;
import com.festina.Config;
import com.festina.L2DatabaseFactory;
import com.festina.gameserver.ai.CtrlIntention;
import com.festina.gameserver.model.L2CharPosition;
import com.festina.gameserver.model.L2Effect;
import com.festina.gameserver.model.L2ItemInstance;
import com.festina.gameserver.model.L2Party;
import com.festina.gameserver.model.L2Spawn;
import com.festina.gameserver.model.L2World;
import com.festina.gameserver.model.SpawnListener;
import com.festina.gameserver.model.actor.instance.L2FestivalMonsterInstance;
import com.festina.gameserver.model.actor.instance.L2NpcInstance;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.gameserver.serverpackets.CreatureSay;
import com.festina.gameserver.serverpackets.MagicSkillUser;
import com.festina.gameserver.templates.L2NpcTemplate;
import com.festina.gameserver.templates.StatsSet;
import com.festina.gameserver.util.Util;

/**
 *  Seven Signs Festival of Darkness Engine
 *    
 *  TODO:
 *  - Archer mobs should target healer characters over other party members.
 *  - ADDED 29 Sep: Players that leave a party during the Seven Signs Festival will now take damage and cannot be healed. 
 *    
 *  @author Tempy
 */
public class SevenSignsFestival implements SpawnListener 
{
    protected static Logger _log = Logger.getLogger(SevenSignsFestival.class.getName());
    private static SevenSignsFestival _instance;
    
    public static final String FESTIVAL_DATA_FILE = "config/signs.properties";
    
    /**
     * These length settings are important! :)
     * All times are relative to the ELAPSED time (in ms) since a festival begins.
     * 
     * Festival manager start is the time after the server starts to begin the first
     * festival cycle.
     * 
     * The cycle length should ideally be at least 2x longer than the festival length.
     * This allows ample time for players to sign-up to participate in the festival.
     * 
     * The intermission is the time between the festival participants being moved
     * to the "arenas" and the spawning of the first set of mobs.
     * 
     * The monster swarm time is the time before the monsters swarm to the center of the arena,
     * after they are spawned.
     * 
     * The chest spawn time is for when the bonus festival chests spawn, usually 
     * towards the end of the festival.
     */
    public static final long FESTIVAL_SIGNUP_TIME = Config.ALT_FESTIVAL_CYCLE_LENGTH - Config.ALT_FESTIVAL_LENGTH - 60000;
     
    // Key Constants \\
    private static final int FESTIVAL_MAX_OFFSET_X = 230;
    private static final int FESTIVAL_MAX_OFFSET_Y = 230;
    private static final int FESTIVAL_DEFAULT_RESPAWN = 60; // Specify in seconds!

    public static final int FESTIVAL_COUNT = 5;    
    public static final int FESTIVAL_LEVEL_MAX_31 = 0;
    public static final int FESTIVAL_LEVEL_MAX_42 = 1;
    public static final int FESTIVAL_LEVEL_MAX_53 = 2;
    public static final int FESTIVAL_LEVEL_MAX_64 = 3;
    public static final int FESTIVAL_LEVEL_MAX_NONE = 4;
    public static final int[] FESTIVAL_LEVEL_SCORES = {60, 70, 100, 120, 150}; // 500 maximum possible score 
    
    public static final int FESTIVAL_OFFERING_ID = 5901;
    public static final int FESTIVAL_OFFERING_VALUE = 5;
    
    //////////////////////// \\\\\\\\\\\\\\\\\\\\\\\\\\
    /*
    * The following contains all the necessary spawn data for:
    * - Player Start Locations
    * - Witches
    * - Monsters
    * - Chests
    * 
    * All data is given by: X, Y, Z (coords), Heading, NPC ID (if necessary)
    * This may be moved externally in time, but the data should not change.
    */
    public static final int[][] festivalDawnPlayerSpawns = 
    {
         {-79187, 113186, -4895, 0}, // 31 and below
         {-75918, 110137, -4895, 0}, // 42 and below
         {-73835, 111969, -4895, 0}, // 53 and below
         {-76170, 113804, -4895, 0}, // 64 and below
         {-78927, 109528, -4895, 0}  // No level limit
    };
    
    public static final int[][] festivalDuskPlayerSpawns = 
    {
         {-77200, 88966, -5151, 0}, // 31 and below
         {-76941, 85307, -5151, 0}, // 42 and below
         {-74855, 87135, -5151, 0}, // 53 and below
         {-80208, 88222, -5151, 0}, // 64 and below
         {-79954, 84697, -5151, 0}  // No level limit
    };
    
    protected static final int[][] festivalDawnWitchSpawns = 
    {
         {-79183, 113052, -4891, 0, 8132}, // 31 and below
         {-75916, 110270, -4891, 0, 8133}, // 42 and below
         {-73979, 111970, -4891, 0, 8134}, // 53 and below
         {-76174, 113663, -4891, 0, 8135}, // 64 and below
         {-78930, 109664, -4891, 0, 8136}  // No level limit
    };
    
    protected static final int[][] festivalDuskWitchSpawns = 
    {
         {-77199, 88830, -5147, 0, 8142}, // 31 and below
         {-76942, 85438, -5147, 0, 8143}, // 42 and below
         {-74990, 87135, -5147, 0, 8144}, // 53 and below
         {-80207, 88222, -5147, 0, 8145}, // 64 and below
         {-79952, 84833, -5147, 0, 8146}  // No level limit
    };
    
    protected static final int[][][] festivalDawnPrimarySpawns = 
    {
     {
         /* Level 31 and Below - Offering of the Branded */
         {-78537, 113839, -4895, -1, 12622},
         {-78466, 113852, -4895, -1, 12623},
         {-78509, 113899, -4895, -1, 12623},
         
         {-78481, 112557, -4895, -1, 12622},
         {-78559, 112504, -4895, -1, 12623},
         {-78489, 112494, -4895, -1, 12623},
         
         {-79803, 112543, -4895, -1, 12625},
         {-79854, 112492, -4895, -1, 12626},
         {-79886, 112557, -4895, -1, 12627},
         
         {-79821, 113811, -4895, -1, 12628},
         {-79857, 113896, -4895, -1, 12630},
         {-79878, 113816, -4895, -1, 12631},
         
         // Archers and Marksmen \\
         {-79190, 113660, -4895, -1, 12624},
         {-78710, 113188, -4895, -1, 12624},
         {-79190, 112730, -4895, -1, 12629},
         {-79656, 113188, -4895, -1, 12629}
     },
     {
         /* Level 42 and Below - Apostate Offering */
         {-76558, 110784, -4895, -1, 12632},
         {-76607, 110815, -4895, -1, 12633}, // South West
         {-76559, 110820, -4895, -1, 12633},
         
         {-75277, 110792, -4895, -1, 12632},
         {-75225, 110801, -4895, -1, 12633}, // South East
         {-75262, 110832, -4895, -1, 12633},
         
         {-75249, 109441, -4895, -1, 12635},
         {-75278, 109495, -4895, -1, 12636}, // North East
         {-75223, 109489, -4895, -1, 12637},
         
         {-76556, 109490, -4895, -1, 12638},
         {-76607, 109469, -4895, -1, 12640}, // North West
         {-76561, 109450, -4895, -1, 12641},
         
         // Archers and Marksmen \\
         {-76399, 110144, -4895, -1, 12634},
         {-75912, 110606, -4895, -1, 12634},
         {-75444, 110144, -4895, -1, 12639},
         {-75930, 109665, -4895, -1, 12639}
     },
     {
         /* Level 53 and Below - Witch's Offering */
         {-73184, 111319, -4895, -1, 12642},
         {-73135, 111294, -4895, -1, 12643}, // South West
         {-73185, 111281, -4895, -1, 12643},
         
         {-74477, 111321, -4895, -1, 12642},
         {-74523, 111293, -4895, -1, 12643}, // South East
         {-74481, 111280, -4895, -1, 12643},
         
         {-74489, 112604, -4895, -1, 12645},
         {-74491, 112660, -4895, -1, 12646}, // North East
         {-74527, 112629, -4895, -1, 12647},
         
         {-73197, 112621, -4895, -1, 12648},
         {-73142, 112631, -4895, -1, 12650}, // North West
         {-73182, 112656, -4895, -1, 12651},
         
         // Archers and Marksmen \\
         {-73834, 112430, -4895, -1, 12644}, 
         {-74299, 111959, -4895, -1, 12644}, 
         {-73841, 111491, -4895, -1, 12649}, 
         {-73363, 111959, -4895, -1, 12649} 
     },	
     {
         /* Level 64 and Below - Dark Omen Offering */
         {-75543, 114461, -4895, -1, 12652},
         {-75514, 114493, -4895, -1, 12653}, // South West
         {-75488, 114456, -4895, -1, 12653},
         
         {-75521, 113158, -4895, -1, 12652},
         {-75504, 113110, -4895, -1, 12653}, // South East
         {-75489, 113142, -4895, -1, 12653},
         
         {-76809, 113143, -4895, -1, 12655},
         {-76860, 113138, -4895, -1, 12656}, // North East
         {-76831, 113112, -4895, -1, 12657},
         
         {-76831, 114441, -4895, -1, 12658},
         {-76840, 114490, -4895, -1, 12660}, // North West
         {-76864, 114455, -4895, -1, 12661},
         
         // Archers and Marksmen \\
         {-75703, 113797, -4895, -1, 12654}, 
         {-76180, 114263, -4895, -1, 12654}, 
         {-76639, 113797, -4895, -1, 12659}, 
         {-76180, 113337, -4895, -1, 12659} 
     },
     {
         /* No Level Limit - Offering of Forbidden Path */
         {-79576, 108881, -4895, -1, 12662},
         {-79592, 108835, -4895, -1, 12663}, // South West
         {-79614, 108871, -4895, -1, 12663},
         
         {-79586, 110171, -4895, -1, 12662},
         {-79589, 110216, -4895, -1, 12663}, // South East
         {-79620, 110177, -4895, -1, 12663},
         
         {-78825, 110182, -4895, -1, 12665},
         {-78238, 110182, -4895, -1, 12666}, // North East
         {-78266, 110218, -4895, -1, 12667},
         
         {-78275, 108883, -4895, -1, 12668},
         {-78267, 108839, -4895, -1, 12670}, // North West
         {-78241, 108871, -4895, -1, 12671},
         
         // Archers and Marksmen \\
         {-79394, 109538, -4895, -1, 12664}, 
         {-78929, 109992, -4895, -1, 12664}, 
         {-78454, 109538, -4895, -1, 12669}, 
         {-78929, 109053, -4895, -1, 12669} 
     }
    };
    
    protected static final int[][][] festivalDuskPrimarySpawns = 
    {
     {
         /* Level 31 and Below - Offering of the Branded */
         {-76542, 89653, -5151, -1, 12622},
         {-76509, 89637, -5151, -1, 12623},
         {-76548, 89614, -5151, -1, 12623},
         
         {-76539, 88326, -5151, -1, 12622},
         {-76512, 88289, -5151, -1, 12623},
         {-76546, 88287, -5151, -1, 12623},
         
         {-77879, 88308, -5151, -1, 12625},
         {-77886, 88310, -5151, -1, 12626},
         {-77879, 88278, -5151, -1, 12627},
         
         {-77857, 89605, -5151, -1, 12628},
         {-77858, 89658, -5151, -1, 12630},
         {-77891, 89633, -5151, -1, 12631},
         
         // Archers and Marksmen \\
         {-76728, 88962, -5151, -1, 12624},
         {-77194, 88494, -5151, -1, 12624},
         {-77660, 88896, -5151, -1, 12629},
         {-77195, 89438, -5151, -1, 12629}
     },
     {
         /* Level 42 and Below - Apostate's Offering */
         {-77585, 84650, -5151, -1, 12632},
         {-77628, 84643, -5151, -1, 12633},
         {-77607, 84613, -5151, -1, 12633},
         
         {-76603, 85946, -5151, -1, 12632},
         {-77606, 85994, -5151, -1, 12633},
         {-77638, 85959, -5151, -1, 12633},
         
         {-76301, 85960, -5151, -1, 12635},
         {-76257, 85972, -5151, -1, 12636},
         {-76286, 85992, -5151, -1, 12637},
         
         {-76281, 84667, -5151, -1, 12638},
         {-76291, 84611, -5151, -1, 12640},
         {-76257, 84616, -5151, -1, 12641},
         
         // Archers and Marksmen \\
         {-77419, 85307, -5151, -1, 12634},
         {-76952, 85768, -5151, -1, 12634},
         {-76477, 85312, -5151, -1, 12639},
         {-76942, 84832, -5151, -1, 12639}
     },
     {
         /* Level 53 and Below - Witch's Offering */
         {-74211, 86494, -5151, -1, 12642},
         {-74200, 86449, -5151, -1, 12643},
         {-74167, 86464, -5151, -1, 12643},
         
         {-75495, 86482, -5151, -1, 12642},
         {-75540, 86473, -5151, -1, 12643},
         {-75509, 86445, -5151, -1, 12643},
         
         {-75509, 87775, -5151, -1, 12645},
         {-75518, 87826, -5151, -1, 12646},
         {-75542, 87780, -5151, -1, 12647},
         
         {-74214, 87789, -5151, -1, 12648},
         {-74169, 87801, -5151, -1, 12650},
         {-74198, 87827, -5151, -1, 12651},
         
         // Archers and Marksmen \\
         {-75324, 87135, -5151, -1, 12644},
         {-74852, 87606, -5151, -1, 12644},
         {-74388, 87146, -5151, -1, 12649},
         {-74856, 86663, -5151, -1, 12649}
     },  
     {
         /* Level 64 and Below - Dark Omen Offering */
         {-79560, 89007, -5151, -1, 12652},
         {-79521, 89016, -5151, -1, 12653},
         {-79544, 89047, -5151, -1, 12653},
         
         {-79552, 87717, -5151, -1, 12652},
         {-79552, 87673, -5151, -1, 12653},
         {-79510, 87702, -5151, -1, 12653},
         
         {-80866, 87719, -5151, -1, 12655},
         {-80897, 87689, -5151, -1, 12656},
         {-80850, 87685, -5151, -1, 12657},
         
         {-80848, 89013, -5151, -1, 12658},
         {-80887, 89051, -5151, -1, 12660},
         {-80891, 89004, -5151, -1, 12661},
         
         // Archers and Marksmen \\
         {-80205, 87895, -5151, -1, 12654},
         {-80674, 88350, -5151, -1, 12654},
         {-80209, 88833, -5151, -1, 12659},
         {-79743, 88364, -5151, -1, 12659}
     },
     {
         /* No Level Limit - Offering of Forbidden Path */
         {-80624, 84060, -5151, -1, 12662},
         {-80621, 84007, -5151, -1, 12663},
         {-80590, 84039, -5151, -1, 12663},
         
         {-80605, 85349, -5151, -1, 12662},
         {-80639, 85363, -5151, -1, 12663},
         {-80611, 85385, -5151, -1, 12663},
         
         {-79311, 85353, -5151, -1, 12665},
         {-79277, 85384, -5151, -1, 12666},
         {-79273, 85539, -5151, -1, 12667},
         
         {-79297, 84054, -5151, -1, 12668},
         {-79285, 84006, -5151, -1, 12670},
         {-79260, 84040, -5151, -1, 12671},
         
         // Archers and Marksmen \\
         {-79945, 85171, -5151, -1, 12664},
         {-79489, 84707, -5151, -1, 12664},
         {-79952, 84222, -5151, -1, 12669},
         {-80423, 84703, -5151, -1, 12669}
     }
    };
    
    protected static final int[][][] festivalDawnSecondarySpawns =
    {
     {   
         /* 31 and Below */
         {-78757, 112834, -4895, -1, 12629},
         {-78581, 112834, -4895, -1, 12629},
         
         {-78822, 112526, -4895, -1, 12624},
         {-78822, 113702, -4895, -1, 12624},
         {-78822, 113874, -4895, -1, 12624},
         
         {-79524, 113546, -4895, -1, 12624},
         {-79693, 113546, -4895, -1, 12624},
         {-79858, 113546, -4895, -1, 12624},
         
         {-79545, 112757, -4895, -1, 12629},
         {-79545, 112586, -4895, -1, 12629},
     },
     {
         /* 42 and Below */
         {-75565, 110580, -4895, -1, 12639},
         {-75565, 110740, -4895, -1, 12639},
         
         {-75577, 109776, -4895, -1, 12634},
         {-75413, 109776, -4895, -1, 12634},
         {-75237, 109776, -4895, -1, 12634},
         
         {-76274, 109468, -4895, -1, 12634},
         {-76274, 109635, -4895, -1, 12634},
         {-76274, 109795, -4895, -1, 12634},
         
         {-76351, 110500, -4895, -1, 12669},
         {-76528, 110500, -4895, -1, 12669},    
     },
     {   
         /* 53 and Below */
         {-74191, 111527, -4895, -1, 12649},
         {-74191, 111362, -4895, -1, 12649},
         
         {-73495, 111611, -4895, -1, 12644},
         {-73327, 111611, -4895, -1, 12644},
         {-73154, 111611, -4895, -1, 12644},
         
         {-73473, 112301, -4895, -1, 12644},
         {-73473, 112475, -4895, -1, 12644},
         {-73473, 112649, -4895, -1, 12644},
         
         {-74270, 112326, -4895, -1, 12649},
         {-74443, 112326, -4895, -1, 12649},  
     },
     { 
         /* 64 and Below */
         {-75738, 113439, -4895, -1, 12659},
         {-75571, 113439, -4895, -1, 12659},
         
         {-75824, 114141, -4895, -1, 12654},
         {-75824, 114309, -4895, -1, 12654},
         {-75824, 114477, -4895, -1, 12654},
         
         {-76513, 114158, -4895, -1, 12654},
         {-76683, 114158, -4895, -1, 12654},
         {-76857, 114158, -4895, -1, 12654},
         
         {-76535, 113357, -4895, -1, 12669},
         {-76535, 113190, -4895, -1, 12669},  
     },
     {
         /* No Level Limit */
         {-79350, 109894, -4895, -1, 12669},
         {-79534, 109894, -4895, -1, 12669},
         
         {-79285, 109187, -4895, -1, 12664},
         {-79285, 109019, -4895, -1, 12664},
         {-79285, 108860, -4895, -1, 12664},
         
         {-78587, 109172, -4895, -1, 12664},
         {-78415, 109172, -4895, -1, 12664},
         {-78249, 109172, -4895, -1, 12664},
         
         {-78575, 109961, -4895, -1, 12669},
         {-78575, 110130, -4895, -1, 12669},
     }
    };    
    
    protected static final int[][][] festivalDuskSecondarySpawns =
    {
     {
         /* 31 and Below */
         {-76844, 89304, -5151, -1, 12624},
         {-76844, 89479, -5151, -1, 12624},
         {-76844, 89649, -5151, -1, 12624},
         
         {-77544, 89326, -5151, -1, 12624},
         {-77716, 89326, -5151, -1, 12624},
         {-77881, 89326, -5151, -1, 12624},
         
         {-77561, 88530, -5151, -1, 12629},
         {-77561, 88364, -5151, -1, 12629},
         
         {-76762, 88615, -5151, -1, 12629},
         {-76594, 88615, -5151, -1, 12629},
     },
     {
         /* 42 and Below */
         {-77307, 84969, -5151, -1, 12634},
         {-77307, 84795, -5151, -1, 12634},
         {-77307, 84623, -5151, -1, 12634},
         
         {-76614, 84944, -5151, -1, 12634},
         {-76433, 84944, -5151, -1, 12634},
         {-7626-1, 84944, -5151, -1, 12634},
         
         {-76594, 85745, -5151, -1, 12639},
         {-76594, 85910, -5151, -1, 12639},
         
         {-77384, 85660, -5151, -1, 12639},
         {-77555, 85660, -5151, -1, 12639},
     },
     {
         /* 53 and Below */
         {-74517, 86782, -5151, -1, 12644},
         {-74344, 86782, -5151, -1, 12644},
         {-74185, 86782, -5151, -1, 12644},
         
         {-74496, 87464, -5151, -1, 12644},
         {-74496, 87636, -5151, -1, 12644},
         {-74496, 87815, -5151, -1, 12644},
         
         {-75298, 87497, -5151, -1, 12649},
         {-75460, 87497, -5151, -1, 12649},
         
         {-75219, 86712, -5151, -1, 12649},
         {-75219, 86531, -5151, -1, 12649},
     },
     {
         /* 64 and Below */
         {-79851, 88703, -5151, -1, 12654},
         {-79851, 88868, -5151, -1, 12654},
         {-79851, 89040, -5151, -1, 12654},
         
         {-80548, 88722, -5151, -1, 12654},
         {-80711, 88722, -5151, -1, 12654},
         {-80883, 88722, -5151, -1, 12654},
         
         {-80565, 87916, -5151, -1, 12659},
         {-80565, 87752, -5151, -1, 12659},
         
         {-79779, 87996, -5151, -1, 12659},
         {-79613, 87996, -5151, -1, 12659},
     },
     {
         /* No Level Limit */
         {-79271, 84330, -5151, -1, 12664},
         {-79448, 84330, -5151, -1, 12664},
         {-79601, 84330, -5151, -1, 12664},
         
         {-80311, 84367, -5151, -1, 12664},
         {-80311, 84196, -5151, -1, 12664},
         {-80311, 84015, -5151, -1, 12664},
         
         {-80556, 85049, -5151, -1, 12669},
         {-80384, 85049, -5151, -1, 12669},
         
         {-79598, 85127, -5151, -1, 12669},
         {-79598, 85303, -5151, -1, 12669},
     }
    };
    
    protected static final int[][][] festivalDawnChestSpawns = 
    {
     {
         /* Level 31 and Below */ 
         {-78999, 112957, -4927, -1, 12764},
         {-79153, 112873, -4927, -1, 12764},
         {-79256, 112873, -4927, -1, 12764},
         {-79368, 112957, -4927, -1, 12764},
         
         {-79481, 113124, -4927, -1, 12764},
         {-79481, 113275, -4927, -1, 12764},
         
         {-79364, 113398, -4927, -1, 12764},
         {-79213, 113500, -4927, -1, 12764},
         {-79099, 113500, -4927, -1, 12764},
         {-78960, 113398, -4927, -1, 12764},
         
         {-78882, 113235, -4927, -1, 12764},
         {-78882, 113099, -4927, -1, 12764},
     },
     {
         /* Level 42 and Below */ 
         {-76119, 110383, -4927, -1, 12765},
         {-75980, 110442, -4927, -1, 12765},
         {-75848, 110442, -4927, -1, 12765},
         {-75720, 110383, -4927, -1, 12765},
         
         {-75625, 110195, -4927, -1, 12765},
         {-75625, 110063, -4927, -1, 12765},
         
         {-75722, 109908, -4927, -1, 12765},
         {-75863, 109832, -4927, -1, 12765},
         {-75989, 109832, -4927, -1, 12765},
         {-76130, 109908, -4927, -1, 12765},
         
         {-76230, 110079, -4927, -1, 12765},
         {-76230, 110215, -4927, -1, 12765},
     },
     {
         /* Level 53 and Below */ 
         {-74055, 111781, -4927, -1, 12766},
         {-74144, 111938, -4927, -1, 12766},
         {-74144, 112075, -4927, -1, 12766},
         {-74055, 112173, -4927, -1, 12766},
         
         {-73885, 112289, -4927, -1, 12766},
         {-73756, 112289, -4927, -1, 12766},
         
         {-73574, 112141, -4927, -1, 12766},
         {-73511, 112040, -4927, -1, 12766},
         {-73511, 111912, -4927, -1, 12766},
         {-73574, 111772, -4927, -1, 12766},
         
         {-73767, 111669, -4927, -1, 12766},
         {-73899, 111669, -4927, -1, 12766},
     },
     {
         /* Level 64 and Below */ 
         {-76008, 113566, -4927, -1, 12767},
         {-76159, 113485, -4927, -1, 12767},
         {-76267, 113485, -4927, -1, 12767},
         {-76386, 113566, -4927, -1, 12767},
         
         {-76482, 113748, -4927, -1, 12767},
         {-76482, 113885, -4927, -1, 12767},
         
         {-76371, 114029, -4927, -1, 12767},
         {-76220, 114118, -4927, -1, 12767},
         {-76092, 114118, -4927, -1, 12767},
         {-75975, 114029, -4927, -1, 12767},
         
         {-75861, 11385-1, -4927, -1, 12767},
         {-75861, 113713, -4927, -1, 12767},
     },
     {
         /* No Level Limit */ 
         {-79100, 109782, -4927, -1, 12768},
         {-78962, 109853, -4927, -1, 12768},
         {-78851, 109853, -4927, -1, 12768},
         {-78721, 109782, -4927, -1, 12768},
         
         {-78615, 109596, -4927, -1, 12768},
         {-78615, 109453, -4927, -1, 12768},
         
         {-78746, 109300, -4927, -1, 12768},
         {-78881, 109203, -4927, -1, 12768},
         {-79027, 109203, -4927, -1, 12768},
         {-79159, 109300, -4927, -1, 12768},
         
         {-79240, 109480, -4927, -1, 12768},
         {-79240, 109615, -4927, -1, 12768},
     }
    };
    
    protected static final int[][][] festivalDuskChestSpawns = 
    {
     {
         /* Level 31 and Below */ 
         {-77016, 88726, -5183, -1, 12769},
         {-77136, 88646, -5183, -1, 12769},
         {-77247, 88646, -5183, -1, 12769},
         {-77380, 88726, -5183, -1, 12769},
         
         {-77512, 88883, -5183, -1, 12769},
         {-77512, 89053, -5183, -1, 12769},
         
         {-77378, 89287, -5183, -1, 12769},
         {-77254, 89238, -5183, -1, 12769},
         {-77095, 89238, -5183, -1, 12769},
         {-76996, 89287, -5183, -1, 12769},
         
         {-76901, 89025, -5183, -1, 12769},
         {-76901, 88891, -5183, -1, 12769},
     },
     {
         /* Level 42 and Below */ 
         {-77128, 85553, -5183, -1, 12770},
         {-77036, 85594, -5183, -1, 12770},
         {-76919, 85594, -5183, -1, 12770},
         {-76755, 85553, -5183, -1, 12770},
         
         {-76635, 85392, -5183, -1, 12770},
         {-76635, 85216, -5183, -1, 12770},
         
         {-76761, 85025, -5183, -1, 12770},
         {-76908, 85004, -5183, -1, 12770},
         {-77041, 85004, -5183, -1, 12770},
         {-77138, 85025, -5183, -1, 12770},
         
         {-77268, 85219, -5183, -1, 12770},
         {-77268, 85410, -5183, -1, 12770},
     },
     {
         /* Level 53 and Below */ 
         {-75150, 87303, -5183, -1, 12771},
         {-75150, 87175, -5183, -1, 12771},
         {-75150, 87175, -5183, -1, 12771},
         {-75150, 87303, -5183, -1, 12771},
         
         {-74943, 87433, -5183, -1, 12771},
         {-74767, 87433, -5183, -1, 12771},
         
         {-74556, 87306, -5183, -1, 12771},
         {-74556, 87184, -5183, -1, 12771},
         {-74556, 87184, -5183, -1, 12771},
         {-74556, 87306, -5183, -1, 12771},
         
         {-74757, 86830, -5183, -1, 12771},
         {-74927, 86830, -5183, -1, 12771},
     },
     {
         /* Level 64 and Below */ 
         {-80010, 88128, -5183, -1, 12772},
         {-80113, 88066, -5183, -1, 12772},
         {-80220, 88066, -5183, -1, 12772},
         {-80359, 88128, -5183, -1, 12772},
         
         {-80467, 88267, -5183, -1, 12772},
         {-80467, 88436, -5183, -1, 12772},
         
         {-80381, 88639, -5183, -1, 12772},
         {-80278, 88577, -5183, -1, 12772},
         {-80142, 88577, -5183, -1, 12772},
         {-80028, 88639, -5183, -1, 12772},
         
         {-79915, 88466, -5183, -1, 12772},
         {-79915, 88322, -5183, -1, 12772},
     },
     {
         /* No Level Limit */ 
         {-80153, 84947, -5183, -1, 12773},
         {-80003, 84962, -5183, -1, 12773},
         {-79848, 84962, -5183, -1, 12773},
         {-79742, 84947, -5183, -1, 12773},
         
         {-79668, 84772, -5183, -1, 12773},
         {-79668, 84619, -5183, -1, 12773},
         
         {-79772, 84471, -5183, -1, 12773},
         {-79888, 84414, -5183, -1, 12773},
         {-80023, 84414, -5183, -1, 12773},
         {-80166, 84471, -5183, -1, 12773},
         
         {-80253, 84600, -5183, -1, 12773},
         {-80253, 84780, -5183, -1, 12773},
     }
    };	
    
    //////////////////////// \\\\\\\\\\\\\\\\\\\\\\\\\\
    
    protected FestivalManager _managerInstance; 
    protected ScheduledFuture<?> _managerScheduledTask;
    
    protected int _signsCycle = SevenSigns.getInstance().getCurrentCycle();
    protected int _festivalCycle;
    protected long _nextFestivalCycleStart;
    protected long _nextFestivalStart;
    protected boolean _festivalInitialized;
    protected boolean _festivalInProgress;
    protected List<Integer> _accumulatedBonuses;   // The total bonus available (in Ancient Adena)
    protected Random rnd = new Random();
    
    private L2NpcInstance _dawnChatGuide; 
    private L2NpcInstance _duskChatGuide;
    
    protected Map<Integer, List<L2PcInstance>> _dawnFestivalParticipants;
    protected Map<Integer, List<L2PcInstance>> _duskFestivalParticipants;
    
    protected Map<Integer, List<L2PcInstance>> _dawnPreviousParticipants;
    protected Map<Integer, List<L2PcInstance>> _duskPreviousParticipants;
        
    private Map<Integer, Integer> _dawnFestivalScores;
    private Map<Integer, Integer> _duskFestivalScores;
    
    /** 
     * _festivalData is essentially an instance of the seven_signs_festival table and 
     * should be treated as such. 
     * 
     * Data is initially accessed by the related Seven Signs cycle, with _signsCycle representing
     * data for the current round of Festivals. 
     * 
     * The actual table data is stored as a series of StatsSet constructs. These are accessed by 
     * the use of an offset based on the number of festivals, thus:
     * 
     *  offset = FESTIVAL_COUNT + festivalId 
     *  (Data for Dawn is always accessed by offset > FESTIVAL_COUNT) 
     */
    private Map<Integer, Map<Integer, StatsSet>> _festivalData;
    
    public SevenSignsFestival() 
    {
        _accumulatedBonuses = new FastList<Integer>();
        
        _dawnFestivalParticipants = new FastMap<Integer, List<L2PcInstance>>();
        _dawnPreviousParticipants = new FastMap<Integer, List<L2PcInstance>>();
        _dawnFestivalScores = new FastMap<Integer, Integer>();
        
        _duskFestivalParticipants = new FastMap<Integer, List<L2PcInstance>>();
        _duskPreviousParticipants = new FastMap<Integer, List<L2PcInstance>>();
        _duskFestivalScores = new FastMap<Integer, Integer>();

        _festivalData = new FastMap<Integer, Map<Integer, StatsSet>>();
        
        restoreFestivalData();
        
        if (SevenSigns.getInstance().isSealValidationPeriod()) 
        { 
	if(Config.DEBUG)
            _log.info("SevenSignsFestival: Initialization bypassed due to Seal Validation in effect.");
            return; 
        }
        
        L2Spawn.addSpawnListener(this);
        startFestivalManager();
    }
    
    public static SevenSignsFestival getInstance() 
    {
        if (_instance == null) 
            _instance = new SevenSignsFestival();

        return _instance;
    }
    
    /**
     * Returns the associated name (level range) to a given festival ID.
     * 
     * @param int festivalID
     * @return String festivalName
     */
    public static final String getFestivalName(int festivalID)
    {
        String festivalName;
        
        switch (festivalID) 
        {
            case FESTIVAL_LEVEL_MAX_31:
                festivalName = "Level 31 or lower";
                break;
            case FESTIVAL_LEVEL_MAX_42:
                festivalName = "Level 42 or lower";
                break;
            case FESTIVAL_LEVEL_MAX_53:
                festivalName = "Level 53 or lower";
                break;
            case FESTIVAL_LEVEL_MAX_64:
                festivalName = "Level 64 or lower";
                break;
            default:
                festivalName = "No Level Limit";
            break;
        }
        
        return festivalName;
    }
    
    /**
     * Returns the maximum allowed player level for the given festival type.
     * 
     * @param festivalId
     * @return int maxLevel
     */
    public static final int getMaxLevelForFestival(int festivalId)
    {
        int maxLevel = 78;
        
        switch (festivalId) 
        {
            case SevenSignsFestival.FESTIVAL_LEVEL_MAX_31:
                maxLevel = 31;    
                break;
            case SevenSignsFestival.FESTIVAL_LEVEL_MAX_42:
                maxLevel = 42;
                break;
            case SevenSignsFestival.FESTIVAL_LEVEL_MAX_53:
                maxLevel = 53;
                break;
            case SevenSignsFestival.FESTIVAL_LEVEL_MAX_64:
                maxLevel = 64;
                break;
        }
        
        return maxLevel;
    }
    
    /**
     * Returns true if the monster ID given is of an archer/marksman type. 
     * 
     * @param npcId
     * @return boolean isArcher
     */
    protected static final boolean isFestivalArcher(int npcId)
    {
        if (npcId < 12622 || npcId > 12721)
            return false;
        
        String npcIdStr = String.valueOf(npcId);
        return (npcIdStr.substring(4).equals("4") || npcIdStr.substring(4).equals("9"));
    }
    
    /**
     * Returns true if the monster ID given is a festival chest.
     * 
     * @param npcId
     * @return boolean isChest
     */
    protected static final boolean isFestivalChest(int npcId)
    {
        return (npcId < 12764 || npcId > 12773);
    }
    
    /**
     * Primarily used to terminate the Festival Manager, when the Seven Signs period changes.
     * 
     * @return ScheduledFuture festManagerScheduler
     */
    protected final ScheduledFuture<?> getFestivalManagerSchedule()
    {
        if (_managerScheduledTask == null)
            startFestivalManager();
        
        return _managerScheduledTask;
    }
    
    /**
     * Used to start the Festival Manager, if the current period is not Seal Validation.
     */
    protected void startFestivalManager()
    {
        // Start the Festival Manager for the first time after the server has started
        // at the specified time, then invoke it automatically after every cycle.
        FestivalManager fm = new FestivalManager(); 
        setNextFestivalStart(Config.ALT_FESTIVAL_MANAGER_START + FESTIVAL_SIGNUP_TIME);
        _managerScheduledTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(fm, Config.ALT_FESTIVAL_MANAGER_START, Config.ALT_FESTIVAL_CYCLE_LENGTH);
        if(Config.DEBUG)
        _log.info("SevenSignsFestival: The first Festival of Darkness cycle begins in " + (Config.ALT_FESTIVAL_MANAGER_START / 60000) + " minute(s).");
    }
    
    /**
     * Restores saved festival data, basic settings from the properties file 
     * and past high score data from the database.
     * 
     * @throws Exception
     */
    protected void restoreFestivalData()
    { 
        Connection con = null;
        PreparedStatement statement = null;
        ResultSet rset = null;
        
        if (Config.DEBUG)
            _log.info("SevenSignsFestival: Restoring festival data. Current SS Cycle: " + _signsCycle);

        try
        {
	        con = L2DatabaseFactory.getInstance().getConnection();
	        statement = con.prepareStatement("SELECT festivalId, cabal, cycle, date, score, members " +
	            "FROM seven_signs_festival");
	        rset = statement.executeQuery();
	            
	        while (rset.next())
	        {
	            int festivalCycle = rset.getInt("cycle");
	            int festivalId = rset.getInt("festivalId");
	            String cabal = rset.getString("cabal");
	            
	            StatsSet festivalDat = new StatsSet(); 
	            festivalDat.set("festivalId", festivalId);
	            festivalDat.set("cabal", cabal);
	            festivalDat.set("cycle", festivalCycle);
	            festivalDat.set("date", rset.getString("date"));
	            festivalDat.set("score", rset.getInt("score"));
	            festivalDat.set("members", rset.getString("members"));
	
	            if (Config.DEBUG)
	                _log.info("SevenSignsFestival: Loaded data from DB for (Cycle = " + festivalCycle + ", Oracle = " + cabal + ", Festival = "+ getFestivalName(festivalId));
	            
	            if (cabal.equals("dawn"))
	                festivalId += FESTIVAL_COUNT;
	                
	            Map<Integer, StatsSet> tempData = _festivalData.get(festivalCycle);
	            
	            if (tempData == null)
	                tempData = new FastMap<Integer, StatsSet>();
	            
	            tempData.put(festivalId, festivalDat);
	            _festivalData.put(festivalCycle, tempData);
	        }
	        
	        rset.close();
	        statement.close();

	        String query = "SELECT festival_cycle, ";
            
	        for (int i = 0; i < FESTIVAL_COUNT-1; i++)
                
	            query += "accumulated_bonus" + String.valueOf(i) + ", ";
	        query += "accumulated_bonus" + String.valueOf(FESTIVAL_COUNT -1) + " ";
	        query += "FROM seven_signs_status WHERE id=0";
            
	        statement = con.prepareStatement(query);
	        rset = statement.executeQuery();
	        
	        while (rset.next())
	        {
	            _festivalCycle = rset.getInt("festival_cycle");
                
	            for(int i = 0; i < FESTIVAL_COUNT; i++)
	                _accumulatedBonuses.add(i, rset.getInt("accumulated_bonus" + String.valueOf(i)));
	        }
	        
	        rset.close();
	        statement.close();
	        con.close();
	        
	        if (Config.DEBUG)
	            _log.info("SevenSignsFestival: Loaded data from database.");
        }
        catch (SQLException e)
        {
            _log.severe("SevenSignsFestival: Failed to load configuration: " + e);        	
        }
        finally
        {
        	try
        	{
        	    rset.close();
        	    statement.close();
        	    con.close();
        	}
        	catch (SQLException e) {}
        }
     }
    
    /**
     * Stores current festival data, basic settings to the properties file 
     * and past high score data to the database.
     * 
     * If updateSettings = true, then all Seven Signs data is updated in the database.
     * 
     * @param updateSettings
     * @throws Exception
     */
    public void saveFestivalData(boolean updateSettings)
    {
    	Connection con = null;
    	PreparedStatement statement = null;
    	
    	if (Config.DEBUG)
    		_log.info("SevenSignsFestival: Saving festival data to disk.");

        try
        {
        	con = L2DatabaseFactory.getInstance().getConnection();
            
        	for (Map<Integer, StatsSet> currCycleData : _festivalData.values())
	        {
	            for (StatsSet festivalDat : currCycleData.values())
	            {
	                int festivalCycle = festivalDat.getInteger("cycle");
	                int festivalId = festivalDat.getInteger("festivalId");
	                String cabal = festivalDat.getString("cabal");
	                
	                // Try to update an existing record.
	                statement = con.prepareStatement(
	                    "UPDATE seven_signs_festival SET date=?, score=?, members=? WHERE cycle=? AND cabal=? AND festivalId=?");
	                statement.setLong(1, Long.valueOf(festivalDat.getString("date")));
	                statement.setInt(2, festivalDat.getInteger("score"));
	                statement.setString(3, festivalDat.getString("members"));
	                statement.setInt(4, festivalCycle);
	                statement.setString(5, cabal);
	                statement.setInt(6, festivalId);
	                
	                // If there was no record to update, assume it doesn't exist and add a new one,
	                // otherwise continue with the next record to store.
	                if (statement.executeUpdate() > 0) 
	                {
	                    if (Config.DEBUG)
	                        _log.info("SevenSignsFestival: Updated data in DB (Cycle = " + festivalCycle + ", Cabal = " + cabal + ", FestID = " + festivalId + ")");
	
		                statement.close();
	                    continue;
	                }

	                statement.close();

	            	statement = con.prepareStatement(
	                    "INSERT INTO seven_signs_festival (festivalId, cabal, cycle, date, score, members) VALUES (?,?,?,?,?,?)");
	                statement.setInt(1, festivalId);
	                statement.setString(2, cabal);                
	                statement.setInt(3, festivalCycle);
	                statement.setLong(4, Long.valueOf(festivalDat.getString("date")));
	                statement.setInt(5, festivalDat.getInteger("score"));
	                statement.setString(6, festivalDat.getString("members"));
	                statement.execute();
	                statement.close();
	                
	                if (Config.DEBUG)
	                    _log.info("SevenSignsFestival: Inserted data in DB (Cycle = " + festivalCycle + ", Cabal = " + cabal + ", FestID = " + festivalId + ")");
	            }
	        }
        	
        	con.close();
	            
	        // Updates Seven Signs DB data also, so call only if really necessary.
	        if (updateSettings)
	            SevenSigns.getInstance().saveSevenSignsData(null, true);
        }
        catch (SQLException e)
        {
        	_log.severe("SevenSignsFestival: Failed to save configuration: " + e);
        }
        finally
        {
        	try
        	{
        	    statement.close();
        	    con.close();
        	}
        	catch (Exception e) {}
        }
    }
    
    /**
     * Used to reset all festival data at the beginning of a new quest event period.
     */
    protected void resetFestivalData(boolean updateSettings) 
    {
        _festivalCycle = 0;
        _signsCycle = SevenSigns.getInstance().getCurrentCycle();
        
        // Set all accumulated bonuses back to 0.
        for (int i = 0; i < FESTIVAL_COUNT; i++) 
            _accumulatedBonuses.set(i, 0);
        
        _dawnFestivalParticipants.clear();
        _dawnPreviousParticipants.clear();
        _dawnFestivalScores.clear();
        
        _duskFestivalParticipants.clear();
        _duskPreviousParticipants.clear();
        _duskFestivalScores.clear();
        
        // Set up a new data set for the current cycle of festivals
        Map<Integer, StatsSet> newData = new FastMap<Integer, StatsSet>();
        
        for (int i = 0; i < FESTIVAL_COUNT * 2; i++)
        {
            int festivalId = i;
            
            if (i >= FESTIVAL_COUNT)
                festivalId -= FESTIVAL_COUNT;
                
            // Create a new StatsSet with "default" data for Dusk
            StatsSet tempStats = new StatsSet();
            tempStats.set("festivalId", festivalId);
            tempStats.set("cycle", _signsCycle);
            tempStats.set("date", "0");
            tempStats.set("score", 0);
            tempStats.set("members", "");
            
            if (i >= FESTIVAL_COUNT)
                tempStats.set("cabal", SevenSigns.getCabalShortName(SevenSigns.CABAL_DAWN));
            else
                tempStats.set("cabal", SevenSigns.getCabalShortName(SevenSigns.CABAL_DUSK));
            
            newData.put(i, tempStats);
        }
        
        // Add the newly created cycle data to the existing festival data, and
        // subsequently save it to the database.
        _festivalData.put(_signsCycle, newData);
        
        saveFestivalData(updateSettings);
        
        // Remove any unused blood offerings from online players.
        for (L2PcInstance onlinePlayer : L2World.getInstance().getAllPlayers())
        {
            L2ItemInstance bloodOfferings = onlinePlayer.getInventory().getItemByItemId(FESTIVAL_OFFERING_ID);
            
            if (bloodOfferings != null) 
                onlinePlayer.destroyItem("SevenSigns", bloodOfferings, null, false);
        }
        if(Config.DEBUG)
        _log.info("SevenSignsFestival: Reinitialized engine for next competition period.");
    }
   

    public final int getCurrentFestivalCycle() 
    {
        return _festivalCycle;
    }
    
    public final boolean isFestivalInitialized() 
    {
        return _festivalInitialized;
    }
    
    public final boolean isFestivalInProgress() 
    {
        return _festivalInProgress;
    }
    
    public void setNextCycleStart() 
    {
        _nextFestivalCycleStart = System.currentTimeMillis() + Config.ALT_FESTIVAL_CYCLE_LENGTH;
    }
    
    public void setNextFestivalStart(long milliFromNow) 
    {
        _nextFestivalStart = System.currentTimeMillis() + milliFromNow;
    }
    
    public final int getMinsToNextCycle() 
    {
        if (SevenSigns.getInstance().isSealValidationPeriod()) 
            return -1;
        
        return Math.round((_nextFestivalCycleStart - System.currentTimeMillis()) / 60000);
    }
    
    public final int getMinsToNextFestival() 
    {
        if (SevenSigns.getInstance().isSealValidationPeriod()) 
            return -1;
        
        return Math.round((_nextFestivalStart - System.currentTimeMillis()) / 60000) + 1;
    }
    
    public final String getTimeToNextFestivalStr() 
    {
        if (SevenSigns.getInstance().isSealValidationPeriod()) 
            return "<font color=\"FF0000\">This is the Seal Validation period. Festivals will resume next week.</font>";
        
        return "<font color=\"FF0000\">The next festival will begin in " + getMinsToNextFestival() + " minute(s).</font>";            
    }
    
    /**
     * Returns the current festival ID and oracle ID that the specified player is in,
     * but will return the default of {-1, -1} if the player is not found as a participant.
     * 
     * @param player
     * @return int[] playerFestivalInfo
     */
    public final int[] getFestivalForPlayer(L2PcInstance player)
    {
        int[] playerFestivalInfo = {-1, -1};
        int festivalId = 0;
        
        while (festivalId < FESTIVAL_COUNT)
        {
            List<L2PcInstance> participants = _dawnFestivalParticipants.get(festivalId);
            
            // If there are no participants in this festival, move on to the next.
            if (participants != null && participants.contains(player)) 
            {
                playerFestivalInfo[0] = SevenSigns.CABAL_DAWN;
                playerFestivalInfo[1] = festivalId;
                
                return playerFestivalInfo;
            }
            
            festivalId++;
            
            participants = _duskFestivalParticipants.get(festivalId);
            
            if (participants != null && participants.contains(player)) 
            {
                playerFestivalInfo[0] = SevenSigns.CABAL_DUSK;
                playerFestivalInfo[1] = festivalId;
                
                return playerFestivalInfo;
            }
            
            festivalId++;
        }

        // Return default data if the player is not found as a participant.
        return playerFestivalInfo;
    }
    
    public final boolean isParticipant(L2PcInstance player)
    {
        if (SevenSigns.getInstance().isSealValidationPeriod())
            return false;
        
        if (_managerInstance == null)
            return false;
       
        for (List<L2PcInstance> participants : _dawnFestivalParticipants.values())
            if (participants.contains(player))
                return true;
        
        for (List<L2PcInstance> participants : _duskFestivalParticipants.values())
            if (participants.contains(player))
                return true;
        
        return false;
    }
    
    public final List<L2PcInstance> getParticipants(int oracle, int festivalId) 
    {
        if (oracle == SevenSigns.CABAL_DAWN)
            return _dawnFestivalParticipants.get(festivalId);
        
        return _duskFestivalParticipants.get(festivalId);
    }
    
    public final List<L2PcInstance> getPreviousParticipants(int oracle, int festivalId) 
    {
        if (oracle == SevenSigns.CABAL_DAWN)
            return _dawnPreviousParticipants.get(festivalId);
        
        return _duskPreviousParticipants.get(festivalId);
    }
    
    public void setParticipants(int oracle, int festivalId, L2Party festivalParty) 
    {
        List<L2PcInstance> participants = new FastList<L2PcInstance>();
        
        if (festivalParty != null) 
        {
            participants = festivalParty.getPartyMembers();
        
            if (Config.DEBUG)
                _log.info("SevenSignsFestival: " + festivalParty.getPartyMembers().toString() + 
                    " have signed up to the " + SevenSigns.getCabalShortName(oracle) + " " + getFestivalName(festivalId) + " festival.");
        }
        
        if (oracle == SevenSigns.CABAL_DAWN)
            _dawnFestivalParticipants.put(festivalId, participants);
        else
            _duskFestivalParticipants.put(festivalId, participants);
    }
    
    public void updateParticipants(L2PcInstance player, L2Party festivalParty)
    {
        if (!isParticipant(player))
            return;
        
        final int[] playerFestInfo = getFestivalForPlayer(player);
        final int oracle = playerFestInfo[0];
        final int festivalId = playerFestInfo[1];
        
        if (festivalId > -1) 
        {
            if (_festivalInitialized) 
            {
                L2DarknessFestival festivalInst = _managerInstance.getFestivalInstance(oracle, festivalId);

                if (festivalParty == null)
                    for (L2PcInstance partyMember : getParticipants(oracle, festivalId))
                        festivalInst.relocatePlayer(partyMember, true);
                else
                    festivalInst.relocatePlayer(player, true);
            }
            
            setParticipants(oracle, festivalId, festivalParty);
        }
    }
    
    public final int getFinalScore(int oracle, int festivalId)
    {
        if (oracle == SevenSigns.CABAL_DAWN)
            return _dawnFestivalScores.get(festivalId);
        
        return _duskFestivalScores.get(festivalId);
    }
    
    public final int getHighestScore(int oracle, int festivalId)
    {
        return getHighestScoreData(oracle, festivalId).getInteger("score");
    }
    
    /**
     * Returns a stats set containing the highest score <b>this cycle</b> for the 
     * the specified cabal and associated festival ID.
     * 
     * @param oracle
     * @param festivalId
     * @return StatsSet festivalDat
     */
    public final StatsSet getHighestScoreData(int oracle, int festivalId)
    {
        int offsetId = festivalId;
        
        if (oracle == SevenSigns.CABAL_DAWN)
            offsetId += 5;
        
        // Attempt to retrieve existing score data (if found), otherwise create a 
        // new blank data set and display a console warning.
        StatsSet currData = null;
        
        try 
        {
            currData = _festivalData.get(_signsCycle).get(offsetId);
        }
        catch (Exception e) 
        {
            currData = new StatsSet();
            currData.set("score", 0);
            currData.set("members", "");
            
            if (Config.DEBUG)
            	_log.info("SevenSignsFestival: Data missing for " + SevenSigns.getCabalName(oracle) + ", FestivalID = " + festivalId + " (Current Cycle " + _signsCycle + ")");
        }
        
        return currData;
    }
    
    /**
     * Returns a stats set containing the highest ever recorded 
     * score data for the specified festival.
     * 
     * @param festivalId
     * @return StatsSet result
     */
    public final StatsSet getOverallHighestScoreData(int festivalId)
    {
        StatsSet result = null; 
        int highestScore = 0;
        
        for (Map<Integer, StatsSet> currCycleData : _festivalData.values())
        {
            for (StatsSet currFestData : currCycleData.values()) 
            {
                int currFestID = currFestData.getInteger("festivalId");
                int festivalScore = currFestData.getInteger("score");

                if (currFestID != festivalId) 
                    continue;
                
                if (festivalScore > highestScore) 
                {
                    highestScore = festivalScore;
                    result = currFestData;
                }
            }
        }

        return result;
    }
    
    /**
     * Set the final score details for the last participants of the specified festival data.
     * Returns <b>true</b> if the score is higher than that previously recorded <b>this cycle</b>.
     * 
     * @param player
     * @param oracle
     * @param festivalId
     * @param offeringScore
     * @return boolean isHighestScore
     */
    public boolean setFinalScore(L2PcInstance player, int oracle, int festivalId, int offeringScore)
    {
        List<String> partyMembers;
        
        int currDawnHighScore = getHighestScore(SevenSigns.CABAL_DAWN, festivalId);
        int currDuskHighScore = getHighestScore(SevenSigns.CABAL_DUSK, festivalId);
        
        int thisCabalHighScore = 0;
        int otherCabalHighScore = 0;
        
        if (oracle == SevenSigns.CABAL_DAWN) 
        {
            thisCabalHighScore = currDawnHighScore;
            otherCabalHighScore = currDuskHighScore;
            
            _dawnFestivalScores.put(festivalId, offeringScore);
        }
        else 
        {
            thisCabalHighScore = currDuskHighScore;
            otherCabalHighScore = currDawnHighScore;
            
            _duskFestivalScores.put(festivalId, offeringScore);
        }            
        
        StatsSet currFestData = getHighestScoreData(oracle, festivalId);
        
        // Check if this is the highest score for this level range so far for the player's cabal.
        if (offeringScore > thisCabalHighScore)
        {
            // If the current score is greater than that for the other cabal, 
            // then they already have the points from this festival.
            if (thisCabalHighScore > otherCabalHighScore)
                return false;
            
            partyMembers = new FastList<String>();
            List<L2PcInstance> prevParticipants = getPreviousParticipants(oracle, festivalId);
            
            // Record a string list of the party members involved.
            for (L2PcInstance partyMember : prevParticipants) 
                partyMembers.add(partyMember.getName());
            
            // Update the highest scores and party list.
            currFestData.set("date", String.valueOf(System.currentTimeMillis()));
            currFestData.set("score", offeringScore);
            currFestData.set("members", Util.implodeString(partyMembers, ","));
            
            if (Config.DEBUG)
                _log.info("SevenSignsFestival: " + player.getName() + "'s party has the highest score (" + 
                    offeringScore + ") so far for " + SevenSigns.getCabalName(oracle) + " in " + getFestivalName(festivalId));            
            
            // Only add the score to the cabal's overall if it's higher than the other cabal's score.
            if (offeringScore > otherCabalHighScore) 
            {
                int contribPoints = FESTIVAL_LEVEL_SCORES[festivalId];
                
                // Give this cabal the festival points, while deducting them from the other.
                SevenSigns.getInstance().addFestivalScore(oracle, contribPoints);
                
                //if (Config.DEBUG)
                    _log.info("SevenSignsFestival: This is the highest score overall so far for the " + getFestivalName(festivalId) + " festival!");
            }
            
            saveFestivalData(true);
            
            return true;
        }
        
        return false;
    }
    
    public final int getAccumulatedBonus(int festivalId)
    {
        return _accumulatedBonuses.get(festivalId);
    }
    
    public final int getTotalAccumulatedBonus()
    {
        int totalAccumBonus = 0;
        
        for (int accumBonus : _accumulatedBonuses)
            totalAccumBonus += accumBonus;
        
        return totalAccumBonus; 
    }
    
    public void addAccumulatedBonus(int festivalId, int stoneType, int stoneAmount)
    {
        int eachStoneBonus = 0;
        
        switch (stoneType) 
        {
            case SevenSigns.SEAL_STONE_BLUE_ID:
                eachStoneBonus = SevenSigns.SEAL_STONE_BLUE_VALUE;
                break;
            case SevenSigns.SEAL_STONE_GREEN_ID:
                eachStoneBonus = SevenSigns.SEAL_STONE_GREEN_VALUE;
                break;
            case SevenSigns.SEAL_STONE_RED_ID:
                eachStoneBonus = SevenSigns.SEAL_STONE_RED_VALUE;
                break;
        }
        
        int newTotalBonus = _accumulatedBonuses.get(festivalId) + (stoneAmount * eachStoneBonus);
        _accumulatedBonuses.set(festivalId, newTotalBonus);
    }
    
    /**
     * Calculate and return the proportion of the accumulated bonus for the festival 
     * where the player was in the winning party, if the winning party's cabal won the event.
     * 
     * The accumulated bonus is then updated, with the player's share deducted.
     * 
     * @param player
     * @return playerBonus (the share of the bonus for the party)
     */
    public final int distribAccumulatedBonus(L2PcInstance player)
    {
        int playerBonus = 0;
        String playerName = player.getName();
        int playerCabal = SevenSigns.getInstance().getPlayerCabal(player);
        
        if (playerCabal != SevenSigns.getInstance().getCabalHighestScore())
            return 0;
        
        if (_festivalData.get(_signsCycle) != null)
            for (StatsSet festivalData : _festivalData.get(_signsCycle).values())
            {
                if (festivalData.getString("members").indexOf(playerName) > -1)
                {
                    int festivalId = festivalData.getInteger("festivalId");
                    int numPartyMembers = festivalData.getString("members").split(",").length;
                    int totalAccumBonus = _accumulatedBonuses.get(festivalId);
                    
                    playerBonus = totalAccumBonus / numPartyMembers;
                    _accumulatedBonuses.set(festivalId, totalAccumBonus - playerBonus);
                    break;
                } 
            }
        
        return playerBonus;
    }
    
    /**
     * Used to send a "shout" message to all players currently present in an Oracle.
     * Primarily used for Festival Guide and Witch related speech.
     * 
     * @param senderName
     * @param message
     */
    public void sendMessageToAll(String senderName, String message)
    {
        if (_dawnChatGuide == null || _duskChatGuide == null) 
            return;

        CreatureSay cs = new CreatureSay(_dawnChatGuide.getObjectId(), 1, senderName, message);
        _dawnChatGuide.broadcastPacket(cs);
        
        cs = new CreatureSay(_duskChatGuide.getObjectId(), 1, senderName, message);
        _duskChatGuide.broadcastPacket(cs);
    }
    
    /**
     * Basically a wrapper-call to signal to increase the challenge of the specified festival.
     * 
     * @param oracle
     * @param festivalId
     * @return boolean isChalIncreased
     */
    public final boolean increaseChallenge(int oracle, int festivalId)
    {
        L2DarknessFestival festivalInst = _managerInstance.getFestivalInstance(oracle, festivalId);
        
        return festivalInst.increaseChallenge();
    }
    
    /**
     * Used with the SpawnListener, to update the required "chat guide" instances,
     * for use with announcements in the oracles.
     * 
     * @param npc
     */
    public void npcSpawned(L2NpcInstance npc)
    {
        if (npc == null) 
            return;
            
        int npcId = npc.getNpcId();
        
        // If the spawned NPC ID matches the ones we need, assign their instances.
        if (npcId == 8127) 
        {
            if (Config.DEBUG)
                _log.config("SevenSignsFestival: Instance found for NPC ID 8127 (" + npc.getObjectId() + ").");
            
            _dawnChatGuide = npc;
        }
        
        if (npcId == 8137) 
        {
            if (Config.DEBUG)
                _log.config("SevenSignsFestival: Instance found for NPC ID 8137 (" + npc.getObjectId() + ").");
            
            _duskChatGuide = npc; 
        }
    }
    
    
    /**
     * The FestivalManager class is the main runner of all the festivals.
     * It is used for easier integration and management of all running festivals.
     * 
     * @author Tempy
     */
    private class FestivalManager implements Runnable 
    {
        protected Map<Integer, L2DarknessFestival> _festivalInstances;
        
        public FestivalManager()
        {
            _festivalInstances = new FastMap<Integer, L2DarknessFestival>();
            _managerInstance = this;
            
            // Increment the cycle counter.
            _festivalCycle++;
            
            // Set the next start timers.
            setNextCycleStart();
            setNextFestivalStart(Config.ALT_FESTIVAL_CYCLE_LENGTH - FESTIVAL_SIGNUP_TIME);
        }
        
        public synchronized void run()
        {
            // The manager shouldn't be running if Seal Validation is in effect.
            if (SevenSigns.getInstance().isSealValidationPeriod())
                return;
            
            // If the next period is due to start before the end of this 
            // festival cycle, then don't run it.
            if (SevenSigns.getInstance().getMilliToPeriodChange() < Config.ALT_FESTIVAL_CYCLE_LENGTH)
                return;
            
            if (Config.DEBUG)
                _log.info("SevenSignsFestival: Festival manager initialized. Those wishing to participate have " + getMinsToNextFestival() + " minute(s) to sign up.");
            
            sendMessageToAll("Festival Guide", "The main event will start in " + getMinsToNextFestival() + " minutes. Please register now.");
            
            // Stand by until the allowed signup period has elapsed.
            try {
                wait(FESTIVAL_SIGNUP_TIME); 
            } 
            catch (InterruptedException e) { }
            
            // Clear past participants, they can no longer register their score if not done so already.
            _dawnPreviousParticipants.clear();
            _duskPreviousParticipants.clear();
            
            // Get rid of random monsters that avoided deletion after last festival
            for (L2DarknessFestival festivalInst : _festivalInstances.values()) 
            	festivalInst.unspawnMobs();
            
            /* INITIATION */
            // Set the festival timer to 0, as it is just beginning.
            long elapsedTime = 0;
            
            // Create the instances for the festivals in both Oracles,
            // but only if they have participants signed up for them.
            for (int i = 0; i < FESTIVAL_COUNT; i++) 
            {
                if (_duskFestivalParticipants.get(i) != null)
                    _festivalInstances.put(10 + i, new L2DarknessFestival(SevenSigns.CABAL_DUSK, i));
                
                if (_dawnFestivalParticipants.get(i) != null)
                    _festivalInstances.put(20 + i, new L2DarknessFestival(SevenSigns.CABAL_DAWN, i));
            }
            
            // Prevent future signups while festival is in progress.
            _festivalInitialized = true;
            
            setNextFestivalStart(Config.ALT_FESTIVAL_CYCLE_LENGTH);
            sendMessageToAll("Festival Guide", "The main event is now starting.");
            
            if (Config.DEBUG)
                _log.info("SevenSignsFestival: The current set of festivals will begin in " + (Config.ALT_FESTIVAL_FIRST_SPAWN / 1000 / 60) + " minute(s).");
            
            // Stand by for a short length of time before starting the festival.
            try {
                wait(Config.ALT_FESTIVAL_FIRST_SPAWN); 
            } 
            catch (InterruptedException e) { }
            
            elapsedTime = Config.ALT_FESTIVAL_FIRST_SPAWN;
            
            // Participants can now opt to increase the challenge, if desired.
            _festivalInProgress = true;
            
            
            /* PROPOGATION */
            // Sequentially set all festivals to begin, spawn the Festival Witch and notify participants.
            for (L2DarknessFestival festivalInst : _festivalInstances.values())
            {
                festivalInst.festivalStart();
                festivalInst.sendMessageToParticipants("The festival is about to begin!");
            }
            
            if (Config.DEBUG)
            	_log.info("SevenSignsFestival: Each of the festivals will end in " + (Config.ALT_FESTIVAL_LENGTH / 60000) + " minutes. New participants can signup then.");
            
            
            // After a short time period, move all idle spawns to the center of the arena.
            try {
                wait(Config.ALT_FESTIVAL_FIRST_SWARM - Config.ALT_FESTIVAL_FIRST_SPAWN); 
            } 
            catch (InterruptedException e) { }
            
            elapsedTime += Config.ALT_FESTIVAL_FIRST_SWARM - Config.ALT_FESTIVAL_FIRST_SPAWN;
            
            for (L2DarknessFestival festivalInst : _festivalInstances.values()) 
                festivalInst.moveMonstersToCenter();
            
            
            // Stand by until the time comes for the second spawn.
            try {
                wait(Config.ALT_FESTIVAL_SECOND_SPAWN - Config.ALT_FESTIVAL_FIRST_SWARM); 
            } 
            catch (InterruptedException e) { }
            
            // Spawn an extra set of monsters (archers) on the free platforms with 
            // a faster respawn when killed.
            for (L2DarknessFestival festivalInst : _festivalInstances.values())
            {
                festivalInst.spawnFestivalMonsters(FESTIVAL_DEFAULT_RESPAWN / 2, 2);
                festivalInst.sendMessageToParticipants("The festival will end in " + ((Config.ALT_FESTIVAL_LENGTH - Config.ALT_FESTIVAL_SECOND_SPAWN) / 60000) + " minute(s).");
            }
            
            elapsedTime += Config.ALT_FESTIVAL_SECOND_SPAWN - Config.ALT_FESTIVAL_FIRST_SWARM;
            
            
            // After another short time period, again move all idle spawns to the center of the arena.
            try {
                wait(Config.ALT_FESTIVAL_SECOND_SWARM - Config.ALT_FESTIVAL_SECOND_SPAWN); 
            } 
            catch (InterruptedException e) { }
            
            for (L2DarknessFestival festivalInst : _festivalInstances.values()) 
                festivalInst.moveMonstersToCenter();
            
            elapsedTime += Config.ALT_FESTIVAL_SECOND_SWARM - Config.ALT_FESTIVAL_SECOND_SPAWN;
            
            
            // Stand by until the time comes for the chests to be spawned.
            try {
                wait(Config.ALT_FESTIVAL_CHEST_SPAWN - Config.ALT_FESTIVAL_SECOND_SWARM); 
            } 
            catch (InterruptedException e) { }
            
            // Spawn the festival chests, which enable the team to gain greater rewards
            // for each chest they kill.
            for (L2DarknessFestival festivalInst : _festivalInstances.values())
            {
                festivalInst.spawnFestivalMonsters(FESTIVAL_DEFAULT_RESPAWN, 3);
                festivalInst.sendMessageToParticipants("The chests have spawned! Be quick, the festival will end soon.");
            }
            
            elapsedTime += Config.ALT_FESTIVAL_CHEST_SPAWN - Config.ALT_FESTIVAL_SECOND_SWARM;
            
            // Stand by and wait until it's time to end the festival.
            try {
                wait(Config.ALT_FESTIVAL_LENGTH - elapsedTime); 
            } 
            catch (InterruptedException e) { }
            
            // Participants can no longer opt to increase the challenge, as the festival will soon close.
            _festivalInProgress = false;
            
            
            /* TERMINATION */
            // Sequentially begin the ending sequence for all running festivals.
            for (L2DarknessFestival festivalInst : _festivalInstances.values()) 
                festivalInst.festivalEnd();
            
            // Clear the participants list for the next round of signups.
            _dawnFestivalParticipants.clear();
            _duskFestivalParticipants.clear();
            
            // Allow signups for the next festival cycle.
            _festivalInitialized = false;
            
            sendMessageToAll("Festival Witch", "That will do! I'll move you to the outside soon.");
            
            if (Config.DEBUG)
                _log.info("SevenSignsFestival: The next set of festivals begin in " + getMinsToNextFestival() + " minute(s).");
        }
        
        /**
         * Returns the running instance of a festival for the given Oracle and festivalID.
         * <BR>
         * A <B>null</B> value is returned if there are no participants in that festival.
         * 
         * @param oracle
         * @param festivalId
         * @return L2DarknessFestival festivalInst
         */
        public final L2DarknessFestival getFestivalInstance(int oracle, int festivalId)
        {
            if (!isFestivalInitialized())
                return null;
            
            /* Compute the offset if a Dusk instance is required.
             * 
             * ID:      0   1   2   3   4
             * Dusk 1:  10  11  12  13  14
             * Dawn 2:  20  21  22  23  24
             */
            
            festivalId += (oracle == SevenSigns.CABAL_DUSK) ? 10 : 20;
            return _festivalInstances.get(festivalId);            
        }
        
        /**
         * Returns the number of currently running festivals <b>WITH</b> participants.
         * 
         * @return int Count
         */
        public final int getInstanceCount()
        {
            return _festivalInstances.size();
        }
    }
    
    
    /**
     * Each running festival is represented by an L2DarknessFestival class.
     * It contains all the spawn information and data for the running festival.
     * 
     * All festivals are managed by the FestivalManager class, which must be initialized first.
     * 
     * @author Tempy
     */
    private class L2DarknessFestival
    {
        protected final int _cabal;
        protected final int _levelRange;
        protected boolean _challengeIncreased;
        
        private FestivalSpawn _startLocation;
        private FestivalSpawn _witchSpawn;
        
        private L2NpcInstance _witchInst;
        private List<L2FestivalMonsterInstance> _npcInsts;
        
        private List<L2PcInstance> _participants;
        private Map<L2PcInstance, FestivalSpawn> _originalLocations;
        
        protected L2DarknessFestival(int cabal, int levelRange) 
        {
            _cabal = cabal;
            _levelRange = levelRange;
            _originalLocations = new FastMap<L2PcInstance, FestivalSpawn>();
            _npcInsts = new FastList<L2FestivalMonsterInstance>();
            
            if (cabal == SevenSigns.CABAL_DAWN)
            {
                _participants = _dawnFestivalParticipants.get(levelRange);
                _witchSpawn = new FestivalSpawn(festivalDawnWitchSpawns[levelRange]);
                _startLocation = new FestivalSpawn(festivalDawnPlayerSpawns[levelRange]);
            }
            else
            {
                _participants = _duskFestivalParticipants.get(levelRange);
                _witchSpawn = new FestivalSpawn(festivalDuskWitchSpawns[levelRange]);
                _startLocation = new FestivalSpawn(festivalDuskPlayerSpawns[levelRange]);
            }
            
            // FOR TESTING!
            if (_participants == null)
                _participants = new FastList<L2PcInstance>();
            
            festivalInit();
        }
        
        protected void festivalInit()
        {
            boolean isPositive;
            
            if (Config.DEBUG)
                _log.info("SevenSignsFestival: Initializing festival for " + SevenSigns.getCabalShortName(_cabal) + " (" + getFestivalName(_levelRange) + ")");
            
            // Teleport all players to arena and notify them.
            if (_participants.size() > 0) 
            {
                for (L2PcInstance participant : _participants)
                { 
                    _originalLocations.put(participant, new FestivalSpawn(participant.getX(), participant.getY(), participant.getZ(), participant.getHeading()));
                    
                    // Randomize the spawn point around the specific centerpoint for each player.
                    int x = _startLocation._x;
                    int y = _startLocation._y;
                    
                    isPositive = (rnd.nextInt(2) == 1);
                    
                    if (isPositive) 
                    {
                        x += rnd.nextInt(FESTIVAL_MAX_OFFSET_X);
                        y += rnd.nextInt(FESTIVAL_MAX_OFFSET_Y);
                    }
                    else 
                    {
                        x -= rnd.nextInt(FESTIVAL_MAX_OFFSET_X);
                        y -= rnd.nextInt(FESTIVAL_MAX_OFFSET_Y);
                    }
                    
                    participant.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
                    participant.teleToLocation(x, y, _startLocation._z);
                    
                    // Remove all buffs from all participants on entry. Works like the skill Cancel.
                    for (L2Effect e : participant.getAllEffects())
                        e.exit();
                    
                    // Remove any stray blood offerings in inventory
                    L2ItemInstance bloodOfferings = participant.getInventory().getItemByItemId(FESTIVAL_OFFERING_ID);
                    if (bloodOfferings != null)
                    	participant.destroyItem("SevenSigns", bloodOfferings, null, true);
                }
            }
            
            L2NpcTemplate witchTemplate = NpcTable.getInstance().getTemplate(_witchSpawn._npcId);
            
            // Spawn the festival witch for this arena
            try 
            {
                L2Spawn npcSpawn = new L2Spawn(witchTemplate);
                
                npcSpawn.setLocx(_witchSpawn._x);
                npcSpawn.setLocy(_witchSpawn._y);
                npcSpawn.setLocz(_witchSpawn._z);
                npcSpawn.setHeading(_witchSpawn._heading);
                npcSpawn.setAmount(1);
                npcSpawn.setRespawnDelay(1);
                
                // Needed as doSpawn() is required to be called also for the NpcInstance it returns.
                npcSpawn.startRespawn();
                
                SpawnTable.getInstance().addNewSpawn(npcSpawn, false);
                _witchInst = npcSpawn.doSpawn();
                
                if (Config.DEBUG)
                    _log.fine("SevenSignsFestival: Spawned the Festival Witch " + npcSpawn.getNpcid() + " at " + _witchSpawn._x + " " + _witchSpawn._y + " " + _witchSpawn._z);
            }
            catch (Exception e) 
            {
                _log.warning("SevenSignsFestival: Error while spawning Festival Witch ID " + _witchSpawn._npcId + ": " + e);
            }
            
            // Make it appear as though the Witch has apparated there.
            MagicSkillUser msu = new MagicSkillUser(_witchInst, _witchInst, 2003, 1, 1, 0);
            _witchInst.broadcastPacket(msu);
            
            // And another one...:D
            msu = new MagicSkillUser(_witchInst, _witchInst, 2133, 1, 1, 0);
            _witchInst.broadcastPacket(msu);
            
            // Send a message to all participants from the witch.
            sendMessageToParticipants("The festival will begin in 2 minutes.");
        }
        
        protected void festivalStart() 
        {
            if (Config.DEBUG)
                _log.info("SevenSignsFestival: Starting festival for " + SevenSigns.getCabalShortName(_cabal) + " (" + getFestivalName(_levelRange) + ")");
            
            spawnFestivalMonsters(FESTIVAL_DEFAULT_RESPAWN, 0);
        }
        
        protected void moveMonstersToCenter()
        {
            boolean isPositive;
            
            if (Config.DEBUG)
                _log.info("SevenSignsFestival: Moving spawns to arena center for festival " + SevenSigns.getCabalShortName(_cabal) + " (" + getFestivalName(_levelRange) + ")");
            
            for (L2FestivalMonsterInstance festivalMob : _npcInsts)
            {
                if (festivalMob.isDead())
                    continue;
                
                // Only move monsters that are idle or doing their usual functions.
                CtrlIntention currIntention = festivalMob.getAI().getIntention();
                
                if (currIntention != CtrlIntention.AI_INTENTION_IDLE && currIntention != CtrlIntention.AI_INTENTION_ACTIVE)
                    continue;
                
                int x = _startLocation._x;
                int y = _startLocation._y;
                
                /*
                 * Random X and Y coords around the player start location, up to half of the 
                 * maximum allowed offset are generated to prevent the mobs from all moving 
                 * to the exact same place.
                 */
                isPositive = (rnd.nextInt(2) == 1);
                
                if (isPositive) 
                {
                    x += rnd.nextInt(FESTIVAL_MAX_OFFSET_X);
                    y += rnd.nextInt(FESTIVAL_MAX_OFFSET_Y);
                }
                else 
                {
                    x -= rnd.nextInt(FESTIVAL_MAX_OFFSET_X);
                    y -= rnd.nextInt(FESTIVAL_MAX_OFFSET_Y);
                }
                
                L2CharPosition moveTo = new L2CharPosition(x, y, _startLocation._z, rnd.nextInt(65536));
                
                festivalMob.setRunning();
                festivalMob.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, moveTo);
            }
        }
        
        public void setSpawnRate(int respawnDelay)
        {
            if (Config.DEBUG)
                _log.info("SevenSignsFestival: Modifying spawn rate of festival mobs to " + respawnDelay + " ms for festival " + SevenSigns.getCabalShortName(_cabal) + " (" + getFestivalName(_levelRange) + ")");
            
            for (L2FestivalMonsterInstance monsterInst : _npcInsts)
                monsterInst.getSpawn().setRespawnDelay(respawnDelay);
        }
        
        /**
         * Used to spawn monsters unique to the festival.
         * <BR>
         * Valid SpawnTypes:<BR>
         * 0 - All Primary Monsters (starting monsters)
         * <BR>
         * 1 - Same as 0, but without archers/marksmen. (used for challenge increase)
         * <BR>
         * 2 - Secondary Monsters (archers)
         * <BR>
         * 3 - Festival Chests
         * 
         * @param respawnDelay
         * @param spawnType
         */
        protected void spawnFestivalMonsters(int respawnDelay, int spawnType)
        {
            int[][] _npcSpawns = null;
            
            switch (spawnType) 
            {
                case 0:
                case 1:
                    _npcSpawns = (_cabal == SevenSigns.CABAL_DAWN) ? festivalDawnPrimarySpawns[_levelRange] : festivalDuskPrimarySpawns[_levelRange];
                    break;
                case 2:
                    _npcSpawns = (_cabal == SevenSigns.CABAL_DAWN) ? festivalDawnSecondarySpawns[_levelRange] : festivalDuskSecondarySpawns[_levelRange];
                    break;
                case 3:
                    _npcSpawns = (_cabal == SevenSigns.CABAL_DAWN) ? festivalDawnChestSpawns[_levelRange] : festivalDuskChestSpawns[_levelRange];
                    break;
            }
            
            for (int i = 0; i < _npcSpawns.length; i++)
            {
                FestivalSpawn currSpawn = new FestivalSpawn(_npcSpawns[i]);
                
                // Only spawn archers/marksmen if specified to do so.
                if (spawnType == 1 && isFestivalArcher(currSpawn._npcId))
                    continue;
                
                L2NpcTemplate npcTemplate = NpcTable.getInstance().getTemplate(currSpawn._npcId);
                
                try 
                {
                    L2Spawn npcSpawn = new L2Spawn(npcTemplate);
                    
                    npcSpawn.setLocx(currSpawn._x);
                    npcSpawn.setLocy(currSpawn._y);
                    npcSpawn.setLocz(currSpawn._z);
                    npcSpawn.setHeading(rnd.nextInt(65536));
                    npcSpawn.setAmount(1);
                    npcSpawn.setRespawnDelay(respawnDelay);
                    
                    // Needed as doSpawn() is required to be called also for the NpcInstance it returns.
                    npcSpawn.startRespawn(); 
                    
                    SpawnTable.getInstance().addNewSpawn(npcSpawn, false);
                    L2FestivalMonsterInstance festivalMob = (L2FestivalMonsterInstance)npcSpawn.doSpawn();
                    
                    // Set the offering bonus to 2x or 5x the amount per kill, 
                    // if this spawn is part of an increased challenge or is a festival chest.
                    if (spawnType == 1)
                        festivalMob.setOfferingBonus(2);
                    else if (spawnType == 3)
                        festivalMob.setOfferingBonus(5);
                    
                    _npcInsts.add(festivalMob);
                    
                    if (Config.DEBUG)
                        _log.fine("SevenSignsFestival: Spawned NPC ID " + currSpawn._npcId + " at " + currSpawn._x + " " + currSpawn._y + " " + currSpawn._z);
                }
                catch (Exception e) 
                {
                    _log.warning("SevenSignsFestival: Error while spawning NPC ID " + currSpawn._npcId + ": " + e);
                }
            }
        }
        
        protected boolean increaseChallenge()
        {
            if (_challengeIncreased)
                return false;
            
            // Set this flag to true to make sure that this can only be done once. 
            _challengeIncreased = true;
            
            if (Config.DEBUG)
                _log.info("SevenSignsFestival: " + _participants.get(0).getName() + "'s team have opted to increase the festival challenge!");
            
            // Spawn more festival monsters, but this time with a twist.
            spawnFestivalMonsters(FESTIVAL_DEFAULT_RESPAWN, 1);
            return true;
        }
        
        public void sendMessageToParticipants(String message)
        {
            if (_participants.size() > 0)
            {
                CreatureSay cs = new CreatureSay(_witchInst.getObjectId(), 0, "Festival Witch", message);
                
                for (L2PcInstance participant : _participants)
                    participant.sendPacket(cs);
            }
        }
        
        protected void festivalEnd() 
        {
            if (Config.DEBUG)
                _log.info("SevenSignsFestival: Ending festival for " + SevenSigns.getCabalShortName(_cabal) + " (" + getFestivalName(_levelRange) + ")");
            
            if (_participants.size() > 0) 
            {
                for (L2PcInstance participant : _participants)
                {
                    relocatePlayer(participant, false);
                    participant.sendMessage("The festival has ended. Your party leader must now register your score before the next festival takes place.");
                }
                
                if (_cabal == SevenSigns.CABAL_DAWN)
                    _dawnPreviousParticipants.put(_levelRange, _participants);
                else
                    _duskPreviousParticipants.put(_levelRange, _participants);
            }
            _participants = null;
            
            unspawnMobs();
        }
        
        protected void unspawnMobs() 
        {
        	// Delete all the NPCs in the current festival arena.
        	if (_witchInst != null)
        		_witchInst.deleteMe();
            
        	if (_npcInsts != null)
	            for (L2FestivalMonsterInstance monsterInst : _npcInsts)
	            	if (monsterInst != null)
	            		monsterInst.deleteMe();
        }
        
        public void relocatePlayer(L2PcInstance participant, boolean isRemoving)
        {
            try 
            {
                FestivalSpawn origPosition = _originalLocations.get(participant);
                
                if (isRemoving) _originalLocations.remove(participant);
                
                participant.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
                participant.teleToLocation(origPosition._x, origPosition._y, origPosition._z);
            }
            catch (Exception e) 
            {
                // If an exception occurs, just move the player to the nearest town.
                participant.teleToLocation(MapRegionTable.TeleportWhereType.Town);
            }
            
            participant.sendMessage("You have been removed from the festival arena.");            
        }
    }
    
    private class FestivalSpawn
    {
        protected final int _x;
        protected final int _y;
        protected final int _z;
        protected final int _heading;
        protected final int _npcId;
        
        protected FestivalSpawn(int x, int y, int z, int heading)
        {
            _x = x;
            _y = y;
            _z = z;
            
            // Generate a random heading if no positive one given.
            _heading = (heading < 0) ? rnd.nextInt(65536) : heading;
            
            _npcId = -1;
        }
        
        protected FestivalSpawn(int[] spawnData)
        {
            _x = spawnData[0];
            _y = spawnData[1];
            _z = spawnData[2];

            _heading = (spawnData[3] < 0) ? rnd.nextInt(65536) : spawnData[3];
            
            if (spawnData.length > 4)
                _npcId = spawnData[4];
            else
                _npcId = -1;
        }
    }
}
