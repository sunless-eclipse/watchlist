package server.watchlist.data;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import server.watchlist.Application;

public class ApiThread implements Runnable {
	private final long DELTA_T = 2_000L;

	private ConcurrentLinkedQueue<String> requestQ;
	private ConcurrentLinkedQueue<String> responseQ;
	private long nextValidTime;
	private boolean paused = false;

	public ApiThread(ConcurrentLinkedQueue<String> requestQ, ConcurrentLinkedQueue<String> responseQ) {
		this.requestQ = requestQ;
		this.responseQ = responseQ;
		nextValidTime = System.currentTimeMillis();
	}
	
	public synchronized void pause() {
		paused = true;
		try {
			TimeUnit.NANOSECONDS.sleep(DELTA_T);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public synchronized void unpause() {
		paused = false;
		try {
			TimeUnit.NANOSECONDS.sleep(DELTA_T);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		while (true) {
			while ((System.currentTimeMillis() - nextValidTime) < 0);
			while(paused);
			String requestString = null;
			while (requestString == null) {
				requestString = requestQ.poll();
			}
			
			nextValidTime = System.currentTimeMillis() + DELTA_T;
			
			// send request
			HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
			HttpRequest request = HttpRequest.newBuilder().uri(URI.create("https://graphql.anilist.co")).timeout(Duration.ofSeconds(20)).header("Content-Type", "application/json").POST(BodyPublishers.ofString(requestString)).build();
			String responseString;
			try {
				HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
				Optional<String> rateLimitHeaderOptional = response.headers().firstValue("X-RateLimit-Remaining");
				if(rateLimitHeaderOptional.isPresent()) {
					if(Integer.parseInt(rateLimitHeaderOptional.get()) > 0) {
						nextValidTime = System.currentTimeMillis();
					}
				}
				Optional<String> rateLimitResetHeaderOptional = response.headers().firstValue("X-RateLimit-Reset");
				if(rateLimitResetHeaderOptional.isPresent()) {
					nextValidTime = Long.parseLong(rateLimitResetHeaderOptional.get()) * 1000;
				}
				if(response.statusCode() == 200) {
					responseString = response.body();
					System.out.println(responseString);
				} else {
					responseString = null;
				}
			} catch (IOException | InterruptedException e) {
				responseString = null;
			}
			if(responseString == null)
				continue;
			//return responseString to other thread
			responseQ.offer(responseString);
		}
	}

}
