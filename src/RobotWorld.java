import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PImage;

import java.util.LinkedList;
import java.util.Hashtable;
import java.util.Random;
import java.util.PriorityQueue;
import java.io.File;

/**
 * @author Verónica Arriola
 * @author Jonathan Andrade
 */
public class RobotWorld extends PApplet {

    PFont fuente;               // Fuente para mostrar texto en pantalla
    int tamanioMosaico = 50;    // Tamanio de cada mosaico en pixeles
    int columnas = 21;
    int renglones = 15;
    PImage face;

    Mapa mapa;
    boolean expande = false;    // Bandera para solicitar la expansión del siguiente nodo.
    Robot robot;
    Random r;
    
    public void settings() {
        size(columnas * tamanioMosaico, renglones * tamanioMosaico);
        //String path = RobotWorld.class.getClassLoader("").getPath();
        // File file = new File(path);
        //System.out.println(path);
        //face = loadImage(path +  "robot.png");
    }


    /** Configuracion inicial */
    @Override
    public void setup(){
        frameRate(5);
            //size(columnas * tamanioMosaico, renglones * tamanioMosaico + 70);
        background(50);
        r =  new Random();
        fuente = createFont("Arial",12,true);
        textFont(fuente, 12);
        mapa = new Mapa(columnas, renglones);

        generaObstaculos(15, 8);
        robot = new Robot();
        actulizaCreencia();

    }

    /** Dibuja la imagen en cada ciclo */
    @Override
    public void draw() {

        if (expande) {
            System.out.println("Aqui se debe mover");
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
                        stroke(200,200,200); fill(200,200,200); break;
                    case ROBOT:
                        //image(face, robot.pos.x, robot.pos.y, tamanioMosaico, tamanioMosaico);
                        fill(0,200,0); 
                        break;
                    case ESTADO_FINAL:
                        stroke(200,0,0); fill(200,0,0); break;
                }
                rect(j*tamanioMosaico, i*tamanioMosaico, tamanioMosaico, tamanioMosaico);

                fill(0);
                switch (s) {
                    case CALCULADO:
                        text("d=" + m.distancia, j*tamanioMosaico+4, i*tamanioMosaico + 15);
                        ellipse((float)((0.5 + j) * tamanioMosaico), (float)((0.5 + i) * tamanioMosaico), (float)10, (float)10);
                        continue;
                }
            }
        }
    }

    
    @Override
    public void mouseClicked() {
        expande = true;
    }

    public void generaObstaculos(int nObstaculos, int maxSize) {
        Random r =  new Random();
        int celdasOcupadas = 0;
        for (int i = 0; i < nObstaculos; ++i) {

            int startX = r.nextInt(columnas);
            int startY = r.nextInt(renglones);
            int size = r.nextInt(maxSize) + 1;

            startX = (startX + size) > columnas - 1 ? columnas - size - 1 : startX;
            startY = (startY + size) > renglones - 1 ? renglones - size - 1: startY;
            
            for (int j = 0; j < size; ++j) {
                boolean way = r.nextBoolean(); 
                if (way) {
                    mapa.mundo[++startY][startX].tipo = Tipo.OBSTACULO;
                }  else {
                    mapa.mundo[startY][startX++].tipo = Tipo.OBSTACULO;
                }
                celdasOcupadas++;
            }
        }
        mapa.totalObstaculos = celdasOcupadas;
    }

    public void actulizaCreencia() {
        float creencia = 1.0f / ((mapa.totalCeldas - mapa.totalObstaculos) * 1.0f);
        for (Mosaico[] row : mapa.mundo) {
            for (Mosaico m : row) {
                m.creencia = creencia;
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
        float creencia;
        float distancia;

        Mosaico(int renglon, int columna, Mapa mapa){
            this.renglon = renglon;
            this.columna = columna;
            this.mapa = mapa;
            this.creencia = 0;
        }

        /**
        * Devuelve una referencia al mosaico del mapa a donde se movería el agente
        * con la acción indicada.
        */
        Mosaico aplicaDireccion(Direccion a){
            Mosaico vecino;
            switch(a) {
                case N:
                    if(renglon > 0) {
                    vecino = mapa.mundo[renglon - 1][columna];
                    } else return null;
                    break;
                case S:
                    if(renglon < mapa.renglones - 1) {
                    vecino = mapa.mundo[renglon + 1][columna];
                    } else return null;
                    break;
                case O:
                    if(columna > 0) {
                    vecino = mapa.mundo[renglon][columna - 1];
                    } else return null;
                    break;
                case E:
                    if(columna < mapa.columnas - 1) {
                    vecino = mapa.mundo[renglon][columna + 1];
                    } else return null;
                    break;
                case NO:
                    if(renglon > 0 && columna > 0) {
                    vecino = mapa.mundo[renglon - 1][columna - 1];
                    } else return null;
                    break;
                case NE:
                    if(renglon < mapa.renglones - 1 && columna > 0) {
                    vecino = mapa.mundo[renglon + 1][columna - 1];
                    } else return null;
                    break;
                case SO:
                    if(renglon > 0 && columna < mapa.columnas - 1) {
                    vecino = mapa.mundo[renglon - 1][columna + 1];
                    } else return null;
                    break;
                case SE:
                    if(renglon < mapa.renglones - 1 && columna < mapa.columnas - 1) {
                    vecino = mapa.mundo[renglon + 1][columna + 1];
                    } else return null;
                    break;
                default:
                    throw new IllegalArgumentException("Acción inválida" + a);
            }
            return vecino;
        }
    }

    // --- Clase Mapa
    class Mapa {
        int columnas, renglones;
        int totalCeldas;
        int totalObstaculos; 
        Mosaico[][] mundo;

        Mapa(int columnas, int renglones) {
            this.columnas = columnas;
            this.renglones = renglones;
            totalCeldas = columnas * renglones;
            mundo = new Mosaico[renglones][columnas];
            for(int i = 0; i < renglones; i++)
                for(int j = 0; j < columnas; j++)
                  mundo[i][j] = new Mosaico(i, j, this);
        }

    }

    // --- Clase nodo de búsqueda
    class NodoBusqueda implements Comparable<NodoBusqueda> {
        NodoBusqueda padre;  // Nodo que generó a este nodo.
        Direccion accionPadre;  // Acción que llevó al agente a este nodo.
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
            for(Direccion a : Direccion.values()) {
                sucesor = estado.aplicaDireccion(a);
                if(sucesor != null) {
                    nodoSucesor = new NodoBusqueda(sucesor);
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

    class Posicion {
        int x;
        int y;
        int angulo;
    }

    class Robot {
        Posicion pos;
        float sensorOdometrico;
        float laser;

        Robot () {
            Random r = new Random();
            int startX = r.nextInt(columnas);
            int startY = r.nextInt(renglones);
            boolean posOk  = mapa.mundo[startY][startX].tipo != Tipo.OBSTACULO;
            pos = new Posicion();

            while (!posOk) {
                startX = r.nextInt(columnas);
                startY = r.nextInt(renglones);
                posOk  = mapa.mundo[startY][startX].tipo != Tipo.OBSTACULO;
            }
            mover(startX, startY);
        }

        Robot (int x, int y) {
            sensorOdometrico = laser = 0;
            mover(x, y);
        }

        void mover(int x, int y) {
            mover(x,y,0);
        }

        void mover(int x, int y, int angulo) {
            pos.x = x;
            pos.y = y;
            pos.angulo = angulo;
            mapa.mundo[y][x].tipo = Tipo.ROBOT;
            actualizaDistancias(x, y);
        }

        /* 
        debemos calculas las distancias en linea recta
        sobre las 8 direcciones a encotrar los obstaculos y almacenamos
        la informacion sobre las celdas
        */
        void actualizaDistancias(int x, int y) {
            Mosaico robot = mapa.mundo[y][x];            
            for(Direccion dir: Direccion.values()) {
                // buscamos el proximo obstaculo
                buscaObstaculo(robot, dir, 0);
            }
        }

        /*
         Agregamos el costo de haber llegado hasta aquí dentro de 
         las celdas de los mosaicos
        */
        void buscaObstaculo(Mosaico m , Direccion dir, float distancia) {
            if (m ==  null)
                return;

            float nd = distancia + tamanioMosaico * dir.distancia();

            if (m.tipo == Tipo.OBSTACULO) {
                m.distancia = nd;
                m.situacion = Situacion.CALCULADO;
                System.out.println("nd: "+nd + m.columna + " ," + m.renglon + " " + m.tipo);
            } else {                buscaObstaculo(m.aplicaDireccion(dir), dir, nd);
            }
        }
    }

    static public void main(String args[]) {
        PApplet.main(new String[] { "RobotWorld" });
    }

}
