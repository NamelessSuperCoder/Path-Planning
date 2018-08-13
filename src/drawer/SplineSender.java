package drawer;

import edu.wpi.first.wpilibj.networktables.NetworkTable;

public class SplineSender {

	//	TODO make it so that you can change these and it will
//	public static final String IP_ADDRESS = "10.0.0.24"; //IP address to connect to when in server mode
	//	public static final String NETWORK_TABLE_TABLE_KEY = "SmartDashboard"; //network table to send the data to
	public static final String NETWORK_TABLE_TABLE_KEY = String
		.format("Path Planner - %s", System.getProperty("user.name")); //network table to send the data to
	private static final int TEAM_NUMBER = 2974; // team number
	private static final boolean IS_CLIENT = false; // if the program will send to robotRIO or not
	public static NetworkTable networkTable;

	private static boolean hasBeenStarted = false;

	/**
	 * Initializes the network table with ip address settings etc...
	 */
	public static void initNetworkTable() {

		if (!hasBeenStarted) {
			hasBeenStarted = true;

			if (IS_CLIENT) {
				NetworkTable.setClientMode();
				NetworkTable.setTeam(TEAM_NUMBER);
			} else {
				NetworkTable.setServerMode();
//				NetworkTable.setIPAddress(IP_ADDRESS);
				NetworkTable.setIPAddress("localhost");
			}

			networkTable = NetworkTable.getTable(NETWORK_TABLE_TABLE_KEY);
//			networkTable.putString("Hello", "Hello");
		}
	}

	public static void initNetworkTableParallel() {
		Thread thread = new Thread(SplineSender::initNetworkTable);
		thread.start();
	}
}