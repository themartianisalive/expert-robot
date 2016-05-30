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
	boolean mueve = false;    // Bandera para solicitar la expansión del siguiente nodo.
	Robot robot;                // Nuestro agente
	Algoritmo algoritmo;        // Instancia del algoritmo de Localizacion
	Random r;
	int clicks = 0;
	final int UP = 38;
	final int DOWN = 40;
	final int LEFT = 37;
	final int RIGH = 39;
	
	public void settings() {
		size(columnas * tamanioMosaico, renglones * tamanioMosaico+70);
	}

	/** Configuracion inicial */
	@Override
	public void setup(){
		frameRate(15);

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
		try {
			if (clicks == 1) {
				algoritmo.actualizaCreenciaEstatico();
				clicks++;
			} else if (mueve) {
				int avanza = r.nextInt(10);
				if (avanza <= 10) {
					int gira = r.nextInt(10);
					if (gira <= 2) {
						robot.giraRandom();
					} else {
						robot.avanzaRandom();
					}
				} else {
					algoritmo.actualizaCreenciaEstatico();
				}
				mueve = false;
			}
		} catch (Exception e) {
			e.printStackTrace();
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
						text("B=" + m.creencia , j*tamanioMosaico+4, i*tamanioMosaico+15);
						break;
					default :
						fill(200);
						text("B=" + m.creencia , j*tamanioMosaico+4, i*tamanioMosaico+15);
						break;    
				}
			}
		}

		fill(0);
		textFont(fuente, 12);

		rect(0, renglones * tamanioMosaico, columnas *  tamanioMosaico, 70);

		fill(255);
		text("Ang :" + robot.pos.angulo + "g", 30, renglones * tamanioMosaico + 30);
		text("Pos: " + robot.pos.x + "," + robot.pos.y, 30, renglones * tamanioMosaico + 50);

        fill(0,200,0);
        rect(2 * tamanioMosaico, renglones * tamanioMosaico + 10, 20, 20);
        fill(255);
        text("Robot", 2 * tamanioMosaico + 30, renglones * tamanioMosaico + 30);


		fill(100);
		rect(2 * tamanioMosaico, renglones * tamanioMosaico + 30, 20, 20);
		fill(255);
		text("obstaculos", 2 * tamanioMosaico + 30, renglones * tamanioMosaico + 50);

		fill(150,0,150);
		rect(4 * tamanioMosaico, renglones * tamanioMosaico + 10, 20, 20);
		fill(255);
		text("Nodo actual", 4 * tamanioMosaico + 30, renglones * tamanioMosaico + 30);

		fill(0,0,100);
		rect(4 * tamanioMosaico, renglones * tamanioMosaico + 30, 20, 20);
		fill(255);
		text("Solución", 4 * tamanioMosaico + 30, renglones * tamanioMosaico + 50);

	}

	
	@Override
	public void mouseClicked() {
		mueve = true;
		clicks++;
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
			ruidoOdometro = Math.random();
			ruidoGiro = Math.random();
			sigma = Math.random(); 
		}

		/* Inicializamos la creencia de todas las celdas */
	   public void inicializa() {
		   double creencia = 1.0f / ((mapa.totalCeldas - mapa.totalObstaculos) * 1.0f);
		   for (Mosaico[] row : mapa.mundo) {
			   for (Mosaico m : row) {
			   		for (Direccion d : Direccion.values()) {
			   			m.creencias.put(d, creencia);
			   		}
				   m.creencia = creencia;
			   }
		   }
	   }

	   /* Actualizamos las distancias a todas partes del mundo */
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
					double mediaCreencia = 0;
				    for (Direccion d : Direccion.values()) {
						double distanciaReal = m.distancias.get(d);
						/* le agregamos ruido a la lectura de la distancia */
						double laser = distanciaReal * (Math.random() * 2);
						double exponent = -((laser-distanciaReal) * (laser-distanciaReal)) / (2 * sigma * sigma);
						double lLaser = (1.0f / (Math.sqrt(2 * Math.PI) * sigma))  * Math.pow(Math.E, exponent);
						/* el valor de la creencia anterior, por el nuevo calculado*/
						double creenciaAnterior = m.creencias.get(d);
						m.creencias.put(d, creenciaAnterior * lLaser);
						sOdom += creenciaAnterior * lLaser;
						mediaCreencia += creenciaAnterior * lLaser;
				    }
				    /* el valor promedio que mostraremos en la celda */
				    m.creencia =  mediaCreencia / 8;
				}
			}
			/* despues normalizamos */
			for (Mosaico[] row : mapa.mundo) {
				for (Mosaico m : row) {
				   for (Direccion d : Direccion.values()) {
					   	double creencia = m.creencias.get(d);
					   	m.creencias.put(d, 1/sOdom * creencia);
				   }
				}
			}
		}

		/* 
		* Actualizamos la creencia en el caso de que el robot
		* no se haya movido
		*/
		public void actualizaCreenciaGiro(double angulo) {
			for (Mosaico[] row : mapa.mundo) {
				for (Mosaico m : row) {
					double sTheta = 0;
				   for (Direccion ddd : Direccion.values()) {
				   		double exponent = Math.pow((Math.toRadians(ddd.angulo())-robot.pos.angulo) - angulo,2) / (ruidoGiro * ruidoGiro);
				   		double lAngulo = (1.0 / ((2 * Math.PI) * ruidoGiro))  * Math.pow(Math.E, exponent);
				   		lAngulo = (lAngulo == Double.POSITIVE_INFINITY || lAngulo == Double.NEGATIVE_INFINITY) ? 0 : lAngulo;
				   		sTheta += lAngulo;
				   }
				   double creencias = 0;
				   for (Direccion f : Direccion.values()) {
				   		/* sacamos la creencia en t-1 */
				   		double creenciaActual = m.creencias.get(f);
				   		creenciaActual *= sTheta;
				   		creencias += creenciaActual;
				   }
				   /* normalizamos */
				   for (Direccion f : Direccion.values()) {
				   		m.creencias.put(f,  m.creencias.get(f) * sTheta / creencias);
				   }
				   m.creencia = creencias;
				}
			}
		}


		/* 
		* Actualizamos la creencia en el caso de que el robot
		* no se haya movido
		*/
		public void actualizaCreenciaMovimiento(Direccion dir) {
			double sOdom = 0;
			Mosaico actual  = mapa.mundo[robot.pos.y][robot.pos.x];
			Mosaico tMAsUno = actual.aplicaDireccion(dir);
			if (tMAsUno == null)
				return;
			double delta = Math.sqrt(Math.pow((tMAsUno.columna - actual.columna), 2) + Math.pow((tMAsUno.renglon - actual.renglon), 2)) * tamanioMosaico;

			for (Mosaico[] row : mapa.mundo) {
				for (Mosaico m : row) {
					double sCreencia = 0;
				    for (Direccion d : Direccion.values()) {
						double distanciaReal = m.distancias.get(d) * tamanioMosaico;
						double laser = distanciaReal * Math.random() * 2;
						double sX = delta * ruidoOdometro * Math.cos(Math.toRadians(d.angulo()));
						double sY = delta * ruidoOdometro * Math.sin(Math.toRadians(d.angulo()));
						double pX = Math.pow(tMAsUno.columna + delta * Math.cos(Math.toRadians(d.angulo())) - actual.columna, 2) / (sX * sX);
						double pY = Math.pow(tMAsUno.renglon + delta * Math.sin(Math.toRadians(d.angulo()))- actual.renglon, 2) / (sY * sY);
						
						pX = (pX == Double.POSITIVE_INFINITY || pX == Double.NEGATIVE_INFINITY) ? 0 : pX;
						pY = (pY == Double.POSITIVE_INFINITY || pY == Double.NEGATIVE_INFINITY) ? 0 : pY;

						double exponent = -0.5 * (pX + pY);
						double lLaser = 1.0f / ((2 * Math.PI) * sX * sY)  *  Math.pow(Math.E, exponent);

						lLaser = (lLaser == Double.POSITIVE_INFINITY || lLaser == Double.NEGATIVE_INFINITY || Double.isNaN(lLaser)) ? 0 : lLaser;
						sCreencia += lLaser;
				   }
				   double creencia = 0;
				   for (Direccion ff : Direccion.values()) {
				   		creencia += sCreencia * m.creencias.get(ff);
				   }
				   /* normalizamos */
				   for (Direccion ff : Direccion.values()) {
				   		double c = sCreencia * m.creencias.get(ff) / creencia;
				   		m.creencias.put(ff, c);
				   		m.creencia = c;
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
		Hashtable<Direccion, Double> creencias = new Hashtable<>();

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
		double angulo;
	}

	/* 
	* Clase que implementa el comportamiento del robot
	* 
	*/
	class Robot {
		Posicion pos;
		Mosaico m;
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

			m = mapa.mundo[startY][startX];
			mover(startX, startY,0);
		}

		Robot (int x, int y) {
			sensorOdometrico = laser = 0;
			mover(x, y,0);
		}

		void avanzaRandom() {
			int index = new Random().nextInt(Direccion.values().length);
    		Direccion nueva =  Direccion.values()[index];
    		/* calculamos antes de actualiza la referencia para tener los valores "anteriores" */
    		algoritmo.actualizaCreenciaMovimiento(nueva);
    		mover(nueva);
		}

		void giraRandom() {
			int angulo = r.nextInt(8);
			//Calculamos la creencia de la nueva posicion antes de asignarla
			algoritmo.actualizaCreenciaGiro(Math.toRadians(angulo * 45));
			pos.angulo = Math.toRadians(angulo * 45);
		}

		Direccion getDireccion(double angulo) {
			int a = (int) Math.toDegrees(angulo);
			a /= 45;
			switch (a) {
				case 1:
					return Direccion.NE;
				case 2:
					return Direccion.N;
				case 3:
					return Direccion.NO;
				case 4:
					return Direccion.O;
				case 5:
					return Direccion.SO;
				case 6:
					return Direccion.S;
				case 7:
					return Direccion.SE;
				case 8:
				case 0:
				default :
					return Direccion.E;
			}
		}

		/* devuelve la creencia dada la posicion actual */
		double getCreencia() {
			Mosaico celda = mapa.mundo[pos.y][pos.x];
			return celda.creencias.get(getDireccion(pos.angulo));
		}

		void mover(Direccion dir) {
			Mosaico np = m.aplicaDireccion(dir);
			/* la direccion nos mando al carajo, pues insitamos */
			if (np == null || np.tipo == Tipo.OBSTACULO) {
				for (Direccion d :  Direccion.values()) {
					np = m.aplicaDireccion(d);
					if (np != null && np.tipo != Tipo.OBSTACULO)
						break;
				}
				if (np == null)
					return;

			}
			mover(np.columna, np.renglon, Math.toRadians(dir.angulo()));
		}

		/* actualiza la posicion del robot dentro del cuarto */
		void mover(int x, int y, double angulo) {
			pos.x = x;
			pos.y = y;
			pos.angulo = angulo;
			m.tipo = Tipo.VACIO;
			mapa.mundo[y][x].tipo = Tipo.ROBOT;
			mapa.mundo[y][x].situacion = Situacion.ROBOT_DENTRO;
			m = mapa.mundo[y][x];
			//actualizaDistancias(x, y);
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
