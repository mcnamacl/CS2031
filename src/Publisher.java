import java.io.IOException;
import java.net.BindException;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Publisher extends Node implements Runnable {
    static final int DEFAULT_SRC_PORT = (int) ((Math.random() * ((65535 - 1024) + 1) + 1024));
    static final int DEFAULT_DST_PORT = 50001;
    static final String DEFAULT_DST_NODE = "localhost";

    InetSocketAddress dstAddress;
    String topic;
    String message;

    static final int TYPE_OF_PACKET_POS = 0;
    static final int TOPIC_LENGTH_POS = 1;
    static final int MSG_LENGTH_POS = 2;
    static final int REQUEST_POS = 3;

    static final int DATA_BEGIN_POS = 10;

    static final int TYPE_OF_PACKET = 1;
    static final int SENDING_ALL_PUBLICATIONS = 3;
    static final int REQUEST = 3;

    List<DatagramPacket> packetsSent = new ArrayList<>();

    Publisher(String dstHost, int dstPort, int srcPort, String topic, String message) {
        try {
            this.message = message;
            this.topic = topic;
            dstAddress = new InetSocketAddress(dstHost, dstPort);
            socket = new DatagramSocket(srcPort);
            listener.go();
            inputListener.start();
        } catch (java.lang.Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void onReceipt(DatagramPacket packet) {
        byte[] data;
        data = packet.getData();
        DatagramPacket prevPacket;
        if (data[REQUEST_POS] == REQUEST){
            for (int i = 0; i < packetsSent.size(); i++){
                try {
                    prevPacket = packetsSent.get(i);
                    prevPacket.getData()[TYPE_OF_PACKET_POS] = SENDING_ALL_PUBLICATIONS;
                    socket.send(packetsSent.get(i));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        else {
            String content = new String(data);
            System.out.println(content);
            System.out.println("type another message");
        }
    }

    @Override
    public void userInput(String message) {
        byte[] topicBytes = topic.getBytes();
        byte[] msgBytes = message.getBytes();
        byte[] buffer = new byte[DATA_BEGIN_POS + msgBytes.length + topicBytes.length + 1];
        buffer[TOPIC_LENGTH_POS] = (byte) topicBytes.length;
        buffer[MSG_LENGTH_POS] = (byte) msgBytes.length;
        buffer[TYPE_OF_PACKET_POS] = (byte) TYPE_OF_PACKET;
        for (int i = 0; i < topicBytes.length; i++) {
            buffer[DATA_BEGIN_POS + i + 1] = topicBytes[i];
        }
        for (int i = 0; i < msgBytes.length; i++) {
            buffer[DATA_BEGIN_POS + topicBytes.length + 1 + i] = msgBytes[i];
        }
        System.out.println("Sending packet...");
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, dstAddress);
        try {
            packetsSent.add(packet);
            socket.send(packet);
            System.out.println("Packet sent");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void run() {
        DatagramPacket packet;
        byte[] msgBytes = message.getBytes();
        byte[] topicBytes = topic.getBytes();
        byte[] buffer = new byte[DATA_BEGIN_POS + msgBytes.length + topicBytes.length + 1];
        buffer[TOPIC_LENGTH_POS] = (byte) topicBytes.length;
        buffer[MSG_LENGTH_POS] = (byte) msgBytes.length;
        buffer[TYPE_OF_PACKET_POS] = (byte) TYPE_OF_PACKET;
        for (int i = 0; i < topicBytes.length; i++) {
            buffer[DATA_BEGIN_POS + i + 1] = topicBytes[i];
        }
        for (int i = 0; i < msgBytes.length; i++) {
            buffer[DATA_BEGIN_POS + topicBytes.length + 1 + i] = msgBytes[i];
        }
        System.out.println("Sending packet...");
        packet = new DatagramPacket(buffer, buffer.length, dstAddress);
        try {
            socket.send(packet);
            System.out.println("Packet sent");
            packetsSent.add(packet);
            this.wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws BindException {
        System.out.println("What topic would you like to publish to?");
        Scanner input = new Scanner(System.in);
        if (input.hasNext()) {
            String topic = input.nextLine();
            System.out.println("What message would you like to publish?");
            if (input.hasNext()) {
                String message = input.nextLine();
                Thread pub = new Thread(new Publisher(DEFAULT_DST_NODE, DEFAULT_DST_PORT, DEFAULT_SRC_PORT, topic, message));
                pub.start();
            }
        }
    }
}