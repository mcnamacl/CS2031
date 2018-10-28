import java.io.IOException;
import java.net.BindException;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.util.Scanner;

/**
 * Client class
 * <p>
 * An instance accepts user input
 */
public class Publisher extends Node implements Runnable {//65,535 ((Math.random() * ((65535 - 1024) + 1) + 1024));
    static final int DEFAULT_SRC_PORT = (int) ((Math.random() * ((65535 - 1024) + 1) + 1024));
    static final int DEFAULT_DST_PORT = 50001;
    static final String DEFAULT_DST_NODE = "localhost";

    InetSocketAddress dstAddress;
    String topic;
    String message;

    /**
     * Constructor
     * <p>
     * Attempts to create socket at given port and create an InetSocketAddress for
     * the destinations
     */
    Publisher(String dstHost, int dstPort, int srcPort, String topic, String message) {
        try {
            this.message = message;
            this.topic = topic;
            dstAddress = new InetSocketAddress(dstHost, dstPort);
            socket = new DatagramSocket(srcPort);
            listener.go();
            //listener.userInput(null);
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
        System.out.println(content);
        System.out.println("type another message");
        Scanner input = new Scanner(System.in);
        if (input.hasNext()) {
            message = input.nextLine();
            byte[] topicBytes = topic.getBytes();
            byte[] msgBytes = message.getBytes();
            byte[] buffer = new byte[11 + msgBytes.length + topicBytes.length + 1];
            buffer[10] = (byte) topicBytes.length;
            buffer[11] = (byte) msgBytes.length;
            buffer[0] = (byte) 1;
            for (int i = 0; i < topicBytes.length; i++) {
                buffer[11 + i + 1] = topicBytes[i];
            }
            for (int i = 0; i < msgBytes.length; i++) {
                buffer[11 + topicBytes.length + 1 + i] = msgBytes[i];
            }
            System.out.println("Sending packet...");
            packet = new DatagramPacket(buffer, buffer.length, dstAddress);
            try {
                socket.send(packet);
                System.out.println("Packet sent");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Sender Method
     */
    public synchronized void run() {
        DatagramPacket packet;
        byte[] msgBytes = message.getBytes();
        byte[] topicBytes = topic.getBytes();
        byte[] buffer = new byte[11 + msgBytes.length + topicBytes.length + 1];
        buffer[10] = (byte) topicBytes.length;
        buffer[11] = (byte) msgBytes.length;
        buffer[0] = (byte) 1;
        for (int i = 0; i < topicBytes.length; i++) {
            buffer[11 + i + 1] = topicBytes[i];
        }
        for (int i = 0; i < msgBytes.length; i++) {
            buffer[11 + topicBytes.length + 1 + i] = msgBytes[i];
        }
        System.out.println("Sending packet...");
        packet = new DatagramPacket(buffer, buffer.length, dstAddress);
        try {
            socket.send(packet);
            System.out.println("Packet sent");
            this.wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //	/**
//	 * Test method
//	 * 
//	 * Sends a packet to a given address
//	 */
    public static void main(String[] args) throws BindException {
        System.out.println("what topic are you interested in");
        Scanner input = new Scanner(System.in);
        if (input.hasNext()) {
            String topic = input.nextLine();
            if (input.hasNext()) {
                String message = input.nextLine();
                Thread pub = new Thread(new Publisher(DEFAULT_DST_NODE, DEFAULT_DST_PORT, DEFAULT_SRC_PORT, topic, message));
                pub.start();
            }
        }
    }
}