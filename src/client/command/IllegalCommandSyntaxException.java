

package client.command;

public class IllegalCommandSyntaxException extends Exception {
	private static final long serialVersionUID = 1L;

	public IllegalCommandSyntaxException() {
		super();
	}

	public IllegalCommandSyntaxException(String message) {
		super(message);
	}

	public IllegalCommandSyntaxException(int expectedArguments) {
		super("Expected atleast " + expectedArguments + " arguments");
	}
}
