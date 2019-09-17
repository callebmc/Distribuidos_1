/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package trabalho_1_distribuidos;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.Random;

/**
 *
 * @author Calleb Malinoski
 */
public class Trabalho_1_Distribuidos {

    public static void main(String argv[]) throws Exception {
        final Processo processo = new Processo();
        Scanner scanner = new Scanner(System.in);
        System.out.println("Entrando no multicast...");
        MulticastSocket multicast = new MulticastSocket(processo.multicastPORT);
        processo.group = InetAddress.getByName(processo.multicastAddres);
        multicast.joinGroup(processo.group);
        // Comeca a escutar multicast
        Thread listenerThread = new Thread() {
            @Override
            public void run() {
                processo.listenMulticast(multicast);
            }
        };
        listenerThread.start();
        System.out.println("Entramos!");

//        //Aguarda entrar os 5 processos
//        while (processo.membros.size() < processo.minUsuarios) {
//            System.out.println("Aguardando.... Numero de usuarios: " + processo.membros.size() + " de " + processo.minUsuarios);
//            Thread.sleep(2000);
//        }
        processo.geraId();
        processo.entraGrupo(multicast);

        boolean running = true;
        while (running) {
            //Gera valor aleatório
            processo.geraAleatorio();

            //Menu de opções
            Thread.sleep(2000);
            System.out.println("Status atual");
            System.out.println("O que deseja fazer?");
            System.out.println("1 - Envia Valores 1");
            System.out.println("5 - Sair");
            int opt = Integer.parseInt(scanner.nextLine());
			switch(opt) {
                            case 1:
                                processo.enviaEscolhido(multicast); 
                                break;
                            case 5:
                                System.out.println("Estou saindo amigos");
                                multicast.leaveGroup(processo.group);
                                multicast.close();
                                running = false;
                                break;
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

final class Processo {

    //Propriedas do processo
    public String multicastAddres = "230.0.0.0";
    public int multicastPORT = 6789;
    public InetAddress group;
    final static String CRLF = ",";
    public boolean running = true;
    public int minUsuarios = 5;
    /**
     * **********Propriedades do PhaseKing**************
     */
    //ID do processo
    public int id = 0;
    //ID dos usuários
    public int[] qtdMembros = new int[5];
    //Lista dos usuários
    public ArrayList<String> membros = new ArrayList<String>();
    //Array para receber valores
    public int[] valores = new int[5];
    //Valor a ser enviado
    public int c;

    //Método para escutar multicast
    public void listenMulticast(MulticastSocket multicast) {
        try {
            while (this.running) {
                byte[] buffer = new byte[10 * 1024];
                DatagramPacket data = new DatagramPacket(buffer, buffer.length);
                multicast.receive(data);
                String linha = new String(buffer, 0, data.getLength());
                String[] mensagem = linha.split(CRLF);
                // Trata a mensagem
                System.out.println(mensagem[0]);
                switch(mensagem[0]){
                    case "NovoNoGrupo":
                        this.adicionaNoGrupo(mensagem, multicast);
                        break;
                    case "CompartilharValor":
                        System.out.println(mensagem[1]);
                        break;
                }
            }
        } catch (Exception e) {
        }
    }

    //Método para gerar ID sequencial de 1 a 5
    public void geraId() {
        boolean ok = true;
        int aux = 0;
        while (id == 0 && ok) {
            Random rand = new Random();
            aux = rand.nextInt(5) + 1;
            for (int i = 0; i < 5; i++) {
                if (this.qtdMembros[i] != aux) {
                    this.id = aux;
                    ok = true;
                } else {
                    this.id = 0;
                }
            }
        }
        for (int i = 0; i < 5; i++) {
            if (this.qtdMembros[i] == 0) {
                this.qtdMembros[i] = aux;
                break;
            }
        }
    }

    public void geraAleatorio() {
        //Gera valor entre 0 e 1
        Random rand = new Random();
        this.c = rand.nextInt(2);
    }

    /*********************MÉTODOS PARA ARMAZENAR INFORMAÇÕES COMO ID E CHAVE DE TODOS OS MEMBROS****************/
    public void entraGrupo(MulticastSocket multicast) {
        try {
            String mensagemDeEntrada = "NovoNoGrupo" + CRLF;
            mensagemDeEntrada += this.id;
            DatagramPacket data = new DatagramPacket(mensagemDeEntrada.getBytes(), mensagemDeEntrada.length(), this.group, this.multicastPORT);
            multicast.send(data);
        } catch (Exception e) {
        }
    }
    
    public void adicionaNoGrupo(String[] mensagem, MulticastSocket multicast){
        try{
            for (int i = 0; i < 5; i++) {
            if (this.qtdMembros[i] == 0) {
                this.qtdMembros[i] = Integer.parseInt(mensagem[1]);
                break;
            }
            System.out.println("Adicionado membro " + mensagem[1]);
        }
        }
        catch(Exception e){
        }
    }
    /*********************MÉTODOS PARA ARMAZENAR INFORMAÇÕES COMO ID E CHAVE DE TODOS OS MEMBROS****************/
    
    /*********************MÉTODOS PARA ENVIAR O VALOR ESCOLHIDO PARA TODOS OS MEMBROS***************************/
    public void enviaEscolhido(MulticastSocket multicast){
        try{
            String enviaEscolhido = "CompartilharValor" + CRLF;
            enviaEscolhido += this.c;
            DatagramPacket data = new DatagramPacket(enviaEscolhido.getBytes(), enviaEscolhido.length(), this.group, this.multicastPORT);
            multicast.send(data);
        }
        catch(Exception e){}
    }
    
    
}
