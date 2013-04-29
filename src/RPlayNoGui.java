
public class RPlayNoGui {

	public static void main(String[] args) {
		if (args.length < 1) {
			System.err.println("Java port of shairport.");
			System.err.println("usage : java -jar " + RPlayNoGui.class.getCanonicalName() + ".jar <AP_name>");
			
			System.exit(-1);
		}
		new LaunchThread(args[0]).start();
	}
	
}
