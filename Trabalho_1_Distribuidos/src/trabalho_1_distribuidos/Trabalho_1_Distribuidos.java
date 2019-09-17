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

        processo.geraId();
        processo.geraAleatorio();
        processo.entraGrupo(multicast);
        //Aguarda entrar os 5 processos
        while (processo.membros.size() < processo.minUsuarios) {
            System.out.println("Aguardando.... Numero de usuarios: " + processo.membros.size() + " de " + processo.minUsuarios);
            Thread.sleep(5000);
        }

        boolean running = true;
        while (running) {
            //Menu de opções
            Thread.sleep(2000);
            //ACHO QUE NÃO VAI TER ESSE MENU, VAI SER AUTOMÁTICO QUANDO OS 5 ENTRAREM
            //VAI SER UM FOR MALUCO
            System.out.println("Status atual");
            System.out.println("O que deseja fazer?");
            System.out.println("1 - Envia Valores 1");
            System.out.println("2 - Ve o majoritario");
            System.out.println("5 - Sair");
            int opt = Integer.parseInt(scanner.nextLine());
            switch (opt) {
                case 1:
                    processo.enviaEscolhido(multicast);
                    break;
                case 2:
                    System.out.println("O valor maior foi " + processo.majoritario());
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
    int id = 0;
    //Lista dos usuários
    public ArrayList<PK> membros = new ArrayList<PK>();
    //Array para receber valores
    public int[] valores = new int[5];
    //Valor gerado 1 ou 0
    int c;

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
                switch (mensagem[0]) {
                    case "NovoNoGrupo":
                        this.adicionaNoGrupo(mensagem, multicast);
                        break;
                    case "RespostaDoGrupo":
                        this.trataRespostaDoGrupo(mensagem, multicast);
                        break;
                    case "CompartilharValor":
                        System.out.println(mensagem[1]);
                        break;
                }
            }
        } catch (Exception e) {
        }
    }

    //Método para gerar ID  
    public void geraId() {
        Random rand = new Random();
        this.id = rand.nextInt(100) + 1;
    }

    //Método para gerar valor do Consenso
    public void geraAleatorio() {
        //Gera valor entre 0 e 1
        Random rand = new Random();
        this.c = rand.nextInt(2);
    }

    /**
     * *******************MÉTODOS PARA ARMAZENAR INFORMAÇÕES COMO ID E CHAVE DE
     * TODOS OS MEMBROS***************
     */
    //Método para informar que entrou no multicast
    public void entraGrupo(MulticastSocket multicast) {
        try {
            String mensagemDeEntrada = "NovoNoGrupo" + CRLF;
            // ORIGEM
            mensagemDeEntrada += this.id + CRLF;
            // VALOR GERADo
            mensagemDeEntrada += this.c;
            DatagramPacket data = new DatagramPacket(mensagemDeEntrada.getBytes(), mensagemDeEntrada.length(), this.group, this.multicastPORT);
            multicast.send(data);
        } catch (Exception e) {
        }
    }

    public void adicionaNoGrupo(String[] mensagem, MulticastSocket multicast) {
        try {
            PK pk1 = new PK(Integer.parseInt(mensagem[1]), Integer.parseInt(mensagem[2]));
            this.membros.add(pk1);

            String resposta = "RespostaDoGrupo" + CRLF;
            //Destinatário
            resposta += mensagem[1] + CRLF;
            //Conteudo
            resposta += this.id + CRLF;
            resposta += this.c + CRLF;
            //Origem
            resposta += this.id;
            DatagramPacket data = new DatagramPacket(resposta.getBytes(), resposta.length(), this.group, this.multicastPORT);
            multicast.send(data);
            System.out.println("Adicionado membro " + mensagem[1]);
        } catch (Exception e) {
        }
    }

    //Método para adicionar na lista se não for ele mesmo que enviou
    public void trataRespostaDoGrupo(String[] mensagem, MulticastSocket multicast) {
        try {
            if (Integer.parseInt(mensagem[1]) == (this.id) && Integer.parseInt(mensagem[4]) != this.id) {
                PK pk1 = new PK(Integer.parseInt(mensagem[2]), Integer.parseInt(mensagem[3]));
                this.membros.add(pk1);
            }
        } catch (Exception e) {
        }
    }

    /**
     * *******************MÉTODOS PARA ARMAZENAR INFORMAÇÕES COMO ID E CHAVE DE
     * TODOS OS MEMBROS***************
     */

    /**
     * *******************MÉTODOS PARA ENVIAR O VALOR ESCOLHIDO PARA TODOS OS MEMBROS**************************
     */
    public void enviaEscolhido(MulticastSocket multicast) {
        try {
            String enviaEscolhido = "CompartilharValor" + CRLF;
            enviaEscolhido += this.c;
            DatagramPacket data = new DatagramPacket(enviaEscolhido.getBytes(), enviaEscolhido.length(), this.group, this.multicastPORT);
            multicast.send(data);
        } catch (Exception e) {
        }
    }

    public void armazenaEscolhid(String[] mensagem, MulticastSocket multicast) {
    }

    //Método para verificar qual o mais escolhid
    public int majoritario() {
        int um = 0;
        int zero = 0;

        Iterator membros = this.membros.iterator();
        while (membros.hasNext()) {
            PK st = (PK) membros.next();
            if (st.valor_gerado == 1) {
                um++;
            }
            if (st.valor_gerado == 0) {
                zero++;
            }
        }
        return um > zero? 1 : 0;
    }
}

class PK {

    //ID dos usuários
    int id;
    //Chave Pública
    //String chavePublica;
    //Chave Privada
    //String chavePrivada;
    //Valor a ser enviado
    int valor_gerado;

    PK(int id, int c) {
        this.id = id;
        this.valor_gerado = c;
    }
}
