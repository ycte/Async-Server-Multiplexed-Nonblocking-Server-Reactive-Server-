import java.nio.*;
import java.nio.channels.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;



public class EchoLineReadWriteHandler implements IReadWriteHandler {

	private static final String WWW_ROOT = "./";
	private ByteBuffer inBuffer;
	private ByteBuffer outBuffer;

	private boolean requestComplete;
	private boolean responseReady;
	private boolean responseSent;
	private boolean channelClosed;
	static Map<String, String> fileCache = new HashMap<>();
	private StringBuffer request;

	// private enum State {
	// READ_REQUEST, REQUEST_COMPLETE, GENERATING_RESPONSE, RESPONSE_READY,
	// RESPONSE_SENT
	// }
	// private State state;

	public EchoLineReadWriteHandler() {
		inBuffer = ByteBuffer.allocate(4096);
		outBuffer = ByteBuffer.allocate(4096 * 1024);

		// initial state
		requestComplete = false;
		responseReady = false;
		responseSent = false;
		channelClosed = false;

		request = new StringBuffer(4096);
	}

	public int getInitOps() {
		return SelectionKey.OP_READ;
	}

	public void handleException() {
	}

	public void handleRead(SelectionKey key) throws Exception {

		// a connection is ready to be read
		Debug.DEBUG("->handleRead");

		if (requestComplete) { // this call should not happen, ignore
			return;
		}

		// process data
		processInBuffer(key);

		// update state
		updateState(key);

		Debug.DEBUG("handleRead->");

	} // end of handleRead

	private void updateState(SelectionKey key) throws IOException {

		Debug.DEBUG("->Update dispatcher.");

		if (channelClosed)
			return;

		/*
		 * if (responseSent) { Debug.DEBUG(
		 * "***Response sent; shutdown connection"); client.close();
		 * dispatcher.deregisterSelection(sk); channelClosed = true; return; }
		 */

		int nextState = key.interestOps();
		if (requestComplete) {
			nextState = nextState & ~SelectionKey.OP_READ;
			Debug.DEBUG("New state: -Read since request parsed complete");
		} else {
			nextState = nextState | SelectionKey.OP_READ;
			Debug.DEBUG("New state: +Read to continue to read");
		}

		if (responseReady) {

			if (!responseSent) {
				nextState = nextState | SelectionKey.OP_WRITE;
				Debug.DEBUG("New state: +Write since response ready but not done sent");
			} else {
				nextState = nextState & ~SelectionKey.OP_WRITE;
				Debug.DEBUG("New state: -Write since response ready and sent");
			}
		}

		key.interestOps(nextState);

	}

	public void handleWrite(SelectionKey key) throws IOException {
		Debug.DEBUG("->handleWrite");

		// process data
		SocketChannel client = (SocketChannel) key.channel();
		Debug.DEBUG("handleWrite: Write data to connection " + client + "; from buffer " + outBuffer);
		int writeBytes = client.write(outBuffer);
		Debug.DEBUG("handleWrite: write " + writeBytes + " bytes; after write " + outBuffer);

		if (responseReady && (outBuffer.remaining() == 0)) {
			responseSent = true;
			Debug.DEBUG("handleWrite: responseSent");
		}

		// update state
		updateState(key);

		// try {Thread.sleep(5000);} catch (InterruptedException e) {}
		Debug.DEBUG("handleWrite->");
	} // end of handleWrite

	private void processInBuffer(SelectionKey key) throws Exception {
		Debug.DEBUG("processInBuffer");
		SocketChannel client = (SocketChannel) key.channel();
		inBuffer.clear();
//		wait(10);
		int readBytes = client.read(inBuffer);
//		int count = 100;
//		int readCount = 0; // 已经成功读取的字节的个数
//		ByteBuffer[] b = new ByteBuffer[count];
//		while (readCount < count) {
//			readCount += client.read(b, readCount, count - readCount);
//		}
//		Debug.DEBUG(new String(b[0].array()));
		Debug.DEBUG("handleRead: Read data from connection " + client + " for " + readBytes + " byte(s); to buffer "
				+ inBuffer);
		Debug.DEBUG(new String(inBuffer.array()));
		if (readBytes == -1) { // end of stream
			requestComplete = true;

			Debug.DEBUG("handleRead: readBytes == -1");
		} else {
			Debug.DEBUG(inBuffer.capacity() + " " + inBuffer.limit());
			inBuffer.flip(); // read input
			// outBuffer = ByteBuffer.allocate( inBuffer.remaining() );
			while (!requestComplete && inBuffer.hasRemaining() && request.length() < request.capacity()) {
				char ch = (char) inBuffer.get();
//				Debug.DEBUG("Ch: " + ch);
				request.append(ch);
//				if (ch == '\r' || ch == '\n') {
//					requestComplete = true;
//					// client.shutdownInput();
//					Debug.DEBUG("handleRead: find terminating chars");
//				} // end if
			} // end of while
		}

//		inBuffer.clear(); // we do not keep things in the inBuffer
		requestComplete = true;
		if (requestComplete) {
		generateResponse();
		}

	} // end of process input

	private void generateResponse() throws Exception {
//		for (int i = 0; i < request.length(); i++) {
//			char ch = (char) request.charAt(i);
//
//			ch = Character.toUpperCase(ch);
//
//			outBuffer.put((byte) ch);
//		}
		WebRequestHandlerString wrh =
				new WebRequestHandlerString(inBuffer, Server.cfgMap, fileCache, Server.cacheSize);

		String re = wrh.processRequest();
		Debug.DEBUG("re:" + re.length());
//		Debug.DEBUG(re);
		for (int i = 0; i < re.length(); i++) {
			char ch = re.charAt(i);

//			ch = Character.toUpperCase(ch);

			outBuffer.put((byte) ch);
//			outBuffer.flip();
//			outBuffer.clear();
		}
//		Debug.DEBUG(new String(outBuffer.array()));
//		outBuffer.put(Byte.parseByte("iamshauige"));
		outBuffer.flip();
//		outBuffer.clear();
		responseReady = true;
	} // end of generate response
}