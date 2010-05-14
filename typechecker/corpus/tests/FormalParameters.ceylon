class FormalParameters {
	
	
	class MethodParameters {
	
		void noParameter() {}
	
		void singleParameter(Natural count) {}
		
		void multipleParameters(Natural count, String description) {}
		
		void defaultValueParameters(Natural param=1, 
		                            String description="", 
		                            Lock lock=null) {}
		
		void varargsParameter(String... strings) {}
		
		void entryParameter(String name->Person person) {}
		
		void rangeParameter(Natural min..max) {}
		
		void combinationParameters(String name->Person person,
		                           Natural count, 
		                           String description="", 
		                           Natural min..max = 1..10, 
		                           Lock lock=null, 
		                           String... strings) {}
		                                 
	}
	
	class InitParameters {
	
		class NoParameter() {}
	
		class SingleParameter(Natural count) {}
		
		class MultipleParameters(Natural count, String description) {}
		
		class DefaultValueParameters(Natural param=1, 
		                                  String description="", 
		                                  Lock lock=null) {}
		
		class VarargsParameter(String... strings) {}
		
		class EntryParameter(String name->Person person) {}
		
		class RangeParameter(Natural min..max) {}
		
		class CombinationParameters(String name->Person person,
		                            Natural count, 
		                            String description="", 
		                            Natural min..max = 1..10, 
		                            Lock lock=null, 
		                            String... strings) {}
		                                 
	}
	
	
}