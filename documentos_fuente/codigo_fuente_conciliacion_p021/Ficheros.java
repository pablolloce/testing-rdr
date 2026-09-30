package util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Ficheros 
{
	
	
	public File creaFichero(String fileName, String header)throws Exception
	{

		PrintWriter pw = null;		OutputStreamWriter os = null;		FileOutputStream fos = null;		File f = null;
		try 	{
			f = new File(fileName);
			f.setExecutable(true,false);	f.setWritable(true,false);		f.setReadable(true,false);
			
			fos = new FileOutputStream(f,true);	os = new OutputStreamWriter(fos,"ISO-8859-1");
			pw = new PrintWriter(os);		pw.println(header);
			os.close();				fos.close();
			
		} catch (FileNotFoundException e1) {e1.printStackTrace();
		} catch (UnsupportedEncodingException e) {e.printStackTrace();
		} catch (IOException e) {e.printStackTrace();	}
		
		return f;
		
	}    
	
	public void escribirFicheroGeneral (File f, ArrayList<String> composicionFichero)throws Exception
	{
		PrintWriter pw = null;		OutputStreamWriter os = null;		FileOutputStream fos = null;
		
		try 	{
			f.setExecutable(true,false);	f.setWritable(true,false);		f.setReadable(true,false);
			fos = new FileOutputStream(f,true);	os = new OutputStreamWriter(fos,"ISO-8859-1");		pw = new PrintWriter(os);
		
		} catch (FileNotFoundException e1) 	{e1.printStackTrace();
		} catch (UnsupportedEncodingException e){e.printStackTrace();		}
		
		for (int i = 0 ; i < composicionFichero.size(); i++)	{
			pw.println(composicionFichero.get(i));
		}
		try {
			os.close();			fos.close();
		} catch (IOException e)	{	e.printStackTrace();		}
	}
	public void Zippear(String pFile, String pZipFile)
	{
		// objetos en memoria
		FileInputStream fis = null;
		FileOutputStream fos = null;
		ZipOutputStream zipos = null;
 
		// buffer
		int BUFFER_SIZE = 1024;
		String [] str1 = pFile.split("\\/");
		String pFile2 = str1[str1.length -1];

		byte[] buffer = new byte[BUFFER_SIZE];
		try {
			// fichero a comprimir
			fis = new FileInputStream(pFile);
			// fichero contenedor del zip
			fos = new FileOutputStream(pZipFile);
			// fichero comprimido
			zipos = new ZipOutputStream(fos);
			ZipEntry zipEntry = new ZipEntry(pFile2);
			zipos.putNextEntry(zipEntry);
			int len = 0;
			// zippear
			while ((len = fis.read(buffer, 0, BUFFER_SIZE)) != -1)
				zipos.write(buffer, 0, len);
			// volcar la memoria al disco
			zipos.flush();
		} catch (Exception e) 
		{
			try {
				throw e;
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		} finally {
			// cerramos los files
			try {
				zipos.close();		fis.close();		fos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

		} // end try
	} // end Zippear
			
	public void historifica(String file) throws Exception
	{

		String[] str = file.split("\\/");
		String ruta = new String();
		for (int i=0; i<str.length -1 ;i++)
		{
			ruta = ruta + str[i] + "/";
		}
		String rutaold = ruta + "old" ;
		File old = new File(rutaold);
		if (!old.exists())
		{
			old.mkdirs();
		}
		rutaold = rutaold + "/" + str[str.length - 1];
		ruta = ruta + str[str.length - 1];
		File fichero = new File(ruta);
        File fichero2 = new File(rutaold);
        
        if (!fichero.exists())
        {
        	this.creaFichero(file,"");
        }
        if (fichero2.exists())
        {
        	fichero2.delete();
        }
        
        boolean success = fichero.renameTo(fichero2);
        
        if (!success) {
            System.out.println("Error intentando cambiar el nombre de fichero");
        }
	}
	
	public void historificaZip(String file) throws Exception  
	{
		String[] str = file.split("\\/");
		String ruta = new String();
		for (int i=0; i<str.length -1 ;i++)
		{
			ruta = ruta + str[i] + "/";
		}
		String rutaold = ruta + "old" ;
		File old = new File(rutaold);
		if (!old.exists())
		{
			old.mkdirs();
		}
		rutaold = rutaold + "/" + str[str.length - 1].split("\\.")[0] + ".zip";
		ruta = ruta + str[str.length - 1];
		File fichero = new File(ruta);
        File fichero2 = new File(rutaold);
        if (!fichero.exists())
        {
        	this.creaFichero(file, "");
        }
        if (fichero2.exists())
        {
        	fichero2.delete();
        }

        this.Zippear(file, rutaold);
        fichero.delete();
	}
	
	
}
