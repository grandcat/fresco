package dk.alexandra.fresco.demo;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.Iterator;
import java.util.logging.Logger;

import dk.alexandra.fresco.demo.CmdResult.Status;
import dk.alexandra.fresco.demo.PreparePhase.Participant;
import dk.alexandra.fresco.demo.SMCGrpc.SMCImplBase;
import io.grpc.Server;
import io.grpc.benchmarks.Utils;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;

public class RPCServer {
	private static final Logger logger = Logger.getLogger(RPCServer.class.getName());

	/* The port on which the server should run */
	private int port = 50052;
	private Server server;

	public RPCServer() {
		// TODO Auto-generated constructor stub
	}

	public void start() throws IOException {
		// this.server = ServerBuilder.forPort(port).addService(new
		// SMCImpl()).build().start();
		
		// Unix socket properties (Linux only)
		final EventLoopGroup boss;
		final EventLoopGroup worker;
		final Class<? extends ServerChannel> channelType;
		try {
	        Class<?> groupClass = Class.forName("io.netty.channel.epoll.EpollEventLoopGroup");
	        @SuppressWarnings("unchecked")
	        Class<? extends ServerChannel> channelClass = (Class<? extends ServerChannel>)
	            Class.forName("io.netty.channel.epoll.EpollServerDomainSocketChannel");
			boss = (EventLoopGroup) groupClass.getConstructor().newInstance();
			worker = (EventLoopGroup) groupClass.getConstructor().newInstance();
			channelType = channelClass;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		SocketAddress address = Utils.parseSocketAddress("unix:///tmp/grpc.sock");
		this.server = NettyServerBuilder.forAddress(address).bossEventLoopGroup(boss)
				.workerEventLoopGroup(worker).channelType(channelType).addService(new SMCImpl()).build().start();
		logger.info("RPC server started, listening on " + port);
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				// Use stderr here since the logger may have been reset by its
				// JVM shutdown hook.
				System.err.println("*** shutting down gRPC server since JVM is shutting down");
				RPCServer.this.stop();
				System.err.println("*** server shut down");
			}
		});
	}

	/**
	 * Await termination on the main thread since the grpc library uses daemon
	 * threads.
	 */
	public void blockUntilShutdown() throws InterruptedException {
		if (server != null) {
			server.awaitTermination();
		}
	}

	private void stop() {
		if (server != null) {
			server.shutdown();
		}
	}

	// For testing the cross language support
	public static void main(String[] args) throws IOException, InterruptedException {
		final RPCServer server = new RPCServer();
		server.start();
		server.blockUntilShutdown();
	}

	private class SMCImpl extends SMCImplBase {

		@Override
		public void init(SessionCtx req, StreamObserver<CmdResult> responseObserver) {
			// Reply
			CmdResult reply = CmdResult.newBuilder().setMsg("Init done. Nice session ID " + req.getSessionID())
					.setStatus(Status.SUCCESS).build();
			responseObserver.onNext(reply);
			responseObserver.onCompleted();
		}

		@Override
		public void doPrepare(PreparePhase req, StreamObserver<CmdResult> responseObserver) {
			StringBuilder sb = new StringBuilder();
			for (Iterator i = req.getParticipantsList().iterator(); i.hasNext();) {
				Participant p = (Participant) i.next();
				sb.append(p.getAddr());
				sb.append(';');
			}

			logger.info("doPrepare called: " + sb.toString());

			// Reply
			CmdResult reply = CmdResult.newBuilder().setMsg("doPrepare done.").setStatus(Status.SUCCESS).build();
			responseObserver.onNext(reply);
			responseObserver.onCompleted();
		}
	}

}
