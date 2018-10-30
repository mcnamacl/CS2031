import javax.xml.crypto.Data;
import java.io.IOException;
import java.net.BindException;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.util.*;

/**
 * Client class
 * An instance accepts user input
 */
public class Subscriber extends Node implements Runnable {
    static final int DEFAULT_SRC_PORT = (int) ((Math.random() * ((65535 - 1024) + 1) + 1024));
    static final int DEFAULT_DST_PORT = 50001;
    static final String DEFAULT_DST_NODE = "localhost";

    static final int TYPE_OF_PACKET_POS = 0;
    static final int TOPIC_LENGTH_POS = 1;

    static final int UN_SUB = 3;
    static final int SUB = 0;


    List<String> topics = new ArrayList<String>();
    InetSocketAddress dstAddress;
    boolean ended = false;

    /**
     * Constructor
     * Attempts to create socket at given port and create an InetSocketAddress for
     * the destinations
     */
    Subscriber(String dstHost, int dstPort, int srcPort, String topic) {
        try {
            dstAddress = new InetSocketAddress(dstHost, dstPort);
            socket = new DatagramSocket(srcPort);
            listener.go();
            topics.add(topic);
            //listener.userInput(null);
        } catch (java.lang.Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Assume that incoming packets contain a String and print the string.
     */
    public synchronized void onReceipt(DatagramPacket packet) {
        if (!ended) {
            byte[] data;
            data = packet.getData();
            String content = new String(data);
            System.out.println(content);
            System.out.println("to unsubscribe press 1, to subscriber to a new topic press 2, to see a list of all topics" +
                    " you are currently subscribed to press 3, else press 0");
            Scanner input = new Scanner(System.in);
            if (input.hasNext("1")) {
                System.out.println("Which topic would you like to unsubscribe from?");
                input = new Scanner(System.in);
                if (input.hasNext()) {
                    String message = input.next();
                    unSub(message);
                }
            }
            else if (input.hasNext("2")){
                System.out.println("What topic would you also like to subscribe to?");
                input = new Scanner(System.in);
                if (input.hasNext()) {
                    String topic = input.next();
                    topics.add(topic);
                    subToNewTopic(topic.getBytes());
                }
            }
            else if (input.hasNext("3")){
                if (topics.size()==0){
                    System.out.println("You are not currently subscribed to any topics");
                }
                else {
                    for (int i = 0; i < topics.size(); i++) {
                        System.out.println(topics.get(i));
                    }
                }
            }
        }
    }

    public synchronized void unSub(String topic) {
        topics.remove(topic);
        byte[] topicBytes = topic.getBytes();
        try {
            byte[] unSub = new byte[1 + topicBytes.length + 10];
            unSub[TOPIC_LENGTH_POS] = (byte) topicBytes.length;
            unSub[TYPE_OF_PACKET_POS] = UN_SUB;
            for (int i = 0; i < topicBytes.length; i++) {
                unSub[TOPIC_LENGTH_POS + i + 1] = topicBytes[i];
            }
            DatagramPacket packet = new DatagramPacket(unSub, unSub.length, dstAddress);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("you have unsubscribed from " + topic + " if you would like to end all subscriptions " +
                " type end else press 0");
        Scanner scanner = new Scanner(System.in);
        if (scanner.hasNext("end")) {
            ended = true;
            this.notify();
        }
    }

    public void subToNewTopic(byte[] topicBytes){
        topics.add(new String(topicBytes));
        byte[] buffer = new byte[TOPIC_LENGTH_POS + topicBytes.length + 1];
        buffer[TYPE_OF_PACKET_POS] = (byte) SUB;
        buffer[TOPIC_LENGTH_POS] = (byte) topicBytes.length;
        for (int i = 0; i < topicBytes.length; i++) {
            buffer[TOPIC_LENGTH_POS + i + 1] = topicBytes[i];
        }
        System.out.println("Sending packet...");
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, dstAddress);
        try {
            socket.send(packet);
            System.out.println("Packet has been sent.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sender Method
     */
    public synchronized void run() {
        DatagramPacket packet;
        byte[] topicBytes = topics.get(0).getBytes();
        byte[] buffer = new byte[TOPIC_LENGTH_POS + topicBytes.length + 1];
        buffer[TYPE_OF_PACKET_POS] = (byte) SUB;
        buffer[TOPIC_LENGTH_POS] = (byte) topicBytes.length;
        for (int i = 0; i < topicBytes.length; i++) {
            buffer[TOPIC_LENGTH_POS + i + 1] = topicBytes[i];
        }
        System.out.println("Sending packet...");
        packet = new DatagramPacket(buffer, buffer.length, dstAddress);
        try {
            socket.send(packet);
            this.wait();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //	/**
//	 * Test method
//	 *
//	 * Sends a packet to a given address
//	 */
    public static void main(String[] args) throws BindException {
        System.out.println("What topic do you want to subscribe to?");
        Scanner input = new Scanner(System.in);
        if (input.hasNext()) {
            String topic = input.next();
            Thread sub = new Thread(new Subscriber(DEFAULT_DST_NODE, DEFAULT_DST_PORT, DEFAULT_SRC_PORT, topic));
            sub.start();
        }
    }
}