
public class RPlayNoGui {
	
	
	private static final void usage() {
		System.err.println("Java port of shairport.");
		System.err.println("usage : ");
		System.err.println("     java "+RPlayNoGui.class.getCanonicalName()+" <AP_name>");
	}

	public static void main(String[] args) {
		if (args.length != 1 && args[1].length()>1) {
			usage();
			System.exit(-1);
		}
		new LaunchThread(args[0]).start();
	}
	
}
