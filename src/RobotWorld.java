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

    Mapa mapa;                  // El mapa de la habitación
    boolean expande = false;    // Bandera para solicitar la expansión del siguiente nodo.
    Robot robot;                // Nuestro agente
    Algoritmo algoritmo;        // Instancia del algoritmo de Localizacion
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

        background(50);
        r =  new Random();
        fuente = createFont("Arial",11,true);
        textFont(fuente, 11);
        mapa = new Mapa(columnas, renglones);

        generaObstaculos(15, 8);
        robot = new Robot();
        algoritmo =  new Algoritmo();
        algoritmo.inicializa();
        algoritmo.calculaDistancias();
    }

    /** Dibuja la imagen en cada ciclo */
    @Override
    public void draw() {

        if (expande) {
            System.out.println("Aqui se debe mover");
            algoritmo.actualizaCreenciaEstatico();
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
                        stroke(100,100,100); fill(100,100,100); break;
                    case ROBOT:
                        //image(face, robot.pos.x, robot.pos.y, tamanioMosaico, tamanioMosaico);
                        fill(0,200,0); 
                        break;
                    case ESTADO_FINAL:
                        stroke(200,0,0); fill(200,0,0); break;
                }
                rect(j*tamanioMosaico, i*tamanioMosaico, tamanioMosaico, tamanioMosaico);

                switch (s) {
                    case CALCULADO:
                        fill(0);
                        text("B=" + m.creencia, j*tamanioMosaico+4, i*tamanioMosaico+15);
                        break;
                    default :
                        fill(200);
                        text("B=" + m.creencia, j*tamanioMosaico+4, i*tamanioMosaico+15);
                        break;    
                }
            }
        }
    }

    
    @Override
    public void mouseClicked() {
        expande = true;
    }

    /* 
    * Generamos obstaculos aleatorios dado un numero de obstaculos a
    * generar y un tamaño maximo para cada obstaculo
    */
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

    /*
    * Clase que implementa el algoritmo de localizacion
    */
    class Algoritmo {

        double sigma;
        double ruidoOdometro;
        double ruidoGiro;

        Algoritmo() {
            ruidoOdometro = ruidoGiro = sigma = Math.random();
        }

        /* Inicializamos la creencia de todas las celdas*/
       public void inicializa() {
           double creencia = 1.0f / ((mapa.totalCeldas - mapa.totalObstaculos) * 1.0f);
           for (Mosaico[] row : mapa.mundo) {
               for (Mosaico m : row) {
                   m.creencia = creencia;
                   m.creencia = Math.round(m.creencia * 1000.0) / 1000.0;
               }
           }
       }

       /* Actualizamos las disntcias a todas partes del mundo */
        public void calculaDistancias() {
            for (Mosaico[] row : mapa.mundo) {
                for (Mosaico m : row) {
                   for (Direccion d : Direccion.values()) {
                        double dis = distanciaObstaculo(m, d);
                        m.distancias.put(d, dis);
                   }
                }
            }
        }

        /* 
        * Actualizamos la creencia en el caso de que el robot
        * no se haya movido
        */
        public void actualizaCreenciaEstatico() {
            double sOdom = 0;

            for (Mosaico[] row : mapa.mundo) {
                for (Mosaico m : row) {
                   for (Direccion d : Direccion.values()) {
                        double distanciaReal = m.distancias.get(d);
                        double laser = distanciaReal * 0.95;
                        double exponent = -((laser-distanciaReal) * (laser-distanciaReal)) / (2 * sigma * sigma);
                        double lLaser = (1.0f / (Math.sqrt(2 * Math.PI) * sigma))  * Math.pow(Math.E, exponent);

                        m.creencia = m.creencia * lLaser;
                        sOdom += m.creencia;
                   }
                }
            }
            for (Mosaico[] row : mapa.mundo) {
                for (Mosaico m : row) {
                   for (Direccion d : Direccion.values()) {
                        m.creencia = (1 / sOdom) * m.creencia;
                   }
                }
            }
        }

        /* 
        * Actualizamos la creencia en el caso de que el robot
        * no se haya movido
        */
        public void actualizaCreenciaGiro(Direccion d) {
            double sOdom = 0;

            for (Mosaico[] row : mapa.mundo) {
                for (Mosaico m : row) {
                   for (Direccion ddd : Direccion.values()) {
                        double distanciaReal = m.distancias.get(d);
                        double laser = distanciaReal * 0.95;
                        double exponent = -((laser-distanciaReal) * (laser-distanciaReal)) / (2 * sigma * sigma);
                        double lLaser = (1.0f / (Math.sqrt(2 * Math.PI) * sigma))  * Math.pow(Math.E, exponent);

                        m.creencia = m.creencia * lLaser;
                        sOdom += m.creencia;
                   }
                }
            }
            for (Mosaico[] row : mapa.mundo) {
                for (Mosaico m : row) {
                   for (Direccion dd : Direccion.values()) {
                        m.creencia = (1 / sOdom) * m.creencia;
                   }
                }
            }

        }

        /* 
        * Actualizamos la creencia en el caso de que el robot
        * no se haya movido
        */
        public void actualizaCreenciaMovimiento(float delta) {
            double sOdom = 0;

            for (Mosaico[] row : mapa.mundo) {
                for (Mosaico m : row) {
                   for (Direccion d : Direccion.values()) {
                        double distanciaReal = m.distancias.get(d);
                        double laser = distanciaReal * 0.95;
                        double exponent = -((laser-distanciaReal) * (laser-distanciaReal)) / (2 * sigma * sigma);
                        double lLaser = (1.0f / (Math.sqrt(2 * Math.PI) * sigma))  * Math.pow(Math.E, exponent);

                        m.creencia = m.creencia * lLaser;
                        sOdom += m.creencia;
                   }
                }
            }
            for (Mosaico[] row : mapa.mundo) {
                for (Mosaico m : row) {
                   for (Direccion d : Direccion.values()) {
                        m.creencia = (1 / sOdom) * m.creencia;
                   }
                }
            }
        }

    }

    /*
     Agregamos el costo de haber llegado hasta aquí dentro de 
     las celdas de los mosaicos
    */
    void buscaObstaculo(Mosaico m , Direccion dir, double distancia) {
        if (m ==  null)
            return;

        double nd = distancia + tamanioMosaico * dir.distancia();

        if (m.tipo == Tipo.OBSTACULO) {
            m.distancia = nd;
            m.situacion = Situacion.CALCULADO;
            System.out.println("nd: "+nd + m.columna + " ," + m.renglon + " " + m.tipo);
        } else {                
            buscaObstaculo(m.aplicaDireccion(dir), dir, nd);
        }
    } 

    /*
    + Calcula la distancia desde un mosaico libre hasta uno que 
    * sea un obstaculo
    */
    double distanciaObstaculo(Mosaico origen, Direccion dir) {
        if (origen == null)
            return 0;
        if (origen.tipo ==  Tipo.OBSTACULO)
            return dir.distancia();
        else
            return dir.distancia() + distanciaObstaculo(origen.aplicaDireccion(dir), dir);
    }

    // --- Clase Mosaico
    // Representa cada casilla del mundo, corresponde a un estado posible del agente.
    class Mosaico {
        Situacion situacion = Situacion.SIN_VISITAR;
        Tipo tipo = Tipo.VACIO;
        int renglon, columna;  // Coordenadas de este mosaico
        Mapa mapa;             // Referencia al mapa en el que se encuentra este mosaico.
        double creencia;
        double distancia;
        Hashtable<Direccion, Double> distancias = new Hashtable<>();

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

    /* La posicion del robot dentro del cuarto */
    class Posicion {
        int x;
        int y;
        int angulo;
    }

    /* 
    * Clase que implementa el comportamiento del robot
    * 
    */
    class Robot {
        Posicion pos;
        double sensorOdometrico;
        double laser;

        Robot() {
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
        /* actualiza la posicion del robot dentro del cuarto */
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
    }

    static public void main(String args[]) {
        PApplet.main(new String[] { "RobotWorld" });
    }

}
