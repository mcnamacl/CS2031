import java.io.IOException;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.util.*;
/**
 *
 * Client class
 *
 * An instance accepts user input
 *
 */
public class Subscriber extends Node implements Runnable {
    static final int DEFAULT_SRC_PORT = 50000;
    static final int DEFAULT_DST_PORT = 50001;
    static final String DEFAULT_DST_NODE = "localhost";

    String topic;
    InetSocketAddress dstAddress;

    /**
     * Constructor
     *
     * Attempts to create socket at given port and create an InetSocketAddress for
     * the destinations
     */
    Subscriber(String dstHost, int dstPort, int srcPort, String topic) {
        try {
            dstAddress = new InetSocketAddress(dstHost, dstPort);
            socket = new DatagramSocket(srcPort);
            listener.go();
            this.topic = topic;
        } catch (java.lang.Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Assume that incoming packets contain a String and print the string.
     */
    public synchronized void onReceipt(DatagramPacket packet) {
        byte[] data;
        data = packet.getData();
        String content = new String(data);
        //this.notify();
        System.out.println(content.toString());
    }

    /**
     * Sender Method
     *
     */
    public synchronized void run() {
        DatagramPacket packet;
       /* String text = "cats";
        byte[] data = text.getBytes();
        byte[] buffer = new byte[10 + data.length + 1];
        buffer[10] = (byte) data.length;
        for (int i = 0; i < data.length; i++) {
            buffer[10 + i + 1] = data[i];
        }*/
        byte[] buffer = new byte[10 + 1];
        buffer[0] = (byte) 0;
        byte[] topicBytes = topic.getBytes();
        for (int i = 0; i < topicBytes.length; i++){buffer[i+1] = topicBytes[i];}
        System.out.println("Sending packet...");
        packet = new DatagramPacket(buffer, buffer.length, dstAddress);
        try {
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Packet sent");
        try {
            this.wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
            List<Subscriber> subscribers = new ArrayList<>();
            boolean finished = false;
            while (!finished) {
                System.out.println("what topic are you interested in");
                Scanner input = new Scanner(System.in);
                if (input.hasNext()) {
                    String topic = input.next();
                    Subscriber sub = new Subscriber(DEFAULT_DST_NODE, DEFAULT_DST_PORT, DEFAULT_SRC_PORT, topic);
                    subscribers.add(sub);
                    sub.run();
                    //Subscriber sub1 = new Subscriber();
                }
            }
            System.out.println("Program completed");
        } catch (java.lang.Exception e) {
            e.printStackTrace();
        }
    }
}