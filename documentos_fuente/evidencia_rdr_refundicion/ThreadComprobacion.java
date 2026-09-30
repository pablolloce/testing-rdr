package entities;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.util.ArrayList;
import jdbc.Querys;

public class ThreadComprobacion implements Runnable {
   Connection connection;
   Querys jdbc = new Querys();
   String rutaFichero;

   public ThreadComprobacion(String rutaFichero) throws Exception {
      this.rutaFichero = rutaFichero;
   }

   public void run() {
      boolean condicion = true;

      while(condicion) {
         this.insertarRLT1();
         if (Querys.fin) {
            condicion = false;
         }
      }

   }

   private synchronized void insertarRLT1() {
      ArrayList<String> inserciones = Querys.insercionesRLT1;
      String queryInsercionRLT1 = "";
      synchronized(inserciones) {
         try {
            if (inserciones != null && !inserciones.isEmpty() && inserciones.size() > 0 && inserciones.get(0) != null) {
               queryInsercionRLT1 = (String)inserciones.get(0);
               inserciones.remove(0);
               Querys.insercionesRLT1 = inserciones;
               this.jdbc.ejecutarQuery(this.connection, queryInsercionRLT1);
               queryInsercionRLT1 = queryInsercionRLT1.concat(";");

               try {
                  String rutaOrigen = this.rutaFichero.concat("/InsercionesRLT1.txt");
                  FileOutputStream fos = new FileOutputStream(rutaOrigen, true);
                  PrintWriter pw = new PrintWriter(fos);
                  pw.println(queryInsercionRLT1);
                  pw.flush();
                  pw.close();
                  fos.close();
               } catch (FileNotFoundException var7) {
                  var7.printStackTrace();
               } catch (IOException var8) {
                  var8.printStackTrace();
               }
            }
         } catch (Exception var9) {
            System.out.println("Excepcion en insertarRLT1: " + var9);
         }

      }
   }
}
