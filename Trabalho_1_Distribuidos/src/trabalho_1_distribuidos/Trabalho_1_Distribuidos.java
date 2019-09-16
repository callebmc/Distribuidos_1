/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package trabalho_1_distribuidos;

import java.io.IOException;
import java.net.* ;
import java.util.* ;


/**
 *
 * @author Calleb Malinoski
 */
public class Trabalho_1_Distribuidos {

    public static void main(String argv[]) throws Exception
    {
   	 final Processo processo = new Processo();
   	 Scanner scanner = new Scanner(System.in);
   	 System.out.println("Entrando no multicast...");
   	 MulticastSocket multicast = new MulticastSocket(processo.multicastPORT);
   	 processo.group  = InetAddress.getByName(processo.multicastAddres);
   	 multicast.joinGroup(processo.group);
   	 // Comeca a escutar multicast
   	 Thread listenerThread = new Thread(){
   		 @Override
   		 public void run(){
   			 processo.listenMulticast(multicast);
   		 }
   	 };
   	 listenerThread.start();
   	 System.out.println("Entramos!");
   	 boolean running = true;
            	while(running) {
                	String opt = scanner.nextLine();
                	DatagramPacket data = new DatagramPacket(opt.getBytes(),
       			 opt.length(),
       			 processo.group,
       			 processo.multicastPORT);
                	multicast.send(data);
                	if("ok".equals(opt)){
                    	System.out.println("Estou saindo amigos");
                    	multicast.leaveGroup(processo.group);
                    	multicast.close();
                    	running = false;
                	}
   	 }
   	 System.out.println("Encerrando processo...");
   	 processo.running = false;
   	 multicast.close();
   	 scanner.close();
   	 System.out.println("Tchau!");
    }

	static void main() {
    	throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}
}

final class Processo  {
    
    //Propriedas do processo
    public String multicastAddres = "230.0.0.0";
    public int multicastPORT = 6789;
    public InetAddress group;
    final static String CRLF = ",";
    public ArrayList<String> membros = new ArrayList<String>();
    public boolean running = true;
    
    //Método para escutar multicast
    public void listenMulticast(MulticastSocket multicast){
   	 try{
   		 while(this.running) {
   			 byte[] buffer = new byte[10*1024];
                            	DatagramPacket data = new DatagramPacket(buffer, buffer.length);
   			 multicast.receive(data);
   			 String linha = new String(buffer, 0, data.getLength());
   			 String[] mensagem = linha.split(CRLF);
   			 // Trata a mensagem
                            	System.out.println(mensagem[0]);
   		 }
   	 }catch(Exception e){
   	 }
    } 
}



