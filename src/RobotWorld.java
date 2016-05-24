import processing.core.PApplet;
import processing.core.PFont;

import java.util.LinkedList;
import java.util.Hashtable;
import java.util.Random;
import java.util.PriorityQueue;

/**
 * @author Verónica Arriola
 * @author Jonathan Andrade
 */
public class RobotWorld extends PApplet {

    PFont fuente;               // Fuente para mostrar texto en pantalla
    int tamanioMosaico = 50;    // Tamanio de cada mosaico en pixeles
    int columnas = 15;
    int renglones = 15;

    Mapa mapa;
    boolean expande = false;    // Bandera para solicitar la expansión del siguiente nodo.
    Algoritmo algoritmo;
    Robot robot;
    Random r;
    
    public void settings() {
        size(columnas * tamanioMosaico, renglones * tamanioMosaico);
    }


    /** Configuracion inicial */
    @Override
    public void setup(){
        //size(columnas * tamanioMosaico, renglones * tamanioMosaico + 70);
        background(50);
        r =  new Random();
        fuente = createFont("Arial",12,true);
        textFont(fuente, 12);
        mapa = new Mapa(columnas, renglones);
        generaObstaculos(8, 4);


        algoritmo = new Algoritmo();

        int startX = r.nextInt(columnas);
        int startY = r.nextInt(renglones);
        robot = new Robot(startX, startY);

    }

    /** Dibuja la imagen en cada ciclo */
    @Override
    public void draw(){
        if (expande) {
            algoritmo.expandeNodoSiguiente();
            expande = false;
        }

        Mosaico m;
        Situacion s;

        for(int i = 0; i < renglones; i++){
            for(int j = 0; j < columnas; j++){
                m = mapa.mundo[i][j];
                s = m.situacion;
                // Dibujar cuadro
                switch(s) {
                    case SIN_VISITAR:
                        stroke(0); fill(50); break;
                    case EN_LISTA_CERRADA:
                        stroke(0); fill(200,200,0); break;
                    case EN_LISTA_ABIERTA:
                        stroke(0); fill(0,200,200); break;
                    case ACTUAL:
                        stroke(0); fill(150,0,150); break;
                    case EN_SOLUCION:
                        stroke(255); fill(0,0,100); break;
                    default:
                        stroke(0); fill(0);
                }
                switch(m.tipo) {
                    case OBSTACULO:
                        stroke(0); fill(200); break;
                    case ESTADO_INICIAL:
                        stroke(0,200,0); fill(0,200,0); break;
                    case ESTADO_FINAL:
                        stroke(200,0,0); fill(200,0,0); break;
                }
                rect(j*tamanioMosaico, i*tamanioMosaico, tamanioMosaico, tamanioMosaico);
            }
        }
    }

    
    @Override
    public void mouseClicked() {
        expande = true;
    }

    public void generaObstaculos(int nObstaculos, int maxSize) {
        Random r =  new Random();

        for (int i = 0; i < nObstaculos; ++i) {

            int startX = r.nextInt(columnas);
            int startY = r.nextInt(renglones);
            int size = r.nextInt(maxSize) + 1;

            startX = (startX + size) > columnas - 1 ? columnas - size - 1 : startX;
            startY = (startY + size) > renglones - 1 ? renglones - size - 1: startY;
            
            for (int j = 0; j < size; ++j) {
                boolean way = r.nextBoolean(); 
                if (way) {
                    mapa.mundo[startY++][startX].tipo = Tipo.OBSTACULO;
                }  else {
                    mapa.mundo[startY][startX++].tipo = Tipo.OBSTACULO;
                }
            }
        }
    }

    // --- Clase Mosaico
    // Representa cada casilla del mundo, corresponde a un estado posible del agente.
    class Mosaico {
        Situacion situacion = Situacion.SIN_VISITAR;
        Tipo tipo = Tipo.VACIO;
        int renglon, columna;  // Coordenadas de este mosaico
        int gn;                // Distancia que ha tomado llegar hasta aquí.
        int hn;                // Distancia estimada a la meta.
        Mosaico padre;         // Mosaico desde el cual se ha llegado.
        Mapa mapa;             // Referencia al mapa en el que se encuentra este mosaico.

        Mosaico(int renglon, int columna, Mapa mapa){
            this.renglon = renglon;
            this.columna = columna;
            this.mapa = mapa;
        }

        /** Devuelve el valor actual de fn. */
        int fn() {
            return gn + hn;
        }

        /** Calcula la distancia Manhattan a la meta. */
        void calculaHeuristica(Mosaico meta) {            
            int deltaX = Math.abs(renglon - meta.renglon);
            int deltaY = Math.abs(columna - meta.columna);
            hn = (deltaX + deltaY) * 10;
        }

        /**
        * Devuelve una referencia al mosaico del mapa a donde se movería el agente
        * con la acción indicada.
        */
        Mosaico aplicaAccion(Accion a){
            Mosaico vecino;
            switch(a) {
                case MOVE_UP:
                    if(renglon > 0) {
                    vecino = mapa.mundo[renglon - 1][columna];
                    } else return null;
                    break;
                case MOVE_DOWN:
                    if(renglon < mapa.renglones - 1) {
                    vecino = mapa.mundo[renglon + 1][columna];
                    } else return null;
                    break;
                case MOVE_LEFT:
                    if(columna > 0) {
                    vecino = mapa.mundo[renglon][columna - 1];
                    } else return null;
                    break;
                case MOVE_RIGHT:
                    if(columna < mapa.columnas - 1) {
                    vecino = mapa.mundo[renglon][columna + 1];
                    } else return null;
                    break;
                case MOVE_NW:
                    if(renglon > 0 && columna > 0) {
                    vecino = mapa.mundo[renglon - 1][columna - 1];
                    } else return null;
                    break;
                case MOVE_NE:
                    if(renglon < mapa.renglones - 1 && columna > 0) {
                    vecino = mapa.mundo[renglon + 1][columna - 1];
                    } else return null;
                    break;
                case MOVE_SW:
                    if(renglon > 0 && columna < mapa.columnas - 1) {
                    vecino = mapa.mundo[renglon - 1][columna + 1];
                    } else return null;
                    break;
                case MOVE_SE:
                    if(renglon < mapa.renglones - 1 && columna < mapa.columnas - 1) {
                    vecino = mapa.mundo[renglon + 1][columna + 1];
                    } else return null;
                    break;
                default:
                    throw new IllegalArgumentException("Acción inválida" + a);
            }
            if (vecino.tipo == Tipo.OBSTACULO) return null;
            else return vecino;
        }
    }

    // --- Clase Mapa
    class Mapa {
        int columnas, renglones;
        Mosaico[][] mundo;

        Mapa(int columnas, int renglones) {
            this.columnas = columnas;
            this.renglones = renglones;
            mundo = new Mosaico[renglones][columnas];
            for(int i = 0; i < renglones; i++)
                for(int j = 0; j < columnas; j++)
                  mundo[i][j] = new Mosaico(i, j, this);
        }

    }

    // --- Clase nodo de búsqueda
    class NodoBusqueda implements Comparable<NodoBusqueda> {
        NodoBusqueda padre;  // Nodo que generó a este nodo.
        Accion accionPadre;  // Acción que llevó al agente a este nodo.
        Mosaico estado;      // Refencia al estado al que se llegó.
        int gn;              // Costo de llegar hasta este nodo.

        NodoBusqueda(Mosaico estado) {
            this.estado = estado; 
        }

        /** Asume que hn ya fue calculada. */
        int getFn() {
            return gn + estado.hn;
        }

        /** Calcula los nodos de búsqueda sucesores. */
        LinkedList<NodoBusqueda> getSucesores() {
            LinkedList<NodoBusqueda> sucesores = new LinkedList();
            Mosaico sucesor;
            NodoBusqueda nodoSucesor;
            for(Accion a : Accion.values()) {
                sucesor = estado.aplicaAccion(a);
                if(sucesor != null) {
                    nodoSucesor = new NodoBusqueda(sucesor);
                    nodoSucesor.gn = this.gn + a.costo();
                    nodoSucesor.padre = this;
                    nodoSucesor.accionPadre = a;
                    sucesores.add(nodoSucesor);
                }
            }
            return sucesores;
        }

        public int compareTo(NodoBusqueda nb){
            return getFn() - nb.getFn();
        }

        /** En la lista abierta se considera que dos nodos son iguales si se refieren al mismo estado. */
        public boolean equals(Object o) {
            NodoBusqueda otro = (NodoBusqueda)o;
            return estado.equals(otro.estado);
        }
    }

    class Robot {
        int x;
        int y;
        float sensorOdometrico;
        float laser;

        Robot (int x, int y) {
            sensorOdometrico = laser = 0;
            mover(x, y);
        }

        void mover(int x, int y) {
            this.x = x;
            this.y = y;
            mapa.mundo[y][x].tipo = Tipo.ESTADO_INICIAL;
            actualizaSensor(x, y);
        }

        void actualizaSensor(int x, int y) {

        }
    }

    // --- A*
    class Algoritmo {
        private PriorityQueue<NodoBusqueda> listaAbierta;
        private Hashtable<Mosaico, Mosaico> listaCerrada;
        Mosaico estadoFinal;  // Referencia al mosaico meta.
        boolean resuelto;

        NodoBusqueda nodoActual;
        NodoBusqueda nodoPrevio;

        void inicializa(Mosaico estadoInicial, Mosaico estadoFinal) {
            resuelto = false;
            this.estadoFinal = estadoFinal;
            // aqui deben incializar sus listas abierta y cerrada
            listaAbierta = new PriorityQueue<NodoBusqueda>();
            listaCerrada = new Hashtable<Mosaico, Mosaico>();
            estadoInicial.calculaHeuristica(estadoFinal);
            estadoInicial.tipo = Tipo.ESTADO_INICIAL;
            estadoFinal.tipo = Tipo.ESTADO_FINAL;

            nodoPrevio = new NodoBusqueda(estadoInicial);
            listaAbierta.offer(nodoPrevio);
        }

        void expandeNodoSiguiente() {
            if (resuelto || listaAbierta.isEmpty())
                return;

            NodoBusqueda nodoActual  = listaAbierta.poll();
            listaCerrada.put(nodoActual.estado, nodoActual.estado);
            nodoActual.estado.situacion = Situacion.EN_LISTA_CERRADA;

            // /* generar a sus sucesores */
            LinkedList<NodoBusqueda> vecinos = nodoActual.getSucesores();
            for (NodoBusqueda n : vecinos ) {
                if (n.estado.situacion != Situacion.EN_LISTA_CERRADA) {
                    if (n.estado.situacion != Situacion.EN_LISTA_ABIERTA) {
                        n.estado.calculaHeuristica(estadoFinal);
                        n.padre = nodoActual;
                        listaAbierta.add(n);
                        n.estado.situacion = Situacion.EN_LISTA_ABIERTA;
                    } else {
                        if (n.gn < nodoActual.gn) {
                            listaAbierta.remove(n);
                            n.padre = nodoActual;
                            n.estado.calculaHeuristica(estadoFinal);
                            listaAbierta.add(n);
                        }
                    }
                }
            }

            if (estadoFinal.situacion == Situacion.EN_LISTA_CERRADA) {
                resuelto =  true;
                NodoBusqueda tmp = nodoActual;
                while (tmp != null) {
                    tmp.estado.situacion = Situacion.EN_SOLUCION;
                    tmp = tmp.padre;
                }
                return;
            }
        }
    }


    static public void main(String args[]) {
        PApplet.main(new String[] { "RobotWorld" });
    }

}
