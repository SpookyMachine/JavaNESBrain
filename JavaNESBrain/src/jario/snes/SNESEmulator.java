/**
 * Copyright 2013 Jason LaDere
 */

package jario.snes;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Properties;

public class SNESEmulator
{
	
	public static String propertiesPath =  "jario" + File.separator + "snes" + File.separator + "components.properties";
	
	
	public static void main(String args[])
	{
		start();
	}
	
	
	public static void start()
	{
		try
		{
			File dir = new File("components" + File.separator);
			File file = new File("components.properties");
			ClassLoader loader = SNESEmulator.class.getClassLoader();
			Properties prop = new Properties();
			try
			{
				if (dir.exists() && dir.listFiles().length > 0)
				{
					File[] files = dir.listFiles();
					URL[] urls = new URL[files.length];
					for (int i = 0; i < files.length; i++) urls[i] = files[i].toURI().toURL();
					loader = new URLClassLoader(urls, SNESEmulator.class.getClassLoader());
				}
				 
				
				URL url = file.exists() ? file.toURI().toURL() : loader.getResource(propertiesPath);
				
				if (url != null)
					{
					prop.load(url.openStream());
					}else{
						System.err.println("COULD NOT LOAD " + propertiesPath );
					}
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}

			Class.forName(prop.getProperty("SYSTEM", "SYSTEM"), true, loader).newInstance();
		}
		catch (InstantiationException e)
		{
			e.printStackTrace();
		}
		catch (IllegalAccessException e)
		{
			e.printStackTrace();
		}
		catch (ClassNotFoundException e)
		{
			e.printStackTrace();
		}
	}
}
