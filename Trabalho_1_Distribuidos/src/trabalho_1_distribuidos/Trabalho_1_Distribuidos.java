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
 * @author Ana Yanaze e Calleb Malinoski
 */
public class Trabalho_1_Distribuidos {

    public static void main(String argv[]) throws Exception {

        /* ana: tive que colocar pra rodar no meu mac, ele espera ipv6 por default, se der zica no seu comenta a linha abaixo*/
        System.setProperty("java.net.preferIPv4Stack", "true");
        //Váriaveis para Máximo de processo com falhas, numero de processos e número de fases
        int max_falhas, n_processos, fases, i = 0;

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

        //Gera ID do processo e um valor V
        processo.geraId();
        processo.geraAleatorio();
        processo.entraGrupo(multicast);
        Thread.sleep(2000);

        //Aguarda entrar os 5 processos
        //utiliza o processo.minUsuarios ou n_processos tantofaz
        while (processo.membros.size() < processo.minUsuarios) {
            System.out.println("Aguardando.... Numero de usuarios: " + processo.membros.size() + " de " + processo.minUsuarios);
            Thread.sleep(5000);
        }

        boolean running = true;

        n_processos = 5; //total de processos por definição. 
        max_falhas = 1; //falhas toleradas
        fases = 1; //quantos reis já foram
        int n_fases = 2;
        int v = 0; //padrão só
        int majority = v, mult = 0, tiebreaker;

        // ******* PHASE KING *********
        while (running) {
            Thread.sleep(2000);

            //elege king pelo maior id;
            int m = 0;

            //PRIMEIRO ROUND - each process sends its estimate to all other processes.
            Iterator membros = processo.membros.iterator();
            //primeiro rei sempre vai ser a posição 
            PK king = processo.membros.get(i); //inicializando variável só pra tirar warning

            //na primeira fase o king sempre vai ser a primeiro posicao pq nao implementamos a chave
            while (membros.hasNext()) {
                PK st = (PK) membros.next();
                //FAZ MULTICAST DOS VALORES 
                processo.fazMulticast(multicast); //faz multicast do ID com valor V
                majority = processo.majoritario();//Para cada processo, verifica qual é o majoritário e retorna

                //Escolhe o próximo King com o maior ID caso tenha alterado de fase, não podendo ser o mesmo da fase anterior 
                if (st.id > m && fases > 1) {
                    m = st.id;              //Pega maior ID
                    king = st;              //Define o rei
                    System.out.println("Maior valor: " + king.id);
                }
            }

            //FIM PRIMEIRO ROUND
            //SEGUNDO ROUND
            //tem que passar os métodos pra classe PK?
            processo.enviaTiebraker(multicast, king.valor_gerado, king.id);

            //FIM SEGUNDO ROUND
            //verifica atingiu o numero maximo de fases
            if (fases >= n_fases) {
                running = false; //sai do loop
                System.out.println("Fase " + fases + ": O valor final do consenso binário nesta fase é " + processo.v);//exibe o consenso
            }

            fases++;
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
    public int mult;
    public int n_falhas = 0;
    public int max_falhas = 1;
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
    //Valor escolhido final
    int v = 0;

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
                switch (mensagem[0]) {
                    case "NovoNoGrupo":     //Mensagem de entrada no multicast
                        this.adicionaNoGrupo(mensagem, multicast);
                        break;
                    case "RespostaDoGrupo": //Resposta da entrada do multicast
                        this.trataRespostaDoGrupo(mensagem, multicast);
                        break;
                    case "CompartilharValor": //Envio dos valores pro consenso
                        this.enviaNovoEscolhido(mensagem, multicast);
                        break;
                    case "ReenviandoValores": //Resposta dos valores do consenso
                        this.recebeEscolhido(mensagem, multicast);
                        break;
                    case "EnviaTiebraker": //Mensagem do rei para o tiebraker
                        this.trataTiebraker(mensagem, multicast);
                        break;

                }
            }
        } catch (Exception e) {
        }
    }

    //Método para gerar ID, gera um valor entre 1 e 100.  
    public void geraId() {
        Random rand = new Random();
        this.id = rand.nextInt(100) + 1;
        Iterator membros = this.membros.iterator();
        while (membros.hasNext()) {
            PK st = (PK) membros.next();
            if (this.id == st.id) //garantindo que eh identificação unica. 
            {
                this.geraId();
            }
        }
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

            // VALOR GERADO
            mensagemDeEntrada += this.c;
            DatagramPacket data = new DatagramPacket(mensagemDeEntrada.getBytes(), mensagemDeEntrada.length(), this.group, this.multicastPORT);
            multicast.send(data);
        } catch (Exception e) {
        }
    }

    //Ao receber que alguem entrou no grupo, salva dentro do ArrayList<PK> os membros do grupo
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
     * *******************MÉTODOS PARA ENVIAR O VALOR ESCOLHIDO PARA TODOS OS
     * MEMBROS**************************
     */
    //Método para verificar qual o mais escolhido
    public int majoritario() {
        int um = 0;
        int zero = 0;

        //Loop para verificar qual ocorre mais vezes
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

        if (um > zero) {
            if (zero > this.max_falhas) {
                System.out.println("HALT");
            }
            this.mult = um;
        } else if (zero > um) {
            if (um > this.max_falhas) {
                System.out.println("HALT!");
            }
            this.mult = zero;
        }

        //Pra atualizar o valor de majo e mult na classe PK do projeto
        Iterator membrosAux = this.membros.iterator();
        while (membrosAux.hasNext()) {
            PK st = (PK) membrosAux.next();
            if (st.id == this.id) {
                if (um > zero) {
                    st.majoritario = 1;
                    st.valor_qtd = um;
                } else {
                    st.majoritario = 0;
                    st.valor_qtd = zero;
                }
            }
        }

        return um > zero ? 1 : 0;
    }

    //ENVIA NOVO VALOR DE 0/1 AO MULTICAST
    //@ param: faz multicast do valor escolhido
    //Método que solicita os membros do grupo compartilharem seus valores
    public void fazMulticast(MulticastSocket multicast) {
        try {
            String enviaEscolhido = "CompartilharValor" + CRLF;
            DatagramPacket data = new DatagramPacket(enviaEscolhido.getBytes(), enviaEscolhido.length(), this.group, this.multicastPORT);
            multicast.send(data);
        } catch (Exception e) {
        }
    }

    //RETORNA A TODOS OS PROCESSOS O NOVO VALOR ESCOLHIDO
    public void enviaNovoEscolhido(String[] mensagem, MulticastSocket multicast) {
        try {
            this.geraAleatorio();
            String enviaEscolhido = "ReenviandoValores" + CRLF;
            enviaEscolhido += this.id + CRLF;
            enviaEscolhido += this.c + CRLF;
            DatagramPacket data = new DatagramPacket(enviaEscolhido.getBytes(), enviaEscolhido.length(), this.group, this.multicastPORT);
            multicast.send(data);
        } catch (Exception e) {
        }
    }

    //Atualiza os valores novos
    public void recebeEscolhido(String[] mensagem, MulticastSocket multicast) {
        try {
            Iterator membros = this.membros.iterator();
            while (membros.hasNext()) {
                PK st = (PK) membros.next();
                if (st.id == Integer.parseInt(mensagem[1])) {
                    st.valor_gerado = Integer.parseInt(mensagem[2]);
                }
            }
        } catch (Exception e) {
        }
    }

    //Método que vai enviar a mensagem do tiebraker apenas se o ID do processo for igual o processo rei
    public void enviaTiebraker(MulticastSocket multicast, int kingTie, int kingId) {
        try {
            if (this.id == kingId) {
                System.out.println("Sou o Rei" + this.id + "  " + kingTie);
                String tiebraker = "EnvioTiebraker" + CRLF;
                tiebraker += kingTie + CRLF;
                DatagramPacket data = new DatagramPacket(tiebraker.getBytes(), tiebraker.length(), this.group, this.multicastPORT);
                multicast.send(data);
            }
        } catch (Exception e) {
        }
    }

    //Faz a parte da fase 2, onde verifica se é maioria ou se precisa do tiebraker
    public void trataTiebraker(String[] mensagem, MulticastSocket multicast) {
        try {
            int n_processos = 5;
            //verifica se está dentro do numero tolerável de falhas
            if (this.mult > ((n_processos / 2) + max_falhas)) {
                this.v = this.majoritario();
            } else {
                this.v = Integer.parseInt(mensagem[1]);
            }
        } catch (Exception e) {
        }
    }
}

class PK {

    //ID dos usuários
    int id;
    int position;
    //Chave Pública
    //String chavePublica;
    //Chave Privada
    //String chavePrivada;
    //Valor a ser enviado
    int valor_gerado;
    int valor_qtd = 0;
    int majoritario = 0;

    PK(int id, int c) {
        this.id = id;
        this.valor_gerado = c;
    }

}
