package org.upm.cc.monitor;

import org.upm.cc.monitor.TipoFabrica.TIPO_FABRICA;
import org.upm.cc.monitor.TipoFabrica.TIPO_VELOCIDAD;


public class Fabrica {

	public static void main(String[] args) throws InterruptedException, FabricaException {

		long tini = System.currentTimeMillis();

		// Establece el tipo de fábrica (mas o menos pedidos, robots... y la velocidad de los procesos)
		TipoFabrica.setTipoFabrica(TIPO_FABRICA.TIPO_1);
		TipoFabrica.setVelocidadFabrica(TIPO_VELOCIDAD.RAPIDO);
		
		// Creamos el recurso de control de la fábrica e imprimimos los datos 
		ControlFabricaInterface cFabrica = new ControlFabricaMonitor(TipoFabrica.N_PEDIDOS(),TipoFabrica.N_ROBOTS());
		TipoFabrica.imprimirInformacion();

		ControlAlmacenInterface cAlmacen = new ControlAlmacenMonitor();

		// Tenemos N_PEDIDOS threads que simulan el comportamiento de los pedidos
		Pedido pedidos [] = new Pedido [TipoFabrica.N_PEDIDOS()];
		
		// Tenemos N_ROBOTS threads que simulan el comportamiento de los robots 
		Robot robots [] = new Robot [TipoFabrica.N_ROBOTS()];

		// Tenemos N_ROBOTS threads que simulan el comportamiento de los verificadores
		Mecanico verificadores [] = new Mecanico [TipoFabrica.N_ROBOTS()];

		// Tenemos un almacén para la fábrica
		Almacen almacen = new Almacen(cAlmacen);

		for (int i = 0; i < TipoFabrica.N_PEDIDOS(); i++) {
			pedidos[i] = new Pedido (i, cFabrica,cAlmacen);
		}

		for (int i = 0; i < TipoFabrica.N_ROBOTS(); i++) {
			robots[i] = new Robot (i,cFabrica);
			verificadores[i] = new Mecanico(i,cFabrica);
		}	

		almacen.start();

		for(int i = 0; i < TipoFabrica.N_ROBOTS(); i++) {
			robots[i].start();
			verificadores[i].start();
		}

		// Arrancamos los pedidos
		for (int i = 0; i < TipoFabrica.N_PEDIDOS(); i++) {
			pedidos[i].start();
		}

		// Esperamos a que terminen los pedidos, los robots, los verificadores y el almacén.

		for (int i = 0; i < TipoFabrica.N_PEDIDOS(); i++) {
			pedidos[i].join();
		}

		for(int i = 0; i < TipoFabrica.N_ROBOTS(); i++) {
			robots[i].join();
			verificadores[i].join();
		}
		almacen.join();

		// Comprobamos que todos han terminado correctamente
		System.out.println();
		System.out.println("Terminados todos los procesos...");
		System.out.println();

		long tend = System.currentTimeMillis();
		long ttotal = (tend - tini) / 1000;
		
		checkResultados(pedidos, robots, almacen, verificadores);

		System.out.println("Tiempo total invertido: " + ttotal + " segundos");
		
	}
	
	public static void checkResultados (Pedido [] pedidos, 
										Robot [] robots, 
										Almacen almacen,
										Mecanico [] verificadores) throws FabricaException{

		System.out.println();
		System.out.println("---- COMPROBANDO RESULTADOS: ");
		System.out.println();
		System.out.println("*** Comprobando los Robots ***");
		for(int i = 0; i < robots.length; i ++) {
			robots[i].checkRobot();
			System.out.println(robots[i] + "--> OK");
		}
		System.out.println();
		System.out.println("*** Comprobando los pedidos del almacén ***");
		System.out.println();
		almacen.checkAlmacen();
		System.out.println();
		System.out.println("TODOS LOS PEDIDOS SE HAN PROCESADO CORRECTAMENTE!");
		System.out.println();
	}
	

	
}
  