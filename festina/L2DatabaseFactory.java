package com.festina;

import com.festina.gameserver.ThreadPoolManager;
import com.jolbox.bonecp.BoneCPConfig;
import com.jolbox.bonecp.BoneCPDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class L2DatabaseFactory
{
  static Logger _log = Logger.getLogger(L2DatabaseFactory.class.getName());
  private static ScheduledExecutorService _executor;
  private ProviderType _providerType;
  private BoneCPDataSource _source;

  public L2DatabaseFactory()
  {
    _log.info("Loading BoneCP");
    try
    {
      _source = new BoneCPDataSource();
      _source.getConfig().setDefaultAutoCommit(Boolean.valueOf(true));

      _source.getConfig().setPoolAvailabilityThreshold(10);
      _source.getConfig().setMinConnectionsPerPartition(10);
      _source.getConfig().setMaxConnectionsPerPartition(Config.DATABASE_MAX_CONNECTIONS);

      _source.setPartitionCount(3);

      _source.setAcquireRetryAttempts(0);
      _source.setAcquireRetryDelayInMs(500L);

      _source.setAcquireIncrement(5);

      _source.setConnectionTimeoutInMs(0L);

      _source.setIdleConnectionTestPeriodInMinutes(1L);
      _source.setIdleMaxAgeInSeconds(1800L);

      _source.setTransactionRecoveryEnabled(true);

      _source.setDriverClass(Config.DATABASE_DRIVER);
      _source.setJdbcUrl(Config.DATABASE_URL);
      _source.setUsername(Config.DATABASE_LOGIN);
      _source.setPassword(Config.DATABASE_PASSWORD);

      _source.getConnection().close();

      if (Config.DEBUG) {
        _log.fine("Database Connection Working");
      }
      if (Config.DATABASE_DRIVER.toLowerCase().contains("microsoft"))
        _providerType = ProviderType.MsSql;
      else
        _providerType = ProviderType.MySql;
    }
    catch (Exception e)
    {
      if (Config.DEBUG)
        _log.fine("Database Connection FAILED");
      throw new Error("L2DatabaseFactory: Failed to init database connections: " + e.getMessage(), e);
    }
  }

  public final String prepQuerySelect(String[] fields, String tableName, String whereClause, boolean returnOnlyTopRecord)
  {
    String msSqlTop1 = "";
    String mySqlTop1 = "";
    if (returnOnlyTopRecord)
    {
      if (getProviderType() == ProviderType.MsSql)
        msSqlTop1 = " Top 1 ";
      if (getProviderType() == ProviderType.MySql)
        mySqlTop1 = " Limit 1 ";
    }
    String query = "SELECT " + msSqlTop1 + safetyString(fields) + " FROM " + tableName + " WHERE " + whereClause + mySqlTop1;
    return query;
  }

  public void shutdown()
  {
    try
    {
      _source.close();
      _source = null;
    }
    catch (Exception e)
    {
      _log.log(Level.WARNING, e.getMessage(), e);
    }
  }

  public final String safetyString(String[] whatToCheck)
  {
    char braceRight;
    char braceLeft;
    if (getProviderType() == ProviderType.MsSql)
    {
      braceLeft = '[';
      braceRight = ']';
    }
    else
    {
      braceLeft = '`';
      braceRight = '`';
    }

    int length = 0;

    for (String word : whatToCheck)
    {
      length += word.length() + 4;
    }

    StringBuilder sbResult = new StringBuilder(length);

    for (String word : whatToCheck)
    {
      if (sbResult.length() > 0)
      {
        sbResult.append(", ");
      }

      sbResult.append(braceLeft);
      sbResult.append(word);
      sbResult.append(braceRight);
    }

    return sbResult.toString();
  }

  public Connection getConnection()
  {
    Connection con = null;

    while (con == null)
    {
      try
      {
        con = _source.getConnection();
        if (Server.SERVER_MODE == 1)
          ThreadPoolManager.getInstance().scheduleGeneral(new ConnectionCloser(con, new RuntimeException()), 60000L);
        else
          getExecutor().schedule(new ConnectionCloser(con, new RuntimeException()), 60L, TimeUnit.SECONDS);
      }
      catch (SQLException e)
      {
        _log.log(Level.WARNING, "L2DatabaseFactory: getConnection() failed, trying again " + e.getMessage(), e);
      }
    }
    return con;
  }

  public static void close(Connection con)
  {
    if (con == null) {
      return;
    }
    try
    {
      con.close();
    }
    catch (SQLException e)
    {
      _log.log(Level.WARNING, "Failed to close database connection!", e);
    }
  }

  private static ScheduledExecutorService getExecutor()
  {
    if (_executor == null)
    {
      synchronized (L2DatabaseFactory.class)
      {
        if (_executor == null)
          _executor = Executors.newSingleThreadScheduledExecutor();
      }
    }
    return _executor;
  }

  public int getBusyConnectionCount()
  {
    return _source.getTotalLeased();
  }

  public final ProviderType getProviderType()
  {
    return _providerType;
  }

  public static L2DatabaseFactory getInstance()
  {
    return SingletonHolder.INSTANCE;
  }

  private static final class SingletonHolder
  {
    private static final L2DatabaseFactory INSTANCE = new L2DatabaseFactory();
  }

  private static class ConnectionCloser
    implements Runnable
  {
    private Connection c;
    private RuntimeException exp;

    public ConnectionCloser(Connection con, RuntimeException e)
    {
      c = con;
      exp = e;
    }

    public void run()
    {
      try
      {
        if (!c.isClosed())
        {
          if (Config.DEBUG) L2DatabaseFactory._log.log(Level.WARNING, "Unclosed connection! Trace: " + exp.getStackTrace()[1], exp);
          c.close();
        }
      }
      catch (SQLException e)
      {
        L2DatabaseFactory._log.log(Level.WARNING, "", e);
      }
    }
  }

  public static enum ProviderType
  {
    MySql, MsSql;
  }
}