import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.util.*;

public class Broker extends Node {
    static final int DEFAULT_PORT = 50001;

    HashMap<String, List<SocketAddress>> subscriberList = new HashMap<>();

    Broker( int port) {
        try {
            socket= new DatagramSocket(port);
            listener.go();
        }
        catch(java.lang.Exception e) {e.printStackTrace();}
    }

    public synchronized void onReceipt(DatagramPacket packet) {
        try {
            byte[] data= packet.getData();
            byte[] topicBuffer= new byte[data[10]];
            boolean receivedCorrectly = false;
            String topic;
            switch(data[0]) {
                case 0: //Subscription
                    for(int i=0;i<topicBuffer.length;i++){topicBuffer[i]=data[10+i+1];}
                    topic = new String(topicBuffer);
                    handleSubscription(packet, topic);
                    System.out.println("New subscription to " + topic);
                    receivedCorrectly = true;
                    break;
                case 1: //Publication
                    for(int i=0;i<topicBuffer.length;i++){topicBuffer[i]=data[11+i+1];}
                    topic = new String(topicBuffer);
                    byte[] msgBuffer = new byte[data[11]];
                    for (int i = 0; i < msgBuffer.length; i++){msgBuffer[i]=data[11 + topicBuffer.length + i + 1];}
                    handlePublication(topic, msgBuffer);
                    System.out.println("New publication to " + topic);
                    receivedCorrectly = true;
                    break;
                case 3: //unsub
                    for(int i=0;i<topicBuffer.length;i++){topicBuffer[i]=data[10+i+1];}
                    topic = new String(topicBuffer);
                    List<SocketAddress> currentTopicSubs = subscriberList.get(topic);
                    for (int i = 0; i < currentTopicSubs.size(); i++){
                        if(currentTopicSubs.get(i).equals(packet.getSocketAddress())){
                            currentTopicSubs.remove(currentTopicSubs.get(i));
                        }
                    }
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
            }
            else {
                DatagramPacket response;
                byte[] res;
                String ack = "did not receive packet correctly";
                res = ack.getBytes();
                response = new DatagramPacket(res, res.length);
                response.setSocketAddress(packet.getSocketAddress());
                socket.send(response);
            }
        }
        catch(Exception e) {e.printStackTrace();}
    }

    public void handleSubscription(DatagramPacket packet, String topic){
        if (!subscriberList.containsKey(topic)) {
            List<SocketAddress> subscriber = new ArrayList<>();
            subscriber.add(packet.getSocketAddress());
            subscriberList.put(topic, subscriber);
        }
        else {
            List<SocketAddress> tmpList = subscriberList.get(topic);
            tmpList.add(packet.getSocketAddress());
            subscriberList.put(topic, tmpList);
        }
    }

    public void handlePublication(String topic, byte[] message) throws IOException {
            if (subscriberList.get(topic) != null){
                byte[] topicToSend = (topic + " : ").getBytes();
                byte[] messageToSend = new byte[topicToSend.length + message.length];
                for (int i = 0; i < topicToSend.length; i++){messageToSend[i] = topicToSend[i];}
                for (int i = 0; i < message.length; i++){messageToSend[topicToSend.length + i] = message[i];}
                DatagramPacket messagePacket = new DatagramPacket(messageToSend, messageToSend.length);
                List<SocketAddress> subscribers = subscriberList.get(topic);
                for (int i = 0; i < subscribers.size(); i++){
                    messagePacket.setSocketAddress(subscribers.get(i));
                    socket.send(messagePacket);
                }
            }
            else {
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
        } catch(java.lang.Exception e) {e.printStackTrace();}
    }
}