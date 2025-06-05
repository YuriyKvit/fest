/**
 * 
 */
package com.festina.gameserver.cache.sql;

//import org.hibernate.HibernateException;
//import org.hibernate.SessionFactory;
//import org.hibernate.cfg.Configuration;
//import org.hibernate.classic.Session;
/*
public class CacheD
{
	private static SessionFactory sessionFactory;
	
	public CacheD()
	{
		try
		{
			sessionFactory = new Configuration().configure().buildSessionFactory();
		}
		catch (Throwable ex)
		{
			System.err.println("[CacheD] fatal: " + ex);
			throw new ExceptionInInitializerError(ex);
		}		
	}
	
	public void pushObject(Object o)
	{
		Session sess = null;

		try
		{
			sess = sessionFactory.getCurrentSession();

			sess.beginTransaction();

			if(o instanceof DummyEntity)
			{
				DummyEntity e = (DummyEntity)o;
				sess.save(e);
			}
		
			sess.getTransaction().commit();
		}
		catch(HibernateException e)
		{
			System.err.print("[CacheD] pushObject failed!");
			if(sess != null && sess.getTransaction() != null)
			{
				sess.getTransaction().rollback();
				System.err.print(" rollback...");
			}
			System.err.println("");
			e.printStackTrace();
		}
	}
}
*/