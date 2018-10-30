import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.util.*;

public class Broker extends Node {
    static final int DEFAULT_PORT = 50001;

    static final int TYPE_OF_PACKET_POS = 0;
    static final int TOPIC_LENGTH_POS = 1;
    static final int MSG_LENGTH_POS = 2;
    static final int SEND_ALL_PUBLICATIONS_POS = 3;
    static final int DATA_BEGIN_POS = 10;

    static final int SEND_ALL_PUBLICATIONS = 3;

    HashMap<String, List<SocketAddress>> subscriberList = new HashMap<>();
    HashMap<String, List<SocketAddress>> publisherList = new HashMap<>();

    SocketAddress newestSub;

    Broker(int port) {
        try {
            socket = new DatagramSocket(port);
            listener.go();
        } catch (java.lang.Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void onReceipt(DatagramPacket packet) {
        try {
            byte[] data = packet.getData();
            byte[] topicBuffer = new byte[data[TOPIC_LENGTH_POS]];
            boolean receivedCorrectly = false;
            String topic;
            switch (data[TYPE_OF_PACKET_POS]) {
                case 0: //Subscription
                    for (int i = 0; i < topicBuffer.length; i++) {
                        topicBuffer[i] = data[DATA_BEGIN_POS + i + 1];
                    }
                    topic = new String(topicBuffer);
                    handleSubscription(packet, topic);
                    System.out.println("New subscription to " + topic);
                    receivedCorrectly = true;
                    break;
                case 1: //Publication
                    for (int i = 0; i < topicBuffer.length; i++) {
                        topicBuffer[i] = data[DATA_BEGIN_POS + i + 1];
                    }
                    topic = new String(topicBuffer);
                    byte[] msgBuffer = new byte[data[MSG_LENGTH_POS]];
                    for (int i = 0; i < msgBuffer.length; i++) {
                        msgBuffer[i] = data[DATA_BEGIN_POS + topicBuffer.length + i + 1];
                    }
                    handlePublication(packet, topic, msgBuffer);
                    System.out.println("New publication to " + topic);
                    receivedCorrectly = true;
                    break;
                case 2: //unsub
                    for (int i = 0; i < topicBuffer.length; i++) {
                        topicBuffer[i] = data[DATA_BEGIN_POS + i + 1];
                    }
                    topic = new String(topicBuffer);
                    List<SocketAddress> currentTopicSubs = subscriberList.get(topic);
                    for (int i = 0; i < currentTopicSubs.size(); i++) {
                        if (currentTopicSubs.get(i).equals(packet.getSocketAddress())) {
                            currentTopicSubs.remove(currentTopicSubs.get(i));
                            break;
                        }
                    }
                    receivedCorrectly = true;
                    break;
                case 3:
                    for (int i = 0; i < topicBuffer.length; i++) {
                        topicBuffer[i] = data[DATA_BEGIN_POS + i + 1];
                    }
                    topic = new String(topicBuffer);
                    msgBuffer = new byte[data[MSG_LENGTH_POS]];
                    for (int i = 0; i < msgBuffer.length; i++) {
                        msgBuffer[i] = data[DATA_BEGIN_POS + topicBuffer.length + i + 1];
                    }
                    byte[] topicToSend = (topic + ": ").getBytes();
                    byte[] messageToSend = new byte[topicToSend.length + msgBuffer.length];
                    for (int i = 0; i < topicToSend.length; i++) {
                        messageToSend[i] = topicToSend[i];
                    }
                    for (int i = 0; i < msgBuffer.length; i++) {
                        messageToSend[topicToSend.length + i] = msgBuffer[i];
                    }
                    DatagramPacket messagePacket = new DatagramPacket(messageToSend, messageToSend.length);
                    messagePacket.setSocketAddress(newestSub);
                    socket.send(messagePacket);
                    receivedCorrectly = true;
                    break;
            }
            if (receivedCorrectly) {
                DatagramPacket response;
                byte[] res;
                String ack = "ok";
                res = ack.getBytes();
                response = new DatagramPacket(res, res.length);
                response.setSocketAddress(packet.getSocketAddress());
                socket.send(response);
            } else {
                DatagramPacket response;
                byte[] res;
                String ack = "Did not receive packet correctly";
                res = ack.getBytes();
                response = new DatagramPacket(res, res.length);
                response.setSocketAddress(packet.getSocketAddress());
                socket.send(response);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void userInput(String message) {

    }

    public void handleSubscription(DatagramPacket packet, String topic) throws IOException {
        newestSub = packet.getSocketAddress();
        if (!subscriberList.containsKey(topic)) {
            List<SocketAddress> subscriber = new ArrayList<>();
            subscriber.add(packet.getSocketAddress());
            subscriberList.put(topic, subscriber);
        } else {
            List<SocketAddress> tmpList = subscriberList.get(topic);
            tmpList.add(packet.getSocketAddress());
            subscriberList.put(topic, tmpList);
        }
        if (publisherList.containsKey(topic)){
            List<SocketAddress> tmpList = publisherList.get(topic);
            byte[] toSend = new byte[4];
            toSend[SEND_ALL_PUBLICATIONS_POS] = SEND_ALL_PUBLICATIONS;
            DatagramPacket requestingAllPublications = new DatagramPacket(toSend, toSend.length);
            for (int i = 0; i < tmpList.size(); i++){
                requestingAllPublications.setSocketAddress(tmpList.get(i));
                socket.send(requestingAllPublications);
            }
        }
    }

    public void handlePublication(DatagramPacket packet, String topic, byte[] message) throws IOException {
        if (!publisherList.containsKey(topic)){
            List<SocketAddress> publisher = new ArrayList<>();
            publisher.add(packet.getSocketAddress());
            publisherList.put(topic, publisher);
        }
        else if (!publisherList.containsValue(packet.getSocketAddress())) {
            List<SocketAddress> tmpList = publisherList.get(topic);
            tmpList.add(packet.getSocketAddress());
            publisherList.put(topic, tmpList);
        }
        if (subscriberList.get(topic) != null) {
            byte[] topicToSend = (topic + ": ").getBytes();
            byte[] messageToSend = new byte[topicToSend.length + message.length];
            for (int i = 0; i < topicToSend.length; i++) {
                messageToSend[i] = topicToSend[i];
            }
            for (int i = 0; i < message.length; i++) {
                messageToSend[topicToSend.length + i] = message[i];
            }
            DatagramPacket messagePacket = new DatagramPacket(messageToSend, messageToSend.length);
            List<SocketAddress> subscribers = subscriberList.get(topic);
            for (int i = 0; i < subscribers.size(); i++) {
                messagePacket.setSocketAddress(subscribers.get(i));
                socket.send(messagePacket);
            }
        } else {
            System.out.println("Sorry, there are no subscribers currently for this topic");
        }
    }

    public synchronized void start() {
        System.out.println("Waiting for contact");
        try {
            this.wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            Broker broker = new Broker(DEFAULT_PORT);
            broker.start();
            System.out.println("Program completed");
        } catch (java.lang.Exception e) {
            e.printStackTrace();
        }
    }
}