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
package com.festina.gameserver.communitybbs.BB;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;
import com.festina.L2DatabaseFactory;
import com.festina.gameserver.communitybbs.Manager.ForumsBBSManager;
import com.festina.gameserver.communitybbs.Manager.TopicBBSManager;

public class Forum
{
	//type
	public static final int ROOT = 0;
	public static final int NORMAL = 1;
	public static final int CLAN = 2;
	public static final int MEMO = 3;
	public static final int MAIL = 4;
	//perm
	public static final int INVISIBLE = 0;
	public static final int ALL = 1;
	public static final int CLANMEMBERONLY = 2;
	public static final int OWNERONLY = 3;

	private static Logger _log = Logger.getLogger(Forum.class.getName());
	private List<Forum> _children;
	private Map<Integer,Topic> _topic;
	private int _ForumId;
	private String _ForumName;
	private int _ForumParent;
	private int _ForumType;
	private int _ForumPost;
	private int _ForumPerm;
	private Forum _FParent;
	private int _OwnerID;
	private boolean loaded = false;
	/**
	 * @param i
	 */
	public Forum(int Forumid, Forum FParent)
	{
		_ForumId = Forumid;
		_FParent = FParent;
		_children = new FastList<Forum>();
		_topic = new FastMap<Integer,Topic>();	
		
		/*load();	
		getChildren();	*/					
		ForumsBBSManager.getInstance().addForum(this);
		
	}

	/**
	 * @param name
	 * @param parent
	 * @param type
	 * @param perm
	 */
	public Forum(String name, Forum parent, int type, int perm, int OwnerID)
	{
		_ForumName = name;
		_ForumId = ForumsBBSManager.getInstance().GetANewID();
		_ForumParent = parent.getID();
		_ForumType = type;
		_ForumPost = 0;
		_ForumPerm = perm;
		_FParent = parent;
		_OwnerID = OwnerID;
		_children = new FastList<Forum>();
		_topic = new FastMap<Integer,Topic>();	
		parent._children.add(this);		
		ForumsBBSManager.getInstance().addForum(this);
		loaded = true;
	}

	/**
	 * 
	 */
	private void load()
	{
		java.sql.Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT * FROM forums WHERE forum_id=?");
			statement.setInt(1, _ForumId);
			ResultSet result = statement.executeQuery();

			if (result.next())
			{
				_ForumName = result.getString("forum_name");
				_ForumParent = Integer.parseInt(result.getString("forum_parent"));
				_ForumPost = Integer.parseInt(result.getString("forum_post"));
				_ForumType = Integer.parseInt(result.getString("forum_type"));
				_ForumPerm = Integer.parseInt(result.getString("forum_perm"));
				_OwnerID = Integer.parseInt(result.getString("forum_owner_id"));
			}
			result.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("data error on Forum " + _ForumId + " : " + e);
			e.printStackTrace();
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
			}
		}
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT * FROM topic WHERE topic_forum_id=? ORDER BY topic_id DESC");
			statement.setInt(1, _ForumId);
			ResultSet result = statement.executeQuery();
			
			while (result.next())
			{
				Topic t = new Topic(Topic.ConstructorType.RESTORE,Integer.parseInt(result.getString("topic_id")),Integer.parseInt(result.getString("topic_forum_id")),result.getString("topic_name"),Long.parseLong(result.getString("topic_date")),result.getString("topic_ownername"),Integer.parseInt(result.getString("topic_ownerid")),Integer.parseInt(result.getString("topic_type")),Integer.parseInt(result.getString("topic_reply")));
				_topic.put(t.getID(),t);				
				if(t.getID() > TopicBBSManager.getInstance().getMaxID(this))
				{
					TopicBBSManager.getInstance().setMaxID(t.getID(),this);
				}
			}
			result.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("data error on Forum " + _ForumId + " : " + e);
			e.printStackTrace();
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
			}
		}
	}

	/**
	 * 
	 */
	private void getChildren()
	{
		java.sql.Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT forum_id FROM forums WHERE forum_parent=?");
			statement.setInt(1, _ForumId);
			ResultSet result = statement.executeQuery();

			while (result.next())
			{
				
				_children.add(new Forum(Integer.parseInt(result.getString("forum_id")), this));
			}
			result.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("data error on Forum (children): " + e);
			e.printStackTrace();
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
			}
		}

	}
	
	public int getTopicSize()
	{
		if(loaded == false)
		{
			load();
			getChildren();
			loaded = true;
		}
		return _topic.size();
	}
	public Topic gettopic(int j)
	{
		if(loaded == false)
		{
			load();
			getChildren();
			loaded = true;
		}
		return _topic.get(j);
	}
	public void addtopic(Topic t)
	{
		if(loaded == false)
		{
			load();
			getChildren();
			loaded = true;
		}
		_topic.put(t.getID(),t);
	}
	/**
	* @return
	*/
	public int getID()
	{		
		return _ForumId;
	}

	public String getName()
	{
		if(loaded == false)
		{
			load();
			getChildren();
			loaded = true;
		}
		return _ForumName;
	}
	public int getType()
	{
		if(loaded == false)
		{
			load();
			getChildren();
			loaded = true;
		}
		return _ForumType;
	}
	/**
	 * @param name
	 * @return
	 */
	public Forum GetChildByName(String name)
	{
		if(loaded == false)
		{
			load();
			getChildren();
			loaded = true;
		}
		for (Forum f : _children)
		{
			if (f.getName().equals(name))
			{
				return f;
			}
		}		
		return null;
	}
	/**
	 * @param id
	 */
	public void RmTopicByID(int id)
	{
		_topic.remove(id);
		
	}
	/**
	 * 
	 */
	public void insertindb()
	{
		java.sql.Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("INSERT INTO forums (forum_id,forum_name,forum_parent,forum_post,forum_type,forum_perm,forum_owner_id) values (?,?,?,?,?,?,?)");
			statement.setInt(1, _ForumId);
			statement.setString(2, _ForumName);
			statement.setInt(3, _FParent.getID());
			statement.setInt(4, _ForumPost);
			statement.setInt(5, _ForumType);
			statement.setInt(6, _ForumPerm);
			statement.setInt(7, _OwnerID);
			statement.execute();
			statement.close();
			
		}
		catch (Exception e)
		{
			_log.warning("error while saving new Forum to db " + e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
			}
		}

	}

	/**
	 * 
	 */
	public void vload()
	{		
		if(loaded == false)
		{
			load();
			getChildren();
			loaded = true;
		}
	}

	

	/**
	 * @return
	 */
	

}