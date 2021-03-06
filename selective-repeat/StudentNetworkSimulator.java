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
    
    // Add any necessary class variables here.  Remember, you cannot use
    // these variables to send messages error free!  They can only hold
    // state information for A or B.
    // Also add any necessary methods (e.g. checksum of a String)

    public Packet[] aPktBuffer;
    public Packet[] bPktBuffer;  
    public int nextSeqNum;
    public int nextAckNum;
    public int aBase;
    public int bBase;   
    public int lastAcked;

    // returns true if the packet in the aPktBuffer has been acknowledged
    // (ackNum set to -1 when first sent, updated to value of ackNum when ack is received)
    public boolean isAcked(int seqNum){
    	
    	if(aPktBuffer[seqNum] != null && aPktBuffer[seqNum].getAcknum() >= 0){
            return true;
        }	
        return false;
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
	    LimitSeqNo = winsize+1;
	    RxmtInterval = delay;
    }

    
    // This routine will be called whenever the upper layer at the sender [A]
    // has a message to send.  It is the job of your protocol to insure that
    // the data in such a message is delivered in-order, and correctly, to
    // the receiving upper layer.
    protected void aOutput(Message message)
    {
        if(nextSeqNum < 50){
	        String payload = message.getData(); 
	        int checkSum = computeChecksum(nextSeqNum, -1, payload); 
	        aPktBuffer[nextSeqNum] = new Packet(nextSeqNum, -1, checkSum, payload); 
	        if(nextSeqNum < aBase + WindowSize){
		        toLayer3(A, aPktBuffer[nextSeqNum]);    
		        if(nextSeqNum == FirstSeqNo){
                    startTimer(A, RxmtInterval); 
		        }
	        }    
	        nextSeqNum++;
        }         
    }
    
    // This routine will be called whenever a packet sent from the B-side 
    // (i.e. as a result of a toLayer3() being done by a B-side procedure)
    // arrives at the A-side.  "packet" is the (possibly corrupted) packet
    // sent from the B-side.
    protected void aInput(Packet packet)
    {
	    if(packet.getChecksum() == computeChecksum(packet.getSeqnum(), packet.getAcknum(), packet.getPayload())){
	        int ackNum = packet.getAcknum();
            if(aPktBuffer[ackNum] != null && isAcked(ackNum)){
                if(aPktBuffer[ackNum + 1] != null && !isAcked(ackNum + 1)){
                    toLayer3(A, aPktBuffer[ackNum + 1]);
                }
            }
	        if(ackNum < aBase + WindowSize && ackNum >= aBase){
		        stopTimer(A);
		        for(int i = aBase; i <= ackNum; i++){
		            aPktBuffer[i].setAcknum(i);
		            if(aPktBuffer[i + WindowSize - 1] != null){
			            toLayer3(A, aPktBuffer[i + WindowSize - 1]);
		            }   
		        }
		        aBase = ackNum + 1;
		        startTimer(A, RxmtInterval); 
	        }    
	    }
    }
    
    // This routine will be called when A's timer expires (thus generating a 
    // timer interrupt). You'll probably want to use this routine to control 
    // the retransmission of packets. See startTimer() and stopTimer(), above,
    // for how the timer is started and stopped. 
    protected void aTimerInterrupt()
    {
        toLayer3(A, aPktBuffer[aBase]);
        startTimer(A, RxmtInterval);  
    }

    
    // This routine will be called once, before any of your other A-side 
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of entity A).
    protected void aInit()
    {
    	aPktBuffer = new Packet[50];
	    nextSeqNum = FirstSeqNo;
	    aBase = FirstSeqNo;
    }
    
    // This routine will be called whenever a packet sent from the B-side 
    // (i.e. as a result of a toLayer3() being done by an A-side procedure)
    // arrives at the B-side.  "packet" is the (possibly corrupted) packet
    // sent from the A-side.
    protected void bInput(Packet packet)
    {    int seqNum = packet.getSeqnum();
         if(packet.getChecksum() == computeChecksum(seqNum, packet.getAcknum(), packet.getPayload())){
             packet.setAcknum(seqNum);
             bPktBuffer[seqNum] = new Packet(packet);
             if(lastAcked > -1 && seqNum > lastAcked && seqNum != bBase && bPktBuffer[lastAcked] != null){
                int lastSeqNum = bPktBuffer[lastAcked].getSeqnum();
                int lastAckNum = bPktBuffer[lastAcked].getAcknum();
                int lastCheck = computeChecksum(lastSeqNum, lastAckNum, "");
                toLayer3(B, new Packet(lastSeqNum, lastAckNum, lastCheck));  
             }              
             else if(seqNum == bBase){
                toLayer5(bPktBuffer[bBase].getPayload());
                bBase++;
                while(bPktBuffer[bBase] != null){
                    toLayer5(bPktBuffer[bBase].getPayload());
                    bBase++;
                }
                int newSeq = bPktBuffer[bBase - 1].getSeqnum();
                int newAck = bPktBuffer[bBase - 1].getAcknum();
                int newCheck = computeChecksum(newSeq, newAck, "");
                Packet ackPack = new Packet(newSeq, newAck, newCheck);  
                toLayer3(B, ackPack);
                lastAcked = newAck;
             }
              
         }
                        
    }
    
    // This routine will be called once, before any of your other B-side 
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of entity B).
    protected void bInit()
    {
	    bPktBuffer = new Packet[50];
	    nextAckNum = FirstSeqNo;
	    bBase = FirstSeqNo;
        lastAcked = -1;
    }

    // Use to print final statistics
    protected void Simulation_done()
    {
                 
    }	

}
