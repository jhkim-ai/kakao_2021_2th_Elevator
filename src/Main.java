public class Main {

	public static ElevatorController controller;
	
	public static void main(String[] args) {
		// 문제
//		String userKey = "tester";
//		int problemId = 0;
//		int numOfElevators = 4;
		
//		String userKey = "tester";
//		int problemId = 1;
//		int numOfElevators = 4;
		
		String userKey = "tester";
		int problemId = 2;
		int numOfElevators = 4;
		
		
		
		String response = start(userKey, problemId, numOfElevators);

		controller = new ElevatorController(problemId, numOfElevators);
		
		if (response.equals("200")) {
			controller.control();
		}
		
		
	}

	private static String start(String userKey, int problemId, int numOfElevators) {
		String response = TokenManger.getInstance().createToken(userKey, problemId, numOfElevators);
		System.out.println("Token : " + TokenManger.getInstance().getToken());
		return response;
	}

}
