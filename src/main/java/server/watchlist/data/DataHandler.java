package server.watchlist.data;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.Cookie;
import server.watchlist.Application;
import server.watchlist.security.Session;

public class DataHandler {
	public static final String VERSION = "v1.1";
	private static final int ENTRIES_PER_PAGE = 10;
	
	private static User[] users;
	private static ConcurrentLinkedQueue<String> requestQ = new ConcurrentLinkedQueue<String>();
	private static ConcurrentLinkedQueue<String> responseQ = new ConcurrentLinkedQueue<String>();
	private static HashMap<Integer, ResponseObject> details = new HashMap<Integer, ResponseObject>();
	private static Thread t;
	private static ApiThread apiT;
	
	public static void startup() throws StreamReadException, DatabindException, IOException {
		deserializeUsers();
		deserializeDetails();
		startApiThread();
		for(User u : users) {
			for(AnimeEntry ae : u.getList()) {
				getDetails(ae.getId());
			}
		}
	}
	
	public static void save() {
		try {
			serializeUsers();
		} catch (IOException e) {
			e.printStackTrace();
		}
		refreshDetails();
	}
	
	private static void startApiThread() {
		apiT = new ApiThread(requestQ, responseQ);
		t = new Thread(apiT);
		t.start();
	}
	
	public static ResponseObject getDetails(int id) {
		ResponseObject r = details.getOrDefault(id, new ResponseObject(id));
		if(!r.isValid()) {
			requestInfoById(id);
			Application.log("failed to get #" + id + ", retrying");
		}
		return r;
	}
	
	public static void requestInfoById(int id) {
		String requestString = buildRequestString("Int", "id", id, false);
		queueRequest(requestString);
	}
	
	public static List<ResponseObject> requestInfoBySearch(String search) {
		apiT.pause();
		ConcurrentLinkedQueue<String> qIn_manual = new ConcurrentLinkedQueue<String>();
		ConcurrentLinkedQueue<String> qOut_manual = new ConcurrentLinkedQueue<String>();
		Thread t_manual = new Thread(new ApiThread(qIn_manual, qOut_manual));
		String requestString = buildRequestString("String", "search", "\"" + search + "\"", true);
		t_manual.start();
		qIn_manual.offer(requestString);
		
		final long T_TIMEOUT = 25_000000000L;
		long start = System.nanoTime();
		
		while(qOut_manual.isEmpty() && (System.nanoTime() - start - T_TIMEOUT < 0));
		String out = qOut_manual.poll();
		
		try {
			t_manual.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		apiT.unpause();
		if(out == null)
			return null;
		try {
			return deserializeResponseList(out).toList();
		} catch (JsonProcessingException e) {
			Application.log("error while deserializing");
			return null;
		}
	}
	
	private static void queueRequest(String requestString) {
		if(!requestQ.contains(requestString))
			requestQ.offer(requestString);
	}
	
	private static <T> String buildRequestString(String variableType, String variableName, T variableValue, boolean listMode) {
		String requestString = "";
		requestString += "{\"query\":\"query ($";
		
		if(listMode)
			requestString += "page: Int, $perPage: Int, $";
		
		requestString += variableName;
		requestString += ": ";
		requestString += variableType;
		requestString += ") {";
		
		if(listMode) {
			requestString += "Page(page: $page, perPage: $perPage) {media";
		} else {
			requestString += "Media";
		}
		
		requestString += "(" + variableName + ": $" + variableName + ", type: ANIME){id episodes title{romaji english} coverImage{medium} genres siteUrl}"; //internal query (from graphql query builder)
		
		if(listMode)
			requestString += "}";
		
		requestString += "}\", \"variables\": { \"";
		requestString += variableName;
		requestString += "\": ";
		requestString += variableValue;
		
		if(listMode) {
			requestString += ", \"page\": 1, \"perPage\": " + ENTRIES_PER_PAGE;
		}
		
		requestString += "}}";
		return requestString;
	}
	
	public static Cookie makeUsernameCookie(String username) {
		Cookie c = new Cookie("username", username);
		c.setHttpOnly(true);
		c.setMaxAge(60 * 60 * 24 * 365);
		c.setPath("/");
		return c;
	}
	
	public static Cookie makeNewSessionIdCookie(Session session) {
		Cookie c;
		try {
			c = new Cookie("sessionId", session.generateNewId());
			save();
		} catch (Exception e) {
			return new Cookie("", "");
		}
		c.setHttpOnly(true);
		c.setMaxAge(60 * 60 * 24 * 365); //1 year
		c.setPath("/");
		return c;
	}
	
	public static void refreshDetails() {
		while(responseQ.peek() != null) {
			ResponseObject r = null;
			try {
				r = deserializeResponse(responseQ.poll());
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
			if(r == null)
				continue;
			details.put(r.getId(), r);
			try {
				serializeDetails();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private static ResponseObjectList deserializeResponseList(String responseString) throws JsonMappingException, JsonProcessingException {
		return new ObjectMapper().readValue(responseString, ResponseObjectList.class);
	}
	
	private static ResponseObject deserializeResponse(String responseString) throws JsonMappingException, JsonProcessingException {
		return new ObjectMapper().readValue(responseString, ResponseObject.class);
	}
	
	public static void deserializeUsers() throws StreamReadException, DatabindException, IOException {
		byte[] data = Files.readAllBytes(Paths.get("data.json"));
		users = new ObjectMapper().readValue(data, User[].class);
	}
	
	public static void serializeUsers() throws JsonProcessingException, IOException {
		Files.writeString(Paths.get("data.json"), new ObjectMapper().writeValueAsString(users), StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
	}
	
	public static void serializeDetails() throws IOException {
		FileOutputStream file = new FileOutputStream("cache.data");
		ObjectOutputStream out = new ObjectOutputStream(file);
		out.writeObject(details);
		out.close();
		file.close();
	}
	
	public static void deserializeDetails() {
		try {
			FileInputStream file = new FileInputStream("cache.data");
			ObjectInputStream in = new ObjectInputStream(file);
			details = (HashMap<Integer, ResponseObject>) in.readObject();
			in.close();
			file.close();
		} catch (IOException | ClassNotFoundException e) {
			details = new HashMap<Integer, ResponseObject>();
		}
	}
	
	public static User[] getUsers() {
		return users;
	}
	
	public static User getUser(String name) throws Exception {
		for(User u : users) {
			if(u.getName().equals(name))
				return u;
		}
		throw new Exception("User not found");
	}
	
	public static String exportList(User user) {
		String str = "";
		for(AnimeEntry ae : user.getList()) {
			if(ae.getFinished())
				str += "#";
			else
				str += " ";
			str += switch(user.getLang()) {
			case english -> getDetails(ae.getId()).getEnglish();
			case romaji -> getDetails(ae.getId()).getRomaji();
			};
			if(!ae.getNote().isEmpty()) {
				str += " (" + ae.getNote();
				str += ")";
			}
			str += "\n";
		}
		return str;
	}
}
