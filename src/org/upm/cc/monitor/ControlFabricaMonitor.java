package org.upm.cc.monitor;

import java.util.LinkedList;
import es.upm.babel.cclib.Monitor;

public class ControlFabricaMonitor implements ControlFabricaInterface {

	// Atributos para el recurso
	private boolean pedidosEnEspera[];
	private LinkedList<Integer>colaRobot[];
	private int numProcesados[];
	private boolean enMantenimiento[];

	// Monitores y Condiciones
	private Monitor mutex;
	private Monitor.Cond cond_PASAR_CONTROL[];
	private Monitor.Cond cond_PROCESAR_PEDIDO[];
	private Monitor.Cond cond_VERIFICAR_ROBOT[];

	public ControlFabricaMonitor (int nPedidos, int nRobots) {

		// Inicializamos los atributos
		pedidosEnEspera = new boolean[nPedidos];
		colaRobot = new LinkedList[nRobots];
		numProcesados = new int[nRobots];
		enMantenimiento = new boolean[nRobots];

		// Inicializamos el monitor y las condiciones de los monitores
		mutex = new Monitor();
		cond_PASAR_CONTROL = new Monitor.Cond[nPedidos];
		cond_PROCESAR_PEDIDO = new Monitor.Cond[nRobots];
		cond_VERIFICAR_ROBOT = new Monitor.Cond[nRobots];

		// Bucle nPedidos
		for(int i = 0; i < nPedidos; i++){
			pedidosEnEspera[i] = false;
			cond_PASAR_CONTROL[i] = mutex.newCond();
		}

		// Bucle nRobots
		for(int i = 0; i < nRobots; i++){
			colaRobot[i] = new LinkedList<Integer>();
			numProcesados[i] = 0;
			enMantenimiento[i] = false;
			cond_PROCESAR_PEDIDO[i] = mutex.newCond();
			cond_VERIFICAR_ROBOT[i] = mutex.newCond();
		}
	}

	/**
	 * Notifica que el pedido <source>pid</source> se encuentra preparado para ser procesado por
	 * el robot <rid>
	 * @param pid Identificador del pedido
	 * @param rid Identiticador del robot
	 */

	@Override
	public void notificarLlegadaPedido(int pid, int rid) {

		// Cogemos Mutex
		mutex.enter();

		// CPRE: 
		// Cierto
		// << codigo vacio >>

		// POST: 
		/* self = (	pe ⊕ {pid → cierto }, 
		 			cr ⊕ {rid → cr(rid) + (pid)}, 
		 			np, em) 
		 */
		pedidosEnEspera[pid] = true;
		colaRobot[rid].addLast(pid);

		// Desbloqueos
		procesarSignals();

		// Liberamos Mutex
		mutex.leave();
	}

	/**
	 * Pone en espera el pedido <source>pid</source> hasta ser procesado
	 * @param pid Identificador del pedido
	 */

	@Override
	public void pasarControl(int pid) {

		// Cogemos Mutex
		mutex.enter();

		// CPRE:
		// ¬self.pedidosEnEspera(pid)
		if(pedidosEnEspera[pid]){
			cond_PASAR_CONTROL[pid].await();
		}

		// POST:
		// self
		// << codigo vacio >>

		// Desbloqueos
		// << codigo vacio >>

		// Liberamos Mutex
		mutex.leave();
	}

	/**
	 * El robot <source>rid</source> procesa el primer pedido que se encuentra en su cola de pedidos
	 * @param rid Indentificador del pedido
	 * @return el <source>pid</source> del pedido que ha procesado
	 */

	@Override
	public int procesarPedido(int rid) {

		// Cogemos Mutex
		mutex.enter();

		// CPRE:
		// Longitud(self.colaRobot(rid)) > 0 ^ ¬self.enMantenimiento(rid)
		if(colaRobot[rid].isEmpty() || enMantenimiento[rid]){
			cond_PROCESAR_PEDIDO[rid].await();
		}

		// POST:
		/*
		self = (pe ⊕ {pid → falso},
				cr ⊕ {rid → cr(rid)(1..l − 1),
				np ⊕ {rid → np(rid) + 1},
				em ⊕ {rid → (np(rid) + 1 mod N_PROCS(rid)) = 0 ⇒ cierto)})
		 */

		int pid = colaRobot[rid].removeFirst();
		pedidosEnEspera[pid] = false;
		numProcesados[rid]++; 
		if(numProcesados[rid] % TipoFabrica.N_PROCS(rid) == 0){
			enMantenimiento[rid] = true;
		}

		// Desbloqueos
		procesarSignals();

		// Liberamos Mutex
		mutex.leave();
		return pid;
	}

	/**
	 * Verifica el robot <source>rid</source> cuando este hay superado el numero N_PROCS de pedidos
	 * sin ser revisado.
	 * @param rid Identificador del Robot
	 * @return El número de pedidos procesados cuando ha saltado la verificación
	 */

	@Override
	public int verificarRobot(int rid) {

		// Cogemos Mutex
		mutex.enter();

		// CPRE:
		// self.enMantenimiento(rid)
		if(!enMantenimiento[rid]){
			cond_VERIFICAR_ROBOT[rid].await();
		}

		// POST:
		// snumprocs = selfpre.numProcesados(rid)^self = selfpre
		// << codigo vacio >>


		// Desbloqueos
		// << codigo vacio >>

		// Liberamos Mutex
		mutex.leave();
		return numProcesados[rid];
	}

	/**
	 * Notifica que el robot <source>rid</source> ya ha completado su verificación y puede 
	 * continuar procesando pedidos normalmente 
	 * @param rid Identificador del robot
	 */

	@Override
	public void notificarFinMantenimiento(int rid) {

		// Cogemos Mutex
		mutex.enter();

		// CPRE: 
		// Cierto
		// << codigo vacio >>

		// POST:
		// self.enMantenimiento(rid) = falso
		enMantenimiento[rid] = false;
		

		// Desbloqueos
		procesarSignals();

		// Liberamos Mutex
		mutex.leave();
	}

	/**
	 * Función auxiliar que se encarga de llevar a cabo las llamadas signal de las condiciones
	 */

	public void procesarSignals(){

		boolean salir = true;

		// Bucle de cond_PASAR_CONTROL
		for(int i = 0; i < cond_PASAR_CONTROL.length; i++){
			if(!pedidosEnEspera[i] && cond_PASAR_CONTROL[i].waiting() > 0 && salir){
				cond_PASAR_CONTROL[i].signal();
				salir = false;
			}
		}

		// Bucle de cond_PROCESAR_PEDIDO
		for(int i = 0; i < cond_PROCESAR_PEDIDO.length; i++){
			if(!colaRobot[i].isEmpty() && !enMantenimiento[i] && cond_PROCESAR_PEDIDO[i].waiting() > 0 && salir){
				cond_PROCESAR_PEDIDO[i].signal();
				salir = false;
			}
		}

		// Bucle de cond_VERIFICAR_ROBOT
		for(int i = 0; i < cond_VERIFICAR_ROBOT.length; i++){
			if(enMantenimiento[i] && cond_VERIFICAR_ROBOT[i].waiting() > 0 && salir){
					cond_VERIFICAR_ROBOT[i].signal();
					salir = false;
			}
		}
	}
}
