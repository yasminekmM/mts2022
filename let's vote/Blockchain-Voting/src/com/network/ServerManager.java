package com.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.crypto.SealedObject;

import com.blockchain.Block;


public class ServerManager extends NetworkManager {
	
	
	private ServerSocket _svrSocket = null;
	

	private volatile AtomicInteger _cid = null;
	

	private volatile Map<Integer, Socket> _clients = null;
	
	
	public ServerManager(int svrPort) {
		try {
			_clients = new ConcurrentSkipListMap<Integer, Socket>();
			_cid = new AtomicInteger(0);
			
			_svrSocket = new ServerSocket(svrPort);
			
			System.out.println("Waiting for clients...");
			System.out.println("Please connect to " + InetAddress.getLocalHost() + ":" + svrPort + ".");
		} catch (IOException e) {
			System.out.println("ERROR: failed to listen on port " + svrPort);
			e.printStackTrace();
		}
	}
	

	@Override
	public void run() {
		while (true) {
			try {

				Socket socket = _svrSocket.accept();
				addClient(socket);
				System.out.println("New client(cid is " + getCid(socket) + ") connected!");
				

				new ServerHandler(this, socket).start();

				sendMsg(socket, new MessageStruct(2, Integer.valueOf(getCid(socket))));
			} catch (IOException e) {

				break;
			}
		}
	}
	
	public void clientDisconnected(Socket client) {
		int cid = getCid(client);
		System.out.println("Client " + cid + " has disconnected.");
		
		deleteClient(cid);
	}
	
/* ================== Message handlers begin ==================*/
	
	@Override
	public void msgHandler(MessageStruct msg, Socket src) {
		switch (msg._code) {
		case 0:

			break;
		case 1:

			System.out.println("Broadcasting : " + (String)msg._content.toString());
			
			broadcast((SealedObject)msg._content,src );
			break;
		default:
			break;
		}
	}
	
	private void broadcast(SealedObject o, Socket src) {
		

		int srcCid=getCid(src);
//		System.out.println("Source id : "+ srcCid);
		for(int i = _cid.get()-1 ;i>=0;i--) {
			if(i!=srcCid) {
				try {
					sendMsg(getClient(i), new MessageStruct(0, o));
				} catch (IOException e) {
					System.out.println("ERROR: Connection with " + srcCid + " is broken, message cannot be sent!");
					e.printStackTrace();
				} catch (NullPointerException e) {
					continue;
				}
				
				
			}
		}
	}
	

	private void addClient(Socket socket) {
		_clients.put(Integer.valueOf(_cid.getAndIncrement()), socket);
	}
	
	private boolean deleteClient(int idx) {
		if (_clients.remove(Integer.valueOf(idx)) == null) {
			System.out.println("delete failed!");
			return false;
		}
		return true;
	}
	
	private Socket getClient(int cid) {
		return (Socket)_clients.get(Integer.valueOf(cid));
	}
	
	private int getCid(Socket socket) {
		for (Map.Entry<Integer, Socket> entry : _clients.entrySet()) {
		    if (entry.getValue() == socket) {
		    	return entry.getKey().intValue();
		    }
		}
		return -1;
	}
	

	

	public void close() {
		System.out.println("Server is about to close. All connected clients will exit.");
		try {
			_svrSocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Bye~");
	}

}
