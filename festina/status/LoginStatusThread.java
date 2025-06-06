package com.festina.status;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;

import com.festina.Base64;
import com.festina.Config;
import com.festina.L2DatabaseFactory;
import com.festina.loginserver.GameServerTable;
import com.festina.loginserver.LoginController;
import com.festina.loginserver.LoginServer;


public class LoginStatusThread extends Thread
{
    private static final Logger _log = Logger.getLogger(LoginStatusThread.class.getName());
    
    private Socket                  _csocket;
    
    private PrintWriter             _print;
    private BufferedReader          _read;
    

	private boolean _redirectLogger;

	private String _pass;
      
    private void telnetOutput(int type, String text) {
        if ( type == 1 ) 
        	_log.info("TELNET | "+text);
        else if ( type == 2 ) 
        	_log.info("TELNET | "+text);
        else if ( type == 3 ) 
        	_log.info(text);
        else if ( type == 4 ) 
        	_log.info(text);
        else 
        	_log.info("TELNET | "+text);
    }
    
    private boolean isValidIP(Socket client) {
        boolean result = false;        
        InetAddress ClientIP = client.getInetAddress();
               
        // convert IP to String, and compare with list
        String clientStringIP = ClientIP.getHostAddress();
        
        telnetOutput(1, "Connection from: "+clientStringIP);
        
        // read and loop thru list of IPs, compare with newIP
        if ( Config.DEVELOPER ) telnetOutput(2, "");    
        
        try {
            Properties telnetSettings = new Properties();
            InputStream telnetIS = new FileInputStream(new File(Config.TELNET_FILE));
            telnetSettings.load(telnetIS);
            telnetIS.close();
            
            String HostList = telnetSettings.getProperty("ListOfHosts", "127.0.0.1,localhost");
            
            if ( Config.DEVELOPER ) telnetOutput(3, "Comparing ip to list...");
            
            // compare
            String ipToCompare = null;
            for (String ip:HostList.split(",")) {
                if ( !result ) {
                    ipToCompare = InetAddress.getByName(ip).getHostAddress();
                    if ( clientStringIP.equals(ipToCompare) ) result = true;
                    if ( Config.DEVELOPER ) telnetOutput(3, clientStringIP + " = " + ipToCompare + "("+ip+") = " + result);
                }
            }    
        }
        catch ( IOException e) {
            if ( Config.DEVELOPER ) telnetOutput(4, "");
            telnetOutput(1, "Error: "+e);
        }
        
        if ( Config.DEVELOPER ) telnetOutput(4, "Allow IP: "+result);
        return result;
    }
    
    public LoginStatusThread(Socket client, int uptime) throws IOException
    {
        _csocket = client;
        
        _print = new PrintWriter(_csocket.getOutputStream());
        _read  = new BufferedReader(new InputStreamReader(_csocket.getInputStream()));
        
        if ( isValidIP(client) ) {    
            telnetOutput(1, client.getInetAddress().getHostAddress()+" accepted.");
            _print.println("Welcome To The L2J Telnet Session.");
            _print.println("Please Insert Your Login!");
            _print.print("Login: ");
            _print.flush();
            String tmpLine = _read.readLine();
            if ( tmpLine == null )  {
                _print.println("Error.");
                _print.println("Disconnected...");
                _print.flush();
                _csocket.close();
                return;
            }
            else {
                if (!validLogin(tmpLine))
                {
                    _print.println("Incorrect Login!");
                    _print.println("Disconnected...");
                    _print.flush();
                    _csocket.close();
                    return;
                }
                else
                {
                    _print.println("Login Correct!");
                    _print.flush();
                }
            }
            _print.println("Please Insert Your Password!");
            _print.print("Password: ");
            _print.flush();
            tmpLine = _read.readLine();
            if ( tmpLine == null )  {
                _print.println("Error.");
                _print.println("Disconnected...");
                _print.flush();
                _csocket.close();
            }
            else {
                if (!validPassword(tmpLine))
                {
                    _print.println("Incorrect Password!");
                    _print.println("Disconnected...");
                    _print.flush();
                    _csocket.close();
                }
                else
                {
                    _print.println("Password Correct!");
                    _print.println("[L2J]");
                    _print.print("");
                    _print.flush();
                    start();
                }
            }
        }
        else {
            telnetOutput(1, "Connection attempt from "+ client.getInetAddress().getHostAddress() +" rejected.");
            _csocket.close();
        }
    }
    
    /**
	 * @param tmpLine
	 * @return
	 */
	private boolean validPassword(String password)
	{
		byte[] expectedPass = Base64.decode(_pass);
		MessageDigest md;
		try
		{
			md = MessageDigest.getInstance("SHA");
			byte[] raw = password.getBytes("UTF-8");
			byte[] hash = md.digest(raw);
			for (int i=0;i<expectedPass.length;i++)
			{
			    if (hash[i] != expectedPass[i])
			    {
			        return false;
			    }
			}
			return true;
		}
		catch (NoSuchAlgorithmException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch( UnsupportedEncodingException uee)
		{
			
		}
		return false;
	}

	/**
	 * @param tmpLine
	 * @return
	 */
	private boolean validLogin(String login)
	{
		if(!LoginController.getInstance().isGM(login))
			return false;
		java.sql.Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT password FROM accounts WHERE login=?");
			statement.setString(1, login);
			ResultSet rset = statement.executeQuery();
			if (rset.next())
			{
				_pass = rset.getString("password");
				statement.close();
				con.close();
				return true;
			}
			statement.close();
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
		}
		finally
		{
			try { con.close(); } catch (Exception e) {}
		}
		return false;
	}

	public void run()
    {
        String _usrCommand = "";
        try
        {
            while (_usrCommand.compareTo("quit") != 0 && _usrCommand.compareTo("exit") != 0)
            {
                _usrCommand = _read.readLine();
                if(_usrCommand == null)
                {
                	_csocket.close();
                	break;
                }
                if (_usrCommand.equals("help")) {
                    _print.println("The following is a list of all available commands: ");
                    _print.println("help                - shows this help.");
                    _print.println("status              - displays basic server statistics.");
                    _print.println("unblockip <ip>      - removes <ip> from hacking protection list.");
                    _print.println("shutdown			- shuts down server.");
                    _print.println("restart				- restarts the server.");
                    _print.println("RedirectLogger		- Telnet will give you some info about server in real time.");
                    _print.println("quit                - closes telnet session.");
                    _print.println("");
                }
                else if (_usrCommand.equals("status"))
                {
                	for(String str : GameServerTable.getInstance().status())
                	{
                		_print.println(str);
                	}
                }
                else if (_usrCommand.startsWith("unblock"))
                {
                    try
                    {
                        _usrCommand = _usrCommand.substring(8);
                        if (LoginServer.getInstance().unblockIp(_usrCommand))
                        {
                            _log.warning("IP removed via TELNET by host: " + _csocket.getInetAddress().getHostAddress());
                            _print.println("The IP " + _usrCommand + " has been removed from the hack protection list!");
                        }
                        else
                        {
                            _print.println("IP not found in hack protection list...");
                        }
                    }
                    catch (StringIndexOutOfBoundsException e)
                    {
                        _print.println("Please Enter the IP to Unblock!");
                    }
                }
                else if (_usrCommand.startsWith("shutdown"))
                {
                	LoginServer.getInstance().shutdown(false);
                	_print.println("Bye Bye!");
    	            _print.flush();
                	_csocket.close();
                }
                else if (_usrCommand.startsWith("restart"))
                {
                	LoginServer.getInstance().shutdown(true);
                	_print.println("Bye Bye!");
    	            _print.flush();
                	_csocket.close();
                }
                else if (_usrCommand.equals("RedirectLogger")) {_redirectLogger = true;}
                else if (_usrCommand.equals("quit")) { /* Do Nothing :p - Just here to save us from the "Command Not Understood" Text */ }
                else if (_usrCommand.length() == 0) { /* Do Nothing Again - Same reason as the quit part */ }
                else
                {
                    _print.println("Invalid Command");
                }
                _print.print("");
                _print.flush();
            }
            if(!_csocket.isClosed())
            {
	            _print.println("Bye Bye!");
	            _print.flush();
	            _csocket.close();
            }
            telnetOutput(1, "Connection from "+_csocket.getInetAddress().getHostAddress()+" was closed by client.");
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
   
    public void printToTelnet(String msg)
    {
    	synchronized(_print)
    	{
    		_print.println(msg);
    		_print.flush();
    	}
    }

	/**
	 * @return Returns the redirectLogger.
	 */
	public boolean isRedirectLogger()
	{
		return _redirectLogger;
	}
}
