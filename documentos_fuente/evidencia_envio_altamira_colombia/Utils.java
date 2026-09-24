package util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

//import com.bbva.kytl.services.SHIVAToken;
//import com.sun.org.glassfish.gmbal.ParameterNames;
import org.apache.log4j.Logger;

public class Utils {

	public static ArrayList<BufferedWriter> listaBufferedWriter =new ArrayList<BufferedWriter>(); 
	public static ArrayList<String> imprimir_error = new ArrayList<String>();

	public void sacarFichero(Logger LOGGER, String fichero, ArrayList<String> IDs){
		LOGGER.info("sacarFichero");
		File ficherosalida = new File(fichero);
		try {
			if (ficherosalida.exists()) {
				if (ficherosalida.delete()){
					System.out.println("El fichero ha sido borrado satisfactoriamente");
					LOGGER.info("El fichero ha sido borrado satisfactoriamente");
				}else{
					System.out.println("El fichero no puede ser borrado");
					LOGGER.info("El fichero ha sido borrado satisfactoriamente");
				}
			}
			ficherosalida.createNewFile();
			BufferedWriter bw=new BufferedWriter(new FileWriter(ficherosalida)); 
		    for(int i=0;i<IDs.size();i++){
		    	bw.write(IDs.get(i));
		    	bw.newLine();
		    	bw.flush();
		    }
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String generateJOBID()
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
	
	//TODO: Adaptar este método a la clave o claves recibidas desde XMAS
	public void decryptAnt(Logger LOGGER, String inputPath, String outputPath, String keyPath1, String keyPath2) {
		try {
			System.out.println("-----Descifrador 3DES-----");
			LOGGER.info("-----Descifrador 3DES-----");
			System.out.println("Descifrando " + inputPath);
			LOGGER.info("Descifrando " + inputPath);
			//System.out.println("Con claves: ");
			//LOGGER.info("Con claves: ");
			//System.out.println(keyPath1);
			//LOGGER.info(keyPath1);
			//System.out.println(keyPath2);
			//LOGGER.info(keyPath2);
			//Procesa los archivos con las claves
			String pKey = processKeyFiles(new String[]{keyPath1, keyPath2});
			if (pKey != "") {
				System.out.println("Clave de 24 bytes procesada");
				LOGGER.info("Clave de 24 bytes procesada");
			}
			else {
				throw new Exception("Error procesando clave");
			}
			byte[] key = DatatypeConverter.parseHexBinary(pKey);
			byte[] iVector = {0,0,0,0,0,0,0,0};

			IvParameterSpec ivectorSpec = new IvParameterSpec(iVector);
			List<String> lines = Files.readAllLines(Paths.get(inputPath));

			SecretKey secretKey = new SecretKeySpec(key, "DESede");
			Cipher decipher = Cipher.getInstance("DESede/CBC/noPadding");
			decipher.init(Cipher.DECRYPT_MODE, secretKey, ivectorSpec);
			byte[] message;
			byte[] plainText;
			String plainString;
			FileWriter fw = new FileWriter(outputPath);
			System.out.println("Descifrando...");
			LOGGER.info("Descifrando...");
			for (int i = 0; i < lines.size(); i++) {
				message = DatatypeConverter.parseHexBinary(lines.get(i));
				plainText = decipher.doFinal(message);
				plainString = new String(plainText, "UTF-8");
				fw.write(plainString+"\r\n");
				//System.out.println(plainString);
			}
			fw.close();
			System.out.println("Descifrado al archivo: " + outputPath);
			LOGGER.info("Descifrado al archivo: " + outputPath);

		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.error(e);
			System.out.println("No descifrado");
			LOGGER.error("No descifrado");
		}
	}
	
	
	//TODO: Adaptar este metodo a la clave o claves recibidas desde XMAS
		public boolean decrypt(Logger LOGGER, String inputPath, String outputPath, String key1, String key2) {
			try {
				System.out.println("-----Descifrador 3DES-----");
				LOGGER.info("-----Descifrador 3DES-----");
				System.out.println("Descifrando " + inputPath);
				LOGGER.info("Descifrando " + inputPath);
				//System.out.println("Con claves: "+key1 + " - "+ key2);
				//LOGGER.info("Con claves: "+key1 + " - "+ key2);

				//Procesa los archivos con las claves
				//String pKey = processKeyFiles(new String[]{keyPath1, keyPath2});
				String pKey = processKey(key1, key2);
				if (pKey != "") {
					System.out.println("Clave de 24 bytes procesada");
					LOGGER.info("Clave de 24 bytes procesada");
				}
				else {
					throw new Exception("Error procesando clave");
				}
				byte[] key = DatatypeConverter.parseHexBinary(pKey);
				byte[] iVector = {0,0,0,0,0,0,0,0};

				IvParameterSpec ivectorSpec = new IvParameterSpec(iVector);
				List<String> lines = Files.readAllLines(Paths.get(inputPath));

				SecretKey secretKey = new SecretKeySpec(key, "DESede");
				Cipher decipher = Cipher.getInstance("DESede/CBC/noPadding");
				decipher.init(Cipher.DECRYPT_MODE, secretKey, ivectorSpec);
				byte[] message;
				byte[] plainText;
				String plainString;
				FileWriter fw = new FileWriter(outputPath);
				System.out.println("Descifrando...");
				LOGGER.info("Descifrando...");
				for (int i = 0; i < lines.size(); i++) {
				//for (int i = 0; i < 2; i++) {
					message = DatatypeConverter.parseHexBinary(lines.get(i));
					plainText = decipher.doFinal(message);
					plainString = new String(plainText, "UTF-8");
					fw.write(plainString+"\r\n");
					//System.out.println(plainString);
				}
				fw.close();
				System.out.println("Descifrado al archivo: " + outputPath);
				LOGGER.info("Descifrado al archivo: " + outputPath);
				return true;

			} catch (Exception e) {
				e.printStackTrace();
				LOGGER.error(e);
				System.out.println("No descifrado");
				LOGGER.error("No descifrado");
				return false;
			}
		}

	//Metodo que extrae las claves de dos archivos y devuelve la clave resultante de la op XOR
	String processKeyFiles(String paths[]) {
		String xoredKeysStr = "";
		String line, eightBytes;
		byte[] xoredKeys = new byte[24];
		String[] keys = {"", ""};
		List<String> lines;
		try {
			for (int i = 0; i <= 1; i++) {
				lines = Files.readAllLines(Paths.get(paths[i]));
				line = "";
				for (int j = 12; j <= 14; j++) {
					line = lines.get(j).substring(26, 46);
					eightBytes = line.replaceAll("\\s", "");
					keys[i] += eightBytes;
				}
				System.out.println("Extracted key " + (i+1) +": " + keys[i] + " from " + paths[i]);
			}

			byte[] keyBytes1 = DatatypeConverter.parseHexBinary(keys[0]);
			byte[] keyBytes2 = DatatypeConverter.parseHexBinary(keys[1]);

			for (int i = 0; i < keyBytes1.length; i++) {
				xoredKeys[i] = (byte) (keyBytes1[i]^keyBytes2[i]);
			}
			xoredKeysStr = DatatypeConverter.printHexBinary(xoredKeys);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return xoredKeysStr;
	}
	
	//Metodo que extrae las claves de dos archivos y devuelve la clave resultante de la op XOR
		String processKey(String key1, String key2) {
			String xoredKeysStr = "";
			byte[] xoredKeys = new byte[24];
			try {
				byte[] keyBytes1 = DatatypeConverter.parseHexBinary(key1);
				byte[] keyBytes2 = DatatypeConverter.parseHexBinary(key2);

				for (int i = 0; i < keyBytes1.length; i++) {
					xoredKeys[i] = (byte) (keyBytes1[i]^keyBytes2[i]);
				}
				xoredKeysStr = DatatypeConverter.printHexBinary(xoredKeys);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			return xoredKeysStr;
		}

}
