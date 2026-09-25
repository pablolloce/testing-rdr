package utilities;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.apache.log4j.Logger;

public class FicheroExtraccion {
   public static final Logger LOGGER = Logger.getLogger(FicheroExtraccion.class);

   public static synchronized void grabarFichero(String xml, File ficherosalida, String rutaFichero) throws InterruptedException, IOException {
      OutputStreamWriter bw = null;
      Object var4 = null;

      try {
         bw = new OutputStreamWriter(new FileOutputStream(ficherosalida, true), StandardCharsets.UTF_8);
         BufferedWriter bw1 = new BufferedWriter(bw);
         bw1.write(xml);
         bw1.newLine();
         bw1.flush();
      } catch (UnsupportedEncodingException var18) {
         LOGGER.error(var18);
      } catch (FileNotFoundException var19) {
         LOGGER.error(var19);
      } catch (IOException var20) {
         LOGGER.error(var20);
      } finally {
         if (bw != null) {
            try {
               bw.close();
            } catch (IOException var17) {
               LOGGER.error(var17);
            }
         }

      }

	   }

   //nuevo metodo que permite escribir +1 linea de resultados
   public static synchronized void grabarFicheroMultilinea(ArrayList<String> resultList, File ficherosalida, String rutaFichero) throws InterruptedException, IOException {
	      OutputStreamWriter bw = null;
	      Object var4 = null;

	      try {
	         bw = new OutputStreamWriter(new FileOutputStream(ficherosalida, true), StandardCharsets.UTF_8);
	         BufferedWriter bw1 = new BufferedWriter(bw);
	         for(String line : resultList){
	         bw1.write(line);
	         bw1.newLine();
	         bw1.flush();
	         }
	      } catch (UnsupportedEncodingException var18) {
	         LOGGER.error(var18);
	      } catch (FileNotFoundException var19) {
	         LOGGER.error(var19);
	      } catch (IOException var20) {
	         LOGGER.error(var20);
	      } finally {
	         if (bw != null) {
	            try {
	               bw.close();
	            } catch (IOException var17) {
	               LOGGER.error(var17);
	            }
	         }

	      }

	   }
}
