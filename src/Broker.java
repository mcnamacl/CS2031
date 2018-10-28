import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.util.*;

public class Broker extends Node {
    static final int DEFAULT_PORT = 50001;
    static final int DEFAULT_SRC_PORT = 50000;
    static final int DEFAULT_DST_PORT = 50001;
    static final String DEFAULT_DST_NODE = "localhost";

    Hashtable<String, List<SocketAddress>> subscriberList = new Hashtable<>();

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
            byte[] stbuffer= new byte[data[10]];
            for(int i=0;i<stbuffer.length;i++){stbuffer[i]=data[10+i+1];}
            String topic = new String(stbuffer);
            boolean receivedCorrectly = false;
            switch(data[0]) {
                case 0: //Subscription
                    handleSubscription(packet, topic);
                    receivedCorrectly = true;
                    break;
                case 1: //Publication
                    handlePublication(packet, topic);
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
            //this.notify();
        }
        catch(Exception e) {e.printStackTrace();}
    }

    public void handleSubscription(DatagramPacket packet, String topic){
        if (!subscriberList.contains(topic)) {
            System.out.println(topic);
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

    public void handlePublication(DatagramPacket packet, String topic) throws IOException {
            if (subscriberList.contains(topic)){
                DatagramPacket message;
                message = new DatagramPacket(packet.getData(), packet.getLength());
                List<SocketAddress> subscribers = subscriberList.get(topic);
                for (int i = 0; i < subscribers.size(); i++){
                    message.setSocketAddress(subscribers.get(i));
                    socket.send(message);
                }
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
           /* List<Subscriber> subscribers = new ArrayList<Subscriber>();
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
            }*/
            System.out.println("Program completed");
        } catch(java.lang.Exception e) {e.printStackTrace();}
    }
}