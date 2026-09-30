package util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import java.util.Random;

public class Utilidades 
{
	//Metodo de lectura de properties
	public ArrayList<String> leerProperties(String select,String Servicio) throws IOException
	{
		ArrayList<String> arrayParam = new ArrayList<String>();
		Properties prop = new Properties();
		String propFileName = select.toString();
		InputStream is = new FileInputStream(propFileName);
		prop.load(is);
		
		//Creación de reloj
		Date time = new Date(System.currentTimeMillis());
		String query = "";
		query = prop.getProperty("query" + Servicio).toString();
		System.out.println("QUERYA: " + query);
		System.out.println("QUERYD: " + query);
		arrayParam.add(query);
		arrayParam.add(prop.getProperty("ruta"));
		arrayParam.add(prop.getProperty("cabecera" + Servicio));
		arrayParam.add(prop.getProperty("fileName" + Servicio));
		
		//Mostrar lectura de properties
		System.out.println("[" + time + "]" + "Se ha leido correctamente del properties");
		return arrayParam;
	}
	
	
	public String generateJOBID()
	{
		//Generador de un JobID
		char[] chars = "abcdefghijklmnopqrstuvwxyz".toCharArray();
		StringBuilder sb = new StringBuilder();
		Random random = new Random();
		for (int i = 0; i < 16; i++) {
		    char c = chars[random.nextInt(chars.length)];
		    sb.append(c);
		}
		String output = sb.toString();
	    
	    return output;
	}
}
