import java.util.*;
import java.io.*;

public class StudentNetworkSimulator extends NetworkSimulator
{
    /*
     * Predefined Constants (static member variables):
     *
     *   int MAXDATASIZE : the maximum size of the Message data and
     *                     Packet payload
     *
     *   int A           : a predefined integer that represents entity A
     *   int B           : a predefined integer that represents entity B 
     *
     * Predefined Member Methods:
     *
     *  void stopTimer(int entity): 
     *       Stops the timer running at "entity" [A or B]
     *  void startTimer(int entity, double increment): 
     *       Starts a timer running at "entity" [A or B], which will expire in
     *       "increment" time units, causing the interrupt handler to be
     *       called.  You should only call this with A.
     *  void toLayer3(int callingEntity, Packet p)
     *       Puts the packet "p" into the network from "callingEntity" [A or B]
     *  void toLayer5(String dataSent)
     *       Passes "dataSent" up to layer 5
     *  double getTime()
     *       Returns the current time in the simulator.  Might be useful for
     *       debugging.
     *  int getTraceLevel()
     *       Returns TraceLevel
     *  void printEventList()
     *       Prints the current event list to stdout.  Might be useful for
     *       debugging, but probably not.
     *
     *
     *  Predefined Classes:
     *
     *  Message: Used to encapsulate a message coming from layer 5
     *    Constructor:
     *      Message(String inputData): 
     *          creates a new Message containing "inputData"
     *    Methods:
     *      boolean setData(String inputData):
     *          sets an existing Message's data to "inputData"
     *          returns true on success, false otherwise
     *      String getData():
     *          returns the data contained in the message
     *  Packet: Used to encapsulate a packet
     *    Constructors:
     *      Packet (Packet p):
     *          creates a new Packet that is a copy of "p"
     *      Packet (int seq, int ack, int check, String newPayload)
     *          creates a new Packet with a sequence field of "seq", an
     *          ack field of "ack", a checksum field of "check", and a
     *          payload of "newPayload"
     *      Packet (int seq, int ack, int check)
     *          chreate a new Packet with a sequence field of "seq", an
     *          ack field of "ack", a checksum field of "check", and
     *          an empty payload
     *    Methods:
     *      boolean setSeqnum(int n)
     *          sets the Packet's sequence field to "n"
     *          returns true on success, false otherwise
     *      boolean setAcknum(int n)
     *          sets the Packet's ack field to "n"
     *          returns true on success, false otherwise
     *      boolean setChecksum(int n)
     *          sets the Packet's checksum to "n"
     *          returns true on success, false otherwise
     *      boolean setPayload(String newPayload)
     *          sets the Packet's payload to "newPayload"
     *          returns true on success, false otherwise
     *      int getSeqnum()
     *          returns the contents of the Packet's sequence field
     *      int getAcknum()
     *          returns the contents of the Packet's ack field
     *      int getChecksum()
     *          returns the checksum of the Packet
     *      int getPayload()
     *          returns the Packet's payload
     *
     */

    /*   Please use the following variables in your routines.
     *   int WindowSize  : the window size
     *   double RxmtInterval   : the retransmission timeout
     *   int LimitSeqNo  : when sequence number reaches this value, it wraps around
     */

    public static final int FirstSeqNo = 0;
    private int WindowSize;
    private double RxmtInterval;
    private int LimitSeqNo;

    public static int aCurrentSeqNo;
    public static int aAcked;
    public static int bLastAcked;
    public static Packet[] aBuffer;
    public static Packet[] bBuffer;
    public static int corruptionCount = 0;
    public static int sendCount = 0;
    public static int receiveCount = 0;
    public static int retransmit = 0;

    // Add any necessary class variables here.  Remember, you cannot use
    // these variables to send messages error free!  They can only hold
    // state information for A or B.
    // Also add any necessary methods (e.g. checksum of a String)

    // This is the constructor.  Don't touch!
    public StudentNetworkSimulator(int numMessages,
                                   double loss,
                                   double corrupt,
                                   double avgDelay,
                                   int trace,
                                   int seed,
                                   int winsize,
                                   double delay)
    {
        super(numMessages, loss, corrupt, avgDelay, trace, seed);
	WindowSize = winsize;
	LimitSeqNo = winsize*2;
	RxmtInterval = delay;
    }

    public boolean checkSum(int seqNo, int ackNo, int checksum, String payload)
    {
        return (computeChecksum(seqNo, ackNo, payload) == checksum);
    }

    public int getStringSum(String str){
        int sum = 0;
        if(str != null && !str.isEmpty()){
            for (char c : str.toCharArray()){
                sum += (int) c;
            }
        }
        return sum;
    }

    public int computeChecksum(int seqNum, int ackNum, String payload){
        int checkSum = seqNum + ackNum;
        if(payload != null && !payload.isEmpty()){
            checkSum += getStringSum(payload);
        }
        return checkSum;
    }

    // This routine will be called whenever the upper layer at the sender [A]
    // has a message to send.  It is the job of your protocol to ensure that
    // the data in such a message is delivered in-order, and correctly, to
    // the receiving upper layer.
    protected void aOutput(Message message)
    {
        String payload = message.getData();
        int seqNum = aCurrentSeqNo;
        int ackNum = aCurrentSeqNo;
        aCurrentSeqNo++;
        int checkSum = computeChecksum(seqNum, ackNum, payload);
        Packet packet = new Packet(seqNum, ackNum, checkSum, payload);
        aBuffer[seqNum] = packet;
        if (seqNum < aAcked + WindowSize)
        {
            toLayer3(A, packet);
            // keep track of total packets sent
            sendCount++;
            // startTimer(A, RxmtInterval);
            System.out.println("Packet " + Integer.toString(ackNum) + " sent to B");
        }
    }
    
    // This routine will be called whenever a packet sent from the B-side 
    // (i.e. as a result of a toLayer3() being done by a B-side procedure)
    // arrives at the A-side.  "packet" is the (possibly corrupted) packet
    // sent from the B-side.
    protected void aInput(Packet packet)
    {
        receiveCount++;
        if (checkSum(packet.getSeqnum(), packet.getAcknum(), packet.getChecksum(), packet.getPayload()))
        {

            int prev_aAcked = aAcked;
            // update Ack index
            aAcked = packet.getSeqnum();
            // duplicate ack case
            if (aAcked == prev_aAcked)
            {
                if (aBuffer[aAcked + 1] != null)
                {
                    toLayer3(A, aBuffer[aAcked + 1]);
                    // keep track of total packets sent
                    sendCount++;
                    retransmit++;
                    System.out.println("Duplicate Ack " + Integer.toString(aAcked)
                            + " received at A. Packet " + Integer.toString(aAcked + 1)
                            + " re-sent to B.");
                    stopTimer(A);
                    startTimer(A, RxmtInterval);
                }
            }
            else
                {
                // Ack is not a duplicate.
                // discard packets from previously acked packet up to
                // currently acked packet.
                for (int i = prev_aAcked; i <= aAcked; i++)
                {
                    if (i > -1)
                    {
                        aBuffer[i] = null;
                    }
                }
                System.out.println("Window size adjusted from " + Integer.toString(prev_aAcked)
                        + " to " + Integer.toString(aAcked + WindowSize) + ".");
                // send packets within adjusted sender window
                for (int i = prev_aAcked + WindowSize; i < aAcked + WindowSize; i++)
                {
                    if (aBuffer[i] != null)
                    {
                        toLayer3(A, aBuffer[i]);
                        // keep track of total packets sent
                        sendCount++;
                        System.out.println("Packet " + Integer.toString(aBuffer[i].getAcknum())
                                + " sent to B after window adjustment.");

                    }
                }
                stopTimer(A);
                startTimer(A, RxmtInterval);
            }
        }
        else
            {
            corruptionCount += 1;
            }
    }
    
    // This routine will be called when A's timer expires (thus generating a 
    // timer interrupt). You'll probably want to use this routine to control 
    // the retransmission of packets. See startTimer() and stopTimer(), above,
    // for how the timer is started and stopped. 
    protected void aTimerInterrupt()
    {
        if (aBuffer[aAcked + 1] != null)
        {
            toLayer3(A, aBuffer[aAcked + 1]);
            // keep track of total packets sent
            sendCount++;
            retransmit++;
            System.out.println("Packet " + Integer.toString(aBuffer[aAcked + 1].getAcknum())
                    + " re-sent to B because of timeout.");
            startTimer(A, RxmtInterval);
        } else {
            startTimer(A, RxmtInterval);
        }
    }
    
    // This routine will be called once, before any of your other A-side 
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of entity A).
    protected void aInit()
    {
        aCurrentSeqNo = FirstSeqNo;
        aAcked = -1;
        aBuffer = new Packet[5000];
        startTimer(A, RxmtInterval);
    }
    
    // This routine will be called whenever a packet sent from the B-side 
    // (i.e. as a result of a toLayer3() being done by an A-side procedure)
    // arrives at the B-side.  "packet" is the (possibly corrupted) packet
    // sent from the A-side.
    protected void bInput(Packet packet)
    {
        // keep track of total packets delivered
        receiveCount++;
        if (checkSum(packet.getSeqnum(), packet.getAcknum(), packet.getChecksum(), packet.getPayload()))
        {
            System.out.println("Packet " + Integer.toString(packet.getSeqnum()) + " received at B.");
            bBuffer[packet.getSeqnum()] = packet;
            int i = bLastAcked + 1;
            while(bBuffer[i] != null)
            {
                toLayer5(bBuffer[i].getPayload());
                i++;
            }
            if (i - 1 > -1)
            {
                if (bBuffer[i - 1] != null)
                {
                    int checkSum = computeChecksum(bBuffer[i - 1].getSeqnum(), bBuffer[i - 1].getAcknum(), "");
                    Packet ackPack = new Packet(bBuffer[i - 1].getSeqnum(), bBuffer[i - 1].getAcknum(), checkSum);
                    // check if ack is duplicate
                    if (bLastAcked == i - 1)
                    {
                        toLayer3(B, ackPack);
                        sendCount++;
                        retransmit++;
                    }
                    // original packet
                    else
                    {
                        bLastAcked = i - 1;
                        toLayer3(B, ackPack);
                        // keep track of total packets sent
                        sendCount++;
                        System.out.println("Ack for " + Integer.toString(bBuffer[i - 1].getSeqnum())
                                + " sent from B to A");
                    }
                }
            }
        } else {
            corruptionCount += 1;
        }
    }
    
    // This routine will be called once, before any of your other B-side 
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of entity B).
    protected void bInit()
    {
        bBuffer = new Packet[5000];
        bLastAcked = -1;
    }

    // Use to print final statistics
    protected void Simulation_done()
    {
        System.out.println("Done!");
        System.out.println();
        System.out.println("Some stats: ");
        System.out.println("Total packets sent to/from both sides: " + Integer.toString(sendCount));
        System.out.println("Total packets received by both sides: " + Integer.toString(receiveCount));
        System.out.println("Total packets lost due to corruption: " + Integer.toString(corruptionCount));
        System.out.println("Total packets lost due to error: " + Integer.toString(sendCount - receiveCount
                - corruptionCount));
        System.out.println("Total retransmitted packets: " + Integer.toString(retransmit));
    }
}
