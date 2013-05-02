
public class RPlayNoGui {

	public static void main(String[] args) {
		
		
		if(args.length == 1) {
			// Name only
			new LaunchThread(args[0]).start();
		} else if(args.length == 2) {
			// Name and password
			new LaunchThread(args[0], args[1]).start();
		} else {
			System.err.println("Java port of shairport.");
			System.err.println("usage : java -jar " + RPlayNoGui.class.getCanonicalName() + ".jar <AP_name> [<password>]");
			System.exit(-1);
		}	
	}
}
