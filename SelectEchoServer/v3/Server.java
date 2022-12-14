import java.nio.channels.*;
import java.net.*;
import java.io.IOException;
import java.util.HashMap;

public class Server {

	public static int DEFAULT_PORT = 6789;

	public static int cacheSize = 8096;

	public static int reqCnt = 0;

	static HashMap<String, String> cfgMap = new HashMap<String, String>();

	public static ServerSocketChannel openServerChannel(int port) {
		ServerSocketChannel serverChannel = null;
		try {

			// open server socket for accept
			serverChannel = ServerSocketChannel.open();

			// extract server socket of the server channel and bind the port
			ServerSocket ss = serverChannel.socket();
			InetSocketAddress address = new InetSocketAddress(port);
			ss.bind(address);

			// configure it to be non blocking
			serverChannel.configureBlocking(false);

			Debug.DEBUG("Server listening for connections on port " + port);

		} catch (IOException ex) {
			ex.printStackTrace();
			System.exit(1);
		} // end of catch

		return serverChannel;
	} // end of open serverChannel

	public static void main(String[] args) throws IOException {
		int port;
		port = DEFAULT_PORT;
		// get dispatcher/selector
		Dispatcher dispatcher = new Dispatcher();
		// see if there is .conf
		cfgMap.put("vb_default", "./");
		String confName = "httpd.conf";
		if (args.length >= 2)
			if (args[0].equals("-config")) {
				confName = args[1];
				FileTest cfg = new FileTest();
				String cfgFileContent = cfg.cfgRead(confName);
				cfgMap = cfg.generateCfgMap(cfgFileContent);
				port = Integer.parseInt(cfgMap.get("Listen"));
				cacheSize = Integer.parseInt(cfgMap.get("CacheSize"));
				System.out.println(cfgMap.get("vb_yunxi.site"));
				System.out.println(cfgMap.toString());
			}
		// open server socket channel
		ServerSocketChannel sch = openServerChannel(port);

		// create server acceptor for Echo Line ReadWrite Handler
		ISocketReadWriteHandlerFactory echoFactory = new EchoLineReadWriteHandlerFactory();
		Acceptor acceptor = new Acceptor(echoFactory);

		Thread dispatcherThread;
		// register the server channel to a selector
		try {
			SelectionKey key = sch.register(dispatcher.selector(), SelectionKey.OP_ACCEPT);
			key.attach(acceptor);

			// start dispatcher
			dispatcherThread = new Thread(dispatcher);
			dispatcherThread.start();
		} catch (IOException ex) {
			System.out.println("Cannot register and start server");
			System.exit(1);
		}
		// may need to join the dispatcher thread

	} // end of main

} // end of class
