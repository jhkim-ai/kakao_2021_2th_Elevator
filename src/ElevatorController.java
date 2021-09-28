import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/*	Elevator Controller Using SCAN Algorithm
 * */

public class ElevatorController {
	/* Commands */
	public static final String COMMAND_STOP = "STOP";
	public static final String COMMAND_ENTER = "ENTER";
	public static final String COMMAND_EXIT = "EXIT";
	public static final String COMMAND_UP = "UP";
	public static final String COMMAND_DOWN = "DOWN";
	public static final String COMMAND_OPEN = "OPEN";
	public static final String COMMAND_CLOSE = "CLOSE";
	/* Elevator Status */
	public static final String STATUS_STOPPED = "STOPPED";
	public static final String STATUS_OPENED = "OPENED";
	public static final String STATUS_UPWARD = "UPWARD";
	public static final String STATUS_DOWNWARD = "DOWNWARD";
	/* Direction of Elevators and Calls */
	public static final int DIR_UP = 1;
	public static final int DIR_DOWN = -1;
	
	public static final int MAXIMUM_NUMBER_OF_PEOPLE = 8;
	
	private JSONParser parser = new JSONParser();
	
	private List<Elevator> elevatorList = new ArrayList<>();
	private List<Call> callList = new ArrayList<>();
	private List<Command> commandList = new ArrayList<>();
	
	private int[] elevDirection; // default: 1 (DIR_UP)
	// call Direction은 Call.getDirection()을 이용한다.
	
	private int minHeight = 1;
	private int maxHeight;
	
	public ElevatorController(int problemId, int numOfElevators) {
		
		switch (problemId) {
		case GlobalData.APEACH_MANSION:
			System.out.println("APEACH_MANSION");
			this.maxHeight = GlobalData.APEACH_MANSION_HEIGHT;
			break;
		case GlobalData.JAY_G_BUILDING:
			System.out.println("JAY_G_BUILDING");
			this.maxHeight = GlobalData.JAY_G_BUILDING_HEIGHT;
			break;
		case GlobalData.RYAN_TOWER:
			System.out.println("RYAN_TOWER");
			this.maxHeight = GlobalData.RYAN_TOWER_HEGITH;
			break;
		}
		
		this.elevDirection = new int[numOfElevators];
		Arrays.fill(this.elevDirection, DIR_UP);
		
		System.out.println("INIT");
		System.out.println("num of Elevator : " + numOfElevators + "\nelevDirection : " + Arrays.toString(elevDirection));
	}
	
	public void control() {
		int time = -1;
		
		while (true) {
			time++;
			System.out.println("\n\n---------------timestamp : " + time + "--------------");
			
			boolean is_finished = onCalls();
			if (is_finished) {
				System.out.println("<< FINISHED!! >>");
				break;
			}
			
			System.out.println("남은  Call 개수 :" + callList.size());
			
//			System.out.println("\n***elevator command generator loop start\n");
//			System.out.println("\nDirections:");
//			System.out.println(Arrays.toString(elevDirection));
//			
//			System.out.println("floor");
//			elevatorList.forEach(e -> System.out.print(e.getFloor() + ", "));
			
			
			generateCommands();
//			
//			System.out.println("\n***elevator command generator loop end\n");	
			System.out.println("\n Commands");
			commandList.stream().forEach(c -> System.out.println(c.getJsonCommandData().toString()));
//			System.out.println(" - job list - \n" + Arrays.toString(jobs));

			action();
			try {
				Thread.sleep(25); // 40 request per second
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
		}
	}
	
	
	private void generateCommands() {
		commandList = new ArrayList<>();

		for (Elevator elevator : elevatorList) {
			Command command = generateCommand(elevator);
			commandList.add(command);
		}
	}

	private void action() {
		
		JSONArray commandArray = parser.getCommandsJSONArray(commandList);
		
		Connection.getInstance().action(commandArray);
	}
	
	
	private Command generateCommand(Elevator elevator) {
		int eID = elevator.getId(); // elevator ID
		int curFloor = elevator.getFloor();
		String curStatus = elevator.getStatus();
		ArrayList<Call> curPassengers = elevator.getPassengers();
		int numOfCurPassengers = curPassengers.size();
		int curDir = elevDirection[eID];
		
		Command command = new Command(eID); // it'll be returned

		boolean takeOff = curPassengers.stream().anyMatch((c) -> c.getEnd() == curFloor); // 내릴 손님 유무
		
		boolean takeOnSameDir = callList.stream().anyMatch((c) -> c.getStart() == curFloor && c.getDirection() == curDir); // 같은 방향으로 갈 손님의 유무
		
		boolean takeOnDiffDir = callList.stream().anyMatch((c) -> c.getStart() == curFloor && c.getDirection() != curDir); // 최상층, 반대방향으로 갈 손님의 유무
		
		// 상승 중
		if (curDir == DIR_UP) { 
			
			// 상승중이며 최상층일때
			if (curFloor == maxHeight) {
				
				// 올라가던 중이였으면 정지
				if (curStatus.equals(STATUS_UPWARD)) { 
					command.setCommand(COMMAND_STOP);
				} 
				// 올라가던 상태 완료
				
				// 정지된 상태면 탈사람, 내릴사람 판단하여 command
				else if (curStatus.equals(STATUS_STOPPED)) { 
					
					if (takeOff || takeOnDiffDir) { // 내릴사람, 탈사람 있다면 문 열기
						command.setCommand(COMMAND_OPEN);
					} else { // 없다면 반대 방향으로 이동
						command.setCommand(COMMAND_DOWN);
						toggleElevDir(eID);
					}
				}
				// 정지된 상태 완료
				
				
				// 문이 열려 있는 상태라면
				else if (curStatus.equals(STATUS_OPENED)) {
					
					// 1. 내릴사람이 있으면 내린다
					if (takeOff) {
						int[] callIds = new int[numOfCurPassengers];
						int idx = 0;
						for (Call passenger : curPassengers) {
							callIds[idx++] = passenger.getId();
						}
						command.setCommand(COMMAND_EXIT);
						command.setCallIds(callIds);
					} 

					// 2. 탈사람이 있으면 태운다
					else if (takeOnDiffDir && MAXIMUM_NUMBER_OF_PEOPLE - numOfCurPassengers > 0) {
						// 태운 사람은 callList에서 삭제하여 다음 엘리베이터에서 callList를 사용할 때 중복된 passenger를 태우지 못하도록 한다.
						int count = MAXIMUM_NUMBER_OF_PEOPLE - numOfCurPassengers;
						
						List<Call> removeList = new ArrayList<>();
						for (Call passenger : callList) { // 모든 call중 최상층, DOWN방향이면 다 태운다.
							if (passenger.getDirection() == DIR_DOWN && passenger.getStart() == maxHeight) {
								curPassengers.add(passenger);
								removeList.add(passenger);
								count--;
								if (count == 0)
									break;
							}
						}
						
						callList.removeAll(removeList);
						
						int[] callIds = new int[curPassengers.size()];
						int idx = 0;
						for (Call passenger : curPassengers) {
							callIds[idx++] = passenger.getId();
						}
						command.setCommand(COMMAND_ENTER);
						command.setCallIds(callIds);
					}

					// 3. 내릴사람, 탈사람이 더이상 없으면 문을닫고 반대방향으로 지정한다.
					else {
						command.setCommand(COMMAND_CLOSE);
						toggleElevDir(eID);
					}
				}
				// 문 열려있는 상태 완료
				
				
			// 상승중이며 최상층이 아닐 때	
			} else {
				
				// 내릴사람이 있다
				if (takeOff) {
					
					// 올라가는 중 이면 STOP
					if (curStatus.equals(STATUS_UPWARD)) {
						command.setCommand(COMMAND_STOP);
					
					//	정지 상태이면 OPEN
					} else if (curStatus.equals(STATUS_STOPPED)) {
						command.setCommand(COMMAND_OPEN);
					
					// 문이 열려 있으면 내릴 사람들 내려주기
					} else if (curStatus.equals(STATUS_OPENED)) {
						List<Integer> takeOffList = new LinkedList<>();
						for (Call passenger : curPassengers) {
							if (passenger.getEnd() == curFloor) { // 내릴층이 같으면
								takeOffList.add(passenger.getId());
							}
						}
						int[] callIds = new int[takeOffList.size()];
						int idx = 0;
						for (Integer id : takeOffList) {
							callIds[idx++] = id;
						}
						command.setCommand(COMMAND_EXIT);
						command.setCallIds(callIds);
					}
					
				// 내릴사람이 없고 탈사람만 있으면서 탈 공간이 있다.
				} else if (takeOnSameDir && numOfCurPassengers < MAXIMUM_NUMBER_OF_PEOPLE) {
					
					// 올라가는 중 이면 STOP
					if (curStatus.equals(STATUS_UPWARD)) {
						command.setCommand(COMMAND_STOP);
						
					// 정지 상태이면 OPEN
					} else if (curStatus.equals(STATUS_STOPPED)) {
						command.setCommand(COMMAND_OPEN);
					
					// 문이 열려 있으면 태울 사람 태우기
					} else if (curStatus.equals(STATUS_OPENED)) {
						// 태운 사람은 callList에서 삭제하여 다음 엘리베이터에서 callList를 사용할 때 중복된 passenger를 태우지 못하도록 한다.
						List<Integer> takeOnList = new LinkedList<>();
						List<Call> removeList = new ArrayList<>();
						int count = MAXIMUM_NUMBER_OF_PEOPLE - numOfCurPassengers;
						for (Call passenger : callList) { // 모든 call중 현재 층, UP방향이면 다 태운다.
							if (passenger.getDirection() == curDir && passenger.getStart() == curFloor) {
								takeOnList.add(passenger.getId());
								removeList.add(passenger);
								count--;
								if (count == 0)
									break;
							}
						}
						callList.removeAll(removeList);
						int[] callIds = new int[takeOnList.size()];
						int idx = 0;
						for (Integer id : takeOnList) {
							callIds[idx++] = id;
						}
						command.setCommand(COMMAND_ENTER);
						command.setCallIds(callIds);
					}
					
					
				// 내릴사람도 없고 탈사람도 없다 -> 올라가도록 함
				} else {
					
					// 문 열려있으면 문닫고
					if (curStatus.equals(STATUS_OPENED)) {
						command.setCommand(COMMAND_CLOSE);
					
					// 문 닫겨있거나 위로 올라가던 중이라면, 올라가거라
					} else {
						command.setCommand(COMMAND_UP);
					}
					
				}
				
			}
			// 상승중이며 최상층 아닌 경우 완료 안됨
			
			
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		// 하강 중
		} else if (curDir == DIR_DOWN) { 
			
			// 하강중이며 최하층일때
			if (curFloor == minHeight) {
				
				// 내려가던 중이였으면 정지
				if (curStatus.equals(STATUS_DOWNWARD)) { 
					command.setCommand(COMMAND_STOP);
				} 
				// 내려가던 상태 완료
				
				// 정지된 상태면 탈사람, 내릴사람 판단하여 command
				else if (curStatus.equals(STATUS_STOPPED)) { 
					
					if (takeOff || takeOnDiffDir) { // 내릴사람, 탈사람 있다면 문 열기
						command.setCommand(COMMAND_OPEN);
					} else { // 없다면 반대 방향으로 이동
						command.setCommand(COMMAND_UP);
						toggleElevDir(eID);
					}
				}
				// 정지된 상태 완료
				
				
				// 문이 열려 있는 상태라면
				else if (curStatus.equals(STATUS_OPENED)) {
					
					// 1. 내릴사람이 있으면 내린다
					if (takeOff) {
						int[] callIds = new int[numOfCurPassengers];
						int idx = 0;
						for (Call passenger : curPassengers) {
							callIds[idx++] = passenger.getId();
						}
						command.setCommand(COMMAND_EXIT);
						command.setCallIds(callIds);
					} 

					// 2. 탈사람이 있으면 태운다
					else if (takeOnDiffDir && MAXIMUM_NUMBER_OF_PEOPLE - numOfCurPassengers > 0) {
						// 태운 사람은 callList에서 삭제하여 다음 엘리베이터에서 callList를 사용할 때 중복된 passenger를 태우지 못하도록 한다.
						int count = MAXIMUM_NUMBER_OF_PEOPLE - numOfCurPassengers;
						List<Call> removeList = new ArrayList<>();
						for (Call passenger : callList) { // 모든 call중 최상층, DOWN방향이면 다 태운다.
							if (passenger.getDirection() == DIR_UP && passenger.getStart() == minHeight) {
								curPassengers.add(passenger);
								removeList.add(passenger);
								count--;
								if (count == 0)
									break;
							}
						}
						callList.removeAll(removeList);
						int[] callIds = new int[curPassengers.size()];
						int idx = 0;
						for (Call passenger : curPassengers) {
							callIds[idx++] = passenger.getId();
						}
						command.setCommand(COMMAND_ENTER);
						command.setCallIds(callIds);
					}

					// 3. 내릴사람, 탈사람이 더이상 없으면 문을닫고 반대방향으로 지정한다.
					else {
						command.setCommand(COMMAND_CLOSE);
						toggleElevDir(eID);
					}
				}
				// 문 열려있는 상태 완료
				
				
				
			//	하강중이며 최하층이 아닐 때
			} else {
				// 내릴사람이 있다
				if (takeOff) {
					
					// 올라가는 중 이면 STOP
					if (curStatus.equals(STATUS_DOWNWARD)) {
						command.setCommand(COMMAND_STOP);
					
					//	정지 상태이면 OPEN
					} else if (curStatus.equals(STATUS_STOPPED)) {
						command.setCommand(COMMAND_OPEN);
					
					// 문이 열려 있으면 내릴 사람들 내려주기
					} else if (curStatus.equals(STATUS_OPENED)) {
						List<Integer> takeOffList = new LinkedList<>();
						for (Call passenger : curPassengers) {
							if (passenger.getEnd() == curFloor) { // 내릴층이 같으면
								takeOffList.add(passenger.getId());
							}
						}
						int[] callIds = new int[takeOffList.size()];
						int idx = 0;
						for (Integer id : takeOffList) {
							callIds[idx++] = id;
						}
						command.setCommand(COMMAND_EXIT);
						command.setCallIds(callIds);
					}
					
				// 내릴사람이 없고 탈사람만 있으면서 탈 공간이 있다.
				} else if (takeOnSameDir && numOfCurPassengers < MAXIMUM_NUMBER_OF_PEOPLE) {
					
					// 올라가는 중 이면 STOP
					if (curStatus.equals(STATUS_DOWNWARD)) {
						command.setCommand(COMMAND_STOP);
						
					// 정지 상태이면 OPEN
					} else if (curStatus.equals(STATUS_STOPPED)) {
						command.setCommand(COMMAND_OPEN);
					
					// 문이 열려 있으면 태울 사람 태우기
					} else if (curStatus.equals(STATUS_OPENED)) {
						// 태운 사람은 callList에서 삭제하여 다음 엘리베이터에서 callList를 사용할 때 중복된 passenger를 태우지 못하도록 한다.
						List<Integer> takeOnList = new LinkedList<>();
						List<Call> removeList = new ArrayList<>();
						int count = MAXIMUM_NUMBER_OF_PEOPLE - numOfCurPassengers;
						for (Call passenger : callList) { // 모든 call중 현재 층, UP방향이면 다 태운다.
							if (passenger.getDirection() == curDir && passenger.getStart() == curFloor) {
								takeOnList.add(passenger.getId());
								removeList.add(passenger);
								count--;
								if (count == 0)
									break;
							}
						}
						callList.removeAll(removeList);
						
						int[] callIds = new int[takeOnList.size()];
						int idx = 0;
						for (Integer id : takeOnList) {
							callIds[idx++] = id;
						}
						command.setCommand(COMMAND_ENTER);
						command.setCallIds(callIds);
					}
					
					
				// 내릴사람도 없고 탈사람도 없다 -> 올라가도록 함
				} else {
					
					// 문 열려있으면 문닫고
					if (curStatus.equals(STATUS_OPENED)) {
						command.setCommand(COMMAND_CLOSE);
					
					// 문 닫겨있거나 위로 올라가던 중이라면, 올라가거라
					} else {
						command.setCommand(COMMAND_DOWN);
					}
					
				}
			
			}
			
		}
		
		
		
		
		return command;
	}
	
	private void toggleElevDir(int eID) {
		this.elevDirection[eID] *= (-1);
	}

	private boolean onCalls() {
		JSONObject responseJson = Connection.getInstance().onCalls();
//		System.out.println("onCalls JSONDATA : " + responseJson + "\n");
		elevatorList.clear();
		callList.clear();
		try {
			elevatorList.addAll(parser.getElevatorsFromOnCalls(responseJson));
//			System.out.println("onCalls::: elevatorList\n" + elevatorList);
			callList.addAll(parser.getCallsFromOnCalls(responseJson));
//			System.out.println("onCalls::: callList\n" + callList);
			
			return responseJson.getBoolean("is_end");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
}
