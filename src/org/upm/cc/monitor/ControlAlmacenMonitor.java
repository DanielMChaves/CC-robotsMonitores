package org.upm.cc.monitor;

import java.util.LinkedList;

import es.upm.babel.cclib.Monitor;
import es.upm.babel.cclib.Monitor.Cond;

public class ControlAlmacenMonitor implements ControlAlmacenInterface {

	/**
	 * Cola para almacenar los pedidos que van llegando
	 */
	private LinkedList<Integer> colaAlmacen;
	
	/**
	 * Monitor para controlar el control de acceso
	 */
	private Monitor mutex = new Monitor();
	/**
	 * Condition para dejar esperando al almacén mientras llegan pedidos.
	 */
	private Cond almacen;
	
	public ControlAlmacenMonitor () {
	
		colaAlmacen = new LinkedList();
		almacen = mutex.newCond();
	}
	
	@Override
	public void almacenarPedido(int pid) {
		mutex.enter();
		
		colaAlmacen.addLast(pid);
		
		almacen.signal();
		mutex.leave();
	}

	@Override
	public int colocarPedido() {
		mutex.enter();
		
		if (colaAlmacen.size() == 0){
			almacen.await();
		}
		
		int pedido = colaAlmacen.poll();
		
		mutex.leave();
		return pedido;
	}
	

}
