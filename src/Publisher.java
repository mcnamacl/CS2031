import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;

/**
 *
 * Client class
 *
 * An instance accepts user input
 *
 */
public class Publisher extends Node {
    static final int DEFAULT_SRC_PORT = 50003;
    static final int DEFAULT_DST_PORT = 50001;
    static final String DEFAULT_DST_NODE = "localhost";

    InetSocketAddress dstAddress;

    /**
     * Constructor
     *
     * Attempts to create socket at given port and create an InetSocketAddress for
     * the destinations
     */
    Publisher(String dstHost, int dstPort, int srcPort) {
        try {
            dstAddress = new InetSocketAddress(dstHost, dstPort);
            socket = new DatagramSocket(srcPort);
            listener.go();
        } catch (java.lang.Exception e) {
            e.printStackTrace();
        }
    }

    Publisher (){

    }

    /**
     * Assume that incoming packets contain a String and print the string.
     */
    public synchronized void onReceipt(DatagramPacket packet) {
        byte[] data;
        data = packet.getData();
        String content = new String(data);
        this.notify();
        System.out.println(content.toString());
    }

    /**
     * Sender Method
     *
     */
    public synchronized void start() throws Exception {
        DatagramPacket packet = null;
        String text = "marco";
        byte[] data = new byte[8];
        data = text.getBytes();
        byte[] buffer = new byte[10 + data.length + 1];
        buffer[10] = (byte) data.length;
        for (int i = 0; i < data.length; i++) {
            buffer[10 + i + 1] = data[i];
        }
        System.out.println("Sending packet...");
        packet = new DatagramPacket(data, data.length, dstAddress);
        socket.send(packet);
        System.out.println("Packet sent");
        this.wait();
    }

//	@Override
//	public void run() {
//			DatagramPacket packet = null;
//			String text = "marco";
//			byte[] data = new byte[8];
//			data = text.getBytes();
//			byte[] buffer = new byte[10 + data.length + 1];
//			buffer[10] = (byte) data.length;
//			for (int i = 0; i < data.length; i++) {
//				buffer[10 + i + 1] = data[i];
//			}
//			System.out.println("Sending packet...");
//			packet = new DatagramPacket(data, data.length, dstAddress);
//			try {
//				socket.send(packet);
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//			System.out.println("Packet sent");
//			try {
//				this.wait();
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//	}

    //	/**
//	 * Test method
//	 * 
//	 * Sends a packet to a given address
//	 */
    public static void main(String[] args) {
        try {
            Publisher sub = new Publisher(DEFAULT_DST_NODE, DEFAULT_DST_PORT, DEFAULT_SRC_PORT);
            sub.start();
            System.out.println("Program completed");
        } catch (java.lang.Exception e) {
            e.printStackTrace();
        }
    }
}