package dev.jorel.commandapi;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class Arguments {
	
	private final List<String> argumentNodeNames;
	private final List<Object> arguments;
	
	Arguments(List<String> argumentNodeNames, List<Object> arguments) {
		this.argumentNodeNames = argumentNodeNames;
		this.arguments = arguments; 
	}
	
	//Used for converted commands only, since arguments don't have names
	Arguments(String[] arguments) {
		this.argumentNodeNames = null;
		this.arguments = Arrays.asList(Arrays.copyOf(arguments, arguments.length, Object[].class)); 
	}
	
	public Object[] getArgs() {
		return this.arguments.toArray();
	}
	
	@SuppressWarnings("unchecked")
	public <T> T get(String name) {
		return (T) arguments.get(this.argumentNodeNames.indexOf(name));
	}
	
	@SuppressWarnings("unchecked")
	public <T> T get(int index) {
		return (T) arguments.get(index);
	}
	
	public <T> Optional<T> getOptional(String name) {
		return Optional.ofNullable(get(name));
	}
	
	public <T> Optional<T> getOptional(int index) {
		return Optional.ofNullable(get(index));
	}
	
}
