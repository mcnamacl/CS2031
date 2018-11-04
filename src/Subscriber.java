import javax.xml.crypto.Data;
import java.io.IOException;
import java.net.BindException;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.util.*;

public class Subscriber extends Node implements Runnable {
    static final int DEFAULT_SRC_PORT = (int) ((Math.random() * ((65535 - 1024) + 1) + 1024));
    static final int DEFAULT_DST_PORT = 50001;
    static final String DEFAULT_DST_NODE = "localhost";

    static final int TYPE_OF_PACKET_POS = 0;
    static final int TOPIC_LENGTH_POS = 1;
    static final int PUBLISHER_NUMBER_POS = 4;
    static final int PRIORITY_POS = 5;
    static final int DATA_BEGIN_POS = 10;

    static final int SENDING_MULTIPLE_PACKETS = 5;
    static final int FINISHED_SENDING_ALL_PACKETS = 4;
    static final int UN_SUB = 2;
    static final int SUB = 0;

    List<String> topics = new ArrayList<>();
    HashMap<Integer, List<DatagramPacket>> packetsReceived = new HashMap<>();
    InetSocketAddress dstAddress;
    boolean ended = false;

    Subscriber(String dstHost, int dstPort, int srcPort, String topic) {
        try {
            dstAddress = new InetSocketAddress(dstHost, dstPort);
            socket = new DatagramSocket(srcPort);
            listener.go();
            inputListener.start();
            topics.add(topic);
        } catch (java.lang.Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void onReceipt(DatagramPacket packet) {
        byte[] data;
        data = packet.getData();
        if (data[TYPE_OF_PACKET_POS] == SENDING_MULTIPLE_PACKETS && !packetsReceived.containsKey(Integer.valueOf(data[PUBLISHER_NUMBER_POS]))){
            List<DatagramPacket> list = new ArrayList<>();
            list.add(packet);
            packetsReceived.put(Integer.valueOf(data[PUBLISHER_NUMBER_POS]), list);
        }
        else if (data[TYPE_OF_PACKET_POS] == SENDING_MULTIPLE_PACKETS && packetsReceived.containsKey(Integer.valueOf(data[PUBLISHER_NUMBER_POS]))){
            List<DatagramPacket> list = packetsReceived.get(Integer.valueOf(data[PUBLISHER_NUMBER_POS]));
            list.add(packet);
            packetsReceived.put((Integer.valueOf(data[PUBLISHER_NUMBER_POS])), list);
        }
        if (packet.getData()[TYPE_OF_PACKET_POS]==FINISHED_SENDING_ALL_PACKETS){
            int amountOfPacketsReceived = 0;
            for (int count = 0; count < packetsReceived.size(); count++){
                amountOfPacketsReceived = amountOfPacketsReceived + packetsReceived.get(count).size();
            }
            DatagramPacket[] packets = new DatagramPacket[amountOfPacketsReceived];
            int lastSize = 0;
            for (int i = 0; i < packetsReceived.size(); i++){
                List<DatagramPacket> list = packetsReceived.get(i);
                for (int j = 0; j < list.size(); j++){
                    packets[list.get(j).getData()[PRIORITY_POS] + lastSize] = list.get(j);
                }
                lastSize = list.size() + lastSize;
            }
            for (int k = 0 ; k < packets.length; k++){
                byte[] message = new byte[packets[k].getData().length - DATA_BEGIN_POS];
                for (int j = 0 ; j < message.length; j++){
                    message[j] = packets[k].getData()[DATA_BEGIN_POS + j];
                }
                String content = new String(message);
                System.out.println(content);
            }
            packetsReceived = null;
        }
        else if (data[TYPE_OF_PACKET_POS] == SUB){
            byte[] contentToBePrinted = new byte[data.length - DATA_BEGIN_POS];
            for (int i = 0; i < contentToBePrinted.length; i++){
                contentToBePrinted[i] = data[i + DATA_BEGIN_POS];
            }
            String content = new String(contentToBePrinted);
            System.out.println(content);
        }
    }

    @Override
    public void userInput(String message) {
        if (!ended) {
            if (message.equals("1")) {
                System.out.println("Which topic would you like to unsubscribe from?");
                Scanner input = new Scanner(System.in);
                if (input.hasNext()) {
                    message = input.next();
                    unSub(message);
                }
            } else if (message.equals("2")) {
                System.out.println("What topic would you also like to subscribe to?");
                Scanner input = new Scanner(System.in);
                if (input.hasNext()) {
                    String topic = input.next();
                    subToNewTopic(topic.getBytes());
                }
            } else if (message.equals("3")) {
                if (topics.size() == 0) {
                    System.out.println("You are not currently subscribed to any topics");
                } else {
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
                unSub[DATA_BEGIN_POS + i + 1] = topicBytes[i];
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

    public void subToNewTopic(byte[] topicBytes) {
        topics.add(new String(topicBytes));
        byte[] buffer = new byte[DATA_BEGIN_POS + topicBytes.length + 1];
        buffer[TYPE_OF_PACKET_POS] = (byte) SUB;
        buffer[TOPIC_LENGTH_POS] = (byte) topicBytes.length;
        for (int i = 0; i < topicBytes.length; i++) {
            buffer[DATA_BEGIN_POS + i + 1] = topicBytes[i];
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

    public synchronized void run() {
        DatagramPacket packet;
        byte[] topicBytes = topics.get(0).getBytes();
        byte[] buffer = new byte[DATA_BEGIN_POS + topicBytes.length + 1];
        buffer[TYPE_OF_PACKET_POS] = (byte) SUB;
        buffer[TOPIC_LENGTH_POS] = (byte) topicBytes.length;
        for (int i = 0; i < topicBytes.length; i++) {
            buffer[DATA_BEGIN_POS + i + 1] = topicBytes[i];
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