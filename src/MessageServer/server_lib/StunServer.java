package MessageServer.server_lib;

import java.io.IOException;
import java.net.*;
import java.util.Vector;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.SimpleFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.javawi.jstun.attribute.ChangeRequest;
import de.javawi.jstun.attribute.ChangedAddress;
import de.javawi.jstun.attribute.MappedAddress;
import de.javawi.jstun.attribute.MessageAttributeException;
import de.javawi.jstun.attribute.MessageAttributeParsingException;
import de.javawi.jstun.attribute.ResponseAddress;
import de.javawi.jstun.attribute.SourceAddress;
import de.javawi.jstun.attribute.UnknownAttribute;
import de.javawi.jstun.attribute.UnknownMessageAttributeException;
import de.javawi.jstun.attribute.MessageAttributeInterface.MessageAttributeType;
import de.javawi.jstun.header.MessageHeader;
import de.javawi.jstun.header.MessageHeaderParsingException;
import de.javawi.jstun.header.MessageHeaderInterface.MessageHeaderType;
import de.javawi.jstun.util.Address;
import de.javawi.jstun.util.UtilityException;

public class StunServer {
//    private static final Logger LOGGER = LoggerFactory.getLogger(de.javawi.jstun.test.demo.StunServer.class);
    Vector<DatagramSocket> sockets;
    private boolean doRun;
    public StunServer(int primaryPort) throws SocketException {
        sockets = new Vector<DatagramSocket>();
        sockets.add(new DatagramSocket(primaryPort));
    }
    public void start() throws SocketException {
        doRun = true;
        for (DatagramSocket socket : sockets) {
            socket.setReceiveBufferSize(2000);
            socket.setSoTimeout(5000);
            StunServerReceiverThread ssrt = new StunServerReceiverThread(socket);
            ssrt.start();
        }
    }

    public void stop() {
        doRun = false;
        final long start = System.currentTimeMillis();
        for ( DatagramSocket socket : sockets ) {
            // Wait for a wile to allow sockets to disconnect gracefully.
            while ( !socket.isClosed() && System.currentTimeMillis() - start < 7000 ) {
                try {
                    Thread.sleep( 50 );
                } catch ( InterruptedException e ) {
                    e.printStackTrace();
                }
            }
            socket.close();
        }
    }

    /*
     * Inner class to handle incoming packets and react accordingly.
     * I decided not to start a thread for every received Binding Request, because the time
     * required to receive a Binding Request, parse it, generate a Binding Response and send
     * it varies only between 2 and 4 milliseconds. This amount of time is small enough so
     * that no extra thread is needed for incoming Binding Request.
     */
    class StunServerReceiverThread extends Thread {
        private DatagramSocket receiverSocket;
        private DatagramSocket changedPort;
        private DatagramSocket changedIP;
        private DatagramSocket changedPortIP;

        StunServerReceiverThread(DatagramSocket datagramSocket) {
            this.receiverSocket = datagramSocket;
            for (DatagramSocket socket : sockets) {
                if ((socket.getLocalPort() != receiverSocket.getLocalPort()) &&
                        (socket.getLocalAddress().equals(receiverSocket.getLocalAddress())))
                    changedPort = socket;
                if ((socket.getLocalPort() == receiverSocket.getLocalPort()) &&
                        (!socket.getLocalAddress().equals(receiverSocket.getLocalAddress())))
                    changedIP = socket;
                if ((socket.getLocalPort() != receiverSocket.getLocalPort()) &&
                        (!socket.getLocalAddress().equals(receiverSocket.getLocalAddress())))
                    changedPortIP = socket;
            }
        }

        public void run() {
            while (doRun) {
                try {
                    DatagramPacket receive = new DatagramPacket(new byte[200], 200);
                    receiverSocket.receive(receive);
//                    LOGGER.debug(receiverSocket.getLocalAddress().getHostAddress() + ":" + receiverSocket.getLocalPort() + " datagram received from " + receive.getAddress().getHostAddress() + ":" + receive.getPort());
                    MessageHeader receiveMH = MessageHeader.parseHeader(receive.getData());
                    try {
                        receiveMH.parseAttributes(receive.getData());
                        if (receiveMH.getType() == MessageHeaderType.BindingRequest) {
//                            LOGGER.debug(receiverSocket.getLocalAddress().getHostAddress() + ":" + receiverSocket.getLocalPort() + " Binding Request received from " + receive.getAddress().getHostAddress() + ":" + receive.getPort());
                            ChangeRequest cr = (ChangeRequest) receiveMH.getMessageAttribute(MessageAttributeType.ChangeRequest);
                            if (cr == null) throw new MessageAttributeException("Message attribute change request is not set.");
                            ResponseAddress ra = (ResponseAddress) receiveMH.getMessageAttribute(MessageAttributeType.ResponseAddress);

                            MessageHeader sendMH = new MessageHeader(MessageHeaderType.BindingResponse);
                            sendMH.setTransactionID(receiveMH.getTransactionID());

                            // Mapped address attribute
                            MappedAddress ma = new MappedAddress();
                            ma.setAddress(new Address(receive.getAddress().getAddress()));
                            ma.setPort(receive.getPort());
                            sendMH.addMessageAttribute(ma);
                            // Changed address attribute
                            ChangedAddress ca = new ChangedAddress();
                            ca.setAddress(new Address(changedPortIP.getLocalAddress().getAddress()));
                            ca.setPort(changedPortIP.getLocalPort());
                            sendMH.addMessageAttribute(ca);
                            if (cr.isChangePort() && (!cr.isChangeIP())) {
//                                LOGGER.debug("Change port received in Change Request attribute");
                                // Source address attribute
                                SourceAddress sa = new SourceAddress();
                                sa.setAddress(new Address(changedPort.getLocalAddress().getAddress()));
                                sa.setPort(changedPort.getLocalPort());
                                sendMH.addMessageAttribute(sa);
                                byte[] data = sendMH.getBytes();
                                DatagramPacket send = new DatagramPacket(data, data.length);
                                if (ra != null) {
                                    send.setPort(ra.getPort());
                                    send.setAddress(ra.getAddress().getInetAddress());
                                } else {
                                    send.setPort(receive.getPort());
                                    send.setAddress(receive.getAddress());
                                }
                                changedPort.send(send);
//                                LOGGER.debug(changedPort.getLocalAddress().getHostAddress() + ":" + changedPort.getLocalPort() + " send Binding Response to " + send.getAddress().getHostAddress() + ":" + send.getPort());
                            } else if ((!cr.isChangePort()) && cr.isChangeIP()) {
//                                LOGGER.debug("Change ip received in Change Request attribute");
                                // Source address attribute
                                SourceAddress sa = new SourceAddress();
                                sa.setAddress(new Address(changedIP.getLocalAddress().getAddress()));
                                sa.setPort(changedIP.getLocalPort());
                                sendMH.addMessageAttribute(sa);
                                byte[] data = sendMH.getBytes();
                                DatagramPacket send = new DatagramPacket(data, data.length);
                                if (ra != null) {
                                    send.setPort(ra.getPort());
                                    send.setAddress(ra.getAddress().getInetAddress());
                                } else {
                                    send.setPort(receive.getPort());
                                    send.setAddress(receive.getAddress());
                                }
                                changedIP.send(send);
//                                LOGGER.debug(changedIP.getLocalAddress().getHostAddress() + ":" + changedIP.getLocalPort() + " send Binding Response to " + send.getAddress().getHostAddress() + ":" + send.getPort());
                            } else if ((!cr.isChangePort()) && (!cr.isChangeIP())) {
//                                LOGGER.debug("Nothing received in Change Request attribute");
                                // Source address attribute
                                SourceAddress sa = new SourceAddress();
                                sa.setAddress(new Address(receiverSocket.getLocalAddress().getAddress()));
                                sa.setPort(receiverSocket.getLocalPort());
                                sendMH.addMessageAttribute(sa);
                                byte[] data = sendMH.getBytes();
                                DatagramPacket send = new DatagramPacket(data, data.length);
                                if (ra != null) {
                                    send.setPort(ra.getPort());
                                    send.setAddress(ra.getAddress().getInetAddress());
                                } else {
                                    send.setPort(receive.getPort());
                                    send.setAddress(receive.getAddress());
                                }
                                receiverSocket.send(send);
//                                LOGGER.debug(receiverSocket.getLocalAddress().getHostAddress() + ":" + receiverSocket.getLocalPort() + " send Binding Response to " + send.getAddress().getHostAddress() + ":" + send.getPort());
                            } else if (cr.isChangePort() && cr.isChangeIP()) {
//                                LOGGER.debug("Change port and ip received in Change Request attribute");
                                // Source address attribute
                                SourceAddress sa = new SourceAddress();
                                sa.setAddress(new Address(changedPortIP.getLocalAddress().getAddress()));
                                sa.setPort(changedPortIP.getLocalPort());
                                sendMH.addMessageAttribute(sa);
                                byte[] data = sendMH.getBytes();
                                DatagramPacket send = new DatagramPacket(data, data.length);
                                if (ra != null) {
                                    send.setPort(ra.getPort());
                                    send.setAddress(ra.getAddress().getInetAddress());
                                } else {
                                    send.setPort(receive.getPort());
                                    send.setAddress(receive.getAddress());
                                }
                                changedPortIP.send(send);
//                                LOGGER.debug(changedPortIP.getLocalAddress().getHostAddress() + ":" + changedPortIP.getLocalPort() + " send Binding Response to " + send.getAddress().getHostAddress() + ":" + send.getPort());
                            }
                        }
                    } catch (UnknownMessageAttributeException umae) {
                        umae.printStackTrace();
                        // Generate Binding error response
                        MessageHeader sendMH = new MessageHeader(MessageHeaderType.BindingErrorResponse);
                        sendMH.setTransactionID(receiveMH.getTransactionID());

                        // Unknown attributes
                        UnknownAttribute ua = new UnknownAttribute();
                        ua.addAttribute(umae.getType());
                        sendMH.addMessageAttribute(ua);

                        byte[] data = sendMH.getBytes();
                        DatagramPacket send = new DatagramPacket(data, data.length);
                        send.setPort(receive.getPort());
                        send.setAddress(receive.getAddress());
                        receiverSocket.send(send);
//                        LOGGER.debug(changedPortIP.getLocalAddress().getHostAddress() + ":" + changedPortIP.getLocalPort() + " send Binding Error Response to " + send.getAddress().getHostAddress() + ":" + send.getPort());
                    }
                } catch (SocketTimeoutException ioe) {
                    // No data for SO_TIMEOUT milliseconds.
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                } catch (MessageAttributeParsingException mape) {
                    mape.printStackTrace();
                } catch (MessageAttributeException mae) {
                    mae.printStackTrace();
                } catch (MessageHeaderParsingException mhpe) {
                    mhpe.printStackTrace();
                } catch (UtilityException ue) {
                    ue.printStackTrace();
                } catch (ArrayIndexOutOfBoundsException aioobe) {
                    aioobe.printStackTrace();
                }
            }
            receiverSocket.close();
        }
    }
}
