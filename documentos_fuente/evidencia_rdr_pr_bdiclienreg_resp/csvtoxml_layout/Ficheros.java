import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import jdbc.ConDB;

public class Ficheros {
	
	public static Connection obj_con = null;
	public static ConDB obj_ConDB = null;
	private static PreparedStatement stmt = null;
	public String str_split = "";

	public ArrayList<HashMap<String, String>> obtenerDatos(File fich, String[] args) {
		ArrayList<HashMap<String, String>> obj = new ArrayList<HashMap<String, String>>();
		FileInputStream fis = null;
		InputStreamReader fr = null;
		BufferedReader br = null;
		String linea = "";
		File f = fich;
		
		getSplitter(args);
		
		try {
			fis = new FileInputStream(f);
			fr = new InputStreamReader(fis, "UTF-8");
			br = new BufferedReader(fr);
			if (f.exists()) {
				int contador = 0;
				ArrayList<String> cabecera = new ArrayList<String>();
				while ((linea = br.readLine()) != null) {
					HashMap<String, String> cad_linea = new HashMap<String, String>();
					contador++;
					String[] str_linea = linea.split(str_split);
					if (contador == 1) {
						for (int i = 0; i < str_linea.length; i++) {
							if (str_linea[0] != null && !str_linea[0].isEmpty() && !str_linea[0].trim().equals("")) {
								cabecera.add(str_linea[i].trim());
							}
						}
					} else {
						for (int i = 0; i < str_linea.length; i++) {
							String dato = new String();
							String[] str_dato = str_linea[i].split("~");
							for (int j = 0; j < str_dato.length; j++) {
								if (str_dato[j] != null && !str_dato[j].isEmpty() && !str_dato[j].trim().equals("")) {
									dato = xmlEscapeText(str_dato[j].trim());
								} else {
									dato = "";
								}
							}
							cad_linea.put(cabecera.get(i), dato);
						}
						obj.add(cad_linea);
					}
				}
			}
			br.close();
			fis.close();
			fr.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return obj;
	}

	String xmlEscapeText(String t) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < t.length(); i++) {
			char c = t.charAt(i);
			switch (c) {
			case 'Á':
				sb.append("Á");
				break;
			case 'É':
				sb.append("É");
				break;
			case 'Í':
				sb.append("Í");
				break;
			case 'Ó':
				sb.append("Ó");
				break;
			case 'Ú':
				sb.append("Ú");
				break;
			case 'á':
				sb.append("á");
				break;
			case 'é':
				sb.append("é");
				break;
			case 'í':
				sb.append("í");
				break;
			case 'ó':
				sb.append("ó");
				break;
			case 'ú':
				sb.append("ú");
				break;
			case '<':
				sb.append("&lt;");
				break;
			case '>':
				sb.append("&gt;");
				break;
			case '\"':
				sb.append("&quot;");
				break;
			case '&':
				sb.append("&amp;");
				break;
			case '\'':
				sb.append("&apos;");
				break;
			case 'Ñ':
				sb.append("Ñ");
				break;
			case 'ñ':
				sb.append("ñ");
				break;
			default:
				if (c > 0x7e) {
					sb.append("&#" + ((int) c) + ";");
				} else {
					sb.append(c);
				}
			}
		}
		return sb.toString();
	}

	public void escribirDatos(String direct, String XML_SALIDA) {
		/*
		 * Date fechaActual = new Date(); DateFormat formatoFecha = new
		 * SimpleDateFormat("yyyyMMddhhmmss"); String
		 * fich_sal=direct+"/COMPASS_"+formatoFecha.format(fechaActual)+".xml";
		 */
		String fich_sal = direct;
		PrintWriter pw = null;
		OutputStreamWriter os = null;
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(new File(fich_sal));
			os = new OutputStreamWriter(fos, "UTF-8");
			pw = new PrintWriter(os);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		pw.print(XML_SALIDA);

		try {
			pw.close();
			os.close();
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void getSplitter(String[] args) {
		try {
			/******************************
			 * INICIALIZACION DE CONEXIONES
			 *******************************************************/
			obj_ConDB = new ConDB();
			obj_ConDB.ObtenerCredenciales();
			obj_con = obj_ConDB.ObtenerConexion();
			/******************************
			 * INICIALIZACION DE STATEMENTS
			 *******************************************************/
			try {
				System.out.println("**** Ficheros::Splitter::Configurada conexion  ****");
				ResultSet rs = null;
				stmt = obj_con.prepareStatement("select par1_value from ft_t_PAR1 where PARAMETER_CTXT_TYP = 'STR_SPLIT' and par1_nme = ?");

				if(args.length>=4&&args[3].equals("IP")){	
					stmt.setString(1, "STR_SPLIT_FONDOS");
					rs= stmt.executeQuery();
				}else if(args.length>=4&&args[3].equals("SCFF")){	
					stmt.setString(1, "STR_SPLIT_SCFF");
					rs= stmt.executeQuery();
				}else if(args.length>=4&&args[3].equals("GENERICO_CTP")){	
					stmt.setString(1, "STR_SPLIT_GENERICO_CTP");
					rs= stmt.executeQuery();
				}else{										
					str_split = ";";			
				}
				//rs= stmt.executeQuery();
				if (rs!=null&&rs.next()) {	str_split = rs.getString("PAR1_VALUE");			} 			
				System.out.println("**** Ficheros::Splitter::Obteniendo splitter " + str_split);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			System.out.println("**** Ficheros::Splitter::Problema al establecer conexion  ****");
			e.printStackTrace();
		}
		/**
		 * Cierre de la conexión con la base de datos.
		 */
		try {
			if (stmt != null) {
				stmt.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			if (obj_con != null) {
				obj_con.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
